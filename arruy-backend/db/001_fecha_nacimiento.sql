-- Aruy es solo para mayores de 18 anos: se guarda la fecha de nacimiento de quien se registra.
-- Se ejecuta UNA vez en el SQL Editor de Neon. Es segura de repetir (IF NOT EXISTS) y no toca los datos actuales.
-- Mientras no se ejecute, el servidor valida la edad igual pero no guarda la fecha.
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_nacimiento date;

-- Para comprobar:
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'usuarios' AND column_name = 'fecha_nacimiento';
