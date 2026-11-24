package es.pedrazamiguez.splittrip.domain.enums

enum class ExpenseCategory(val contexts: Set<GroupContext>) {
    TRANSPORT(setOf(GroupContext.TRAVEL, GroupContext.EVERYDAY)),
    FOOD(setOf(GroupContext.TRAVEL, GroupContext.EVERYDAY)),
    LODGING(setOf(GroupContext.TRAVEL)),
    HOME(setOf(GroupContext.EVERYDAY)),
    HEALTH_PERSONAL_CARE(setOf(GroupContext.EVERYDAY)),
    ACTIVITIES(setOf(GroupContext.TRAVEL)),
    INSURANCE(setOf(GroupContext.TRAVEL, GroupContext.EVERYDAY)),
    ENTERTAINMENT(setOf(GroupContext.TRAVEL, GroupContext.EVERYDAY)),
    SHOPPING(setOf(GroupContext.TRAVEL, GroupContext.EVERYDAY)),
    OTHER(setOf(GroupContext.TRAVEL, GroupContext.EVERYDAY));

    fun appliesTo(context: GroupContext): Boolean = context in contexts

    companion object {
        fun fromString(category: String): ExpenseCategory = entries.find {
            it.name.equals(
                category,
                ignoreCase = true
            )
        } ?: throw IllegalArgumentException("Unknown category: $category")

        fun forContext(context: GroupContext): List<ExpenseCategory> =
            entries.filter { it.appliesTo(context) }
    }
}
