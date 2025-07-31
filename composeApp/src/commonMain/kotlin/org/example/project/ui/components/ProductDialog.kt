package org.example.project.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
    var categoria by remember { mutableStateOf(initialProduct?.categoria ?: "") }

    var nombreError by remember { mutableStateOf("") }
    var precioError by remember { mutableStateOf("") }
    var cantidadError by remember { mutableStateOf("") }
    var categoriaError by remember { mutableStateOf("") }

    var categoriaExpanded by remember { mutableStateOf(false) }

    // Reemplazo la lista local por la recibida por parámetro
    var categorias by remember { mutableStateOf(categorias) }

    fun validarCampos(): Boolean {
        nombreError = if (nombre.isBlank()) "El nombre es requerido" else ""
        categoriaError = if (categoria.isBlank()) "La categoría es requerida" else ""

        precioError = try {
            val precioValue = precio.toDouble()
            if (precioValue <= 0) "El precio debe ser mayor a 0" else ""
        } catch (_: NumberFormatException) {
            "Precio inválido"
        }

        cantidadError = try {
            val cantidadValue = cantidad.toInt()
            if (cantidadValue < 0) "La cantidad no puede ser negativa" else ""
        } catch (_: NumberFormatException) {
            "Cantidad inválida"
        }

        return nombreError.isEmpty() && precioError.isEmpty() &&
               cantidadError.isEmpty() && categoriaError.isEmpty()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header del dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = onDismiss) {
                        Text("✕", style = MaterialTheme.typography.headlineSmall)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Campos del formulario
                TextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        nombreError = ""
                    },
                    label = { Text("Nombre del producto") },
                    placeholder = { Text("Ej: Extintor ABC 5kg") },
                    isError = nombreError.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (nombreError.isNotEmpty()) {
                    Text(
                        text = nombreError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de categorías
                ExposedDropdownMenuBox(
                    expanded = categoriaExpanded,
                    onExpandedChange = { categoriaExpanded = !categoriaExpanded }
                ) {
                    TextField(
                        readOnly = true,
                        value = categoria,
                        onValueChange = { },
                        label = { Text("Categoría") },
                        placeholder = { Text("Seleccione una categoría") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpanded)
                        },
                        isError = categoriaError.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = categoriaExpanded,
                        onDismissRequest = { categoriaExpanded = false }
                    ) {
                        categorias.forEach { categoriaItem ->
                            DropdownMenuItem(
                                text = { Text(text = categoriaItem) },
                                onClick = {
                                    categoria = categoriaItem
                                    categoriaError = ""
                                    categoriaExpanded = false
                                }
                            )
                        }
                    }
                }

                // Campo para nueva categoría
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = categoria,
                    onValueChange = {
                        categoria = it
                        categoriaError = ""
                    },
                    label = { Text("Nueva categoría (si no existe)") },
                    placeholder = { Text("Ej: Nueva categoría") },
                    isError = categoriaError.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (categoriaError.isNotEmpty()) {
                    Text(
                        text = categoriaError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = precio,
                        onValueChange = {
                            precio = it
                            precioError = ""
                        },
                        label = { Text("Precio") },
                        placeholder = { Text("0.00") },
                        isError = precioError.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    TextField(
                        value = cantidad,
                        onValueChange = {
                            cantidad = it.filter { c -> c.isDigit() }
                            cantidadError = ""
                        },
                        label = { Text("Cantidad") },
                        placeholder = { Text("0") },
                        isError = cantidadError.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                if (precioError.isNotEmpty()) {
                    Text(
                        text = precioError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (cantidadError.isNotEmpty()) {
                    Text(
                        text = cantidadError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Opcional") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (validarCampos()) {
                                // Si la categoría no existe, agregarla
                                if (categoria.isNotBlank() && !categorias.contains(categoria)) {
                                    categorias = categorias + categoria
                                }
                                val productoRequest = ProductoRequest(
                                    nombre = nombre,
                                    descripcion = if (descripcion.isBlank()) null else descripcion,
                                    precio = precio.toDoubleOrNull() ?: 0.0,
                                    cantidad = cantidad.toIntOrNull() ?: 0,
                                    categoria = categoria
                                )
                                onConfirm(productoRequest)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (initialProduct != null) "Actualizar" else "Agregar")
                    }
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
