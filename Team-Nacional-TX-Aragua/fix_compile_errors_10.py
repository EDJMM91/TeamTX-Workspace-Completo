import os
import re

# 1. Fix PermissionHandler.kt
p = "app/src/main/java/com/example/ui/screens/permissions/PermissionHandler.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

c = c.replace("val showRationale by remember { mutableStateOf(false) }", "var showRationale by remember { mutableStateOf(false) }")
c = re.sub(r"val shouldShowRationaleForAny = permissions\.any \{.*?\}.*?if \(shouldShowRationaleForAny", "val shouldShowRationaleForAny = multiplePermissionsState.shouldShowRationale\n        \n        if (shouldShowRationaleForAny", c, flags=re.DOTALL)

with open(p, "w", encoding="utf-8") as f:
    f.write(c)

print("Done phase 10")
