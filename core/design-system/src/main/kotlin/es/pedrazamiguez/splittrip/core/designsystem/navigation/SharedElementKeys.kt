package es.pedrazamiguez.splittrip.core.designsystem.navigation

/**
 * Centralized shared element transition keys for cross-feature FAB → Screen animations.
 *
 * These constants are used by both the originating FAB (in a tab screen) and the
 * destination screen (in a separate feature module) to match [SharedTransitionSurface]
 * keys. Placing them here avoids a compile-time dependency between feature modules.
 */
object SharedElementKeys {
    const val ADD_CONTRIBUTION = "add_contribution_container"
    const val ADD_CASH_WITHDRAWAL = "add_cash_withdrawal_container"
    const val RECEIPT_VIEWER_SHARED_ELEMENT_KEY = "receipt_viewer_container"
}
