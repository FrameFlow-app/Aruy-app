// Aplica en orden los scripts de db/ (000, 001, 002, 003...) a la base que indique DATABASE_URL.
// Todos son idempotentes (IF NOT EXISTS): se pueden correr varias veces y NO borran ni cambian datos.
// Uso (desde arruy-backend):  node db/aplicar.js
const fs = require('fs');
const path = require('path');
const pool = require('../db');

(async () => {
  const destino = await pool.query('SELECT current_database() AS base, current_user AS usuario');
  const host = new URL(process.env.DATABASE_URL).hostname; // solo el servidor; nunca se imprime la contrasena
  console.log(`Aplicando en: base="${destino.rows[0].base}" usuario="${destino.rows[0].usuario}" servidor="${host}"`);

  const archivos = fs.readdirSync(__dirname).filter((f) => /^\d+_.+\.sql$/.test(f)).sort();
  for (const f of archivos) {
    const sql = fs.readFileSync(path.join(__dirname, f), 'utf8');
    const cliente = await pool.connect();
    try {
      await cliente.query('BEGIN');
      await cliente.query(sql);
      await cliente.query('COMMIT');
      console.log(`  ok  ${f}`);
    } catch (e) {
      await cliente.query('ROLLBACK').catch(() => {});
      console.error(`  ERROR en ${f}: ${e.message}`);
      process.exitCode = 1;
      break;
    } finally {
      cliente.release();
    }
  }
  const tablas = await pool.query("SELECT count(*)::int AS n FROM information_schema.tables WHERE table_schema = 'public'");
  console.log(`Tablas en la base: ${tablas.rows[0].n} (se esperan 10)`);
  await pool.end();
})().catch((e) => { console.error('Error:', e.message); process.exit(1); });
