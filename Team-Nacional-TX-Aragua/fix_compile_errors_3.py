import os

# 1. Fix MembersScreen.kt
path = "app/src/main/java/com/example/ui/screens/MembersScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("suspensionEndDate =", "suspensionEndTimestamp =")
content = content.replace("suspensionStartDate =", "suspensionStartTimestamp =")
content = content.replace("costUsd =", "costUsdCents =")
content = content.replace("costVes =", "costVesCents =")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 2. Fix ProfileAndAdminScreen.kt
path = "app/src/main/java/com/example/ui/screens/ProfileAndAdminScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.foundation.clickable")
with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 3. Fix EmergencySosScreen.kt
path = "app/src/main/java/com/example/ui/screens/EmergencySosScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport com.example.ui.screens.dialogs.EmitSosDialog")
with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 4. Fix PermissionHandler.kt
path = "app/src/main/java/com/example/ui/screens/permissions/PermissionHandler.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# I replaced `locationPermissionState =` with `` earlier. I need to undo that or just fix it.
# Wait, I don't know exactly what I did. I will just run a regex to restore the state initialization if it's broken.
# The error is: `val  rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)` which is bad.
content = content.replace("val  rememberPermissionState", "val locationPermissionState = rememberPermissionState")

# Fix `Activity.shouldShowRationale` since it's an internal function that can't be used
content = content.replace("Activity.shouldShowRationale", "shouldShowRationale")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done phase 3")
