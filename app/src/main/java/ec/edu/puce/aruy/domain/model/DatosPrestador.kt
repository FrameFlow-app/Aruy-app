package ec.edu.puce.aruy.domain.model

/** Lo que cuenta de su oficio una persona que ya tiene cuenta y quiere empezar a ofrecer servicios. */
data class DatosPrestador(
    val oficio: String,
    val zona: String,
    val descripcion: String? = null,
    val aniosExperiencia: Int? = null
)
