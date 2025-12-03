# TesisV3 ExtinGraficApp

Sistema de inventario y extintores con backend Ktor + PostgreSQL y app Kotlin Multiplatform (Android/iOS/Desktop). Incluye kardex con exportación PDF/CSV y flujo de aprobación de ajustes.

## 🧩 Stack
- **Backend:** Ktor, Exposed, PostgreSQL, HikariCP, iText (PDF)
- **App:** Kotlin Multiplatform + Compose Multiplatform, Ktor Client
- **Infra local:** Docker Compose (server + db)

## ✅ Qué está implementado (Fases 0-2)
- Migraciones base y seeds de productos/usuarios/movimientos.
- CRUD inventario con soft-delete y filtros.
- Movimientos de inventario (entradas, salidas, ajustes) y aprobación de ajustes pendientes.
- Kardex por producto con filtros (tipo, estado, fechas) y exportación PDF/CSV.
- Restock desde Inventario crea movimiento **ENTRADA** y solo permite mantener/aumentar stock.
- Ventas generan salidas de stock automáticamente.

## 🚀 Cómo correr rápido (Docker)
```bash
# build jar
JAVA_HOME= ./gradlew :server:shadowJar

# levantar stack (server+db con credenciales del compose)
docker compose up -d
```

Variables de entorno (ejemplo):
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

## 🔗 Endpoints clave
- Inventario:
  - `GET /api/inventario?search=&estado=&limit=&offset=`
  - `POST /api/inventario` (crear)
  - `PUT /api/inventario/{id}` (editar)
  - `PATCH /api/inventario/{id}/stock` (restock → registra ENTRADA; solo ≥ stock actual)
  - `PATCH /api/inventario/{id}/estado` (ACTIVO/INACTIVO)
- Movimientos:
  - `POST /api/movimientos` (ENTRADA/SALIDA/AJUSTE; ajustes pueden quedar pendientes)
  - `POST /api/movimientos/{id}/aprobar` (aprobado=true/false)
  - `GET /api/movimientos?kardex filters`
  - `GET /api/movimientos/kardex?productoId=&desde=&hasta=&tipo=&estado=`
  - `GET /api/movimientos/export/csv|pdf?productoId=...`
- Salud: `GET /health`

### Ejemplos rápidos (curl)
```bash
# Kardex producto 1 (fechas inclusivas)
curl "http://localhost:8080/api/movimientos/kardex?productoId=1&desde=2025-12-01&hasta=2025-12-31"

# Crear ajuste pendiente
curl -X POST http://localhost:8080/api/movimientos \
  -H "Content-Type: application/json" \
  -d '{"productoId":1,"tipo":"AJUSTE","cantidad":-2,"motivo":"Conteo físico","requiereAprobacion":true}'

# Aprobar ajuste (id=10)
curl -X POST http://localhost:8080/api/movimientos/10/aprobar \
  -H "Content-Type: application/json" \
  -d '{"aprobado":true,"usuarioId":1}'

# Restock (registra ENTRADA)
curl -X PATCH http://localhost:8080/api/inventario/1/stock \
  -H "Content-Type: application/json" \
  -d '{"cantidad":120}'
```

## 📱 App (KMP)
- Requiere SDK Android configurado.
- Build debug: `JAVA_HOME= ./gradlew :composeApp:assembleDebug`
- En emulador Android, el cliente usa `10.0.2.2:8080` como fallback.
- Pantalla Kardex: filtros, exportación PDF/CSV, creación de ajustes pendientes, aprobación/rechazo.

## 🗂️ Estructura relevante
```
composeApp/   # App KMP (Android/iOS/Desktop)
server/       # Backend Ktor + Exposed
shared/       # Código común (network, models)
docker-compose.yml
```

## 🧭 Flujo típico
1) Levantar backend (`docker compose up -d`).
2) En la app, seleccionar producto → Consultar → ver Kardex.
3) Restock desde Inventario (PATCH stock) → se registra ENTRADA.
4) Ventas generan SALIDA automáticamente.
5) Ajuste físico: crear ajuste pendiente → supervisor aprueba/rechaza → stock se mueve al aprobar.

## 🧪 Notas
- Fechas en Kardex: `desde/hasta` son inclusivas; si usas solo fecha (`YYYY-MM-DD`), se toma inicio/fin de día.
- El backend está semillado con productos, usuarios y movimientos de ejemplo.

## 📄 Licencia
Proyecto académico.
