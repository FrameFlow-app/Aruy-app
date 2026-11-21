// Prepara la base de datos al encender el servidor. Todo es idempotente (IF NOT EXISTS) y NO borra ni cambia datos:
// solo agrega lo que falte. Si algo no se puede (por ejemplo, falta de permisos), se anota y el servidor sigue
// funcionando; las pruebas y los scripts de db/ dicen lo mismo, por si prefieren ejecutarlo a mano en Neon.

const SENTENCIAS = [
  {
    nombre: 'usuarios.fecha_nacimiento (solo mayores de 18)',
    sql: 'ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_nacimiento date',
  },
  {
    nombre: 'tabla solicitudes',
    sql: `CREATE TABLE IF NOT EXISTS solicitudes (
      id uuid PRIMARY KEY,
      servicio_id uuid REFERENCES servicios(id) ON DELETE SET NULL,
      perfil_prestador_id uuid NOT NULL REFERENCES perfiles_prestadores(id) ON DELETE CASCADE,
      contratante_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
      mensaje text NOT NULL,
      estado text NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'CANCELADA', 'COMPLETADA')),
      respuesta text,
      visto_prestador boolean NOT NULL DEFAULT false,
      visto_contratante boolean NOT NULL DEFAULT true,
      fecha_creacion timestamptz NOT NULL DEFAULT NOW(),
      fecha_actualizacion timestamptz NOT NULL DEFAULT NOW()
    )`,
  },
  {
    nombre: 'indice de solicitudes recibidas',
    sql: 'CREATE INDEX IF NOT EXISTS solicitudes_prestador_idx ON solicitudes (perfil_prestador_id, estado)',
  },
  {
    nombre: 'indice de solicitudes enviadas',
    sql: 'CREATE INDEX IF NOT EXISTS solicitudes_contratante_idx ON solicitudes (contratante_id, fecha_creacion DESC)',
  },
  {
    nombre: 'una sola solicitud pendiente por persona y servicio',
    sql: "CREATE UNIQUE INDEX IF NOT EXISTS solicitudes_una_pendiente ON solicitudes (contratante_id, servicio_id) WHERE estado = 'PENDIENTE'",
  },
  {
    nombre: 'solicitudes.whatsapp_compartido',
    sql: 'ALTER TABLE solicitudes ADD COLUMN IF NOT EXISTS whatsapp_compartido boolean NOT NULL DEFAULT false',
  },
  {
    nombre: 'tabla mensajes (chat por solicitud)',
    sql: `CREATE TABLE IF NOT EXISTS mensajes (
      id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
      solicitud_id uuid NOT NULL REFERENCES solicitudes(id) ON DELETE CASCADE,
      autor_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
      texto varchar(1000) NOT NULL,
      tipo varchar(10) NOT NULL DEFAULT 'TEXTO' CHECK (tipo IN ('TEXTO', 'WHATSAPP')),
      leido boolean NOT NULL DEFAULT false,
      fecha_creacion timestamptz NOT NULL DEFAULT now()
    )`,
  },
  {
    nombre: 'indice de mensajes por solicitud',
    sql: 'CREATE INDEX IF NOT EXISTS mensajes_solicitud_idx ON mensajes (solicitud_id, fecha_creacion)',
  },
  {
    nombre: 'tabla codigos_recuperacion (olvide mi contrasena)',
    sql: `CREATE TABLE IF NOT EXISTS codigos_recuperacion (
      id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
      usuario_id uuid REFERENCES usuarios(id) ON DELETE CASCADE,
      codigo_hash text NOT NULL,
      expira timestamptz NOT NULL,
      intentos int NOT NULL DEFAULT 0,
      usado boolean NOT NULL DEFAULT false,
      fecha_creacion timestamptz DEFAULT now()
    )`,
  },
];

async function asegurarEsquema(pool, { log = console.log } = {}) {
  const resultado = [];
  for (const { nombre, sql } of SENTENCIAS) {
    try {
      await pool.query(sql);
      resultado.push({ nombre, ok: true });
    } catch (e) {
      resultado.push({ nombre, ok: false, motivo: e.message });
      log(`Base de datos: no se pudo preparar "${nombre}": ${e.message}`);
    }
  }
  return resultado;
}

module.exports = { asegurarEsquema, SENTENCIAS };
