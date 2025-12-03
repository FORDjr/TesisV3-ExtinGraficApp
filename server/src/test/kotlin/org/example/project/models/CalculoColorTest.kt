package org.example.project.models

import kotlinx.datetime.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalculoColorTest {
    @Test
    fun colorGrisCuandoFechaEsNull() {
        val (color, dias) = calcularColor(null)
        assertEquals("gris", color)
        assertNull(dias)
    }

    @Test
    fun colorRojoCuandoDiasHasta30() {
        val fecha = Clock.System.now()
            .plus(10, DateTimeUnit.DAY, TimeZone.UTC)
            .toLocalDateTime(TimeZone.UTC)
        val (color, dias) = calcularColor(fecha)
        assertEquals("rojo", color)
        // dias podría ser 9 o 10 según truncamiento, verificar rango
        require(dias != null)
        assert(dias in 0..30) { "dias esperado <=30 actual=$dias" }
    }

    @Test
    fun colorAmarilloCuandoEntre31y60() {
        val fecha = Clock.System.now()
            .plus(45, DateTimeUnit.DAY, TimeZone.UTC)
            .toLocalDateTime(TimeZone.UTC)
        val (color, dias) = calcularColor(fecha)
        assertEquals("amarillo", color)
        require(dias != null)
        assert(dias in 31..60) { "dias esperado en 31..60 actual=$dias" }
    }

    @Test
    fun colorVerdeCuandoMayor60() {
        val fecha = Clock.System.now()
            .plus(80, DateTimeUnit.DAY, TimeZone.UTC)
            .toLocalDateTime(TimeZone.UTC)
        val (color, dias) = calcularColor(fecha)
        assertEquals("verde", color)
        require(dias != null)
        assert(dias > 60) { "dias esperado >60 actual=$dias" }
    }
}

