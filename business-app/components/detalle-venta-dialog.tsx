                  <span>$0.00</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span>Descuento:</span>
                  <span>$0.00</span>
                </div>
                <Separator />
                <div className="flex justify-between text-lg font-bold">
                  <span>Total:</span>
                  <span>${venta.total.toFixed(2)}</span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Acciones */}
          <div className="flex justify-between">
            <div className="flex gap-2">
              <Button variant="outline" size="sm">
                Imprimir Recibo
              </Button>
              <Button variant="outline" size="sm">
                Enviar por Email
              </Button>
            </div>
            <div className="flex gap-2">
              {venta.estado === "Pendiente" && (
                <>
                  <Button variant="outline" size="sm">
                    Cancelar Venta
                  </Button>
                  <Button size="sm">
                    Marcar Completada
                  </Button>
                </>
              )}
              {venta.estado === "Completada" && (
                <Button variant="outline" size="sm">
                  Generar Devolución
                </Button>
              )}
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
"use client"

import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { Calendar, User, CreditCard, FileText, Package } from "lucide-react"

interface ProductoVenta {
  id: number
  nombre: string
  cantidad: number
  precio: number
  subtotal: number
}

interface Venta {
  id: string
  cliente: string
  fecha: string
  total: number
  estado: string
  metodoPago: string
  observaciones?: string
  productos: ProductoVenta[]
}

interface DetalleVentaDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  venta: Venta | null
}

export function DetalleVentaDialog({ open, onOpenChange, venta }: DetalleVentaDialogProps) {
  if (!venta) return null

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

  const getMetodoPagoIcon = (metodo: string) => {
    return <CreditCard className="h-4 w-4" />
  }

  const formatearFecha = (fecha: string) => {
    return new Date(fecha).toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            Detalles de Venta
            <Badge variant="outline">{venta.id}</Badge>
          </DialogTitle>
          <DialogDescription>
            Información completa de la transacción
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Información general */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Información General</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex items-center gap-3">
                  <User className="h-5 w-5 text-muted-foreground" />
                  <div>
                    <p className="text-sm font-medium">Cliente</p>
                    <p className="text-sm text-muted-foreground">{venta.cliente}</p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <Calendar className="h-5 w-5 text-muted-foreground" />
                  <div>
                    <p className="text-sm font-medium">Fecha</p>
                    <p className="text-sm text-muted-foreground">{formatearFecha(venta.fecha)}</p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  {getMetodoPagoIcon(venta.metodoPago)}
                  <div>
                    <p className="text-sm font-medium">Método de Pago</p>
                    <p className="text-sm text-muted-foreground capitalize">{venta.metodoPago}</p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <div className="h-5 w-5 flex items-center justify-center">
                    {getEstadoBadge(venta.estado)}
                  </div>
                  <div>
                    <p className="text-sm font-medium">Estado</p>
                  </div>
                </div>
              </div>

              {venta.observaciones && (
                <div className="pt-4 border-t">
                  <div className="flex items-start gap-3">
                    <FileText className="h-5 w-5 text-muted-foreground mt-0.5" />
                    <div className="flex-1">
                      <p className="text-sm font-medium">Observaciones</p>
                      <p className="text-sm text-muted-foreground">{venta.observaciones}</p>
                    </div>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Productos */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg flex items-center gap-2">
                <Package className="h-5 w-5" />
                Productos ({venta.productos.length})
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {venta.productos.map((producto, index) => (
                  <div key={index} className="flex items-center justify-between p-3 border rounded-lg">
                    <div className="flex-1">
                      <p className="font-medium">{producto.nombre}</p>
                      <p className="text-sm text-muted-foreground">
                        ${producto.precio.toFixed(2)} × {producto.cantidad}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="font-medium">${producto.subtotal.toFixed(2)}</p>
                    </div>
                  </div>
                ))}
              </div>

              <Separator className="my-4" />

              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span>Subtotal:</span>
                  <span>${venta.total.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span>Impuestos:</span>
