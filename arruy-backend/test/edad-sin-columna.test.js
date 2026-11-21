process.env.AYUDA_SIN_COLUMNA_FECHA = '1';
const test = require('node:test');
const assert = require('node:assert/strict');
const { encender } = require('./ayuda');

const registrar = (url, ip, datos) =>
  fetch(`${url}/auth/registro`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': ip },
    body: JSON.stringify({ nombre: 'Prueba', password: 'ClaveCorrecta123', rol: 'CONTRATANTE', ...datos }),
  });

test('si la base aun no tiene la columna fecha_nacimiento, el registro funciona igual (la edad se valida, no se guarda)', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const mayor = await registrar(url, '192.0.2.250', { email: 'sin-columna@ejemplo.com', telefono: '0987654321', fecha_nacimiento: '1990-01-20' });
  assert.equal(mayor.status, 201);
  const menor = await registrar(url, '192.0.2.251', { email: 'menor@ejemplo.com', telefono: '0987654322', fecha_nacimiento: '2015-01-01' });
  assert.equal(menor.status, 400, 'aunque no haya columna, un menor no entra');
});
