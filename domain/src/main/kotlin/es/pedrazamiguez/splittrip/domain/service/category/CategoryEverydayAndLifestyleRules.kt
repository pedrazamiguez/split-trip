package es.pedrazamiguez.splittrip.domain.service.category

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory

internal object CategoryEverydayAndLifestyleRules {

    val rules: List<CategoryKeywordRule> = listOf(
        // ── HOME ───────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.RENT_MORTGAGE,
            keywords = listOf(
                "alquiler", "hipoteca", "comunidad", "renta", "arrendamiento",
                "rent", "mortgage", "hoa", "landlord", "casero"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.UTILITIES,
            phrases = listOf(
                "water bill", "electricity bill", "electric bill", "gas bill", "internet bill",
                "factura agua", "factura del agua", "factura luz", "factura de la luz",
                "factura gas", "factura internet", "recibo luz", "recibo agua", "recibo gas"
            ),
            keywords = listOf(
                "factura", "recibo", "luz", "electricidad", "agua", "gas",
                "internet", "fibra", "wifi", "suministros", "utilities", "electricity",
                "calefaccion", "iberdrola", "endesa", "naturgy", "digi", "movistar", "vodafone"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.HOME_REPAIR,
            phrases = listOf("reparacion hogar", "home repair"),
            keywords = listOf(
                "fontanero", "electricista", "cerrajero", "pintor", "carpintero",
                "plumber", "electrician", "locksmith", "handyman", "albanil", "manitas", "caldera"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.FURNITURE_APPLIANCES,
            keywords = listOf(
                "tostadora", "toaster", "mueble", "muebles", "sofa", "colchon",
                "mattress", "nevera", "frigorifico", "fridge", "lavadora", "washer",
                "lavavajillas", "dishwasher", "microondas", "microwave", "cafetera",
                "aspiradora", "vacuum", "airfryer", "ikea", "leroy", "electrodomestico",
                "electrodomesticos", "furniture", "appliance", "appliances", "lampara",
                "lamp", "escritorio", "desk", "silla", "chair", "estanteria", "horno", "oven"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.HOME_SUPPLIES,
            phrases = listOf(
                "productos limpieza",
                "home supplies",
                "cleaning supplies",
                "papel higienico",
                "toilet paper"
            ),
            keywords = listOf(
                "detergente", "suavizante", "lejia", "servilletas", "fregona",
                "escoba", "limpieza", "ferreteria", "hardware", "bombilla",
                "pilas", "velas", "detergent", "cleaning"
            )
        ),

        // ── HEALTH & PERSONAL CARE ─────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.DOCTOR_DENTIST,
            keywords = listOf(
                "medico", "doctor", "doctora", "dentista", "consulta", "clinica",
                "fisioterapeuta", "fisio", "dentist", "clinic", "hospital",
                "pediatra", "oculista", "optica", "gafas", "psicologo", "therapy",
                "dermatologo", "analisis", "vacuna"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.HAIRDRESSER_BARBER,
            phrases = listOf("corte pelo", "corte de pelo", "hair cut"),
            keywords = listOf(
                "peluqueria", "peluquero", "peluquera", "barberia", "barbero",
                "hairdresser", "barber", "haircut", "tinte", "mechas", "manicura", "pedicura"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.GYM_FITNESS,
            phrases = listOf("cuota gimnasio", "personal trainer", "entrenador personal", "gym membership"),
            keywords = listOf(
                "gimnasio", "gym", "fitness", "crossfit", "pilates", "yoga",
                "natacion", "piscina", "padel", "spinning", "basicfit", "mcfit", "vivagym"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.PERSONAL_CARE,
            phrases = listOf("cuidado personal", "personal care"),
            keywords = listOf(
                "higiene", "cosmetica", "maquillaje", "crema", "champu", "shampoo",
                "gel", "desodorante", "deodorant", "perfume", "colonia", "skincare",
                "cosmetics", "toiletries", "maquinilla", "cuchillas", "sephora", "primor", "druni"
            )
        ),

        // ── SHOPPING ───────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.CLOTHING,
            keywords = listOf(
                "ropa", "clothing", "clothes", "camiseta", "shirt", "pantalon",
                "pants", "jeans", "vaqueros", "zapatos", "shoes", "zapatillas",
                "sneakers", "chaqueta", "jacket", "abrigo", "coat", "vestido",
                "dress", "calcetines", "socks", "zara", "uniqlo", "mango",
                "primark", "nike", "adidas", "decathlon", "stradivarius", "bershka"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.SOUVENIRS_GIFTS,
            keywords = listOf(
                "souvenir", "souvenirs", "recuerdo", "recuerdos", "iman", "imanes",
                "magnet", "postal", "postales", "postcard", "llavero", "keychain",
                "regalo", "regalos", "gift", "gifts"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.GIFTS,
            keywords = listOf(
                "regalo", "regalos", "cumpleanos", "birthday", "flores", "flowers",
                "floristeria", "ramo", "bouquet", "aniversario", "anniversary",
                "navidad", "christmas", "boda", "wedding", "gift", "gifts", "present"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.ELECTRONICS,
            phrases = listOf("apple store"),
            keywords = listOf(
                "electronica", "electronics", "cargador", "charger", "cable",
                "adaptador", "adapter", "auriculares", "headphones", "earbuds",
                "airpods", "movil", "phone", "iphone", "macbook", "samsung",
                "portatil", "laptop", "tablet", "ipad", "raton", "mouse",
                "teclado", "keyboard", "bateria", "powerbank", "usb", "mediamarkt", "pccomponentes"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.PHARMACY_HEALTH,
            keywords = listOf(
                "farmacia", "parafarmacia", "medicamento", "medicamentos", "medicina",
                "pastillas", "ibuprofeno", "paracetamol", "tirita", "tiritas",
                "pharmacy", "medicine", "drugstore", "boots", "cvs", "walgreens",
                "vitaminas", "vitamins", "antibiotico", "jarabe"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.CONVENIENCE_STORE,
            phrases = listOf("7 eleven", "seven eleven", "convenience store"),
            keywords = listOf(
                "bazar", "chino", "konbini", "familymart", "lawson",
                "oxxo", "kiosco", "quiosco", "minisuper"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.TOBACCO_VAPING,
            keywords = listOf(
                "estanco", "tabaco", "cigarros", "cigarrillos", "vaper", "vape",
                "vaping", "iqos", "tobacco", "cigarettes", "mecheros", "papelillos"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.HOBBIES,
            phrases = listOf("juego de mesa", "board game", "casa del libro"),
            keywords = listOf(
                "hobby", "hobbies", "libro", "libros", "libreria", "comic",
                "comics", "manga", "puzzle", "lego", "manualidades", "book",
                "books", "bookstore", "fnac", "pintura", "guitarra", "piano"
            )
        ),

        // ── INSURANCE ──────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = null,
            keywords = listOf("seguro", "insurance", "poliza", "policy", "aseguradora")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.TRAVEL_INSURANCE,
            phrases = listOf("seguro viaje", "seguro de viaje", "travel insurance", "asistencia viaje", "axa viaje"),
            keywords = listOf("heymondo", "iatiseguros", "iati", "chapka")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.MEDICAL_HEALTH,
            phrases = listOf(
                "seguro medico",
                "seguro salud",
                "seguro dental",
                "health insurance",
                "medical insurance",
                "dental insurance",
                "mapfre salud"
            ),
            keywords = listOf("adeslas", "sanitas", "asisa", "dkv")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.VEHICLE_INSURANCE,
            phrases = listOf(
                "seguro coche",
                "seguro moto",
                "seguro auto",
                "car insurance",
                "auto insurance",
                "vehicle insurance",
                "motorbike insurance",
                "linea directa"
            ),
            keywords = listOf("mutua", "verti")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.EQUIPMENT_INSURANCE,
            phrases = listOf(
                "seguro equipaje",
                "seguro equipo",
                "seguro camara",
                "equipment insurance",
                "luggage insurance",
                "baggage insurance",
                "gear insurance"
            ),
            keywords = listOf("applecare")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.HOME_INSURANCE,
            phrases = listOf(
                "seguro hogar",
                "seguro casa",
                "seguro piso",
                "home insurance",
                "house insurance",
                "renters insurance"
            )
        ),

        // ── OTHER (specific subcategories only) ────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.TIPS_GRATUITIES,
            keywords = listOf("propina", "propinas", "tip", "tips", "gratuity", "gratuities", "bote")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.SIM_COMMUNICATIONS,
            phrases = listOf("tarjeta sim", "sim card", "plan datos", "recarga movil"),
            keywords = listOf("sim", "esim", "roaming", "holafly", "airalo", "telefonia", "recarga", "datos", "saily")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.LAUNDRY,
            phrases = listOf("dry cleaning", "lavadora fichas"),
            keywords = listOf("lavanderia", "laundry", "laundromat", "tintoreria", "secadora")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.FEES_SURCHARGES,
            phrases = listOf(
                "comision bancaria",
                "bank fee",
                "atm fee",
                "booking fee",
                "gastos gestion",
                "tasa turistica",
                "city tax",
                "tourist tax"
            ),
            keywords = listOf(
                "comision", "comisiones", "recargo", "fee", "fees",
                "surcharge", "surcharges", "tasa", "impuesto", "multa", "fine", "aduana", "customs"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.PET_CARE,
            phrases = listOf("comida perro", "comida gato", "pet food", "dog food", "cat food", "arena gato"),
            keywords = listOf(
                "mascota", "mascotas", "perro", "perros", "gato", "gatos",
                "veterinario", "veterinaria", "vet", "pienso", "pet", "pets",
                "dog", "cat", "kiwoko", "tiendanimal"
            )
        )
    )
}
