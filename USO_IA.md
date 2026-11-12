# Uso de inteligencia artificial: equipo FrameFlow

Declaramos el uso de IA en este proyecto. Los datos de entrevistas y encuestas salen de personas reales; la IA no los inventó.

| Herramienta | Para qué se usó | Qué revisó el equipo |
|---|---|---|
| Claude (Anthropic) | Identificar plataformas competidoras y redactar preguntas de encuesta (dossier). | Se comprobó que las plataformas existen y operan en Ecuador y se contrastó con la experiencia de un integrante. |
| Claude (Anthropic) | Ordenar y redactar el documento técnico, los formatos 3, 5, 6 y 12, la landing page y la política de privacidad del Control 2, a partir de las fichas y encuestas reales del equipo. | El equipo revisó cada cifra contra sus fichas y el repositorio. |
| Claude (Anthropic), en claude.ai | Apoyo en la guía de arquitectura y en el código de la app y del servidor: el servidor (Node.js y Express), sus pruebas automáticas, la validación de las fotos con ML Kit, la configuración de Render y los pasos de Git. | Todo el código se revisó y probó; cada integrante puede explicarlo. Cada cambio entra por pull request, con pruebas automáticas y prueba en un celular real. |

## IA dentro del producto

La app usa **ML Kit Image Labeling** de Google, que se ejecuta **en el teléfono**: las fotos no se envían a ningún servicio de IA. Se usa para sugerir una categoría y para comprobar que la foto de un trabajo corresponda al servicio publicado. Es un filtro de confianza con límites conocidos (el modelo base solo conoce unas 430 etiquetas generales); no prueba que el trabajo sea real. Detalles en `README.md`.

Los prompts usados están en `/docs/prompts`.

Fecha de la última actualización: 11 de noviembre de 2026
