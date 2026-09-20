import sys
path = "features/expenses/src/main/kotlin/es/pedrazamiguez/splittrip/features/expense/presentation/screen/ExpensesScreen.kt"
with open(path, "r") as f:
    content = f.read()

content = content.replace("""                                                modifier = Modifier
                                                    .animateItem()
                                                    .sharedElementAnimation(
                                                        key = SharedElementKeys.expenseCard(expense.id),
                                                        sharedTransitionScope = sharedTransitionScope,
                                                        animatedVisibilityScope = animatedVisibilityScope
                                                    ),""", """                                                modifier = Modifier.animateItem(),
                                                innerModifier = Modifier.sharedElementAnimation(
                                                    key = SharedElementKeys.expenseCard(expense.id),
                                                    sharedTransitionScope = sharedTransitionScope,
                                                    animatedVisibilityScope = animatedVisibilityScope
                                                ),""")

with open(path, "w") as f:
    f.write(content)
