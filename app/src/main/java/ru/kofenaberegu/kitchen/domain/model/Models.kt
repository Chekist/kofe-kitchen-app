package ru.kofenaberegu.kitchen.domain.model

enum class UserRole(val title: String) {
    COOK("Повар"),
    ADMIN("Администратор"),
    MANAGER("Руководитель")
}

enum class CheckMark(val symbol: String, val label: String) {
    OK("✓", "Выполнено"),
    ISSUE("!", "Есть замечание"),
    NA("Н/П", "Не применимо")
}

data class ClosingChecklistItem(
    val id: Int,
    val title: String,
    val mark: CheckMark? = null,
    val comment: String = ""
)

enum class WriteOffReason(val title: String, val requiresExplanation: Boolean = false) {
    EXPIRED("Истёк срок годности"),
    TEMP_VIOLATION("Нарушение температуры"),
    SPOILAGE("Порча продукта"),
    PREP_DEFECT("Брак приготовления"),
    PACKAGE_DAMAGE("Повреждение упаковки"),
    LEFTOVER("Остаток приготовленной партии по истечении срока", requiresExplanation = true)
}

enum class LabelingOperation(val title: String) {
    OPENED("Вскрыто"),
    SLICED("Нарезано"),
    PREPARED("Приготовлено"),
    DEFROST_START("Начало размораживания"),
    DEFROST_END("Окончание размораживания")
}

data class PhotoGuide(
    val id: String,
    val title: String,
    val hint: String
)

object KitchenRegulationData {
    val defaultChecklist = listOf(
        ClosingChecklistItem(1, "Продукты проверены по срокам, закрыты и размещены на хранение"),
        ClosingChecklistItem(2, "Внутренняя маркировка читается; связь с упаковкой сохранена"),
        ClosingChecklistItem(3, "Списания взвешены, фото отправлены, запись в учёте сделана"),
        ClosingChecklistItem(4, "Столы, доски, ножи, щипцы и гастроёмкости очищены"),
        ClosingChecklistItem(5, "Грили (прижимной и роликовый) пустые, очищены и выключены"),
        ClosingChecklistItem(6, "Фритюр проверен и безопасно закрыт; запись о масле сделана"),
        ClosingChecklistItem(7, "Мойка, слив, смеситель и посудомоечная машина очищены"),
        ClosingChecklistItem(8, "Пол и мусорные ёмкости убраны; инвентарь хранится отдельно"),
        ClosingChecklistItem(9, "Холодильники работают, температуры записаны, двери закрыты"),
        ClosingChecklistItem(10, "О дефиците и неисправностях сообщено; фотоотчёт отправлен")
    )

    val closingSteps = listOf(
        "1. Защита продуктов" to "Завершить заказы. Убрать пищу и упаковку из зоны химии. Отделить продукцию на списание с пометкой «Не использовать».",
        "2. Списания" to "Взвесить на чистой таре, сфотографировать до утилизации, внести в учёт и отправить в Telegram.",
        "3. Подготовка оборудования" to "Выключить прижимной и роликовый грили, фритюр. Дать поверхностям и маслу остыть.",
        "4. Очистка и обработка" to "Удалить крошки и жир. Вымыть и продезинфицировать по листу химии с соблюдением времени выдержки и смывания.",
        "5. Инвентарь и посуда" to "Высушить и убрать гастроёмкости, ножи, доски и щипцы. Повреждённый инвентарь передать администратору.",
        "6. Уборка помещения" to "Очистить мойку и слив, убрать тряпки с бортов, вынести мусор, вымыть пол последним, убрать лужи.",
        "7. Проверка и отчёт" to "Заполнить чек-лист (10 пунктов), зафиксировать температуры, снять обязательные фото и отправить отчёт."
    )

    val closingPhotoGuides = listOf(
        PhotoGuide("tables", "1. Рабочие столы и фасады", "Снимите общий вид столов: нет соусов, крошек, воды на полу"),
        PhotoGuide("grills", "2. Пустые грили и фритюр", "Снимите пустые очищенные роликовый/прижимной грили и закрытый фритюр"),
        PhotoGuide("sink", "3. Мойка и соседний стол", "Снимите чистую чашу, слив без остатков и борта без тряпок"),
        PhotoGuide("fridge_table", "4. Отсеки холодильного стола", "Снимите закрытые гастроёмкости с читаемой маркировкой"),
        PhotoGuide("freezer", "5. Морозильник и морозильный стол", "Снимите закрытые упаковки и отсутствие наледи"),
        PhotoGuide("floor_waste", "6. Пол и место отходов", "Снимите чистый пол и обработанный мусорный контейнер"),
        PhotoGuide("closeup", "7. Крупный план маркировки / весов", "Снимите этикетку или показания термометра крупным планом")
    )

    const val FRIDGE_MIN = 0.0
    const val FRIDGE_MAX = 6.0
    const val FREEZER_MAX = -18.0
}
