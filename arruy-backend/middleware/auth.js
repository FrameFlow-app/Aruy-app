const jwt = require('jsonwebtoken');

// Cuanto dura una sesion. Por defecto 30 dias (la app de celular no debe pedir el login a diario).
// Se puede cambiar con JWT_EXPIRES_IN en el .env, por ejemplo 12h o 7d.
const DURACION_POR_DEFECTO = '30d';
function duracionToken() {
  const valor = (process.env.JWT_EXPIRES_IN || '').trim();
  return /^[1-9]\d*[smhd]$/.test(valor) ? valor : DURACION_POR_DEFECTO;
}

function firmarToken(usuario) {
  return jwt.sign({ rol: usuario.rol }, process.env.JWT_SECRET, {
    subject: String(usuario.id),
    expiresIn: duracionToken(),
  });
}

// Guardia de la puerta: revisa que la persona traiga un token valido
function exigirToken(req, res, next) {
  const [tipo, token] = (req.headers.authorization || '').split(' ');
  if (tipo !== 'Bearer' || !token) {
    return res.status(401).json({ error: 'Falta el token (Authorization: Bearer ...)' });
  }
  try {
    const datos = jwt.verify(token, process.env.JWT_SECRET);
    req.usuario = { id: datos.sub, rol: datos.rol };
    next();
  } catch {
    res.status(401).json({ error: 'Token invalido o vencido' });
  }
}

// Revisa que el rol del usuario (PRESTADOR o CONTRATANTE) tenga permiso
function exigirRol(...roles) {
  return (req, res, next) =>
    roles.includes(req.usuario && req.usuario.rol)
      ? next()
      : res.status(403).json({ error: 'No tienes permiso para esta accion' });
}

module.exports = { exigirToken, exigirRol, firmarToken, duracionToken };
