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

module.exports = {
  normalizarTitulo, esUuid, textoValido, esEmail, esUrlHttps, passwordValida, limpiarEtiquetas, CATEGORIAS, ROLES,
};
