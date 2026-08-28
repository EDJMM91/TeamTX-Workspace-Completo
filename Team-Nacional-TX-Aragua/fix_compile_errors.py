import os
import re

# 1. Fix PermissionHandler.kt
path = "app/src/main/java/com/example/ui/screens/permissions/PermissionHandler.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Fix array to list
content = content.replace("val multiplePermissionsState = rememberMultiplePermissionsState(permissions)", 
                          "val multiplePermissionsState = rememberMultiplePermissionsState(permissions.toList())")

# Fix launchMultiplePermissionRequest
content = content.replace("multiplePermissionsState.launchPermissionRequest()", 
                          "multiplePermissionsState.launchMultiplePermissionRequest()")

# Fix shouldShowRationale for single permission
content = content.replace("locationPermissionState.shouldShowRationale", 
                          "locationPermissionState.status.shouldShowRationale")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 2. Fix SplashScreen.kt
path = "app/src/main/java/com/example/ui/screens/SplashScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()
imports = """import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
"""
content = content.replace("import androidx.compose.runtime.Composable", imports + "import androidx.compose.runtime.Composable")
content = content.replace("Circle", "androidx.compose.foundation.shape.CircleShape")
with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 3. Fix EmitSosDialog.kt
path = "app/src/main/java/com/example/ui/screens/dialogs/EmitSosDialog.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()
content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.foundation.clickable")
content = content.replace("outlinedButtonColors(containerColor", "outlinedButtonColors(containerColor")
# Wait, the error is:
# e: file:///D:/Team-Nacional-TX-Aragua/app/src/main/java/com/example/ui/screens/dialogs/EmitSosDialog.kt:191:33 None of the following candidates is applicable: fun outlinedButtonColors(): ButtonColors ...
# It's probably because it's ButtonDefaults.outlinedButtonColors and someone used invalid arguments.
# Let's fix that.
content = re.sub(r'ButtonDefaults\.outlinedButtonColors\([^)]*\)', 'ButtonDefaults.outlinedButtonColors()', content)
with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 4. Fix TeamTxViewModel.kt
path = "app/src/main/java/com/example/ui/viewmodel/TeamTxViewModel.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# isSuspended
content = content.replace("""suspensionStartDate = startDate,
                suspensionEndDate = endDate""", """suspensionStartTimestamp = System.currentTimeMillis(),
                suspensionEndTimestamp = if (durationDays > 0) System.currentTimeMillis() + (durationDays * 24L * 60L * 60L * 1000L) else 0L""")

content = content.replace("""suspensionStartDate = "",
                suspensionEndDate = \"\"""", """suspensionStartTimestamp = 0L,
                suspensionEndTimestamp = 0L""")

# costUsd/Ves
content = content.replace("""costUsd = costUsd,
                costVes = costVes,""", """costUsdCents = (costUsd * 100).toLong(),
                costVesCents = (costVes * 100).toLong(),""")

# amountUsd/Ves
content = content.replace("""amountUsd = amountUsd,
                amountVes = amountVes,""", """amountUsdCents = (amountUsd * 100).toLong(),
                amountVesCents = (amountVes * 100).toLong(),""")

# InventoryItem
content = content.replace("""lastMaintenanceDate = "Reciente",""", """lastMaintenanceTimestamp = System.currentTimeMillis(),""")

# EquipmentLoan
content = content.replace("""loanDate = "Hoy",
                    expectedReturnDate = expectedReturnDate,""", """loanTimestamp = System.currentTimeMillis(),
                    expectedReturnTimestamp = System.currentTimeMillis() + 86400000L,""")

content = content.replace('actualReturnDate = "Hoy"', 'actualReturnTimestamp = System.currentTimeMillis()')

# Member insertion
content = content.replace("val newId = repository.insertMember(newMem)\n                _currentMemberId.value = newId", "val newId = repository.insertMemberAndGetId(newMem)\n                _currentMemberId.value = newId")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done fixing files")
