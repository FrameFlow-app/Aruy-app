const test = require('node:test');
const assert = require('node:assert/strict');
const { encender } = require('./ayuda');

let red = 0;
const registrar = (url, datos) =>
  fetch(`${url}/auth/registro`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': `203.0.113.${(red = (red % 250) + 1)}` },
    body: JSON.stringify({ nombre: 'Prueba', password: 'ClaveCorrecta123', rol: 'CONTRATANTE', fecha_nacimiento: '1995-06-15', ...datos }),
  });

test('registrarse con un correo y un celular nuevos funciona', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const r = await registrar(url, { email: 'nuevo@ejemplo.com', telefono: '0991111111' });
  assert.equal(r.status, 201);
  assert.equal((await r.json()).usuario.rol, 'CONTRATANTE');
});

test('un correo repetido dice que el repetido es el CORREO (aunque cambie mayusculas o espacios)', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  assert.equal((await registrar(url, { email: 'ana@ejemplo.com', telefono: '0992222222' })).status, 201);
  const r = await registrar(url, { email: '  ANA@Ejemplo.com ', telefono: '0993333333' });
  assert.equal(r.status, 409);
  const cuerpo = await r.json();
  assert.equal(cuerpo.campo, 'email');
  assert.match(cuerpo.error, /correo/);
});

test('un CELULAR repetido con un correo distinto dice que el repetido es el CELULAR (el error que se veia)', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  assert.equal((await registrar(url, { email: 'luis@ejemplo.com', telefono: '0994444444' })).status, 201);
  const r = await registrar(url, { email: 'otro-correo@ejemplo.com', telefono: '0994444444' });
  assert.equal(r.status, 409);
  const cuerpo = await r.json();
  assert.equal(cuerpo.campo, 'telefono');
  assert.match(cuerpo.error, /celular/);
});

test('con correo y celular distintos se pueden crear varias cuentas seguidas', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  for (let i = 1; i <= 3; i++) {
    const r = await registrar(url, { email: `persona${i}@ejemplo.com`, telefono: `099555000${i}` });
    assert.equal(r.status, 201, `cuenta ${i}`);
  }
});
