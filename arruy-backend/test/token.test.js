process.env.JWT_SECRET = 'clave-de-prueba-solo-para-tests';
const test = require('node:test');
const assert = require('node:assert/strict');
const jwt = require('jsonwebtoken');
const { firmarToken, duracionToken } = require('../middleware/auth');

const usuario = { id: '22222222-2222-4222-8222-222222222222', rol: 'PRESTADOR' };
const duracionEnSegundos = (token) => {
  const d = jwt.verify(token, process.env.JWT_SECRET);
  return d.exp - d.iat;
};

test('la sesion dura 30 dias por defecto', () => {
  delete process.env.JWT_EXPIRES_IN;
  assert.equal(duracionToken(), '30d');
  assert.equal(duracionEnSegundos(firmarToken(usuario)), 30 * 24 * 3600);
});

test('se puede cambiar con JWT_EXPIRES_IN', () => {
  process.env.JWT_EXPIRES_IN = '12h';
  assert.equal(duracionEnSegundos(firmarToken(usuario)), 12 * 3600);
  process.env.JWT_EXPIRES_IN = '7d';
  assert.equal(duracionEnSegundos(firmarToken(usuario)), 7 * 24 * 3600);
});

test('un valor invalido o 0 vuelve a los 30 dias (nunca un token que vence al instante)', () => {
  for (const malo of ['abc', '0d', '-5h', '10', '1.5d', ' ']) {
    process.env.JWT_EXPIRES_IN = malo;
    assert.equal(duracionToken(), '30d', `valor "${malo}"`);
  }
  delete process.env.JWT_EXPIRES_IN;
});

test('el token lleva el usuario (sub) y el rol', () => {
  const d = jwt.verify(firmarToken(usuario), process.env.JWT_SECRET);
  assert.equal(d.sub, usuario.id);
  assert.equal(d.rol, 'PRESTADOR');
});
