import sys

def patch_file(path):
    with open(path, "r") as f:
        content = f.read()
    
    if "innerModifier: Modifier = Modifier," not in content:
        content = content.replace(
            "modifier: Modifier = Modifier,",
            "modifier: Modifier = Modifier,\n    innerModifier: Modifier = Modifier,"
        )
        
        # We need to find the FlatCard call and insert .then(innerModifier) ONLY there.
        # Find FlatCard(
        flatcard_idx = content.find("FlatCard(")
        if flatcard_idx != -1:
            # Find the clip call inside the FlatCard modifier
            # It's usually the first clip after FlatCard
            clip_large_idx = content.find(".clip(MaterialTheme.shapes.large)", flatcard_idx)
            clip_card_idx = content.find(".clip(cardShape)", flatcard_idx)
            
            # Find which one comes first
            idx = -1
            length = 0
            if clip_large_idx != -1 and (clip_card_idx == -1 or clip_large_idx < clip_card_idx):
                idx = clip_large_idx
                length = len(".clip(MaterialTheme.shapes.large)")
            elif clip_card_idx != -1:
                idx = clip_card_idx
                length = len(".clip(cardShape)")
            
            if idx != -1:
                # Insert .then(innerModifier) right after the clip
                insert_idx = idx + length
                content = content[:insert_idx] + "\n                .then(innerModifier)" + content[insert_idx:]
            
        with open(path, "w") as f:
            f.write(content)

patch_file("features/groups/src/main/kotlin/es/pedrazamiguez/splittrip/features/group/presentation/component/SelectedGroupCard.kt")
patch_file("features/groups/src/main/kotlin/es/pedrazamiguez/splittrip/features/group/presentation/component/GroupItem.kt")
patch_file("features/expenses/src/main/kotlin/es/pedrazamiguez/splittrip/features/expense/presentation/component/list/ExpenseItem.kt")
patch_file("features/balances/src/main/kotlin/es/pedrazamiguez/splittrip/features/balance/presentation/component/ContributionHistoryItem.kt")
