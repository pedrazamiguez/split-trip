import sys
path = "features/balances/src/main/kotlin/es/pedrazamiguez/splittrip/features/balance/presentation/component/BalancesListContent.kt"
with open(path, "r") as f:
    content = f.read()

content = content.replace("""                    modifier = Modifier
                        .animateItem()
                        .sharedElementAnimation(
                            key = SharedElementKeys.contributionCard(item.id),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )""", """                    modifier = Modifier.animateItem(),
                    innerModifier = Modifier.sharedElementAnimation(
                        key = SharedElementKeys.contributionCard(item.id),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )""")

with open(path, "w") as f:
    f.write(content)
