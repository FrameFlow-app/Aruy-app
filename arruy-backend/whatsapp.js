// Numero para abrir WhatsApp (https://wa.me/<numero>): solo digitos, con el codigo de Ecuador (593).
// Sirve para celulares ecuatorianos (09XXXXXXXX o +593 9XXXXXXXX); si no se puede calcular, null.
function aWhatsapp(telefono) {
  const d = String(telefono || '').replace(/\D/g, '');
  if (/^593\d{9}$/.test(d)) return d;
  if (/^0\d{9}$/.test(d)) return `593${d.slice(1)}`;
  return null;
}

module.exports = { aWhatsapp };
