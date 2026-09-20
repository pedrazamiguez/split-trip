import sys
path = "features/groups/src/main/kotlin/es/pedrazamiguez/splittrip/features/group/presentation/component/GroupsListContent.kt"
with open(path, "r") as f:
    content = f.read()

content = content.replace("""                    modifier = Modifier
                        .animateItem(fadeInSpec = null, fadeOutSpec = null)
                        .sharedElementAnimation(
                            key = SharedElementKeys.groupCard(selectedGroup.id),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        ),""", """                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                    innerModifier = Modifier.sharedElementAnimation(
                        key = SharedElementKeys.groupCard(selectedGroup.id),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    ),""")

content = content.replace("""                modifier = Modifier
                    .animateItem()
                    .sharedElementAnimation(
                        key = SharedElementKeys.groupCard(group.id),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    ),""", """                modifier = Modifier.animateItem(),
                innerModifier = Modifier.sharedElementAnimation(
                    key = SharedElementKeys.groupCard(group.id),
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                ),""")

with open(path, "w") as f:
    f.write(content)
