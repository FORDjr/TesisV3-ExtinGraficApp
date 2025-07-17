// Configuración de la API para conectarse al servidor de la universidad
const API_CONFIG = {
  // URL del servidor de la universidad - Puerto 1609 (funcionando correctamente)
  BASE_URL: 'http://146.83.198.35:1609',

  // Endpoints de la API
  ENDPOINTS: {
    HEALTH: '/health',
    INVENTARIO: '/api/inventario',
    AUTH: '/api/auth'
  },

  // Configuración de timeouts
  TIMEOUT: 10000
}

export default API_CONFIG
