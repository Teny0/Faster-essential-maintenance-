package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    // Entities
    @Query("SELECT * FROM entities")
    fun getAllEntities(): Flow<List<EntityItem>>

    @Query("SELECT * FROM entities WHERE id = :id")
    suspend fun getEntityById(id: Int): EntityItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntity(entity: EntityItem): Long

    @Query("DELETE FROM entities")
    suspend fun clearEntities()

    // Services
    @Query("SELECT * FROM services")
    fun getAllServices(): Flow<List<ServiceItem>>

    @Query("SELECT * FROM services WHERE entityId = :entityId")
    fun getServicesByEntity(entityId: Int): Flow<List<ServiceItem>>

    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun getServiceById(id: Int): ServiceItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceItem): Long

    @Query("UPDATE services SET status = :status, statusDetail = :detail WHERE id = :id")
    suspend fun updateServiceStatus(id: Int, status: String, detail: String)

    @Query("DELETE FROM services")
    suspend fun clearServices()

    // Requests
    @Query("SELECT * FROM requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<RequestItem>>

    @Query("SELECT * FROM requests WHERE entityId = :entityId ORDER BY createdAt DESC")
    fun getRequestsByEntity(entityId: Int): Flow<List<RequestItem>>

    @Query("SELECT * FROM requests WHERE id = :id")
    suspend fun getRequestById(id: Int): RequestItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RequestItem): Long

    @Update
    suspend fun updateRequest(request: RequestItem)

    @Query("DELETE FROM requests")
    suspend fun clearRequests()

    // Schedule Tasks
    @Query("SELECT * FROM schedule_tasks ORDER BY nextDueDate ASC")
    fun getAllScheduleTasks(): Flow<List<ScheduleTaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleTask(task: ScheduleTaskItem): Long

    @Query("UPDATE schedule_tasks SET lastRunDate = :lastRun, nextDueDate = :nextDue WHERE id = :id")
    suspend fun markScheduleTaskRun(id: Int, lastRun: Long, nextDue: Long)

    @Query("DELETE FROM schedule_tasks")
    suspend fun clearScheduleTasks()

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()
}

@Database(
    entities = [
        EntityItem::class,
        ServiceItem::class,
        RequestItem::class,
        ScheduleTaskItem::class,
        NotificationItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun maintenanceDao(): MaintenanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "maintainhub_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
