"use client"

import { useState } from "react"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Plus, Search, Package, AlertTriangle, Edit, Trash2, DollarSign } from "lucide-react"
import { ProductForm } from "./product-form"
import { DeleteProductDialog } from "./delete-product-dialog"

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

export function InventarioContent() {
  const [productos, setProductos] = useState<Producto[]>([
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
      fechaIngreso: "2024-01-08",
      estado: "Activo",
    },
    {
      id: 3,
      nombre: "Teclado Mecánico",
      categoria: "Accesorios",
      stock: 0,
      stockMinimo: 5,
      precio: 149.99,
      costo: 89.99,
      proveedor: "Corsair",
      fechaIngreso: "2024-01-05",
      estado: "Agotado",
    },
    {
      id: 4,
      nombre: "Monitor 4K Samsung",
      categoria: "Electrónicos",
      stock: 8,
      stockMinimo: 3,
      precio: 399.99,
      costo: 299.99,
      proveedor: "Samsung",
      fechaIngreso: "2024-01-12",
      estado: "Activo",
    },
    {
      id: 5,
      nombre: "Webcam HD",
      categoria: "Accesorios",
      stock: 25,
      stockMinimo: 10,
      precio: 79.99,
      costo: 45.99,
      proveedor: "Logitech",
      fechaIngreso: "2024-01-15",
      estado: "Activo",
    },
  ])

  const [searchTerm, setSearchTerm] = useState("")
  const [selectedCategory, setSelectedCategory] = useState("all")
  const [selectedStatus, setSelectedStatus] = useState("all")
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState<Producto | null>(null)
  const [deletingProduct, setDeletingProduct] = useState<Producto | null>(null)

  const categorias = ["Electrónicos", "Accesorios", "Oficina", "Hogar"]

  const getEstadoBadge = (producto: Producto) => {
    if (producto.stock === 0) return <Badge variant="destructive">Agotado</Badge>
    if (producto.stock <= producto.stockMinimo) return <Badge variant="secondary">Stock Bajo</Badge>
    return <Badge variant="default">En Stock</Badge>
  }

  const filteredProductos = productos.filter((producto) => {
    const matchesSearch =
      producto.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
      producto.proveedor.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesCategory = selectedCategory === "all" || producto.categoria === selectedCategory
    const matchesStatus =
      selectedStatus === "all" ||
      (selectedStatus === "stock-bajo" && producto.stock <= producto.stockMinimo) ||
      (selectedStatus === "agotado" && producto.stock === 0) ||
      (selectedStatus === "en-stock" && producto.stock > producto.stockMinimo)

    return matchesSearch && matchesCategory && matchesStatus
  })

  const handleAddProduct = (productData: Omit<Producto, "id">) => {
    const newProduct = {
      ...productData,
      id: Math.max(...productos.map((p) => p.id)) + 1,
    }
    setProductos([...productos, newProduct])
    setIsAddDialogOpen(false)
  }

  const handleEditProduct = (productData: Omit<Producto, "id">) => {
    if (editingProduct) {
      setProductos(productos.map((p) => (p.id === editingProduct.id ? { ...productData, id: editingProduct.id } : p)))
      setEditingProduct(null)
    }
  }

  const handleDeleteProduct = (id: number) => {
    setProductos(productos.filter((p) => p.id !== id))
    setDeletingProduct(null)
  }

  const totalProductos = productos.length
  const valorTotal = productos.reduce((sum, p) => sum + p.stock * p.costo, 0)
  const productosAgotados = productos.filter((p) => p.stock === 0).length
  const productosStockBajo = productos.filter((p) => p.stock > 0 && p.stock <= p.stockMinimo).length

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4">
        <div>
          <h1 className="text-3xl font-bold">Inventario</h1>
          <p className="text-muted-foreground">Gestiona tus productos y stock</p>
        </div>

        {/* Botón agregar - siempre visible */}
        <div className="flex justify-end">
          <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
            <DialogTrigger asChild>
              <Button className="w-full sm:w-auto">
                <Plus className="mr-2 h-4 w-4" />
                Agregar Producto
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
              <DialogHeader>
                <DialogTitle>Agregar Nuevo Producto</DialogTitle>
                <DialogDescription>
                  Completa la información del producto para agregarlo al inventario.
                </DialogDescription>
              </DialogHeader>
              <ProductForm onSubmit={handleAddProduct} />
            </DialogContent>
          </Dialog>
        </div>
      </div>

      {/* Métricas - Grid simple */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-2">
              <Package className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">Total</p>
                <p className="text-xl font-bold">{totalProductos}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-2">
              <DollarSign className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-sm text-muted-foreground">Valor</p>
                <p className="text-xl font-bold">${Math.round(valorTotal / 1000)}k</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-yellow-500" />
              <div>
                <p className="text-sm text-muted-foreground">Stock Bajo</p>
                <p className="text-xl font-bold text-yellow-600">{productosStockBajo}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-destructive" />
              <div>
                <p className="text-sm text-muted-foreground">Agotados</p>
                <p className="text-xl font-bold text-destructive">{productosAgotados}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Búsqueda y filtros - Stack en móvil */}
      <Card>
        <CardContent className="p-4">
          <div className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Buscar productos..."
                className="pl-9"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                <SelectTrigger>
                  <SelectValue placeholder="Categoría" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todas</SelectItem>
                  {categorias.map((categoria) => (
                    <SelectItem key={categoria} value={categoria}>
                      {categoria}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select value={selectedStatus} onValueChange={setSelectedStatus}>
                <SelectTrigger>
                  <SelectValue placeholder="Estado" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todos</SelectItem>
                  <SelectItem value="en-stock">En Stock</SelectItem>
                  <SelectItem value="stock-bajo">Stock Bajo</SelectItem>
                  <SelectItem value="agotado">Agotado</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Lista de productos - Cards en móvil */}
      <div className="space-y-4">
        {filteredProductos.map((producto) => (
          <Card key={producto.id} className="hover:shadow-md transition-shadow">
            <CardContent className="p-4">
              <div className="flex justify-between items-start mb-3">
                <div className="flex-1">
                  <h3 className="font-semibold text-lg">{producto.nombre}</h3>
                  <p className="text-sm text-muted-foreground">
                    {producto.categoria} • {producto.proveedor}
                  </p>
                </div>
                {getEstadoBadge(producto)}
              </div>

              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <p className="text-sm text-muted-foreground">Stock</p>
                  <p className="font-medium">
                    {producto.stock}
                    {producto.stock <= producto.stockMinimo && producto.stock > 0 && (
                      <span className="text-yellow-600 text-xs ml-1">¡Bajo!</span>
                    )}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Precio</p>
                  <p className="font-medium">${producto.precio}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Stock Mín.</p>
                  <p className="font-medium">{producto.stockMinimo}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">ID</p>
                  <p className="font-medium">#{producto.id}</p>
                </div>
              </div>

              <div className="flex gap-2">
                <Dialog>
                  <DialogTrigger asChild>
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex-1 bg-transparent"
                      onClick={() => setEditingProduct(producto)}
                    >
                      <Edit className="h-3 w-3 mr-1" />
                      Editar
                    </Button>
                  </DialogTrigger>
                  <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
                    <DialogHeader>
                      <DialogTitle>Editar Producto</DialogTitle>
                      <DialogDescription>Modifica la información del producto.</DialogDescription>
                    </DialogHeader>
                    {editingProduct && <ProductForm initialData={editingProduct} onSubmit={handleEditProduct} />}
                  </DialogContent>
                </Dialog>

                <Button variant="outline" size="sm" onClick={() => setDeletingProduct(producto)} className="px-3">
                  <Trash2 className="h-3 w-3" />
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {filteredProductos.length === 0 && (
        <Card>
          <CardContent className="p-8 text-center">
            <Package className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
            <h3 className="text-lg font-medium mb-2">No se encontraron productos</h3>
            <p className="text-muted-foreground mb-4">Intenta ajustar los filtros o agregar un nuevo producto.</p>
            <Button onClick={() => setIsAddDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Agregar Primer Producto
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Dialog para eliminar producto */}
      {deletingProduct && (
        <DeleteProductDialog
          product={deletingProduct}
          onConfirm={() => handleDeleteProduct(deletingProduct.id)}
          onCancel={() => setDeletingProduct(null)}
        />
      )}
    </div>
  )
}
