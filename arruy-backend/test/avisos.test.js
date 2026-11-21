const test = require('node:test');
const assert = require('node:assert/strict');
const { encender } = require('./ayuda');

const VARS = ['AVISO_MANTENIMIENTO', 'AVISO_MANTENIMIENTO_INICIO', 'AVISO_MANTENIMIENTO_FIN', 'APP_VERSION_RECIENTE', 'APP_VERSION_MINIMA', 'APP_NOTAS_VERSION'];

async function pedir(t, env = {}) {
  const guardado = {};
  for (const v of VARS) {
    guardado[v] = process.env[v];
    delete process.env[v];
  }
  Object.assign(process.env, env);
  const { servidor, url } = await encender();
  t.after(() => {
    servidor.close();
    for (const v of VARS) {
      if (guardado[v] === undefined) delete process.env[v];
      else process.env[v] = guardado[v];
    }
  });
  const r = await fetch(`${url}/avisos`);
  return { r, cuerpo: await r.json() };
}

test('sin variables: sin mantenimiento y versiones en 0, sin token', async (t) => {
  const { r, cuerpo } = await pedir(t);
  assert.equal(r.status, 200);
  assert.deepEqual(cuerpo, { mantenimiento: null, version: { reciente: 0, minima: 0, notas: '' } });
});

test('con mantenimiento y fechas validas', async (t) => {
  const fin = new Date(Date.now() + 3600_000).toISOString();
  const { cuerpo } = await pedir(t, {
    AVISO_MANTENIMIENTO: 'Volvemos pronto',
    AVISO_MANTENIMIENTO_INICIO: '2026-01-01T00:00:00Z',
    AVISO_MANTENIMIENTO_FIN: fin,
  });
  assert.deepEqual(cuerpo.mantenimiento, { activo: true, mensaje: 'Volvemos pronto', inicio: '2026-01-01T00:00:00.000Z', fin });
});

test('fechas invalidas se devuelven como null', async (t) => {
  const { cuerpo } = await pedir(t, { AVISO_MANTENIMIENTO: 'Aviso', AVISO_MANTENIMIENTO_INICIO: 'ayer', AVISO_MANTENIMIENTO_FIN: 'x' });
  assert.deepEqual(cuerpo.mantenimiento, { activo: true, mensaje: 'Aviso', inicio: null, fin: null });
});

test('si el fin ya paso, no hay mantenimiento', async (t) => {
  const { cuerpo } = await pedir(t, { AVISO_MANTENIMIENTO: 'Aviso', AVISO_MANTENIMIENTO_FIN: '2020-01-01T00:00:00Z' });
  assert.equal(cuerpo.mantenimiento, null);
});

test('versiones validas, invalidas y notas largas', async (t) => {
  const ok = await pedir(t, { APP_VERSION_RECIENTE: '12', APP_VERSION_MINIMA: '10', APP_NOTAS_VERSION: 'a'.repeat(600) });
  assert.equal(ok.cuerpo.version.reciente, 12);
  assert.equal(ok.cuerpo.version.minima, 10);
  assert.equal(ok.cuerpo.version.notas.length, 500);
  const mal = await pedir(t, { APP_VERSION_RECIENTE: 'abc', APP_VERSION_MINIMA: '-3' });
  assert.equal(mal.cuerpo.version.reciente, 0);
  assert.equal(mal.cuerpo.version.minima, 0);
});

test('el header de cache permite guardar 5 minutos', async (t) => {
  const { r } = await pedir(t);
  assert.equal(r.headers.get('cache-control'), 'public, max-age=300');
});
