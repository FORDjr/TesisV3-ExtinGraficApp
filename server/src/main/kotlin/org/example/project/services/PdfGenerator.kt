package org.example.project.services

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfWriter
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import org.example.project.models.Cliente
import org.example.project.models.Extintor
import org.example.project.models.OrdenServicio
import org.example.project.models.Sede
import org.example.project.models.Usuario
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset

object PdfGenerator {
    private const val BASE_DIR = "generated-certificates"

    fun ensureDir() {
        val dir = File(BASE_DIR)
        if (!dir.exists()) dir.mkdirs()
    }

    private fun cell(text: String, font: Font, bold: Boolean = false, align: Int = Element.ALIGN_LEFT): PdfPCell {
        val f = if (bold) Font(font.family, font.size, Font.BOLD) else font
        return PdfPCell(Phrase(text, f)).apply {
            horizontalAlignment = align
            setPadding(6f)
        }
    }

    fun generarCertificado(
        numero: String,
        ext: Extintor,
        cliente: Cliente? = null,
        sede: Sede? = null,
        supervisor: Usuario? = null
    ): String {
        ensureDir()
        val filename = "$BASE_DIR/certificado-$numero.pdf"
        val doc = Document(PageSize.A4, 36f, 36f, 54f, 54f)
        PdfWriter.getInstance(doc, FileOutputStream(filename))
        doc.open()
        val titleFont: Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)
        val sectionFont: Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f)
        val normal: Font = FontFactory.getFont(FontFactory.HELVETICA, 10f)

        // Encabezado
        doc.add(Paragraph("ExtinGrafic - Certificado de Servicio", titleFont))
        doc.add(Paragraph("Número: $numero", normal))
        doc.add(Paragraph("Fecha de emisión: ${LocalDateTime.now(ZoneOffset.UTC)} UTC", normal))
        doc.add(Paragraph(" "))

        // Datos de cliente y supervisor
        doc.add(Paragraph("Datos del cliente", sectionFont))
        val clienteTable = PdfPTable(2).apply { widthPercentage = 100f }
        clienteTable.addCell(cell("Cliente", normal, bold = true))
        clienteTable.addCell(cell(cliente?.nombre ?: "N/D", normal))
        clienteTable.addCell(cell("RUT", normal, bold = true))
        clienteTable.addCell(cell(cliente?.rut ?: "N/D", normal))
        clienteTable.addCell(cell("Sede", normal, bold = true))
        clienteTable.addCell(cell(sede?.nombre ?: "Sin sede asociada", normal))
        clienteTable.addCell(cell("Direccion", normal, bold = true))
        clienteTable.addCell(cell(sede?.direccion ?: "-", normal))
        clienteTable.addCell(cell("Supervisor/Técnico", normal, bold = true))
        clienteTable.addCell(cell(supervisor?.let { "${it.nombre} ${it.apellido}" } ?: "No informado", normal))
        doc.add(clienteTable)
        doc.add(Paragraph(" "))

        // Datos de extintor
        doc.add(Paragraph("Detalle del extintor", sectionFont))
        val extTable = PdfPTable(2).apply { widthPercentage = 100f }
        extTable.addCell(cell("Código QR", normal, bold = true))
        extTable.addCell(cell(ext.codigoQr, normal))
        extTable.addCell(cell("Tipo / Agente", normal, bold = true))
        extTable.addCell(cell("${ext.tipo} - ${ext.agente}", normal))
        extTable.addCell(cell("Capacidad", normal, bold = true))
        extTable.addCell(cell(ext.capacidad, normal))
        extTable.addCell(cell("Última recarga", normal, bold = true))
        extTable.addCell(cell(ext.fechaUltimaRecarga?.toString() ?: "No registrada", normal))
        extTable.addCell(cell("Próximo vencimiento", normal, bold = true))
        extTable.addCell(cell(ext.fechaProximoVencimiento?.toString() ?: "No calculado", normal))
        doc.add(extTable)
        doc.add(Paragraph(" "))

        // Declaración
        val texto = """
            Certificamos que el extintor indicado ha sido inspeccionado y recargado según las
            especificaciones del fabricante y normas vigentes. El equipo queda habilitado para uso
            hasta la fecha de vencimiento indicada, salvo daño físico o descarga.
        """.trimIndent()
        val parrafo = Paragraph(texto, normal)
        parrafo.alignment = Element.ALIGN_JUSTIFIED
        doc.add(parrafo)
        doc.add(Paragraph(" "))

        val firmaTable = PdfPTable(2).apply { widthPercentage = 100f }
        firmaTable.addCell(cell("_________________________", normal, align = Element.ALIGN_CENTER))
        firmaTable.addCell(cell("_________________________", normal, align = Element.ALIGN_CENTER))
        firmaTable.addCell(cell("Supervisor", normal, align = Element.ALIGN_CENTER))
        firmaTable.addCell(cell("Cliente / Recepción", normal, align = Element.ALIGN_CENTER))
        doc.add(firmaTable)

        doc.close()
        return filename
    }

    fun generarOrdenPdf(numero: String, orden: OrdenServicio, extintores: List<Extintor>): String {
        ensureDir()
        val filename = "$BASE_DIR/orden-$numero.pdf"
        val doc = Document(PageSize.A4)
        PdfWriter.getInstance(doc, FileOutputStream(filename))
        doc.open()
        val titleFont: Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f)
        val normal: Font = FontFactory.getFont(FontFactory.HELVETICA, 10f)
        doc.add(Paragraph("ORDEN DE SERVICIO", titleFont))
        doc.add(Paragraph("Orden ID: ${orden.id.value}", normal))
        doc.add(Paragraph("Fecha Programada: ${orden.fechaProgramada}", normal))
        doc.add(Paragraph("Estado: ${orden.estado}", normal))
        doc.add(Paragraph("Cliente ID: ${orden.clienteId.value}", normal))
        orden.sedeId?.let { doc.add(Paragraph("Sede ID: ${it.value}", normal)) }
        doc.add(Paragraph("Extintores Asociados:", normal))
        extintores.forEach { e ->
            doc.add(Paragraph(" - ${e.id.value} | QR=${e.codigoQr} | Tipo=${e.tipo} | Agente=${e.agente} | Cap=${e.capacidad} | Próx=${e.fechaProximoVencimiento}", normal))
        }
        doc.add(Paragraph("Generado: ${LocalDateTime.now(ZoneOffset.UTC)} UTC", normal))
        doc.close()
        return filename
    }
}
