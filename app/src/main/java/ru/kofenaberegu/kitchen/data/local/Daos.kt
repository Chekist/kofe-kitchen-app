package ru.kofenaberegu.kitchen.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KitchenDao {
    @Query("SELECT * FROM report_history ORDER BY createdAt DESC")
    fun observeHistory(): Flow<List<ReportHistoryEntity>>

    @Insert
    suspend fun insertReport(report: ReportHistoryEntity): Long

    @Query("UPDATE report_history SET status = :status WHERE id = :id")
    suspend fun updateReportStatus(id: Long, status: String)

    @Query("SELECT COUNT(*) FROM write_off_records WHERE shiftDate = :shiftDate AND productNameNormalized = :normalizedName")
    suspend fun countWriteOffForShift(shiftDate: String, normalizedName: String): Int

    @Insert
    suspend fun insertWriteOff(record: WriteOffRecordEntity)

    @Query("SELECT * FROM admin_remarks ORDER BY isResolved ASC, createdAt DESC")
    fun observeRemarks(): Flow<List<AdminRemarkEntity>>

    @Insert
    suspend fun insertRemark(remark: AdminRemarkEntity)

    @Query("UPDATE admin_remarks SET isResolved = 1, resolvedPhotoPath = :photoPath WHERE id = :id")
    suspend fun markRemarkResolved(id: Long, photoPath: String)

    @Query("SELECT * FROM chemical_sheet ORDER BY id ASC")
    fun observeChemicals(): Flow<List<ChemicalItemEntity>>

    @Insert
    suspend fun insertChemical(item: ChemicalItemEntity)

    @Delete
    suspend fun deleteChemical(item: ChemicalItemEntity)

    @Query("SELECT * FROM custom_reminders ORDER BY id DESC")
    fun observeReminders(): Flow<List<CustomReminderEntity>>

    @Insert
    suspend fun insertReminder(reminder: CustomReminderEntity)

    @Query("UPDATE custom_reminders SET isEnabled = :enabled WHERE id = :id")
    suspend fun toggleReminder(id: Long, enabled: Boolean)

    @Query("SELECT * FROM message_queue ORDER BY createdAt ASC")
    suspend fun getPendingQueue(): List<MessageQueueEntity>

    @Query("SELECT COUNT(*) FROM message_queue")
    fun observePendingCount(): Flow<Int>

    @Insert
    suspend fun enqueueMessage(msg: MessageQueueEntity)

    @Delete
    suspend fun removeFromQueue(msg: MessageQueueEntity)
}
