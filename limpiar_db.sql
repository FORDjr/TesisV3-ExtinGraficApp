-- Limpia todas las tablas de la app para que el seeder repueble con datos frescos.
-- Úsalo desde tu cliente Postgres (DBeaver: abre una pestaña SQL sobre tu DB y ejecuta).

BEGIN;

-- Relaciones hijas primero
DROP TABLE IF EXISTS venta_productos CASCADE;
DROP TABLE IF EXISTS ventas CASCADE;

DROP TABLE IF EXISTS service_registro_productos CASCADE;
DROP TABLE IF EXISTS service_registros CASCADE;

DROP TABLE IF EXISTS alertas CASCADE;
DROP TABLE IF EXISTS certificados CASCADE;
DROP TABLE IF EXISTS orden_servicio_extintores CASCADE;
DROP TABLE IF EXISTS ordenes_servicio CASCADE;

DROP TABLE IF EXISTS extintores CASCADE;
DROP TABLE IF EXISTS sedes CASCADE;
DROP TABLE IF EXISTS clientes CASCADE;

DROP TABLE IF EXISTS movimientos_inventario CASCADE;
DROP TABLE IF EXISTS proveedores CASCADE;
DROP TABLE IF EXISTS productos CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;

COMMIT;

-- Después de ejecutar:
-- 1) Levanta el backend (./gradlew run o docker compose up) para que DatabaseSeeder repueble.
-- 2) Refresca en DBeaver para ver los datos nuevos.
