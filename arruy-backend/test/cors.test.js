const test = require('node:test');
const assert = require('node:assert/strict');
const { encender } = require('./ayuda');

const PERMITIDO = 'https://frameflow-app.github.io';

async function servidor(t) {
  const { servidor: s, url } = await encender();
  t.after(() => s.close());
  return url;
}

test('preflight desde un origen permitido responde 204 con las cabeceras CORS', async (t) => {
  const url = await servidor(t);
  const r = await fetch(`${url}/servicios`, {
    method: 'OPTIONS',
    headers: { Origin: PERMITIDO, 'Access-Control-Request-Method': 'POST', 'Access-Control-Request-Headers': 'authorization,content-type' },
  });
  assert.equal(r.status, 204);
  assert.equal(r.headers.get('access-control-allow-origin'), PERMITIDO);
  assert.equal(r.headers.get('access-control-allow-methods'), 'GET,POST,PATCH,DELETE,OPTIONS');
  assert.equal(r.headers.get('access-control-allow-headers'), 'Authorization, Content-Type');
  assert.equal(r.headers.get('access-control-max-age'), '600');
  assert.equal(r.headers.get('access-control-allow-credentials'), null);
  assert.match(r.headers.get('vary') || '', /Origin/i);
});

test('GET desde un origen permitido trae Access-Control-Allow-Origin', async (t) => {
  const url = await servidor(t);
  const r = await fetch(`${url}/health`, { headers: { Origin: 'http://localhost:5173' } });
  assert.equal(r.status, 200);
  assert.equal(r.headers.get('access-control-allow-origin'), 'http://localhost:5173');
});

test('origen no permitido: sin cabeceras CORS pero con Vary: Origin', async (t) => {
  const url = await servidor(t);
  const r = await fetch(`${url}/health`, { headers: { Origin: 'https://malo.example.com' } });
  assert.equal(r.headers.get('access-control-allow-origin'), null);
  assert.match(r.headers.get('vary') || '', /Origin/i);
  const pre = await fetch(`${url}/health`, { method: 'OPTIONS', headers: { Origin: 'https://frameflow-app.github.io.malo.com', 'Access-Control-Request-Method': 'GET' } });
  assert.equal(pre.headers.get('access-control-allow-origin'), null);
});

test('sin Origin (app Android) funciona igual y sin CORS', async (t) => {
  const url = await servidor(t);
  const r = await fetch(`${url}/health`);
  assert.equal(r.status, 200);
  assert.deepEqual(await r.json(), { estado: 'ok' });
  assert.equal(r.headers.get('access-control-allow-origin'), null);
});

test('CORS_ORIGENES suma origenes extra', async (t) => {
  const url = await servidor(t);
  const antes = process.env.CORS_ORIGENES;
  process.env.CORS_ORIGENES = 'https://extra.example.com, https://otro.example.com/';
  t.after(() => {
    if (antes === undefined) delete process.env.CORS_ORIGENES;
    else process.env.CORS_ORIGENES = antes;
  });
  for (const o of ['https://extra.example.com', 'https://otro.example.com']) {
    const r = await fetch(`${url}/health`, { headers: { Origin: o } });
    assert.equal(r.headers.get('access-control-allow-origin'), o);
  }
});
