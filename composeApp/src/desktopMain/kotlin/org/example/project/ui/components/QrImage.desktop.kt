package org.example.project.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

@Composable
actual fun QrImage(
    data: String,
    modifier: Modifier,
    contentDescription: String?
) {
    val matrix = remember(data) { generateBitMatrix(data) }
    Canvas(modifier = modifier) {
        val cellSize = minOf(size.width / matrix.width, size.height / matrix.height)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x * cellSize, y * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

private fun generateBitMatrix(content: String, size: Int = 512): BitMatrix =
    MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
