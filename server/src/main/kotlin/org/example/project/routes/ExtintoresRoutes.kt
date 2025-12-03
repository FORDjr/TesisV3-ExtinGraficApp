package org.example.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.project.models.ClienteRequest
import org.example.project.models.SedeRequest
import org.example.project.models.ExtintorRequest
import org.example.project.models.ExtintorUpdateRequest
import org.example.project.models.CrearOrdenServicioRequest
import org.example.project.models.EstadoOrdenServicio
import org.example.project.models.CertificadoResponse
import org.example.project.services.ExtintoresService
import org.example.project.services.ServiceRegistroService
import org.example.project.services.AlertService
import java.io.File
import org.example.project.models.CrearServiceRegistroRequest
import org.example.project.models.OrdenServicio
import org.example.project.models.Extintor
import org.example.project.services.PdfGenerator
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.extintoresRoutes() {
    val service = ExtintoresService()
    val serviceRegistroService = ServiceRegistroService()
    val alertService = AlertService(service)

    // Nuevo: scan por codigo QR
    route("/api/extintores/scan") {
        get {
            val codigo = call.request.queryParameters["codigoQr"]
            if (codigo.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "codigoQr requerido")); return@get
            }
            val lista = service.listarExtintores(null, null, null).filter { it.codigoQr == codigo }
            if (lista.isEmpty()) call.respond(HttpStatusCode.NotFound, mapOf("error" to "No encontrado")) else call.respond(lista.first())
        }
    }

    route("/api/clientes") {
        get { call.respond(service.listarClientes()) }
        post {
            val req = call.receive<ClienteRequest>()
            call.respond(HttpStatusCode.Created, service.crearCliente(req))
        }
    }

    route("/api/sedes") {
        get {
            val clienteId = call.request.queryParameters["clienteId"]?.toIntOrNull()
            call.respond(service.listarSedes(clienteId))
        }
        post {
            val req = call.receive<SedeRequest>()
            call.respond(HttpStatusCode.Created, service.crearSede(req))
        }
    }

    route("/api/extintores") {
        get {
            val clienteId = call.request.queryParameters["clienteId"]?.toIntOrNull()
            val sedeId = call.request.queryParameters["sedeId"]?.toIntOrNull()
            val color = call.request.queryParameters["color"]
            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val size = call.request.queryParameters["size"]?.toIntOrNull()
            call.respond(service.listarExtintores(clienteId, sedeId, color, page, size))
        }
        post {
            try {
                val req = call.receive<ExtintorRequest>()
                val creado = service.crearExtintor(req)
                call.respond(HttpStatusCode.Created, creado)
            } catch (e: Exception) {
                val msg = e.message ?: "error"
                if (msg.contains("duplicate key", ignoreCase = true) || msg.contains("unique", ignoreCase = true)) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "codigoQr duplicado"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to msg))
                }
            }
        }
        patch("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                return@patch
            }
            val update = call.receive<ExtintorUpdateRequest>()
            val res = service.actualizarExtintor(id, update)
            if (res == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "No encontrado"))
            else call.respond(res)
        }
    }

    // Certificados
    route("/api/certificados") {
        get {
            val extintorId = call.request.queryParameters["extintorId"]?.toIntOrNull()
            call.respond(service.listarCertificados(extintorId))
        }
        post("/{extintorId}/emitir") {
            val extintorId = call.parameters["extintorId"]?.toIntOrNull()
            if (extintorId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "extintorId inválido"))
                return@post
            }
            val cert = service.emitirCertificado(extintorId)
            if (cert == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Extintor no encontrado"))
            else call.respond(HttpStatusCode.Created, cert)
        }
        get("/{id}/pdf") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido")); return@get }
            val path = service.obtenerRutaPdfCertificado(id)
            if (path == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Certificado no encontrado")); return@get }
            val file = File(path)
            if (!file.exists()) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Archivo no existe")); return@get }
            call.response.headers.append(HttpHeaders.ContentDisposition, "attachment; filename=\"${file.name}\"")
            call.respondFile(file)
        }
    }

    // Registros de servicio
    route("/api/servicios") {
        get {
            val extintorId = call.request.queryParameters["extintorId"]?.toIntOrNull()
            val ordenId = call.request.queryParameters["ordenId"]?.toIntOrNull()
            call.respond(serviceRegistroService.listar(extintorId, ordenId))
        }
        post {
            try {
                val req = call.receive<CrearServiceRegistroRequest>()
                val res = serviceRegistroService.crear(req)
                call.respond(HttpStatusCode.Created, res)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "error")))
            }
        }
    }

    // Nuevo: extintor por ID
    route("/api/extintores/{id}") {
        get {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido")); return@get }
            val uno = service.obtenerExtintor(id)
            if (uno == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "No encontrado")) else call.respond(uno)
        }
    }

    // Nuevo: Rutas de órdenes de servicio
    route("/api/ordenes") {
        get {
            val clienteId = call.request.queryParameters["clienteId"]?.toIntOrNull()
            val estadoParam = call.request.queryParameters["estado"]
            val estado = try { estadoParam?.let { EstadoOrdenServicio.valueOf(it) } } catch (_: Exception) { null }
            call.respond(service.listarOrdenes(clienteId, estado))
        }
        post {
            try {
                val req = call.receive<CrearOrdenServicioRequest>()
                val creada = service.crearOrden(req)
                call.respond(HttpStatusCode.Created, creada)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "error")))
            }
        }
        patch("/{id}/estado") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido")); return@patch }
            val body = call.receive<Map<String,String>>()
            val estadoStr = body["estado"]
            val estado = try { estadoStr?.let { EstadoOrdenServicio.valueOf(it) } } catch (_: Exception) { null }
            if (estado == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Estado inválido")); return@patch }
            val res = service.actualizarEstadoOrden(id, estado)
            if (res == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "No encontrada")) else call.respond(res)
        }
    }

    // Nuevo: PDF de orden
    route("/api/ordenes/{id}/pdf") {
        get {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido")); return@get }
            val ordResp = service.obtenerOrden(id)
            if (ordResp == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Orden no encontrada")); return@get }
            // Obtener entidades extintor en transacción para PDF
            val pair: Pair<OrdenServicio?, List<Extintor>> = transaction {
                val ord = OrdenServicio.findById(id)
                val exts = ordResp.extintores.mapNotNull { Extintor.findById(it) }
                ord to exts
            }
            val (ordenEntidad, extList) = pair
            if (ordenEntidad == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "Orden no encontrada")); return@get }
            val numero = "OS-${id}-${System.currentTimeMillis()%10000}"
            val pdfPath = try { PdfGenerator.generarOrdenPdf(numero, ordenEntidad, extList) } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error generando PDF: ${e.message}")); return@get
            }
            val file = java.io.File(pdfPath)
            if (!file.exists()) { call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Archivo no generado")); return@get }
            call.response.headers.append(HttpHeaders.ContentDisposition, "attachment; filename=\"${file.name}\"")
            call.respondFile(file)
        }
    }

    // Alertas
    route("/api/alertas") {
        get {
            val pend = call.request.queryParameters["pendientes"]?.toBooleanStrictOrNull()
            call.respond(alertService.listar(pend))
        }
        post("/generar") {
            val creadas = alertService.generarAlertasVencimiento()
            call.respond(mapOf("creadas" to creadas))
        }
        post("/generarStock") {
            val creadas = alertService.generarAlertasStock()
            call.respond(mapOf("creadas" to creadas))
        }
        post("/reenviar") {
            val reenviadas = alertService.reenviarPendientes()
            call.respond(mapOf("reenviadas" to reenviadas))
        }
        patch("/{id}/enviada") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido")); return@patch }
            val res = alertService.marcarEnviada(id)
            if (res == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "No encontrada")) else call.respond(res)
        }
    }
}
