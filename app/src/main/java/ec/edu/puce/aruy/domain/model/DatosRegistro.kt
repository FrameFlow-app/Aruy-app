package ec.edu.puce.aruy.domain.model

import java.time.LocalDate

/** Datos del formulario de registro. Los de prestador solo se usan si [rol] es PRESTADOR. */
data class DatosRegistro(
    val nombre: String,
    val correo: String,
    val celular: String,
    val password: String,
    val rol: RolUsuario,
    val oficio: String? = null,
    val zona: String? = null,
    /** Aruy es solo para mayores de 18 años; el caso de uso lo exige. */
    val fechaNacimiento: LocalDate? = null
)
