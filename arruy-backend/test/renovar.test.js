const test = require('node:test');
const assert = require('node:assert/strict');
const jwt = require('jsonwebtoken');
const { encender, agregarUsuario, crearPerfilDe, tokenDe } = require('./ayuda');

const renovar = (url, token) =>
  fetch(`${url}/auth/renovar`, { method: 'POST', headers: token ? { Authorization: `Bearer ${token}` } : {} });

test('con un token vigente se entrega uno nuevo, con los mismos datos de la cuenta', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const u = agregarUsuario('renueva@ejemplo.com', 'ClaveCorrecta123', { rol: 'PRESTADOR' });
  const perfil = crearPerfilDe(u);
  const r = await renovar(url, tokenDe(u));
  assert.equal(r.status, 200);
  const cuerpo = await r.json();
  assert.equal(cuerpo.usuario.id, u.id);
  assert.equal(cuerpo.perfil_prestador_id, perfil.id);
  assert.equal(cuerpo.usuario.password_hash, undefined, 'nunca se entrega el hash');
  const datos = jwt.verify(cuerpo.token, process.env.JWT_SECRET);
  assert.equal(datos.sub, u.id);
  assert.ok(datos.exp - datos.iat >= 29 * 24 * 3600, 'dura unos 30 dias');
});

test('sin token, con token falso o de una cuenta que ya no existe, no se renueva', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  assert.equal((await renovar(url)).status, 401);
  assert.equal((await renovar(url, 'token.falso.aqui')).status, 401);
  const fantasma = tokenDe({ id: '00000000-0000-4000-8000-999999999999', rol: 'CONTRATANTE' });
  assert.equal((await renovar(url, fantasma)).status, 401);
});
