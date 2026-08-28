# 🏍️ Team TX Venezuela — Workspace Maestro Completo (`MAPA`)

> ## ⚠️ REPOSITORIO OFICIAL Y PRIMARIO — A PARTIR DE AHORA TODO SE SUBE AQUÍ
> **Este es el repositorio oficial, completo y definitivo del proyecto Team Nacional TX Venezuela.**  
> Todos los commits, cambios de código, actualizaciones y nuevas funcionalidades deben subirse a:  
> ### 👉 **[https://github.com/EDJMM91/TeamTX-Workspace-Completo](https://github.com/EDJMM91/TeamTX-Workspace-Completo)**
>
> **REGLA PARA AGENTES**: Siempre hacer `git push` desde la raíz `D:\MAPA`, **nunca** desde `D:\MAPA\Team-Nacional-TX-Aragua`.  
> El repositorio `Team-Nacional-TX-Aragua` en GitHub existe como referencia histórica pero ya no es el primario.

---

## 📂 Arquitectura del Workspace

```
D:\MAPA/
│
├── 📁 android/
│   ├── 📁 OsmAnd/          ← Motor nativo de renderizado de mapas vectoriales
│   ├── 📁 OsmAnd-api/      ← API de integración de servicios GIS
│   ├── 📁 OsmAnd-java/     ← Lógica de geolocalización y cálculo de rutas offline
│   └── 📁 OsmAnd-shared/   ← Librerías y componentes compartidos del mapa
│
├── 📁 resources/           ← Archivos de renderizado, estilos de mapa y assets
│
└── 📁 Team-Nacional-TX-Aragua/ ← Aplicación Android (Jetpack Compose + Room + Firebase)
    ├── 📁 app/             ← Código fuente de la app, vistas, viewmodels, módulos
    ├── 📁 apk/             ← APKs oficiales de distribución OTA
    └── 📄 build.gradle.kts ← Configuración de compilación
```

---

## 🚀 Restauración Rápida del Entorno (1-Click Clone)

Si necesitas restaurar todo tu entorno de trabajo en una nueva computadora o disco:

### 1. Clonar el Workspace Completo
```bash
git clone --recurse-submodules https://github.com/EDJMM91/TeamTX-Workspace-Completo MAPA
```

### 2. Entrar a la Carpeta de la App
```bash
cd MAPA/Team-Nacional-TX-Aragua
```

### 3. Compilar el APK
```bash
./gradlew assembleDebug
```

Las rutas relativas (`../android/OsmAnd`) se enlazarán automáticamente y el proyecto compilará sin necesidad de configuración adicional.

---

## 🔗 Repositorios Relacionados

- **App Principal (Código y OTA)**: [EDJMM91/Team-Nacional-TX-Aragua](https://github.com/EDJMM91/Team-Nacional-TX-Aragua)
- **Base de Mapas Original**: [EDJMM91/WR-MapaBase](https://github.com/EDJMM91/WR-MapaBase)
- **Workspace Maestro (Este Repo)**: [EDJMM91/TeamTX-Workspace-Completo](https://github.com/EDJMM91/TeamTX-Workspace-Completo)

---

## 📄 Licencia y Derechos
Desarrollado para el **Team Nacional TX Venezuela**. Todos los derechos reservados.
