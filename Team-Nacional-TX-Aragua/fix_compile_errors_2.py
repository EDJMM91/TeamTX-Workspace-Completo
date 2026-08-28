import os
import re

# 1. Fix SplashScreen.kt
path = "app/src/main/java/com/example/ui/screens/SplashScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("import androidx.compose.animation.animateFloatAsState", "")
content = content.replace("import androidx.compose.animation.core.animateFloatAsState", "import androidx.compose.animation.core.animateFloatAsState")

# Fix multiple Modifier imports if present
content = re.sub(r'import androidx.compose.ui.Modifier\n.*import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier', content, flags=re.DOTALL)
content = re.sub(r'import androidx.compose.runtime.Composable\n.*import androidx.compose.runtime.Composable', 'import androidx.compose.runtime.Composable', content, flags=re.DOTALL)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 2. Fix EmitSosDialog.kt
path = "app/src/main/java/com/example/ui/screens/dialogs/EmitSosDialog.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Fix outlinedButtonColors
bad_chip = """        colors = ButtonDefaults.outlinedButtonColors(),
            contentColor = Color(0xFFE2E6EE),
            borderColor = Color(0xFF283244)
        ),"""

good_chip = """        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E6EE)),
        border = BorderStroke(1.dp, Color(0xFF283244)),"""
content = content.replace(bad_chip, good_chip)
with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 3. Fix PermissionHandler.kt
path = "app/src/main/java/com/example/ui/screens/permissions/PermissionHandler.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("val PermissionStatus.shouldShowRationale: Boolean", "")
content = content.replace("Activity.shouldShowRationale(permission: String): Boolean", "")
# Fix `val locationPermissionState` can not be reassigned
content = content.replace("locationPermissionState =", "")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 4. Fix ProfileAndAdminScreen.kt
path = "app/src/main/java/com/example/ui/screens/ProfileAndAdminScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Remove the trailing code that is outside the function
bad_code = """if (showDirectivaPanel) {
    DirectivaExclusiveScreen(
        currentMember = currentMember,
        isDirectivaMode = isDirectivaMode,
        isLeaderSuperAdmin = isLeaderSuperAdmin,
        allMembers = allMembers,
        invitationCodes = emptyList(),
        accessRequests = emptyList(),
        directivaChatMessages = emptyList(),
        onGenerateCode = onGenerateCode,
        onDeleteCode = onDeleteCode,
        onApproveRequest = onApproveRequest,
        onRejectRequest = onRejectRequest,
        onTransferCargo = onTransferCargo,
        onAbandonCargo = onAbandonCargo,
        onAssignRole = onAssignRole,
        onSendDirectivaChatMessage = onSendDirectivaChatMessage,
        onRequestDirectivaAccess = onRequestDirectivaAccess,
        onUnlockWithMasterCode = onUnlockWithMasterCode,
        modifier = Modifier.fillMaxSize()
    )
}
}"""
content = content.replace(bad_code, "")
with open(path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done phase 2")
