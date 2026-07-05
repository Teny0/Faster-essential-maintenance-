package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class MaintenanceRepository(private val dao: MaintenanceDao) {

    val allEntities: Flow<List<EntityItem>> = dao.getAllEntities()
    val allServices: Flow<List<ServiceItem>> = dao.getAllServices()
    val allRequests: Flow<List<RequestItem>> = dao.getAllRequests()
    val allScheduleTasks: Flow<List<ScheduleTaskItem>> = dao.getAllScheduleTasks()
    val allNotifications: Flow<List<NotificationItem>> = dao.getAllNotifications()

    fun getServicesByEntity(entityId: Int): Flow<List<ServiceItem>> = dao.getServicesByEntity(entityId)
    fun getRequestsByEntity(entityId: Int): Flow<List<RequestItem>> = dao.getRequestsByEntity(entityId)

    suspend fun getEntityById(id: Int): EntityItem? = dao.getEntityById(id)
    suspend fun getServiceById(id: Int): ServiceItem? = dao.getServiceById(id)
    suspend fun getRequestById(id: Int): RequestItem? = dao.getRequestById(id)

    suspend fun insertEntity(entity: EntityItem): Int {
        return dao.insertEntity(entity).toInt()
    }

    suspend fun insertService(service: ServiceItem): Int {
        return dao.insertService(service).toInt()
    }

    suspend fun updateServiceStatus(id: Int, status: String, detail: String) {
        dao.updateServiceStatus(id, status, detail)
        
        // When status is updated, we can automatically insert a notification about it!
        val service = dao.getServiceById(id)
        if (service != null) {
            val title = when (status) {
                "DOWN" -> "🚨 SERVICE DOWN: ${service.name}"
                "UNDER_MAINTENANCE" -> "⚙️ SERVICE UNDER MAINTENANCE: ${service.name}"
                else -> "✅ SERVICE RESTORED: ${service.name}"
            }
            val message = "Service '${service.name}' status changed to '$status'. Detail: $detail"
            val type = if (status == "DOWN") "ALERT" else if (status == "UNDER_MAINTENANCE") "INFO" else "SUCCESS"
            
            dao.insertNotification(
                NotificationItem(
                    entityId = service.entityId,
                    title = title,
                    message = message,
                    type = type
                )
            )

            // Dynamic check for cascading impacts
            if (status == "DOWN" || status == "UNDER_MAINTENANCE") {
                // Find services that depend on this service in the same entity
                val allEntServices = dao.getServicesByEntity(service.entityId).firstOrNull() ?: emptyList()
                val dependentServices = allEntServices.filter { 
                    val deps = it.dependencyIds.split(",").map { idStr -> idStr.trim() }.filter { s -> s.isNotEmpty() }
                    deps.contains(id.toString())
                }
                if (dependentServices.isNotEmpty()) {
                    val depNames = dependentServices.joinToString { it.name }
                    dao.insertNotification(
                        NotificationItem(
                            entityId = service.entityId,
                            title = "⚠️ CASCADING WARNING: Multiple Services Affected",
                            message = "Failure in '${service.name}' affects: $depNames",
                            type = "CASCADING"
                        )
                    )
                }
            }
        }
    }

    suspend fun raiseRequest(request: RequestItem): Int {
        val requestId = dao.insertRequest(request).toInt()
        
        // Create an alert notification
        val entity = dao.getEntityById(request.entityId)
        val service = dao.getServiceById(request.serviceId)
        if (entity != null && service != null) {
            dao.insertNotification(
                NotificationItem(
                    entityId = request.entityId,
                    title = "New Request: [${request.priority}] ${service.category}",
                    message = "Raised by ${request.raisedBy} in ${request.location}: ${request.description}",
                    type = if (request.priority == "EMERGENCY" || request.priority == "HIGH") "ALERT" else "INFO"
                )
            )
        }
        return requestId
    }

    suspend fun updateRequest(request: RequestItem) {
        dao.updateRequest(request)
        
        // Automatically update service status if request state suggests it, or alert
        val service = dao.getServiceById(request.serviceId)
        if (service != null) {
            val title = "Request Updated (ID: #${request.id})"
            val message = "Request for '${service.name}' is now ${request.status}."
            dao.insertNotification(
                NotificationItem(
                    entityId = request.entityId,
                    title = title,
                    message = message,
                    type = "INFO"
                )
            )
        }
    }

    suspend fun insertScheduleTask(task: ScheduleTaskItem): Int {
        return dao.insertScheduleTask(task).toInt()
    }

    suspend fun runScheduleTask(taskId: Int) {
        val tasks = dao.getAllScheduleTasks().firstOrNull() ?: emptyList()
        val task = tasks.find { it.id == taskId } ?: return
        
        val lastRun = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastRun
        
        when (task.frequency) {
            "WEEKLY" -> calendar.add(Calendar.DAY_OF_YEAR, 7)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "QUARTERLY" -> calendar.add(Calendar.MONTH, 3)
            else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val nextDue = calendar.timeInMillis
        
        dao.markScheduleTaskRun(taskId, lastRun, nextDue)
        
        // Also automatically trigger a mock resolved request to document PM run
        val service = dao.getServiceById(task.serviceId)
        if (service != null) {
            val pmRequest = RequestItem(
                entityId = task.entityId,
                serviceId = task.serviceId,
                description = "Preventive Maintenance: Run task '${task.taskName}' successfully.",
                priority = "LOW",
                status = "VERIFIED",
                assignedTo = "Auto-System",
                createdAt = lastRun,
                resolvedAt = lastRun,
                raisedBy = "System Schedule",
                location = "Standard Maintenance Unit",
                technicianNotes = "Performed standard preventive checkups. All systems nominal."
            )
            dao.insertRequest(pmRequest)
            
            dao.insertNotification(
                NotificationItem(
                    entityId = task.entityId,
                    title = "✅ PM Task Completed: ${task.taskName}",
                    message = "Preventive check performed on '${service.name}'. Next due: ${android.text.format.DateFormat.format("dd MMM yyyy", nextDue)}",
                    type = "SUCCESS"
                )
            )
        }
    }

    suspend fun markNotificationAsRead(id: Int) = dao.markNotificationAsRead(id)
    suspend fun markAllNotificationsAsRead() = dao.markAllNotificationsAsRead()

    suspend fun seedDatabaseIfNeeded() {
        val currentEntities = dao.getAllEntities().firstOrNull() ?: emptyList()
        if (currentEntities.isNotEmpty()) return

        // 1. Entities
        val ent1Id = dao.insertEntity(EntityItem(name = "Sunset Green Heights", type = "HOUSE", address = "Road No. 4, Residential Valley")).toInt()
        val ent2Id = dao.insertEntity(EntityItem(name = "TechPark Alpha Office", type = "COMPANY", address = "Sector 5, IT Hub Block B")).toInt()
        val ent3Id = dao.insertEntity(EntityItem(name = "West Zone Municipal", type = "CITY_ZONE", address = "Zone-5 Municipal Corporation, Metro Area")).toInt()

        // 2. Services
        // Sunset Green Heights (Residential)
        val s1 = dao.insertService(ServiceItem(entityId = ent1Id, category = "ELECTRICITY", name = "Main Power Substation", dependencyIds = "", status = "HEALTHY", statusDetail = "Grid input 230V stable"))
        val s2 = dao.insertService(ServiceItem(entityId = ent1Id, category = "WATER", name = "Society Water Pump House", dependencyIds = "$s1", status = "HEALTHY", statusDetail = "Main valve open"))
        val s3 = dao.insertService(ServiceItem(entityId = ent1Id, category = "ELEVATOR", name = "Residential Elevators (Block A)", dependencyIds = "$s1", status = "HEALTHY", statusDetail = "Sensors calibrated"))
        val s4 = dao.insertService(ServiceItem(entityId = ent1Id, category = "INTERNET", name = "Society Fiber Switch", dependencyIds = "$s1", status = "HEALTHY", statusDetail = "Gbps backup connected"))
        val s5 = dao.insertService(ServiceItem(entityId = ent1Id, category = "GENERATOR", name = "Basement Generator Set", dependencyIds = "", status = "HEALTHY", statusDetail = "Diesel fuel 85%"))

        // TechPark Alpha (Company Office)
        val s6 = dao.insertService(ServiceItem(entityId = ent2Id, category = "ELECTRICITY", name = "Main Electrical Transformer", dependencyIds = "", status = "HEALTHY", statusDetail = "Feeders active"))
        val s7 = dao.insertService(ServiceItem(entityId = ent2Id, category = "HVAC", name = "HVAC Central Chiller Plant", dependencyIds = "$s6", status = "HEALTHY", statusDetail = "Chilled water flow 7 m/s"))
        val s8 = dao.insertService(ServiceItem(entityId = ent2Id, category = "INTERNET", name = "Server Room Router Rack", dependencyIds = "$s6", status = "HEALTHY", statusDetail = "Main ISP fiber online"))
        val s9 = dao.insertService(ServiceItem(entityId = ent2Id, category = "SECURITY", name = "Fire Alarm & CCTV Security Hub", dependencyIds = "$s6", status = "HEALTHY", statusDetail = "UPS battery fully charged"))

        // West Zone Municipal (City)
        val s10 = dao.insertService(ServiceItem(entityId = ent3Id, category = "ELECTRICITY", name = "High Voltage Grid Feed", dependencyIds = "", status = "HEALTHY", statusDetail = "33KV feeder active"))
        val s11 = dao.insertService(ServiceItem(entityId = ent3Id, category = "WASTE", name = "Sub-Station Sewage Treatment Plant", dependencyIds = "$s10", status = "HEALTHY", statusDetail = "Primary aeration nominal"))
        val s12 = dao.insertService(ServiceItem(entityId = ent3Id, category = "ROAD_INFRA", name = "Arterial Road Layout", dependencyIds = "", status = "HEALTHY", statusDetail = "Signals synced"))
        val s13 = dao.insertService(ServiceItem(entityId = ent3Id, category = "PLUMBING", name = "Zone 5 Water Reservoir Valves", dependencyIds = "$s10", status = "HEALTHY", statusDetail = "Telemetry monitoring live"))

        // 3. Requests
        // Request 1: Elevator issue (Active)
        dao.insertRequest(
            RequestItem(
                entityId = ent1Id,
                serviceId = s3.toInt(),
                description = "Elevator in Block A makes a strange grinding noise during descent and stops suddenly at the 4th floor.",
                priority = "HIGH",
                status = "ASSIGNED",
                assignedTo = "Rohit Kumar",
                createdAt = System.currentTimeMillis() - 2 * 3600 * 1000, // 2 hrs ago
                raisedBy = "John resident (Flat 402)",
                location = "Block A Lift Core"
            )
        )

        // Request 2: Server room Internet failure (Emergency)
        dao.insertRequest(
            RequestItem(
                entityId = ent2Id,
                serviceId = s8.toInt(),
                description = "Server Room primary fiber link down. Failing over to backup ISP. Speeds are extremely slow.",
                priority = "EMERGENCY",
                status = "IN_PROGRESS",
                assignedTo = "Amit Sharma",
                createdAt = System.currentTimeMillis() - 4 * 3600 * 1000, // 4 hrs ago
                raisedBy = "Alice (IT Admin)",
                location = "Floor 4 Server Room"
            )
        )

        // Request 3: Municipal Pothole (Pending)
        dao.insertRequest(
            RequestItem(
                entityId = ent3Id,
                serviceId = s12.toInt(),
                description = "Massive pothole near the central square crossroad. Extremely dangerous for two-wheelers in the rain.",
                priority = "LOW",
                status = "PENDING",
                raisedBy = "Citizen Rajan",
                location = "Central Square Crossing"
            )
        )

        // Request 4: Water Pressure (Resolved)
        dao.insertRequest(
            RequestItem(
                entityId = ent1Id,
                serviceId = s2.toInt(),
                description = "Water flow is very sluggish on the top floors of Block B. Seems like a valve airlock.",
                priority = "MEDIUM",
                status = "VERIFIED",
                assignedTo = "Rohit Kumar",
                createdAt = System.currentTimeMillis() - 24 * 3600 * 1000, // 1 day ago
                resolvedAt = System.currentTimeMillis() - 22 * 3600 * 1000,
                raisedBy = "Mrs. Gupta (Flat 1002)",
                location = "Block B Valve Room",
                feedbackRating = 5,
                feedbackComment = "Very prompt! Rohit cleared the airlock within 30 minutes. Thank you!",
                technicianNotes = "Opened the relief valve on the 10th floor manifold. Cleared trapped air bubble. Maintained pressure of 3.2 bar."
            )
        )

        // 4. Schedules
        val c = Calendar.getInstance()
        
        c.timeInMillis = System.currentTimeMillis()
        c.add(Calendar.DAY_OF_YEAR, 3)
        dao.insertScheduleTask(ScheduleTaskItem(entityId = ent1Id, serviceId = s3.toInt(), taskName = "Monthly Elevator Governor Check", frequency = "MONTHLY", nextDueDate = c.timeInMillis))
        
        c.timeInMillis = System.currentTimeMillis()
        c.add(Calendar.DAY_OF_YEAR, 1)
        dao.insertScheduleTask(ScheduleTaskItem(entityId = ent1Id, serviceId = s5.toInt(), taskName = "Weekly Generator Diesel and Oil Level Check", frequency = "WEEKLY", nextDueDate = c.timeInMillis))
        
        c.timeInMillis = System.currentTimeMillis()
        c.add(Calendar.DAY_OF_YEAR, 8)
        dao.insertScheduleTask(ScheduleTaskItem(entityId = ent2Id, serviceId = s7.toInt(), taskName = "Quarterly Chiller Compressor Pressure Test", frequency = "QUARTERLY", nextDueDate = c.timeInMillis))
        
        c.timeInMillis = System.currentTimeMillis()
        c.add(Calendar.DAY_OF_YEAR, 2)
        dao.insertScheduleTask(ScheduleTaskItem(entityId = ent3Id, serviceId = s11.toInt(), taskName = "Bi-weekly Sewer Aerator Dissolved Oxygen Check", frequency = "WEEKLY", nextDueDate = c.timeInMillis))

        // 5. Initial Notifications
        dao.insertNotification(NotificationItem(title = "Welcome to faster essential maintenance", message = "System initialized successfully with mock data. Switch roles at the top to explore.", type = "INFO"))
        dao.insertNotification(NotificationItem(entityId = ent2Id, title = "🚨 Emergency Alert Active", message = "Emergency issue reported on Server Room Router Rack", type = "ALERT"))
    }
}
