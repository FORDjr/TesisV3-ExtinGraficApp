package org.example.project.services

import com.lowagie.text.Document
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfWriter
import org.example.project.models.Extintor
import org.example.project.models.OrdenServicio
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

    fun generarCertificado(numero: String, ext: Extintor): String {
        ensureDir()
        val filename = "$BASE_DIR/certificado-$numero.pdf"
        val doc = Document(PageSize.A4)
        PdfWriter.getInstance(doc, FileOutputStream(filename))
        doc.open()
        val titleFont: Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)
        val normal: Font = FontFactory.getFont(FontFactory.HELVETICA, 11f)
        doc.add(Paragraph("CERTIFICADO DE RECARGA / MANTENCIÓN", titleFont))
        doc.add(Paragraph("Número: $numero", normal))
        doc.add(Paragraph("Extintor ID: ${ext.id.value}", normal))
        doc.add(Paragraph("Código QR: ${ext.codigoQr}", normal))
        doc.add(Paragraph("Cliente ID: ${ext.clienteId.value}", normal))
        doc.add(Paragraph("Tipo: ${ext.tipo}", normal))
        doc.add(Paragraph("Agente: ${ext.agente}", normal))
        doc.add(Paragraph("Capacidad: ${ext.capacidad}", normal))
        doc.add(Paragraph("Próximo Vencimiento: ${ext.fechaProximoVencimiento}", normal))
        doc.add(Paragraph("Generado: ${LocalDateTime.now(ZoneOffset.UTC)} UTC", normal))
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
