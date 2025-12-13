# ExtinGraficApp (Kotlin Multiplatform + Ktor)

Suite para gestión de extintores, inventario/ventas y agenda de servicios. Backend Ktor + Postgres en Docker, cliente KMP (Android/iOS/Desktop) con Compose.

## Qué hay en el repo
- `server/`: Ktor + Exposed + Postgres (Docker Compose).
- `composeApp/`: app KMP (Android, iOS, Desktop).
- `shared/`: modelos y servicios comunes.
- Artefacto desktop generado: `composeApp/build/compose/binaries/main/msi/org.example.project-1.0.0.msi` (instalador MSI).

## Credenciales demo
- Usuario admin: `admin@extingrafic.com`
- Password: `Admin123!`

## Requisitos
- JDK 21 (ej.: `C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot`).
- Docker / Docker Compose (para backend + DB).
- Android SDK configurado en `local.properties` (para build Android).
- ngrok activo si expones el backend públicamente.

## Backend (Docker)
```bash
# Desde Windows PowerShell o WSL
./gradlew :server:shadowJar --no-build-cache
docker compose build server
docker compose up -d db server   # o docker compose restart server si ya existen
```
Variables principales: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.  
Endpoint de salud: `GET /health`.

## URL base / túnel
Configurable por `SERVER_BASE_URL` (var. de entorno o `local.properties`). En este entorno se está usando:  
`https://shantae-nonimaginational-rima.ngrok-free.dev`

## App Android
```powershell
$env:SERVER_BASE_URL="https://shantae-nonimaginational-rima.ngrok-free.dev"
.\gradlew.bat :composeApp:assembleDebug
```
El APK debug queda en `composeApp/build/outputs/apk/debug/`.

## App Desktop (Windows)
```powershell
$env:SERVER_BASE_URL="https://shantae-nonimaginational-rima.ngrok-free.dev"
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
.\gradlew.bat :composeApp:run          # ejecutar
.\gradlew.bat :composeApp:packageDistributionForCurrentOS  # generar MSI
```
Instalador generado: `composeApp/build/compose/binaries/main/msi/org.example.project-1.0.0.msi`.  
Si necesitas compartirlo públicamente, súbelo como asset a GitHub Releases (no se versiona en git por tamaño).

## App iOS
- Abrir `iosApp/` en Xcode y apuntar el `SERVER_BASE_URL` en `local.properties`/entorno.
- Framework generado por el target `iosArm64/iosSimulatorArm64` (`ComposeApp`).

## Funcionalidades clave
- Extintores: creación con cliente/sede obligatoria, escaneo QR, sticker/imprimir, mover extintor, servicio y próximo vencimiento.
- Servicios y calendario: agenda unificada; diálogo “Servicio realizado” + “Próxima fecha por vencer”; títulos “Venc. EXT-xxx”; alertas duplicadas filtradas.
- Clientes y sedes: pantalla combinada, dropdowns filtrables, validación de RUT.
- Inventario/Ventas: stock crítico, flujo de ventas, devoluciones parciales, kardex/ajustes, export CSV/PDF.
- Mantención: tabs Taller/Terreno, consumo de servicios desde backend (préstamos/repuestos ocultos).

## Flujo rápido sugerido
1) Levanta backend + DB (`docker compose up -d db server`) y verifica ngrok.  
2) Desktop/Android: setea `SERVER_BASE_URL` al túnel y ejecuta `:composeApp:run` o instala el MSI.  
3) Login admin y recorre: Inventario → Ventas → Extintores (crear/mover/servicio) → Clientes/Sedes → Calendario.

## Notas y pendientes
- Backend actualiza estado de extintores según fechas; revisar envío de fabricación/última recarga en requests (casos pendientes).
- Servicios desde QR/calendario actualizan próximo vencimiento; flujo de fabricación/recarga pendiente de confirmación.
- SLF4J sin binder en desktop → logger NOP (no afecta ejecución).

## Estructura rápida
```
composeApp/   # App KMP (androidMain, iosMain, desktopMain, commonMain)
server/       # Backend Ktor
shared/       # Código común
docker-compose.yml
```
