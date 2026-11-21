const test = require('node:test');
const assert = require('node:assert/strict');
const { edadEnAnios } = require('../validar');
const { encender } = require('./ayuda');

const hoy = new Date();
// fecha de hace 'anios' anios (mas 'dias' dias), en AAAA-MM-DD
const fechaHace = (anios, dias = 0) =>
  new Date(Date.UTC(hoy.getUTCFullYear() - anios, hoy.getUTCMonth(), hoy.getUTCDate() + dias)).toISOString().slice(0, 10);

let red = 0;
const registrar = (url, datos) =>
  fetch(`${url}/auth/registro`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': `192.0.2.${(red = (red % 250) + 1)}` },
    body: JSON.stringify({
      nombre: 'Prueba', password: 'ClaveCorrecta123', rol: 'CONTRATANTE',
      email: `edad${red}@ejemplo.com`, telefono: `098000${String(red).padStart(4, '0')}`, ...datos,
    }),
  });

test('la edad se calcula en anios cumplidos', () => {
  const ref = new Date(Date.UTC(2026, 10, 15));
  assert.equal(edadEnAnios('2008-11-15', ref), 18, 'cumple 18 hoy');
  assert.equal(edadEnAnios('2008-11-16', ref), 17, 'cumple 18 manana');
  assert.equal(edadEnAnios('1995-06-15', ref), 31);
  assert.equal(edadEnAnios('2000-02-30', ref), null, 'esa fecha no existe');
  assert.equal(edadEnAnios('15/06/1995', ref), null, 'formato equivocado');
  assert.equal(edadEnAnios('', ref), null);
  assert.ok(edadEnAnios('2030-01-01', ref) < 0, 'fecha futura');
});

test('un mayor de edad se registra', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  assert.equal((await registrar(url, { fecha_nacimiento: '1990-01-20' })).status, 201);
  assert.equal((await registrar(url, { fecha_nacimiento: fechaHace(18) })).status, 201, 'justo hoy cumple 18');
});

test('un menor de edad NO se puede registrar', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  for (const fecha of [fechaHace(17), fechaHace(18, 1), fechaHace(10)]) {
    const r = await registrar(url, { fecha_nacimiento: fecha });
    assert.equal(r.status, 400, `fecha ${fecha}`);
    const cuerpo = await r.json();
    assert.equal(cuerpo.campo, 'fecha_nacimiento');
    assert.match(cuerpo.error, /mayores de 18/);
  }
});

test('sin fecha, con fecha inventada o futura, no se registra', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  for (const fecha of [undefined, '', '2000-02-30', 'ayer', '2999-01-01', '1800-01-01']) {
    const r = await registrar(url, { fecha_nacimiento: fecha });
    assert.equal(r.status, 400, `fecha ${fecha}`);
    assert.equal((await r.json()).campo, 'fecha_nacimiento');
  }
});
