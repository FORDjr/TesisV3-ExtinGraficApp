package org.example.project.utils

import kotlin.math.abs
import kotlin.math.roundToLong

object Formatters {
    fun formatPesos(value: Long): String {
        val body = formatNumber(abs(value))
        return if (value < 0) "-$$body" else "$$body"
    }

    fun formatPesos(value: Int): String = formatPesos(value.toLong())

    fun formatPesos(value: Double): String = formatPesos(value.roundToLong())

    private fun formatNumber(absValue: Long): String {
        val digits = absValue.toString()
        val sb = StringBuilder()
        digits.forEachIndexed { index, ch ->
            sb.append(ch)
            val remaining = digits.length - index - 1
            if (remaining > 0 && remaining % 3 == 0) {
                sb.append('.')
            }
        }
        return sb.toString()
    }
}
