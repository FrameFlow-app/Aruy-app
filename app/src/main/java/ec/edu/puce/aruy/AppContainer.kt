package ec.edu.puce.aruy

import ec.edu.puce.aruy.ui.viewmodel.SolicitudesViewModel
import ec.edu.puce.aruy.notificaciones.ProgramadorAvisos
import ec.edu.puce.aruy.notificaciones.NotificadorSolicitudes
import ec.edu.puce.aruy.notificaciones.AvisosSolicitudesStore
import ec.edu.puce.aruy.domain.usecase.ObtenerSolicitudesUseCase
import ec.edu.puce.aruy.domain.usecase.ObtenerResumenSolicitudesUseCase
import ec.edu.puce.aruy.domain.usecase.MarcarSolicitudesVistasUseCase
import ec.edu.puce.aruy.domain.usecase.EnviarSolicitudUseCase
import ec.edu.puce.aruy.domain.usecase.CambiarEstadoSolicitudUseCase
import ec.edu.puce.aruy.domain.repository.SolicitudRepository
import ec.edu.puce.aruy.data.solicitudes.ResumenSolicitudesStore
import ec.edu.puce.aruy.data.repository.SolicitudRepositoryImpl
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import ec.edu.puce.aruy.data.local.BorradorStore
import ec.edu.puce.aruy.data.local.DatabaseProvider
import ec.edu.puce.aruy.data.local.PreferenciasStore
import ec.edu.puce.aruy.data.red.ConexionRed
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.repository.ActividadLocalRepository
import ec.edu.puce.aruy.data.repository.AuthRepositoryImpl
import ec.edu.puce.aruy.data.repository.CatalogoRepositoryImpl
import ec.edu.puce.aruy.data.repository.FavoritoRepository
import ec.edu.puce.aruy.data.repository.PerfilRepository
import ec.edu.puce.aruy.data.repository.PrestadorRepositoryImpl
import ec.edu.puce.aruy.data.repository.SuscripcionRepository
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.data.sesion.RenovadorSesion
import ec.edu.puce.aruy.data.sesion.SesionStore
import ec.edu.puce.aruy.domain.repository.AuthRepository
import ec.edu.puce.aruy.domain.repository.CatalogoRepository
import ec.edu.puce.aruy.domain.repository.FotoPerfilRepository
import ec.edu.puce.aruy.domain.repository.PrestadorRepository
import ec.edu.puce.aruy.domain.usecase.ActivarPerfilPrestadorUseCase
import ec.edu.puce.aruy.domain.usecase.IniciarSesionUseCase
import ec.edu.puce.aruy.domain.usecase.ObtenerCatalogoUseCase
import ec.edu.puce.aruy.domain.usecase.PublicarServicioUseCase
import ec.edu.puce.aruy.domain.usecase.QuitarFotoPerfilUseCase
import ec.edu.puce.aruy.domain.usecase.RegistrarCuentaUseCase
import ec.edu.puce.aruy.domain.usecase.SubirFotoPerfilUseCase
import ec.edu.puce.aruy.domain.usecase.SubirFotoPortafolioUseCase
import ec.edu.puce.aruy.domain.usecase.ValidarFotoServicioUseCase
import ec.edu.puce.aruy.ui.viewmodel.ActivarPrestadorViewModel
import ec.edu.puce.aruy.ui.viewmodel.ActividadLocalViewModel
import ec.edu.puce.aruy.ui.viewmodel.AjustesViewModel
import ec.edu.puce.aruy.ui.viewmodel.AruyProViewModel
import ec.edu.puce.aruy.ui.viewmodel.BajaCuentaViewModel
import ec.edu.puce.aruy.ui.viewmodel.EditarPerfilViewModel
import ec.edu.puce.aruy.ui.viewmodel.AuthViewModel
import ec.edu.puce.aruy.ui.viewmodel.BuscarViewModel
import ec.edu.puce.aruy.ui.viewmodel.CatalogoViewModel
import ec.edu.puce.aruy.ui.viewmodel.EditorServicioViewModel
import ec.edu.puce.aruy.ui.viewmodel.MisServiciosViewModel
import ec.edu.puce.aruy.ui.viewmodel.fabricaVm
import ec.edu.puce.aruy.util.ConversorWebp
import ec.edu.puce.aruy.util.EtiquetadorFotos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Inyección de dependencias manual: crea una sola vez los repositorios y las fábricas de ViewModel.
 * Las demás fases agregan aquí sus repositorios (fotos, perfil...).
 */
class AppContainer(contexto: Context) {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = DatabaseProvider.getDatabase(contexto.applicationContext)

    val preferencias = PreferenciasStore(contexto.applicationContext)

    val conexion = ConexionRed(contexto)

    val sesionManager = SesionManager(SesionStore(contexto.applicationContext), alcance)

    private val cliente = RetrofitClient(
        token = sesionManager::tokenActual,
        alExpirar = sesionManager::expirar
    )

    // Fotos: se convierten a WebP y se etiquetan con ML Kit en el teléfono antes de subirse
    val conversorWebp = ConversorWebp(contexto.applicationContext)
    val etiquetadorFotos = EtiquetadorFotos()
    val borradorStore = BorradorStore(contexto.applicationContext)

    // Los ViewModels y los casos de uso solo conocen las interfaces del dominio; aquí se elige la implementación
    val renovadorSesion = RenovadorSesion(cliente.authApi, sesionManager)

    // Solicitudes dentro de la app y sus avisos
    val solicitudRepository: SolicitudRepository = SolicitudRepositoryImpl(cliente.solicitudApi)
    val enviarSolicitudCaso = EnviarSolicitudUseCase(solicitudRepository)
    val obtenerSolicitudesCaso = ObtenerSolicitudesUseCase(solicitudRepository)
    val cambiarEstadoSolicitudCaso = CambiarEstadoSolicitudUseCase(solicitudRepository)
    val obtenerResumenSolicitudesCaso = ObtenerResumenSolicitudesUseCase(solicitudRepository)
    val marcarSolicitudesVistasCaso = MarcarSolicitudesVistasUseCase(solicitudRepository)
    val resumenSolicitudes = ResumenSolicitudesStore(obtenerResumenSolicitudesCaso)
    val avisosSolicitudesStore = AvisosSolicitudesStore(contexto.applicationContext)
    val notificadorSolicitudes = NotificadorSolicitudes(contexto.applicationContext)
    val programadorAvisos = ProgramadorAvisos(contexto.applicationContext)

    val authRepository: AuthRepository = AuthRepositoryImpl(cliente.authApi, sesionManager)
    val prestadorRepository: PrestadorRepository = PrestadorRepositoryImpl(cliente.prestadorApi, cliente.servicioApi)
    val perfilRepository = PerfilRepository(cliente.usuarioApi, cliente.prestadorApi)
    val suscripcionRepository = SuscripcionRepository(cliente.suscripcionApi)
    val catalogoRepository: CatalogoRepository =
        CatalogoRepositoryImpl(cliente.servicioApi, cliente.perfilApi, cliente.calificacionApi)
    val favoritoRepository = FavoritoRepository(database.favoritoDao())
    val actividadLocalRepository = ActividadLocalRepository(
        database.bloqueadoDao(), database.contactoDao(), database.reporteDao(), database.vistoDao()
    )

    // Casos de uso: cada intención de la persona vive en su propia clase, con sus reglas de negocio
    val iniciarSesionCaso = IniciarSesionUseCase(authRepository)
    val registrarCuentaCaso = RegistrarCuentaUseCase(authRepository)
    val activarPerfilPrestadorCaso = ActivarPerfilPrestadorUseCase(authRepository)
    val fotoPerfilRepository: FotoPerfilRepository = perfilRepository
    val subirFotoPerfilCaso = SubirFotoPerfilUseCase(fotoPerfilRepository)
    val quitarFotoPerfilCaso = QuitarFotoPerfilUseCase(fotoPerfilRepository)
    val obtenerCatalogoCaso = ObtenerCatalogoUseCase(catalogoRepository)
    val validarFotoCaso = ValidarFotoServicioUseCase()
    val publicarServicioCaso = PublicarServicioUseCase(prestadorRepository)
    val subirFotoCaso = SubirFotoPortafolioUseCase(prestadorRepository, validarFotoCaso)

    val authViewModelFactory = AuthViewModel.fabrica(authRepository, iniciarSesionCaso, registrarCuentaCaso, conversorWebp, subirFotoPerfilCaso, sesionManager)
    val catalogoViewModelFactory: ViewModelProvider.Factory =
        fabricaVm { CatalogoViewModel(catalogoRepository, obtenerCatalogoCaso, actividadLocalRepository, sesionManager) }
    val buscarViewModelFactory: ViewModelProvider.Factory = fabricaVm { BuscarViewModel() }
    val solicitudesViewModelFactory: ViewModelProvider.Factory = fabricaVm {
        SolicitudesViewModel(obtenerSolicitudesCaso, cambiarEstadoSolicitudCaso, marcarSolicitudesVistasCaso, resumenSolicitudes, sesionManager)
    }
    val activarPrestadorViewModelFactory: ViewModelProvider.Factory =
        fabricaVm { ActivarPrestadorViewModel(activarPerfilPrestadorCaso) }
    val misServiciosViewModelFactory: ViewModelProvider.Factory =
        fabricaVm { MisServiciosViewModel(prestadorRepository, subirFotoCaso, sesionManager, conversorWebp, etiquetadorFotos) }

    val editarPerfilViewModelFactory: ViewModelProvider.Factory =
        fabricaVm { EditarPerfilViewModel(perfilRepository, sesionManager, conversorWebp) }
    val bajaCuentaViewModelFactory: ViewModelProvider.Factory =
        fabricaVm {
            BajaCuentaViewModel(perfilRepository, sesionManager) { usuarioId ->
                favoritoRepository.borrarTodos(usuarioId)
                actividadLocalRepository.borrarTodo(usuarioId)
                borradorStore.borrar(usuarioId)
            }
        }
    val aruyProViewModelFactory: ViewModelProvider.Factory = fabricaVm { AruyProViewModel(suscripcionRepository) }
    val ajustesViewModelFactory: ViewModelProvider.Factory = fabricaVm { AjustesViewModel(preferencias) }

    fun editorViewModelFactory(servicioId: String?): ViewModelProvider.Factory = fabricaVm {
        EditorServicioViewModel(
            servicioId, prestadorRepository, publicarServicioCaso, validarFotoCaso, sesionManager, conversorWebp, etiquetadorFotos, borradorStore
        )
    }
    val actividadLocalViewModelFactory: ViewModelProvider.Factory =
        fabricaVm { ActividadLocalViewModel(favoritoRepository, actividadLocalRepository, sesionManager) }

    init {
        // Lee la sesión guardada; hasta entonces el estado es "Cargando" y se ve el splash
        // y, si al token le quedan pocos días, se renueva solo
        alcance.launch {
            sesionManager.cargar()
            renovadorSesion.renovarSiHaceFalta()
        }
        // Despierta el servidor gratuito mientras se ve el splash (se duerme tras 15 min sin uso)
        alcance.launch { cliente.despertarServidor() }
    }
}
