const test = require('node:test');
const assert = require('node:assert/strict');
const jwt = require('jsonwebtoken');
const { encender, agregarUsuario, crearPerfilDe, tokenDe } = require('./ayuda');

let red = 0;
const enviar = (url, ruta, token, cuerpo, metodo = 'POST') =>
  fetch(`${url}${ruta}`, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      'X-Forwarded-For': `198.51.100.${(red = (red % 250) + 1)}`,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(cuerpo),
  });

const DATOS = { oficio_principal: 'Plomero', zona_cobertura: 'Norte de Quito', anios_experiencia: 3 };

test('una cuenta de contratante pasa a prestador con el MISMO correo, sin crear otra cuenta', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const u = agregarUsuario('misma@ejemplo.com', 'ClaveCorrecta123', { rol: 'CONTRATANTE' });
  const tokenViejo = tokenDe(u);

  const r = await enviar(url, '/perfiles/activar', tokenViejo, DATOS);
  assert.equal(r.status, 201);
  const cuerpo = await r.json();
  assert.equal(cuerpo.usuario.rol, 'PRESTADOR');
  assert.match(cuerpo.perfil_prestador_id, /^[0-9a-f-]{36}$/);

  // el token nuevo ya dice PRESTADOR y es de la misma persona (el viejo seguia diciendo CONTRATANTE)
  const nuevo = jwt.verify(cuerpo.token, process.env.JWT_SECRET);
  assert.equal(nuevo.rol, 'PRESTADOR');
  assert.equal(nuevo.sub, u.id);
  assert.equal(jwt.verify(tokenViejo, process.env.JWT_SECRET).rol, 'CONTRATANTE');

  // y al iniciar sesion de nuevo con el mismo correo y contrasena, ya aparece como prestador con su perfil
  const login = await enviar(url, '/auth/login', null, { email: 'misma@ejemplo.com', password: 'ClaveCorrecta123' });
  assert.equal(login.status, 200);
  const sesion = await login.json();
  assert.equal(sesion.usuario.rol, 'PRESTADOR');
  assert.equal(sesion.perfil_prestador_id, cuerpo.perfil_prestador_id);
});

test('activar dos veces da 409; datos invalidos 400; sin token 401', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const u = agregarUsuario('dos@ejemplo.com', 'ClaveCorrecta123');
  const token = tokenDe(u);

  assert.equal((await enviar(url, '/perfiles/activar', token, { ...DATOS, zona_cobertura: '' })).status, 400);
  assert.equal((await enviar(url, '/perfiles/activar', token, { ...DATOS, anios_experiencia: 99 })).status, 400);
  assert.equal((await enviar(url, '/perfiles/activar', null, DATOS)).status, 401);

  assert.equal((await enviar(url, '/perfiles/activar', token, DATOS)).status, 201);
  const otra = await enviar(url, '/perfiles/activar', token, DATOS);
  assert.equal(otra.status, 409);
  assert.match((await otra.json()).error, /Ya ofreces servicios/);
});

test('un prestador tambien puede calificar a otros, pero no a si mismo', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const duena = agregarUsuario('duena@ejemplo.com', 'ClaveCorrecta123', { rol: 'PRESTADOR' });
  const perfilDuena = crearPerfilDe(duena);
  const colega = agregarUsuario('colega@ejemplo.com', 'ClaveCorrecta123', { rol: 'PRESTADOR' });
  const cliente = agregarUsuario('cliente@ejemplo.com', 'ClaveCorrecta123', { rol: 'CONTRATANTE' });
  const calificar = (token, perfilId, puntuacion = 5) =>
    enviar(url, '/calificaciones', token, { perfil_prestador_id: perfilId, puntuacion, comentario: 'Muy puntual' });

  assert.equal((await calificar(tokenDe(cliente), perfilDuena.id)).status, 201, 'un contratante califica');
  assert.equal((await calificar(tokenDe(colega), perfilDuena.id)).status, 201, 'un prestador califica a otro');

  const propio = await calificar(tokenDe(duena), perfilDuena.id);
  assert.equal(propio.status, 403);
  assert.equal((await propio.json()).error, 'No puedes calificar tu propio perfil');

  assert.equal((await calificar(tokenDe(cliente), perfilDuena.id)).status, 409, 'una sola vez por prestador');
  assert.equal((await calificar(tokenDe(cliente), '00000000-0000-4000-9000-999999999999')).status, 404);
  assert.equal((await calificar(tokenDe(colega), perfilDuena.id, 6)).status, 400);
});

test('lo exclusivo del prestador sigue cerrado para un contratante', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const cliente = agregarUsuario('solo-cliente@ejemplo.com', 'ClaveCorrecta123', { rol: 'CONTRATANTE' });
  const r = await enviar(url, '/servicios', tokenDe(cliente), { titulo: 'x', descripcion: 'x', categoria: 'OTROS' });
  assert.equal(r.status, 403);
});
