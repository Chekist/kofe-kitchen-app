package ru.kofenaberegu.kitchen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kofenaberegu.kitchen.data.local.ChemicalItemEntity
import ru.kofenaberegu.kitchen.data.remote.TelegramSender
import ru.kofenaberegu.kitchen.domain.formatter.TelegramMessageFormatter
import ru.kofenaberegu.kitchen.domain.model.*
import ru.kofenaberegu.kitchen.ui.KitchenViewModel
import ru.kofenaberegu.kitchen.ui.camera.StrictCameraCaptureDialog
import ru.kofenaberegu.kitchen.ui.components.ReportPreviewSheet
import ru.kofenaberegu.kitchen.worker.ReminderScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenMainApp(viewModel: KitchenViewModel) {
    var selectedScreen by remember { mutableStateOf("home") }
    val role by viewModel.currentRole.collectAsState()
    val cookName by viewModel.cookName.collectAsState()
    val pendingCount by viewModel.pendingQueueCount.collectAsState()

    var cameraRequest by remember { mutableStateOf<Triple<String, String, (String) -> Unit>?>(null) }

    if (cameraRequest != null) {
        val (title, hint, callback) = cameraRequest!!
        StrictCameraCaptureDialog(
            promptTitle = title,
            promptHint = hint,
            onPhotoCaptured = { path ->
                callback(path)
                cameraRequest = null
            },
            onDismiss = { cameraRequest = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Кофе на Берегу • Кухня", fontWeight = FontWeight.Bold)
                        Text(
                            "Роль: ${role.title} | Очередь TG: $pendingCount",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    if (selectedScreen != "home") {
                        IconButton(onClick = { selectedScreen = "home" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { selectedScreen = "settings" }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedScreen) {
                "home" -> HomeMenuScreen(
                    role = role,
                    cookName = cookName,
                    onRoleChange = { viewModel.currentRole.value = it },
                    onCookNameChange = { viewModel.cookName.value = it },
                    onNavigate = { selectedScreen = it }
                )
                "shift_accept" -> ShiftAcceptanceScreen(viewModel) { title, hint, cb ->
                    cameraRequest = Triple(title, hint, cb)
                }
                "closing" -> KitchenClosingWizardScreen(viewModel) { title, hint, cb ->
                    cameraRequest = Triple(title, hint, cb)
                }
                "write_off" -> WriteOffScreen(viewModel) { title, hint, cb ->
                    cameraRequest = Triple(title, hint, cb)
                }
                "labeling" -> LabelingScreen(viewModel) { title, hint, cb ->
                    cameraRequest = Triple(title, hint, cb)
                }
                "temperature" -> TemperatureControlScreen(viewModel) { title, hint, cb ->
                    cameraRequest = Triple(title, hint, cb)
                }
                "shortage" -> ShortageDefectScreen(viewModel) { title, hint, cb ->
                    cameraRequest = Triple(title, hint, cb)
                }
                "remarks" -> AdminRemarksScreen(viewModel) { title, hint, cb ->
                    cameraRequest = Triple(title, hint, cb)
                }
                "chemicals" -> ChemicalsAndGeneralCleaningScreen(viewModel)
                "history" -> HistoryScreen(viewModel)
                "settings" -> SettingsAndRemindersScreen(viewModel)
            }
        }
    }
}

@Composable
fun HomeMenuScreen(
    role: UserRole,
    cookName: String,
    onRoleChange: (UserRole) -> Unit,
    onCookNameChange: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            UserRole.entries.forEach { r ->
                FilterChip(
                    selected = role == r,
                    onClick = { onRoleChange(r) },
                    label = { Text(r.title) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = cookName,
            onValueChange = onCookNameChange,
            label = { Text("ФИО сотрудника на смене") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        BigMenuButton("1. Приём смены (15:30)", "Сверка отчёта и состояние зон") { onNavigate("shift_accept") }
        BigMenuButton("2. Закрытие кухни (00:00)", "7 шагов + чек-лист 10 пунктов + фотоотчёт", Color(0xFF1B5E20)) { onNavigate("closing") }
        BigMenuButton("3. Списания", "Чистая тара, фото весов, защита от дублей") { onNavigate("write_off") }
        BigMenuButton("4. Маркировка", "Этикетка вскрытия/нарезки/разморозки") { onNavigate("labeling") }
        BigMenuButton("5. Температурный контроль", "Холодильник, морозильный стол, морозильник") { onNavigate("temperature") }
        BigMenuButton("6. Дефицит и неисправности", "Срочные заявки и поломки оборудования") { onNavigate("shortage") }
        BigMenuButton("7. Замечания Администратора", "Постановка и фото-подтверждение устранения") { onNavigate("remarks") }
        BigMenuButton("8. Ген. уборка и Лист химии", "Справочник средств и назначение уборки") { onNavigate("chemicals") }
        BigMenuButton("9. История и офлайн-очередь", "Локальный архив всех отчётов") { onNavigate("history") }
    }
}

@Composable
fun BigMenuButton(title: String, subtitle: String, containerColor: Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().height(68.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ShiftAcceptanceScreen(viewModel: KitchenViewModel, openCamera: (String, String, (String) -> Unit) -> Unit) {
    var checkedPrevReport by remember { mutableStateOf(false) }
    var zonesOk by remember { mutableStateOf(true) }
    var unresolvedText by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewText by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Приём смены (15:30)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checkedPrevReport, onCheckedChange = { checkedPrevReport = it })
            Text("Прошлый отчёт закрытия сверен")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = zonesOk, onClick = { zonesOk = true }, label = { Text("Состояние: ОК") })
            FilterChip(selected = !zonesOk, onClick = { zonesOk = false }, label = { Text("Есть замечания") })
        }
        OutlinedTextField(
            value = unresolvedText,
            onValueChange = { unresolvedText = it },
            label = { Text("Неустранённые замечания") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                previewText = TelegramMessageFormatter.formatShiftAcceptance(
                    dateTime = viewModel.nowDateTime(),
                    cookName = viewModel.cookName.value,
                    status = if (zonesOk) "ОК" else "Замечания",
                    unresolved = unresolvedText
                )
            },
            enabled = checkedPrevReport && (zonesOk || unresolvedText.isNotBlank()),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text("Сформировать отчёт")
        }
    }

    previewText?.let { text ->
        ReportPreviewSheet(
            initialText = text,
            photoPaths = photos,
            photoPromptTitle = "Фото приёма смены",
            photoPromptHint = "Зафиксируйте обнаруженные замечания при приёме кухни",
            onRequestCamera = { t, h -> openCamera(t, h) { photos = photos + it } },
            onSendTelegram = { final ->
                viewModel.submitReport("SHIFT_ACCEPT", "Приём смены", final, photos, true)
                previewText = null
            },
            onSaveOffline = { final ->
                viewModel.submitReport("SHIFT_ACCEPT", "Приём смены", final, photos, false)
                previewText = null
            },
            onDismiss = { previewText = null }
        )
    }
}

@Composable
fun KitchenClosingWizardScreen(viewModel: KitchenViewModel, openCamera: (String, String, (String) -> Unit) -> Unit) {
    var stepIndex by remember { mutableStateOf(0) }
    var checklist by remember { mutableStateOf(KitchenRegulationData.defaultChecklist) }
    var writeOffInfo by remember { mutableStateOf("") }
    var t1 by remember { mutableStateOf("+3.5") }
    var t2 by remember { mutableStateOf("-19.0") }
    var t3 by remember { mutableStateOf("-20.0") }
    var issues by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentGuideIdx by remember { mutableStateOf(0) }
    var previewText by remember { mutableStateOf<String?>(null) }

    val allChecklistValid = checklist.all { it.mark != null && (it.mark != CheckMark.ISSUE || it.comment.isNotBlank()) }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LinearProgressIndicator(progress = (stepIndex + 1) / 7f, modifier = Modifier.fillMaxWidth())
        val (stepTitle, stepDesc) = KitchenRegulationData.closingSteps[stepIndex]
        Text(stepTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(stepDesc, modifier = Modifier.padding(12.dp))
        }

        if (stepIndex == 6) {
            Text("Электронный чек-лист (10 пунктов):", fontWeight = FontWeight.Bold)
            checklist.forEachIndexed { idx, item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("${item.id}. ${item.title}", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CheckMark.entries.forEach { mark ->
                                FilterChip(
                                    selected = item.mark == mark,
                                    onClick = {
                                        checklist = checklist.toMutableList().also {
                                            it[idx] = item.copy(mark = mark)
                                        }
                                    },
                                    label = { Text("${mark.symbol} ${mark.label}") }
                                )
                            }
                        }
                        if (item.mark == CheckMark.ISSUE) {
                            OutlinedTextField(
                                value = item.comment,
                                onValueChange = { c ->
                                    checklist = checklist.toMutableList().also { it[idx] = item.copy(comment = c) }
                                },
                                label = { Text("Обязательный комментарий к замечанию (!)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            OutlinedTextField(value = writeOffInfo, onValueChange = { writeOffInfo = it }, label = { Text("Списания (нет / перечень)") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = t1, onValueChange = { t1 = it }, label = { Text("Холод. стол") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = t2, onValueChange = { t2 = it }, label = { Text("Мороз. стол") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = t3, onValueChange = { t3 = it }, label = { Text("Морозильник") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = issues, onValueChange = { issues = it }, label = { Text("Дефицит и неисправности") }, modifier = Modifier.fillMaxWidth())

            val guide = KitchenRegulationData.closingPhotoGuides[currentGuideIdx % KitchenRegulationData.closingPhotoGuides.size]
            OutlinedButton(
                onClick = {
                    openCamera(guide.title, guide.hint) { path ->
                        photos = photos + path
                        currentGuideIdx++
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Фото (${photos.size}/6): ${guide.title}")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (stepIndex > 0) {
                OutlinedButton(onClick = { stepIndex-- }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text("Назад")
                }
            }
            if (stepIndex < 6) {
                Button(onClick = { stepIndex++ }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text("Шаг выполнен → Далее")
                }
            } else {
                Button(
                    onClick = {
                        val hasIssues = checklist.any { it.mark == CheckMark.ISSUE }
                        val summary = checklist.joinToString("\n") {
                            "${it.id}. [${it.mark?.symbol}] ${it.title}${if (it.comment.isNotBlank()) " — ${it.comment}" else ""}"
                        }
                        previewText = TelegramMessageFormatter.formatKitchenClosing(
                            dateTime = viewModel.nowDateTime(),
                            cookName = viewModel.cookName.value,
                            cleaningStatus = if (hasIssues) "Замечания" else "Да",
                            writeOffsInfo = writeOffInfo,
                            tFridge = t1,
                            tFreezerTable = t2,
                            tFreezer = t3,
                            issues = issues,
                            checklistSummary = summary
                        )
                    },
                    enabled = allChecklistValid && photos.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text("Сформировать отчёт")
                }
            }
        }
    }

    previewText?.let { text ->
        ReportPreviewSheet(
            initialText = text,
            photoPaths = photos,
            photoPromptTitle = "Доп. фото закрытия",
            photoPromptHint = "Снимите чистые столы, грили, мойку или холодильник",
            onRequestCamera = { t, h -> openCamera(t, h) { photos = photos + it } },
            onSendTelegram = { final ->
                viewModel.submitReport("CLOSING", "Закрытие кухни", final, photos, true)
                previewText = null
            },
            onSaveOffline = { final ->
                viewModel.submitReport("CLOSING", "Закрытие кухни", final, photos, false)
                previewText = null
            },
            onDismiss = { previewText = null }
        )
    }
}

@Composable
fun WriteOffScreen(viewModel: KitchenViewModel, openCamera: (String, String, (String) -> Unit) -> Unit) {
    val context = LocalContext.current
    var product by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var cleanTareConfirmed by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf(WriteOffReason.EXPIRED) }
    var reasonExplanation by remember { mutableStateOf("") }
    var inAccounting by remember { mutableStateOf(true) }
    var duplicateError by remember { mutableStateOf(false) }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(product) {
        if (product.isNotBlank()) {
            ReminderScheduler.scheduleEventTrigger(
                context,
                "unfinished_writeoff",
                15,
                "Завершите оформление списания!",
                "Начато списание «$product», но отчёт ещё не отправлен."
            )
        }
    }

    LaunchedEffect(product) {
        duplicateError = product.isNotBlank() && viewModel.isProductAlreadyWrittenOffToday(product)
    }

    val isReasonValid = !selectedReason.requiresExplanation || reasonExplanation.isNotBlank()
    val canSubmit = product.isNotBlank() && amount.isNotBlank() && cleanTareConfirmed && isReasonValid && !duplicateError

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Списание кухни", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = product, onValueChange = { product = it }, label = { Text("Наименование продукта") }, modifier = Modifier.fillMaxWidth())
        if (duplicateError) {
            Text("ОШИБКА: Повторное списание этого продукта за текущую смену запрещено регламентом!", color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Вес / кол-во (г / кг / шт)") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = cleanTareConfirmed, onCheckedChange = { cleanTareConfirmed = it })
            Text("Взвешено в отдельной чистой таре (тара обнулена)")
        }

        Text("Причина списания:", fontWeight = FontWeight.Bold)
        WriteOffReason.entries.forEach { reason ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason })
                Text(reason.title)
            }
        }
        if (selectedReason.requiresExplanation) {
            OutlinedTextField(
                value = reasonExplanation,
                onValueChange = { reasonExplanation = it },
                label = { Text("Обязательное пояснение к остатку (партия / время)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = inAccounting, onCheckedChange = { inAccounting = it })
            Text("Внесено в журнал/таблицу учёта")
        }

        OutlinedButton(
            onClick = {
                openCamera("Фото весов и маркировки", "На фото должны быть видны продукт, показания весов и читаемая дата маркировки") {
                    photos = photos + it
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Снять весы и маркировку (${photos.size})")
        }

        Button(
            onClick = {
                val fullReason = if (reasonExplanation.isNotBlank()) "${selectedReason.title} ($reasonExplanation)" else selectedReason.title
                previewText = TelegramMessageFormatter.formatWriteOff(
                    dateTime = viewModel.nowDateTime(),
                    product = product,
                    amount = amount,
                    reason = fullReason,
                    cookName = viewModel.cookName.value,
                    inAccounting = inAccounting
                )
            },
            enabled = canSubmit && photos.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text("Предпросмотр списания")
        }
    }

    previewText?.let { text ->
        ReportPreviewSheet(
            initialText = text,
            photoPaths = photos,
            photoPromptTitle = "Фото весов и маркировки",
            photoPromptHint = "Снимите продукт на весах до удаления отходов",
            onRequestCamera = { t, h -> openCamera(t, h) { photos = photos + it } },
            onSendTelegram = { final ->
                val fullReason = if (reasonExplanation.isNotBlank()) "${selectedReason.title} ($reasonExplanation)" else selectedReason.title
                viewModel.registerWriteOffRecord(product, amount, fullReason)
                viewModel.submitReport("WRITE_OFF", "Списание: $product", final, photos, true)
                ReminderScheduler.cancelEventTrigger(context, "unfinished_writeoff")
                previewText = null
            },
            onSaveOffline = { final ->
                val fullReason = if (reasonExplanation.isNotBlank()) "${selectedReason.title} ($reasonExplanation)" else selectedReason.title
                viewModel.registerWriteOffRecord(product, amount, fullReason)
                viewModel.submitReport("WRITE_OFF", "Списание: $product", final, photos, false)
                ReminderScheduler.cancelEventTrigger(context, "unfinished_writeoff")
                previewText = null
            },
            onDismiss = { previewText = null }
        )
    }
}

@Composable
fun LabelingScreen(viewModel: KitchenViewModel, openCamera: (String, String, (String) -> Unit) -> Unit) {
    val context = LocalContext.current
    var product by remember { mutableStateOf("") }
    var operation by remember { mutableStateOf(LabelingOperation.OPENED) }
    var opTime by remember { mutableStateOf(viewModel.nowDateTime()) }
    var useUntil by remember { mutableStateOf("") }
    var storage by remember { mutableStateOf("+2...+6°C") }
    var batch by remember { mutableStateOf("") }
    var defrostCheckHours by remember { mutableStateOf("4") }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewText by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Маркировка продукта", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = product, onValueChange = { product = it }, label = { Text("Наименование продукта / заготовки") }, modifier = Modifier.fillMaxWidth())

        Text("Операция:", fontWeight = FontWeight.Bold)
        LabelingOperation.entries.forEach { op ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = operation == op, onClick = { operation = op })
                Text(op.title)
            }
        }

        if (operation == LabelingOperation.DEFROST_START) {
            OutlinedTextField(
                value = defrostCheckHours,
                onValueChange = { defrostCheckHours = it },
                label = { Text("Напомнить проверить разморозку через (часов)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(value = opTime, onValueChange = { opTime = it }, label = { Text("Время операции") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = useUntil, onValueChange = { useUntil = it }, label = { Text("Использовать до (дата и время по этикетке)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = storage, onValueChange = { storage = it }, label = { Text("Условия хранения (t°)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = batch, onValueChange = { batch = it }, label = { Text("Связь с партией / заводской упаковкой") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                if (operation == LabelingOperation.DEFROST_START) {
                    val hrs = defrostCheckHours.toLongOrNull() ?: 4L
                    ReminderScheduler.scheduleEventTrigger(
                        context,
                        "defrost_${product.hashCode()}",
                        hrs * 60,
                        "Проверка разморозки: $product",
                        "Проверьте окончание размораживания и срок годности «$product»"
                    )
                }
                previewText = TelegramMessageFormatter.formatLabeling(
                    product = product,
                    operation = operation.title,
                    operationTime = opTime,
                    useUntil = useUntil,
                    storageConditions = storage,
                    cookName = viewModel.cookName.value,
                    batchInfo = batch
                )
            },
            enabled = product.isNotBlank() && useUntil.isNotBlank() && batch.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text("Сформировать этикетку и отчёт")
        }
    }

    previewText?.let { text ->
        ReportPreviewSheet(
            initialText = text,
            photoPaths = photos,
            photoPromptTitle = "Крупный план маркировки",
            photoPromptHint = "Снимите наклеенную этикетку так, чтобы читались дата и партия",
            onRequestCamera = { t, h -> openCamera(t, h) { photos = photos + it } },
            onSendTelegram = { final ->
                viewModel.submitReport("LABELING", "Маркировка: $product", final, photos, true)
                previewText = null
            },
            onSaveOffline = { final ->
                viewModel.submitReport("LABELING", "Маркировка: $product", final, photos, false)
                previewText = null
            },
            onDismiss = { previewText = null }
        )
    }
}

@Composable
fun TemperatureControlScreen(viewModel: KitchenViewModel, openCamera: (String, String, (String) -> Unit) -> Unit) {
    var fridgeT by remember { mutableStateOf("") }
    var freezerTableT by remember { mutableStateOf("") }
    var freezerT by remember { mutableStateOf("") }
    var measures by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewText by remember { mutableStateOf<String?>(null) }

    val fVal = fridgeT.replace(',', '.').toDoubleOrNull()
    val ftVal = freezerTableT.replace(',', '.').toDoubleOrNull()
    val frzVal = freezerT.replace(',', '.').toDoubleOrNull()

    val hasDeviation = (fVal != null && (fVal < KitchenRegulationData.FRIDGE_MIN || fVal > KitchenRegulationData.FRIDGE_MAX)) ||
            (ftVal != null && ftVal > KitchenRegulationData.FREEZER_MAX) ||
            (frzVal != null && frzVal > KitchenRegulationData.FREEZER_MAX)

    val canSend = fVal != null && ftVal != null && frzVal != null && (!hasDeviation || measures.isNotBlank())

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Температурный контроль", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = fridgeT, onValueChange = { fridgeT = it }, label = { Text("Холодильный стол, °C (норма 0..+6)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = freezerTableT, onValueChange = { freezerTableT = it }, label = { Text("Морозильный стол, °C (норма ≤ -18)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = freezerT, onValueChange = { freezerT = it }, label = { Text("Отдельный морозильник, °C (норма ≤ -18)") }, modifier = Modifier.fillMaxWidth())

        if (hasDeviation) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    "ВНИМАНИЕ: Обнаружено отклонение от нормы! Отправка заблокирована без указания принятых мер (перенос продуктов, уведомление администратора).",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            OutlinedTextField(
                value = measures,
                onValueChange = { measures = it },
                label = { Text("Принятые меры (обязательно)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = {
                previewText = TelegramMessageFormatter.formatTemperature(
                    dateTime = viewModel.nowDateTime(),
                    fridgeTemp = fridgeT,
                    freezerTableTemp = freezerTableT,
                    freezerTemp = freezerT,
                    hasDeviations = hasDeviation,
                    measures = measures
                )
            },
            enabled = canSend,
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text("Сформировать отчёт температур")
        }
    }

    previewText?.let { text ->
        ReportPreviewSheet(
            initialText = text,
            photoPaths = photos,
            photoPromptTitle = "Фото показаний термометра",
            photoPromptHint = "Снимите индикатор температуры крупным планом",
            onRequestCamera = { t, h -> openCamera(t, h) { photos = photos + it } },
            onSendTelegram = { final ->
                viewModel.submitReport("TEMPERATURE", "Температурный контроль", final, photos, true)
                previewText = null
            },
            onSaveOffline = { final ->
                viewModel.submitReport("TEMPERATURE", "Температурный контроль", final, photos, false)
                previewText = null
            },
            onDismiss = { previewText = null }
        )
    }
}

@Composable
fun ShortageDefectScreen(viewModel: KitchenViewModel, openCamera: (String, String, (String) -> Unit) -> Unit) {
    var type by remember { mutableStateOf("ДЕФИЦИТ") }
    var item by remember { mutableStateOf("") }
    var remaining by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Высокая") }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewText by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Дефицит и неисправности", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = type == "ДЕФИЦИТ", onClick = { type = "ДЕФИЦИТ" }, label = { Text("Дефицит продукта/средства") })
            FilterChip(selected = type == "ПОЛОМКА", onClick = { type = "ПОЛОМКА" }, label = { Text("Неисправность") })
        }
        OutlinedTextField(value = item, onValueChange = { item = it }, label = { Text("Наименование позиции / прибора") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = remaining, onValueChange = { remaining = it }, label = { Text("Остаток (с единицей) / суть поломки") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = urgency == "Высокая", onClick = { urgency = "Высокая" }, label = { Text("Срочность: Высокая") })
            FilterChip(selected = urgency == "Средняя", onClick = { urgency = "Средняя" }, label = { Text("Срочность: Средняя") })
        }
        Button(
            onClick = {
                previewText = TelegramMessageFormatter.formatShortageOrDefect(type, item, remaining, urgency)
            },
            enabled = item.isNotBlank() && remaining.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text("Сформировать заявку")
        }
    }

    previewText?.let { text ->
        ReportPreviewSheet(
            initialText = text,
            photoPaths = photos,
            photoPromptTitle = "Фото дефицита / поломки",
            photoPromptHint = "Снимите остаток товара или неисправный узел",
            onRequestCamera = { t, h -> openCamera(t, h) { photos = photos + it } },
            onSendTelegram = { final ->
                viewModel.submitReport("SHORTAGE", "$type: $item", final, photos, true)
                previewText = null
            },
            onSaveOffline = { final ->
                viewModel.submitReport("SHORTAGE", "$type: $item", final, photos, false)
                previewText = null
            },
            onDismiss = { previewText = null }
        )
    }
}

@Composable
fun AdminRemarksScreen(viewModel: KitchenViewModel, openCamera: (String, String, (String) -> Unit) -> Unit) {
    val role by viewModel.currentRole.collectAsState()
    val remarks by viewModel.remarks.collectAsState()
    var text by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf("Вечерний повар") }
    var deadline by remember { mutableStateOf("Сегодня до 23:30") }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Замечания Администратора", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (role != UserRole.COOK) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Новое замечание (Администратор)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Текст замечания") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = assignee, onValueChange = { assignee = it }, label = { Text("Исполнитель") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Срок устранения") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            viewModel.addRemark(text, assignee, deadline, "")
                            val msg = TelegramMessageFormatter.formatNewRemark(text, assignee, deadline)
                            viewModel.submitReport("REMARK_NEW", "Новое замечание", msg, emptyList(), true)
                            text = ""
                        },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Поставить задачу и отправить в TG")
                    }
                }
            }
        }

        remarks.forEach { r ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(r.text, fontWeight = FontWeight.Bold)
                    Text("Исполнитель: ${r.assignee} | Срок: ${r.deadline}")
                    Text(if (r.isResolved) "Статус: УСТРАНЕНО ✓" else "Статус: В РАБОТЕ (!)")
                    if (!r.isResolved) {
                        Button(
                            onClick = {
                                openCamera("Фото-подтверждение устранения", "Снимите устранённое замечание") { photoPath ->
                                    viewModel.resolveRemark(r.id, photoPath)
                                    val msg = TelegramMessageFormatter.formatRemarkResolved(r.text, viewModel.cookName.value)
                                    viewModel.submitReport("REMARK_DONE", "Устранено замечание", msg, listOf(photoPath), true)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Устранил (снять фото-подтверждение)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChemicalsAndGeneralCleaningScreen(viewModel: KitchenViewModel) {
    val role by viewModel.currentRole.collectAsState()
    val chemicals by viewModel.chemicals.collectAsState()

    var genDate by remember { mutableStateOf("") }
    var genAssignee by remember { mutableStateOf("") }

    var chemCategory by remember { mutableStateOf("Моющее для пищевых поверхностей") }
    var chemName by remember { mutableStateOf("") }
    var chemConc by remember { mutableStateOf("") }
    var chemExp by remember { mutableStateOf("") }
    var chemRinse by remember { mutableStateOf("Обязательно проточной водой") }
    var chemPpe by remember { mutableStateOf("Перчатки") }
    var chemLink by remember { mutableStateOf("Папка инструкций на кухне") }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Генеральная уборка и Лист средств", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (role == UserRole.ADMIN) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Назначить генеральную уборку (Админ)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = genDate, onValueChange = { genDate = it }, label = { Text("Дата ген. уборки") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = genAssignee, onValueChange = { genAssignee = it }, label = { Text("Ответственный") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            val msg = TelegramMessageFormatter.formatGeneralCleaning(genDate, genAssignee)
                            viewModel.submitReport("GEN_CLEAN", "Генеральная уборка", msg, emptyList(), true)
                        },
                        enabled = genDate.isNotBlank() && genAssignee.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Назначить и уведомить в TG")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Добавить средство в Лист химии (строго по инструкции производителя)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = chemCategory, onValueChange = { chemCategory = it }, label = { Text("Назначение") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chemName, onValueChange = { chemName = it }, label = { Text("Название средства") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chemConc, onValueChange = { chemConc = it }, label = { Text("Разведение / концентрация") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chemExp, onValueChange = { chemExp = it }, label = { Text("Время выдержки") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chemRinse, onValueChange = { chemRinse = it }, label = { Text("Режим смывания") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chemPpe, onValueChange = { chemPpe = it }, label = { Text("Средства защиты") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chemLink, onValueChange = { chemLink = it }, label = { Text("Место хранения инструкции") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            viewModel.addChemical(
                                ChemicalItemEntity(
                                    category = chemCategory,
                                    name = chemName,
                                    concentration = chemConc,
                                    exposureTime = chemExp,
                                    rinsingRequired = chemRinse,
                                    ppe = chemPpe,
                                    instructionLocation = chemLink
                                )
                            )
                            chemName = ""
                            chemConc = ""
                            chemExp = ""
                        },
                        enabled = chemName.isNotBlank() && chemConc.isNotBlank() && chemExp.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Сохранить средство в справочник")
                    }
                }
            }
        } else {
            Text("Заполнение и изменение Листа средств доступно только Администратору.", style = MaterialTheme.typography.bodySmall)
        }

        Text("Утверждённый лист химических средств:", fontWeight = FontWeight.Bold)
        if (chemicals.isEmpty()) {
            Text("Список пока пуст. Администратор должен внести средства до их применения.")
        }
        chemicals.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${item.name} (${item.category})", fontWeight = FontWeight.Bold)
                    Text("Концентрация: ${item.concentration} | Выдержка: ${item.exposureTime}")
                    Text("Смывание: ${item.rinsingRequired} | СИЗ: ${item.ppe}")
                    Text("Инструкция: ${item.instructionLocation}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: KitchenViewModel) {
    val history by viewModel.history.collectAsState()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(history) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${item.title} • [${item.status}]", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(item.messageText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SettingsAndRemindersScreen(viewModel: KitchenViewModel) {
    val context = LocalContext.current
    val role by viewModel.currentRole.collectAsState()
    val reminders by viewModel.reminders.collectAsState()

    var token by remember { mutableStateOf(TelegramSender.getBotToken(context)) }
    var chatId by remember { mutableStateOf(TelegramSender.getChatId(context)) }
    val (qStartInit, qEndInit) = TelegramSender.getQuietHours(context)
    var qStart by remember { mutableStateOf(qStartInit) }
    var qEnd by remember { mutableStateOf(qEndInit) }

    var remTitle by remember { mutableStateOf("") }
    var remTime by remember { mutableStateOf("") }
    var remRepeat by remember { mutableStateOf("Ежедневно") }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Настройки Telegram и уведомлений", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("Telegram Bot Token") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = chatId, onValueChange = { chatId = it }, label = { Text("Telegram Chat ID рабочей группы") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = qStart, onValueChange = { qStart = it }, label = { Text("Тихие часы с") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = qEnd, onValueChange = { qEnd = it }, label = { Text("до") }, modifier = Modifier.weight(1f))
        }
        Button(
            onClick = { TelegramSender.saveSettings(context, token, chatId, qStart, qEnd) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить настройки интеграции")
        }

        if (role == UserRole.ADMIN) {
            Divider()
            Text("Кастомные напоминания (Администратор)", fontWeight = FontWeight.Bold)
            OutlinedTextField(value = remTitle, onValueChange = { remTitle = it }, label = { Text("Текст напоминания") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = remTime, onValueChange = { remTime = it }, label = { Text("Дата и время") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = remRepeat, onValueChange = { remRepeat = it }, label = { Text("Повтор") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    viewModel.addCustomReminder(remTitle, remTime, remRepeat)
                    remTitle = ""
                },
                enabled = remTitle.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Добавить напоминание")
            }
        }

        reminders.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(r.title, fontWeight = FontWeight.Bold)
                    Text("${r.dateTimeText} (${r.repeatRule})", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = r.isEnabled, onCheckedChange = { viewModel.toggleReminder(r.id, it) })
            }
        }
    }
}
