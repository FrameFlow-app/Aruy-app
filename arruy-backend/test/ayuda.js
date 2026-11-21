// Ayuda para las pruebas: una base de datos falsa (en memoria) y un servidor en un puerto libre.
// Las pruebas NO tocan Neon ni Supabase.
process.env.JWT_SECRET = 'clave-de-prueba-solo-para-tests';
process.env.DATABASE_URL = 'postgresql://x:y@localhost/z';
process.env.TRUST_PROXY = '1'; // para poder simular redes distintas con X-Forwarded-For

const bcrypt = require('bcryptjs');

const usuarios = new Map(); // correo -> usuario
const perfiles = []; // { id, usuario_id }
const servicios = []; // { id, prestador_id, titulo, ... }
const calificaciones = []; // { contratante_id, perfil_prestador_id }
const solicitudes = []; // filas de la tabla solicitudes
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

function crearServicioDe(perfil, { titulo = 'Servicio de prueba', disponible = true } = {}) {
  const servicio = { id: `00000000-0000-4000-a000-${String(++contador).padStart(12, '0')}`, prestador_id: perfil.id, titulo, disponible };
  servicios.push(servicio);
  return servicio;
}

const porId = (id) => [...usuarios.values()].find((u) => u.id === id);
const sinHash = (u) => {
  const { password_hash: _oculto, ...resto } = u;
  return resto;
};

async function consulta(sql, parametros = []) {
  sql = sql.replace(/\s+/g, ' ').trim();
  const [p0, p1, p2, p3, p4, p5, p6, p7] = parametros;
  if (['BEGIN', 'COMMIT', 'ROLLBACK'].includes(sql)) return { rows: [] };
  if (sql.includes('password_hash FROM usuarios WHERE lower(email)')) {
    const u = usuarios.get(p0);
    return { rows: u ? [{ ...u }] : [] };
  }
  if (sql.startsWith('SELECT 1 FROM information_schema.columns')) {
    // AYUDA_SIN_COLUMNA_FECHA=1 simula una base a la que todavia no se le aplico db/001_fecha_nacimiento.sql
    return { rows: process.env.AYUDA_SIN_COLUMNA_FECHA ? [] : [{ '?column?': 1 }] };
  }
  if (sql.startsWith('SELECT id, nombre') && sql.endsWith('FROM usuarios WHERE id = $1')) {
    const u = porId(p0);
    return { rows: u ? [sinHash(u)] : [] };
  }
  if (sql.startsWith('SELECT 1 FROM usuarios WHERE lower(email)')) {
    return { rows: usuarios.has(p0) ? [{ '?column?': 1 }] : [] };
  }
  if (sql.startsWith('SELECT 1 FROM usuarios WHERE telefono')) {
    return { rows: [...usuarios.values()].some((u) => u.telefono === p0) ? [{ '?column?': 1 }] : [] };
  }
  if (sql.startsWith('INSERT INTO usuarios')) {
    const nuevo = {
      id: p0, nombre: p1, telefono: p2, rol: p3, estado_verificacion: 'NO_SOLICITADA', foto_perfil: p4, activo: true, password_hash: p6,
    };
    if (sql.includes('fecha_nacimiento')) nuevo.fecha_nacimiento = p7;
    usuarios.set(p5, nuevo);
    return { rows: [sinHash(nuevo)] };
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
  // ---------------- solicitudes ----------------
  const perfilPorId = (id) => perfiles.find((x) => x.id === id);
  if (sql.startsWith('SELECT s.id, s.titulo, s.disponible')) {
    const s = servicios.find((x) => x.id === p0);
    const perfil = s && perfilPorId(s.prestador_id);
    const dueno = perfil && porId(perfil.usuario_id);
    return {
      rows: s && dueno
        ? [{ id: s.id, titulo: s.titulo, disponible: s.disponible, prestador_id: s.prestador_id, usuario_id: perfil.usuario_id, perfil_disponible: perfil.disponible ?? true, prestador_nombre: dueno.nombre, prestador_foto: null }]
        : [],
    };
  }
  if (sql.startsWith('SELECT COUNT(*) AS n FROM solicitudes')) {
    return { rows: [{ n: String(solicitudes.filter((x) => x.contratante_id === p0 && x.estado === 'PENDIENTE').length) }] };
  }
  if (sql.startsWith('INSERT INTO solicitudes')) {
    if (solicitudes.some((x) => x.contratante_id === p3 && x.servicio_id === p1 && x.estado === 'PENDIENTE')) {
      throw Object.assign(new Error('duplicado'), { code: '23505' });
    }
    const fila = {
      id: p0, servicio_id: p1, perfil_prestador_id: p2, contratante_id: p3, mensaje: p4, estado: 'PENDIENTE', respuesta: null,
      visto_prestador: false, visto_contratante: true, fecha_creacion: new Date(), fecha_actualizacion: new Date(),
    };
    solicitudes.push(fila);
    return { rows: [{ ...fila }] };
  }
  if (sql.startsWith('SELECT sol.id, sol.servicio_id')) {
    const enviadas = sql.includes('WHERE sol.contratante_id = $1');
    const mias = solicitudes.filter((x) => (enviadas ? x.contratante_id === p0 : x.perfil_prestador_id === p0));
    return {
      rows: mias
        .map((x) => {
          const otra = enviadas ? porId((perfilPorId(x.perfil_prestador_id) || {}).usuario_id) : porId(x.contratante_id);
          const s = servicios.find((y) => y.id === x.servicio_id);
          return {
            ...x, servicio_titulo: s ? s.titulo : null,
            contraparte_nombre: otra ? otra.nombre : null, contraparte_foto: null, contraparte_telefono: otra ? otra.telefono : null,
          };
        })
        .reverse(),
    };
  }
  if (sql.startsWith('SELECT sol.id, sol.estado, sol.contratante_id')) {
    const x = solicitudes.find((y) => y.id === p0);
    return { rows: x ? [{ id: x.id, estado: x.estado, contratante_id: x.contratante_id, prestador_usuario_id: (perfilPorId(x.perfil_prestador_id) || {}).usuario_id }] : [] };
  }
  if (sql.startsWith('UPDATE solicitudes SET estado = $2')) {
    const x = solicitudes.find((y) => y.id === p0);
    x.estado = p1;
    if (p2 != null) x.respuesta = p2;
    x.visto_contratante = p3;
    x.visto_prestador = p4;
    x.fecha_actualizacion = new Date();
    return { rows: [{ ...x }] };
  }
  if (sql.startsWith('SELECT COUNT(*) FILTER')) {
    const n = (f) => String(solicitudes.filter(f).length);
    return {
      rows: [{
        recibidas_nuevas: n((x) => x.perfil_prestador_id === p1 && !x.visto_prestador),
        recibidas_pendientes: n((x) => x.perfil_prestador_id === p1 && x.estado === 'PENDIENTE'),
        enviadas_novedades: n((x) => x.contratante_id === p0 && !x.visto_contratante),
      }],
    };
  }
  if (sql.startsWith('UPDATE solicitudes SET visto_contratante = true')) {
    const afectadas = solicitudes.filter((x) => x.contratante_id === p0 && !x.visto_contratante);
    afectadas.forEach((x) => { x.visto_contratante = true; });
    return { rows: [], rowCount: afectadas.length };
  }
  if (sql.startsWith('UPDATE solicitudes SET visto_prestador = true')) {
    const mios = perfiles.filter((x) => x.usuario_id === p0).map((x) => x.id);
    const afectadas = solicitudes.filter((x) => mios.includes(x.perfil_prestador_id) && !x.visto_prestador);
    afectadas.forEach((x) => { x.visto_prestador = true; });
    return { rows: [], rowCount: afectadas.length };
  }
  if (sql.startsWith('SELECT COUNT(*)::int AS n FROM servicios WHERE prestador_id')) {
    return { rows: [{ n: servicios.filter((x) => x.prestador_id === p0).length }] };
  }
  if (sql.startsWith('INSERT INTO servicios')) {
    const fila = { id: p0, prestador_id: p1, titulo: p2, descripcion: p3, categoria: p4, precio_referencial: parametros[5], disponible: parametros[6] };
    servicios.push(fila);
    return { rows: [{ ...fila }] };
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

module.exports = { servicios, encender, agregarUsuario, crearPerfilDe, crearServicioDe, tokenDe };
