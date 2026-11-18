package ru.kofenaberegu.kitchen.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kofenaberegu.kitchen.data.local.*
import ru.kofenaberegu.kitchen.data.remote.TelegramSender
import ru.kofenaberegu.kitchen.domain.model.UserRole
import ru.kofenaberegu.kitchen.worker.TelegramSyncWorker
import java.text.SimpleDateFormat
import java.util.*

class KitchenViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = KitchenDatabase.getInstance(application).kitchenDao()

    val currentRole = MutableStateFlow(UserRole.COOK)
    val cookName = MutableStateFlow("Повар смены")

    val history = dao.observeHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val remarks = dao.observeRemarks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val chemicals = dao.observeChemicals().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val reminders = dao.observeReminders().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val pendingQueueCount = dao.observePendingCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun nowDateTime(): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
    fun todayDate(): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

    suspend fun isProductAlreadyWrittenOffToday(product: String): Boolean {
        return dao.countWriteOffForShift(todayDate(), product.trim().lowercase()) > 0
    }

    fun registerWriteOffRecord(product: String, amount: String, reason: String) {
        viewModelScope.launch {
            dao.insertWriteOff(
                WriteOffRecordEntity(
                    shiftDate = todayDate(),
                    productNameNormalized = product.trim().lowercase(),
                    amountText = amount,
                    reason = reason,
                    cookName = cookName.value
                )
            )
        }
    }

    fun submitReport(
        moduleType: String,
        title: String,
        messageText: String,
        photoPaths: List<String>,
        sendToTelegramNow: Boolean
    ) {
        viewModelScope.launch {
            val joinedPhotos = photoPaths.joinToString("|")
            val reportId = dao.insertReport(
                ReportHistoryEntity(
                    moduleType = moduleType,
                    title = title,
                    messageText = messageText,
                    photoPathsJson = joinedPhotos,
                    status = if (sendToTelegramNow) "QUEUED" else "OFFLINE_SAVED"
                )
            )

            if (sendToTelegramNow) {
                val sentViaBot = withContext(Dispatchers.IO) {
                    TelegramSender.sendViaBotApi(getApplication(), messageText, photoPaths)
                }
                if (sentViaBot) {
                    dao.updateReportStatus(reportId, "SENT")
                    Toast.makeText(getApplication(), "Отправлено в Telegram!", Toast.LENGTH_SHORT).show()
                } else {
                    dao.enqueueMessage(
                        MessageQueueEntity(
                            reportId = reportId,
                            text = messageText,
                            photoPathsJoined = joinedPhotos
                        )
                    )
                    if (TelegramSender.getBotToken(getApplication()).isBlank()) {
                        TelegramSender.shareViaTelegramIntent(getApplication(), messageText, photoPaths)
                    } else {
                        Toast.makeText(getApplication(), "Нет сети: поставлено в очередь отправки", Toast.LENGTH_LONG).show()
                        val req = OneTimeWorkRequestBuilder<TelegramSyncWorker>()
                            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                            .build()
                        WorkManager.getInstance(getApplication()).enqueue(req)
                    }
                }
            } else {
                Toast.makeText(getApplication(), "Сохранено локально в историю", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addRemark(text: String, assignee: String, deadline: String, photoPath: String) {
        viewModelScope.launch {
            dao.insertRemark(
                AdminRemarkEntity(
                    text = text,
                    assignee = assignee,
                    deadline = deadline,
                    adminPhotoPath = photoPath
                )
            )
        }
    }

    fun resolveRemark(id: Long, photoPath: String) {
        viewModelScope.launch {
            dao.markRemarkResolved(id, photoPath)
        }
    }

    fun addChemical(item: ChemicalItemEntity) {
        viewModelScope.launch { dao.insertChemical(item) }
    }

    fun addCustomReminder(title: String, dateTime: String, repeatRule: String) {
        viewModelScope.launch {
            dao.insertReminder(CustomReminderEntity(title = title, dateTimeText = dateTime, repeatRule = repeatRule))
        }
    }

    fun toggleReminder(id: Long, enabled: Boolean) {
        viewModelScope.launch { dao.toggleReminder(id, enabled) }
    }
}
