package ec.edu.puce.aruy.data.remote

import ec.edu.puce.aruy.BuildConfig
import ec.edu.puce.aruy.data.remote.api.AuthApi
import ec.edu.puce.aruy.data.remote.api.CalificacionApi
import ec.edu.puce.aruy.data.remote.api.PerfilApi
import ec.edu.puce.aruy.data.remote.api.PrestadorApi
import ec.edu.puce.aruy.data.remote.api.ServicioApi
import ec.edu.puce.aruy.data.remote.api.SuscripcionApi
import ec.edu.puce.aruy.data.remote.api.UsuarioApi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP de la app. Recibe de dónde sacar el token JWT y qué hacer si el servidor lo rechaza,
 * para no depender de la sesión directamente.
 */
class RetrofitClient(
    token: () -> String?,
    alExpirar: () -> Unit,
    // Definida en app/build.gradle.kts (buildConfigField BASE_URL, según el flavor emulador/celular)
    private val baseUrl: String = BuildConfig.BASE_URL
) {

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            // BASIC no imprime cabeceras: el token JWT nunca llega al log
            level = HttpLoggingInterceptor.Level.BASIC
        }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(token, alExpirar))
        .addInterceptor(loggingInterceptor)
        // El servidor gratuito (Render) se duerme tras 15 min sin uso y tarda hasta ~50 s en despertar.
        // Con 15 s la app se rendía antes y mostraba un error falso: se espera hasta 60 s.
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)

    val usuarioApi: UsuarioApi = retrofit.create(UsuarioApi::class.java)

    val servicioApi: ServicioApi = retrofit.create(ServicioApi::class.java)

    val perfilApi: PerfilApi = retrofit.create(PerfilApi::class.java)

    val calificacionApi: CalificacionApi = retrofit.create(CalificacionApi::class.java)

    val prestadorApi: PrestadorApi = retrofit.create(PrestadorApi::class.java)

    val suscripcionApi: SuscripcionApi = retrofit.create(SuscripcionApi::class.java)

    /**
     * Despierta el servidor (GET /health) mientras se ve la pantalla de inicio, para que ya esté listo
     * cuando la persona llegue al catálogo. Bloquea el hilo: llamar fuera del hilo principal. Nunca lanza error.
     */
    fun despertarServidor() {
        runCatching {
            val peticion = Request.Builder().url(baseUrl.trimEnd('/') + "/health").build()
            okHttpClient.newCall(peticion).execute().close()
        }
    }
}
