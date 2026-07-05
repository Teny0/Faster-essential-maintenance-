package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entities")
data class EntityItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "HOUSE", "COMPANY", "CITY_ZONE"
    val address: String
)

@Entity(tableName = "services")
data class ServiceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityId: Int,
    val category: String, // "ELECTRICITY", "WATER", "PLUMBING", "GAS", "INTERNET", "ELEVATOR", "GENERATOR", "HVAC", "WASTE", "SECURITY", "ROAD_INFRA"
    val name: String,
    val dependencyIds: String, // Comma-separated list of service IDs that this service depends on (e.g., "1,2")
    val status: String, // "HEALTHY", "UNDER_MAINTENANCE", "DOWN"
    val statusDetail: String = ""
)

@Entity(tableName = "requests")
data class RequestItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityId: Int,
    val serviceId: Int,
    val description: String,
    val priority: String, // "LOW", "MEDIUM", "HIGH", "EMERGENCY"
    val status: String, // "PENDING", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "VERIFIED"
    val assignedTo: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val raisedBy: String,
    val location: String,
    val feedbackRating: Int? = null,
    val feedbackComment: String? = null,
    val technicianNotes: String? = null
)

@Entity(tableName = "schedule_tasks")
data class ScheduleTaskItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityId: Int,
    val serviceId: Int,
    val taskName: String,
    val frequency: String, // "WEEKLY", "MONTHLY", "QUARTERLY"
    val nextDueDate: Long,
    val lastRunDate: Long? = null
)

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityId: Int? = null, // null if system-wide
    val title: String,
    val message: String,
    val type: String, // "INFO", "ALERT", "CASCADING", "SUCCESS"
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
