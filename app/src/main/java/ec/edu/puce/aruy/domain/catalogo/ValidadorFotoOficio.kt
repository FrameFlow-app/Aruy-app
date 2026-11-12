package ec.edu.puce.aruy.domain.catalogo

import ec.edu.puce.aruy.domain.model.CategoriaServicio
import java.text.Normalizer
import java.util.Locale

/** Resultado de revisar si una foto corresponde al servicio. */
sealed interface ResultadoFoto {
    object Aceptada : ResultadoFoto

    /** [mensaje] se muestra tal cual a la persona; [detectado] son las etiquetas (en español) que vio la IA. */
    data class Rechazada(val mensaje: String, val detectado: List<String>) : ResultadoFoto
}

/**
 * Confianza ante todo: la foto tiene que corresponder al servicio que la persona publicó.
 *
 * El modelo base de ML Kit solo conoce unas 400 etiquetas generales (no existen "libro", "escoba", "tubería" ni
 * "limpieza"), así que se usan las que SÍ existen como pistas del lugar o la escena de cada oficio.
 *
 * 1. Se mira el TÍTULO y la descripción del servicio (y la categoría) para saber qué se espera ver:
 *    "Limpieza de casas" -> una casa por dentro (cuarto, cocina, baño...); "Desarrollo de apps" -> pantallas, etc.
 * 2. Si el servicio es de un tipo conocido, la foto debe tener AL MENOS UNA etiqueta esperada.
 *    Si no hay ninguna (o no se reconoció nada), se rechaza: "No pudimos reconocer ... Intenta con otra imagen".
 * 3. Si el título no corresponde a ningún tipo conocido, se aplica una regla suave: solo se rechaza lo claramente
 *    ajeno (selfie, comida, mascotas, calle, pantallas...).
 * Una selfie y una calle (sin obra) se rechazan siempre. Es una ayuda de confianza, no una prueba de que el trabajo
 * sea real.
 */
object ValidadorFotoOficio {

    /** Pistas de "una casa por dentro o una obra": existen en el modelo y no aparecen en cualquier foto. */
    private val ESCENA_HOGAR = setOf(
        "room", "bedroom", "bathroom", "kitchen", "sink", "countertop", "cabinetry", "wall", "roof", "stairs",
        "handrail", "tile", "brick", "construction", "curtain", "couch", "loveseat", "bunk bed", "lampshade",
        "porcelain", "bathing", "cushion", "pillow"
    )

    /** Etiquetas esperadas en un servicio de belleza. */
    private val ETIQUETAS_BELLEZA = setOf(
        "hair", "nail", "eyelash", "lipstick", "skin", "beard", "bangs", "moustache", "tattoo", "mouth", "ear",
        "toe", "foot", "hand", "flesh", "jewellery"
    )

    /**
     * [retratos]: se permiten selfis y primeros planos (belleza, eventos). [calle]: la calle es parte del trabajo
     * (mecánica, transporte), así que no se rechaza por eso.
     */
    private class Familia(
        val raices: List<String>,
        val exactas: Set<String>,
        val etiquetas: Set<String>,
        val retratos: Boolean = false,
        val calle: Boolean = false
    )

    /** Tipos de servicio que se reconocen por palabras del título o la descripción (sin tildes ni mayúsculas). */
    private val FAMILIAS = listOf(
        // Limpieza del hogar
        Familia(
            raices = listOf("limpi", "aseo", "barrer", "barrid", "trape", "lavand", "planch", "domestic", "empleada", "quehacer"),
            exactas = emptySet(),
            etiquetas = ESCENA_HOGAR
        ),
        // Arreglos, reparaciones y mantenimiento del hogar ("casa" u "hogar" solos no bastan: "Limpieza de casas" es de limpieza)
        Familia(
            raices = listOf(
                "arregl", "repar", "manten", "remodel", "instal", "construc", "albanil", "pint", "plom", "gasfit",
                "electric", "carpint", "cerraj", "techo", "vivienda", "obra"
            ),
            exactas = setOf("piso", "pared", "tuberia"),
            etiquetas = ESCENA_HOGAR + setOf("metal", "wheelbarrow", "soil", "sand", "rock", "garden", "flowerpot", "plant")
        ),
        // Jardinería
        Familia(
            raices = listOf("jardin", "jardiner", "planta", "podar", "cesped", "paisaj"),
            exactas = emptySet(),
            etiquetas = setOf("garden", "plant", "flower", "flowerpot", "soil", "wheelbarrow", "branch", "twig", "petal", "flora", "field", "farm", "ranch", "barn")
        ),
        // Belleza
        Familia(
            raices = listOf("peluquer", "barber", "maquill", "manicur", "pedicur", "estetic", "cabello", "depil", "ceja", "pestan", "belleza", "unas"),
            exactas = setOf("corte", "cortes"),
            etiquetas = ETIQUETAS_BELLEZA,
            retratos = true
        ),
        // Tecnología
        Familia(
            raices = listOf("aplicac", "softwar", "program", "pagina", "computad", "laptop", "celular", "movil", "informat", "digital", "ciber", "tecnolog", "internet", "hosting", "ordenador"),
            exactas = setOf("app", "apps", "pc", "web", "ti"),
            etiquetas = setOf("computer", "screenshot", "web page", "television", "mobile phone", "icon", "presentation", "whiteboard", "blackboard", "desk", "class")
        ),
        // Mecánica, máquinas y vehículos
        Familia(
            raices = listOf("mecanic", "taller", "maquina", "motor", "vehicul", "soldad", "torner", "industrial", "refriger"),
            exactas = setOf("auto", "autos", "carro", "carros", "moto", "motos"),
            etiquetas = setOf("metal", "chain", "wheel", "tire", "vehicle", "car", "van", "motorcycle", "tractor", "bicycle", "factory", "windshield", "bumper", "odometer", "infrastructure", "fire"),
            calle = true
        ),
        // Comida y cocina
        Familia(
            raices = listOf("cocin", "chef", "catering", "reposter", "pastel", "panader", "comida", "almuerz", "torta", "gastronom"),
            exactas = emptySet(),
            etiquetas = setOf("food", "cuisine", "cake", "bread", "pizza", "cookie", "pie", "meal", "supper", "lunch", "coffee", "juice", "cutlery", "tableware", "tablecloth", "kitchen", "cookware and bakeware", "icing", "pasteles", "fruit", "vegetable", "sushi", "gelato", "bento", "cheeseburger", "hot dog", "fast food", "eating")
        ),
        // Costura y ropa
        Familia(
            raices = listOf("costur", "sastr", "modist", "confecc", "bordad", "tejid", "zapat"),
            exactas = setOf("ropa"),
            etiquetas = setOf("textile", "cotton", "wool", "needlework", "knitting", "crochet", "quilting", "dress", "jeans", "denim", "leather", "jacket", "outerwear", "shoe", "sneakers", "tights", "sari", "gown", "blazer", "tuxedo", "scarf", "hat", "cap", "beanie")
        ),
        // Mascotas
        Familia(
            raices = listOf("veterin", "mascota", "perro", "gato", "canin"),
            exactas = emptySet(),
            etiquetas = setOf("dog", "cat", "pet", "horse", "bird", "fur", "cage", "dalmatian", "shetland sheepdog", "basset hound", "cairn terrier", "ragdoll", "sphynx", "pixie-bob")
        ),
        // Eventos, música y fotografía
        Familia(
            raices = listOf("evento", "fiesta", "fotograf", "musica", "animac", "decorac", "boda", "cumple", "bailar"),
            exactas = setOf("dj"),
            etiquetas = setOf("party", "event", "concert", "dance", "deejay", "musician", "piano", "musical instrument", "musical", "balloon", "marriage", "bride", "groom", "prom", "carnival", "fireworks", "crowd", "singer", "ballroom"),
            retratos = true,
            calle = true
        ),
        // Clases y educación
        Familia(
            raices = listOf("clase", "tutor", "profesor", "ensenanz", "academ", "refuerz", "idioma"),
            exactas = emptySet(),
            etiquetas = setOf("class", "school", "whiteboard", "blackboard", "presentation", "desk", "chair", "computer", "paper", "fiction", "community", "team", "crew", "interaction")
        ),
        // Cuidado de personas
        Familia(
            raices = listOf("ninera", "cuidado", "bebe", "anciano", "enfermer"),
            exactas = emptySet(),
            etiquetas = setOf("baby", "grandparent", "toy", "stuffed toy", "plush", "bedroom", "couch", "cushion", "pillow", "playground", "love")
        ),
        // Transporte y mudanzas
        Familia(
            raices = listOf("mudanza", "transport", "flete", "delivery", "taxi"),
            exactas = emptySet(),
            etiquetas = setOf("van", "car", "vehicle", "bus", "trunk", "bicycle", "motorcycle", "tractor", "rickshaw", "bag", "wheel", "tire"),
            calle = true
        )
    )

    /** Qué se espera ver según el oficio elegido (categoría). */
    private fun etiquetasDeCategoria(categoria: CategoriaServicio?): Set<String> = when (categoria) {
        CategoriaServicio.PLOMERIA -> ESCENA_HOGAR + setOf("metal")
        CategoriaServicio.ELECTRICIDAD -> ESCENA_HOGAR + setOf("metal", "neon", "infrastructure")
        CategoriaServicio.ALBANILERIA -> ESCENA_HOGAR + setOf("wheelbarrow", "soil", "sand", "rock", "infrastructure", "factory")
        CategoriaServicio.PINTURA -> ESCENA_HOGAR + setOf("watercolor paint", "roller")
        CategoriaServicio.CARPINTERIA -> ESCENA_HOGAR + setOf("drawer", "shelf", "desk", "chair", "bench", "armrest")
        CategoriaServicio.BELLEZA -> ETIQUETAS_BELLEZA
        CategoriaServicio.OTROS, null -> emptySet()
    }

    /** Calle y ciudad (etiqueta exacta): una foto con esto se rechaza, salvo que haya una obra o sea mecánica/transporte/eventos. */
    private val CALLE = setOf("road", "asphalt", "skyline", "skyscraper")

    /** Nombres en español de las etiquetas que más se muestran. */
    private val NOMBRES: Map<String, String> = mapOf(
        "selfie" to "Selfi", "person" to "Persona", "smile" to "Sonrisa", "laugh" to "Risa", "crowd" to "Multitud",
        "computer" to "Computadora", "screenshot" to "Captura de pantalla", "web page" to "Página web", "television" to "Televisión",
        "mobile phone" to "Celular", "icon" to "Ícono", "presentation" to "Presentación", "poster" to "Afiche", "menu" to "Menú",
        "paper" to "Papel", "fiction" to "Libro o texto", "news" to "Noticias", "newspaper" to "Periódico", "receipt" to "Recibo",
        "comics" to "Historieta", "product" to "Producto", "desk" to "Escritorio", "chair" to "Silla", "tableware" to "Vajilla",
        "tablecloth" to "Mantel", "road" to "Calle", "asphalt" to "Asfalto", "skyline" to "Paisaje urbano", "skyscraper" to "Rascacielos",
        "sky" to "Cielo", "building" to "Edificio", "infrastructure" to "Infraestructura", "tower" to "Torre", "park" to "Parque",
        "food" to "Comida", "cuisine" to "Comida", "eating" to "Comiendo", "pizza" to "Pizza", "cake" to "Pastel", "coffee" to "Café",
        "cat" to "Gato", "dog" to "Perro", "pet" to "Mascota", "bird" to "Ave", "horse" to "Caballo", "plant" to "Planta", "flower" to "Flor",
        "garden" to "Jardín", "mountain" to "Montaña", "beach" to "Playa", "lake" to "Lago", "sunset" to "Atardecer",
        "party" to "Fiesta", "event" to "Evento", "toy" to "Juguete", "dress" to "Vestido", "hair" to "Cabello", "skin" to "Piel",
        "room" to "Cuarto", "bedroom" to "Dormitorio", "bathroom" to "Baño", "kitchen" to "Cocina", "wall" to "Pared", "sink" to "Lavabo",
        "tile" to "Baldosa", "brick" to "Ladrillo", "roof" to "Techo", "stairs" to "Escalera", "couch" to "Sofá", "curtain" to "Cortina"
    )

    /**
     * [titulo] y [descripcion] son los del servicio; con ellos se sabe qué fotos se esperan.
     */
    fun evaluar(
        etiquetas: List<EtiquetaFoto>,
        categoria: CategoriaServicio?,
        titulo: String = "",
        descripcion: String = ""
    ): ResultadoFoto {
        val originales = etiquetas.map { it.original.lowercase(Locale.ROOT).trim() }
        val tokens = tokensDe("$titulo $descripcion")
        val familias = FAMILIAS.filter { f -> tokens.any { t -> t in f.exactas || f.raices.any { t.startsWith(it) } } }
        val esperadas = (etiquetasDeCategoria(categoria) + familias.flatMap { it.etiquetas }).toSet()

        val permiteRetratos = categoria == CategoriaServicio.BELLEZA || familias.any { it.retratos }
        val permiteCalle = familias.any { it.calle }
        val hayRetrato = "selfie" in originales && !permiteRetratos
        val hayCalle = !permiteCalle && "construction" !in originales && originales.any { it in CALLE }

        val acepta = if (esperadas.isNotEmpty()) {
            // Servicio conocido: tiene que verse algo de lo esperado, y ni una selfie ni una calle
            etiquetas.isNotEmpty() && originales.any { it in esperadas } && !hayRetrato && !hayCalle
        } else {
            // Servicio sin tipo conocido ("Otros"): solo se rechaza lo claramente ajeno
            etiquetas.isEmpty() || (!hayRetrato && !hayCalle && !hayAjenaSinTrabajo(originales))
        }
        if (acepta) return ResultadoFoto.Aceptada

        if (etiquetas.isEmpty()) {
            return ResultadoFoto.Rechazada("No pudimos reconocer qué muestra la foto. Intenta con otra imagen más clara.", emptyList())
        }
        val detectado = etiquetas
            .sortedByDescending { it.confianza }
            .take(3)
            .map { nombreEnEspanol(it.original.lowercase(Locale.ROOT).trim(), it) }
            .distinct()
        val nombre = titulo.trim().take(40)
        val de = when {
            nombre.isNotEmpty() -> " de «$nombre»"
            categoria != null && categoria != CategoriaServicio.OTROS -> " de ${categoria.etiqueta.lowercase(ES)}"
            else -> ""
        }
        val mensaje = "No pudimos reconocer en esta foto un trabajo$de. Detectamos: ${detectado.joinToString(", ")}. Intenta con otra imagen."
        return ResultadoFoto.Rechazada(mensaje, detectado)
    }

    /** Servicios sin tipo conocido: ¿hay algo claramente ajeno y ninguna pista de trabajo? */
    private fun hayAjenaSinTrabajo(originales: List<String>): Boolean {
        if (originales.any { it in ESCENA_HOGAR }) return false
        return originales.any { it in AJENAS_CLARAS }
    }

    private val AJENAS_CLARAS = setOf(
        "selfie", "crowd", "food", "cuisine", "eating", "pizza", "sushi", "cake", "cookie", "juice", "coffee", "bread", "fruit", "vegetable",
        "cat", "dog", "pet", "bird", "horse", "cattle", "bull", "insect", "butterfly", "party", "concert", "circus", "casino", "nightclub",
        "fireworks", "christmas", "toy", "stuffed toy", "plush", "comics", "poster", "menu", "screenshot", "web page", "computer", "television",
        "mobile phone", "receipt", "passport", "money", "newspaper", "news", "paper", "fiction", "dress", "jeans", "swimwear", "tuxedo",
        "soccer", "badminton", "marathon", "surfing", "skateboarder", "racing", "stadium", "road", "asphalt", "skyline", "skyscraper",
        "bus", "car", "van", "airplane", "boat", "bicycle", "motorcycle"
    )

    private fun tokensDe(texto: String): List<String> {
        val limpio = Normalizer.normalize(texto.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        return limpio.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
    }

    /** Nombre en español de lo que vio la IA (para mostrarlo a la persona). */
    fun nombrePara(etiqueta: EtiquetaFoto): String = nombreEnEspanol(etiqueta.original.lowercase(Locale.ROOT).trim(), etiqueta)

    private fun nombreEnEspanol(texto: String, etiqueta: EtiquetaFoto): String =
        NOMBRES[texto] ?: etiqueta.texto

    private val ES: Locale = Locale.forLanguageTag("es")
}
