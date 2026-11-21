package ec.edu.puce.aruy.data

import ec.edu.puce.aruy.data.solicitudes.ResumenSolicitudesStore
import ec.edu.puce.aruy.domain.model.ContraparteSolicitud
import ec.edu.puce.aruy.domain.model.EstadoSolicitud
import ec.edu.puce.aruy.domain.model.ResumenSolicitudes
import ec.edu.puce.aruy.domain.model.Solicitud
import ec.edu.puce.aruy.domain.model.TipoSolicitudes
import ec.edu.puce.aruy.domain.repository.SolicitudRepository
import ec.edu.puce.aruy.domain.usecase.ObtenerResumenSolicitudesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ResumenSolicitudesStoreTest {

    /** Entrega lo que se le ponga en [siguiente]; si es null, falla (como si no hubiera internet). */
    private class Resumenes : SolicitudRepository {
        var siguiente: ResumenSolicitudes? = null
        override suspend fun resumen(): ResumenSolicitudes = siguiente ?: throw java.io.IOException("sin internet")
        override suspend fun enviar(servicioId: String, mensaje: String): Solicitud = throw NotImplementedError()
        override suspend fun listar(tipo: TipoSolicitudes): List<Solicitud> = emptyList()
        override suspend fun cambiarEstado(id: String, estado: EstadoSolicitud, respuesta: String?): Solicitud = throw NotImplementedError()
        override suspend fun marcarVistas(tipo: TipoSolicitudes) = Unit
    }

    @Test
    fun refrescar_guarda_lo_que_dice_el_servidor() = runBlocking {
        val repo = Resumenes().apply { siguiente = ResumenSolicitudes(recibidasNuevas = 2, enviadasNovedades = 1) }
        val store = ResumenSolicitudesStore(ObtenerResumenSolicitudesUseCase(repo))

        store.refrescar()

        assertEquals(3, store.resumen.value.nuevas)
    }

    @Test
    fun si_no_hay_internet_se_queda_con_lo_que_habia() = runBlocking {
        val repo = Resumenes().apply { siguiente = ResumenSolicitudes(recibidasNuevas = 2) }
        val store = ResumenSolicitudesStore(ObtenerResumenSolicitudesUseCase(repo))
        store.refrescar()

        repo.siguiente = null
        store.refrescar() // falla en silencio

        assertEquals(2, store.resumen.value.recibidasNuevas)
    }

    @Test
    fun avisa_solo_cuando_aparece_algo_nuevo() = runBlocking {
        val repo = Resumenes()
        val store = ResumenSolicitudesStore(ObtenerResumenSolicitudesUseCase(repo))
        val llegadas = mutableListOf<ResumenSolicitudes>()
        // Unconfined: el que escucha empieza a recibir de inmediato, sin esperar a otro hilo
        val recolector = CoroutineScope(Dispatchers.Unconfined).launch { store.llegaron.collect { llegadas += it } }

        repo.siguiente = ResumenSolicitudes(recibidasNuevas = 1)
        store.refrescar() // 0 -> 1: llegó algo
        repo.siguiente = ResumenSolicitudes(recibidasNuevas = 1)
        store.refrescar() // igual: no avisa
        repo.siguiente = ResumenSolicitudes(recibidasNuevas = 0)
        store.refrescar() // bajó (la vio): no avisa
        repo.siguiente = ResumenSolicitudes(recibidasNuevas = 0, enviadasNovedades = 1)
        store.refrescar() // una respuesta nueva: avisa

        recolector.cancel()
        assertEquals(2, llegadas.size)
        assertEquals(1, llegadas.first().recibidasNuevas)
        assertEquals(1, llegadas.last().enviadasNovedades)
    }

    @Test
    fun limpiar_borra_los_numeritos() = runBlocking {
        val repo = Resumenes().apply { siguiente = ResumenSolicitudes(recibidasNuevas = 4) }
        val store = ResumenSolicitudesStore(ObtenerResumenSolicitudesUseCase(repo))
        store.refrescar()

        store.limpiar()

        assertEquals(ResumenSolicitudes.VACIO, store.resumen.value)
    }
}
