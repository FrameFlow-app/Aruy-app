// Herramienta solo para ti: muestra las reglas (llaves foraneas, checks, unicos) de la base. NO la subas a Git.
const pool = require('./db');

pool.query(`
  SELECT conrelid::regclass::text AS tabla,
         conname AS regla,
         CASE contype WHEN 'f' THEN 'llave foranea' WHEN 'c' THEN 'check' WHEN 'u' THEN 'unico' END AS tipo,
         pg_get_constraintdef(oid) AS definicion
  FROM pg_constraint
  WHERE connamespace = 'public'::regnamespace AND contype IN ('f', 'c', 'u')
  ORDER BY 1, 2
`)
  .then((r) => {
    console.table(r.rows);
    return pool.end();
  })
  .catch((e) => {
    console.error('Error:', e.message);
    process.exit(1);
  });
