package ru.kofenaberegu.kitchen.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "report_history")
data class ReportHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleType: String,
    val title: String,
    val messageText: String,
    val photoPathsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String
)

@Entity(tableName = "write_off_records")
data class WriteOffRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftDate: String,
    val productNameNormalized: String,
    val amountText: String,
    val reason: String,
    val cookName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_remarks")
data class AdminRemarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val assignee: String,
    val deadline: String,
    val adminPhotoPath: String = "",
    val isResolved: Boolean = false,
    val resolvedPhotoPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chemical_sheet")
data class ChemicalItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val name: String,
    val concentration: String,
    val exposureTime: String,
    val rinsingRequired: String,
    val ppe: String,
    val instructionLocation: String
)

@Entity(tableName = "custom_reminders")
data class CustomReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateTimeText: String,
    val repeatRule: String,
    val isEnabled: Boolean = true
)

@Entity(tableName = "message_queue")
data class MessageQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportId: Long,
    val text: String,
    val photoPathsJoined: String,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
