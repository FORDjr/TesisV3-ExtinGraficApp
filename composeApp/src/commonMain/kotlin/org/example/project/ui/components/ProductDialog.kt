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

@Composable
fun ProductDialog(
    title: String,
    initialProduct: ProductoUI? = null,
    onDismiss: () -> Unit,
    onConfirm: (ProductoRequest) -> Unit
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
        ExtintorCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevated = true
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
                        fontWeight = FontWeight.Bold,
                        color = ExtintorColors.CharcoalBlack
                    )

                    TextButton(onClick = onDismiss) {
                        Text("✕", style = MaterialTheme.typography.headlineSmall)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Campos del formulario
                ExtintorTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        nombreError = ""
                    },
                    label = "Nombre del producto",
                    placeholder = "Ej: Extintor ABC 5kg",
                    leadingText = "🛒",
                    isError = nombreError.isNotEmpty(),
                    errorMessage = nombreError
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExtintorTextField(
                    value = categoria,
                    onValueChange = {
                        categoria = it
                        categoriaError = ""
                    },
                    label = "Categoría",
                    placeholder = "Ej: Extintores, Señalización",
                    leadingText = "🏷️",
                    isError = categoriaError.isNotEmpty(),
                    errorMessage = categoriaError
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                        label = "Precio",
                        placeholder = "0.00",
                        leadingText = "$",
                        isError = precioError.isNotEmpty(),
                        errorMessage = precioError,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    ExtintorTextField(
                        value = cantidad,
                        onValueChange = {
                            cantidad = it
                            cantidadError = ""
                        },
                        label = "Cantidad",
                        placeholder = "0",
                        leadingText = "#",
                        isError = cantidadError.isNotEmpty(),
                        errorMessage = cantidadError,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ExtintorTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = "Descripción (opcional)",
                    placeholder = "Descripción del producto...",
                    leadingText = "📝",
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExtintorButton(
                        text = "Cancelar",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Outline
                    )

                    ExtintorButton(
                        text = if (initialProduct != null) "Actualizar" else "Agregar",
                        onClick = {
                            if (validarCampos()) {
                                val producto = ProductoRequest(
                                    nombre = nombre.trim(),
                                    descripcion = descripcion.trim().takeIf { it.isNotEmpty() },
                                    precio = precio.toDouble(),
                                    cantidad = cantidad.toInt(),
                                    categoria = categoria.trim()
                                )
                                onConfirm(producto)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Primary
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
                errorLabelColor = ExtintorColors.Error
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
