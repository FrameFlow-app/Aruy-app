// Envio de correos (codigo para recuperar la contrasena) por SMTP de Gmail.
// Necesita SMTP_USER y SMTP_PASS (contrasena de aplicacion de Google) en el .env. Si faltan, solo se anota.
const nodemailer = require('nodemailer');
const { registrarError } = require('./registro');

let transporte;
function obtenerTransporte() {
  if (!transporte) {
    transporte = nodemailer.createTransport({
      host: 'smtp.gmail.com',
      port: 465,
      secure: true,
      auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS },
    });
  }
  return transporte;
}

async function enviarCodigo(email, codigo) {
  if (!process.env.SMTP_USER || !process.env.SMTP_PASS) {
    console.warn('Correo no configurado: define SMTP_USER y SMTP_PASS para enviar codigos de recuperacion');
    return false;
  }
  try {
    await obtenerTransporte().sendMail({
      from: `Aruy <${process.env.SMTP_USER}>`,
      to: email,
      subject: 'Tu código para recuperar tu contraseña de Aruy',
      text:
        `Hola,\n\nTu código para recuperar tu contraseña es: ${codigo}\n\n` +
        'Este código vence en 15 minutos. Si tú no lo pediste, ignora este mensaje.\n\nEquipo Aruy',
    });
    return true;
  } catch (e) {
    registrarError(e);
    return false;
  }
}

module.exports = { enviarCodigo };
