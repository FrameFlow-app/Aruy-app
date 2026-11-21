package ec.edu.puce.aruy.domain.repository

import ec.edu.puce.aruy.domain.model.Usuario

/** Contrato de la foto de perfil: subir una nueva o quitarla. Devuelve el usuario ya actualizado. */
interface FotoPerfilRepository {
    /** Sube una foto ya convertida a WebP; el servidor la deja en 512 px y le quita la ubicación. */
    suspend fun subirFoto(webp: ByteArray): Usuario

    suspend fun quitarFoto(): Usuario
}
