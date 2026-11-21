const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { registrarError } = require('../registro');
const { exigirToken, exigirRol } = require('../middleware/auth');
const { esUuid, textoValido } = require('../validar');
const { urlPublica } = require('../almacenamiento');

const router = express.Router();
router.use(exigirToken, exigirRol('CONTRATANTE', 'PRESTADOR'));

const MENSAJE_MIN = 5;
const MENSAJE_MAX = 500;
const RESPUESTA_MAX = 300;
const MAX_PENDIENTES = 15;

// Quien puede pasar una solicitud de un estado a otro. El prestador responde; quien la envio la puede cancelar.
const TRANSICIONES = {
  prestador: { PENDIENTE: ['ACEPTADA', 'RECHAZADA'], ACEPTADA: ['COMPLETADA', 'CANCELADA'] },
  contratante: { PENDIENTE: ['CANCELADA'], ACEPTADA: ['CANCELADA'] },
};

// $2 es siempre el usuario que consulta (para contar los mensajes de la otra persona que no ha leido)
const COLUMNAS_LISTA = `sol.id, sol.servicio_id, sol.estado, sol.mensaje, sol.respuesta, sol.fecha_creacion,
  sol.fecha_actualizacion, sol.visto_prestador, sol.visto_contratante, sol.whatsapp_compartido, s.titulo AS servicio_titulo,
  (SELECT COUNT(*) FROM mensajes m WHERE m.solicitud_id = sol.id AND m.autor_id <> $2 AND m.leido = false)::int AS mensajes_sin_leer,
  (SELECT m.texto FROM mensajes m WHERE m.solicitud_id = sol.id ORDER BY m.fecha_creacion DESC LIMIT 1) AS ultimo_mensaje_texto,
  (SELECT m.fecha_creacion FROM mensajes m WHERE m.solicitud_id = sol.id ORDER BY m.fecha_creacion DESC LIMIT 1) AS ultimo_mensaje_fecha`;

function aSolicitud(f, tipo) {
  const compartido = f.whatsapp_compartido === true;
  return {
    id: String(f.id),
    servicio_id: f.servicio_id ? String(f.servicio_id) : null,
    servicio_titulo: f.servicio_titulo ?? null,
    estado: f.estado,
    mensaje: f.mensaje,
    respuesta: f.respuesta ?? null,
    fecha_creacion: f.fecha_creacion,
    fecha_actualizacion: f.fecha_actualizacion,
    sin_ver: tipo === 'recibidas' ? f.visto_prestador === false : f.visto_contratante === false,
    whatsapp_compartido: compartido,
    mensajes_sin_leer: Number(f.mensajes_sin_leer || 0),
    ultimo_mensaje: f.ultimo_mensaje_texto ? { texto: f.ultimo_mensaje_texto, fecha: f.ultimo_mensaje_fecha } : null,
    contraparte: {
      nombre: f.contraparte_nombre ?? 'Cuenta eliminada',
      foto_perfil: urlPublica(f.contraparte_foto ?? null),
      // El telefono del prestador solo le llega al contratante cuando el prestador comparte su WhatsApp en el chat.
      // El del contratante le llega al prestador cuando la solicitud esta ACEPTADA.
      telefono: (tipo === 'enviadas' ? compartido : f.estado === 'ACEPTADA') ? (f.contraparte_telefono ?? null) : null,
    },
  };
}

const COLUMNAS_FILA = 'id, servicio_id, estado, mensaje, respuesta, fecha_creacion, fecha_actualizacion, visto_prestador, visto_contratante, whatsapp_compartido';

// Enviar una solicitud: cualquier persona con cuenta, menos para su propio servicio
router.post('/', async (req, res) => {
  const { servicio_id, mensaje } = req.body || {};
  if (!esUuid(servicio_id)) return res.status(400).json({ error: 'servicio_id es obligatorio y debe ser un id valido' });
  if (!textoValido(mensaje, MENSAJE_MAX) || mensaje.trim().length < MENSAJE_MIN) {
    return res.status(400).json({ error: `mensaje debe tener entre ${MENSAJE_MIN} y ${MENSAJE_MAX} caracteres` });
  }
  try {
    const s = await pool.query(
      `SELECT s.id, s.titulo, s.disponible, s.prestador_id, p.usuario_id, p.disponible AS perfil_disponible,
              u.nombre AS prestador_nombre, u.foto_perfil AS prestador_foto
       FROM servicios s
       JOIN perfiles_prestadores p ON p.id = s.prestador_id
       JOIN usuarios u ON u.id = p.usuario_id
       WHERE s.id = $1`,
      [servicio_id]
    );
    const f = s.rows[0];
    if (!f) return res.status(404).json({ error: 'Servicio no encontrado' });
    if (String(f.usuario_id) === String(req.usuario.id)) {
      return res.status(403).json({ error: 'No puedes enviar una solicitud a tu propio servicio' });
    }
    if (f.disponible === false || f.perfil_disponible === false) {
      return res.status(409).json({ error: 'Este servicio no esta recibiendo solicitudes ahora' });
    }
    const pendientes = await pool.query(
      "SELECT COUNT(*) AS n FROM solicitudes WHERE contratante_id = $1 AND estado = 'PENDIENTE'",
      [req.usuario.id]
    );
    if (Number(pendientes.rows[0].n) >= MAX_PENDIENTES) {
      return res.status(409).json({ error: 'Tienes muchas solicitudes pendientes. Espera respuestas o cancela alguna' });
    }
    const r = await pool.query(
      `INSERT INTO solicitudes (id, servicio_id, perfil_prestador_id, contratante_id, mensaje)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING ${COLUMNAS_FILA}`,
      [crypto.randomUUID(), servicio_id, f.prestador_id, req.usuario.id, mensaje.trim()]
    );
    res.status(201).json(
      aSolicitud({ ...r.rows[0], servicio_titulo: f.titulo, contraparte_nombre: f.prestador_nombre, contraparte_foto: f.prestador_foto }, 'enviadas')
    );
  } catch (e) {
    if (e.code === '23505') return res.status(409).json({ error: 'Ya enviaste una solicitud pendiente para este servicio' });
    registrarError(e);
    res.status(500).json({ error: 'Error al enviar la solicitud' });
  }
});

// Resumen para el numerito y las notificaciones: cuantas solicitudes nuevas tengo
router.get('/resumen', async (req, res) => {
  try {
    const perfil = await pool.query('SELECT id FROM perfiles_prestadores WHERE usuario_id = $1', [req.usuario.id]);
    const perfilId = perfil.rows[0] ? perfil.rows[0].id : null;
    const r = await pool.query(
      `SELECT
         COUNT(*) FILTER (WHERE perfil_prestador_id = $2 AND visto_prestador = false) AS recibidas_nuevas,
         COUNT(*) FILTER (WHERE perfil_prestador_id = $2 AND estado = 'PENDIENTE') AS recibidas_pendientes,
         COUNT(*) FILTER (WHERE contratante_id = $1 AND visto_contratante = false) AS enviadas_novedades
       FROM solicitudes WHERE contratante_id = $1 OR perfil_prestador_id = $2`,
      [req.usuario.id, perfilId]
    );
    const x = r.rows[0] || {};
    // Mensajes de la otra persona que no he leido (si la tabla aun no existe, no se tumba el resumen)
    let mensajesSinLeer = 0;
    try {
      const m = await pool.query(
        `SELECT COUNT(*)::int AS n FROM mensajes m JOIN solicitudes sol ON sol.id = m.solicitud_id
         WHERE m.autor_id <> $1 AND m.leido = false AND (sol.contratante_id = $1 OR sol.perfil_prestador_id = $2)`,
        [req.usuario.id, perfilId]
      );
      mensajesSinLeer = Number(m.rows[0] ? m.rows[0].n : 0);
    } catch (e) {
      registrarError(e);
    }
    res.json({
      recibidas_nuevas: Number(x.recibidas_nuevas || 0),
      recibidas_pendientes: Number(x.recibidas_pendientes || 0),
      enviadas_novedades: Number(x.enviadas_novedades || 0),
      mensajes_sin_leer: mensajesSinLeer,
    });
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al consultar el resumen de solicitudes' });
  }
});

// Marcar como vistas las solicitudes de una bandeja (al abrirla)
router.post('/vistas', async (req, res) => {
  const { tipo } = req.body || {};
  if (tipo !== 'recibidas' && tipo !== 'enviadas') {
    return res.status(400).json({ error: 'tipo debe ser enviadas o recibidas' });
  }
  try {
    const r =
      tipo === 'enviadas'
        ? await pool.query('UPDATE solicitudes SET visto_contratante = true WHERE contratante_id = $1 AND visto_contratante = false', [req.usuario.id])
        : await pool.query(
            `UPDATE solicitudes SET visto_prestador = true
             WHERE perfil_prestador_id IN (SELECT id FROM perfiles_prestadores WHERE usuario_id = $1) AND visto_prestador = false`,
            [req.usuario.id]
          );
    res.json({ marcadas: r.rowCount ?? 0 });
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al marcar las solicitudes' });
  }
});

// Mis solicitudes: ?tipo=enviadas (las que yo hice) o ?tipo=recibidas (las que me hicieron como prestador)
router.get('/', async (req, res) => {
  const { tipo } = req.query;
  if (tipo !== 'recibidas' && tipo !== 'enviadas') {
    return res.status(400).json({ error: 'tipo debe ser enviadas o recibidas' });
  }
  try {
    let r;
    if (tipo === 'enviadas') {
      r = await pool.query(
        `SELECT ${COLUMNAS_LISTA}, u.nombre AS contraparte_nombre, u.foto_perfil AS contraparte_foto, u.telefono AS contraparte_telefono
         FROM solicitudes sol
         LEFT JOIN servicios s ON s.id = sol.servicio_id
         LEFT JOIN perfiles_prestadores p ON p.id = sol.perfil_prestador_id
         LEFT JOIN usuarios u ON u.id = p.usuario_id
         WHERE sol.contratante_id = $1
         ORDER BY sol.fecha_creacion DESC LIMIT 100`,
        [req.usuario.id, req.usuario.id]
      );
    } else {
      const perfil = await pool.query('SELECT id FROM perfiles_prestadores WHERE usuario_id = $1', [req.usuario.id]);
      if (!perfil.rows[0]) return res.json([]);
      r = await pool.query(
        `SELECT ${COLUMNAS_LISTA}, u.nombre AS contraparte_nombre, u.foto_perfil AS contraparte_foto, u.telefono AS contraparte_telefono
         FROM solicitudes sol
         LEFT JOIN servicios s ON s.id = sol.servicio_id
         LEFT JOIN usuarios u ON u.id = sol.contratante_id
         WHERE sol.perfil_prestador_id = $1
         ORDER BY sol.fecha_creacion DESC LIMIT 100`,
        [perfil.rows[0].id, req.usuario.id]
      );
    }
    res.json(r.rows.map((f) => aSolicitud(f, tipo)));
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al consultar las solicitudes' });
  }
});

// Cambiar el estado: el prestador acepta, rechaza o completa; quien la envio la cancela
router.patch('/:id', async (req, res) => {
  if (!esUuid(req.params.id)) return res.status(400).json({ error: 'Id invalido' });
  const { estado, respuesta } = req.body || {};
  if (!['ACEPTADA', 'RECHAZADA', 'CANCELADA', 'COMPLETADA'].includes(estado)) {
    return res.status(400).json({ error: 'estado debe ser ACEPTADA, RECHAZADA, CANCELADA o COMPLETADA' });
  }
  if (respuesta != null && !textoValido(respuesta, RESPUESTA_MAX)) {
    return res.status(400).json({ error: `respuesta invalida (maximo ${RESPUESTA_MAX} caracteres)` });
  }
  try {
    const r = await pool.query(
      `SELECT sol.id, sol.estado, sol.contratante_id, p.usuario_id AS prestador_usuario_id
       FROM solicitudes sol
       LEFT JOIN perfiles_prestadores p ON p.id = sol.perfil_prestador_id
       WHERE sol.id = $1`,
      [req.params.id]
    );
    const f = r.rows[0];
    const soyPrestador = Boolean(f) && String(f.prestador_usuario_id) === String(req.usuario.id);
    const soyContratante = Boolean(f) && String(f.contratante_id) === String(req.usuario.id);
    if (!soyPrestador && !soyContratante) return res.status(404).json({ error: 'Solicitud no encontrada' });

    const permitidos = TRANSICIONES[soyPrestador ? 'prestador' : 'contratante'][f.estado] || [];
    if (!permitidos.includes(estado)) {
      return res.status(409).json({ error: `Una solicitud ${f.estado} no puede pasar a ${estado}` });
    }
    // Quien NO hizo el cambio es quien debe enterarse
    const u = await pool.query(
      `UPDATE solicitudes
       SET estado = $2, respuesta = COALESCE($3, respuesta), visto_contratante = $4, visto_prestador = $5, fecha_actualizacion = NOW()
       WHERE id = $1
       RETURNING ${COLUMNAS_FILA}`,
      [req.params.id, estado, respuesta == null ? null : respuesta.trim(), !soyPrestador, soyPrestador]
    );
    res.json(aSolicitud({ ...u.rows[0], contraparte_nombre: null }, soyPrestador ? 'recibidas' : 'enviadas'));
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al cambiar la solicitud' });
  }
});

module.exports = router;
