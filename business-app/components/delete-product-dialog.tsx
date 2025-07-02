"use client"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"

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

interface DeleteProductDialogProps {
  product: Producto
  onConfirm: () => void
  onCancel: () => void
}

export function DeleteProductDialog({ product, onConfirm, onCancel }: DeleteProductDialogProps) {
  return (
    <AlertDialog open={true} onOpenChange={onCancel}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Estás seguro?</AlertDialogTitle>
          <AlertDialogDescription>
            Esta acción no se puede deshacer. Se eliminará permanentemente el producto{" "}
            <strong>"{product.nombre}"</strong> del inventario.
            {product.stock > 0 && (
              <div className="mt-2 p-2 bg-yellow-50 border border-yellow-200 rounded">
                <strong>Advertencia:</strong> Este producto tiene {product.stock} unidades en stock.
              </div>
            )}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel onClick={onCancel}>Cancelar</AlertDialogCancel>
          <AlertDialogAction onClick={onConfirm} className="bg-red-600 hover:bg-red-700">
            Eliminar Producto
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
