            Nueva Venta
          </Button>
        </div>
"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Plus, TrendingUp, DollarSign, ShoppingCart, Search, Filter, Download, RefreshCw } from "lucide-react"
import { NuevaVentaDialog } from "./nueva-venta-dialog"
            <div className="text-2xl font-bold">${metricas.ventasHoy.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground">
              {metricas.crecimientoVentasHoy >= 0 ? '+' : ''}{metricas.crecimientoVentasHoy}% vs ayer
            </p>

interface ProductoVenta {
  id: number
  nombre: string
  cantidad: number
  precio: number
  subtotal: number
}

            <div className="text-2xl font-bold">{metricas.ordenesHoy}</div>
            <p className="text-xs text-muted-foreground">
              {metricas.crecimientoOrdenes >= 0 ? '+' : ''}{metricas.crecimientoOrdenes} vs ayer
            </p>
  cliente: string
  fecha: string
  total: number
  estado: string
  metodoPago: string
  observaciones?: string
  productos: ProductoVenta[]
}

            <div className="text-2xl font-bold">${metricas.ticketPromedio.toFixed(2)}</div>
            <p className="text-xs text-muted-foreground">
              {metricas.crecimientoTicket >= 0 ? '+' : ''}{metricas.crecimientoTicket}% vs ayer
            </p>
  ordenesHoy: number
  ticketPromedio: number
  ventasMes: number
  crecimientoVentasHoy: number
  crecimientoOrdenes: number
  crecimientoTicket: number
  crecimientoMes: number
}

            <div className="text-2xl font-bold">${metricas.ventasMes.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground">
              {metricas.crecimientoMes >= 0 ? '+' : ''}{metricas.crecimientoMes}% vs mes anterior
            </p>
  const [metricas, setMetricas] = useState<MetricasVentas>({
    ventasHoy: 0,
    ordenesHoy: 0,
    ticketPromedio: 0,
    ventasMes: 0,
    crecimientoVentasHoy: 0,
    crecimientoOrdenes: 0,
    crecimientoTicket: 0,
    crecimientoMes: 0
  })
  const [loading, setLoading] = useState(true)
  const [busqueda, setBusqueda] = useState("")
  const [filtroEstado, setFiltroEstado] = useState("todos")
  const [filtroFecha, setFiltroFecha] = useState("todos")
  const [ventaSeleccionada, setVentaSeleccionada] = useState<Venta | null>(null)
              <p className="text-xs text-muted-foreground mt-1">Integración con librería de gráficos pendiente</p>
  const [mostrarNuevaVenta, setMostrarNuevaVenta] = useState(false)
  const [mostrarDetalle, setMostrarDetalle] = useState(false)

  useEffect(() => {
    cargarDatos()
      {/* Filtros y búsqueda */}

  const cargarDatos = async () => {
          <CardTitle>Historial de Ventas</CardTitle>
          <CardDescription>
            {ventasFiltradas.length} de {ventas.length} ventas
          </CardDescription>
      const [ventasResponse, metricasResponse] = await Promise.all([
        apiService.get('/ventas'),
          <div className="flex flex-col sm:flex-row gap-4 mb-6">
            <div className="relative flex-1">
              <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Buscar por cliente o ID de venta..."
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
                className="pl-8"
              />
            </div>

            <Select value={filtroEstado} onValueChange={setFiltroEstado}>
              <SelectTrigger className="w-[180px]">
                <Filter className="mr-2 h-4 w-4" />
                <SelectValue placeholder="Estado" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="todos">Todos los estados</SelectItem>
                <SelectItem value="Completada">Completada</SelectItem>
                <SelectItem value="Pendiente">Pendiente</SelectItem>
                <SelectItem value="Cancelada">Cancelada</SelectItem>
              </SelectContent>
            </Select>

            <Select value={filtroFecha} onValueChange={setFiltroFecha}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Fecha" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="todos">Todas las fechas</SelectItem>
                <SelectItem value="hoy">Hoy</SelectItem>
                <SelectItem value="semana">Esta semana</SelectItem>
                <SelectItem value="mes">Este mes</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Tabla de ventas */}
        apiService.get('/ventas/metricas')
      ])

      setVentas(ventasResponse.data || [])
      setMetricas(metricasResponse.data || metricas)
    } catch (error) {
      console.error('Error al cargar datos:', error)
      // Usar datos de ejemplo si falla la API
      setVentas(ventasEjemplo)
                    <th className="h-12 px-4 text-left align-middle font-medium">Método Pago</th>
    } finally {
      setLoading(false)
    }
  }

                  {loading ? (
                    <tr>
                      <td colSpan={7} className="h-24 text-center">
                        <div className="flex items-center justify-center">
                          <RefreshCw className="h-4 w-4 animate-spin mr-2" />
                          Cargando ventas...
                        </div>
                      </td>
                    </tr>
                  ) : ventasFiltradas.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="h-24 text-center text-muted-foreground">
                        No se encontraron ventas
                      </td>
                    </tr>
                  ) : (
                    ventasFiltradas.map((venta) => (
                      <tr key={venta.id} className="border-b hover:bg-muted/50">
                        <td className="p-4 font-medium">{venta.id}</td>
                        <td className="p-4">{venta.cliente}</td>
                        <td className="p-4">
                          {new Date(venta.fecha).toLocaleDateString('es-ES')}
                        </td>
                        <td className="p-4 font-medium">${venta.total.toFixed(2)}</td>
                        <td className="p-4 capitalize">{venta.metodoPago}</td>
                        <td className="p-4">{getEstadoBadge(venta.estado)}</td>
                        <td className="p-4">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleVerDetalle(venta)}
                          >
                            Ver Detalles
                          </Button>
                        </td>
                      </tr>
                    ))
                  )}
      cliente: "Carlos López",
      fecha: "2024-01-15T14:20:00Z",
      total: 149.5,
      estado: "Pendiente",
      metodoPago: "efectivo",
      productos: [

      {/* Diálogos */}
      <NuevaVentaDialog
        open={mostrarNuevaVenta}
        onOpenChange={setMostrarNuevaVenta}
        onVentaCreada={handleVentaCreada}
      />

      <DetalleVentaDialog
        open={mostrarDetalle}
        onOpenChange={setMostrarDetalle}
        venta={ventaSeleccionada}
      />
        { id: 2, nombre: "Producto B", cantidad: 1, precio: 149.50, subtotal: 149.50 }
      ]
    },
    {
      id: "V003",
      cliente: "Ana Martínez",
      fecha: "2024-01-14T16:45:00Z",
      total: 89.99,
      estado: "Completada",
      metodoPago: "transferencia",
      productos: [
        { id: 3, nombre: "Producto C", cantidad: 1, precio: 89.99, subtotal: 89.99 }
      ]
    },
    {
      id: "V004",
      cliente: "Luis Rodríguez",
      fecha: "2024-01-14T11:15:00Z",
      total: 199.99,
      estado: "Cancelada",
      metodoPago: "credito",
      productos: [
        { id: 4, nombre: "Producto D", cantidad: 1, precio: 199.99, subtotal: 199.99 }
      ]
    },
    {
      id: "V005",
      cliente: "Elena Fernández",
      fecha: "2024-01-13T09:30:00Z",
      total: 349.99,
      estado: "Completada",
      metodoPago: "tarjeta",
      productos: [
        { id: 5, nombre: "Producto E", cantidad: 1, precio: 349.99, subtotal: 349.99 }
      ]
    }
  ]

  const getEstadoBadge = (estado: string) => {
    switch (estado) {
      case "Completada":
        return <Badge variant="default" className="bg-green-500">Completada</Badge>
      case "Pendiente":
        return <Badge variant="secondary">Pendiente</Badge>
      case "Cancelada":
        return <Badge variant="destructive">Cancelada</Badge>
      default:
        return <Badge variant="outline">{estado}</Badge>
    }
  }

  const ventasFiltradas = ventas.filter(venta => {
    const coincideBusqueda = venta.cliente.toLowerCase().includes(busqueda.toLowerCase()) ||
                            venta.id.toLowerCase().includes(busqueda.toLowerCase())

    const coincideEstado = filtroEstado === "todos" || venta.estado === filtroEstado

    let coincideFecha = true
    if (filtroFecha !== "todos") {
      const fechaVenta = new Date(venta.fecha)
      const hoy = new Date()

      switch (filtroFecha) {
        case "hoy":
          coincideFecha = fechaVenta.toDateString() === hoy.toDateString()
          break
        case "semana":
          const inicioSemana = new Date(hoy)
          inicioSemana.setDate(hoy.getDate() - 7)
          coincideFecha = fechaVenta >= inicioSemana
          break
        case "mes":
          coincideFecha = fechaVenta.getMonth() === hoy.getMonth() &&
                         fechaVenta.getFullYear() === hoy.getFullYear()
          break
      }
    }

    return coincideBusqueda && coincideEstado && coincideFecha
  })

  const handleVerDetalle = (venta: Venta) => {
    setVentaSeleccionada(venta)
    setMostrarDetalle(true)
  }

  const handleVentaCreada = () => {
    cargarDatos()
  }

  const exportarVentas = () => {
    // Implementar exportación a CSV/Excel
    console.log("Exportando ventas...")
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold">Ventas</h1>
          <p className="text-muted-foreground">Gestiona tus ventas y pedidos</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={cargarDatos} disabled={loading}>
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Actualizar
          </Button>
          <Button variant="outline" onClick={exportarVentas}>
            <Download className="mr-2 h-4 w-4" />
            Exportar
          </Button>
          <Button onClick={() => setMostrarNuevaVenta(true)}>
            <Plus className="mr-2 h-4 w-4" />
      </div>

      {/* Métricas de ventas */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Ventas Hoy</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">$2,350</div>
            <p className="text-xs text-muted-foreground">+12% vs ayer</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Órdenes Hoy</CardTitle>
            <ShoppingCart className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">23</div>
            <p className="text-xs text-muted-foreground">+5 vs ayer</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Ticket Promedio</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">$102.17</div>
            <p className="text-xs text-muted-foreground">+8% vs ayer</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Ventas Mes</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">$45,231</div>
            <p className="text-xs text-muted-foreground">+20% vs mes anterior</p>
          </CardContent>
        </Card>
      </div>

      {/* Gráfico de ventas */}
      <Card>
        <CardHeader>
          <CardTitle>Tendencia de Ventas</CardTitle>
          <CardDescription>Ventas de los últimos 7 días</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="h-[300px] flex items-center justify-center bg-muted/20 rounded-lg">
            <div className="text-center">
              <TrendingUp className="h-12 w-12 mx-auto text-muted-foreground mb-2" />
              <p className="text-sm text-muted-foreground">Gráfico de tendencia de ventas</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Lista de ventas recientes */}
      <Card>
        <CardHeader>
          <CardTitle>Ventas Recientes</CardTitle>
          <CardDescription>Últimas transacciones realizadas</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b bg-muted/50">
                    <th className="h-12 px-4 text-left align-middle font-medium">ID Venta</th>
                    <th className="h-12 px-4 text-left align-middle font-medium">Cliente</th>
                    <th className="h-12 px-4 text-left align-middle font-medium">Fecha</th>
                    <th className="h-12 px-4 text-left align-middle font-medium">Total</th>
                    <th className="h-12 px-4 text-left align-middle font-medium">Estado</th>
                    <th className="h-12 px-4 text-left align-middle font-medium">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {ventasRecientes.map((venta) => (
                    <tr key={venta.id} className="border-b">
                      <td className="p-4 font-medium">{venta.id}</td>
                      <td className="p-4">{venta.cliente}</td>
                      <td className="p-4">{venta.fecha}</td>
                      <td className="p-4">${venta.total}</td>
                      <td className="p-4">{getEstadoBadge(venta.estado)}</td>
                      <td className="p-4">
                        <Button variant="outline" size="sm">
                          Ver Detalles
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
