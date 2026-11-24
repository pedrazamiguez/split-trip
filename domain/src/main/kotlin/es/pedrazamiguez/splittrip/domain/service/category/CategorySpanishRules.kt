package es.pedrazamiguez.splittrip.domain.service.category

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.CategoryKeywordRule

internal object CategorySpanishRules {

    val rules: List<CategoryKeywordRule> = listOf(
        // ── TRANSPORT ──────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = null,
            keywords = listOf(
                "trayecto",
                "transporte",
                "desplazamiento",
                "billete",
                "pasaje",
                "vuelo",
                "avion",
                "aerolinea"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.INTERNATIONAL_FLIGHT,
            phrases = listOf("vuelo internacional", "vuelo larga distancia")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.DOMESTIC_FLIGHT,
            phrases = listOf("vuelo nacional", "vuelo interno")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.TRAIN,
            phrases = listOf("tren alta velocidad", "billete tren"),
            keywords = listOf("tren", "ave", "renfe", "ouigo", "iryo", "avlo", "ferrocarril", "cercanias", "rodalies")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.BUS,
            phrases = listOf("billete bus"),
            keywords = listOf("autobus", "autocar", "alsa", "guagua")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.PRIVATE_TRANSFER,
            phrases = listOf("traslado privado", "traslado aeropuerto"),
            keywords = listOf("chofer", "traslado")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.CAR_RENTAL,
            phrases = listOf("alquiler coche", "alquiler de coche", "alquiler auto")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.MOTORBIKE_RENTAL,
            phrases = listOf("alquiler moto", "alquiler scooter")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.FUEL,
            keywords = listOf(
                "gasolina",
                "gasolinera",
                "repostaje",
                "combustible",
                "gasoil",
                "repsol",
                "cepsa",
                "galp",
                "plenoil"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.TOLLS_PARKING,
            phrases = listOf("zona azul", "zona verde"),
            keywords = listOf("peaje", "autopista", "aparcamiento", "garaje", "telepeaje", "viat", "parquimetro")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.FERRY_BOAT,
            phrases = listOf("fred olsen"),
            keywords = listOf("barco", "crucero", "lancha", "travesia", "balearia", "trasmed", "naviera")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.PUBLIC_TRANSIT,
            phrases = listOf("transporte publico", "abono transporte", "tarjeta metro"),
            keywords = listOf("tranvia")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.CAR_REPAIR_MAINTENANCE,
            phrases = listOf("revision coche", "cambio aceite", "taller mecanico"),
            keywords = listOf("taller", "mecanico", "itv", "neumaticos", "ruedas", "frenos")
        ),

        // ── FOOD ───────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.RESTAURANT,
            phrases = listOf("menu del dia"),
            keywords = listOf(
                "cena",
                "cenar",
                "almuerzo",
                "almorzar",
                "comida",
                "comer",
                "restaurante",
                "hamburguesa",
                "bocadillo"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.STREET_FOOD,
            phrases = listOf("puesto callejero", "comida callejera", "mercado nocturno")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET,
            phrases = listOf("compra semanal"),
            keywords = listOf(
                "supermercado", "fruteria", "carniceria", "pescaderia", "panaderia",
                "mercadona", "alcampo", "eroski", "consum", "ahorramas", "despensa"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.CAFE_BREAKFAST,
            phrases = listOf("cafe y tostada", "cafe con leche"),
            keywords = listOf("desayuno", "merienda", "cafeteria", "tostada", "pasteleria")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.BAR_DRINKS,
            keywords = listOf(
                "cerveza",
                "cervezas",
                "cerveceria",
                "vino",
                "copa",
                "copas",
                "canas",
                "coctel",
                "vermut",
                "taberna"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.DELIVERY,
            phrases = listOf("comida a domicilio"),
            keywords = listOf("domicilio")
        ),

        // ── LODGING ────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOTEL,
            keywords = listOf("parador", "alojamiento")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOSTEL,
            keywords = listOf("albergue", "hostal")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.VACATION_RENTAL,
            phrases = listOf("apartamento turistico", "casa rural"),
            keywords = listOf("apartamento")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.RESORT,
            phrases = listOf("todo incluido")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.CAMPING,
            phrases = listOf("tienda campana"),
            keywords = listOf("caravana", "autocaravana")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOMESTAY,
            phrases = listOf("casa huespedes"),
            keywords = listOf("posada", "pension")
        ),

        // ── ACTIVITIES ─────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.MUSEUM_CULTURE,
            phrases = listOf("entrada museo"),
            keywords = listOf(
                "museo",
                "galeria",
                "exposicion",
                "catedral",
                "palacio",
                "castillo",
                "templo",
                "monumento",
                "prado"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.TOUR_EXCURSION,
            phrases = listOf("visita guiada"),
            keywords = listOf("excursion", "guia", "senderismo")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.NATURE_PARK,
            phrases = listOf("parque nacional", "reserva natural", "jardin botanico"),
            keywords = listOf("botanico", "mirador")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.WATER_SPORTS,
            keywords = listOf("buceo")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.ADVENTURE_SPORTS,
            keywords = listOf("esqui", "forfait", "escalada", "tirolina", "paracaidismo")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.SPA_WELLNESS,
            phrases = listOf("banos arabes"),
            keywords = listOf("masaje", "balneario", "termas")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.TICKETS_ATTRACTIONS,
            phrases = listOf("parque atracciones", "parque acuatico"),
            keywords = listOf("atracciones", "acuario", "teleferico", "noria", "portaventura", "warner")
        ),

        // ── ENTERTAINMENT ──────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.CONCERT_FESTIVAL,
            keywords = listOf("concierto", "recital", "sonar")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.NIGHTLIFE_CLUB,
            phrases = listOf("entrada copa"),
            keywords = listOf("discoteca", "fiesta")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.CINEMA_THEATER,
            keywords = listOf("cine", "pelicula", "teatro", "kinepolis", "cinesa")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.GAMES_ARCADE,
            keywords = listOf("bolera", "escape", "billar", "videojuegos")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.SPORTS_EVENT,
            phrases = listOf("partido futbol"),
            keywords = listOf("estadio", "partido", "futbol", "baloncesto", "tenis")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.SUBSCRIPTIONS_STREAMING,
            keywords = listOf("suscripcion")
        ),

        // ── HOME ───────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.RENT_MORTGAGE,
            keywords = listOf("alquiler", "hipoteca", "comunidad", "renta", "arrendamiento", "casero")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.UTILITIES,
            phrases = listOf(
                "factura agua", "factura del agua", "factura luz", "factura de la luz",
                "factura gas", "factura internet", "recibo luz", "recibo agua", "recibo gas"
            ),
            keywords = listOf(
                "factura", "recibo", "luz", "electricidad", "agua", "fibra",
                "suministros", "calefaccion", "iberdrola", "endesa", "naturgy", "digi", "movistar"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.HOME_REPAIR,
            phrases = listOf("reparacion hogar"),
            keywords = listOf(
                "fontanero",
                "electricista",
                "cerrajero",
                "pintor",
                "carpintero",
                "albanil",
                "manitas",
                "caldera"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.FURNITURE_APPLIANCES,
            keywords = listOf(
                "tostadora", "mueble", "muebles", "colchon", "nevera", "frigorifico", "lavadora",
                "lavavajillas", "microondas", "cafetera", "aspiradora", "leroy", "electrodomestico",
                "electrodomesticos", "lampara", "escritorio", "silla", "estanteria", "horno"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.HOME_SUPPLIES,
            phrases = listOf("productos limpieza", "papel higienico"),
            keywords = listOf(
                "detergente",
                "suavizante",
                "lejia",
                "servilletas",
                "fregona",
                "escoba",
                "limpieza",
                "ferreteria",
                "bombilla",
                "pilas",
                "velas"
            )
        ),

        // ── HEALTH & PERSONAL CARE ─────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.DOCTOR_DENTIST,
            keywords = listOf(
                "medico", "doctora", "dentista", "consulta", "clinica", "fisioterapeuta",
                "fisio", "pediatra", "oculista", "optica", "gafas", "psicologo", "dermatologo", "analisis", "vacuna"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.HAIRDRESSER_BARBER,
            phrases = listOf("corte pelo", "corte de pelo"),
            keywords = listOf(
                "peluqueria",
                "peluquero",
                "peluquera",
                "barberia",
                "barbero",
                "tinte",
                "mechas",
                "manicura",
                "pedicura"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.GYM_FITNESS,
            phrases = listOf("cuota gimnasio", "entrenador personal"),
            keywords = listOf("gimnasio", "natacion", "piscina", "padel", "basicfit", "mcfit", "vivagym")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.PERSONAL_CARE,
            phrases = listOf("cuidado personal"),
            keywords = listOf(
                "higiene",
                "cosmetica",
                "maquillaje",
                "crema",
                "champu",
                "desodorante",
                "colonia",
                "maquinilla",
                "cuchillas",
                "primor",
                "druni"
            )
        ),

        // ── SHOPPING ───────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.CLOTHING,
            keywords = listOf(
                "ropa",
                "camiseta",
                "pantalon",
                "vaqueros",
                "zapatos",
                "zapatillas",
                "chaqueta",
                "abrigo",
                "vestido",
                "calcetines"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.SOUVENIRS_GIFTS,
            keywords = listOf(
                "recuerdo",
                "recuerdos",
                "iman",
                "imanes",
                "postal",
                "postales",
                "llavero",
                "regalo",
                "regalos"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.GIFTS,
            keywords = listOf(
                "regalo",
                "regalos",
                "cumpleanos",
                "flores",
                "floristeria",
                "ramo",
                "aniversario",
                "navidad",
                "boda"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.ELECTRONICS,
            keywords = listOf(
                "electronica",
                "cargador",
                "adaptador",
                "auriculares",
                "movil",
                "portatil",
                "raton",
                "teclado",
                "bateria",
                "pccomponentes"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.PHARMACY_HEALTH,
            keywords = listOf(
                "farmacia",
                "parafarmacia",
                "medicamento",
                "medicamentos",
                "medicina",
                "pastillas",
                "ibuprofeno",
                "paracetamol",
                "tirita",
                "tiritas",
                "vitaminas",
                "antibiotico",
                "jarabe"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.CONVENIENCE_STORE,
            keywords = listOf("bazar", "chino", "kiosco", "quiosco", "minisuper")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.TOBACCO_VAPING,
            keywords = listOf("estanco", "tabaco", "cigarros", "cigarrillos", "vaper", "mecheros", "papelillos")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.HOBBIES,
            phrases = listOf("juego de mesa", "casa del libro"),
            keywords = listOf("libro", "libros", "libreria", "manualidades", "pintura", "guitarra")
        ),

        // ── INSURANCE ──────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = null,
            keywords = listOf("seguro", "poliza", "aseguradora")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.TRAVEL_INSURANCE,
            phrases = listOf("seguro viaje", "seguro de viaje", "asistencia viaje", "axa viaje"),
            keywords = listOf("heymondo", "iatiseguros", "iati")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.MEDICAL_HEALTH,
            phrases = listOf("seguro medico", "seguro salud", "seguro dental", "mapfre salud"),
            keywords = listOf("adeslas", "sanitas", "asisa", "dkv")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.VEHICLE_INSURANCE,
            phrases = listOf("seguro coche", "seguro moto", "seguro auto", "linea directa"),
            keywords = listOf("mutua", "verti")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.EQUIPMENT_INSURANCE,
            phrases = listOf("seguro equipaje", "seguro equipo", "seguro camara")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.HOME_INSURANCE,
            phrases = listOf("seguro hogar", "seguro casa", "seguro piso")
        ),

        // ── OTHER ──────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.TIPS_GRATUITIES,
            keywords = listOf("propina", "propinas", "bote")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.SIM_COMMUNICATIONS,
            phrases = listOf("tarjeta sim", "plan datos", "recarga movil"),
            keywords = listOf("telefonia", "recarga", "datos")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.LAUNDRY,
            phrases = listOf("lavadora fichas"),
            keywords = listOf("lavanderia", "tintoreria", "secadora")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.FEES_SURCHARGES,
            phrases = listOf("comision bancaria", "gastos gestion", "tasa turistica"),
            keywords = listOf("comision", "comisiones", "recargo", "tasa", "impuesto", "multa", "aduana")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.PET_CARE,
            phrases = listOf("comida perro", "comida gato", "arena gato"),
            keywords = listOf(
                "mascota",
                "mascotas",
                "perro",
                "perros",
                "gato",
                "gatos",
                "veterinario",
                "veterinaria",
                "pienso",
                "kiwoko",
                "tiendanimal"
            )
        )
    )
}
