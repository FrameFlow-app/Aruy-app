// Enciende el servidor. La aplicacion en si esta en app.js.
const app = require('./app');
const pool = require('./db');
const { registrarError } = require('./registro');

// Un error que nadie atrapo no debe pasar en silencio.
process.on('unhandledRejection', (motivo) => {
  console.error('Promesa rechazada sin atrapar:');
  registrarError(motivo instanceof Error ? motivo : new Error(String(motivo)));
});
process.on('uncaughtException', (err) => {
  console.error('Error no capturado; el proceso se reinicia para quedar en un estado limpio:');
  registrarError(err);
  setTimeout(() => process.exit(1), 200);
});

const PORT = process.env.PORT || 3000;
const servidor = app.listen(PORT, () => console.log(`Servidor en http://localhost:${PORT}`));

// Detras del proxy de Render, Node cerraba las conexiones inactivas (a los 5 s) antes que el proxy,
// y eso produce errores 502 intermitentes. Se mantienen abiertas mas tiempo que las del proxy.
servidor.keepAliveTimeout = 65 * 1000;
servidor.headersTimeout = 66 * 1000;

// Al desplegar, Render manda SIGTERM: se terminan las peticiones en curso y se cierra la base.
function apagar(senal) {
  console.log(`${senal}: cerrando el servidor...`);
  servidor.close(() => {
    pool.end().catch(() => {}).finally(() => process.exit(0));
  });
  setTimeout(() => process.exit(0), 10 * 1000).unref();
}
process.on('SIGTERM', () => apagar('SIGTERM'));
process.on('SIGINT', () => apagar('SIGINT'));
