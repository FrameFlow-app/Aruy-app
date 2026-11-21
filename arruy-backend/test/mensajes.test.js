const test = require('node:test');
const assert = require('node:assert/strict');
const { encender, agregarUsuario, crearPerfilDe, crearServicioDe, tokenDe } = require('./ayuda');

let n = 0;
async function escenario(t) {
  const { servidor, url } = await encender();
  t.after(() => servidor.close());
  n += 1;
  const prestador = agregarUsuario(`chat-prestador${n}@ejemplo.com`, 'ClaveCorrecta123', { rol: 'PRESTADOR' });
  prestador.telefono = '0990000001';
  const servicio = crearServicioDe(crearPerfilDe(prestador), { titulo: 'Arreglo de tuberias' });
  const cliente = agregarUsuario(`chat-cliente${n}@ejemplo.com`, 'ClaveCorrecta123');
  cliente.telefono = '0990000002';
  const tercero = agregarUsuario(`chat-tercero${n}@ejemplo.com`, 'ClaveCorrecta123');
  const api = (usuario) => async (metodo, ruta, cuerpo) => {
    const r = await fetch(`${url}${ruta}`, {
      method: metodo,
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tokenDe(usuario)}` },
      body: cuerpo ? JSON.stringify(cuerpo) : undefined,
    });
    const texto = await r.text();
    return { status: r.status, cuerpo: texto ? JSON.parse(texto) : null };
  };
  const comoCliente = api(cliente);
  const sol = await comoCliente('POST', '/solicitudes', { servicio_id: servicio.id, mensaje: 'Hola, necesito arreglar una tuberia' });
  return { prestador, cliente, solicitud: sol.cuerpo, comoPrestador: api(prestador), comoCliente, comoTercero: api(tercero) };
}

test('un tercero no puede leer ni escribir en el chat', async (t) => {
  const e = await escenario(t);
  const ruta = `/solicitudes/${e.solicitud.id}/mensajes`;
  assert.equal((await e.comoTercero('GET', ruta)).status, 403);
  assert.equal((await e.comoTercero('POST', ruta, { texto: 'hola' })).status, 403);
  assert.equal((await e.comoTercero('POST', `/solicitudes/${e.solicitud.id}/whatsapp`)).status, 403);
  assert.equal((await e.comoCliente('GET', '/solicitudes/00000000-0000-4000-8000-999999999999/mensajes')).status, 404);
  assert.equal((await e.comoCliente('GET', '/solicitudes/no-es-id/mensajes')).status, 400);
});

test('se envian mensajes, se listan en orden y se marcan como leidos', async (t) => {
  const e = await escenario(t);
  const ruta = `/solicitudes/${e.solicitud.id}/mensajes`;
  const creado = await e.comoCliente('POST', ruta, { texto: '  Hola, puede el sabado?  ' });
  assert.equal(creado.status, 201);
  assert.equal(creado.cuerpo.texto, 'Hola, puede el sabado?');
  assert.equal(creado.cuerpo.tipo, 'TEXTO');
  assert.equal(creado.cuerpo.es_mio, true);
  assert.equal((await e.comoPrestador('POST', ruta, { texto: 'Si, a las 9' })).status, 201);

  // El prestador ve 1 sin leer en su bandeja y en el resumen
  const lista = await e.comoPrestador('GET', '/solicitudes?tipo=recibidas');
  assert.equal(lista.cuerpo[0].mensajes_sin_leer, 1);
  assert.equal(lista.cuerpo[0].ultimo_mensaje.texto, 'Si, a las 9');
  assert.equal((await e.comoPrestador('GET', '/solicitudes/resumen')).cuerpo.mensajes_sin_leer, 1);

  const vistos = await e.comoPrestador('GET', ruta);
  assert.equal(vistos.status, 200);
  assert.deepEqual(vistos.cuerpo.map((m) => [m.texto, m.es_mio]), [['Hola, puede el sabado?', false], ['Si, a las 9', true]]);
  assert.equal((await e.comoPrestador('GET', '/solicitudes/resumen')).cuerpo.mensajes_sin_leer, 0);
  assert.equal((await e.comoCliente('GET', '/solicitudes/resumen')).cuerpo.mensajes_sin_leer, 1);

  const nuevos = await e.comoCliente('GET', `${ruta}?desde=${encodeURIComponent(vistos.cuerpo[0].fecha_creacion)}`);
  assert.deepEqual(nuevos.cuerpo.map((m) => m.texto), ['Si, a las 9']);
  assert.equal((await e.comoCliente('GET', `${ruta}?desde=ayer`)).status, 400);
});

test('el texto del mensaje se valida', async (t) => {
  const e = await escenario(t);
  const ruta = `/solicitudes/${e.solicitud.id}/mensajes`;
  for (const texto of ['', '    ', 'x'.repeat(1001), 5, undefined]) {
    assert.equal((await e.comoCliente('POST', ruta, { texto })).status, 400, String(texto).slice(0, 10));
  }
  assert.equal((await e.comoCliente('POST', ruta, { texto: 'x'.repeat(1000) })).status, 201);
});

test('una solicitud cancelada o rechazada ya no admite mensajes, pero se puede leer', async (t) => {
  const e = await escenario(t);
  const ruta = `/solicitudes/${e.solicitud.id}/mensajes`;
  await e.comoCliente('POST', ruta, { texto: 'Hola' });
  assert.equal((await e.comoCliente('PATCH', `/solicitudes/${e.solicitud.id}`, { estado: 'CANCELADA' })).status, 200);
  assert.equal((await e.comoCliente('POST', ruta, { texto: 'Otra vez' })).status, 409);
  assert.equal((await e.comoPrestador('POST', `/solicitudes/${e.solicitud.id}/whatsapp`)).status, 409);
  assert.equal((await e.comoPrestador('GET', ruta)).cuerpo.length, 1);
  const e2 = await escenario(t);
  await e2.comoPrestador('PATCH', `/solicitudes/${e2.solicitud.id}`, { estado: 'RECHAZADA' });
  assert.equal((await e2.comoPrestador('POST', `/solicitudes/${e2.solicitud.id}/mensajes`, { texto: 'No puedo' })).status, 409);
});

test('el prestador comparte su WhatsApp una sola vez y el telefono se oculta hasta entonces', async (t) => {
  const e = await escenario(t);
  const ruta = `/solicitudes/${e.solicitud.id}/whatsapp`;
  await e.comoPrestador('PATCH', `/solicitudes/${e.solicitud.id}`, { estado: 'ACEPTADA' });

  let enviadas = (await e.comoCliente('GET', '/solicitudes?tipo=enviadas')).cuerpo[0];
  assert.equal(enviadas.whatsapp_compartido, false);
  assert.equal(enviadas.contraparte.telefono, null, 'aceptada no basta: el prestador debe compartir');

  assert.equal((await e.comoCliente('POST', ruta)).status, 403, 'el contratante no puede compartir por el prestador');

  const primero = await e.comoPrestador('POST', ruta);
  assert.equal(primero.status, 201);
  assert.equal(primero.cuerpo.tipo, 'WHATSAPP');
  assert.equal(primero.cuerpo.texto, '593990000001');
  const segundo = await e.comoPrestador('POST', ruta);
  assert.equal(segundo.status, 200);
  assert.equal(segundo.cuerpo.id, primero.cuerpo.id);
  const chat = await e.comoCliente('GET', `/solicitudes/${e.solicitud.id}/mensajes`);
  assert.equal(chat.cuerpo.length, 1);

  enviadas = (await e.comoCliente('GET', '/solicitudes?tipo=enviadas')).cuerpo[0];
  assert.equal(enviadas.whatsapp_compartido, true);
  assert.equal(enviadas.contraparte.telefono, '0990000001');
  assert.equal(enviadas.mensajes_sin_leer, 0, 'ya lo leyo al abrir el chat');
});

test('no se comparte el WhatsApp si el prestador no tiene un celular valido', async (t) => {
  const e = await escenario(t);
  e.prestador.telefono = '12345';
  assert.equal((await e.comoPrestador('POST', `/solicitudes/${e.solicitud.id}/whatsapp`)).status, 400);
});
