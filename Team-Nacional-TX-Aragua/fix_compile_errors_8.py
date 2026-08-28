import os
import glob
import re

# 1. Update EquipmentLoanSync, FinancialTransactionSync, InventoryItemSync
sync_methods = {
    "EquipmentLoanSync.kt": """    override suspend fun dbInsert(item: EquipmentLoan) { database.inventoryDao().insertLoan(item) }
    override suspend fun dbUpsertAll(items: List<EquipmentLoan>) { database.inventoryDao().upsertLoans(items) }
    override suspend fun dbDelete(item: EquipmentLoan) { }
    override fun dbGetAll(): Flow<List<EquipmentLoan>> = database.inventoryDao().getAllLoans()""",
    "FinancialTransactionSync.kt": """    override suspend fun dbInsert(item: FinancialTransaction) { database.financeDao().insertTransaction(item) }
    override suspend fun dbUpsertAll(items: List<FinancialTransaction>) { database.financeDao().upsertTransactions(items) }
    override suspend fun dbDelete(item: FinancialTransaction) { database.financeDao().deleteTransaction(item.id) }
    override fun dbGetAll(): Flow<List<FinancialTransaction>> = database.financeDao().getAllTransactions()""",
    "InventoryItemSync.kt": """    override suspend fun dbInsert(item: InventoryItem) { database.inventoryDao().insertItem(item) }
    override suspend fun dbUpsertAll(items: List<InventoryItem>) { database.inventoryDao().upsertItems(items) }
    override suspend fun dbDelete(item: InventoryItem) { }
    override fun dbGetAll(): Flow<List<InventoryItem>> = database.inventoryDao().getAllItems()""",
}

for sync_file, methods in sync_methods.items():
    p = f"app/src/main/java/com/example/data/remote/{sync_file}"
    if os.path.exists(p):
        with open(p, "r", encoding="utf-8") as f:
            c = f.read()
        if "override fun getDao" in c:
            c = re.sub(r'override fun getDao\(\).*', methods, c)
        else:
            # maybe it wasn't there?
            pass
        
        # also add import Flow
        if "import kotlinx.coroutines.flow.Flow" not in c:
            c = c.replace("import kotlinx.coroutines.CoroutineScope", "import kotlinx.coroutines.flow.Flow\nimport kotlinx.coroutines.CoroutineScope")
            
        with open(p, "w", encoding="utf-8") as f:
            f.write(c)

# Add Flow import to other sync files
other_sync = [
    "InvitationCodeSync.kt",
    "MemberSync.kt",
    "NoticeCommentSync.kt",
    "PublicationSync.kt",
    "RideRegistrationSync.kt",
    "RideSync.kt",
    "RoleConfigSync.kt"
]
for sync_file in other_sync:
    p = f"app/src/main/java/com/example/data/remote/{sync_file}"
    if os.path.exists(p):
        with open(p, "r", encoding="utf-8") as f:
            c = f.read()
        if "import kotlinx.coroutines.flow.Flow" not in c:
            c = c.replace("import kotlinx.coroutines.CoroutineScope", "import kotlinx.coroutines.flow.Flow\nimport kotlinx.coroutines.CoroutineScope")
            with open(p, "w", encoding="utf-8") as f:
                f.write(c)

# RideSync fix status
p = "app/src/main/java/com/example/data/remote/RideSync.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()
c = c.replace('item.copy(status = "CANCELADO")', 'item.copy(status = com.example.data.model.RideStatus.CANCELADO)')
with open(p, "w", encoding="utf-8") as f:
    f.write(c)

# 2. Fix FirebaseChatSync
p = "app/src/main/java/com/example/data/remote/FirebaseChatSync.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()
c = c.replace("import kotlinx.coroutines.channels.Channel", "import kotlinx.coroutines.channels.Channel\nimport kotlinx.coroutines.channels.receive\nimport kotlinx.coroutines.channels.send")
# wait trySend is a member function of Channel, it should be resolved unless Channel isn't imported correctly.
# Oh, in Kotlin 1.5+ trySend is a regular function on SendChannel.
# But wait, send and receive are also extension functions or suspend member functions.
# Let's print FirebaseChatSync.kt to see why it fails.
with open("chat_sync_temp.txt", "w") as f:
    f.write(c)

# 3. Fix MembersScreen.kt
p = "app/src/main/java/com/example/ui/screens/MembersScreen.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()
# 544 and 570
# It might be `member.phone` or something expecting Long but getting String.
# Wait, ID? Let's check MembersScreen.kt content later or just print those lines.
with open("members_temp.txt", "w", encoding="utf-8") as f:
    lines = c.splitlines()
    if len(lines) >= 570:
        f.write(lines[543] + "\n" + lines[569])

# 4. Fix SplashScreen.kt
p = "app/src/main/java/com/example/ui/screens/SplashScreen.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()
# add compose imports
c = c.replace("package com.example.ui.screens", "package com.example.ui.screens\n\nimport androidx.compose.animation.core.*\nimport androidx.compose.foundation.*\nimport androidx.compose.foundation.layout.*\nimport androidx.compose.material3.*\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.graphics.*\nimport androidx.compose.ui.unit.*")
with open(p, "w", encoding="utf-8") as f:
    f.write(c)

print("Done phase 8")
