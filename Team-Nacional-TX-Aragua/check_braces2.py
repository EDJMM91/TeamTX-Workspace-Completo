import sys
path = 'd:/Team-Nacional-TX-Aragua/app/src/main/java/com/example/ui/screens/ProfileAndAdminScreen.kt'
lines = open(path, 'r', encoding='utf-8').read().split('\n')
depth = 0
for i, line in enumerate(lines):
    clean = line.split('//')[0].replace('\"', '')
    prev = depth
    depth += clean.count('{') - clean.count('}')
    if prev != depth:
        print(f"L{i+1}: {clean.strip()} (depth: {depth})")
