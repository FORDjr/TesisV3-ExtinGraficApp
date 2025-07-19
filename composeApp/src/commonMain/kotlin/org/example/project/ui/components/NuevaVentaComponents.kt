package org.example.project.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.models.*
import org.example.project.data.model.Producto
import org.example.project.data.model.ProductoUI

@Composable
fun MetodoPagoSelector(
    metodoPagoSeleccionado: MetodoPago?,
    onMetodoPagoChange: (MetodoPago) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetodoPago.values().forEach { metodo ->
            val isSelected = metodoPagoSeleccionado == metodo
            val (icono, texto, color) = when (metodo) {
                MetodoPago.EFECTIVO -> Triple(Icons.Default.AttachMoney, "Efectivo", Color(0xFF4CAF50))
                MetodoPago.TARJETA -> Triple(Icons.Default.CreditCard, "Tarjeta", Color(0xFF2196F3))
                MetodoPago.TRANSFERENCIA -> Triple(Icons.Default.AccountBalance, "Transferencia", Color(0xFF9C27B0))
                MetodoPago.CREDITO -> Triple(Icons.Default.Schedule, "Crédito", Color(0xFFFF9800))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMetodoPagoChange(metodo) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icono,
                        contentDescription = texto,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = texto,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ProductoDisponibleCard(
    producto: ProductoUI,
    onAgregarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = producto.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                producto.descripcion?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val precioFormateado = "$${String.format("%.2f", producto.precio)}"
                    Text(
                        text = precioFormateado,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (producto.stock > 0)
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else
                            Color(0xFFF44336).copy(alpha = 0.1f)
                    ) {
                        val stockTexto = "Stock: ${producto.stock}"
                        Text(
                            text = stockTexto,
                            fontSize = 10.sp,
                            color = if (producto.stock > 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onAgregarClick,
                enabled = producto.stock > 0,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Agregar",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ProductoCarritoCard(
    producto: ProductoCarrito,
    onCantidadChange: (Int) -> Unit,
    onRemoverClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = producto.nombre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$${String.format("%.2f", producto.precio)} c/u",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (producto.cantidad > 1) {
                            onCantidadChange(producto.cantidad - 1)
                        }
                    },
                    enabled = producto.cantidad > 1
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Reducir cantidad")
                }

                Text(
                    text = "${producto.cantidad}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = { onCantidadChange(producto.cantidad + 1) },
                    enabled = producto.cantidad < producto.stock
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar cantidad")
                }

                Text(
                    text = "$${String.format("%.2f", producto.subtotal)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(onClick = onRemoverClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remover producto",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}
