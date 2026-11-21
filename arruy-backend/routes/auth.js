const express = require('express');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const pool = require('../db');
const { registrarError } = require('../registro');
const { firmarToken, exigirToken } = require('../middleware/auth');
const { edadEnAnios, EDAD_MINIMA, textoValido, esEmail, passwordValida, ROLES } = require('../validar');
const { urlPublica } = require('../almacenamiento');
const correo = require('../correo');

const router = express.Router();

const COLUMNAS_PUBLICAS = 'id, nombre, telefono, rol, estado_verificacion, foto_perfil, activo';

// Se compara contra este hash cuando el correo no existe, para que tarde lo mismo
const HASH_FALSO = bcrypt.hashSync('contrasena-falsa-para-igualar-tiempos', 10);

const aUsuario = (u) => ({ ...u, id: String(u.id), telefono: u.telefono ?? '', foto_perfil: urlPublica(u.foto_perfil) });

// La columna usuarios.fecha_nacimiento se agrega con db/001_fecha_nacimiento.sql. Mientras no exista, la edad se
// valida igual y simplemente no se guarda. Se consulta una sola vez.
let hayColumnaFecha;
async function guardaFechaNacimiento(client) {
  if (hayColumnaFecha !== true) {
    try {
      const r = await client.query(
        "SELECT 1 FROM information_schema.columns WHERE table_name = 'usuarios' AND column_name = 'fecha_nacimiento'"
      );
      hayColumnaFecha = r.rows.length > 0;
    } catch {
      return false; // no se recuerda el fallo: se vuelve a preguntar en el siguiente registro
    }
  }
  return hayColumnaFecha;
}

// Registro. Si el rol es PRESTADOR tambien se crea su perfil (perfiles_prestadores),
// porque en la base los servicios cuelgan del perfil y no del usuario.
router.post('/registro', async (req, res) => {
  const {
    nombre, telefono, email, password, rol, fecha_nacimiento,
    oficio_principal, zona_cobertura, descripcion_profesional, anios_experiencia,
  } = req.body || {};

  if (!textoValido(nombre, 100)) return res.status(400).json({ error: 'nombre es obligatorio' });
  if (!textoValido(telefono, 20)) return res.status(400).json({ error: 'telefono es obligatorio' });
  if (!esEmail(email)) return res.status(400).json({ error: 'email invalido' });
  if (!passwordValida(password)) {
    return res.status(400).json({ error: 'password debe tener al menos 8 caracteres' });
  }
  if (!ROLES.includes(rol)) {
    return res.status(400).json({ error: 'rol debe ser PRESTADOR o CONTRATANTE' });
  }
  // Aruy es solo para mayores de edad
  const edad = edadEnAnios(fecha_nacimiento);
  if (edad === null || edad < 0 || edad > 120) {
    return res.status(400).json({ error: 'fecha_nacimiento debe ser una fecha valida con el formato AAAA-MM-DD', campo: 'fecha_nacimiento' });
  }
  if (edad < EDAD_MINIMA) {
    return res.status(400).json({ error: `Aruy es solo para mayores de ${EDAD_MINIMA} anos`, campo: 'fecha_nacimiento' });
  }
  if (rol === 'PRESTADOR') {
    if (!textoValido(oficio_principal, 100)) {
      return res.status(400).json({ error: 'oficio_principal es obligatorio para un PRESTADOR' });
    }
    if (!textoValido(zona_cobertura, 100)) {
      return res.status(400).json({ error: 'zona_cobertura es obligatoria para un PRESTADOR' });
    }
    if (descripcion_profesional != null && !textoValido(descripcion_profesional, 2000)) {
      return res.status(400).json({ error: 'descripcion_profesional invalida' });
    }
    if (anios_experiencia != null &&
        !(Number.isInteger(anios_experiencia) && anios_experiencia >= 0 && anios_experiencia <= 80)) {
      return res.status(400).json({ error: 'anios_experiencia debe ser un entero entre 0 y 80' });
    }
  }

  const client = await pool.connect();
  try {
    const correo = email.trim().toLowerCase();
    const celular = telefono.trim();
    // Se revisa cada dato por separado para decirle a la persona CUAL esta repetido (el celular tambien es unico)
    const porCorreo = await client.query('SELECT 1 FROM usuarios WHERE lower(email) = $1', [correo]);
    if (porCorreo.rows.length > 0) {
      return res.status(409).json({ error: 'Ese correo ya esta registrado', campo: 'email' });
    }
    const porCelular = await client.query('SELECT 1 FROM usuarios WHERE telefono = $1', [celular]);
    if (porCelular.rows.length > 0) {
      return res.status(409).json({ error: 'Ese celular ya esta registrado en otra cuenta', campo: 'telefono' });
    }
    const hash = await bcrypt.hash(password, 10);

    await client.query('BEGIN');
    const columnas = ['id', 'nombre', 'telefono', 'rol', 'estado_verificacion', 'foto_perfil', 'activo', 'fecha_registro', 'email', 'password_hash'];
    const valores = ['$1', '$2', '$3', '$4', "'NO_SOLICITADA'", '$5', 'true', 'NOW()', '$6', '$7'];
    const parametros = [crypto.randomUUID(), nombre.trim(), celular, rol, null, correo, hash];
    if (await guardaFechaNacimiento(client)) {
      columnas.push('fecha_nacimiento');
      valores.push('$8');
      parametros.push(fecha_nacimiento.trim());
    }
    const r = await client.query(
      `INSERT INTO usuarios (${columnas.join(', ')}) VALUES (${valores.join(', ')}) RETURNING ${COLUMNAS_PUBLICAS}`,
      parametros
    );
    const u = r.rows[0];

    let perfilId = null;
    if (rol === 'PRESTADOR') {
      perfilId = crypto.randomUUID();
      await client.query(
        `INSERT INTO perfiles_prestadores
           (id, usuario_id, oficio_principal, descripcion_profesional, zona_cobertura, anios_experiencia, disponible)
         VALUES ($1, $2, $3, $4, $5, $6, true)`,
        [
          perfilId, u.id, oficio_principal.trim(),
          descripcion_profesional == null ? null : descripcion_profesional.trim(),
          zona_cobertura.trim(), anios_experiencia ?? null,
        ]
      );
    }
    await client.query('COMMIT');
    res.status(201).json({ token: firmarToken(u), usuario: aUsuario(u), perfil_prestador_id: perfilId });
  } catch (e) {
    await client.query('ROLLBACK').catch(() => {});
    if (e.code === '23505') {
      // Si dos personas se registran a la vez, la base de datos es quien avisa el duplicado
      const donde = `${e.constraint || ''} ${e.detail || ''}`.toLowerCase();
      if (/telefono/.test(donde)) return res.status(409).json({ error: 'Ese celular ya esta registrado en otra cuenta', campo: 'telefono' });
      if (/email|correo/.test(donde)) return res.status(409).json({ error: 'Ese correo ya esta registrado', campo: 'email' });
      return res.status(409).json({ error: 'El telefono o el correo ya estan registrados' });
    }
    registrarError(e);
    res.status(500).json({ error: 'Error al registrar el usuario' });
  } finally {
    client.release();
  }
});

router.post('/login', async (req, res) => {
  const { email, password } = req.body || {};
  if (!esEmail(email) || typeof password !== 'string' || password.length === 0) {
    return res.status(400).json({ error: 'email y password son obligatorios' });
  }
  try {
    const r = await pool.query(
      `SELECT ${COLUMNAS_PUBLICAS}, password_hash FROM usuarios WHERE lower(email) = $1`,
      [email.trim().toLowerCase()]
    );
    const u = r.rows[0];
    const coincide = await bcrypt
      .compare(password, u ? u.password_hash : HASH_FALSO)
      .catch(() => false);
    if (!u || !coincide || !u.activo) {
      return res.status(401).json({ error: 'Correo o contrasena incorrectos' });
    }
    delete u.password_hash;
    const p = await pool.query('SELECT id FROM perfiles_prestadores WHERE usuario_id = $1', [u.id]);
    res.json({
      token: firmarToken(u),
      usuario: aUsuario(u),
      perfil_prestador_id: p.rows[0] ? String(p.rows[0].id) : null,
    });
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al iniciar sesion' });
  }
});

// Renovar la sesion: con un token vigente se entrega uno nuevo de 30 dias. La app lo pide al abrirse cuando al token
// le quedan pocos dias, y asi quien usa Aruy seguido nunca tiene que volver a escribir su contrasena.
router.post('/renovar', exigirToken, async (req, res) => {
  try {
    const r = await pool.query(`SELECT ${COLUMNAS_PUBLICAS} FROM usuarios WHERE id = $1`, [req.usuario.id]);
    const u = r.rows[0];
    if (!u || u.activo === false) return res.status(401).json({ error: 'Esta cuenta ya no existe' });
    const p = await pool.query('SELECT id FROM perfiles_prestadores WHERE usuario_id = $1', [u.id]);
    res.json({
      token: firmarToken(u),
      usuario: aUsuario(u),
      perfil_prestador_id: p.rows[0] ? String(p.rows[0].id) : null,
    });
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al renovar la sesion' });
  }
});

// Olvide mi contrasena: se envia un codigo de 6 digitos al correo. La respuesta es siempre la misma,
// exista o no la cuenta, para no revelar quien esta registrado.
const MENSAJE_OLVIDE = 'Si el correo está registrado, te enviamos un código.';
const CODIGO_VIGENCIA_MS = 15 * 60 * 1000;
const CODIGO_MAX_INTENTOS = 5;
const CODIGO_INVALIDO = 'El código no es válido o ya expiró.';

router.post('/olvide', async (req, res) => {
  const { email } = req.body || {};
  if (esEmail(email)) {
    try {
      const correoUsuario = email.trim().toLowerCase();
      const r = await pool.query('SELECT id, nombre FROM usuarios WHERE lower(email) = $1 AND activo = true', [correoUsuario]);
      const u = r.rows[0];
      if (u) {
        const codigo = String(crypto.randomInt(0, 1000000)).padStart(6, '0');
        const hash = await bcrypt.hash(codigo, 10);
        await pool.query('UPDATE codigos_recuperacion SET usado = true WHERE usuario_id = $1 AND usado = false', [u.id]);
        await pool.query('INSERT INTO codigos_recuperacion (usuario_id, codigo_hash, expira) VALUES ($1, $2, $3)', [
          u.id,
          hash,
          new Date(Date.now() + CODIGO_VIGENCIA_MS),
        ]);
        // Sin esperar: si el envio falla solo se anota, la respuesta es la misma
        Promise.resolve(correo.enviarCodigo(correoUsuario, codigo)).catch(registrarError);
      }
    } catch (e) {
      registrarError(e);
    }
  }
  res.json({ mensaje: MENSAJE_OLVIDE });
});

router.post('/restablecer', async (req, res) => {
  const { email, codigo, password } = req.body || {};
  if (!esEmail(email) || typeof codigo !== 'string' || !/^\d{6}$/.test(codigo.trim())) {
    return res.status(400).json({ error: CODIGO_INVALIDO });
  }
  try {
    const r = await pool.query(
      `SELECT c.id, c.codigo_hash, c.intentos, c.usuario_id
       FROM codigos_recuperacion c JOIN usuarios u ON u.id = c.usuario_id
       WHERE lower(u.email) = $1 AND u.activo = true AND c.usado = false AND c.expira > NOW()
       ORDER BY c.fecha_creacion DESC LIMIT 1`,
      [email.trim().toLowerCase()]
    );
    const c = r.rows[0];
    if (!c) return res.status(400).json({ error: CODIGO_INVALIDO });
    const coincide = await bcrypt.compare(codigo.trim(), c.codigo_hash).catch(() => false);
    if (!coincide) {
      await pool.query('UPDATE codigos_recuperacion SET intentos = intentos + 1, usado = (intentos + 1 >= $2) WHERE id = $1', [
        c.id,
        CODIGO_MAX_INTENTOS,
      ]);
      return res.status(400).json({ error: CODIGO_INVALIDO });
    }
    if (!passwordValida(password)) {
      return res.status(400).json({ error: 'password debe tener al menos 8 caracteres' });
    }
    const hash = await bcrypt.hash(password, 10);
    await pool.query('UPDATE usuarios SET password_hash = $2 WHERE id = $1', [c.usuario_id, hash]);
    await pool.query('UPDATE codigos_recuperacion SET usado = true WHERE usuario_id = $1 AND usado = false', [c.usuario_id]);
    res.json({ mensaje: 'Contraseña actualizada' });
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al restablecer la contraseña' });
  }
});

module.exports = router;
