package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** Fábrica de un ViewModel que no necesita más que lo que ya tiene el [ec.edu.puce.aruy.AppContainer]. */
inline fun <reified T : ViewModel> fabricaVm(crossinline crear: () -> T): ViewModelProvider.Factory =
    viewModelFactory { initializer { crear() } }

/**
 * Fábrica de un ViewModel que además guarda su estado simple en un [SavedStateHandle].
 * Así el estado sobrevive a la muerte del proceso (Android cierra la app en segundo plano y la restaura).
 */
inline fun <reified T : ViewModel> fabricaVmConEstado(crossinline crear: (SavedStateHandle) -> T): ViewModelProvider.Factory =
    viewModelFactory { initializer { crear(createSavedStateHandle()) } }
