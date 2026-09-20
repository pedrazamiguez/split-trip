import os

def patch_file(path, component_name):
    with open(path, "r") as f:
        content = f.read()
    
    if "innerModifier: Modifier = Modifier," not in content:
        content = content.replace(
            "modifier: Modifier = Modifier,",
            "modifier: Modifier = Modifier,\n    innerModifier: Modifier = Modifier,"
        )
        
        # In the FlatCard call, apply innerModifier
        content = content.replace(
            ".clip(MaterialTheme.shapes.large)",
            ".clip(MaterialTheme.shapes.large)\n                .then(innerModifier)"
        ).replace(
            ".clip(cardShape)",
            ".clip(cardShape)\n                .then(innerModifier)"
        )
        
        with open(path, "w") as f:
            f.write(content)

patch_file("features/groups/src/main/kotlin/es/pedrazamiguez/splittrip/features/group/presentation/component/SelectedGroupCard.kt", "SelectedGroupCard")
patch_file("features/groups/src/main/kotlin/es/pedrazamiguez/splittrip/features/group/presentation/component/GroupItem.kt", "GroupItem")
patch_file("features/expenses/src/main/kotlin/es/pedrazamiguez/splittrip/features/expense/presentation/component/list/ExpenseItem.kt", "ExpenseItem")
patch_file("features/balances/src/main/kotlin/es/pedrazamiguez/splittrip/features/balance/presentation/component/ContributionHistoryItem.kt", "ContributionHistoryItem")
