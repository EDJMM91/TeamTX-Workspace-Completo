import os

p = "app/src/main/java/com/example/TeamTxApplication.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

c = c.replace("import com.google.firebase.appdistribution.FirebaseAppDistribution\n", "")
c = c.replace("import com.google.firebase.appdistribution.FirebaseAppDistribution", "")
c = c.replace("        FirebaseAppDistribution.getInstance().setInAppUpdateEnabled(true)\n", "")
c = c.replace("        FirebaseAppDistribution.getInstance().setInAppUpdateEnabled(true)", "")

with open(p, "w", encoding="utf-8") as f:
    f.write(c)

print("Done phase 11")
