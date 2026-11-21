package ec.edu.puce.aruy.domain.validation

/** Reglas del chat (el servidor las vuelve a comprobar). */
object ValidacionesMensaje {
    const val MAX = 1000

    fun texto(valor: String): String? = when {
        valor.isBlank() -> "Escribe un mensaje."
        valor.trim().length > MAX -> "Es muy largo: máximo $MAX caracteres."
        else -> null
    }
}

/** Reglas de "Olvidé mi contraseña". La contraseña nueva usa [Validaciones.password], igual que el registro. */
object ValidacionesRecuperar {
    const val LARGO_CODIGO = 6

    fun codigo(valor: String): String? =
        if (valor.trim().length == LARGO_CODIGO && valor.trim().all(Char::isDigit)) null
        else "Escribe el código de $LARGO_CODIGO dígitos que te enviamos."

    fun confirmacion(password: String, confirmar: String): String? =
        if (password == confirmar) null else "Las contraseñas no coinciden."
}
