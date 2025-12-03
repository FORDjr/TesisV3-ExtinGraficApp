-- Script para verificar el estado actual de la base de datos

-- Verificar si existe la tabla productos
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public' AND table_name = 'productos';

-- Ver la estructura de la tabla productos si existe
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'productos' AND table_schema = 'public'
ORDER BY ordinal_position;

-- Ver todos los datos en la tabla productos
SELECT * FROM productos;

-- Contar registros
SELECT COUNT(*) as total_productos FROM productos;
