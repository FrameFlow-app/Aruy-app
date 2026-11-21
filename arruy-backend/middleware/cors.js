// CORS para que la PWA (otro origen) pueda llamar a la API. La app Android no manda Origin y no se ve afectada.
// Lista blanca con comparacion EXACTA del origen; se pueden sumar mas con CORS_ORIGENES (separados por coma).
// No se envia Access-Control-Allow-Credentials: la sesion va en el header Authorization, no en cookies.
const BASE = ['https://frameflow-app.github.io', 'http://localhost:5173'];

// Se lee en cada peticion para poder cambiar la variable sin reiniciar (y para las pruebas)
function origenesPermitidos() {
  const extra = (process.env.CORS_ORIGENES || '')
    .split(',')
    .map((o) => o.trim().replace(/\/+$/, ''))
    .filter(Boolean);
  return new Set([...BASE, ...extra]);
}

function cors(req, res, next) {
  res.vary('Origin');
  const origen = req.headers.origin;
  if (!origen || !origenesPermitidos().has(origen)) return next(); // sin cabeceras CORS: el navegador lo bloquea
  res.set({
    'Access-Control-Allow-Origin': origen,
    'Access-Control-Allow-Methods': 'GET,POST,PATCH,DELETE,OPTIONS',
    'Access-Control-Allow-Headers': 'Authorization, Content-Type',
    'Access-Control-Max-Age': '600',
  });
  // Preflight: se responde aqui, antes de los limites de velocidad y la autenticacion
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
}

module.exports = cors;
