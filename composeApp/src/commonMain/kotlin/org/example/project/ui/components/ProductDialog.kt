package org.example.project.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToLong
import org.example.project.data.model.EstadoProductoRemote
import org.example.project.data.model.ProductoEstado
import org.example.project.data.model.ProductoRequest
import org.example.project.data.model.ProductoUI
import org.example.project.ui.theme.ExtintorColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    title: String,
    initialProduct: ProductoUI? = null,
    onDismiss: () -> Unit,
    onConfirm: (ProductoRequest) -> Unit,
    categorias: List<String>, // Recibo las categorías como parámetro
) {
    var nombre by remember { mutableStateOf(initialProduct?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(initialProduct?.descripcion ?: "") }
    var precio by remember { mutableStateOf(initialProduct?.precio?.toString() ?: "") }
    var cantidad by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "") }
    var stockMinimo by remember { mutableStateOf(initialProduct?.stockMinimo?.toString() ?: "0") } // NUEVO
    var categoriaSeleccionada by remember { mutableStateOf(initialProduct?.categoria ?: "") }
    var nuevaCategoria by remember { mutableStateOf("") }
    var estadoSeleccionado by remember { mutableStateOf(initialProduct?.estado ?: ProductoEstado.ACTIVO) }

    var nombreError by remember { mutableStateOf("") }
    var precioError by remember { mutableStateOf("") }
    var cantidadError by remember { mutableStateOf("") }
    var stockMinError by remember { mutableStateOf("") } // NUEVO
    var categoriaError by remember { mutableStateOf("") }

    var categoriaExpanded by remember { mutableStateOf(false) }

    // Reemplazo la lista local por la recibida por parámetro
    var categoriasDisponibles by remember { mutableStateOf(categorias) }

    fun categoriaFinal(): String = nuevaCategoria.ifBlank { categoriaSeleccionada }

    fun validarCampos(): Boolean {
        nombreError = if (nombre.isBlank()) "El nombre es requerido" else ""
        categoriaError = if (categoriaFinal().isBlank()) "La categoría es requerida" else ""

        precioError = try {
            val precioValue = precio.toDouble()
            if (precioValue <= 0) "El precio debe ser mayor a 0" else ""
        } catch (_: NumberFormatException) { "Precio inválido" }

        cantidadError = try {
            val cantidadValue = cantidad.toInt()
            if (cantidadValue < 0) "La cantidad no puede ser negativa" else ""
        } catch (_: NumberFormatException) { "Cantidad inválida" }

        stockMinError = try {
            val minVal = stockMinimo.toInt()
            if (minVal < 0) "No negativo" else ""
        } catch (_: NumberFormatException) { "Inválido" }

        return listOf(nombreError, precioError, cantidadError, categoriaError, stockMinError).all { it.isEmpty() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            color = ExtintorColors.SoftLavender
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (initialProduct == null) "Completa los datos para agregar un producto" else "Actualiza los campos necesarios y guarda los cambios",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                ExtintorTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        nombreError = ""
                    },
                    label = "Nombre del producto",
                    placeholder = "Ej: Extintor ABC 5kg",
                    isError = nombreError.isNotEmpty(),
                    errorMessage = nombreError,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector de categorías existente
                ExposedDropdownMenuBox(
                    expanded = categoriaExpanded,
                    onExpandedChange = { categoriaExpanded = !categoriaExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = categoriaSeleccionada,
                        onValueChange = { },
                        label = { Text("Categoría") },
                        placeholder = { Text("Seleccione una categoría") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpanded)
                        },
                        isError = categoriaError.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ExtintorColors.ExtintorRed,
                            focusedLabelColor = ExtintorColors.ExtintorRed,
                            cursorColor = ExtintorColors.ExtintorRed,
                            errorBorderColor = ExtintorColors.Error,
                            errorLabelColor = ExtintorColors.Error,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = categoriaExpanded,
                        onDismissRequest = { categoriaExpanded = false }
                    ) {
                        categoriasDisponibles.forEach { categoriaItem ->
                            DropdownMenuItem(
                                text = { Text(text = categoriaItem) },
                                onClick = {
                                    categoriaSeleccionada = categoriaItem
                                    categoriaError = ""
                                    categoriaExpanded = false
                                }
                            )
                        }
                    }
                }

                ExtintorTextField(
                    value = nuevaCategoria,
                    onValueChange = {
                        nuevaCategoria = it
                        categoriaError = ""
                    },
                    label = "Nueva categoría (opcional)",
                    placeholder = "Escribe una nueva si no está en la lista",
                    isError = categoriaError.isNotEmpty(),
                    errorMessage = categoriaError,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Estado",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExtintorChip(
                            text = "Activo",
                            selected = estadoSeleccionado != ProductoEstado.INACTIVO,
                            onClick = { estadoSeleccionado = ProductoEstado.ACTIVO }
                        )
                        ExtintorChip(
                            text = "Inactivo",
                            selected = estadoSeleccionado == ProductoEstado.INACTIVO,
                            onClick = { estadoSeleccionado = ProductoEstado.INACTIVO }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExtintorTextField(
                        value = precio,
                        onValueChange = {
                            precio = it
                            precioError = ""
                        },
                        label = "CLP",
                        placeholder = "0.00",
                        isError = precioError.isNotEmpty(),
                        errorMessage = precioError,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    ExtintorTextField(
                        value = cantidad,
                        onValueChange = {
                            cantidad = it.filter { c -> c.isDigit() }
                            cantidadError = ""
                        },
                        label = "Stock",
                        placeholder = "0",
                        isError = cantidadError.isNotEmpty(),
                        errorMessage = cantidadError,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    ExtintorTextField(
                        value = stockMinimo,
                        onValueChange = {
                            stockMinimo = it.filter { c -> c.isDigit() }
                            stockMinError = ""
                        },
                        label = "Min.",
                        placeholder = "0",
                        isError = stockMinError.isNotEmpty(),
                        errorMessage = stockMinError,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                ExtintorTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = "Descripción",
                    placeholder = "Opcional",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExtintorButton(
                        text = "Cancelar",
                        onClick = onDismiss,
                        variant = ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )

                    ExtintorButton(
                        text = "Guardar",
                        onClick = {
                            if (validarCampos()) {
                                val categoriaElegida = categoriaFinal()
                                if (categoriaElegida.isNotBlank() && !categoriasDisponibles.contains(categoriaElegida)) {
                                    categoriasDisponibles = categoriasDisponibles + categoriaElegida
                                }
                                val precioEntero = (precio.toDoubleOrNull() ?: 0.0).roundToLong()
                                val productoRequest = ProductoRequest(
                                    nombre = nombre,
                                    codigo = initialProduct?.codigo?.ifBlank { null },
                                    descripcion = if (descripcion.isBlank()) null else descripcion,
                                    precio = precioEntero,
                                    precioCompra = precioEntero,
                                    cantidad = cantidad.toIntOrNull() ?: 0,
                                    categoria = categoriaElegida,
                                    estado = if (estadoSeleccionado == ProductoEstado.INACTIVO) {
                                        EstadoProductoRemote.INACTIVO
                                    } else {
                                        EstadoProductoRemote.ACTIVO
                                    },
                                    proveedorId = initialProduct?.proveedorId,
                                    stockMinimo = stockMinimo.toIntOrNull() ?: 0
                                )
                                onConfirm(productoRequest)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun EditChoiceDialog(
    onDismiss: () -> Unit,
    onFullEdit: () -> Unit,
    onRestock: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            color = ExtintorColors.SoftLavender
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Editar producto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Desea hacer ReStock o Editar por completo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExtintorButton(
                        text = "Cancelar",
                        onClick = onDismiss,
                        variant = ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    ExtintorButton(
                        text = "ReStock",
                        onClick = onRestock,
                        variant = ButtonVariant.Outline,
                        modifier = Modifier.weight(1f)
                    )
                    ExtintorButton(
                        text = "Editar completo",
                        onClick = onFullEdit,
                        variant = ButtonVariant.Primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun RestockDialog(
    producto: ProductoUI,
    onDismiss: () -> Unit,
    onConfirm: (delta: Int) -> Unit
) {
    var nuevoStockText by remember { mutableStateOf(producto.stock.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val nuevoStock = nuevoStockText.toIntOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            color = ExtintorColors.SoftLavender
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ReStock",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = producto.nombre,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Stock actual: ${producto.stock}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val current = nuevoStock ?: producto.stock
                                val value = (current - 1).coerceAtLeast(0)
                                nuevoStockText = value.toString()
                                error = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Restar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    ExtintorTextField(
                        value = nuevoStockText,
                        onValueChange = {
                            nuevoStockText = it.filter { c -> c.isDigit() }
                            error = null
                        },
                        label = "Nuevo stock",
                        placeholder = "Cantidad final",
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val current = nuevoStock ?: producto.stock
                                    val value = (current + 1).coerceAtLeast(0)
                                    nuevoStockText = value.toString()
                                    error = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                contentDescription = "Sumar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text(
                    text = "Cantidad final: ${nuevoStock ?: producto.stock}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExtintorButton(
                        text = "Cancelar",
                        onClick = onDismiss,
                        variant = ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    ExtintorButton(
                        text = "Guardar",
                        onClick = {
                            val destino = nuevoStock
                            if (destino == null) {
                                error = "Cantidad inválida"
                                return@ExtintorButton
                            }
                            if (destino < 0) {
                                error = "El stock no puede ser negativo"
                                return@ExtintorButton
                            }
                            onConfirm(destino - producto.stock)
                        },
                        variant = ButtonVariant.Primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Extensión para KeyboardOptions en ExtintorTextField
@Composable
fun ExtintorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingText: String? = null,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = leadingText?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ExtintorColors.Gray500,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            },
            trailingIcon = trailingText?.let {
                {
                    TextButton(onClick = { onTrailingClick?.invoke() }) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ExtintorColors.Gray500
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            isError = isError,
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ExtintorColors.ExtintorRed,
                focusedLabelColor = ExtintorColors.ExtintorRed,
                cursorColor = ExtintorColors.ExtintorRed,
                errorBorderColor = ExtintorColors.Error,
                errorLabelColor = ExtintorColors.Error,
                // ESTO ES LO IMPORTANTE: Color del texto que escribes
                focusedTextColor = androidx.compose.ui.graphics.Color.Black,  // Negro cuando escribes y está enfocado
                unfocusedTextColor = androidx.compose.ui.graphics.Color.Black  // Negro cuando no está enfocado
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        )

        if (isError && errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = ExtintorColors.Error,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
