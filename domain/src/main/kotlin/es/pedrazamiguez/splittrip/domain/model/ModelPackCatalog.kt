package es.pedrazamiguez.splittrip.domain.model

data class ModelPackCatalog(
    val packs: List<ModelPackConfig>
) {
    val enabledPacks: List<ModelPackConfig>
        get() = packs.filter { it.isEnabled }

    fun findById(packId: String): ModelPackConfig? =
        packs.firstOrNull { it.packId == packId }

    companion object {
        val EMPTY = ModelPackCatalog(packs = emptyList())
    }
}
