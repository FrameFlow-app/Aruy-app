// Avisos publicos para la app (sin token): mantenimiento programado y version de la app.
// Se configuran con variables de entorno y se leen en cada peticion, sin reiniciar codigo.
const express = require('express');

const router = express.Router();

const fechaIso = (valor) => {
  if (!valor || !String(valor).trim()) return null;
  const d = new Date(String(valor).trim());
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
};

const entero = (valor) => {
  const t = String(valor ?? '').trim();
  return /^\d+$/.test(t) ? Number.parseInt(t, 10) : 0;
};

router.get('/', (req, res) => {
  const env = process.env;
  const mensaje = String(env.AVISO_MANTENIMIENTO || '').trim();
  const inicio = fechaIso(env.AVISO_MANTENIMIENTO_INICIO);
  const fin = fechaIso(env.AVISO_MANTENIMIENTO_FIN);
  const terminado = fin !== null && new Date(fin).getTime() < Date.now();

  res.set('Cache-Control', 'public, max-age=300');
  res.json({
    mantenimiento: mensaje && !terminado ? { activo: true, mensaje, inicio, fin } : null,
    version: {
      reciente: entero(env.APP_VERSION_RECIENTE),
      minima: entero(env.APP_VERSION_MINIMA),
      notas: String(env.APP_NOTAS_VERSION || '').trim().slice(0, 500),
    },
  });
});

module.exports = router;
