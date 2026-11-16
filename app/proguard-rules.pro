# Reglas de R8 para el release (minificado + reducción de recursos).

# Gson y Retrofit leen genéricos y anotaciones por reflexión: sin estos atributos fallan al deserializar.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations

# DTOs de la API (Gson con @SerializedName): los nombres de campo deben conservarse.
-keep class ec.edu.puce.aruy.data.remote.dto.** { *; }

# Clases que Gson guarda/lee como JSON en DataStore (se serializan por nombre de campo, sin @SerializedName).
-keep class ec.edu.puce.aruy.data.local.BorradorServicio { *; }
-keep class ec.edu.puce.aruy.data.sesion.SesionStore$UsuarioGuardado { *; }

# Enums de la app: Gson/valueOf los resuelven por nombre.
-keepclassmembers enum ec.edu.puce.aruy.** { *; }
