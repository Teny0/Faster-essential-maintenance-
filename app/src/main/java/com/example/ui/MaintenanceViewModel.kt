package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.EntityItem
import com.example.data.MaintenanceRepository
import com.example.data.NotificationItem
import com.example.data.RequestItem
import com.example.data.ScheduleTaskItem
import com.example.data.ServiceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MaintenanceViewModel(private val repository: MaintenanceRepository) : ViewModel() {

    // Filter states
    private val _currentRole = MutableStateFlow("Super Admin") // "Super Admin", "Entity Admin", "Technician", "End User"
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    private val _selectedEntityId = MutableStateFlow<Int?>(null) // null means "All" or "Global"
    val selectedEntityId: StateFlow<Int?> = _selectedEntityId.asStateFlow()

    // Database flows
    val entities: StateFlow<List<EntityItem>> = repository.allEntities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawServices: StateFlow<List<ServiceItem>> = repository.allServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawRequests: StateFlow<List<RequestItem>> = repository.allRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduleTasks: StateFlow<List<ScheduleTaskItem>> = repository.allScheduleTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationItem>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulated technician list
    val technicians = listOf("Rohit Kumar", "Amit Sharma", "Priya Patel", "Vikram Singh")

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfNeeded()
        }
    }

    // Computed effective services incorporating cascading outages
    val services: StateFlow<List<ComputedService>> = rawServices
        .combine(rawServices) { rawList, _ ->
            rawList.map { service ->
                val (effStatus, effDetail) = computeEffectiveStatus(service.id, rawList)
                ComputedService(
                    service = service,
                    effectiveStatus = effStatus,
                    effectiveDetail = effDetail
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtering requests based on role and selected entity
    val filteredRequests: StateFlow<List<RequestItem>> = combine(
        rawRequests, _currentRole, _selectedEntityId
    ) { reqs, role, entId ->
        var list = reqs
        // Filter by Entity if one is selected
        if (entId != null) {
            list = list.filter { it.entityId == entId }
        }
        // Filter by Role constraints
        when (role) {
            "Technician" -> {
                // Technicians see requests assigned to them, or all if none matches
                val assignedToCurrentTech = list.filter { it.assignedTo == "Rohit Kumar" || it.assignedTo == "Amit Sharma" }
                if (assignedToCurrentTech.isNotEmpty()) assignedToCurrentTech else list
            }
            "End User" -> {
                // End users see their own complaints (raisedBy != System Schedule)
                list.filter { it.raisedBy != "System Schedule" }
            }
            else -> list // Admins see everything
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtering services based on selected entity
    val filteredServices: StateFlow<List<ComputedService>> = services
        .combine(_selectedEntityId) { list, entId ->
            if (entId != null) {
                list.filter { it.service.entityId == entId }
            } else {
                list
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter schedule tasks by entity
    val filteredSchedules: StateFlow<List<ScheduleTaskItem>> = scheduleTasks
        .combine(_selectedEntityId) { list, entId ->
            if (entId != null) {
                list.filter { it.entityId == entId }
            } else {
                list
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI actions
    fun switchRole(role: String) {
        _currentRole.value = role
        // Adjust entity context if switching roles
        if (role == "Entity Admin" && _selectedEntityId.value == null) {
            // Default to first entity for convenience
            viewModelScope.launch {
                val list = entities.value
                if (list.isNotEmpty()) {
                    _selectedEntityId.value = list.first().id
                }
            }
        }
    }

    fun switchEntity(entityId: Int?) {
        _selectedEntityId.value = entityId
    }

    fun raiseComplaint(
        entityId: Int,
        category: String,
        serviceId: Int,
        description: String,
        priority: String,
        location: String,
        raisedBy: String
    ) {
        viewModelScope.launch {
            val req = RequestItem(
                entityId = entityId,
                serviceId = serviceId,
                description = description,
                priority = priority,
                status = "PENDING",
                raisedBy = raisedBy,
                location = location
            )
            repository.raiseRequest(req)
        }
    }

    fun updateRequestStatus(requestId: Int, status: String, technicianNotes: String?) {
        viewModelScope.launch {
            val req = repository.getRequestById(requestId) ?: return@launch
            val resolvedTime = if (status == "RESOLVED" || status == "VERIFIED") System.currentTimeMillis() else null
            val updated = req.copy(
                status = status,
                resolvedAt = resolvedTime ?: req.resolvedAt,
                technicianNotes = technicianNotes ?: req.technicianNotes
            )
            repository.updateRequest(updated)
        }
    }

    fun assignTechnician(requestId: Int, technicianName: String) {
        viewModelScope.launch {
            val req = repository.getRequestById(requestId) ?: return@launch
            val updated = req.copy(
                status = "ASSIGNED",
                assignedTo = technicianName
            )
            repository.updateRequest(updated)
        }
    }

    fun changeServiceStatus(serviceId: Int, status: String, detail: String) {
        viewModelScope.launch {
            repository.updateServiceStatus(serviceId, status, detail)
        }
    }

    fun runScheduleTask(taskId: Int) {
        viewModelScope.launch {
            repository.runScheduleTask(taskId)
        }
    }

    fun rateRequest(requestId: Int, rating: Int, comment: String) {
        viewModelScope.launch {
            val req = repository.getRequestById(requestId) ?: return@launch
            val updated = req.copy(
                status = "VERIFIED",
                feedbackRating = rating,
                feedbackComment = comment
            )
            repository.updateRequest(updated)
        }
    }

    fun markNotificationRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    // Helper to compute effective service status
    private fun computeEffectiveStatus(serviceId: Int, allServices: List<ServiceItem>): Pair<String, String> {
        val visited = mutableSetOf<Int>()
        return checkStatusRecursive(serviceId, allServices, visited)
    }

    private fun checkStatusRecursive(
        serviceId: Int,
        allServices: List<ServiceItem>,
        visited: MutableSet<Int>
    ): Pair<String, String> {
        // Prevent cyclic dependencies from causing infinite loop
        if (!visited.add(serviceId)) {
            return Pair("HEALTHY", "")
        }

        val service = allServices.find { it.id == serviceId } ?: return Pair("HEALTHY", "")

        // If the service is raw DOWN or UNDER_MAINTENANCE, return its state immediately
        if (service.status == "DOWN") {
            return Pair("DOWN", "Primary failure on ${service.name}")
        }
        if (service.status == "UNDER_MAINTENANCE") {
            return Pair("UNDER_MAINTENANCE", "Direct maintenance work on ${service.name}")
        }

        // Check direct dependencies
        val deps = service.dependencyIds.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toIntOrNull() }

        for (depId in deps) {
            val depService = allServices.find { it.id == depId }
            if (depService != null) {
                val (depEffective, _) = checkStatusRecursive(depId, allServices, visited)
                if (depEffective == "DOWN") {
                    return Pair("DOWN", "Cascading failure: Depended service '${depService.name}' is DOWN")
                }
                if (depEffective == "UNDER_MAINTENANCE") {
                    return Pair("UNDER_MAINTENANCE", "Cascading warning: Depended service '${depService.name}' is Under Maintenance")
                }
            }
        }

        return Pair(service.status, service.statusDetail)
    }
}

// Wrapper for UI presentation that holds the computed state
data class ComputedService(
    val service: ServiceItem,
    val effectiveStatus: String, // "HEALTHY", "UNDER_MAINTENANCE", "DOWN"
    val effectiveDetail: String
)

class MaintenanceViewModelFactory(private val repository: MaintenanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MaintenanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MaintenanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
