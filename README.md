# Кофе на Берегу — Контроль кухни (Android App)

Готовый Android-проект (Kotlin, Jetpack Compose, Room, CameraX, WorkManager, OkHttp), автоматизирующий регламент **«Правила уборки и закрытия кухни»**.

## Как получить готовый `.apk` за 2 минуты (2 способа)

### Способ 1: В облаке через GitHub Actions (без установки Android Studio)
1. Создайте новый репозиторий на GitHub и загрузите в него содержимое папки `KofeNaBereguKitchen`.
2. Перейдите во вкладку **Actions** → выберите workflow **Build Android Debug APK** → нажмите **Run workflow**.
3. Через ~2 минуты скачайте готовый `.apk` из раздела **Artifacts** внизу страницы сборки.

### Способ 2: Локально через Android Studio
1. Откройте папку `KofeNaBereguKitchen` в **Android Studio (Hedgehog / Iguana и новее)**.
2. Дождитесь окончания синхронизации Gradle.
3. В верхнем меню выберите **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Готовый файл появится по пути: `app/build/outputs/apk/debug/app-debug.apk`.
