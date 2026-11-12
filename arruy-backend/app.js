// La aplicacion Express (sin encender el servidor): asi las pruebas la pueden usar sin abrir un puerto fijo.
// El servidor se enciende en index.js.
require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const jwt = require('jsonwebtoken');
const { rateLimit, ipKeyGenerator } = require('express-rate-limit');
const { registrarError } = require('./registro');

if (!process.env.JWT_SECRET) {
  throw new Error('Falta JWT_SECRET en el archivo .env');
}

const app = express();
// Detras de un proxy (Render) todos llegan con la IP del proxy: se confia en 1 salto para ver la IP real.
// Render define RENDER automaticamente; en otro proxy, definir TRUST_PROXY=1.
if (process.env.RENDER || process.env.TRUST_PROXY) app.set('trust proxy', 1);
app.use(helmet());
app.use(express.json({ limit: '100kb' }));

// ---------------- Limites de velocidad ----------------
// Pensados para que NADIE bloquee a los demas: en la universidad o con datos moviles muchas
// personas salen a internet con la misma IP, asi que no se cuenta solo por IP.
const VENTANA = 15 * 60 * 1000;
const comunes = { windowMs: VENTANA, standardHeaders: 'draft-7', legacyHeaders: false };
const porIp = (req) => ipKeyGenerator(req.ip);

// Quien hace la peticion: si trae un token VALIDO, su usuario; si no, su IP.
// (Se verifica la firma: un token falso no sirve para escapar del limite.)
function quien(req) {
  if (!req.claveLimite) {
    req.claveLimite = `ip:${porIp(req)}`;
    const [tipo, token] = (req.headers.authorization || '').split(' ');
    if (tipo === 'Bearer' && token) {
      try {
        const datos = jwt.verify(token, process.env.JWT_SECRET);
        if (datos.sub) req.claveLimite = `u:${datos.sub}`;
      } catch {
        // token invalido: se cuenta por IP
      }
    }
  }
  return req.claveLimite;
}
const esUsuario = (req) => quien(req).startsWith('u:');

// General: 600 peticiones por usuario, o 2000 por IP para quien no ha iniciado sesion (invitados)
app.use(
  rateLimit({
    ...comunes,
    limit: (req) => (esUsuario(req) ? 600 : 2000),
    keyGenerator: (req) => quien(req),
    message: { error: 'Demasiadas peticiones en poco tiempo, intenta de nuevo en unos minutos' },
  })
);

const correoDe = (req) => String((req.body && req.body.email) || '').trim().toLowerCase().slice(0, 254);

// Inicio de sesion: solo cuentan los INTENTOS FALLIDOS, y por cuenta + IP. Asi, equivocarse con una
// cuenta no bloquea a las demas, y quien entra bien muchas veces nunca se bloquea.
const limiteLoginCuenta = rateLimit({
  ...comunes,
  limit: 10,
  skipSuccessfulRequests: true,
  keyGenerator: (req) => `${porIp(req)}|${correoDe(req)}`,
  message: { error: 'Demasiados intentos fallidos con esa cuenta. Espera unos minutos e intenta de nuevo.' },
});
const limiteRegistro = rateLimit({
  ...comunes,
  limit: 30,
  keyGenerator: (req) => porIp(req),
  message: { error: 'Demasiados registros desde esta red. Intenta de nuevo en unos minutos.' },
});
// Techo general para /auth por IP (frena probar muchas cuentas distintas)
const limiteAuthPorIp = rateLimit({
  ...comunes,
  limit: 300,
  keyGenerator: (req) => porIp(req),
  message: { error: 'Demasiados intentos desde esta red. Intenta de nuevo en unos minutos.' },
});
// Subir imagenes: cada una puede pesar hasta 5 MB y se procesa con sharp
const limiteImagenes = rateLimit({
  ...comunes,
  limit: 60,
  keyGenerator: (req) => quien(req),
  message: { error: 'Demasiadas imagenes en poco tiempo, intenta mas tarde' },
});

app.get('/health', (req, res) => res.json({ estado: 'ok' }));
app.post('/auth/login', limiteLoginCuenta, limiteAuthPorIp);
app.post('/auth/registro', limiteRegistro, limiteAuthPorIp);
app.use('/auth', require('./routes/auth'));
app.post('/servicios/:id/fotos', limiteImagenes);
app.post('/usuarios/yo/foto', limiteImagenes);
app.use('/servicios', require('./routes/servicios'));
app.use('/usuarios', require('./routes/usuarios'));
app.use('/perfiles', require('./routes/perfiles'));
app.use('/fotos', require('./routes/fotos'));
app.use('/calificaciones', require('./routes/calificaciones'));
app.use('/suscripciones', require('./routes/suscripciones'));

// Documentación interactiva (Swagger) en /docs
try {
  const swaggerUi = require('swagger-ui-express');
  const especificacion = require('./swagger');
  app.use(
    '/docs',
    (req, res, next) => {
      // La pagina de Swagger usa scripts propios que el CSP de helmet bloquearia
      res.removeHeader('Content-Security-Policy');
      next();
    },
    swaggerUi.serve,
    swaggerUi.setup(especificacion, { customSiteTitle: 'Aruy API' })
  );
  app.get('/docs.json', (req, res) => res.json(especificacion));
} catch (e) {
  if (e.code !== 'MODULE_NOT_FOUND') throw e;
  console.warn('Swagger desactivado: ejecuta "npm install swagger-ui-express" en arruy-backend');
}

app.use((req, res) => res.status(404).json({ error: 'Ruta no encontrada' }));

// Errores que no atrapo ninguna ruta: siempre JSON, nunca el detalle interno
app.use((err, req, res, next) => {
  if (res.headersSent) return next(err);
  if (err.type === 'entity.too.large') return res.status(413).json({ error: 'El cuerpo de la peticion es demasiado grande' });
  if (err.type === 'entity.parse.failed') return res.status(400).json({ error: 'El cuerpo no es un JSON valido' });
  registrarError(err);
  res.status(500).json({ error: 'Error interno del servidor' });
});

module.exports = app;
