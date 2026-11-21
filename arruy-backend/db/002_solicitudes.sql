-- Solicitudes dentro de la app: una persona le pide un servicio a un prestador y este acepta o rechaza.
-- El servidor ya crea esto SOLO al encender (esquema.js). Este archivo queda por si prefieren ejecutarlo a mano
-- en el SQL Editor de Neon. Es seguro de repetir y no toca los datos actuales.
CREATE TABLE IF NOT EXISTS solicitudes (
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
);
CREATE INDEX IF NOT EXISTS solicitudes_prestador_idx ON solicitudes (perfil_prestador_id, estado);
CREATE INDEX IF NOT EXISTS solicitudes_contratante_idx ON solicitudes (contratante_id, fecha_creacion DESC);
CREATE UNIQUE INDEX IF NOT EXISTS solicitudes_una_pendiente ON solicitudes (contratante_id, servicio_id) WHERE estado = 'PENDIENTE';
