const test = require('node:test');
const assert = require('node:assert/strict');
const { encender, agregarUsuario } = require('./ayuda');

const login = (url, email, password, ip) =>
  fetch(`${url}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': ip },
    body: JSON.stringify({ email, password }),
  });

test('equivocarse con UNA cuenta no bloquea a las demas, ni a la misma cuenta desde otra red', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  agregarUsuario('ana@ejemplo.com', 'ClaveCorrecta123');

  // 10 intentos fallidos de la misma cuenta desde la misma red: siguen siendo 401
  for (let i = 0; i < 10; i++) {
    const r = await login(url, 'ana@ejemplo.com', 'mala-contrasena', '203.0.113.5');
    assert.equal(r.status, 401, `intento ${i + 1}`);
  }
  // el 11 se bloquea, y responde JSON claro
  const bloqueado = await login(url, 'ana@ejemplo.com', 'mala-contrasena', '203.0.113.5');
  assert.equal(bloqueado.status, 429);
  assert.match((await bloqueado.json()).error, /intentos fallidos/i);

  // otra cuenta desde LA MISMA red (como en la universidad): no se bloquea
  const otraCuenta = await login(url, 'luis@ejemplo.com', 'x', '203.0.113.5');
  assert.equal(otraCuenta.status, 401);
  // la misma cuenta desde OTRA red: no se bloquea
  const otraRed = await login(url, 'ana@ejemplo.com', 'ClaveCorrecta123', '198.51.100.9');
  assert.equal(otraRed.status, 200);
});

test('quien entra bien muchas veces nunca se bloquea (solo cuentan los intentos fallidos)', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  agregarUsuario('clase@ejemplo.com', 'ClaveCorrecta123');
  for (let i = 0; i < 40; i++) {
    const r = await login(url, 'clase@ejemplo.com', 'ClaveCorrecta123', '203.0.113.77');
    assert.equal(r.status, 200, `inicio de sesion ${i + 1}`);
  }
});

test('un JSON mal formado responde 400 en JSON, sin mostrar detalles internos', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const r = await fetch(`${url}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': '203.0.113.200' },
    body: '{esto no es json',
  });
  assert.equal(r.status, 400);
  assert.deepEqual(await r.json(), { error: 'El cuerpo no es un JSON valido' });
});

test('/health responde siempre, aunque la base de datos no exista', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const r = await fetch(`${url}/health`);
  assert.equal(r.status, 200);
  assert.deepEqual(await r.json(), { estado: 'ok' });
});
