package es.pedrazamiguez.splittrip.domain.enums

enum class ExpenseSubcategory(val parentCategory: ExpenseCategory) {
    // TRANSPORT
    INTERNATIONAL_FLIGHT(ExpenseCategory.TRANSPORT),
    DOMESTIC_FLIGHT(ExpenseCategory.TRANSPORT),
    TRAIN(ExpenseCategory.TRANSPORT),
    BUS(ExpenseCategory.TRANSPORT),
    TAXI_RIDESHARE(ExpenseCategory.TRANSPORT),
    PRIVATE_TRANSFER(ExpenseCategory.TRANSPORT),
    CAR_RENTAL(ExpenseCategory.TRANSPORT),
    MOTORBIKE_RENTAL(ExpenseCategory.TRANSPORT),
    FUEL(ExpenseCategory.TRANSPORT),
    TOLLS_PARKING(ExpenseCategory.TRANSPORT),
    FERRY_BOAT(ExpenseCategory.TRANSPORT),
    PUBLIC_TRANSIT(ExpenseCategory.TRANSPORT),

    // FOOD
    RESTAURANT(ExpenseCategory.FOOD),
    STREET_FOOD(ExpenseCategory.FOOD),
    GROCERIES_SUPERMARKET(ExpenseCategory.FOOD),
    CAFE_BREAKFAST(ExpenseCategory.FOOD),
    BAR_DRINKS(ExpenseCategory.FOOD),
    DELIVERY(ExpenseCategory.FOOD),

    // LODGING
    HOTEL(ExpenseCategory.LODGING),
    HOSTEL(ExpenseCategory.LODGING),
    VACATION_RENTAL(ExpenseCategory.LODGING),
    RESORT(ExpenseCategory.LODGING),
    CAMPING(ExpenseCategory.LODGING),
    HOMESTAY(ExpenseCategory.LODGING),

    // ACTIVITIES
    MUSEUM_CULTURE(ExpenseCategory.ACTIVITIES),
    TOUR_EXCURSION(ExpenseCategory.ACTIVITIES),
    NATURE_PARK(ExpenseCategory.ACTIVITIES),
    WATER_SPORTS(ExpenseCategory.ACTIVITIES),
    ADVENTURE_SPORTS(ExpenseCategory.ACTIVITIES),
    SPA_WELLNESS(ExpenseCategory.ACTIVITIES),
    TICKETS_ATTRACTIONS(ExpenseCategory.ACTIVITIES),

    // ENTERTAINMENT
    CONCERT_FESTIVAL(ExpenseCategory.ENTERTAINMENT),
    NIGHTLIFE_CLUB(ExpenseCategory.ENTERTAINMENT),
    CINEMA_THEATER(ExpenseCategory.ENTERTAINMENT),
    GAMES_ARCADE(ExpenseCategory.ENTERTAINMENT),
    SPORTS_EVENT(ExpenseCategory.ENTERTAINMENT),

    // SHOPPING
    CLOTHING(ExpenseCategory.SHOPPING),
    SOUVENIRS_GIFTS(ExpenseCategory.SHOPPING),
    ELECTRONICS(ExpenseCategory.SHOPPING),
    PHARMACY_HEALTH(ExpenseCategory.SHOPPING),
    CONVENIENCE_STORE(ExpenseCategory.SHOPPING),

    // INSURANCE
    TRAVEL_INSURANCE(ExpenseCategory.INSURANCE),
    MEDICAL_HEALTH(ExpenseCategory.INSURANCE),
    VEHICLE_INSURANCE(ExpenseCategory.INSURANCE),
    EQUIPMENT_INSURANCE(ExpenseCategory.INSURANCE),

    // OTHER
    TIPS_GRATUITIES(ExpenseCategory.OTHER),
    SIM_COMMUNICATIONS(ExpenseCategory.OTHER),
    LAUNDRY(ExpenseCategory.OTHER),
    FEES_SURCHARGES(ExpenseCategory.OTHER),
    UNSPECIFIED(ExpenseCategory.OTHER);

    companion object {
        fun fromString(subcategory: String): ExpenseSubcategory = entries.find {
            it.name.equals(
                subcategory,
                ignoreCase = true
            )
        } ?: throw IllegalArgumentException("Unknown subcategory: $subcategory")

        fun forCategory(category: ExpenseCategory): List<ExpenseSubcategory> =
            entries.filter { it.parentCategory == category && it != UNSPECIFIED }
    }
}
