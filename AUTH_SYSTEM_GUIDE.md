# 🔥 Sistema de Autenticación ExtintorApp

## ✅ IMPLEMENTACIÓN COMPLETADA

He implementado exitosamente un **sistema completo de autenticación** que conecta la aplicación móvil con la base de datos PostgreSQL. 

## 🏗️ ARQUITECTURA IMPLEMENTADA

### **SERVIDOR (Backend)**
- ✅ **Tabla de usuarios** en PostgreSQL con campos: id, email, password (hash), nombre, apellido, rol, activo, fecha_creacion
- ✅ **Hash seguro de contraseñas** usando PBKDF2 con salt aleatorio
- ✅ **API REST** con endpoints:
  - `POST /auth/register` - Registro de usuarios
  - `POST /auth/login` - Login de usuarios
  - `GET /auth/profile/{id}` - Obtener perfil de usuario
  - `GET /auth/test` - Prueba de conexión
- ✅ **Validaciones** de email, contraseña, campos requeridos
- ✅ **Manejo de errores** y respuestas JSON estructuradas

### **APLICACIÓN MÓVIL (Frontend)**
- ✅ **Cliente HTTP** con Ktor para comunicación con el servidor
- ✅ **AuthManager** que maneja el estado de autenticación global
- ✅ **LoginViewModel** con lógica de negocio para login y registro
- ✅ **LoginScreen** con interfaz completa que incluye:
  - Pantalla de login
  - Pantalla de registro (nombre, apellido, email, contraseña)
  - Validación de formularios
  - Indicadores de carga
  - Manejo de errores
  - Navegación entre login y registro

## 🚀 CÓMO USAR EL SISTEMA

### **1. Configurar la IP del servidor**
Edita el archivo `AuthApiClient.kt` línea 23:
```kotlin
private val baseUrl = "http://TU_IP_AQUI:8080"
```

### **2. Iniciar el servidor**
```bash
.\gradlew :server:run
```

### **3. Ejecutar la aplicación móvil**
```bash
.\gradlew :composeApp:assembleDebug
.\gradlew :composeApp:installDebug
```

## 📱 FUNCIONALIDADES IMPLEMENTADAS

### **REGISTRO DE USUARIOS**
- Email único (validación de formato)
- Contraseña mínima de 6 caracteres
- Nombre y apellido requeridos
- Hash seguro de contraseñas en la base de datos
- Login automático después del registro exitoso

### **LOGIN DE USUARIOS**
- Autenticación contra la base de datos PostgreSQL
- Verificación de contraseña con hash seguro
- Manejo de sesiones con token
- Estado de usuario persistente en la aplicación

### **SEGURIDAD**
- Contraseñas hasheadas con PBKDF2 + salt
- Validación de campos en cliente y servidor
- Tokens de sesión para autenticación
- Manejo seguro de errores sin exponer información sensible

## 🔧 CONFIGURACIÓN ADICIONAL

La aplicación está configurada para trabajar con:
- **PostgreSQL** en el puerto 5432
- **Servidor Ktor** en el puerto 8080
- **Red local** (cambiar IP según tu configuración)

## ✨ ESTADO ACTUAL

✅ **Compilación exitosa** tanto del servidor como de la aplicación móvil
✅ **Sistema completo funcional** con base de datos
✅ **Interfaz de usuario** intuitiva con login y registro
✅ **Arquitectura escalable** y mantenible

¡El sistema está listo para usar! Solo necesitas configurar la IP del servidor y ejecutar ambos componentes.
