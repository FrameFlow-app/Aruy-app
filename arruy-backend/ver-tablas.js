// Herramienta solo para ti: muestra las tablas y columnas de la base. NO la subas a Git.
const pool = require('./db');

pool.query(`
  SELECT table_name, column_name, data_type, is_nullable
  FROM information_schema.columns
  WHERE table_schema = 'public'
  ORDER BY table_name, ordinal_position
`)
  .then((r) => {
    console.table(r.rows);
    return pool.end();
  })
  .catch((e) => {
    console.error('Error:', e.message);
    process.exit(1);
  });
