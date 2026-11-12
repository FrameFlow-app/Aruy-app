const test = require('node:test');
const assert = require('node:assert/strict');
const { encender, agregarUsuario, crearPerfilDe, tokenDe } = require('./ayuda');

let red = 0;
const publicar = (url, token, titulo) =>
  fetch(`${url}/servicios`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Forwarded-For': `203.0.113.${(red = (red % 250) + 1)}`,
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ titulo, descripcion: 'Servicio de prueba', categoria: 'PLOMERIA' }),
  });

function prestador(correo) {
  const u = agregarUsuario(correo, 'ClaveCorrecta123', { rol: 'PRESTADOR' });
  crearPerfilDe(u);
  return tokenDe(u);
}

test('un prestador publica 2 servicios y el 3.o recibe 409 con mensaje claro; otro prestador no se ve afectado', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const a = prestador('limite-a@ejemplo.com');
  const b = prestador('limite-b@ejemplo.com');

  assert.equal((await publicar(url, a, 'Uno')).status, 201);
  assert.equal((await publicar(url, a, 'Dos')).status, 201);

  const tercero = await publicar(url, a, 'Tres');
  assert.equal(tercero.status, 409);
  assert.equal((await tercero.json()).error, 'Puedes tener como maximo 2 servicios. Elimina uno para publicar otro.');

  assert.equal((await publicar(url, b, 'Del otro')).status, 201);
});

test('dos publicaciones seguidas con el limite alcanzado siguen dando 409', async (t) => {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  const a = prestador('limite-c@ejemplo.com');
  await publicar(url, a, 'Uno');
  await publicar(url, a, 'Dos');
  const r = await Promise.all([publicar(url, a, 'Tres'), publicar(url, a, 'Cuatro')]);
  assert.deepEqual(r.map((x) => x.status), [409, 409]);
});

// El limite de 3 fotos (MAX_FOTOS) no se prueba aqui: la ruta POST /servicios/:id/fotos exige Supabase
// configurado y la libreria sharp antes de llegar al conteo (responde 503 si faltan), y la base falsa no
// cubre fotos_portafolio. Se verifica por revision del codigo y en la app con la prueba de mensajes.
