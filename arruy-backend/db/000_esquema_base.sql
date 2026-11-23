-- Tablas base de Aruy. Reproduce el esquema de la base original (sacado con ver-esquema.js).
-- Es seguro correrlo varias veces: solo crea lo que falte (IF NOT EXISTS) y NUNCA borra ni cambia datos.
-- Orden de aplicacion: 000 (este), 001, 002, 003. Las tablas solicitudes, mensajes y codigos_recuperacion
-- las crean 002/003 y esquema.js; aqui van las 7 tablas que no estaban en ningun archivo.

CREATE TABLE IF NOT EXISTS usuarios (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  nombre varchar(150) NOT NULL,
  telefono varchar(20) NOT NULL,
  rol varchar(20) NOT NULL,
  estado_verificacion varchar(20) NOT NULL DEFAULT 'NO_SOLICITADA',
  foto_perfil text,
  activo boolean NOT NULL DEFAULT true,
  fecha_registro timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  email varchar(255) NOT NULL,
  password_hash text NOT NULL,
  fecha_nacimiento date,
  CONSTRAINT usuarios_telefono_key UNIQUE (telefono),
  CONSTRAINT usuarios_rol_check CHECK (rol IN ('PRESTADOR', 'CONTRATANTE')),
  CONSTRAINT usuarios_estado_verificacion_check CHECK (estado_verificacion IN ('NO_SOLICITADA', 'PENDIENTE', 'VERIFICADA', 'RECHAZADA')),
  CONSTRAINT chk_usuarios_email_no_vacio CHECK (length(trim(email)) > 0),
  CONSTRAINT chk_usuarios_password_hash_no_vacio CHECK (length(trim(password_hash)) > 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_usuarios_email_normalizado ON usuarios (lower(trim(email)));

CREATE TABLE IF NOT EXISTS perfiles_prestadores (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
  oficio_principal varchar(100) NOT NULL,
  descripcion_profesional text,
  zona_cobertura varchar(150) NOT NULL,
  anios_experiencia integer,
  disponible boolean NOT NULL DEFAULT true,
  CONSTRAINT perfiles_prestadores_usuario_id_key UNIQUE (usuario_id),
  CONSTRAINT perfiles_prestadores_anios_experiencia_check CHECK (anios_experiencia >= 0)
);

CREATE TABLE IF NOT EXISTS servicios (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  prestador_id uuid NOT NULL REFERENCES perfiles_prestadores(id) ON DELETE RESTRICT,
  titulo varchar(150) NOT NULL,
  descripcion text NOT NULL,
  categoria varchar(30) NOT NULL,
  precio_referencial numeric,
  disponible boolean NOT NULL DEFAULT true,
  fecha_publicacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT servicios_categoria_check CHECK (categoria IN ('PLOMERIA', 'ELECTRICIDAD', 'ALBANILERIA', 'PINTURA', 'CARPINTERIA', 'BELLEZA', 'OTROS')),
  CONSTRAINT servicios_precio_referencial_check CHECK (precio_referencial >= 0)
);

CREATE TABLE IF NOT EXISTS fotos_portafolio (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  servicio_id uuid NOT NULL REFERENCES servicios(id) ON DELETE CASCADE,
  referencia_imagen text NOT NULL,
  descripcion text,
  orden integer NOT NULL DEFAULT 0,
  fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fotos_portafolio_orden_check CHECK (orden >= 0)
);
CREATE INDEX IF NOT EXISTS idx_fotos_portafolio_servicio_orden ON fotos_portafolio (servicio_id, orden, id);

CREATE TABLE IF NOT EXISTS etiquetas_fotos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  foto_id uuid NOT NULL,
  etiqueta varchar(100) NOT NULL,
  CONSTRAINT fk_etiquetas_foto FOREIGN KEY (foto_id) REFERENCES fotos_portafolio(id) ON DELETE CASCADE,
  CONSTRAINT uq_etiqueta_por_foto UNIQUE (foto_id, etiqueta),
  CONSTRAINT chk_etiqueta_no_vacia CHECK (length(trim(etiqueta)) > 0)
);

CREATE TABLE IF NOT EXISTS calificaciones (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  contratante_id uuid NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
  perfil_prestador_id uuid NOT NULL REFERENCES perfiles_prestadores(id) ON DELETE RESTRICT,
  puntuacion smallint NOT NULL,
  comentario text,
  fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_calificacion_contratante_prestador UNIQUE (contratante_id, perfil_prestador_id),
  CONSTRAINT calificaciones_puntuacion_check CHECK (puntuacion >= 1 AND puntuacion <= 5)
);
CREATE INDEX IF NOT EXISTS idx_calificaciones_perfil ON calificaciones (perfil_prestador_id);

CREATE TABLE IF NOT EXISTS suscripciones (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  prestador_id uuid NOT NULL REFERENCES perfiles_prestadores(id) ON DELETE RESTRICT,
  tipo_plan varchar(30) NOT NULL,
  estado varchar(20) NOT NULL DEFAULT 'PENDIENTE',
  fecha_inicio timestamptz NOT NULL,
  fecha_fin timestamptz,
  renovacion_automatica boolean NOT NULL DEFAULT false,
  CONSTRAINT suscripciones_tipo_plan_check CHECK (tipo_plan IN ('ARUY_PRO_SEMANAL', 'ARUY_PRO_MENSUAL')),
  CONSTRAINT suscripciones_estado_check CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'CANCELADA', 'VENCIDA')),
  CONSTRAINT chk_fechas_suscripcion CHECK (fecha_fin IS NULL OR fecha_fin > fecha_inicio)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_suscripcion_activa_prestador ON suscripciones (prestador_id) WHERE estado IN ('PENDIENTE', 'ACTIVA');
