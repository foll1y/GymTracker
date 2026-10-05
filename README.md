# GymTracker Android App

Офлайн-приложение для записи тренировок в спортзале на стеке Jetpack Compose, Room, Hilt, Material 3.

## Способ 1: Автоматическая сборка APK через GitHub Actions (без Android Studio)
1. Создайте новый репозиторий на GitHub (например, `gym-tracker`).
2. Распакуйте содержимое этого архива в корень репозитория и выполните:
   ```bash
   git add .
   git commit -m "Initial commit GymTracker"
   git push origin main
   ```
3. Перейдите во вкладку **Actions** в репозитории на GitHub.
4. Процесс сборки **Build Android APK** запустится автоматически.
5. После завершения (2-3 минуты) в секции **Artifacts** появится готовый файл `gym-tracker-debug-apk.zip` с `app-debug.apk`.

## Способ 2: Запуск в Android Studio
1. Откройте Android Studio -> **Open...** -> выберите распакованную папку.
2. Дождитесь завершения Gradle Sync.
3. Нажмите **Run** (зеленый треугольник) для запуска на эмуляторе или телефоне.
4. Для генерации APK выберите меню **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
