package org.example.project.data.api

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.example.project.data.models.MercadoLibreOrder
import org.example.project.data.models.MercadoLibreOrderItem

/**
 * Servicio simplificado que simula el consumo de la API de Mercado Libre.
 * En producción aquí debería integrarse OAuth y las llamadas reales a los endpoints de órdenes.
 */
class MercadoLibreApiService {

    suspend fun obtenerOrdenesRecientes(): Result<List<MercadoLibreOrder>> = runCatching {
        // Por ahora devolvemos datos demo pero con fechas dinámicas para facilitar pruebas.
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val yesterday = today.minus(DatePeriod(days = 1))

        listOf(
            MercadoLibreOrder(
                id = "MLA-${today.toString().replace("-", "")}-01",
                date = today.toString(),
                buyerName = "Juan Pérez",
                totalAmount = 120000.0,
                items = listOf(
                    MercadoLibreOrderItem(
                        mlItemId = "MLM123456",
                        title = "Extintor PQS 6Kg",
                        quantity = 1,
                        unitPrice = 120000.0
                    )
                )
            ),
            MercadoLibreOrder(
                id = "MLA-${yesterday.toString().replace("-", "")}-02",
                date = yesterday.toString(),
                buyerName = "Comercial Seguridad Ltda.",
                totalAmount = 210000.0,
                items = listOf(
                    MercadoLibreOrderItem(
                        mlItemId = "MLM654321",
                        title = "Set 3 Señaléticas Emergencia",
                        quantity = 3,
                        unitPrice = 70000.0
                    )
                )
            )
        )
    }
}
