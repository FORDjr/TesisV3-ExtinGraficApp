package org.example.project.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.project.data.maintenance.ExtinguisherAsset
import org.example.project.data.maintenance.ExtinguisherStatus
import org.example.project.data.maintenance.LoanRecord
import org.example.project.data.maintenance.MaintenanceRecord
import org.example.project.data.maintenance.MaintenanceRepository
import org.example.project.data.maintenance.MaintenanceStatus
import org.example.project.data.maintenance.MaintenanceType
import org.example.project.data.maintenance.MonthlyPartReport
import org.example.project.data.maintenance.PartUsage
import org.example.project.data.maintenance.PurchaseSuggestion
import org.example.project.data.maintenance.StockAlert
import org.example.project.data.model.YearMonth
import kotlinx.datetime.LocalDate

class MaintenanceViewModel : ViewModel() {

    private val repository = MaintenanceRepository()

    val extinguishers = repository.extinguishers
    val maintenanceRecords = repository.maintenanceRecords
    val loanRecords = repository.loanRecords
    val partInventory = repository.partInventory
    val stockAlerts = repository.stockAlerts

    private val _selectedMaintenance = MutableStateFlow<MaintenanceRecord?>(null)
    val selectedMaintenance: StateFlow<MaintenanceRecord?> = _selectedMaintenance.asStateFlow()

    private val _selectedLoan = MutableStateFlow<LoanRecord?>(null)
    val selectedLoan: StateFlow<LoanRecord?> = _selectedLoan.asStateFlow()

    private val _selectedExtinguisher = MutableStateFlow<ExtinguisherAsset?>(null)
    val selectedExtinguisher: StateFlow<ExtinguisherAsset?> = _selectedExtinguisher.asStateFlow()

    private val _selectedMonth = MutableStateFlow(currentMonth())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _monthlyReport = MutableStateFlow<List<MonthlyPartReport>>(emptyList())
    val monthlyReport: StateFlow<List<MonthlyPartReport>> = _monthlyReport.asStateFlow()

    private val _purchaseSuggestions = MutableStateFlow<List<PurchaseSuggestion>>(emptyList())
    val purchaseSuggestions: StateFlow<List<PurchaseSuggestion>> = _purchaseSuggestions.asStateFlow()

    val workshopMaintenances = maintenanceRecords.map { records ->
        records.filter { it.type == MaintenanceType.WORKSHOP }
    }.stateIn(viewModelScope, SharingPolicy, emptyList())

    val fieldMaintenances = maintenanceRecords.map { records ->
        records.filter { it.type == MaintenanceType.FIELD }
    }.stateIn(viewModelScope, SharingPolicy, emptyList())

    val maintenanceOverview: StateFlow<MaintenanceOverview> = combine(
        maintenanceRecords,
        loanRecords,
        stockAlerts,
        extinguishers
    ) { records, loans, alerts, assets ->
        val statusCount = assets.groupingBy { it.status }.eachCount()
        MaintenanceOverview(
            activeWorkshop = records.count { it.type == MaintenanceType.WORKSHOP && it.status !in completedStates },
            activeField = records.count { it.type == MaintenanceType.FIELD && it.status !in completedStates },
            activeLoans = loans.count { it.status != org.example.project.data.maintenance.LoanStatus.RETURNED },
            alerts = alerts.size,
            totalExtinguishers = assets.size,
            workshopExtinguishers = statusCount[ExtinguisherStatus.IN_WORKSHOP] ?: 0,
            fieldExtinguishers = statusCount[ExtinguisherStatus.IN_FIELD_SERVICE] ?: 0,
            loanExtinguishers = statusCount[ExtinguisherStatus.ON_LOAN] ?: 0,
            availableExtinguishers = statusCount[ExtinguisherStatus.AVAILABLE] ?: 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingPolicy,
        initialValue = MaintenanceOverview()
    )

    init {
        viewModelScope.launch {
            repository.refreshFromBackend()
            refreshAnalytics()
        }
    }

    fun registerWorkshopIntake(
        owner: String,
        externalNumber: String,
        technician: String,
        intakeDate: LocalDate,
        retentionDays: Int,
        location: String? = "Taller",
        notes: String? = null
    ) {
        viewModelScope.launch {
            val record = repository.registerWorkshopIntake(
                owner = owner,
                externalNumber = externalNumber,
                technician = technician,
                intakeDate = intakeDate,
                location = location,
                retentionDays = retentionDays,
                notes = notes
            )
            _selectedMaintenance.value = record
            _selectedExtinguisher.value = repository.extinguishers.value.firstOrNull { it.code == record.extinguisherCode }
            refreshAnalytics()
        }
    }

    fun markRetention(maintenanceId: String, days: Int, actor: String, notes: String? = null) {
        viewModelScope.launch {
            val record = repository.markRetainedInWorkshop(maintenanceId, days, actor, notes)
            _selectedMaintenance.value = record
            refreshAnalytics()
        }
    }

    fun registerPartsUsage(maintenanceId: String, parts: List<PartUsage>, actor: String, notes: String? = null) {
        viewModelScope.launch {
            val record = repository.registerPartsUsage(maintenanceId, parts, actor, notes)
            _selectedMaintenance.value = record
            refreshAnalytics()
        }
    }

    fun closeMaintenance(maintenanceId: String, actor: String, notes: String? = null, deliveredOn: LocalDate = today()) {
        viewModelScope.launch {
            val record = repository.closeMaintenance(maintenanceId, actor, notes, deliveredOn)
            _selectedMaintenance.value = record
            refreshAnalytics()
        }
    }

    fun registerFieldVisit(client: String, technician: String, scheduledAt: LocalDate, notes: String? = null) {
        viewModelScope.launch {
            val loan = repository.registerFieldVisit(client, technician, scheduledAt, notes)
            _selectedLoan.value = loan
            refreshAnalytics()
        }
    }

    fun openFieldMaintenance(
        extinguisherCode: String,
        client: String,
        technician: String,
        location: String? = null,
        loanId: String? = null,
        notes: String? = null,
        expectedDelivery: LocalDate? = null
    ) {
        viewModelScope.launch {
            val record = repository.openFieldMaintenance(
                extinguisherCode = extinguisherCode,
                client = client,
                technician = technician,
                location = location,
                loanId = loanId,
                notes = notes,
                expectedDelivery = expectedDelivery
            )
            _selectedMaintenance.value = record
            refreshAnalytics()
        }
    }

    fun createExtinguisher(
        code: String,
        owner: String,
        location: String?,
        status: ExtinguisherStatus,
        actor: String,
        notes: String? = null,
        onComplete: (Result<ExtinguisherAsset>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = runCatching {
                repository.createExtinguisher(code, owner, location, status, actor, notes)
            }
            result.onSuccess {
                _selectedExtinguisher.value = it
                refreshAnalytics()
            }
            onComplete(result)
        }
    }

    fun assignLoanExtinguishers(loanId: String, extinguisherCodes: List<String>, actor: String) {
        viewModelScope.launch {
            val loan = repository.assignLoanExtinguishers(loanId, extinguisherCodes, actor)
            _selectedLoan.value = loan
            refreshAnalytics()
        }
    }

    fun markClientExtinguishersInRepair(loanId: String, extinguisherCodes: List<String>, actor: String) {
        viewModelScope.launch {
            val loan = repository.markOriginalExtinguishersInRepair(loanId, extinguisherCodes, actor)
            _selectedLoan.value = loan
            refreshAnalytics()
        }
    }

    fun registerLoanReturn(
        loanId: String,
        returnedLoanCodes: List<String>,
        repairedExtinguishers: List<String>,
        actor: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val loan = repository.registerLoanReturn(loanId, returnedLoanCodes, repairedExtinguishers, actor, notes)
            _selectedLoan.value = loan
            refreshAnalytics()
        }
    }

    fun reprintQr(code: String, requestedBy: String, reason: String) {
        viewModelScope.launch {
            repository.reprintQr(code, requestedBy, reason)
            _selectedExtinguisher.value = repository.extinguishers.value.firstOrNull { it.code == code }
        }
    }

    fun updateExtinguisherLocation(
        code: String,
        newLocation: String,
        actor: String,
        notes: String? = null,
        status: ExtinguisherStatus? = null,
        onComplete: (Result<ExtinguisherAsset>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = runCatching {
                repository.updateExtinguisherLocation(code, newLocation, actor, notes, status)
                    ?: error("Extintor no encontrado")
            }
            result.onSuccess { _selectedExtinguisher.value = it }
            onComplete(result)
        }
    }

    fun addPartStock(partId: String, quantity: Int, actor: String, notes: String? = null) {
        viewModelScope.launch {
            repository.addPartStock(partId, quantity, actor, notes)
            refreshAnalytics()
        }
    }

    fun updateSelectedMaintenance(id: String?) {
        _selectedMaintenance.value = id?.let { repository.getMaintenanceById(it) }
    }

    fun updateSelectedLoan(id: String?) {
        _selectedLoan.value = id?.let { repository.getLoanById(it) }
    }

    fun updateSelectedExtinguisher(code: String?) {
        _selectedExtinguisher.value = code?.let { repository.extinguishers.value.firstOrNull { asset -> asset.code == it } }
    }

    fun setSelectedMonth(month: YearMonth) {
        _selectedMonth.value = month
        refreshMonthlyReport(month)
    }

    fun refreshAnalytics() {
        refreshMonthlyReport(_selectedMonth.value)
        refreshSuggestions(_selectedMonth.value)
    }

    fun syncMaintenanceData() {
        viewModelScope.launch {
            repository.refreshFromBackend()
            refreshAnalytics()
        }
    }

    private fun refreshMonthlyReport(month: YearMonth) {
        _monthlyReport.value = repository.getMonthlyReport(month)
    }

    private fun refreshSuggestions(month: YearMonth) {
        _purchaseSuggestions.value = repository.getPurchaseSuggestions(month)
    }

    companion object {
        private val completedStates = setOf(MaintenanceStatus.COMPLETED, MaintenanceStatus.CANCELLED)
        private val SharingPolicy = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000)
    }
}

private fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun LocalDate.monthNumberCompat(): Int = this.month.ordinal + 1

fun currentMonth(): YearMonth {
    val now = today()
    return YearMonth(now.year, now.monthNumberCompat())
}

data class MaintenanceOverview(
    val activeWorkshop: Int = 0,
    val activeField: Int = 0,
    val activeLoans: Int = 0,
    val alerts: Int = 0,
    val totalExtinguishers: Int = 0,
    val workshopExtinguishers: Int = 0,
    val fieldExtinguishers: Int = 0,
    val loanExtinguishers: Int = 0,
    val availableExtinguishers: Int = 0
)


