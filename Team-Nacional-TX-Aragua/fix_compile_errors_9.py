import os

# Fix MembersScreen.kt strings for suspensionEndTimestamp
p = "app/src/main/java/com/example/ui/screens/MembersScreen.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

c = c.replace('suspensionEndTimestamp = ""', 'suspensionEndTimestamp = 0L')
c = c.replace('suspensionEndTimestamp = if (days > 0) "+$days dA-as" else "Indefinida"', 'suspensionEndTimestamp = if (days > 0) System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L else -1L')

with open(p, "w", encoding="utf-8") as f:
    f.write(c)

print("Done phase 9")
