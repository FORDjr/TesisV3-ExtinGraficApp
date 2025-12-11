package org.example.project.data.maintenance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import org.example.project.data.model.YearMonth
import org.example.project.data.api.CrearExtintorRequest
import org.example.project.data.api.CrearServiceRegistroRequest
import org.example.project.data.api.MaintenanceApiService
import org.example.project.data.api.RemoteCliente
import org.example.project.data.api.RemoteExtintor
import org.example.project.data.api.RemoteOrdenServicio
import org.example.project.data.api.RemoteSede
import org.example.project.data.api.ServiceRegistroResponse
import org.example.project.data.api.ActualizarExtintorRequest
import kotlin.math.max

private const val DEFAULT_OWNER = "ExtinGrafic"

class MaintenanceRepository {

    private val api = MaintenanceApiService()

    private val _extinguishers = MutableStateFlow<List<ExtinguisherAsset>>(emptyList())
    val extinguishers: StateFlow<List<ExtinguisherAsset>> = _extinguishers.asStateFlow()

    private val _maintenanceRecords = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    val maintenanceRecords: StateFlow<List<MaintenanceRecord>> = _maintenanceRecords.asStateFlow()

    private val _loanRecords = MutableStateFlow<List<LoanRecord>>(emptyList())
    val loanRecords: StateFlow<List<LoanRecord>> = _loanRecords.asStateFlow()

    private val _partInventory = MutableStateFlow<List<PartInventoryItem>>(emptyList())
    val partInventory: StateFlow<List<PartInventoryItem>> = _partInventory.asStateFlow()

    private val _stockAlerts = MutableStateFlow<List<StockAlert>>(emptyList())
    val stockAlerts: StateFlow<List<StockAlert>> = _stockAlerts.asStateFlow()

    private var extinguisherCounter = 1
    private var maintenanceCounter = 1
    private var loanCounter = 1
    private var historyCounter = 1
    private var movementCounter = 1
    private var cachedClientes: List<RemoteCliente> = emptyList()
    private var cachedSedes: List<RemoteSede> = emptyList()

    suspend fun scanExtintor(codigo: String): RemoteExtintor? = try {
        api.scanExtintor(codigo)
    } catch (e: Exception) {
        println("scanExtintor error: ${e.message}")
        null
    }

    suspend fun registrarServicio(
        extintorId: Int,
        tecnicoId: Int?,
        ordenId: Int?,
        observaciones: String?,
        pesoInicial: String?
    ): ServiceRegistroResponse {
        val req = CrearServiceRegistroRequest(
            extintorId = extintorId,
            tecnicoId = tecnicoId,
            ordenId = ordenId,
            observaciones = observaciones,
            pesoInicial = pesoInicial
        )
        val resp = api.registrarServicio(req)
        // refrescar datos para reflejar nuevo vencimiento/estado
        refreshFromBackend()
        return resp
    }

    suspend fun refreshFromBackend() {
        try {
            val clientes = api.obtenerClientes()
            val sedes = api.obtenerSedes()
            val extintores = api.obtenerExtintores()
            val ordenes = api.obtenerOrdenes()
            cachedClientes = clientes
            cachedSedes = sedes

            val clientesMap = clientes.associateBy { it.id }
            val sedesMap = sedes.associateBy { it.id }
            val extintorMap = extintores.associateBy { it.id }

            val assets = extintores.map { it.toAsset(clientesMap, sedesMap) }
            val records = ordenes.map { it.toMaintenanceRecord(clientesMap, sedesMap, extintorMap) }

            _extinguishers.value = assets
            _maintenanceRecords.value = records
            extinguisherCounter = assets.size + 1
            maintenanceCounter = records.size + 1
        } catch (e: Exception) {
            if (_extinguishers.value.isEmpty() && _maintenanceRecords.value.isEmpty()) {
                seedDemoData()
            }
            println("MaintenanceRepository refresh error: ${e.message}")
        }
    }

    fun registerWorkshopIntake(
        owner: String,
        externalNumber: String,
        technician: String,
        intakeDate: LocalDate = today(),
        location: String? = "Taller",
        retentionDays: Int = 2,
        notes: String? = null
    ): MaintenanceRecord {
        val code = generateExtinguisherCode(extinguisherCounter++)
        val qrPayload = buildQrPayload(code)
        val entry = historyEntry(
            action = "Ingreso a taller",
            actor = technician,
            notes = notes
        )
        val asset = ExtinguisherAsset(
            code = code,
            serialNumber = externalNumber,
            owner = owner,
            location = location,
            intakeDate = intakeDate,
            status = ExtinguisherStatus.IN_WORKSHOP,
            qrInfo = QrInfo(code = code, payload = qrPayload),
            history = listOf(entry)
        )
        updateExtinguishers(asset)

        val expectedDelivery = intakeDate.plus(DatePeriod(days = max(1, retentionDays)))
        val record = MaintenanceRecord(
            id = nextMaintenanceId(),
            extinguisherCode = code,
            type = MaintenanceType.WORKSHOP,
            status = MaintenanceStatus.CHECK_IN,
            registeredOn = intakeDate,
            expectedDelivery = expectedDelivery,
            technician = technician,
            client = owner,
            location = location,
            notes = notes,
            history = listOf(entry)
        )
        _maintenanceRecords.value = _maintenanceRecords.value + record
        return record
    }

    suspend fun createExtinguisher(
        code: String,
        owner: String,
        location: String?,
        status: ExtinguisherStatus,
        actor: String,
        notes: String? = null,
        clienteId: Int?,
        sedeId: Int?
    ): ExtinguisherAsset {
        val normalizedCode = code.trim().ifBlank { generateExtinguisherCode(extinguisherCounter++) }
        val normalizedOwner = owner.ifBlank { DEFAULT_OWNER }
        val clienteTarget = clienteId ?: cachedClientes.firstOrNull()?.id ?: 1
        val sedeTarget = sedeId ?: cachedSedes.firstOrNull { it.clienteId == clienteTarget }?.id
        val backend = runCatching {
            api.crearExtintor(
                CrearExtintorRequest(
                    codigoQr = normalizedCode,
                    clienteId = clienteTarget,
                    sedeId = sedeTarget,
                    tipo = "PQS",
                    agente = "ABC",
                    capacidad = "4kg",
                    ubicacion = location,
                    estadoLogistico = status.toBackend()
                )
            )
        }.getOrNull()

        refreshFromBackend()
        val created = _extinguishers.value.firstOrNull { it.code == normalizedCode }
        if (created != null) return created

        // fallback local si backend fallo
        val entry = historyEntry(
            action = "Creacion manual de QR (offline)",
            actor = actor,
            notes = notes ?: location
        )
        val asset = ExtinguisherAsset(
            code = normalizedCode,
            serialNumber = normalizedCode,
            owner = normalizedOwner,
            location = location?.takeUnless { it.isBlank() },
            intakeDate = today(),
            status = status,
            qrInfo = QrInfo(code = normalizedCode, payload = buildQrPayload(normalizedCode)),
            history = listOf(entry)
        )
        updateExtinguishers(asset)
        return asset
    }

    fun markRetainedInWorkshop(
        maintenanceId: String,
        days: Int,
        actor: String,
        notes: String? = null
    ): MaintenanceRecord? {
        val record = _maintenanceRecords.value.firstOrNull { it.id == maintenanceId } ?: return null
        val updatedDelivery = record.registeredOn.plus(DatePeriod(days = max(1, days)))
        val entry = historyEntry(
            action = "Retenido en taller",
            actor = actor,
            notes = "Retencion por ${max(1, days)} dias" + (notes?.let { ". $it" } ?: "")
        )
        val updated = record.copy(
            status = MaintenanceStatus.IN_PROGRESS,
            expectedDelivery = updatedDelivery,
            history = record.history + entry
        )
        replaceMaintenanceRecord(updated)
        updateExtinguisherStatus(
            record.extinguisherCode,
            ExtinguisherStatus.IN_WORKSHOP,
            entry,
            record.location ?: "Taller"
        )
        return updated
    }

    fun registerPartsUsage(
        maintenanceId: String,
        usages: List<PartUsage>,
        actor: String,
        notes: String? = null
    ): MaintenanceRecord? {
        if (usages.isEmpty()) return _maintenanceRecords.value.firstOrNull { it.id == maintenanceId }
        val record = _maintenanceRecords.value.firstOrNull { it.id == maintenanceId } ?: return null
        val entry = historyEntry(
            action = "Registro de repuestos",
            actor = actor,
            notes = notes ?: ""
        )
        val combinedParts = mergePartUsage(record.partsUsed, usages)
        val updated = record.copy(
            partsUsed = combinedParts,
            status = MaintenanceStatus.IN_PROGRESS,
            history = record.history + entry
        )
        replaceMaintenanceRecord(updated)
        usages.forEach { usage ->
            consumePartStock(usage.partId, usage.quantity, maintenanceId, actor, notes)
        }
        updateExtinguisherHistory(record.extinguisherCode, entry)
        return updated
    }

    fun closeMaintenance(
        maintenanceId: String,
        actor: String,
        notes: String? = null,
        deliveredOn: LocalDate = today()
    ): MaintenanceRecord? {
        val record = _maintenanceRecords.value.firstOrNull { it.id == maintenanceId } ?: return null
        val entry = historyEntry(
            action = "Servicio cerrado",
            actor = actor,
            notes = notes
        )
        val updated = record.copy(
            status = MaintenanceStatus.COMPLETED,
            expectedDelivery = deliveredOn,
            history = record.history + entry
        )
        replaceMaintenanceRecord(updated)
        promoteExtinguisherAsAvailable(record.extinguisherCode, deliveredOn, entry)
        if (record.loanId != null) {
            markLoanAsReturned(record.loanId, actor, notes)
        }
        return updated
    }

    fun registerFieldVisit(
        client: String,
        technician: String,
        scheduledAt: LocalDate = today(),
        notes: String? = null
    ): LoanRecord {
        val entry = historyEntry(
            action = "Salida a terreno",
            actor = technician,
            notes = notes
        )
        val loan = LoanRecord(
            id = nextLoanId(),
            client = client,
            technician = technician,
            scheduledAt = scheduledAt,
            status = LoanStatus.PREPARING,
            notes = notes,
            history = listOf(entry)
        )
        _loanRecords.value = _loanRecords.value + loan
        return loan
    }

    fun openFieldMaintenance(
        extinguisherCode: String,
        client: String,
        technician: String,
        location: String? = null,
        loanId: String? = null,
        notes: String? = null,
        expectedDelivery: LocalDate? = null
    ): MaintenanceRecord? {
        val asset = _extinguishers.value.firstOrNull { it.code == extinguisherCode } ?: return null
        val entry = historyEntry(
            action = "Mantenimiento en terreno",
            actor = technician,
            notes = notes
        )
        val record = MaintenanceRecord(
            id = nextMaintenanceId(),
            extinguisherCode = extinguisherCode,
            type = MaintenanceType.FIELD,
            status = MaintenanceStatus.IN_PROGRESS,
            registeredOn = today(),
            expectedDelivery = expectedDelivery,
            technician = technician,
            client = client,
            location = location,
            loanId = loanId,
            notes = notes,
            history = listOf(entry)
        )
        _maintenanceRecords.value = _maintenanceRecords.value + record
        updateExtinguisherStatus(extinguisherCode, ExtinguisherStatus.IN_FIELD_SERVICE, entry, location)
        return record
    }
    fun assignLoanExtinguishers(
        loanId: String,
        loanExtinguishers: List<String>,
        actor: String
    ): LoanRecord? {
        val loan = _loanRecords.value.firstOrNull { it.id == loanId } ?: return null
        val deliveredCount = loanExtinguishers.size
        val entry = historyEntry(
            action = "Entrega de extinguidor de prestamo",
            actor = actor,
            notes = "Se entregaron $deliveredCount unidades"
        )
        val locationHint = loan.notes?.takeUnless { it.isBlank() } ?: loan.client
        val newLoanItems = loanExtinguishers.map { code ->
            updateExtinguisherStatus(code, ExtinguisherStatus.ON_LOAN, entry, locationHint)
            LoanExtinguisher(
                code = code,
                qrPayload = buildLoanQrPayload(code),
                approxLocation = locationHint
            )
        }
        val updated = loan.copy(
            status = LoanStatus.ACTIVE,
            loanExtinguishers = loan.loanExtinguishers + newLoanItems,
            history = loan.history + entry
        )
        replaceLoanRecord(updated)
        return updated
    }

    fun markOriginalExtinguishersInRepair(
        loanId: String,
        extinguisherCodes: List<String>,
        actor: String
    ): LoanRecord? {
        val loan = _loanRecords.value.firstOrNull { it.id == loanId } ?: return null
        val locationHint = loan.client
        extinguisherCodes.forEach { code ->
            val entry = historyEntry(
                action = "Extinguidor en reparacion",
                actor = actor,
                notes = "Asignado al loan $loanId"
            )
            updateExtinguisherStatus(code, ExtinguisherStatus.IN_FIELD_SERVICE, entry, locationHint)
        }
        val entry = historyEntry(
            action = "Extinguidores del cliente en reparacion",
            actor = actor,
            notes = "Se marcaron ${extinguisherCodes.size} unidades"
        )
        val updated = loan.copy(
            originalExtinguishers = (loan.originalExtinguishers + extinguisherCodes).distinct(),
            history = loan.history + entry
        )
        replaceLoanRecord(updated)
        return updated
    }

    fun registerLoanReturn(
        loanId: String,
        returnedLoanCodes: List<String>,
        repairedExtinguishers: List<String>,
        actor: String,
        notes: String? = null
    ): LoanRecord? {
        val loan = _loanRecords.value.firstOrNull { it.id == loanId } ?: return null
        val entry = historyEntry(
            action = "Devolucion de prestamo",
            actor = actor,
            notes = notes
        )
        val loanItems = loan.loanExtinguishers.map { loanExt ->
            if (returnedLoanCodes.contains(loanExt.code)) {
                loanExt.copy(returned = true, returnedOn = today())
            } else {
                loanExt
            }
        }
        returnedLoanCodes.forEach { code ->
            updateExtinguisherStatus(code, ExtinguisherStatus.AVAILABLE, entry, "Taller")
        }
        repairedExtinguishers.forEach { code ->
            promoteExtinguisherAsAvailable(code, today(), entry)
        }
        val status = if (loanItems.all { it.returned }) LoanStatus.RETURNED else loan.status
        val updated = loan.copy(
            status = status,
            loanExtinguishers = loanItems,
            history = loan.history + entry
        )
        replaceLoanRecord(updated)
        return updated
    }

    fun reprintQr(code: String, requestedBy: String, reason: String): QrInfo? {
        val asset = _extinguishers.value.firstOrNull { it.code == code } ?: return null
        val log = QrReprintLog(
            timestamp = today(),
            requestedBy = requestedBy,
            reason = reason
        )
        val refreshedPayload = buildQrPayload(asset.code)
        val updated = asset.copy(
            qrInfo = asset.qrInfo.copy(
                payload = refreshedPayload,
                lastGeneratedOn = today(),
                reprintHistory = asset.qrInfo.reprintHistory + log
            )
        )
        updateExtinguishers(updated)
        return updated.qrInfo
    }

    suspend fun updateExtinguisherLocation(
        code: String,
        newLocation: String,
        actor: String,
        notes: String? = null,
        newStatus: ExtinguisherStatus? = null
    ): ExtinguisherAsset? {
        val asset = _extinguishers.value.firstOrNull { it.code == code } ?: return null
        val backendStatus = newStatus?.toBackend() ?: asset.status.toBackend()
        val req = ActualizarExtintorRequest(
            ubicacion = newLocation,
            estadoLogistico = backendStatus
        )
        runCatching {
            asset.id?.let { api.actualizarExtintor(it, req) }
        }
        val entry = historyEntry(
            action = "Actualizacion de ubicacion",
            actor = actor,
            notes = notes ?: newLocation
        )
        val updated = asset.copy(
            location = newLocation,
            status = newStatus ?: asset.status,
            qrInfo = asset.qrInfo.copy(payload = buildQrPayload(asset.code), lastGeneratedOn = today()),
            history = asset.history + entry
        )
        updateExtinguishers(updated)
        refreshFromBackend()
        return updated
    }

    fun addPartStock(
        partId: String,
        quantity: Int,
        actor: String,
        notes: String? = null
    ): PartInventoryItem? {
        val part = _partInventory.value.firstOrNull { it.id == partId } ?: return null
        val newStock = part.stock + max(0, quantity)
        val movement = StockMovement(
            id = nextMovementId(),
            date = today(),
            quantity = quantity,
            type = StockMovementType.INBOUND,
            notes = notes
        )
        val updated = part.copy(
            stock = newStock,
            movements = part.movements + movement
        )
        replacePart(updated)
        refreshStockAlerts(updated)
        return updated
    }

    fun getMonthlyReport(month: YearMonth): List<MonthlyPartReport> = _partInventory.value.map { part ->
        val consumption = part.monthlyConsumption.firstOrNull { it.month == month }?.quantity ?: 0
        MonthlyPartReport(
            partId = part.id,
            partName = part.name,
            month = month,
            consumedQuantity = consumption
        )
    }

    fun getPurchaseSuggestions(
        currentMonth: YearMonth,
        bufferPercentage: Double = 0.2
    ): List<PurchaseSuggestion> {
        val previous = currentMonth.previous()
        return _partInventory.value.mapNotNull { part ->
            val previousConsumption = part.monthlyConsumption.firstOrNull { it.month == previous }?.quantity ?: 0
            if (previousConsumption == 0) return@mapNotNull null
            val suggested = (previousConsumption * (1.0 + bufferPercentage)).toInt().coerceAtLeast(previousConsumption)
            PurchaseSuggestion(
                partId = part.id,
                partName = part.name,
                suggestedQuantity = suggested,
                rationale = "Basado en consumo de ${previous.monthNumber}/${previous.year}"
            )
        }
    }

    fun getExtinguisherQr(code: String): String? = _extinguishers.value.firstOrNull { it.code == code }?.qrInfo?.payload

    fun getMaintenanceHistoryForExtinguisher(code: String): List<MaintenanceRecord> =
        _maintenanceRecords.value.filter { it.extinguisherCode == code }

    fun getMaintenanceById(id: String): MaintenanceRecord? =
        _maintenanceRecords.value.firstOrNull { it.id == id }

    fun getLoanById(id: String): LoanRecord? =
        _loanRecords.value.firstOrNull { it.id == id }

    private fun replaceMaintenanceRecord(record: MaintenanceRecord) {
        _maintenanceRecords.value = _maintenanceRecords.value.map { if (it.id == record.id) record else it }
    }

    private fun replaceLoanRecord(record: LoanRecord) {
        _loanRecords.value = _loanRecords.value.map { if (it.id == record.id) record else it }
    }

    private fun replacePart(item: PartInventoryItem) {
        _partInventory.value = _partInventory.value.map { if (it.id == item.id) item else it }
    }

    private fun promoteExtinguisherAsAvailable(code: String, maintenanceDate: LocalDate, entry: MaintenanceHistoryEntry) {
        val asset = _extinguishers.value.firstOrNull { it.code == code } ?: return
        val updated = asset.copy(
            status = ExtinguisherStatus.AVAILABLE,
            lastMaintenanceDate = maintenanceDate,
            qrInfo = asset.qrInfo.copy(payload = buildQrPayload(asset.code), lastGeneratedOn = today()),
            history = asset.history + entry
        )
        updateExtinguishers(updated)
    }

    private fun markLoanAsReturned(loanId: String, actor: String, notes: String?) {
        val loan = _loanRecords.value.firstOrNull { it.id == loanId } ?: return
        if (loan.status == LoanStatus.RETURNED) return
        val entry = historyEntry(
            action = "Servicio completado",
            actor = actor,
            notes = notes
        )
        val updated = loan.copy(
            status = LoanStatus.RETURNED,
            history = loan.history + entry
        )
        replaceLoanRecord(updated)
    }

    private fun consumePartStock(
        partId: String,
        quantity: Int,
        maintenanceId: String,
        actor: String,
        notes: String?
    ) {
        if (quantity <= 0) return
        val part = _partInventory.value.firstOrNull { it.id == partId } ?: return
        val consumed = max(0, quantity)
        val newStock = max(0, part.stock - consumed)
        val movement = StockMovement(
            id = nextMovementId(),
            date = today(),
            quantity = consumed,
            type = StockMovementType.OUTBOUND,
            relatedMaintenanceId = maintenanceId,
            notes = notes ?: "Registro por $actor"
        )
        val currentMonth = YearMonth(today().year, today().monthNumber)
        val consumptions = part.monthlyConsumption.toMutableList()
        val monthIndex = consumptions.indexOfFirst { it.month == currentMonth }
        if (monthIndex >= 0) {
            val current = consumptions[monthIndex]
            consumptions[monthIndex] = current.copy(quantity = current.quantity + consumed)
        } else {
            consumptions += MonthlyConsumption(month = currentMonth, quantity = consumed)
        }
        val updated = part.copy(
            stock = newStock,
            monthlyConsumption = consumptions,
            movements = part.movements + movement
        )
        replacePart(updated)
        refreshStockAlerts(updated)
    }

    private fun refreshStockAlerts(updatedPart: PartInventoryItem) {
        val alerts = _partInventory.value.filter { it.isBelowMinimum }.map { part ->
            StockAlert(
                partId = part.id,
                partName = part.name,
                currentStock = part.stock,
                minimumStock = part.minimumStock
            )
        }
        _stockAlerts.value = alerts
    }

    private fun updateExtinguisherStatus(
        code: String,
        status: ExtinguisherStatus,
        entry: MaintenanceHistoryEntry,
        newLocation: String? = null
    ) {
        val asset = _extinguishers.value.firstOrNull { it.code == code } ?: return
        val updated = asset.copy(
            status = status,
            location = newLocation ?: asset.location,
            history = asset.history + entry
        )
        updateExtinguishers(updated)
    }

    private fun updateExtinguisherHistory(code: String, entry: MaintenanceHistoryEntry) {
        val asset = _extinguishers.value.firstOrNull { it.code == code } ?: return
        val updated = asset.copy(history = asset.history + entry)
        updateExtinguishers(updated)
    }

    private fun updateExtinguishers(updated: ExtinguisherAsset) {
        val current = _extinguishers.value.toMutableList()
        val index = current.indexOfFirst { it.code == updated.code }
        if (index >= 0) {
            current[index] = updated
        } else {
            current += updated
        }
        _extinguishers.value = current
    }

    private fun mergePartUsage(existing: List<PartUsage>, incoming: List<PartUsage>): List<PartUsage> {
        val accumulator = mutableMapOf<String, Int>()
        existing.forEach { usage -> accumulator[usage.partId] = (accumulator[usage.partId] ?: 0) + usage.quantity }
        incoming.forEach { usage -> accumulator[usage.partId] = (accumulator[usage.partId] ?: 0) + usage.quantity }
        return accumulator.map { (partId, qty) -> PartUsage(partId = partId, quantity = qty) }
    }

    private fun buildQrPayload(code: String): String = code

    private fun buildLoanQrPayload(code: String): String = code

    private fun historyEntry(action: String, actor: String, notes: String? = null): MaintenanceHistoryEntry = MaintenanceHistoryEntry(
        id = "H-${historyCounter++}",
        date = today(),
        action = action,
        actor = actor,
        notes = notes
    )

    private fun nextMaintenanceId(): String = "MT-${maintenanceCounter++}"

    private fun nextLoanId(): String = "LN-${loanCounter++}"

    private fun nextMovementId(): String = "MV-${movementCounter++}"

    private fun seedDemoData() {
        val baseDate = today().minus(DatePeriod(days = 10))

        val valve = PartInventoryItem(
            id = "PT-VALVE",
            name = "Valvula universal",
            type = PartType.VALVE,
            unit = "unidad",
            stock = 12,
            minimumStock = 5
        )
        val hose = PartInventoryItem(
            id = "PT-HOSE",
            name = "Manguera 1 pulgada",
            type = PartType.HOSE,
            unit = "unidad",
            stock = 8,
            minimumStock = 4
        )
        val liquid = PartInventoryItem(
            id = "PT-LIQUID",
            name = "Agente extintor ABC 6kg",
            type = PartType.LIQUID,
            unit = "kg",
            stock = 25,
            minimumStock = 10
        )
        _partInventory.value = listOf(valve, hose, liquid)
        refreshStockAlerts(valve)

        val code1 = generateExtinguisherCode(extinguisherCounter++)
        val baseHistory = MaintenanceHistoryEntry(
            id = "H-${historyCounter++}",
            date = baseDate,
            action = "Historial inicial",
            actor = "Sistema"
        )
        val asset1 = ExtinguisherAsset(
            code = code1,
            serialNumber = "0001",
            owner = "Cliente Demo",
            location = "Bodega principal",
            intakeDate = baseDate,
            lastMaintenanceDate = baseDate,
            status = ExtinguisherStatus.AVAILABLE,
            qrInfo = QrInfo(
                code = code1,
                payload = buildQrPayload(code1)
            ),
            history = listOf(baseHistory)
        )
        _extinguishers.value = listOf(asset1)
    }

    private fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}

private fun RemoteExtintor.toAsset(
    clientes: Map<Int, RemoteCliente>,
    sedes: Map<Int, RemoteSede>
): ExtinguisherAsset {
    val ownerName = clientes[clienteId]?.nombre ?: "Cliente #$clienteId"
    val sedeName = sedeId?.let { sedes[it]?.nombre }
    val intake = parseLocalDate(fechaProximoVencimiento) ?: currentDate()
    val status = estadoLogistico.toExtinguisherStatusFromBackend()
        ?: when {
            estado != null -> estado.toExtinguisherStatus()
            diasParaVencer != null -> diasParaVencer.toExtinguisherStatusFromDias()
            else -> color.toExtinguisherStatus()
        }
    return ExtinguisherAsset(
        id = id,
        code = codigoQr,
        serialNumber = codigoQr,
        owner = ownerName,
        location = ubicacion ?: sedeName,
        intakeDate = intake,
        lastMaintenanceDate = intake,
        status = status,
        qrInfo = QrInfo(code = codigoQr, payload = codigoQr),
        history = emptyList()
    )
}

private fun RemoteOrdenServicio.toMaintenanceRecord(
    clientes: Map<Int, RemoteCliente>,
    sedes: Map<Int, RemoteSede>,
    extintoresMap: Map<Int, RemoteExtintor>
): MaintenanceRecord {
    val date = parseLocalDate(fechaProgramada) ?: currentDate()
    val codes = if (extintores.isEmpty()) {
        "Sin extintores"
    } else {
        extintores.joinToString(", ") { id ->
            extintoresMap[id]?.codigoQr ?: "EXT-$id"
        }
    }
    return MaintenanceRecord(
        id = "OS-$id",
        extinguisherCode = codes,
        type = MaintenanceType.WORKSHOP,
        status = estado.toMaintenanceStatus(),
        registeredOn = date,
        expectedDelivery = date,
        technician = tecnicoId?.let { "Técnico #$it" },
        client = clientes[clienteId]?.nombre ?: "Cliente #$clienteId",
        location = sedeId?.let { sedes[it]?.nombre },
        notes = null,
        history = emptyList()
    )
}

private fun String?.toExtinguisherStatus(): ExtinguisherStatus = when (this?.lowercase()) {
    "vencido" -> ExtinguisherStatus.OUT_OF_SERVICE
    "por_vencer" -> ExtinguisherStatus.IN_FIELD_SERVICE
    "rojo" -> ExtinguisherStatus.IN_WORKSHOP
    "amarillo" -> ExtinguisherStatus.IN_FIELD_SERVICE
    "verde", "vigente" -> ExtinguisherStatus.AVAILABLE
    else -> ExtinguisherStatus.AVAILABLE
}

private fun String?.toExtinguisherStatusFromBackend(): ExtinguisherStatus? = when (this?.uppercase()) {
    "DISPONIBLE" -> ExtinguisherStatus.AVAILABLE
    "TALLER" -> ExtinguisherStatus.IN_WORKSHOP
    "TERRENO" -> ExtinguisherStatus.IN_FIELD_SERVICE
    "PRESTAMO" -> ExtinguisherStatus.ON_LOAN
    "FUERA_SERVICIO" -> ExtinguisherStatus.OUT_OF_SERVICE
    else -> null
}

private fun String.toMaintenanceStatus(): MaintenanceStatus = when (this.uppercase()) {
    "PLANIFICADA" -> MaintenanceStatus.CHECK_IN
    "EN_PROGRESO" -> MaintenanceStatus.IN_PROGRESS
    "CERRADA" -> MaintenanceStatus.COMPLETED
    "CANCELADA" -> MaintenanceStatus.CANCELLED
    else -> MaintenanceStatus.REGISTERED
}

private fun Long.toExtinguisherStatusFromDias(): ExtinguisherStatus = when {
    this <= 0 -> ExtinguisherStatus.OUT_OF_SERVICE
    this <= 30 -> ExtinguisherStatus.IN_FIELD_SERVICE
    else -> ExtinguisherStatus.AVAILABLE
}

private fun ExtinguisherStatus.toBackend(): String = when (this) {
    ExtinguisherStatus.AVAILABLE -> "DISPONIBLE"
    ExtinguisherStatus.IN_WORKSHOP -> "TALLER"
    ExtinguisherStatus.IN_FIELD_SERVICE -> "TERRENO"
    ExtinguisherStatus.ON_LOAN -> "PRESTAMO"
    ExtinguisherStatus.OUT_OF_SERVICE -> "FUERA_SERVICIO"
}

private fun parseLocalDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return try {
        Instant.parse(value).toLocalDateTime(TimeZone.currentSystemDefault()).date
    } catch (_: Exception) {
        try {
            LocalDateTime.parse(value).date
        } catch (_: Exception) {
            null
        }
    }
}

private fun currentDate(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date







