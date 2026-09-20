import sys

path = "core/design-system/src/main/kotlin/es/pedrazamiguez/splittrip/core/designsystem/transition/SharedTransitionSurface.kt"
with open(path, "r") as f:
    content = f.read()

bad = """                enter = fadeIn(tween(durationMillis = TRANSITION_DURATION_MS)),
                exit = fadeOut(tween(durationMillis = TRANSITION_DURATION_MS))"""
good = """                enter = androidx.compose.animation.EnterTransition.None,
                exit = androidx.compose.animation.ExitTransition.None"""

content = content.replace(bad, good)
with open(path, "w") as f:
    f.write(content)
