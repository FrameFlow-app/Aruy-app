const test = require('node:test');
const assert = require('node:assert/strict');
const { encender, agregarUsuario, codigos } = require('./ayuda');
const correo = require('../correo');

// Correo simulado: no se envia nada real, solo se guarda lo que se habria enviado
const enviados = [];
correo.enviarCodigo = async (email, codigo) => { enviados.push({ email, codigo }); };

let n = 0;
let ip = 0;
async function escenario(t) {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  n += 1;
  const email = `recuperar${n}@ejemplo.com`;
  agregarUsuario(email, 'ClaveVieja123');
  // Cada llamada sale de una "red" distinta para no chocar con los limites por IP
  const post = async (ruta, cuerpo) => {
    ip += 1;
    const r = await fetch(`${url}${ruta}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': `10.9.${Math.floor(ip / 250)}.${ip % 250}` },
      body: JSON.stringify(cuerpo),
    });
    return { status: r.status, cuerpo: await r.json() };
  };
  const pedirCodigo = async () => {
    const antes = enviados.length;
    const r = await post('/auth/olvide', { email });
    assert.equal(r.status, 200);
    return enviados.length > antes ? enviados[enviados.length - 1].codigo : null;
  };
  return { email, post, pedirCodigo };
}

const OLVIDE = 'Si el correo está registrado, te enviamos un código.';
const INVALIDO = 'El código no es válido o ya expiró.';

test('olvide responde igual exista o no el correo, y solo envia si existe', async (t) => {
  const e = await escenario(t);
  const antes = enviados.length;
  const existe = await e.post('/auth/olvide', { email: e.email.toUpperCase() });
  const noExiste = await e.post('/auth/olvide', { email: 'nadie@ejemplo.com' });
  const malo = await e.post('/auth/olvide', { email: 'no-es-correo' });
  for (const r of [existe, noExiste, malo]) {
    assert.equal(r.status, 200);
    assert.deepEqual(r.cuerpo, { mensaje: OLVIDE });
  }
  assert.equal(enviados.length, antes + 1);
  assert.equal(enviados[antes].email, e.email);
  assert.match(enviados[antes].codigo, /^\d{6}$/);
  assert.notEqual(codigos[codigos.length - 1].codigo_hash, enviados[antes].codigo, 'el codigo se guarda con hash');
});

test('olvide no falla aunque el envio del correo falle', async (t) => {
  const e = await escenario(t);
  const original = correo.enviarCodigo;
  correo.enviarCodigo = async () => { throw new Error('SMTP caido'); };
  t.after(() => { correo.enviarCodigo = original; });
  const r = await e.post('/auth/olvide', { email: e.email });
  assert.equal(r.status, 200);
});

test('con el codigo correcto se cambia la contrasena y el codigo no se puede reusar', async (t) => {
  const e = await escenario(t);
  const codigo = await e.pedirCodigo();
  const r = await e.post('/auth/restablecer', { email: e.email, codigo, password: 'ClaveNueva456' });
  assert.equal(r.status, 200);
  assert.deepEqual(r.cuerpo, { mensaje: 'Contraseña actualizada' });
  assert.equal((await e.post('/auth/login', { email: e.email, password: 'ClaveNueva456' })).status, 200);
  assert.equal((await e.post('/auth/login', { email: e.email, password: 'ClaveVieja123' })).status, 401);
  const otra = await e.post('/auth/restablecer', { email: e.email, codigo, password: 'ClaveOtra789' });
  assert.equal(otra.status, 400);
  assert.equal(otra.cuerpo.error, INVALIDO);
});

test('un codigo incorrecto, inexistente o vencido se rechaza', async (t) => {
  const e = await escenario(t);
  const sinCodigo = await e.post('/auth/restablecer', { email: e.email, codigo: '123456', password: 'ClaveNueva456' });
  assert.equal(sinCodigo.status, 400);
  assert.equal(sinCodigo.cuerpo.error, INVALIDO);

  const codigo = await e.pedirCodigo();
  const equivocado = codigo === '000000' ? '111111' : '000000';
  const mal = await e.post('/auth/restablecer', { email: e.email, codigo: equivocado, password: 'ClaveNueva456' });
  assert.equal(mal.status, 400);
  assert.equal(mal.cuerpo.error, INVALIDO);

  codigos[codigos.length - 1].expira = new Date(Date.now() - 1000);
  const vencido = await e.post('/auth/restablecer', { email: e.email, codigo, password: 'ClaveNueva456' });
  assert.equal(vencido.status, 400);
  assert.equal(vencido.cuerpo.error, INVALIDO);
  assert.equal((await e.post('/auth/login', { email: e.email, password: 'ClaveVieja123' })).status, 200, 'la contrasena sigue igual');
});

test('al quinto intento fallido el codigo se anula, aunque despues se escriba bien', async (t) => {
  const e = await escenario(t);
  const codigo = await e.pedirCodigo();
  const equivocado = codigo === '000000' ? '111111' : '000000';
  for (let i = 0; i < 5; i++) {
    assert.equal((await e.post('/auth/restablecer', { email: e.email, codigo: equivocado, password: 'ClaveNueva456' })).status, 400);
  }
  assert.equal(codigos[codigos.length - 1].usado, true);
  const tarde = await e.post('/auth/restablecer', { email: e.email, codigo, password: 'ClaveNueva456' });
  assert.equal(tarde.status, 400);
  assert.equal(tarde.cuerpo.error, INVALIDO);
});

test('una contrasena invalida se rechaza y el codigo sigue sirviendo', async (t) => {
  const e = await escenario(t);
  const codigo = await e.pedirCodigo();
  const corta = await e.post('/auth/restablecer', { email: e.email, codigo, password: 'corta' });
  assert.equal(corta.status, 400);
  assert.match(corta.cuerpo.error, /password/);
  assert.equal((await e.post('/auth/restablecer', { email: e.email, codigo, password: 'ClaveNueva456' })).status, 200);
});

test('un codigo nuevo invalida el anterior', async (t) => {
  const e = await escenario(t);
  const primero = await e.pedirCodigo();
  const segundo = await e.pedirCodigo();
  if (primero !== segundo) {
    assert.equal((await e.post('/auth/restablecer', { email: e.email, codigo: primero, password: 'ClaveNueva456' })).status, 400);
  }
  assert.equal((await e.post('/auth/restablecer', { email: e.email, codigo: segundo, password: 'ClaveNueva456' })).status, 200);
});

test('olvide limita a 3 pedidos por hora por correo', async (t) => {
  const e = await escenario(t);
  for (let i = 0; i < 3; i++) assert.equal((await e.post('/auth/olvide', { email: e.email })).status, 200);
  assert.equal((await e.post('/auth/olvide', { email: e.email })).status, 429);
});
