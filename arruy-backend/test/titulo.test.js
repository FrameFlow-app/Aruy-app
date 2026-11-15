const test = require('node:test');
const assert = require('node:assert/strict');
const { normalizarTitulo } = require('../validar');
const { encender, agregarUsuario, crearPerfilDe, tokenDe } = require('./ayuda');

test('el titulo queda con inicial en mayuscula y el resto en minuscula', () => {
  assert.equal(normalizarTitulo('LIMPIEZA DE CASAS'), 'Limpieza de casas');
  assert.equal(normalizarTitulo('limpieza de casas'), 'Limpieza de casas');
  assert.equal(normalizarTitulo('  LiMpIeZa   dE  cAsAs  '), 'Limpieza de casas');
  assert.equal(normalizarTitulo('ÁRBOL DE NAVIDAD'), 'Árbol de navidad');
  assert.equal(normalizarTitulo('ñandú Y MÁS'), 'Ñandú y más');
  assert.equal(normalizarTitulo('   '), '');
  assert.equal(normalizarTitulo(undefined), '');
});

test('al publicar un servicio, el servidor guarda el titulo bien escrito', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const prestador = agregarUsuario('titulo@ejemplo.com', 'ClaveCorrecta123', { rol: 'PRESTADOR' });
  crearPerfilDe(prestador);
  const r = await fetch(`${url}/servicios`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tokenDe(prestador)}`, 'X-Forwarded-For': '198.51.100.201' },
    body: JSON.stringify({ titulo: 'ARREGLO DE TUBERIAS', descripcion: 'Reparo fugas y cambio llaves', categoria: 'PLOMERIA' }),
  });
  assert.equal(r.status, 201);
  assert.equal((await r.json()).titulo, 'Arreglo de tuberias');
});
