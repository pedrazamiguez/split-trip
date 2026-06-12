package es.pedrazamiguez.splittrip.domain.model

data class CropRect(
    // 0.0 to 1.0 relative to original width
    val left: Float,
    // 0.0 to 1.0 relative to original height
    val top: Float,
    // 0.0 to 1.0 relative to original width
    val right: Float,
    // 0.0 to 1.0 relative to original height
    val bottom: Float
)
