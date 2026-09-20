import sys

path = "features/contributions/src/main/kotlin/es/pedrazamiguez/splittrip/features/contribution/presentation/component/detail/ContributionHeroSection.kt"
with open(path, "r") as f:
    content = f.read()

# Add import if missing
if "import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys" not in content:
    content = content.replace(
        "import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedElementAnimation\n",
        "import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedElementAnimation\nimport es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys\n"
    )

content = content.replace("es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys.", "SharedElementKeys.")

with open(path, "w") as f:
    f.write(content)
