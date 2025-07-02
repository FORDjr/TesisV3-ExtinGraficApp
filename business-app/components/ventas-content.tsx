import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Plus, TrendingUp, DollarSign, ShoppingCart } from "lucide-react"

export function VentasContent() {
  const ventasRecientes = [
    { id: "V001", cliente: "María García", fecha: "2024-01-15", total: 299.99, estado: "Completada" },
    { id: "V002", cliente: "Carlos López", fecha: "2024-01-15", total: 149.5, estado: "Pendiente" },
    { id: "V003", cliente: "Ana Martínez", fecha: "2024-01-14", total: 89.99, estado: "Completada" },
    { id: "V004", cliente: "Luis Rodríguez", fecha: "2024-01-14", total: 199.99, estado: "Cancelada" },
    { id: "V005", cliente: "Elena Fernández", fecha: "2024-01-13", total: 349.99, estado: "Completada" },
  ]

  const getEstadoBadge = (estado: string) => {
    switch (estado) {
      case "Completada":
        return <Badge variant="default">Completada</Badge>
      case "Pendiente":
        return <Badge variant="secondary">Pendiente</Badge>
      case "Cancelada":
        return <Badge variant="destructive">Cancelada</Badge>
      default:
        return <Badge variant="outline">{estado}</Badge>
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold">Ventas</h1>
          <p className="text-muted-foreground">Gestiona tus ventas y pedidos</p>
        </div>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          Nueva Venta
        </Button>
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
