"use client"

import { useState, useEffect } from "react"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Plus, Minus, Search, ShoppingCart } from "lucide-react"
import { apiService } from "@/lib/api-service"

interface Producto {
  id: number
  nombre: string
  precio: number
  stock: number
  descripcion?: string
}

interface ProductoVenta {
  id: number
  nombre: string
  precio: number
  cantidad: number
  subtotal: number
}

interface NuevaVentaDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onVentaCreada: () => void
}

export function NuevaVentaDialog({ open, onOpenChange, onVentaCreada }: NuevaVentaDialogProps) {
  const [cliente, setCliente] = useState("")
  const [productos, setProductos] = useState<Producto[]>([])
  const [productosSeleccionados, setProductosSeleccionados] = useState<ProductoVenta[]>([])
  const [busquedaProducto, setBusquedaProducto] = useState("")
  const [observaciones, setObservaciones] = useState("")
  const [metodoPago, setMetodoPago] = useState("")
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (open) {
      cargarProductos()
    }
  }, [open])

  const cargarProductos = async () => {
    try {
      const response = await apiService.get('/productos')
      setProductos(response.data || [])
    } catch (error) {
      console.error('Error al cargar productos:', error)
    }
  }

  const agregarProducto = (producto: Producto) => {
    const existe = productosSeleccionados.find(p => p.id === producto.id)
    if (existe) {
      if (existe.cantidad < producto.stock) {
        setProductosSeleccionados(prev =>
          prev.map(p =>
            p.id === producto.id
              ? { ...p, cantidad: p.cantidad + 1, subtotal: (p.cantidad + 1) * p.precio }
              : p
          )
        )
      }
    } else {
      setProductosSeleccionados(prev => [
        ...prev,
        {
          id: producto.id,
          nombre: producto.nombre,
          precio: producto.precio,
          cantidad: 1,
          subtotal: producto.precio
        }
      ])
    }
  }

  const actualizarCantidad = (id: number, nuevaCantidad: number) => {
    if (nuevaCantidad === 0) {
      setProductosSeleccionados(prev => prev.filter(p => p.id !== id))
    } else {
      const producto = productos.find(p => p.id === id)
      if (producto && nuevaCantidad <= producto.stock) {
        setProductosSeleccionados(prev =>
          prev.map(p =>
            p.id === id
              ? { ...p, cantidad: nuevaCantidad, subtotal: nuevaCantidad * p.precio }
              : p
          )
        )
      }
    }
  }

  const calcularTotal = () => {
    return productosSeleccionados.reduce((total, producto) => total + producto.subtotal, 0)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!cliente.trim() || productosSeleccionados.length === 0 || !metodoPago) {
      alert("Por favor complete todos los campos obligatorios")
      return
    }

    setLoading(true)
    try {
      const nuevaVenta = {
        cliente,
        productos: productosSeleccionados.map(p => ({
          id: p.id,
          cantidad: p.cantidad,
          precio: p.precio
        })),
        total: calcularTotal(),
        metodoPago,
        observaciones,
        fecha: new Date().toISOString()
      }

      await apiService.post('/ventas', nuevaVenta)

      // Limpiar formulario
      setCliente("")
      setProductosSeleccionados([])
      setObservaciones("")
      setMetodoPago("")
      setBusquedaProducto("")

      onVentaCreada()
      onOpenChange(false)
    } catch (error) {
      console.error('Error al crear venta:', error)
      alert("Error al crear la venta")
    } finally {
      setLoading(false)
    }
  }

  const productosFiltrados = productos.filter(producto =>
    producto.nombre.toLowerCase().includes(busquedaProducto.toLowerCase()) ||
    producto.descripcion?.toLowerCase().includes(busquedaProducto.toLowerCase())
  )

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Nueva Venta</DialogTitle>
          <DialogDescription>
            Registra una nueva venta agregando productos y datos del cliente
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="cliente">Cliente *</Label>
              <Input
                id="cliente"
                value={cliente}
                onChange={(e) => setCliente(e.target.value)}
                placeholder="Nombre del cliente"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="metodoPago">Método de Pago *</Label>
              <Select value={metodoPago} onValueChange={setMetodoPago} required>
                <SelectTrigger>
                  <SelectValue placeholder="Seleccionar método" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="efectivo">Efectivo</SelectItem>
                  <SelectItem value="tarjeta">Tarjeta</SelectItem>
                  <SelectItem value="transferencia">Transferencia</SelectItem>
                  <SelectItem value="credito">Crédito</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Búsqueda y lista de productos */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Buscar Productos</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="relative">
                    <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                    <Input
                      placeholder="Buscar productos..."
                      value={busquedaProducto}
                      onChange={(e) => setBusquedaProducto(e.target.value)}
                      className="pl-8"
                    />
                  </div>

                  <div className="space-y-2 max-h-64 overflow-y-auto">
                    {productosFiltrados.map((producto) => (
                      <div
                        key={producto.id}
                        className="flex items-center justify-between p-3 border rounded-lg hover:bg-accent cursor-pointer"
                        onClick={() => agregarProducto(producto)}
                      >
                        <div className="flex-1">
                          <p className="font-medium">{producto.nombre}</p>
                          <div className="flex items-center gap-2 text-sm text-muted-foreground">
                            <span>${producto.precio}</span>
                            <Badge variant={producto.stock > 0 ? "secondary" : "destructive"}>
                              Stock: {producto.stock}
                            </Badge>
                          </div>
                        </div>
                        <Button
                          type="button"
                          size="sm"
                          disabled={producto.stock === 0}
                          onClick={(e) => {
                            e.stopPropagation()
                            agregarProducto(producto)
                          }}
                        >
                          <Plus className="h-4 w-4" />
                        </Button>
                      </div>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Carrito de compras */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                  <ShoppingCart className="h-5 w-5" />
                  Carrito ({productosSeleccionados.length})
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {productosSeleccionados.length === 0 ? (
                    <p className="text-sm text-muted-foreground text-center py-8">
                      No hay productos seleccionados
                    </p>
                  ) : (
                    <>
                      <div className="space-y-2 max-h-64 overflow-y-auto">
                        {productosSeleccionados.map((producto) => (
                          <div key={producto.id} className="flex items-center justify-between p-2 border rounded">
                            <div className="flex-1">
                              <p className="font-medium text-sm">{producto.nombre}</p>
                              <p className="text-xs text-muted-foreground">${producto.precio} c/u</p>
                            </div>
                            <div className="flex items-center gap-2">
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => actualizarCantidad(producto.id, producto.cantidad - 1)}
                              >
                                <Minus className="h-3 w-3" />
                              </Button>
                              <span className="w-8 text-center text-sm">{producto.cantidad}</span>
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => actualizarCantidad(producto.id, producto.cantidad + 1)}
                              >
                                <Plus className="h-3 w-3" />
                              </Button>
                            </div>
                            <div className="text-right min-w-[60px]">
                              <p className="font-medium text-sm">${producto.subtotal.toFixed(2)}</p>
                            </div>
                          </div>
                        ))}
                      </div>

                      <div className="border-t pt-3">
                        <div className="flex justify-between items-center">
                          <span className="font-medium">Total:</span>
                          <span className="text-lg font-bold">${calcularTotal().toFixed(2)}</span>
                        </div>
                      </div>
                    </>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>

          <div className="space-y-2">
            <Label htmlFor="observaciones">Observaciones</Label>
            <Textarea
              id="observaciones"
              value={observaciones}
              onChange={(e) => setObservaciones(e.target.value)}
              placeholder="Notas adicionales sobre la venta..."
              rows={3}
            />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={loading || productosSeleccionados.length === 0}>
              {loading ? "Procesando..." : "Crear Venta"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
