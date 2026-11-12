# Prompts de Alisson Basantes (Claude, en claude.ai)

Herramienta: **Claude (Anthropic)**, en claude.ai, en un solo chat llamado "Proyecto Android con IA" (más de 500 mensajes).

**Cómo leer este archivo.** Son mensajes reales de Alisson, copiados **tal como los escribió** (con sus errores de ortografía). No es el chat completo: es una selección de los que cambiaron el proyecto. Para cada uno se indica qué resultó y cómo se comprobó. El chat entero se puede exportar desde claude.ai si hace falta.

---

## 1. Inicio: documento técnico y arquitectura

**Prompt (primer mensaje del chat):**

> hola mira este es un proyecto que vamos a iniciar entonces con mi compañeros tenemos esto
> ignora la aprte de wireframe /mockups mejor coloclar los componentes de inteligencia modelo de la IA procesado justificacion tecnica estrategia de datos y presistencia. 9 cronograma de integracion tecnologica considetnado desde la semana 8. 10 consideraciones tecnicas, cicli de vida y UX/UI manejo de cicli de vida concurrencia e hilos diseño adaptable y accesibilidad tambien basate con los arvhicos que te eh dado https://developer.android.com/kotlin/style-guide?hl=es-419
> https://developer.android.com/kotlin/style-guide?hl=es-419
> https://developer.android.com/topic/architecture?hl=es-419
> https://developer.android.com/guide/components/activities/activity-lifecycle?hl=es-419
> https://developer.android.com/codelabs/predictive-back?hl=es-419#0
> igual necesito que ingresos a esos links leas y tambien tomes las cosas importantes porfavor enviame por aqui no me envies por archivo por favor

**Qué resultó:** apoyo para redactar las secciones técnicas del documento del proyecto, basadas en las guías oficiales de Android.
**Cómo se comprobó:** el equipo revisó el contenido contra las guías citadas y contra su repositorio.

## 2. Llevar el servidor a la nube (28 de octubre de 2026)

**Prompt:**

> ahora veras necesito que me ayudes en algo importante esta en local verdad por eso no sirve hay que suber en la nube pues para que pueda funcionar cuando saque al celular no crees osea para que sirvar sin estar conectando la base de datos

**Qué resultó:** pasos para desplegar el servidor (`arruy-backend`) en Render, con las variables de entorno y la comprobación de salud (`/health`).
**Cómo se comprobó:** se abrieron `/health`, `/servicios` y `/docs` en https://aruy-api.onrender.com, y se instaló la app en un celular apuntando a esa dirección.

**Prompt (mismo día):**

> lenguaje me dice docker o que pongo para la app movil veras que esto debe estar configurado para la app movil

**Qué resultó:** aclaración de que Render solo aloja el servidor (se elige Node, no Docker), y que la app se conecta con la dirección del servidor.

## 3. APK sin firma de publicación (30 de octubre de 2026)

**Prompt:**

> quiero sacar el apk desde el andriod sin clave dame los pasos por fa

**Qué resultó:** pasos para generar un APK de depuración (se firma solo, sin pedir claves).
**Cómo se comprobó:** instalación en un celular real.

## 4. La sesión no se conserva (10 de noviembre de 2026)

**Prompt:**

> veras necesito hacer cambios porque la parte cuando ingresaba a la aplicacion ya me registro ya subo mi oficio pero ya salgo de la app y vuelvo ingresar y no se queda con el perfil que ingrese entonces esa parte debemos arreglar dime que opines todavia no hagas nada

**Qué resultó:** revisión de solo lectura del código de la sesión y una lista de causas posibles. Se agregaron mensajes de diagnóstico (etiqueta `AruySesion` en Logcat) y la sesión del servidor pasó de 8 horas a 30 días.
**Estado:** la causa exacta de que la app pida iniciar sesión otra vez **todavía no está confirmada**; falta leer esos mensajes en un celular conectado.

## 5. Revisión del servidor y estabilidad (10 de noviembre de 2026)

**Prompt:**

> aparte estoy iniciando seccion y ya pongo todo correcto me sale el correo o la contraseña no son correctos entonces algo esta igual cuando subo las fotos no identifica con la IA LM KIT para que lea las fotos si son de los trabajos que se realiza ose no tiene nada la aplicacion quiero que ingreses al programa revises y tambien apliques la teoria el contenedor de archivos tambien de infomracion veridica para poder subir esto por favor C:\Users\ALISSON BASANTES\Desktop\6.-Sexto Semestre\programacion movil\Programas\Aruy_Proyecto ingresa a mi computadora y revisa tambien si enceuntras errores avisame del backend tambien quiero cambiar de servidor a azure porque este servidor render se cae mucho y cada rato y no quiero eso por favor ayudame con eso

**Qué resultó:** revisión del servidor y una lista de errores. Se corrigieron: límites de velocidad por cuenta y no solo por IP, memoria de las imágenes (fila de espera), errores no capturados y cierre ordenado. Sobre Azure, se explicó que primero había que saber por qué se caía Render (los registros mostraron que solo se dormía) y se dejó para después del concurso.
**Cómo se comprobó:** 20 pruebas automáticas del servidor (`npm test`) y revisión de los registros de Render.

**Prompt:**

> la cosa es que no duerma osea si se abre la apa que se demore unos 30seg en cargar en encender no 50 segundo eso es mucho tiempo

**Qué resultó:** un monitor de UptimeRobot que consulta `/health` cada 5 minutos, y en la app una espera de hasta 60 s con aviso al servidor al abrirla.
**Cómo se comprobó:** los registros de Render mostraron más de 32 horas sin reinicios.

## 6. Una sola cuenta y pestaña Explorar (11 de noviembre de 2026)

**Prompt:**

> y cachas que tambien hay que crear una cuenta para el que contrata ya con correo deberia hacerse osea literal esta horrible esa app no tiene organizacion creo yo  con el mismo correo se debe ingresar no cress no crear como dos correos que seria algo inchoherente osea hay que arreglar para que el usaurio no haga doble trabajo y si el grupo dijo que agreguemos la opcion de explorar porque a las personas les gusta escrolliar aun no cooro eso de las 19 pruebas hasta que arreglemos esto teniendo todo ahi si para hacer eso de corre .\gradlew.bat testCelularDebugUnitTest --tests "*ValidadorFotoOficioTest*" y eso de la IA que mismo funciona o no ??

**Qué resultó:** en el servidor, `POST /perfiles/activar` (un contratante pasa a prestador con el mismo correo) y permiso para que un prestador califique a otros (no a sí mismo); en la app, la pestaña **Explorar** para el prestador.
**Estado:** falta la pantalla de la app para activar el perfil de prestador.
**Cómo se comprobó:** pruebas automáticas del servidor.

## 7. La IA de las fotos debe usar el título del servicio (11 de noviembre de 2026)

**Prompt:**

> eso pasa cuando subo una foto me sale foto agregada a tu portafolio Carpitenria y esa foto es de un libro osea no es ta funcionando debe funcionar tiene que valer eso de la IA

**Prompt:**

> pero si no detecta deberia salir error de foto no se detecta el trabajo ahora por ejemplo esta bien ve una mesa es carpenteria pero si sale libro que saldria veras que hay algunos trabajos por eso tenemos como opcion otros porque ejemplo mantenimiento de maquinas o desarrollo web pero creo que deberias basarte en el titulo del servicio que pone el usuario por ejemplo Limpieza de casas que debe ser una señora limpiando o una casa o tambien si esta barriendo es limpieza si me entiendes no creo que sea en este caso construir un modelo propio

**Qué resultó:** la validación de fotos se rediseñó para basarse en el título y la descripción del servicio y en la lista oficial de etiquetas de ML Kit (`ValidadorFotoOficio.kt`). Si la foto no tiene ninguna etiqueta esperada, se rechaza con un mensaje.
**Cómo se comprobó:** 20 pruebas unitarias de la app, una simulación de la lógica con 24 casos y la lista oficial de etiquetas, y prueba en un celular real (Alisson confirmó que funciona).

## 8. APK para el equipo y requisitos de entrega (11 de noviembre de 2026)

**Prompt:**

> para decargar el apk para pasarle a mis compañeros pero que si balga porque la otra ves me diste y no les valia en su celular

**Qué resultó:** se encontró que el APK de *Run* solo traía el procesador del celular conectado; se indicó generar el APK con `.\gradlew.bat assembleCelularDebug`.

**Prompt (con la captura del requisito "Repositorio con el release v1.0.0, README completo, LICENSE MIT y, si usaron IA, el archivo USO_IA.md y la carpeta /docs/prompts"):**

> esto me tocaba hacer a mi no hicimos o si ??

**Prompt:**

> si pofa haz lo que puedas hacer , Lo que tienen que hacer ustedes eso que me toca hacer me dices paso a paso para realizar bien porfa

**Qué resultó:** revisión del repositorio, un README completo, el LICENSE en la raíz, `USO_IA.md` completado y este archivo.
