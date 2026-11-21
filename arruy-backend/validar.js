const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const CATEGORIAS = ['PLOMERIA', 'ELECTRICIDAD', 'ALBANILERIA', 'PINTURA', 'CARPINTERIA', 'BELLEZA', 'OTROS'];
const ROLES = ['PRESTADOR', 'CONTRATANTE'];

const esUuid = (v) => typeof v === 'string' && UUID.test(v);
const textoValido = (v, max) =>
  typeof v === 'string' && v.trim().length > 0 && v.trim().length <= max;
const esEmail = (v) => typeof v === 'string' && v.length <= 254 && EMAIL.test(v.trim());
// Direccion https valida (las fotos viven en Firebase Storage; aqui solo se guarda la URL)
const esUrlHttps = (v, max = 1000) => {
  if (typeof v !== 'string' || v.trim().length === 0 || v.length > max) return false;
  try {
    const u = new URL(v.trim());
    return u.protocol === 'https:' && u.hostname.length > 0;
  } catch {
    return false;
  }
};
// bcrypt solo usa los primeros 72 bytes de la contrasena
const passwordValida = (v) => typeof v === 'string' && v.length >= 8 && Buffer.byteLength(v, 'utf8') <= 72;

function limpiarEtiquetas(lista) {
  if (lista === undefined) return [];
  if (!Array.isArray(lista) || lista.length > 20) return null;
  if (!lista.every((e) => textoValido(e, 50))) return null;
  return [...new Set(lista.map((e) => e.trim()))];
}

// "LIMPIEZA DE CASAS" o "limpieza de casas" -> "Limpieza de casas": inicial en mayuscula y el resto en minuscula
// (tambien con acentos y la enie). Los espacios repetidos se juntan.
function normalizarTitulo(titulo) {
  const limpio = String(titulo ?? '').trim().replace(/\s+/g, ' ');
  if (limpio === '') return limpio;
  const minusculas = limpio.toLocaleLowerCase('es');
  return minusculas.charAt(0).toLocaleUpperCase('es') + minusculas.slice(1);
}

// Edad en anios cumplidos a partir de una fecha AAAA-MM-DD (en UTC). Devuelve null si el texto no es una fecha
// real (por ejemplo 2000-02-30). Es negativa si la fecha es futura.
function edadEnAnios(texto, hoy = new Date()) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(texto ?? '').trim());
  if (!m) return null;
  const anio = Number(m[1]);
  const mes = Number(m[2]);
  const dia = Number(m[3]);
  const fecha = new Date(Date.UTC(anio, mes - 1, dia));
  if (fecha.getUTCFullYear() !== anio || fecha.getUTCMonth() !== mes - 1 || fecha.getUTCDate() !== dia) return null;
  let edad = hoy.getUTCFullYear() - anio;
  const hm = hoy.getUTCMonth() + 1;
  if (hm < mes || (hm === mes && hoy.getUTCDate() < dia)) edad -= 1;
  return edad;
}

const EDAD_MINIMA = 18;

module.exports = {
  edadEnAnios, EDAD_MINIMA, normalizarTitulo, esUuid, textoValido, esEmail, esUrlHttps, passwordValida, limpiarEtiquetas, CATEGORIAS, ROLES,
};
