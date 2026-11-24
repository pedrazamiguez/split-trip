package es.pedrazamiguez.splittrip.domain.service.category

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory

internal object CategoryTravelAndCoreRules {

    val rules: List<CategoryKeywordRule> = listOf(
        // ── TRANSPORT ──────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = null,
            keywords = listOf(
                "trayecto", "transporte", "desplazamiento", "billete", "pasaje",
                "commute", "transport", "vuelo", "flight", "avion", "aerolinea", "airlines"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.INTERNATIONAL_FLIGHT,
            phrases = listOf("vuelo internacional", "international flight", "long haul", "vuelo larga distancia"),
            keywords = listOf("lufthansa", "emirates", "qatar", "cathay", "transatlantic")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.DOMESTIC_FLIGHT,
            phrases = listOf("vuelo nacional", "vuelo interno", "domestic flight", "internal flight")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.TRAIN,
            phrases = listOf("high speed train", "tren alta velocidad", "billete tren", "train ticket"),
            keywords = listOf(
                "tren", "train", "ave", "renfe", "ouigo", "iryo", "avlo",
                "eurostar", "tgv", "shinkansen", "railway", "ferrocarril", "cercanias", "rodalies"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.BUS,
            phrases = listOf("bus ticket", "billete bus", "coach ticket"),
            keywords = listOf("bus", "autobus", "autocar", "alsa", "flixbus", "blablabus", "coach", "guagua")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.TAXI_RIDESHARE,
            keywords = listOf("taxi", "uber", "cabify", "bolt", "didi", "grab", "lyft", "freenow", "rideshare", "vtc")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.PRIVATE_TRANSFER,
            phrases = listOf("traslado privado", "private transfer", "airport transfer", "traslado aeropuerto"),
            keywords = listOf("shuttle", "chofer", "chauffeur", "traslado")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.CAR_RENTAL,
            phrases = listOf(
                "alquiler coche",
                "alquiler de coche",
                "alquiler auto",
                "rent a car",
                "car rental",
                "rental car"
            ),
            keywords = listOf("avis", "hertz", "europcar", "sixt", "enterprise", "goldcar", "alamo")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.MOTORBIKE_RENTAL,
            phrases = listOf("alquiler moto", "alquiler scooter", "motorbike rental", "scooter rental", "moto rental"),
            keywords = listOf("scooter", "moto", "vespa", "motorbike", "moped")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.FUEL,
            phrases = listOf("gas station"),
            keywords = listOf(
                "gasolina", "gasolinera", "repostaje", "combustible", "fuel", "petrol",
                "diesel", "gasoil", "repsol", "cepsa", "bp", "shell", "galp", "plenoil"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.TOLLS_PARKING,
            phrases = listOf("zona azul", "zona verde"),
            keywords = listOf(
                "peaje", "autopista", "aparcamiento", "parking", "toll", "tolls",
                "garaje", "garage", "telepeaje", "viat", "parquimetro"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.FERRY_BOAT,
            phrases = listOf("fred olsen"),
            keywords = listOf(
                "ferry", "barco", "crucero", "lancha", "catamaran",
                "travesia", "boat", "balearia", "trasmed", "naviera"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.PUBLIC_TRANSIT,
            phrases = listOf("transporte publico", "abono transporte", "tarjeta metro", "public transit", "metro card"),
            keywords = listOf("metro", "subway", "tram", "tranvia", "underground", "tube", "oyster", "suica", "pasmo")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.CAR_REPAIR_MAINTENANCE,
            phrases = listOf(
                "revision coche",
                "cambio aceite",
                "car repair",
                "car maintenance",
                "auto repair",
                "oil change",
                "taller mecanico"
            ),
            keywords = listOf(
                "taller", "mecanico", "itv", "neumaticos", "ruedas",
                "frenos", "mechanic", "tyres", "tires"
            )
        ),

        // ── FOOD ───────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.RESTAURANT,
            phrases = listOf("bahn mi", "banh mi", "dim sum", "hot pot", "pad thai", "fish and chips", "menu del dia"),
            keywords = listOf(
                "cena", "cenar", "almuerzo", "almorzar", "comida", "comer", "restaurante",
                "restaurant", "dinner", "lunch", "tapas", "pizza", "pizzeria", "sushi",
                "ramen", "burger", "hamburguesa", "paella", "pho", "tacos", "curry",
                "bistro", "trattoria", "brunch", "bocadillo"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.STREET_FOOD,
            phrases = listOf(
                "puesto callejero",
                "comida callejera",
                "street food",
                "food truck",
                "night market",
                "mercado nocturno"
            ),
            keywords = listOf("hawker")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET,
            phrases = listOf("compra semanal"),
            keywords = listOf(
                "supermercado", "supermarket", "groceries", "grocery", "fruteria",
                "carniceria", "pescaderia", "panaderia", "mercadona", "carrefour",
                "lidl", "aldi", "alcampo", "eroski", "consum", "ahorramas", "tesco",
                "sainsbury", "walmart", "wholefoods", "despensa"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.CAFE_BREAKFAST,
            phrases = listOf("cafe y tostada", "cafe con leche", "coffee shop", "specialty coffee"),
            keywords = listOf(
                "desayuno", "breakfast", "merienda", "cafe", "cafeteria", "coffee",
                "latte", "espresso", "cappuccino", "starbucks", "tostada", "croissant",
                "churros", "matcha", "bakery", "pasteleria"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.BAR_DRINKS,
            phrases = listOf("happy hour", "bar drinks"),
            keywords = listOf(
                "bar", "pub", "cerveza", "cervezas", "cerveceria", "beer", "vino",
                "wine", "copa", "copas", "canas", "cocktail", "coctel", "drinks",
                "mojito", "vermut", "taberna", "izakaya"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.DELIVERY,
            phrases = listOf("comida a domicilio", "food delivery", "take away"),
            keywords = listOf(
                "delivery", "takeaway", "glovo", "ubereats", "justeat",
                "deliveroo", "doordash", "grubhub", "domicilio"
            )
        ),

        // ── LODGING ────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOTEL,
            keywords = listOf(
                "hotel", "motel", "ryokan", "parador", "nh", "melia", "marriott",
                "hilton", "ibis", "accor", "hyatt", "radisson", "novotel", "sheraton", "alojamiento"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOSTEL,
            keywords = listOf("hostel", "albergue", "hostal", "backpacker")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.VACATION_RENTAL,
            phrases = listOf("apartamento turistico", "vacation rental", "holiday home", "casa rural"),
            keywords = listOf("airbnb", "vrbo", "apartamento", "apartment", "villa", "chalet", "bungalow")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.RESORT,
            phrases = listOf("all inclusive", "todo incluido", "spa resort"),
            keywords = listOf("resort")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.CAMPING,
            phrases = listOf("tienda campana"),
            keywords = listOf("camping", "campground", "campsite", "caravana", "camper", "autocaravana", "glamping")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOMESTAY,
            phrases = listOf("bed and breakfast", "casa huespedes"),
            keywords = listOf("homestay", "guesthouse", "bnb", "posada", "pension")
        ),

        // ── ACTIVITIES ─────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.MUSEUM_CULTURE,
            phrases = listOf("entrada museo", "museum ticket"),
            keywords = listOf(
                "museo", "museum", "galeria", "gallery", "exposicion", "exhibition",
                "catedral", "cathedral", "palacio", "palace", "castillo", "castle",
                "templo", "temple", "monumento", "monument", "louvre", "prado"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.TOUR_EXCURSION,
            phrases = listOf("free tour", "guided tour", "visita guiada", "day trip"),
            keywords = listOf("tour", "excursion", "guia", "guide", "safari", "trekking", "senderismo", "hiking")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.NATURE_PARK,
            phrases = listOf(
                "parque nacional",
                "national park",
                "reserva natural",
                "nature reserve",
                "jardin botanico"
            ),
            keywords = listOf("botanico", "canyon", "mirador")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.WATER_SPORTS,
            phrases = listOf("paddle surf"),
            keywords = listOf("buceo", "snorkel", "surf", "kayak", "diving", "scuba", "jetski", "rafting", "sailing")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.ADVENTURE_SPORTS,
            keywords = listOf(
                "esqui", "ski", "snowboard", "forfait", "skipass", "escalada",
                "climbing", "tirolina", "zipline", "paracaidismo", "skydiving", "bungee", "quad"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.SPA_WELLNESS,
            phrases = listOf("banos arabes"),
            keywords = listOf(
                "spa", "masaje", "massage", "balneario", "termas",
                "hammam", "onsen", "sauna", "wellness"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.TICKETS_ATTRACTIONS,
            phrases = listOf("parque atracciones", "theme park", "water park", "parque acuatico"),
            keywords = listOf(
                "atracciones", "zoo", "aquarium", "acuario", "teleferico",
                "cablecar", "noria", "ferris", "portaventura", "warner"
            )
        ),

        // ── ENTERTAINMENT ──────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.CONCERT_FESTIVAL,
            keywords = listOf("concierto", "concert", "festival", "recital", "sonar", "coachella", "gig")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.NIGHTLIFE_CLUB,
            phrases = listOf("entrada copa"),
            keywords = listOf("discoteca", "nightclub", "fiesta", "club")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.CINEMA_THEATER,
            keywords = listOf(
                "cine", "cinema", "pelicula", "movie", "teatro", "theater",
                "theatre", "musical", "opera", "ballet", "kinepolis", "cinesa", "imax"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.GAMES_ARCADE,
            keywords = listOf(
                "bolera", "bowling", "arcade", "escape", "billar",
                "billiards", "karaoke", "videojuegos", "gaming", "casino", "darts"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.SPORTS_EVENT,
            phrases = listOf("partido futbol", "football match", "basketball game"),
            keywords = listOf(
                "estadio", "stadium", "partido", "futbol", "football", "baloncesto",
                "basketball", "tenis", "tennis", "f1", "motogp", "champions"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.SUBSCRIPTIONS_STREAMING,
            phrases = listOf("prime video", "youtube premium", "apple music", "apple one", "google one"),
            keywords = listOf(
                "netflix", "spotify", "disney", "hbo", "icloud", "chatgpt",
                "suscripcion", "subscription", "streaming", "twitch", "audible",
                "kindle", "playstation", "xbox", "nintendo"
            )
        )
    )
}
