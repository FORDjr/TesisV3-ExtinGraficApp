# TesisV3 ExtinGraficApp

Plataforma de inventario/extintores con backend Ktor + PostgreSQL y app Kotlin Multiplatform (Android/iOS/Desktop). Kardex con filtros, exportación PDF/CSV y aprobación de ajustes.

## Funcionalidades
- Inventario con soft-delete, filtros y paginación.
- Movimientos: entradas, salidas, ajustes; ajustes pendientes con aprobación/rechazo.
- Kardex por producto con filtros de tipo/estado/fechas y exportación PDF/CSV.
- Restock desde Inventario registra movimiento **ENTRADA** (solo permite mantener/aumentar stock).
- Ventas generan salidas de stock automáticamente.

## Stack
- Backend: Ktor, Exposed, PostgreSQL, HikariCP, iText.
- App: Kotlin Multiplatform + Compose Multiplatform, Ktor Client.
- Infra local: Docker Compose (server + db).

## Correr rápido (Docker)
```bash
# Generar jar
JAVA_HOME= ./gradlew :server:shadowJar

# Levantar stack (usa credenciales del docker-compose)
docker compose up -d
```

Variables de entorno (ejemplo)
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

## App (KMP)
- Build debug: `JAVA_HOME= ./gradlew :composeApp:assembleDebug`
- Emulador Android: el cliente intenta `10.0.2.2:8080` como fallback.
- Kardex screen: filtros, export PDF/CSV, crear ajustes pendientes, aprobar/rechazar.

## API rápida
- Inventario:
  - `GET /api/inventario?search=&estado=&limit=&offset=`
  - `POST /api/inventario`
  - `PUT /api/inventario/{id}`
  - `PATCH /api/inventario/{id}/stock` (restock → ENTRADA; solo ≥ stock actual)
  - `PATCH /api/inventario/{id}/estado`
- Movimientos:
  - `POST /api/movimientos` (ENTRADA/SALIDA/AJUSTE; ajustes pueden ser pendientes)
  - `POST /api/movimientos/{id}/aprobar` (aprobado=true/false)
  - `GET /api/movimientos/kardex?productoId=&desde=&hasta=&tipo=&estado=`
  - `GET /api/movimientos/export/csv|pdf?productoId=...`
- Salud: `GET /health`

### Ejemplos (reemplaza `<BASE_URL>`)
```bash
# Kardex (fechas inclusivas)
curl "<BASE_URL>/api/movimientos/kardex?productoId=1&desde=2025-12-01&hasta=2025-12-31"

# Ajuste pendiente
curl -X POST <BASE_URL>/api/movimientos \
  -H "Content-Type: application/json" \
  -d '{"productoId":1,"tipo":"AJUSTE","cantidad":-2,"motivo":"Conteo físico","requiereAprobacion":true}'

# Aprobar ajuste (id=10)
curl -X POST <BASE_URL>/api/movimientos/10/aprobar \
  -H "Content-Type: application/json" \
  -d '{"aprobado":true,"usuarioId":1}'

# Restock (registra ENTRADA)
curl -X PATCH <BASE_URL>/api/inventario/1/stock \
  -H "Content-Type: application/json" \
  -d '{"cantidad":120}'
```

## Flujo sugerido
1) Levanta backend (Docker).  
2) En la app, selecciona producto → Consultar → ver Kardex.  
3) Restock desde Inventario (PATCH stock) → ENTRADA.  
4) Venta → SALIDA automática.  
5) Ajuste físico → crear pendiente → aprobar/rechazar → stock se mueve al aprobar.

## Notas
- Fechas en Kardex: `desde/hasta` son inclusivas; `YYYY-MM-DD` se interpreta como inicio/fin de día.
- Seeds incluidos (productos/usuarios/movimientos) para pruebas locales.

## Estructura
```
composeApp/   # App KMP (Android/iOS/Desktop)
server/       # Backend Ktor + Exposed
shared/       # Código común (network, models)
docker-compose.yml
```

## Próximos pasos
- Actualizar screenshots y assets.
- Subir configuraciones de despliegue al servidor final.
- Añadir instrucciones para certificados/DOMINIO cuando esté en producción.
