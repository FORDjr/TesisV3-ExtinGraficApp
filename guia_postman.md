# GUÍA PARA PROBAR LA API DE INVENTARIO CON POSTMAN

## 1. Verificar que el servidor funciona
GET http://localhost:8080
- Debería responder: "Servidor de Inventario funcionando correctamente"

GET http://localhost:8080/health
- Debería responder: "Base de datos conectada - Inventario API v1.0"

## 2. CREAR UN PRODUCTO NUEVO
POST http://localhost:8080/api/inventario
Content-Type: application/json

Body (JSON):
{
    "nombre": "Laptop Dell",
    "descripcion": "Laptop Dell Inspiron 15 3000",
    "precio": 599.99,
    "cantidad": 10,
    "categoria": "Electrónicos"
}

## 3. OBTENER TODOS LOS PRODUCTOS
GET http://localhost:8080/api/inventario

## 4. OBTENER UN PRODUCTO POR ID
GET http://localhost:8080/api/inventario/1

## 5. ACTUALIZAR UN PRODUCTO
PUT http://localhost:8080/api/inventario/1
Content-Type: application/json

Body (JSON):
{
    "nombre": "Laptop Dell Actualizada",
    "descripcion": "Laptop Dell Inspiron 15 3000 - Actualizada",
    "precio": 649.99,
    "cantidad": 8,
    "categoria": "Electrónicos"
}

## 6. ACTUALIZAR SOLO EL STOCK
PATCH http://localhost:8080/api/inventario/1/stock
Content-Type: application/json

Body (JSON):
{
    "cantidad": 5
}

## 7. ELIMINAR UN PRODUCTO
DELETE http://localhost:8080/api/inventario/1

## 8. BUSCAR PRODUCTOS POR CATEGORÍA
GET http://localhost:8080/api/inventario/categoria/Electrónicos

## 9. BUSCAR PRODUCTOS CON STOCK BAJO
GET http://localhost:8080/api/inventario/stock-bajo?limite=5

---

## PRUEBAS RECOMENDADAS EN ORDEN:

1. **Verificar servidor**: GET /
2. **Verificar salud**: GET /health
3. **Crear producto**: POST /api/inventario (usar el JSON de ejemplo)
4. **Listar productos**: GET /api/inventario
5. **Obtener por ID**: GET /api/inventario/1
6. **Actualizar producto**: PUT /api/inventario/1
7. **Actualizar stock**: PATCH /api/inventario/1/stock
8. **Buscar por categoría**: GET /api/inventario/categoria/Electrónicos
9. **Verificar stock bajo**: GET /api/inventario/stock-bajo?limite=5
10. **Eliminar producto**: DELETE /api/inventario/1

## NOTAS IMPORTANTES:
- Asegúrate de que el Content-Type sea "application/json" para las peticiones POST, PUT y PATCH
- Los endpoints que esperan JSON deben tener el header Content-Type configurado
- Puedes crear varios productos diferentes para probar mejor las funcionalidades
- El servidor usa PostgreSQL, así que los datos se persisten entre reinicios
