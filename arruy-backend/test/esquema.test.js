const test = require('node:test');
const assert = require('node:assert/strict');
const { asegurarEsquema, SENTENCIAS } = require('../esquema');

test('el esquema solo agrega cosas (IF NOT EXISTS) y nunca borra ni cambia datos', () => {
  for (const { nombre, sql } of SENTENCIAS) {
    assert.match(sql, /IF NOT EXISTS/i, nombre);
    // ("ON DELETE CASCADE" es una regla de la tabla nueva, no un borrado de datos)
    assert.doesNotMatch(sql, /\b(DROP|TRUNCATE)\b|\bDELETE\s+FROM\b|\bUPDATE\s+\w+\s+SET\b/i, nombre);
  }
});

test('ejecuta todas las preparaciones en orden', async () => {
  const hechas = [];
  const pool = { query: async (sql) => { hechas.push(sql); return { rows: [] }; } };
  const r = await asegurarEsquema(pool, { log: () => {} });
  assert.equal(hechas.length, SENTENCIAS.length);
  assert.ok(r.every((x) => x.ok));
});

test('si una preparacion falla (por permisos, por ejemplo) se anota y las demas siguen', async () => {
  const avisos = [];
  const pool = {
    query: async (sql) => {
      if (sql.includes('CREATE TABLE')) throw new Error('permiso denegado');
      return { rows: [] };
    },
  };
  const r = await asegurarEsquema(pool, { log: (m) => avisos.push(m) });
  assert.equal(r.filter((x) => !x.ok).length, 1);
  assert.equal(r.filter((x) => x.ok).length, SENTENCIAS.length - 1);
  assert.match(avisos[0], /permiso denegado/);
});
