package org.example.project.services

import kotlin.math.roundToLong
import org.example.project.models.Venta
import org.example.project.models.VentaProducto
import org.example.project.models.VentaProductos
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

// Helpers to compute net totals considering devoluciones
internal fun Venta.totalDevueltoDesdeDb(): Long =
    VentaProducto.find { VentaProductos.ventaId eq id }.sumOf { vp ->
        if (vp.devuelto <= 0 || vp.cantidad == 0) 0L
        else ((vp.subtotal.toDouble() / vp.cantidad.toDouble()) * vp.devuelto).roundToLong()
    }

internal fun Venta.totalNeto(): Long = (total - totalDevueltoDesdeDb()).coerceAtLeast(0)

internal fun Venta.tieneSaldo(): Boolean = totalNeto() > 0
