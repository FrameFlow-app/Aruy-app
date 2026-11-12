const test = require('node:test');
const assert = require('node:assert/strict');

process.env.SUPABASE_URL = 'https://proyecto-de-prueba.supabase.co/rest/v1/';
process.env.SUPABASE_SECRET_KEY = 'sb_secret_clave_de_prueba';
process.env.SUPABASE_STORAGE_BUCKET = 'aruy-portafolio';
const { mantenerActivo } = require('../almacenamiento');

test('mantenerActivo pide 1 archivo a la API de Storage con la clave del servidor', async (t) => {
  const original = globalThis.fetch;
  t.after(() => { globalThis.fetch = original; });
  let peticion;
  globalThis.fetch = async (url, opciones) => { peticion = { url, opciones }; return { ok: true, status: 200 }; };

  const r = await mantenerActivo();
  assert.deepEqual(r, { ok: true, estado: 200 });
  assert.equal(peticion.url, 'https://proyecto-de-prueba.supabase.co/storage/v1/object/list/aruy-portafolio');
  assert.equal(peticion.opciones.method, 'POST');
  assert.equal(peticion.opciones.headers.apikey, 'sb_secret_clave_de_prueba');
  assert.deepEqual(JSON.parse(peticion.opciones.body), { prefix: '', limit: 1 });
});

test('mantenerActivo nunca lanza error: informa el fallo', async (t) => {
  const original = globalThis.fetch;
  t.after(() => { globalThis.fetch = original; });

  globalThis.fetch = async () => ({ ok: false, status: 401 });
  assert.deepEqual(await mantenerActivo(), { ok: false, estado: 401 });

  globalThis.fetch = async () => { throw new Error('sin red'); };
  assert.deepEqual(await mantenerActivo(), { ok: false, motivo: 'sin red' });
});

test('sin configuracion de Supabase no llama a nadie', async (t) => {
  const url = process.env.SUPABASE_URL;
  t.after(() => { process.env.SUPABASE_URL = url; });
  delete process.env.SUPABASE_URL;
  let llamado = false;
  const original = globalThis.fetch;
  t.after(() => { globalThis.fetch = original; });
  globalThis.fetch = async () => { llamado = true; return { ok: true, status: 200 }; };
  const r = await mantenerActivo();
  assert.equal(r.ok, false);
  assert.equal(llamado, false);
});
