import os

# 1. Fix DirectivaExclusiveScreen.kt
path = "app/src/main/java/com/example/ui/screens/DirectivaExclusiveScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport kotlinx.coroutines.launch")
with open(path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done phase 4")
