package ru.kofenaberegu.kitchen.domain.formatter

object TelegramMessageFormatter {

    fun formatShiftAcceptance(
        dateTime: String,
        cookName: String,
        status: String,
        unresolved: String
    ): String = listOf(
        "ПРИЁМ СМЕНЫ — $dateTime.",
        "Повар: $cookName.",
        "Состояние кухни: $status.",
        "Неустранённые замечания: ${unresolved.ifBlank { "Нет" }}."
    ).joinToString("\n")

    fun formatKitchenClosing(
        dateTime: String,
        cookName: String,
        cleaningStatus: String,
        writeOffsInfo: String,
        tFridge: String,
        tFreezerTable: String,
        tFreezer: String,
        issues: String,
        checklistSummary: String
    ): String = listOf(
        "ЗАКРЫТИЕ КУХНИ — $dateTime.",
        "Повар: $cookName.",
        "Уборка: $cleaningStatus.",
        "Списания: ${writeOffsInfo.ifBlank { "Нет" }}.",
        "Температуры: $tFridge°C / $tFreezerTable°C / $tFreezer°C.",
        "Дефицит/Неисправности: ${issues.ifBlank { "Нет" }}.",
        "Чек-лист:",
        checklistSummary,
        "Фото приложены.",
        "",
        "Администратор: проверил ______ дата/время ______.",
        "Замечание ______ Исполнитель ______ Срок ______.",
        "Устранено ______ Повторное фото/проверка ______."
    ).joinToString("\n")

    fun formatWriteOff(
        dateTime: String,
        product: String,
        amount: String,
        reason: String,
        cookName: String,
        inAccounting: Boolean
    ): String = listOf(
        "СПИСАНИЕ КУХНИ. $dateTime.",
        "Продукт: $product.",
        "Кол-во: $amount.",
        "Причина: $reason.",
        "Повар: $cookName.",
        "Фото весов и маркировки приложены.",
        "В учёт внесено: ${if (inAccounting) "Да" else "Нет"}."
    ).joinToString("\n")

    fun formatLabeling(
        product: String,
        operation: String,
        operationTime: String,
        useUntil: String,
        storageConditions: String,
        cookName: String,
        batchInfo: String
    ): String = listOf(
        "МАРКИРОВКА КУХНИ",
        "Продукт: $product.",
        "Операция: $operation ($operationTime).",
        "Использовать до: $useUntil.",
        "Хранить при: $storageConditions.",
        "Партия/упаковка: $batchInfo.",
        "Сотрудник: $cookName."
    ).joinToString("\n")

    fun formatTemperature(
        dateTime: String,
        fridgeTemp: String,
        freezerTableTemp: String,
        freezerTemp: String,
        hasDeviations: Boolean,
        measures: String
    ): String = listOf(
        "ТЕМПЕРАТУРЫ — $dateTime.",
        "Холодильник: $fridgeTemp°C.",
        "Морозильный стол: $freezerTableTemp°C.",
        "Морозильник: $freezerTemp°C.",
        "Отклонения: ${if (hasDeviations) "Есть" else "Нет"}.",
        "Меры: ${measures.ifBlank { "Не требуются" }}."
    ).joinToString("\n")

    fun formatShortageOrDefect(
        type: String,
        item: String,
        remaining: String,
        urgency: String
    ): String = listOf(
        "$type — $item.",
        "Остаток / описание: $remaining.",
        "Срочность: $urgency.",
        "Фото приложено."
    ).joinToString("\n")

    fun formatNewRemark(text: String, assignee: String, deadline: String): String = listOf(
        "НОВОЕ ЗАМЕЧАНИЕ: $text.",
        "Исполнитель: $assignee.",
        "Срок: $deadline."
    ).joinToString("\n")

    fun formatRemarkResolved(text: String, cookName: String): String = listOf(
        "ЗАМЕЧАНИЕ УСТРАНЕНО: $text.",
        "Исполнитель: $cookName.",
        "Фото-подтверждение приложено."
    ).joinToString("\n")

    fun formatGeneralCleaning(date: String, assignee: String): String = listOf(
        "ГЕНЕРАЛЬНАЯ УБОРКА назначена на $date.",
        "Ответственный: $assignee."
    ).joinToString("\n")
}
