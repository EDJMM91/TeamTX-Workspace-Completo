import os

# 1. Fix InvitationCodeCleanupJob.kt imports
p = "app/src/main/java/com/example/data/remote/InvitationCodeCleanupJob.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

c = c.replace("import kotlinx.coroutines.launch", "import kotlinx.coroutines.launch\nimport kotlinx.coroutines.withContext\nimport kotlinx.coroutines.flow.first")

with open(p, "w", encoding="utf-8") as f:
    f.write(c)

print("Done phase 7")
