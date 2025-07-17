import API_CONFIG from './api-config'

interface Producto {
  id: number
  nombre: string
  categoria: string
  stock: number
  stockMinimo: number
  precio: number
  costo: number
  proveedor: string
  fechaIngreso: string
  estado: string
}

export class ApiService {
  private baseURL: string

  constructor() {
    this.baseURL = API_CONFIG.BASE_URL
  }

  // Método genérico para hacer peticiones HTTP
  async request(endpoint: string, options: RequestInit = {}): Promise<any> {
    try {
      const response = await fetch(`${this.baseURL}${endpoint}`, {
        headers: {
          'Content-Type': 'application/json',
          ...options.headers
        },
        signal: AbortSignal.timeout(API_CONFIG.TIMEOUT),
        ...options
      })

      if (!response.ok) {
        throw new Error(`Error HTTP: ${response.status}`)
      }

      const text = await response.text()
      return text ? JSON.parse(text) : null
    } catch (error) {
      console.error(`Error en petición ${endpoint}:`, error)
      throw error
    }
  }

  // Métodos HTTP simplificados
  async get(endpoint: string) {
    return { data: await this.request(endpoint, { method: 'GET' }) }
  }

  async post(endpoint: string, data: any) {
    return {
      data: await this.request(endpoint, {
        method: 'POST',
        body: JSON.stringify(data)
      })
    }
  }

  async put(endpoint: string, data: any) {
    return {
      data: await this.request(endpoint, {
        method: 'PUT',
        body: JSON.stringify(data)
      })
    }
  }

  async delete(endpoint: string) {
    return { data: await this.request(endpoint, { method: 'DELETE' }) }
  }

  // Verificar conexión con el servidor
  async verificarConexion(): Promise<boolean> {
    try {
      await this.request(API_CONFIG.ENDPOINTS.HEALTH)
      return true
    } catch (error) {
      console.error('Error verificando conexión:', error)
      return false
    }
  }

  // Obtener todos los productos
  async obtenerProductos(): Promise<Producto[]> {
    try {
      const response = await fetch(`${this.baseURL}${API_CONFIG.ENDPOINTS.INVENTARIO}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        },
        signal: AbortSignal.timeout(API_CONFIG.TIMEOUT)
      })

      if (!response.ok) {
        throw new Error(`Error HTTP: ${response.status}`)
      }

      const productos = await response.json()
      return productos
    } catch (error) {
      console.error('Error obteniendo productos:', error)
      // Devolver datos de ejemplo si falla la conexión
      return this.getDatosEjemplo()
    }
  }

  // Crear nuevo producto
  async crearProducto(producto: Omit<Producto, 'id'>): Promise<Producto | null> {
    try {
      const response = await fetch(`${this.baseURL}${API_CONFIG.ENDPOINTS.INVENTARIO}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(producto),
        signal: AbortSignal.timeout(API_CONFIG.TIMEOUT)
      })

      if (!response.ok) {
        throw new Error(`Error HTTP: ${response.status}`)
      }

      return await response.json()
    } catch (error) {
      console.error('Error creando producto:', error)
      return null
    }
  }

  // Actualizar producto
  async actualizarProducto(id: number, producto: Partial<Producto>): Promise<Producto | null> {
    try {
      const response = await fetch(`${this.baseURL}${API_CONFIG.ENDPOINTS.INVENTARIO}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(producto),
        signal: AbortSignal.timeout(API_CONFIG.TIMEOUT)
      })

      if (!response.ok) {
        throw new Error(`Error HTTP: ${response.status}`)
      }

      return await response.json()
    } catch (error) {
      console.error('Error actualizando producto:', error)
      return null
    }
  }

  // Eliminar producto
  async eliminarProducto(id: number): Promise<boolean> {
    try {
      const response = await fetch(`${this.baseURL}${API_CONFIG.ENDPOINTS.INVENTARIO}/${id}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json'
        },
        signal: AbortSignal.timeout(API_CONFIG.TIMEOUT)
      })

      return response.ok
    } catch (error) {
      console.error('Error eliminando producto:', error)
      return false
    }
  }

  // Datos de ejemplo para cuando no hay conexión
  private getDatosEjemplo(): Producto[] {
    return [
      {
        id: 1,
        nombre: "Laptop Dell XPS 13",
        categoria: "Electrónicos",
        stock: 15,
        stockMinimo: 5,
        precio: 1299.99,
        costo: 999.99,
        proveedor: "Dell Inc.",
        fechaIngreso: "2024-01-10",
        estado: "Activo",
      },
      {
        id: 2,
        nombre: "Mouse Logitech MX Master",
        categoria: "Accesorios",
        stock: 3,
        stockMinimo: 10,
        precio: 99.99,
        costo: 65.99,
        proveedor: "Logitech",
        fechaIngreso: "2024-01-15",
        estado: "Activo",
      }
    ]
  }
}

// Instancia singleton del servicio de API
export const apiService = new ApiService()
