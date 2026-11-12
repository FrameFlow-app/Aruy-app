// Ayuda para las pruebas: una base de datos falsa (en memoria) y un servidor en un puerto libre.
// Las pruebas NO tocan Neon ni Supabase.
process.env.JWT_SECRET = 'clave-de-prueba-solo-para-tests';
process.env.DATABASE_URL = 'postgresql://x:y@localhost/z';
process.env.TRUST_PROXY = '1'; // para poder simular redes distintas con X-Forwarded-For

const bcrypt = require('bcryptjs');

const usuarios = new Map(); // correo -> usuario
const perfiles = []; // { id, usuario_id }
const calificaciones = []; // { contratante_id, perfil_prestador_id }
let contador = 0;

function agregarUsuario(email, password, { rol = 'CONTRATANTE' } = {}) {
  const usuario = {
    id: `00000000-0000-4000-8000-${String(++contador).padStart(12, '0')}`,
    nombre: 'Prueba',
    telefono: '0991234567',
    rol,
    estado_verificacion: 'NO_SOLICITADA',
    foto_perfil: null,
    activo: true,
    password_hash: bcrypt.hashSync(password, 4),
  };
  usuarios.set(email, usuario);
  return usuario;
}

function crearPerfilDe(usuario) {
  const perfil = { id: `00000000-0000-4000-9000-${String(++contador).padStart(12, '0')}`, usuario_id: usuario.id };
  perfiles.push(perfil);
  return perfil;
}

const porId = (id) => [...usuarios.values()].find((u) => u.id === id);
const sinHash = (u) => {
  const { password_hash: _oculto, ...resto } = u;
  return resto;
};

async function consulta(sql, parametros = []) {
  sql = sql.replace(/\s+/g, ' ').trim();
  const [p0, p1, p2, p3, p4] = parametros;
  if (['BEGIN', 'COMMIT', 'ROLLBACK'].includes(sql)) return { rows: [] };
  if (sql.includes('password_hash FROM usuarios WHERE lower(email)')) {
    const u = usuarios.get(p0);
    return { rows: u ? [{ ...u }] : [] };
  }
  if (sql.startsWith('SELECT id FROM perfiles_prestadores WHERE usuario_id')) {
    return { rows: perfiles.filter((x) => x.usuario_id === p0).map((x) => ({ id: x.id })) };
  }
  if (sql.includes('FROM usuarios WHERE id = $1 FOR UPDATE')) {
    const u = porId(p0);
    return { rows: u ? [sinHash(u)] : [] };
  }
  if (sql.startsWith('INSERT INTO perfiles_prestadores')) {
    perfiles.push({ id: p0, usuario_id: p1 });
    return { rows: [] };
  }
  if (sql.startsWith("UPDATE usuarios SET rol = 'PRESTADOR'")) {
    const u = porId(p0);
    u.rol = 'PRESTADOR';
    return { rows: [sinHash(u)] };
  }
  if (sql.startsWith('SELECT usuario_id FROM perfiles_prestadores WHERE id')) {
    const x = perfiles.find((f) => f.id === p0);
    return { rows: x ? [{ usuario_id: x.usuario_id }] : [] };
  }
  if (sql.startsWith('INSERT INTO calificaciones')) {
    if (calificaciones.some((c) => c.contratante_id === p1 && c.perfil_prestador_id === p2)) {
      throw Object.assign(new Error('duplicado'), { code: '23505' });
    }
    calificaciones.push({ contratante_id: p1, perfil_prestador_id: p2 });
    return { rows: [{ id: p0, perfil_prestador_id: p2, puntuacion: p3, comentario: p4, fecha_creacion: new Date() }] };
  }
  throw new Error('SQL no previsto en las pruebas: ' + sql.slice(0, 70));
}

const falsa = { query: consulta, connect: async () => ({ query: consulta, release() {} }), on() {}, end: async () => {} };
require.cache[require.resolve('../db')] = { id: 'db', filename: require.resolve('../db'), loaded: true, exports: falsa };

const app = require('../app');

async function encender() {
  const servidor = app.listen(0);
  await new Promise((listo) => servidor.once('listening', listo));
  return { servidor, url: `http://127.0.0.1:${servidor.address().port}` };
}

const { firmarToken } = require('../middleware/auth');
const tokenDe = (usuario) => firmarToken(usuario);

module.exports = { encender, agregarUsuario, crearPerfilDe, tokenDe };
