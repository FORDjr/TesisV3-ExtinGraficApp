package org.example.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.ExtintorColors

// Botón principal con estilo extintor elegante
@Composable
fun ExtintorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Primary
) {
    val colors = when (variant) {
        ButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = ExtintorColors.ExtintorRed,
            contentColor = ExtintorColors.PureWhite,
            disabledContainerColor = ExtintorColors.Gray300,
            disabledContentColor = ExtintorColors.Gray500
        )
        ButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = ExtintorColors.Gray100,
            contentColor = ExtintorColors.CharcoalBlack,
            disabledContainerColor = ExtintorColors.Gray200,
            disabledContentColor = ExtintorColors.Gray400
        )
        ButtonVariant.Outline -> ButtonDefaults.outlinedButtonColors(
            contentColor = ExtintorColors.ExtintorRed,
            disabledContentColor = ExtintorColors.Gray400
        )
    }

    when (variant) {
        ButtonVariant.Outline -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                colors = colors,
                border = BorderStroke(1.5.dp, if (enabled) ExtintorColors.ExtintorRed else ExtintorColors.Gray300),
                shape = RoundedCornerShape(12.dp)
            ) {
                ButtonContent(text, icon)
            }
        }
        else -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                colors = colors,
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                ButtonContent(text, icon)
            }
        }
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

enum class ButtonVariant {
    Primary, Secondary, Outline
}

// Card elegante con estilo extintor
@Composable
fun ExtintorCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ExtintorColors.PureWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (elevated) 4.dp else 0.dp
        ),
        border = if (!elevated) BorderStroke(1.dp, ExtintorColors.Gray200) else null
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

// Input field elegante
@Composable
fun ExtintorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String = "",
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = ExtintorColors.Gray500
                    )
                }
            },
            trailingIcon = trailingIcon?.let {
                {
                    IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = ExtintorColors.Gray500
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ExtintorColors.ExtintorRed,
                focusedLabelColor = ExtintorColors.ExtintorRed,
                cursorColor = ExtintorColors.ExtintorRed,
                errorBorderColor = ExtintorColors.Error,
                errorLabelColor = ExtintorColors.Error
            ),
            shape = RoundedCornerShape(12.dp)
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

// Chip elegante con estilo extintor
@Composable
fun ExtintorChip(
    text: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val backgroundColor = if (selected) {
        ExtintorColors.ExtintorRed.copy(alpha = 0.1f)
    } else {
        ExtintorColors.Gray100
    }

    val contentColor = if (selected) {
        ExtintorColors.ExtintorRed
    } else {
        ExtintorColors.Gray600
    }

    val borderColor = if (selected) {
        ExtintorColors.ExtintorRed
    } else {
        ExtintorColors.Gray300
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Status badge elegante
@Composable
fun StatusBadge(
    text: String,
    status: StatusType,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        StatusType.Success -> ExtintorColors.Success.copy(alpha = 0.1f) to ExtintorColors.Success
        StatusType.Warning -> ExtintorColors.Warning.copy(alpha = 0.1f) to ExtintorColors.Warning
        StatusType.Error -> ExtintorColors.Error.copy(alpha = 0.1f) to ExtintorColors.Error
        StatusType.Info -> ExtintorColors.Info.copy(alpha = 0.1f) to ExtintorColors.Info
        StatusType.Neutral -> ExtintorColors.Gray200 to ExtintorColors.Gray600
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

enum class StatusType {
    Success, Warning, Error, Info, Neutral
}

// Gradient header para pantallas importantes
@Composable
fun ExtintorGradientHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        ExtintorColors.ExtintorRed,
                        ExtintorColors.ExtintorRedLight
                    )
                )
            )
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ExtintorColors.PureWhite,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ExtintorColors.PureWhite,
                    fontWeight = FontWeight.Bold
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ExtintorColors.PureWhite.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
