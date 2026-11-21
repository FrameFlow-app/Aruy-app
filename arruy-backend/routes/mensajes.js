// Chat de cada solicitud: solo lo ven y lo usan el contratante y el prestador de esa solicitud.
const express = require('express');
const pool = require('../db');
const { registrarError } = require('../registro');
const { exigirToken, exigirRol } = require('../middleware/auth');
const { esUuid, textoValido } = require('../validar');
const { aWhatsapp } = require('../whatsapp');

const router = express.Router();
router.use(exigirToken, exigirRol('CONTRATANTE', 'PRESTADOR'));

const TEXTO_MAX = 1000;
const ESTADOS_CERRADOS = ['RECHAZADA', 'CANCELADA'];
const COLUMNAS_MENSAJE = 'id, autor_id, texto, tipo, fecha_creacion';

const aMensaje = (f, yo) => ({
  id: String(f.id),
  autor_id: String(f.autor_id),
  es_mio: String(f.autor_id) === String(yo),
  texto: f.texto,
  tipo: f.tipo,
  fecha_creacion: f.fecha_creacion,
});

// Carga la solicitud y dice quien soy en ella. Responde 400/404/403 y devuelve null si no se puede seguir.
async function cargar(req, res) {
  if (!esUuid(req.params.id)) {
    res.status(400).json({ error: 'Id invalido' });
    return null;
  }
  const r = await pool.query(
    `SELECT sol.id, sol.estado, sol.whatsapp_compartido, sol.contratante_id, p.usuario_id AS prestador_usuario_id,
            u.telefono AS prestador_telefono
     FROM solicitudes sol
     LEFT JOIN perfiles_prestadores p ON p.id = sol.perfil_prestador_id
     LEFT JOIN usuarios u ON u.id = p.usuario_id
     WHERE sol.id = $1`,
    [req.params.id]
  );
  const f = r.rows[0];
  if (!f) {
    res.status(404).json({ error: 'Solicitud no encontrada' });
    return null;
  }
  const soyPrestador = String(f.prestador_usuario_id) === String(req.usuario.id);
  const soyContratante = String(f.contratante_id) === String(req.usuario.id);
  if (!soyPrestador && !soyContratante) {
    res.status(403).json({ error: 'No tienes permiso para ver esta conversacion' });
    return null;
  }
  return { ...f, soyPrestador };
}

// Mensajes de la conversacion (?desde=<fecha ISO> trae solo los nuevos). Marca como leidos los de la otra persona.
router.get('/:id/mensajes', async (req, res) => {
  let desde = null;
  if (req.query.desde != null) {
    const fecha = new Date(String(req.query.desde));
    if (Number.isNaN(fecha.getTime())) return res.status(400).json({ error: 'desde debe ser una fecha valida (ISO 8601)' });
    desde = fecha.toISOString();
  }
  try {
    const sol = await cargar(req, res);
    if (!sol) return;
    const r = await pool.query(
      `SELECT ${COLUMNAS_MENSAJE} FROM mensajes
       WHERE solicitud_id = $1 AND ($2::timestamptz IS NULL OR fecha_creacion > $2::timestamptz)
       ORDER BY fecha_creacion DESC LIMIT 300`,
      [req.params.id, desde]
    );
    await pool.query('UPDATE mensajes SET leido = true WHERE solicitud_id = $1 AND autor_id <> $2 AND leido = false', [
      req.params.id,
      req.usuario.id,
    ]);
    res.json(r.rows.reverse().map((f) => aMensaje(f, req.usuario.id)));
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al consultar los mensajes' });
  }
});

// Enviar un mensaje de texto
router.post('/:id/mensajes', async (req, res) => {
  const { texto } = req.body || {};
  if (!textoValido(texto, TEXTO_MAX)) {
    return res.status(400).json({ error: `texto debe tener entre 1 y ${TEXTO_MAX} caracteres` });
  }
  try {
    const sol = await cargar(req, res);
    if (!sol) return;
    if (ESTADOS_CERRADOS.includes(sol.estado)) {
      return res.status(409).json({ error: `La solicitud esta ${sol.estado} y ya no admite mensajes` });
    }
    const r = await pool.query(
      `INSERT INTO mensajes (solicitud_id, autor_id, texto, tipo) VALUES ($1, $2, $3, 'TEXTO') RETURNING ${COLUMNAS_MENSAJE}`,
      [req.params.id, req.usuario.id, texto.trim()]
    );
    res.status(201).json(aMensaje(r.rows[0], req.usuario.id));
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al enviar el mensaje' });
  }
});

// El prestador comparte su WhatsApp: queda en el chat y el contratante ya puede ver su telefono. Es idempotente.
router.post('/:id/whatsapp', async (req, res) => {
  try {
    const sol = await cargar(req, res);
    if (!sol) return;
    if (!sol.soyPrestador) return res.status(403).json({ error: 'Solo el prestador puede compartir su WhatsApp' });
    if (sol.whatsapp_compartido) {
      const previo = await pool.query(
        `SELECT ${COLUMNAS_MENSAJE} FROM mensajes WHERE solicitud_id = $1 AND tipo = 'WHATSAPP' ORDER BY fecha_creacion ASC LIMIT 1`,
        [req.params.id]
      );
      if (previo.rows[0]) return res.status(200).json(aMensaje(previo.rows[0], req.usuario.id));
    }
    if (ESTADOS_CERRADOS.includes(sol.estado)) {
      return res.status(409).json({ error: `La solicitud esta ${sol.estado} y ya no admite mensajes` });
    }
    const numero = aWhatsapp(sol.prestador_telefono);
    if (!numero) return res.status(400).json({ error: 'Tu telefono no es un celular valido para WhatsApp. Actualizalo en tu perfil.' });
    const r = await pool.query(
      `INSERT INTO mensajes (solicitud_id, autor_id, texto, tipo) VALUES ($1, $2, $3, 'WHATSAPP') RETURNING ${COLUMNAS_MENSAJE}`,
      [req.params.id, req.usuario.id, numero]
    );
    await pool.query('UPDATE solicitudes SET whatsapp_compartido = true WHERE id = $1', [req.params.id]);
    res.status(201).json(aMensaje(r.rows[0], req.usuario.id));
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al compartir el WhatsApp' });
  }
});

module.exports = router;
