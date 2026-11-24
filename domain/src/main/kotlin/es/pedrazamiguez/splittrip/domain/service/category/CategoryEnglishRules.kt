package es.pedrazamiguez.splittrip.domain.service.category

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.CategoryKeywordRule

internal object CategoryEnglishRules {

    val rules: List<CategoryKeywordRule> = listOf(
        // ── TRANSPORT ──────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = null,
            keywords = listOf("commute", "transport", "flight", "airlines")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.INTERNATIONAL_FLIGHT,
            phrases = listOf("international flight", "long haul"),
            keywords = listOf("transatlantic")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.DOMESTIC_FLIGHT,
            phrases = listOf("domestic flight", "internal flight")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.TRAIN,
            phrases = listOf("high speed train", "train ticket"),
            keywords = listOf("train", "railway")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.BUS,
            phrases = listOf("bus ticket", "coach ticket"),
            keywords = listOf("coach")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.TAXI_RIDESHARE,
            keywords = listOf("rideshare")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.PRIVATE_TRANSFER,
            phrases = listOf("private transfer", "airport transfer"),
            keywords = listOf("shuttle", "chauffeur")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.CAR_RENTAL,
            phrases = listOf("rent a car", "car rental", "rental car")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.MOTORBIKE_RENTAL,
            phrases = listOf("motorbike rental", "scooter rental", "moto rental"),
            keywords = listOf("motorbike", "moped")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.FUEL,
            phrases = listOf("gas station"),
            keywords = listOf("fuel", "petrol")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.TOLLS_PARKING,
            keywords = listOf("toll", "tolls", "garage")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.FERRY_BOAT,
            keywords = listOf("boat")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.PUBLIC_TRANSIT,
            phrases = listOf("public transit", "metro card"),
            keywords = listOf("subway", "tram", "underground", "tube", "oyster")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.TRANSPORT,
            subcategory = ExpenseSubcategory.CAR_REPAIR_MAINTENANCE,
            phrases = listOf("car repair", "car maintenance", "auto repair", "oil change"),
            keywords = listOf("mechanic", "tyres", "tires")
        ),

        // ── FOOD ───────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.RESTAURANT,
            phrases = listOf("fish and chips"),
            keywords = listOf("restaurant", "dinner", "lunch")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.STREET_FOOD,
            phrases = listOf("street food", "night market"),
            keywords = listOf("hawker")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET,
            keywords = listOf("supermarket", "groceries", "grocery", "tesco", "sainsbury", "walmart", "wholefoods")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.CAFE_BREAKFAST,
            phrases = listOf("coffee shop", "specialty coffee"),
            keywords = listOf("breakfast", "coffee", "bakery")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.BAR_DRINKS,
            phrases = listOf("bar drinks"),
            keywords = listOf("beer", "wine", "drinks")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.DELIVERY,
            phrases = listOf("food delivery", "take away"),
            keywords = listOf("takeaway")
        ),

        // ── LODGING ────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOSTEL,
            keywords = listOf("backpacker")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.VACATION_RENTAL,
            phrases = listOf("vacation rental", "holiday home"),
            keywords = listOf("apartment")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.RESORT,
            phrases = listOf("all inclusive", "spa resort")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.CAMPING,
            keywords = listOf("campground", "campsite", "camper")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOMESTAY,
            phrases = listOf("bed and breakfast"),
            keywords = listOf("homestay", "guesthouse")
        ),

        // ── ACTIVITIES ─────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.MUSEUM_CULTURE,
            phrases = listOf("museum ticket"),
            keywords = listOf("museum", "gallery", "exhibition", "cathedral", "palace", "castle", "temple", "monument")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.TOUR_EXCURSION,
            phrases = listOf("guided tour", "day trip"),
            keywords = listOf("excursion", "guide", "hiking")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.NATURE_PARK,
            phrases = listOf("national park", "nature reserve"),
            keywords = listOf("canyon")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.WATER_SPORTS,
            keywords = listOf("diving", "sailing")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.ADVENTURE_SPORTS,
            keywords = listOf("climbing", "zipline", "skydiving")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.SPA_WELLNESS,
            keywords = listOf("massage", "wellness")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ACTIVITIES,
            subcategory = ExpenseSubcategory.TICKETS_ATTRACTIONS,
            phrases = listOf("theme park", "water park"),
            keywords = listOf("cablecar", "ferris")
        ),

        // ── ENTERTAINMENT ──────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.CONCERT_FESTIVAL,
            keywords = listOf("concert", "recital", "coachella", "gig")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.NIGHTLIFE_CLUB,
            keywords = listOf("nightclub")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.CINEMA_THEATER,
            keywords = listOf("movie", "theater", "theatre")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.GAMES_ARCADE,
            phrases = listOf("escape room"),
            keywords = listOf("billiards", "gaming", "darts")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.SPORTS_EVENT,
            phrases = listOf("football match", "basketball game"),
            keywords = listOf("stadium", "football", "basketball", "tennis")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.ENTERTAINMENT,
            subcategory = ExpenseSubcategory.SUBSCRIPTIONS_STREAMING,
            keywords = listOf("subscription")
        ),

        // ── HOME ───────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.RENT_MORTGAGE,
            keywords = listOf("rent", "mortgage", "hoa", "landlord")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.UTILITIES,
            phrases = listOf("water bill", "electricity bill", "electric bill", "gas bill", "internet bill"),
            keywords = listOf("utilities", "electricity")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.HOME_REPAIR,
            phrases = listOf("home repair"),
            keywords = listOf("plumber", "electrician", "locksmith", "handyman")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.FURNITURE_APPLIANCES,
            keywords = listOf(
                "toaster", "mattress", "fridge", "washer", "dishwasher", "microwave",
                "vacuum", "furniture", "appliance", "appliances", "lamp", "desk", "chair", "oven"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.HOME_SUPPLIES,
            phrases = listOf("home supplies", "cleaning supplies", "toilet paper"),
            keywords = listOf("hardware", "detergent", "cleaning")
        ),

        // ── HEALTH & PERSONAL CARE ─────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.DOCTOR_DENTIST,
            keywords = listOf("dentist", "clinic", "therapy")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.HAIRDRESSER_BARBER,
            phrases = listOf("hair cut"),
            keywords = listOf("hairdresser", "barber", "haircut")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.GYM_FITNESS,
            phrases = listOf("personal trainer", "gym membership")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.HEALTH_PERSONAL_CARE,
            subcategory = ExpenseSubcategory.PERSONAL_CARE,
            phrases = listOf("personal care"),
            keywords = listOf("shampoo", "deodorant", "skincare", "cosmetics", "toiletries")
        ),

        // ── SHOPPING ───────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.CLOTHING,
            keywords = listOf(
                "clothing",
                "clothes",
                "shirt",
                "pants",
                "shoes",
                "sneakers",
                "jacket",
                "coat",
                "dress",
                "socks"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.SOUVENIRS_GIFTS,
            keywords = listOf("magnet", "postcard", "keychain", "gift", "gifts")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.GIFTS,
            keywords = listOf(
                "gift",
                "gifts",
                "birthday",
                "flowers",
                "bouquet",
                "anniversary",
                "christmas",
                "wedding",
                "present"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.ELECTRONICS,
            keywords = listOf(
                "electronics",
                "charger",
                "adapter",
                "headphones",
                "earbuds",
                "phone",
                "laptop",
                "mouse",
                "keyboard"
            )
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.PHARMACY_HEALTH,
            keywords = listOf("pharmacy", "medicine", "drugstore", "boots", "cvs", "walgreens", "vitamins")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.CONVENIENCE_STORE,
            phrases = listOf("convenience store")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.TOBACCO_VAPING,
            keywords = listOf("vaping", "tobacco", "cigarettes")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.SHOPPING,
            subcategory = ExpenseSubcategory.HOBBIES,
            phrases = listOf("board game"),
            keywords = listOf("book", "books", "bookstore")
        ),

        // ── INSURANCE ──────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = null,
            keywords = listOf("insurance", "policy")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.TRAVEL_INSURANCE,
            phrases = listOf("travel insurance")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.MEDICAL_HEALTH,
            phrases = listOf("health insurance", "medical insurance", "dental insurance")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.VEHICLE_INSURANCE,
            phrases = listOf("car insurance", "auto insurance", "vehicle insurance", "motorbike insurance")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.EQUIPMENT_INSURANCE,
            phrases = listOf("equipment insurance", "luggage insurance", "baggage insurance", "gear insurance")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.INSURANCE,
            subcategory = ExpenseSubcategory.HOME_INSURANCE,
            phrases = listOf("home insurance", "house insurance", "renters insurance")
        ),

        // ── OTHER ──────────────────────────────────────────────────────────
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.TIPS_GRATUITIES,
            keywords = listOf("tip", "tips", "gratuity", "gratuities")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.SIM_COMMUNICATIONS,
            phrases = listOf("sim card")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.LAUNDRY,
            phrases = listOf("dry cleaning"),
            keywords = listOf("laundry", "laundromat")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.FEES_SURCHARGES,
            phrases = listOf("bank fee", "atm fee", "booking fee", "city tax", "tourist tax"),
            keywords = listOf("fee", "fees", "surcharge", "surcharges", "fine", "customs")
        ),
        CategoryKeywordRule(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.PET_CARE,
            phrases = listOf("pet food", "dog food", "cat food"),
            keywords = listOf("vet", "pet", "pets", "dog", "cat")
        )
    )
}
