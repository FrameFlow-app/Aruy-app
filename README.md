# Aruy

Aruy conecta a trabajadores independientes de oficios en Quito que necesitan conseguir más clientes con personas que buscan servicios confiables, rápidos y sin intermediarios.

Proyecto de los cursos Programación Móvil y Emprendimiento Tecnológico (PUCE). Lema: **la confianza que ofrecemos con los datos y la seguridad**.

## Equipo FrameFlow

| Integrante | Rol |
|---|---|
| Gloria Chushig | Product Owner / Arquitecto de Datos |
| Alisson Basantes | Scrum Master / Edge AI y Git |
| David Tapia | Desarrollo Frontend y UI/UX |
| Karol Moreno | DevOps, gestión ágil y QA |

## Cómo está hecho

```
App Android (Kotlin + Jetpack Compose)
   │  ML Kit en el teléfono: etiqueta y valida las fotos antes de subirlas
   │  HTTPS
   ▼
API REST (Node.js + Express, en Render)
   ├──► Neon (PostgreSQL): usuarios, perfiles, servicios, calificaciones, suscripciones
   └──► Supabase Storage: fotos de los trabajos y de perfil (solo WebP)
```

- **App:** Kotlin, Jetpack Compose (Material 3), arquitectura MVVM, Room (guardados, bloqueados y reportes en el teléfono), Retrofit + OkHttp, DataStore, Coil, ML Kit Image Labeling, corrutinas.
- **Servidor:** carpeta `arruy-backend`. Node.js 20.9 o superior, Express 5, `pg`, `bcryptjs`, `jsonwebtoken`, `helmet`, `express-rate-limit`, `sharp` (convierte las fotos a WebP) y Swagger en `/docs`.
- **Base de datos:** Neon (PostgreSQL). **Fotos:** Supabase Storage (bucket público `aruy-portafolio`; la base solo guarda la ruta).

## Qué hace

- **Una sola cuenta con el mismo correo.** Cualquier persona con cuenta busca, contacta y califica. Quien ofrece servicios además tiene Panel, Servicios y Portafolio, y también puede explorar el catálogo (pestaña *Explorar*).
- **Catálogo y búsqueda:** categorías, filtros, detalle del servicio, perfil público del prestador, contacto por WhatsApp y opiniones de 1 a 5.
- **Prestador:** publicar, editar, pausar y borrar servicios; portafolio de fotos de sus trabajos; perfil (oficio, zona, años de experiencia); interruptor "Recibiendo solicitudes"; Aruy Pro (suscripción **simulada**, no se cobra nada).
- **Cuenta y privacidad:** sesión que se conserva al cerrar la app (token cifrado con Android Keystore), ajustes de tema y tamaño de texto, eliminar la cuenta (borra de verdad los datos y las fotos) y páginas de privacidad en `docs/`.

## IA en el teléfono (ML Kit)

Cada foto que sube un prestador se analiza **en su celular** con el etiquetador de imágenes de ML Kit (las fotos no se mandan a ningún servicio de IA). Con las etiquetas, la app comprueba que la foto corresponda al servicio:

1. Lee el **título y la descripción** del servicio ("Limpieza de casas", "Desarrollo de apps"...) para saber qué se espera ver: una casa por dentro, pantallas, plantas, etc. Hay 13 tipos de servicio.
2. Si el tipo es conocido, la foto debe tener **al menos una etiqueta esperada**. Si no, se rechaza con un mensaje: *"No pudimos reconocer en esta foto un trabajo de «…». Detectamos: … Intenta con otra imagen."*
3. Una selfie o una calle (sin obra) se rechazan siempre. Si el título no corresponde a ningún tipo, solo se rechaza lo claramente ajeno.
4. Al aceptar una foto, la app muestra lo que vio la IA.

**Límite honesto:** el modelo base de ML Kit conoce unas 430 etiquetas generales (no existen "escoba", "tubería" ni "libro"), así que es un **filtro de confianza**, no una prueba de que cada foto sea un trabajo real. El código está en `app/src/main/java/ec/edu/puce/aruy/domain/catalogo/ValidadorFotoOficio.kt`.

## Cómo ejecutarlo

### Servidor (`arruy-backend`)

```bash
cd arruy-backend
npm install
cp .env.example .env     # en Windows: copy .env.example .env  (y llenar los valores)
npm start                # http://localhost:3000
npm test                 # pruebas automáticas
```

Documentación interactiva de la API: `http://localhost:3000/docs` (en la nube: https://aruy-api.onrender.com/docs).

Variables del archivo `.env` (nunca se sube a GitHub):

| Variable | Para qué sirve |
|---|---|
| `DATABASE_URL` | Conexión a Neon (PostgreSQL) |
| `JWT_SECRET` | Clave para firmar las sesiones (larga y aleatoria) |
| `JWT_EXPIRES_IN` | Opcional. Duración de la sesión (por defecto `30d`) |
| `PORT` | Opcional. Puerto (Render lo define solo) |
| `SUPABASE_URL` | Dirección del proyecto de Supabase |
| `SUPABASE_SECRET_KEY` | Clave secreta de Supabase (solo vive en el servidor) |
| `SUPABASE_STORAGE_BUCKET` | Nombre del bucket (`aruy-portafolio`) |

### App Android

1. Abrir la carpeta del proyecto con Android Studio y esperar el *Sync* de Gradle.
2. Elegir la variante de compilación (*Build Variants*):

| Variante | Servidor al que se conecta |
|---|---|
| `emulador` | Servidor local del PC (`http://10.0.2.2:3000/`) |
| `celular` | El servidor en la nube. Para usar uno local, poner `aruy.baseUrl=http://IP-DEL-PC:3000/` en `local.properties` |
| `produccion` | El servidor en la nube (HTTPS obligatorio). Se puede cambiar con `aruy.prodUrl` en `local.properties` |

3. Para generar un APK que sirva en cualquier celular y emulador (todos los procesadores):
```
.\gradlew.bat assembleCelularDebug
```
El archivo queda en `app\build\outputs\apk\celular\debug\app-celular-debug.apk`. (El APK que genera el botón *Run* de Android Studio solo sirve para el celular conectado.)

4. Para publicar: copiar `keystore.properties.example` como `keystore.properties` (no se sube) y generar el AAB firmado con la variante `produccion`.

### Pruebas

- **Servidor:** `npm test` en `arruy-backend` (20 pruebas: límites por cuenta, memoria de imágenes, sesión de 30 días, cuenta única, calificaciones, mantenimiento de Supabase, apagado ordenado).
- **App:** `.\gradlew.bat testCelularDebugUnitTest` (unas 110 pruebas unitarias, entre ellas 20 de la validación de fotos).

## API principal

Todo lo marcado con *token* necesita el encabezado `Authorization: Bearer <token>` (se obtiene en `/auth/login` o `/auth/registro`). Detalle completo y pruebas en `/docs`.

| Método y ruta | Qué hace |
|---|---|
| `GET /health` | Estado del servidor |
| `POST /auth/registro`, `POST /auth/login` | Crear cuenta e iniciar sesión |
| `GET /usuarios/yo` · `PATCH` · `DELETE` (token) | Mis datos, editarlos o eliminar mi cuenta |
| `POST /usuarios/yo/foto` (token) | Foto de perfil (el servidor la deja en WebP de 512 px) |
| `GET /perfiles/{id}` | Perfil público de un prestador |
| `GET/PATCH /perfiles/yo` (token) | Mi perfil de prestador |
| `POST /perfiles/activar` (token) | "Quiero ofrecer servicios" con la misma cuenta |
| `GET /servicios`, `GET /servicios/{id}` | Catálogo (público) |
| `POST/PATCH/DELETE /servicios` (token) | Publicar, editar y borrar mis servicios |
| `GET/POST /servicios/{id}/fotos`, `DELETE /fotos/{id}` | Portafolio de fotos |
| `GET /calificaciones`, `POST /calificaciones` (token) | Opiniones (no se puede calificar el propio perfil) |
| `POST /suscripciones`, `GET /suscripciones/mia`, `PATCH /suscripciones/{id}/cancelar` | Aruy Pro (simulado) |

## Seguridad y privacidad

- Contraseñas con `bcrypt`; sesiones con JWT (30 días); el rol viaja dentro del token.
- Límites de velocidad por cuenta, por usuario y por red (en una misma red muchas personas no se bloquean entre sí).
- Las fotos se convierten a WebP, se les quita la información de ubicación (EXIF) y no pasan de 500 KB.
- La clave secreta de Supabase solo existe en el servidor; la app nunca la ve.
- Al eliminar la cuenta se borran los datos y las fotos.
- **Pendiente de decidir:** el teléfono del prestador es público (se usa para el contacto por WhatsApp).

## Despliegue

- **Render** (plan gratis): servicio web de Node con *Root Directory* `arruy-backend`, *Build* `npm install`, *Start* `node index.js`, *Health Check* `/health`, rama `develop`, y las variables de arriba. En el plan gratis el servicio se duerme tras 15 minutos sin uso; un monitor de UptimeRobot lo consulta cada 5 minutos para mantenerlo despierto.
- **Neon** y **Supabase** (planes gratis). Supabase pausa los proyectos con poca actividad: el servidor le hace una consulta al arrancar y cada 12 horas, y además conviene entrar al panel de vez en cuando.

## Limitaciones conocidas

- La foto de perfil ya la acepta el servidor, pero **la app todavía no tiene el botón** para subirla.
- Un contratante ya puede pasar a prestador con la misma cuenta en el servidor, pero **la pantalla "Quiero ofrecer servicios" de la app no está hecha**.
- La validación de fotos es un filtro de confianza, no una verificación del trabajo.
- Las suscripciones de Aruy Pro son simuladas.

## Flujo de trabajo

El desarrollo se hace en la rama `develop` mediante *pull requests* revisados por el equipo. La rama `main` recibe `develop` cuando el proyecto está listo para publicarse, y cada versión se marca con un *release* (por ejemplo `v1.0.0`).

## Licencia y uso de IA

Licencia MIT (ver `LICENSE`). El uso de inteligencia artificial en este proyecto está declarado en `USO_IA.md`, y los prompts en `docs/prompts`.
