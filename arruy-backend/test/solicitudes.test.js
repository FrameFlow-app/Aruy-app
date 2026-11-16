const test = require('node:test');
const assert = require('node:assert/strict');
const { encender, agregarUsuario, crearPerfilDe, crearServicioDe, tokenDe } = require('./ayuda');

let n = 0;
// Un prestador con un servicio, un contratante y sus llamadas a la API
async function escenario(t, { disponible = true } = {}) {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  n += 1;
  const prestador = agregarUsuario(`prestador${n}@ejemplo.com`, 'ClaveCorrecta123', { rol: 'PRESTADOR' });
  prestador.nombre = 'Marcos Prestador';
  prestador.telefono = '0990000001';
  const perfil = crearPerfilDe(prestador);
  const servicio = crearServicioDe(perfil, { titulo: 'Arreglo de tuberias', disponible });
  const cliente = agregarUsuario(`cliente${n}@ejemplo.com`, 'ClaveCorrecta123');
  cliente.nombre = 'Lucia Cliente';
  cliente.telefono = '0990000002';
  const api = (usuario) => async (metodo, ruta, cuerpo) => {
    const r = await fetch(`${url}${ruta}`, {
      method: metodo,
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tokenDe(usuario)}` },
      body: cuerpo ? JSON.stringify(cuerpo) : undefined,
    });
    const texto = await r.text();
    return { status: r.status, cuerpo: texto ? JSON.parse(texto) : null };
  };
  return { url, prestador, perfil, servicio, cliente, comoPrestador: api(prestador), comoCliente: api(cliente) };
}
const enviar = (e, mensaje = 'Hola, necesito arreglar una tuberia el sabado') =>
  e.comoCliente('POST', '/solicitudes', { servicio_id: e.servicio.id, mensaje });

test('sin sesion no se puede usar', async (t) => {
  const { url } = await escenario(t);
  assert.equal((await fetch(`${url}/solicitudes?tipo=enviadas`)).status, 401);
  assert.equal((await fetch(`${url}/solicitudes`, { method: 'POST' })).status, 401);
});

test('una persona envia una solicitud y queda PENDIENTE', async (t) => {
  const e = await escenario(t);
  const r = await enviar(e);
  assert.equal(r.status, 201);
  assert.equal(r.cuerpo.estado, 'PENDIENTE');
  assert.equal(r.cuerpo.servicio_titulo, 'Arreglo de tuberias');
  assert.equal(r.cuerpo.contraparte.nombre, 'Marcos Prestador');
  assert.equal(r.cuerpo.contraparte.telefono, null, 'el telefono no se comparte antes de aceptar');
});

test('los datos de la solicitud se validan', async (t) => {
  const e = await escenario(t);
  const malos = [
    { servicio_id: 'no-es-id', mensaje: 'Hola, necesito ayuda' },
    { servicio_id: e.servicio.id, mensaje: 'hola' },
    { servicio_id: e.servicio.id, mensaje: '     ' },
    { servicio_id: e.servicio.id, mensaje: 'x'.repeat(501) },
    { servicio_id: e.servicio.id },
  ];
  for (const cuerpo of malos) assert.equal((await e.comoCliente('POST', '/solicitudes', cuerpo)).status, 400, JSON.stringify(cuerpo).slice(0, 60));
  const inexistente = await e.comoCliente('POST', '/solicitudes', { servicio_id: '00000000-0000-4000-a000-999999999999', mensaje: 'Hola, necesito ayuda' });
  assert.equal(inexistente.status, 404);
});

test('nadie puede pedirse un servicio a si mismo', async (t) => {
  const e = await escenario(t);
  const r = await e.comoPrestador('POST', '/solicitudes', { servicio_id: e.servicio.id, mensaje: 'Me lo pido a mi mismo' });
  assert.equal(r.status, 403);
});

test('un servicio que no recibe solicitudes no las acepta', async (t) => {
  const e = await escenario(t, { disponible: false });
  assert.equal((await enviar(e)).status, 409);
});

test('no se puede enviar dos veces la misma solicitud pendiente', async (t) => {
  const e = await escenario(t);
  assert.equal((await enviar(e)).status, 201);
  const otra = await enviar(e);
  assert.equal(otra.status, 409);
  assert.match(otra.cuerpo.error, /pendiente/);
});

test('el prestador ve la solicitud recibida, sin el telefono del cliente hasta aceptarla', async (t) => {
  const e = await escenario(t);
  await enviar(e);
  const r = await e.comoPrestador('GET', '/solicitudes?tipo=recibidas');
  assert.equal(r.status, 200);
  assert.equal(r.cuerpo.length, 1);
  assert.equal(r.cuerpo[0].contraparte.nombre, 'Lucia Cliente');
  assert.equal(r.cuerpo[0].contraparte.telefono, null);
  assert.equal(r.cuerpo[0].sin_ver, true);
  assert.equal((await e.comoCliente('GET', '/solicitudes?tipo=recibidas')).cuerpo.length, 0, 'quien no es prestador no recibe nada');
});

test('al aceptar, el cliente ve la respuesta y el prestador ve el telefono del cliente', async (t) => {
  const e = await escenario(t);
  const { cuerpo: creada } = await enviar(e);
  const ok = await e.comoPrestador('PATCH', `/solicitudes/${creada.id}`, { estado: 'ACEPTADA', respuesta: 'Puedo el sabado a las 9' });
  assert.equal(ok.status, 200);
  assert.equal(ok.cuerpo.estado, 'ACEPTADA');
  const delCliente = (await e.comoCliente('GET', '/solicitudes?tipo=enviadas')).cuerpo[0];
  assert.equal(delCliente.estado, 'ACEPTADA');
  assert.equal(delCliente.respuesta, 'Puedo el sabado a las 9');
  assert.equal(delCliente.sin_ver, true, 'el cliente debe enterarse');
  assert.equal(delCliente.contraparte.telefono, '0990000001');
  const delPrestador = (await e.comoPrestador('GET', '/solicitudes?tipo=recibidas')).cuerpo[0];
  assert.equal(delPrestador.contraparte.telefono, '0990000002');
  assert.equal(delPrestador.sin_ver, false);
});

test('el cliente puede cancelar y el prestador se entera', async (t) => {
  const e = await escenario(t);
  const { cuerpo: creada } = await enviar(e);
  const r = await e.comoCliente('PATCH', `/solicitudes/${creada.id}`, { estado: 'CANCELADA' });
  assert.equal(r.status, 200);
  const resumen = await e.comoPrestador('GET', '/solicitudes/resumen');
  assert.equal(resumen.cuerpo.recibidas_nuevas, 1);
  assert.equal(resumen.cuerpo.recibidas_pendientes, 0, 'ya no esta pendiente');
});

test('cada quien solo puede hacer los cambios que le tocan', async (t) => {
  const e = await escenario(t);
  const { cuerpo: creada } = await enviar(e);
  const id = creada.id;
  assert.equal((await e.comoCliente('PATCH', `/solicitudes/${id}`, { estado: 'ACEPTADA' })).status, 409, 'el cliente no acepta su propia solicitud');
  assert.equal((await e.comoPrestador('PATCH', `/solicitudes/${id}`, { estado: 'COMPLETADA' })).status, 409, 'no se completa lo que no se acepto');
  assert.equal((await e.comoPrestador('PATCH', `/solicitudes/${id}`, { estado: 'INVENTADO' })).status, 400);
  assert.equal((await e.comoPrestador('PATCH', `/solicitudes/${id}`, { estado: 'RECHAZADA', respuesta: 'x'.repeat(301) })).status, 400);
  const tercero = agregarUsuario(`tercero${n}@ejemplo.com`, 'ClaveCorrecta123');
  const rt = await fetch(`${e.url}/solicitudes/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tokenDe(tercero)}` },
    body: JSON.stringify({ estado: 'RECHAZADA' }),
  });
  assert.equal(rt.status, 404, 'un tercero ni se entera de que existe');
  assert.equal((await e.comoPrestador('PATCH', `/solicitudes/${id}`, { estado: 'RECHAZADA' })).status, 200);
  assert.equal((await e.comoPrestador('PATCH', `/solicitudes/${id}`, { estado: 'ACEPTADA' })).status, 409, 'una rechazada no se reabre');
});

test('el resumen cuenta lo nuevo y al abrir la bandeja se marca como visto', async (t) => {
  const e = await escenario(t);
  const { cuerpo: creada } = await enviar(e);
  assert.deepEqual((await e.comoPrestador('GET', '/solicitudes/resumen')).cuerpo, {
    recibidas_nuevas: 1, recibidas_pendientes: 1, enviadas_novedades: 0,
  });
  assert.equal((await e.comoPrestador('POST', '/solicitudes/vistas', { tipo: 'recibidas' })).cuerpo.marcadas, 1);
  assert.equal((await e.comoPrestador('GET', '/solicitudes/resumen')).cuerpo.recibidas_nuevas, 0);
  assert.equal((await e.comoPrestador('GET', '/solicitudes/resumen')).cuerpo.recibidas_pendientes, 1, 'sigue pendiente de responder');

  await e.comoPrestador('PATCH', `/solicitudes/${creada.id}`, { estado: 'RECHAZADA', respuesta: 'Ese dia no puedo' });
  assert.equal((await e.comoCliente('GET', '/solicitudes/resumen')).cuerpo.enviadas_novedades, 1);
  assert.equal((await e.comoCliente('POST', '/solicitudes/vistas', { tipo: 'enviadas' })).cuerpo.marcadas, 1);
  assert.equal((await e.comoCliente('GET', '/solicitudes/resumen')).cuerpo.enviadas_novedades, 0);
  assert.equal((await e.comoCliente('POST', '/solicitudes/vistas', { tipo: 'otra' })).status, 400);
});

test('quien ofrece servicios tambien puede pedir el servicio de otro prestador (una sola cuenta)', async (t) => {
  const e = await escenario(t);
  const otro = agregarUsuario(`otro${n}@ejemplo.com`, 'ClaveCorrecta123', { rol: 'PRESTADOR' });
  const perfilOtro = crearPerfilDe(otro);
  const servicioOtro = crearServicioDe(perfilOtro, { titulo: 'Pintura de casas' });
  const r = await e.comoPrestador('POST', '/solicitudes', { servicio_id: servicioOtro.id, mensaje: 'Necesito pintar mi sala' });
  assert.equal(r.status, 201);
  assert.equal((await e.comoPrestador('GET', '/solicitudes?tipo=enviadas')).cuerpo.length, 1);
});

test('la lista pide un tipo valido', async (t) => {
  const e = await escenario(t);
  assert.equal((await e.comoCliente('GET', '/solicitudes')).status, 400);
  assert.equal((await e.comoCliente('GET', '/solicitudes?tipo=todas')).status, 400);
});
