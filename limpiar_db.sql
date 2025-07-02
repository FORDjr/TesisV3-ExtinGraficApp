-- Script para limpiar y recrear las tablas de la base de datos
-- Ejecuta esto en tu cliente PostgreSQL antes de reiniciar el servidor

-- Eliminar tabla si existe
DROP TABLE IF EXISTS productos CASCADE;

-- Recrear tabla con la estructura correcta
CREATE TABLE productos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10,2) NOT NULL,
    cantidad INTEGER NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL
);

-- Verificar que la tabla se creó correctamente
\d productos;

-- Insertar un producto de prueba
INSERT INTO productos (nombre, descripcion, precio, cantidad, categoria, fecha_creacion, fecha_actualizacion)
VALUES ('Producto Test', 'Producto de prueba', 99.99, 5, 'Test', NOW(), NOW());

-- Verificar que se insertó correctamente
SELECT * FROM productos;
