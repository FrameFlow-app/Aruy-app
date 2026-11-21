-- Chat por solicitud y recuperacion de contrasena.
-- El servidor ya crea esto SOLO al encender (esquema.js). Este archivo queda por si prefieren ejecutarlo a mano
-- en el SQL Editor de Neon. Es seguro de repetir y no toca los datos actuales.
ALTER TABLE solicitudes ADD COLUMN IF NOT EXISTS whatsapp_compartido boolean NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS mensajes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  solicitud_id uuid NOT NULL REFERENCES solicitudes(id) ON DELETE CASCADE,
  autor_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  texto varchar(1000) NOT NULL,
  tipo varchar(10) NOT NULL DEFAULT 'TEXTO' CHECK (tipo IN ('TEXTO', 'WHATSAPP')),
  leido boolean NOT NULL DEFAULT false,
  fecha_creacion timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS mensajes_solicitud_idx ON mensajes (solicitud_id, fecha_creacion);

CREATE TABLE IF NOT EXISTS codigos_recuperacion (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id uuid REFERENCES usuarios(id) ON DELETE CASCADE,
  codigo_hash text NOT NULL,
  expira timestamptz NOT NULL,
  intentos int NOT NULL DEFAULT 0,
  usado boolean NOT NULL DEFAULT false,
  fecha_creacion timestamptz DEFAULT now()
);
