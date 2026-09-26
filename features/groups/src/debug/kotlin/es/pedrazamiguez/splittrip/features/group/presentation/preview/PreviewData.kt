package es.pedrazamiguez.splittrip.features.group.presentation.preview

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupCurrencyRateUiModel
import java.time.LocalDateTime
import kotlinx.collections.immutable.persistentListOf

val PREVIEW_USER_1 = User(
    userId = "17415e0b-7acb-40af-bd43-e7fd631116a2",
    email = "antonio@example.com",
    displayName = "Antonio García",
    profileImagePath = "https://i.pravatar.cc/150?u=antonio"
)

val PREVIEW_USER_2 = User(
    userId = "28774fc3-42e8-427f-ab7f-d3fb3642bf71",
    email = "maria@example.com",
    displayName = "María López",
    profileImagePath = "https://i.pravatar.cc/150?u=maria"
)

val PREVIEW_USER_3 = User(
    userId = "39885fd4-53f9-538g-cb8g-e4gc4753cg82",
    email = "pedro@example.com",
    displayName = "Pedro Martínez",
    profileImagePath = null
)

val PREVIEW_USER_4 = User(
    userId = "4a996ge5-64ga-649h-dc9h-f5hd5864dh93",
    email = "laura@example.com",
    displayName = "Laura Sánchez",
    profileImagePath = "https://i.pravatar.cc/150?u=laura"
)

val PREVIEW_USER_5 = User(
    userId = "5b0a7hf6-75hb-750i-ed0i-g6ie6975ei04",
    email = "jorge@example.com",
    displayName = "Jorge Ruiz",
    profileImagePath = "https://i.pravatar.cc/150?u=jorge"
)

val PREVIEW_MEMBER_PROFILES: Map<String, User> = mapOf(
    PREVIEW_USER_1.userId to PREVIEW_USER_1,
    PREVIEW_USER_2.userId to PREVIEW_USER_2,
    PREVIEW_USER_3.userId to PREVIEW_USER_3,
    PREVIEW_USER_4.userId to PREVIEW_USER_4,
    PREVIEW_USER_5.userId to PREVIEW_USER_5
)

val GROUP_DOMAIN_1: Group = Group(
    id = "91e5b13f-8e61-4410-bd6d-96872e3c213e",
    name = "Thai 2.0",
    description = "Presupuesto Tailandia 2026",
    currency = "EUR",
    extraCurrencies = listOf("THB"),
    members = listOf(
        PREVIEW_USER_1.userId,
        PREVIEW_USER_2.userId
    ),
    mainImagePath = "https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=800",
    createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    lastUpdatedAt = LocalDateTime.of(2026, 1, 1, 2, 0)
)

val GROUP_DOMAIN_2: Group = Group(
    id = "b2c4d6e8-f0a1-2345-6789-abcdef012345",
    name = "Summer Trip",
    description = "Beach vacation 2026",
    currency = "USD",
    members = listOf(
        PREVIEW_USER_1.userId,
        PREVIEW_USER_2.userId,
        PREVIEW_USER_3.userId
    ),
    createdAt = LocalDateTime.of(2026, 6, 1, 0, 0),
    lastUpdatedAt = LocalDateTime.of(2026, 6, 15, 18, 0)
)

/** Group without a cover image — gradient placeholder is shown. */
val GROUP_DOMAIN_NO_IMAGE: Group = Group(
    id = "c3d5e7f9-a1b2-3456-789a-bcdef0123456",
    name = "Tokyo Ramen Run",
    description = "Noodles and adventures",
    currency = "JPY",
    members = listOf(
        PREVIEW_USER_1.userId,
        PREVIEW_USER_2.userId,
        PREVIEW_USER_3.userId,
        PREVIEW_USER_4.userId,
        PREVIEW_USER_5.userId
    ),
    createdAt = LocalDateTime.of(2026, 3, 10, 9, 30),
    lastUpdatedAt = LocalDateTime.of(2026, 3, 10, 9, 30)
)

val GROUP_DOMAIN_MULTI_CURRENCY: Group = Group(
    id = "d4e6f8a0-b2c3-4567-89ab-cdef01234567",
    name = "World Tour",
    description = "Across 5 continents",
    currency = "EUR",
    extraCurrencies = listOf("USD", "GBP", "JPY", "THB", "CAD"),
    members = listOf(
        PREVIEW_USER_1.userId,
        PREVIEW_USER_2.userId,
        PREVIEW_USER_4.userId
    ),
    createdAt = LocalDateTime.of(2026, 5, 1, 10, 0),
    lastUpdatedAt = LocalDateTime.of(2026, 5, 10, 15, 30)
)

val PREVIEW_GROUPS = listOf(GROUP_DOMAIN_1, GROUP_DOMAIN_2)
val PREVIEW_GROUPS_WITH_MANY = listOf(GROUP_DOMAIN_1, GROUP_DOMAIN_NO_IMAGE, GROUP_DOMAIN_2)

val CURRENCY_USD = Currency(
    "USD",
    "$",
    "US Dollar",
    2
)

val CURRENCY_EUR = Currency(
    "EUR",
    "€",
    "Euro",
    2
)

val CURRENCY_MXN = Currency(
    "MXN",
    "$",
    "Mexican Peso",
    2
)

val CURRENCY_UI_EUR = CurrencyUiModel(
    code = "EUR",
    displayText = "EUR (€)",
    decimalDigits = 2,
    defaultName = "Euro",
    localizedName = "Euro"
)
val CURRENCY_UI_USD = CurrencyUiModel(
    code = "USD",
    displayText = "USD ($)",
    decimalDigits = 2,
    defaultName = "US Dollar",
    localizedName = "US Dollar"
)
val CURRENCY_UI_MXN = CurrencyUiModel(
    code = "MXN",
    displayText = "MXN ($)",
    decimalDigits = 2,
    defaultName = "Mexican Peso",
    localizedName = "Mexican Peso"
)

val PREVIEW_CURRENCY_RATES = persistentListOf(
    GroupCurrencyRateUiModel(currency = "THB", formattedRate = "1 EUR ≈ 38.25 THB")
)
