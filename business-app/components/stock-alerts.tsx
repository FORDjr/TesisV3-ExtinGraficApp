import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { AlertTriangle, Package, ShoppingCart } from "lucide-react"

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

interface StockAlertsProps {
  productos: Producto[]
}

export function StockAlerts({ productos }: StockAlertsProps) {
  const productosStockBajo = productos.filter((p) => p.stock > 0 && p.stock <= p.stockMinimo)
  const productosAgotados = productos.filter((p) => p.stock === 0)

  if (productosStockBajo.length === 0 && productosAgotados.length === 0) {
    return null
  }

  return (
    <div className="space-y-4">
      {productosAgotados.length > 0 && (
        <Card className="border-red-200">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-red-700">
              <AlertTriangle className="h-5 w-5" />
              Productos Agotados ({productosAgotados.length})
            </CardTitle>
            <CardDescription>Estos productos necesitan reposición urgente</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {productosAgotados.slice(0, 5).map((producto) => (
                <div key={producto.id} className="flex items-center justify-between p-3 bg-red-50 rounded-lg">
                  <div className="flex items-center gap-3">
                    <Package className="h-4 w-4 text-red-500" />
                    <div>
                      <p className="font-medium">{producto.nombre}</p>
                      <p className="text-sm text-muted-foreground">{producto.categoria}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant="destructive">Agotado</Badge>
                    <Button size="sm" variant="outline">
                      <ShoppingCart className="h-3 w-3 mr-1" />
                      Reponer
                    </Button>
                  </div>
                </div>
              ))}
              {productosAgotados.length > 5 && (
                <p className="text-sm text-muted-foreground text-center">
                  Y {productosAgotados.length - 5} productos más...
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {productosStockBajo.length > 0 && (
        <Card className="border-yellow-200">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-yellow-700">
              <AlertTriangle className="h-5 w-5" />
              Stock Bajo ({productosStockBajo.length})
            </CardTitle>
            <CardDescription>Estos productos están por debajo del stock mínimo</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {productosStockBajo.slice(0, 5).map((producto) => (
                <div key={producto.id} className="flex items-center justify-between p-3 bg-yellow-50 rounded-lg">
                  <div className="flex items-center gap-3">
                    <Package className="h-4 w-4 text-yellow-500" />
                    <div>
                      <p className="font-medium">{producto.nombre}</p>
                      <p className="text-sm text-muted-foreground">
                        {producto.categoria} • Stock: {producto.stock}/{producto.stockMinimo}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant="secondary">Stock Bajo</Badge>
                    <Button size="sm" variant="outline">
                      <ShoppingCart className="h-3 w-3 mr-1" />
                      Reponer
                    </Button>
                  </div>
                </div>
              ))}
              {productosStockBajo.length > 5 && (
                <p className="text-sm text-muted-foreground text-center">
                  Y {productosStockBajo.length - 5} productos más...
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
