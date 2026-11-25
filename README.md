<img height="400" alt="изображение" src="https://github.com/user-attachments/assets/13bb9c58-b5ed-4c02-9798-cee9d863ca6b" /> <img height="400" alt="изображение" src="https://github.com/user-attachments/assets/20ba4827-3a5a-4887-bf95-4e7ae139788a" /> <img height="400" alt="изображение" src="https://github.com/user-attachments/assets/4b39961b-3744-4d24-88ef-0e6c8543a397" /> <img height="400" alt="изображение" src="https://github.com/user-attachments/assets/6fbc3e01-603d-4aa4-8b9b-58cf8923a317" />



# Садхана — отчёты по личной садхане (Android)

Локальное приложение: все данные в SQLite на телефоне, без интернета и без аккаунтов.
Экспорт/импорт — в обычный JSON-файл через системный файловый менеджер.

## Файл для установки

`sadhana-2.3.apk` — подписан, minSdk 21 (Android 5.0+), targetSdk 34.
Ставится поверх любой предыдущей версии как обновление: база сохраняется.

Установка: скопировать на телефон → открыть файл → разрешить установку из этого источника
(Android спросит «Неизвестный источник» для файлового менеджера / Telegram).

## История изменений

Все версии и что в них менялось — в [CHANGELOG.md](CHANGELOG.md). Кратко: приложение прошло путь от простого списка отчётов до графиков и напоминаний.

## Что умеет

- Список отчётов по дням: дата, круги джапы, чтение (мин), 5 регулярных пунктов,
  служение, заметки; балл дня `X/7` (5 пунктов + цель по кругам + чтение).
- Сводка сверху: всего отчётов, за последние 7 дней, среднее кругов, дней с подъёмом до 5:00.
- Правка отчёта по нажатию, удаление — долгим нажатием в списке (или кнопкой в редакторе).
- Дата выбирается календарём; отчёт за уже существующую дату — по подтверждению.
- **Экспорт**: кнопка «Экспорт» → выбрать место (Загрузки, диск, облако) → файл
  `sadhana-ГГГГ-ММ-ДД.json`.
- **Импорт**: кнопка «Импорт» → выбрать файл → добавление/обновление с отчётом
  «добавлено / обновлено / пропущено».
- **Напоминание**: ⏰ в шапке → ежедневное уведомление в выбранное время (по умолчанию 21:00).
- **Графики**: 📊 в шапке → круги, регулярные пункты и чтение по дням за 7 / 30 / 90 дней,
  со сводкой (среднее, лучший день, серия подряд).
- **Очистка базы**: 🗑 в шапке → удаление всех записей с подтверждением и предложением
  сделать копию.
- **Поддержать проект**: кнопка ♥ в шапке → страница YooMoney в браузере.

## Логика импорта

- Ключ записи — дата. Запись за ту же дату обновляется.
- Если локальная запись новее импортируемой (`updated_at` больше), она **не затирается** —
  считается «пропущено».
- Импорт принимает: объект с массивом `reports`, просто массив отчётов,
  или один объект отчёта. Чужие поля игнорируются, `app` другого приложения — отказ.
- Дубли внутри файла схлопываются: побеждает запись с большим `updated_at`.

## Формат файла

```json
{
  "app": "sadhana",
  "format": 1,
  "exported_at": 1732300000000,
  "count": 1,
  "reports": [
    {
      "date": "2026-11-23",
      "rounds": 16,
      "reading_min": 30,
      "wake_early": true,
      "sleep_early": true,
      "mangala_arati": true,
      "lecture": true,
      "prasadam": true,
      "service": "уборка в храме",
      "notes": "джапа внимательная",
      "updated_at": 1732300000000
    }
  ]
}
```

## Сборка из исходников

Без Gradle: aapt2 → javac → d8 → zipalign → apksigner (JDK 17 + build-tools 34 лежат
в `toolchain/`, ставились без root).

```bash
python3 make_icon.py     # иконки mipmap-*
./build.sh               # → sadhana-1.0.apk
```

Тесты чистой логики (JSON экспорт/импорт, без Android):

```bash
export JAVA_HOME=$PWD/toolchain/jdk-17.0.21.1+2; export PATH=$JAVA_HOME/bin:$PATH
javac -encoding UTF-8 -d build/test-classes \
  src/ru/rolyc/sadhana/{Report,MiniJson,ReportJson}.java test/ReportJsonTest.java
java -cp build/test-classes ReportJsonTest    # 45 проверок
java -cp build/test-classes NumTest          # 26 проверок
java -cp build/test-classes ReminderTimeTest # 12 проверок
java -cp build/test-classes StatsDataTest    # 27 проверок
java -cp build/test-classes AxisTest         # 22 проверки

python3 preview_charts.py                    # макет графиков в PNG (PIL, для проверки геометрии)
```

## Структура

```
src/ru/rolyc/sadhana/
  MainActivity.java    список, сводка, экспорт/импорт (SAF)
  EditActivity.java    форма отчёта за день
  ReportAdapter.java   строки списка
  ReportsDb.java       SQLite: таблица reports, upsert по дате
  Report.java          модель + балл дня (чистый Java)
  ReportJson.java      формат файла (чистый Java)
  StatsData.java       ряды по дням для графиков (чистый Java)
  Axis.java            шкала графика (чистый Java)
  BarChartView.java    столбчатый график на Canvas
  StatsActivity.java   экран графиков
  MiniJson.java        мини-JSON писатель/разборщик (чистый Java)
  Num.java             разбор числовых полей (чистый Java)
  ReminderTime.java    расчёт времени напоминания (чистый Java)
  ReminderPrefs.java   настройки напоминания (SharedPreferences)
  ReminderScheduler.java  постановка/снятие будильника
  ReminderReceiver.java   срабатывание: уведомление + следующее на завтра
  BootReceiver.java    переустановка будильника после перезагрузки
  Notifications.java   канал и показ уведомления
res/                   layout, стили, иконки
test/                  JVM-тесты чистой логики (5 файлов)
build.sh, make_icon.py, preview_charts.py
CHANGELOG.md           история изменений по версиям
```

## Подпись и ключи

Keystore для подписи — `keystore/sadhana.keystore` (alias `sadhana`). **В репозиторий он не
попадает** (`.gitignore`), вместе с паролем: без того же ключа обновления поверх установленной
версии не встанут, но и публиковать его нельзя — любой получивший keystore может выпускать
подписанные от вашего имени обновления.

Пароль `build.sh` берёт из переменной окружения `KS_PASS` или из файла `keystore/.ks_pass`
(тоже в `.gitignore`). Пути можно переопределить через `KS_PATH` и `KS_ALIAS`.
