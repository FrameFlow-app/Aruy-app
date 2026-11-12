const test = require('node:test');
const assert = require('node:assert/strict');
const net = require('node:net');
const path = require('node:path');
const { spawn } = require('node:child_process');

const puertoLibre = () =>
  new Promise((resolver) => {
    const s = net.createServer().listen(0, () => {
      const { port } = s.address();
      s.close(() => resolver(port));
    });
  });

async function arrancar() {
  const puerto = await puertoLibre();
  const hijo = spawn(process.execPath, [path.join(__dirname, '..', 'index.js')], {
    cwd: path.join(__dirname, '..'),
    env: {
      ...process.env,
      PORT: String(puerto),
      JWT_SECRET: 'clave-de-prueba-solo-para-tests',
      DATABASE_URL: 'postgresql://x:y@127.0.0.1:1/z', // puerto cerrado: la base "no responde"
    },
  });
  let salida = '';
  hijo.stdout.on('data', (d) => (salida += d));
  hijo.stderr.on('data', (d) => (salida += d));
  for (let i = 0; i < 300 && !salida.includes('Servidor en'); i++) await new Promise((r) => setTimeout(r, 50));
  assert.ok(salida.includes('Servidor en'), 'el servidor debe arrancar. Salida: ' + salida);
  return { hijo, url: `http://127.0.0.1:${puerto}`, salida: () => salida };
}

test('si la base de datos no responde, el servidor sigue vivo, responde rapido en JSON y registra el motivo', async (t) => {
  const { hijo, url, salida } = await arrancar();
  t.after(() => hijo.kill());

  const inicio = Date.now();
  const r = await fetch(`${url}/servicios`);
  assert.ok(Date.now() - inicio < 5000, 'debe fallar rapido, no colgarse');
  assert.equal(r.status, 500);
  assert.deepEqual(await r.json(), { error: 'Error al consultar servicios' });
  assert.match(salida(), /ECONNREFUSED/, 'el registro debe mostrar el codigo del error (antes salia vacio)');

  // y sigue respondiendo
  assert.equal((await fetch(`${url}/health`)).status, 200);
});

test('al recibir SIGTERM (cada despliegue en Render) cierra ordenadamente', { skip: process.platform === 'win32' }, async () => {
  const { hijo, salida } = await arrancar();
  const codigo = await new Promise((resolver) => {
    hijo.once('exit', (c) => resolver(c));
    hijo.kill('SIGTERM');
  });
  assert.equal(codigo, 0);
  assert.match(salida(), /SIGTERM: cerrando el servidor/);
});
