package es.pedrazamiguez.splittrip.domain.service.category

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.CategoryKeywordRule

internal object CategoryCommonRules {

    val rules: List<CategoryKeywordRule> = listOf(
        // ── TRANSPORT ──────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.INTERNATIONAL_FLIGHT,
            keywords = listOf("lufthansa", "emirates", "qatar", "cathay")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.TRAIN,
            keywords = listOf("eurostar", "tgv", "shinkansen")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.BUS,
            keywords = listOf("bus", "flixbus", "blablabus")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.TAXI_RIDESHARE,
            keywords = listOf("taxi", "uber", "cabify", "bolt", "didi", "grab", "lyft", "freenow", "vtc")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.CAR_RENTAL,
            keywords = listOf("avis", "hertz", "europcar", "sixt", "enterprise", "goldcar", "alamo")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.MOTORBIKE_RENTAL,
            keywords = listOf("scooter", "moto", "vespa")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.FUEL,
            keywords = listOf("diesel", "bp", "shell")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.TOLLS_PARKING,
            keywords = listOf("parking")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.FERRY_BOAT,
            keywords = listOf("ferry", "catamaran")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.PUBLIC_TRANSIT,
            keywords = listOf("metro", "suica", "pasmo")
        ),

        // ── FOOD ───────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.RESTAURANT,
            phrases = listOf("bahn mi", "banh mi", "dim sum", "hot pot", "pad thai"),
            keywords = listOf(
                "tapas", "pizza", "pizzeria", "sushi", "ramen", "burger",
                "paella", "pho", "tacos", "curry", "bistro", "trattoria", "brunch"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.STREET_FOOD,
            phrases = listOf("food truck")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET,
            keywords = listOf("carrefour", "lidl", "aldi")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.CAFE_BREAKFAST,
            keywords = listOf("cafe", "latte", "espresso", "cappuccino", "starbucks", "croissant", "churros", "matcha")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.BAR_DRINKS,
            phrases = listOf("happy hour"),
            keywords = listOf("bar", "pub", "cocktail", "mojito", "izakaya")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.DELIVERY,
            keywords = listOf("delivery", "glovo", "ubereats", "justeat", "deliveroo", "doordash", "grubhub")
        ),

        // ── LODGING ────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOTEL,
            keywords = listOf(
                "hotel", "motel", "ryokan", "nh", "melia", "marriott", "hilton",
                "ibis", "accor", "hyatt", "radisson", "novotel", "sheraton"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOSTEL,
            keywords = listOf("hostel")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.VACATION_RENTAL,
            keywords = listOf("airbnb", "vrbo", "villa", "chalet", "bungalow")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.RESORT,
            keywords = listOf("resort")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.CAMPING,
            keywords = listOf("camping", "glamping")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOMESTAY,
            keywords = listOf("bnb")
        ),

        // ── ACTIVITIES ─────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.MUSEUM_CULTURE,
            keywords = listOf("louvre")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.TOUR_EXCURSION,
            phrases = listOf("free tour"),
            keywords = listOf("tour", "safari", "trekking")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.WATER_SPORTS,
            phrases = listOf("paddle surf"),
            keywords = listOf("snorkel", "surf", "kayak", "scuba", "jetski", "rafting")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.ADVENTURE_SPORTS,
            keywords = listOf("ski", "snowboard", "skipass", "bungee", "quad")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.SPA_WELLNESS,
            keywords = listOf("spa", "hammam", "onsen", "sauna")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.TICKETS_ATTRACTIONS,
            keywords = listOf("zoo", "aquarium")
        ),

        // ── ENTERTAINMENT ──────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.CONCERT_FESTIVAL,
            keywords = listOf("festival")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.NIGHTLIFE_CLUB,
            keywords = listOf("club")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.CINEMA_THEATER,
            keywords = listOf("cinema", "musical", "opera", "ballet", "imax")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.GAMES_ARCADE,
            keywords = listOf("bowling", "arcade", "karaoke", "casino")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.SPORTS_EVENT,
            keywords = listOf("f1", "motogp", "champions")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.SUBSCRIPTIONS_STREAMING,
            phrases = listOf("prime video", "youtube premium", "apple music", "apple one", "google one"),
            keywords = listOf(
                "netflix", "spotify", "disney", "hbo", "icloud", "chatgpt",
                "streaming", "twitch", "audible", "kindle", "playstation", "xbox", "nintendo"
            )
        ),

        // ── HOME ───────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.UTILITIES,
            keywords = listOf("gas", "internet", "wifi", "vodafone")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.FURNITURE_APPLIANCES,
            keywords = listOf("sofa", "airfryer", "ikea")
        ),

        // ── HEALTH & PERSONAL CARE ─────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.DOCTOR_DENTIST,
            keywords = listOf("doctor", "hospital")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.GYM_FITNESS,
            keywords = listOf("gym", "fitness", "crossfit", "pilates", "yoga", "spinning")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.PERSONAL_CARE,
            keywords = listOf("gel", "perfume", "sephora")
        ),

        // ── SHOPPING ───────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.CLOTHING,
            keywords = listOf(
                "jeans", "zara", "uniqlo", "mango", "primark",
                "nike", "adidas", "decathlon", "stradivarius", "bershka"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.SOUVENIRS_GIFTS,
            keywords = listOf("souvenir", "souvenirs")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.ELECTRONICS,
            phrases = listOf("apple store"),
            keywords = listOf(
                "cable", "airpods", "iphone", "macbook", "samsung",
                "tablet", "ipad", "powerbank", "usb", "mediamarkt"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.CONVENIENCE_STORE,
            phrases = listOf("7 eleven", "seven eleven"),
            keywords = listOf("konbini", "familymart", "lawson", "oxxo")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.TOBACCO_VAPING,
            keywords = listOf("vape", "iqos")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.HOBBIES,
            keywords = listOf("hobby", "hobbies", "comic", "comics", "manga", "puzzle", "lego", "fnac", "piano")
        ),

        // ── INSURANCE ──────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.TRAVEL_INSURANCE,
            keywords = listOf("chapka")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.EQUIPMENT_INSURANCE,
            keywords = listOf("applecare")
        ),

        // ── OTHER ──────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.SIM_COMMUNICATIONS,
            keywords = listOf("sim", "esim", "roaming", "holafly", "airalo", "saily")
        )
    )
}
