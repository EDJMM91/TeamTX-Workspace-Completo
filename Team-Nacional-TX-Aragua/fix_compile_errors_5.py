import os

# 1. Fix TeamTxRepository.kt
path = "app/src/main/java/com/example/data/repository/TeamTxRepository.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("import kotlinx.coroutines.flow.Flow", "import kotlinx.coroutines.flow.Flow\nimport kotlinx.coroutines.flow.first")
with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 2. Fix SplashScreen.kt
path = "app/src/main/java/com/example/ui/screens/SplashScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("import androidx.compose.animation.core.animateFloatAsState", "import androidx.compose.animation.core.animateFloatAsState\nimport androidx.compose.animation.core.tween")
with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 3. Fix BaseFirestoreSync.kt issue
# The issue is probably that Room Dao generic parameter is now different or we need to look at MemberSync.kt
path = "app/src/main/java/com/example/data/remote/MemberSync.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Let's print it to see why it fails
print(content)
