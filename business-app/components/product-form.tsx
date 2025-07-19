"use client"

import type React from "react"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent } from "@/components/ui/card"
import { Plus } from "lucide-react"

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

interface ProductFormProps {
  initialData?: Producto
  onSubmit: (data: Omit<Producto, "id">) => void
}

export function ProductForm({ initialData, onSubmit }: ProductFormProps) {
  const [formData, setFormData] = useState({
    nombre: initialData?.nombre || "",
    categoria: initialData?.categoria || "",
    stock: initialData?.stock || 0,
    stockMinimo: initialData?.stockMinimo || 0,
    precio: initialData?.precio || 0,
    costo: initialData?.costo || 0,
    proveedor: initialData?.proveedor || "",
    fechaIngreso: initialData?.fechaIngreso || new Date().toISOString().split("T")[0],
    estado: initialData?.estado || "Activo",
    descripcion: "",
  })

  const [errors, setErrors] = useState<Record<string, string>>({})
  const [showNewCategoryInput, setShowNewCategoryInput] = useState(false)
  const [newCategory, setNewCategory] = useState("")
  const [customCategories, setCustomCategories] = useState<string[]>([])

  const defaultCategorias = ["Extintores", "Test", "Electrónicos", "Otros"]
  const allCategories = [...defaultCategorias, ...customCategories]

  const validateForm = () => {
    const newErrors: Record<string, string> = {}

    if (!formData.nombre.trim()) {
      newErrors.nombre = "El nombre es requerido"
    }
    if (!formData.categoria) {
      newErrors.categoria = "La categoría es requerida"
    }
    if (formData.stock < 0) {
      newErrors.stock = "El stock no puede ser negativo"
    }
    if (formData.stockMinimo < 0) {
      newErrors.stockMinimo = "El stock mínimo no puede ser negativo"
    }
    if (formData.precio <= 0) {
      newErrors.precio = "El precio debe ser mayor a 0"
    }
    if (formData.costo <= 0) {
      newErrors.costo = "El costo debe ser mayor a 0"
    }
    if (formData.precio <= formData.costo) {
      newErrors.precio = "El precio debe ser mayor al costo"
    }
    if (!formData.proveedor.trim()) {
      newErrors.proveedor = "El proveedor es requerido"
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    if (validateForm()) {
      onSubmit({
        nombre: formData.nombre,
        categoria: formData.categoria,
        stock: formData.stock,
        stockMinimo: formData.stockMinimo,
        precio: formData.precio,
        costo: formData.costo,
        proveedor: formData.proveedor,
        fechaIngreso: formData.fechaIngreso,
        estado: formData.stock === 0 ? "Agotado" : "Activo",
      })
    }
  }

  const handleInputChange = (field: string, value: any) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: "" }))
    }
  }

  const handleCategoryChange = (value: string) => {
    if (value === "__nueva__") {
      setShowNewCategoryInput(true)
      setNewCategory("")
      handleInputChange("categoria", "")
    } else {
      handleInputChange("categoria", value)
      setShowNewCategoryInput(false)
    }
  }

  const handleAddCategory = () => {
    if (newCategory.trim() && !allCategories.includes(newCategory.trim())) {
      setCustomCategories((prev) => [...prev, newCategory.trim()])
      handleInputChange("categoria", newCategory.trim())
      setNewCategory("")
      setShowNewCategoryInput(false)
    }
  }

  const proveedores = ["Dell Inc.", "Logitech", "Corsair", "Samsung", "Apple", "HP", "Lenovo"]

  const margenGanancia =
    formData.precio > 0 && formData.costo > 0
      ? (((formData.precio - formData.costo) / formData.costo) * 100).toFixed(1)
      : "0"

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid gap-6 md:grid-cols-2">
        {/* Información básica */}
        <Card>
          <CardContent className="pt-6">
            <h3 className="text-lg font-medium mb-4">Información Básica</h3>
            <div className="space-y-4">
              <div>
                <Label htmlFor="nombre">Nombre del Producto *</Label>
                <Input
                  id="nombre"
                  value={formData.nombre}
                  onChange={(e) => handleInputChange("nombre", e.target.value)}
                  placeholder="Ej: Laptop Dell XPS 13"
                  className={errors.nombre ? "border-red-500" : ""}
                />
                {errors.nombre && <p className="text-sm text-red-500 mt-1">{errors.nombre}</p>}
              </div>

              <div>
                <Label htmlFor="categoria">Categoría *</Label>
                <Select value={formData.categoria} onValueChange={handleCategoryChange}>
                  <SelectTrigger className={errors.categoria ? "border-red-500" : ""}>
                    <SelectValue placeholder="Selecciona una categoría" />
                  </SelectTrigger>
                  <SelectContent>
                    {allCategories.map((categoria) => (
                      <SelectItem key={categoria} value={categoria}>
                        {categoria}
                      </SelectItem>
                    ))}
                    <SelectItem value="__nueva__">
                      <div className="flex items-center">
                        <Plus className="w-4 h-4 mr-2" />
                        Crear nueva categoría
                      </div>
                    </SelectItem>
                  </SelectContent>
                </Select>
                {errors.categoria && <p className="text-sm text-red-500 mt-1">{errors.categoria}</p>}

                {showNewCategoryInput && (
                  <div className="flex items-center gap-2 mt-2">
                    <Input
                      value={newCategory}
                      onChange={(e) => setNewCategory(e.target.value)}
                      placeholder="Escribe el nombre de la nueva categoría"
                      className="flex-1"
                      onKeyPress={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault()
                          handleAddCategory()
                        }
                      }}
                    />
                    <Button type="button" onClick={handleAddCategory} size="sm">
                      <Plus className="w-4 h-4 mr-1" />
                      Agregar
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setShowNewCategoryInput(false)
                        setNewCategory("")
                        handleInputChange("categoria", "")
                      }}
                    >
                      Cancelar
                    </Button>
                  </div>
                )}
              </div>

              <div>
                <Label htmlFor="proveedor">Proveedor *</Label>
                <Select value={formData.proveedor} onValueChange={(value) => handleInputChange("proveedor", value)}>
                  <SelectTrigger className={errors.proveedor ? "border-red-500" : ""}>
                    <SelectValue placeholder="Selecciona un proveedor" />
                  </SelectTrigger>
                  <SelectContent>
                    {proveedores.map((proveedor) => (
                      <SelectItem key={proveedor} value={proveedor}>
                        {proveedor}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.proveedor && <p className="text-sm text-red-500 mt-1">{errors.proveedor}</p>}
              </div>

              <div>
                <Label htmlFor="descripcion">Descripción</Label>
                <Textarea
                  id="descripcion"
                  value={formData.descripcion}
                  onChange={(e) => handleInputChange("descripcion", e.target.value)}
                  placeholder="Descripción opcional del producto"
                  rows={3}
                />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Inventario y precios */}
        <Card>
          <CardContent className="pt-6">
            <h3 className="text-lg font-medium mb-4">Inventario y Precios</h3>
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="stock">Stock Actual *</Label>
                  <Input
                    id="stock"
                    type="number"
                    min="0"
                    value={formData.stock}
                    onChange={(e) => handleInputChange("stock", Number.parseInt(e.target.value) || 0)}
                    className={errors.stock ? "border-red-500" : ""}
                  />
                  {errors.stock && <p className="text-sm text-red-500 mt-1">{errors.stock}</p>}
                </div>

                <div>
                  <Label htmlFor="stockMinimo">Stock Mínimo *</Label>
                  <Input
                    id="stockMinimo"
                    type="number"
                    min="0"
                    value={formData.stockMinimo}
                    onChange={(e) => handleInputChange("stockMinimo", Number.parseInt(e.target.value) || 0)}
                    className={errors.stockMinimo ? "border-red-500" : ""}
                  />
                  {errors.stockMinimo && <p className="text-sm text-red-500 mt-1">{errors.stockMinimo}</p>}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="costo">Costo Unitario *</Label>
                  <Input
                    id="costo"
                    type="number"
                    step="0.01"
                    min="0"
                    value={formData.costo}
                    onChange={(e) => handleInputChange("costo", Number.parseFloat(e.target.value) || 0)}
                    placeholder="0.00"
                    className={errors.costo ? "border-red-500" : ""}
                  />
                  {errors.costo && <p className="text-sm text-red-500 mt-1">{errors.costo}</p>}
                </div>

                <div>
                  <Label htmlFor="precio">Precio de Venta *</Label>
                  <Input
                    id="precio"
                    type="number"
                    step="0.01"
                    min="0"
                    value={formData.precio}
                    onChange={(e) => handleInputChange("precio", Number.parseFloat(e.target.value) || 0)}
                    placeholder="0.00"
                    className={errors.precio ? "border-red-500" : ""}
                  />
                  {errors.precio && <p className="text-sm text-red-500 mt-1">{errors.precio}</p>}
                </div>
              </div>

              {formData.precio > 0 && formData.costo > 0 && (
                <div className="p-3 bg-muted rounded-lg">
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium">Margen de Ganancia:</span>
                    <span
                      className={`text-sm font-bold ${Number.parseFloat(margenGanancia) > 0 ? "text-green-600" : "text-red-600"}`}
                    >
                      {margenGanancia}%
                    </span>
                  </div>
                  <div className="flex justify-between items-center mt-1">
                    <span className="text-sm text-muted-foreground">Ganancia por unidad:</span>
                    <span className="text-sm font-medium">${(formData.precio - formData.costo).toFixed(2)}</span>
                  </div>
                </div>
              )}

              <div>
                <Label htmlFor="fechaIngreso">Fecha de Ingreso</Label>
                <Input
                  id="fechaIngreso"
                  type="date"
                  value={formData.fechaIngreso}
                  onChange={(e) => handleInputChange("fechaIngreso", e.target.value)}
                />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="flex justify-end gap-4">
        <Button type="button" variant="outline">
          Cancelar
        </Button>
        <Button type="submit">{initialData ? "Actualizar Producto" : "Agregar Producto"}</Button>
      </div>
    </form>
  )
}
