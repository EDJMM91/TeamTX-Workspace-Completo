# SHA Certificates - Team TX

## Firma de Debug (Android Studio)

```
SHA-1:   ED:AB:5F:1B:C2:59:19:33:F1:9F:EF:4B:2C:B7:41:EF:65:00:FA:83
SHA-256: 88:81:64:A4:B3:0E:2C:07:31:03:D2:CD:91:F0:33:C8:67:C2:02:02:1E:07:B0:2D:B8:AE:5C:BF:A2:72:10:A8
```

## Firma de Release (my-upload-key.jks)

```
SHA-1:   10:84:47:F9:E6:D1:DD:F2:04:05:E1:9F:3C:5A:E5:02:13:CB:05:50
SHA-256: 8A:3F:AC:63:F7:23:62:DF:02:16:7C:51:87:86:6C:A3:67:2D:49:21:B4:DD:ED:51:E2:C7:E8:0E:EC:13:7D:FB
```

## Dónde pegarlos

1. Firebase Console → Project Settings (engranadera arriba)
2. Sección "Your apps" → seleccionar la app
3. SHA certificate fingerprints → Add fingerprint
4. Pegar ambos (SHA-1 y SHA-256)

## Keystore Info

- Archivo: `my-upload-key.jks` (raíz del proyecto)
- Alias: `upload`
- Password: `TeamTX2026!`
