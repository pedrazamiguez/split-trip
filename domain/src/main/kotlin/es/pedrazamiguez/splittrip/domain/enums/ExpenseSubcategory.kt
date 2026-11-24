package es.pedrazamiguez.splittrip.domain.enums

private val TRAVEL_ONLY = setOf(GroupContext.TRAVEL)
private val EVERYDAY_ONLY = setOf(GroupContext.EVERYDAY)
private val ALL_CONTEXTS = GroupContext.entries.toSet()

enum class ExpenseSubcategory(
    val parentCategory: ExpenseCategory,
    val contexts: Set<GroupContext>
) {
    // TRANSPORT
    INTERNATIONAL_FLIGHT(ExpenseCategory.TRANSPORT, TRAVEL_ONLY),
    DOMESTIC_FLIGHT(ExpenseCategory.TRANSPORT, TRAVEL_ONLY),
    TRAIN(ExpenseCategory.TRANSPORT, ALL_CONTEXTS),
    BUS(ExpenseCategory.TRANSPORT, ALL_CONTEXTS),
    TAXI_RIDESHARE(ExpenseCategory.TRANSPORT, ALL_CONTEXTS),
    PRIVATE_TRANSFER(ExpenseCategory.TRANSPORT, TRAVEL_ONLY),
    CAR_RENTAL(ExpenseCategory.TRANSPORT, TRAVEL_ONLY),
    MOTORBIKE_RENTAL(ExpenseCategory.TRANSPORT, TRAVEL_ONLY),
    FUEL(ExpenseCategory.TRANSPORT, ALL_CONTEXTS),
    TOLLS_PARKING(ExpenseCategory.TRANSPORT, ALL_CONTEXTS),
    FERRY_BOAT(ExpenseCategory.TRANSPORT, TRAVEL_ONLY),
    PUBLIC_TRANSIT(ExpenseCategory.TRANSPORT, ALL_CONTEXTS),
    CAR_REPAIR_MAINTENANCE(ExpenseCategory.TRANSPORT, EVERYDAY_ONLY),

    // FOOD
    RESTAURANT(ExpenseCategory.FOOD, ALL_CONTEXTS),
    STREET_FOOD(ExpenseCategory.FOOD, TRAVEL_ONLY),
    GROCERIES_SUPERMARKET(ExpenseCategory.FOOD, ALL_CONTEXTS),
    CAFE_BREAKFAST(ExpenseCategory.FOOD, ALL_CONTEXTS),
    BAR_DRINKS(ExpenseCategory.FOOD, ALL_CONTEXTS),
    DELIVERY(ExpenseCategory.FOOD, ALL_CONTEXTS),

    // LODGING
    HOTEL(ExpenseCategory.LODGING, TRAVEL_ONLY),
    HOSTEL(ExpenseCategory.LODGING, TRAVEL_ONLY),
    VACATION_RENTAL(ExpenseCategory.LODGING, TRAVEL_ONLY),
    RESORT(ExpenseCategory.LODGING, TRAVEL_ONLY),
    CAMPING(ExpenseCategory.LODGING, TRAVEL_ONLY),
    HOMESTAY(ExpenseCategory.LODGING, TRAVEL_ONLY),

    // HOME
    RENT_MORTGAGE(ExpenseCategory.HOME, EVERYDAY_ONLY),
    UTILITIES(ExpenseCategory.HOME, EVERYDAY_ONLY),
    HOME_REPAIR(ExpenseCategory.HOME, EVERYDAY_ONLY),
    FURNITURE_APPLIANCES(ExpenseCategory.HOME, EVERYDAY_ONLY),
    HOME_SUPPLIES(ExpenseCategory.HOME, EVERYDAY_ONLY),

    // HEALTH_PERSONAL_CARE
    DOCTOR_DENTIST(ExpenseCategory.HEALTH_PERSONAL_CARE, EVERYDAY_ONLY),
    HAIRDRESSER_BARBER(ExpenseCategory.HEALTH_PERSONAL_CARE, EVERYDAY_ONLY),
    GYM_FITNESS(ExpenseCategory.HEALTH_PERSONAL_CARE, EVERYDAY_ONLY),
    PERSONAL_CARE(ExpenseCategory.HEALTH_PERSONAL_CARE, EVERYDAY_ONLY),

    // ACTIVITIES
    MUSEUM_CULTURE(ExpenseCategory.ACTIVITIES, TRAVEL_ONLY),
    TOUR_EXCURSION(ExpenseCategory.ACTIVITIES, TRAVEL_ONLY),
    NATURE_PARK(ExpenseCategory.ACTIVITIES, TRAVEL_ONLY),
    WATER_SPORTS(ExpenseCategory.ACTIVITIES, TRAVEL_ONLY),
    ADVENTURE_SPORTS(ExpenseCategory.ACTIVITIES, TRAVEL_ONLY),
    SPA_WELLNESS(ExpenseCategory.ACTIVITIES, TRAVEL_ONLY),
    TICKETS_ATTRACTIONS(ExpenseCategory.ACTIVITIES, TRAVEL_ONLY),

    // ENTERTAINMENT
    CONCERT_FESTIVAL(ExpenseCategory.ENTERTAINMENT, ALL_CONTEXTS),
    NIGHTLIFE_CLUB(ExpenseCategory.ENTERTAINMENT, ALL_CONTEXTS),
    CINEMA_THEATER(ExpenseCategory.ENTERTAINMENT, ALL_CONTEXTS),
    GAMES_ARCADE(ExpenseCategory.ENTERTAINMENT, ALL_CONTEXTS),
    SPORTS_EVENT(ExpenseCategory.ENTERTAINMENT, ALL_CONTEXTS),
    SUBSCRIPTIONS_STREAMING(ExpenseCategory.ENTERTAINMENT, EVERYDAY_ONLY),

    // SHOPPING
    CLOTHING(ExpenseCategory.SHOPPING, ALL_CONTEXTS),
    SOUVENIRS_GIFTS(ExpenseCategory.SHOPPING, TRAVEL_ONLY),
    ELECTRONICS(ExpenseCategory.SHOPPING, ALL_CONTEXTS),
    PHARMACY_HEALTH(ExpenseCategory.SHOPPING, ALL_CONTEXTS),
    CONVENIENCE_STORE(ExpenseCategory.SHOPPING, ALL_CONTEXTS),
    GIFTS(ExpenseCategory.SHOPPING, EVERYDAY_ONLY),
    TOBACCO_VAPING(ExpenseCategory.SHOPPING, EVERYDAY_ONLY),
    HOBBIES(ExpenseCategory.SHOPPING, EVERYDAY_ONLY),

    // INSURANCE
    TRAVEL_INSURANCE(ExpenseCategory.INSURANCE, TRAVEL_ONLY),
    MEDICAL_HEALTH(ExpenseCategory.INSURANCE, ALL_CONTEXTS),
    VEHICLE_INSURANCE(ExpenseCategory.INSURANCE, ALL_CONTEXTS),
    EQUIPMENT_INSURANCE(ExpenseCategory.INSURANCE, TRAVEL_ONLY),
    HOME_INSURANCE(ExpenseCategory.INSURANCE, EVERYDAY_ONLY),

    // OTHER
    TIPS_GRATUITIES(ExpenseCategory.OTHER, ALL_CONTEXTS),
    SIM_COMMUNICATIONS(ExpenseCategory.OTHER, ALL_CONTEXTS),
    LAUNDRY(ExpenseCategory.OTHER, ALL_CONTEXTS),
    FEES_SURCHARGES(ExpenseCategory.OTHER, ALL_CONTEXTS),
    PET_CARE(ExpenseCategory.OTHER, EVERYDAY_ONLY),
    UNSPECIFIED(ExpenseCategory.OTHER, ALL_CONTEXTS);

    fun appliesTo(context: GroupContext): Boolean = context in contexts

    companion object {
        fun fromString(subcategory: String): ExpenseSubcategory = entries.find {
            it.name.equals(
                subcategory,
                ignoreCase = true
            )
        } ?: throw IllegalArgumentException("Unknown subcategory: $subcategory")

        fun forCategory(category: ExpenseCategory): List<ExpenseSubcategory> =
            entries.filter { it.parentCategory == category && it != UNSPECIFIED }

        fun forCategory(
            category: ExpenseCategory,
            context: GroupContext
        ): List<ExpenseSubcategory> =
            entries.filter {
                it.parentCategory == category && it != UNSPECIFIED && it.appliesTo(context)
            }
    }
}
