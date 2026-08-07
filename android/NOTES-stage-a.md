# Glucolog Android — заметки к Этапу A

Файл для рабочих заметок по ходу реализации Этапа A. Ссылка добавлена в `android/PLAN.md`.

## Версии зависимостей (проверены по maven-metadata)

| Компонент | Версия | Комментарий |
|---|---|---|
| KSP | `2.2.20-2.0.4` | последний стабильный для Kotlin 2.2.20 |
| Room | `2.8.4` | классический пакет `androidx.room` (не Room 3 / `androidx.room3`) |
| Navigation Compose | `2.9.8` | стабильная |
| DataStore Preferences | `1.2.1` | стабильная |
| WorkManager | `2.11.1` | не подключаем в Этапе A — нужен только для xDrip (Этап D) |
| kotlinx-serialization-json | `1.11.0` | не подключаем в Этапе A — нужен для импорта/экспорта (Этап C) |

## Решения/отклонения от веба (зафиксировать)

1. **Инсулин в один и тот же час**: в вебе `addInsulinPoint(h, bolus, 0)` при добавлении
   базального в тот же час затирает болюс (`db[date].insulin[idx].b = bolus`). Это выглядит
   как баг веба. В Android: `addBolus` меняет только болюс, `addBasal` — только базальный,
   существующее значение второй компоненты сохраняется. Поведение UI «1:1», но без потери данных.
2. **`steps`**: в вебе хранится строкой (в т.ч. `""`). В Room — `Int?` (пустая строка → `NULL`).
3. **Текстовые поля** (заметки, выводы, еда/физ/эмоции): сохранение с debounce ~500 мс
   (веб сохранял на каждый `input`; здесь это избыточно, поэтому debounce).
4. **Глюкоза**: `h` хранится как `REAL` float; дедупликация по `(date, h, source)` с допуском
   `0.001` (как в вебе). Ручной замер и xDrip-чтение в один час — разные строки (`source`).
5. **`water`**: клик по i-му стакану: если i ≤ filled → i-1, иначе i (веб: `render.js:185`).
6. **Сон**: если поля не заполнены, показываем по умолчанию `22:00` / `06:00` (как веб),
   но в БД не пишем до изменения.
7. **Тема в Этапе A**: делаем переключатель system→dark→light в меню сразу (небольшой объём),
   хотя по плану тема — Этап E. Палитра фирменная зелёная сразу.

## Задачи Этапа A (галочки по мере выполнения)

- [x] Проверить окружение (JDK 21, Gradle 8.13, SDK 36)
- [x] Версии зависимостей в `libs.versions.toml` + KSP + Room schema export
- [x] Room: entities, DAOs, AppDatabase
- [x] Domain: DayData, GlucoseSource, даты/время, Sleep, константы
- [x] DayRepository + SettingsDataStore + AppContainer
- [x] UI: тема, MainActivity, AppNavHost, DayScreen, DayViewModel
- [x] Секции экрана дня (11 шт.)
- [x] Сборка `./gradlew :app:assembleDebug`

## Примечания к реализации Этапа A

- `material-icons-core` не входит в Compose BOM 2026.04.01 — добавлен явно.
  Стрелки ‹ › в шапке — текстовые (как в вебе), т.к. `ChevronLeft/Right` живут в
  `material-icons-extended` (не тянем ради 2 иконок).
- `DayTextField` — enum в `domain/` (в черновике был в `data/DayRepository.kt`).
- Схема Room v1 экспортирована: `app/schemas/com.koftamainee.glucolog.data.db.AppDatabase/1.json`
  (5 таблиц, уникальные индексы glucose(date,h,source), insulin(date,h), meal(date,key)).
- Сборка: `BUILD SUCCESSFUL` без предупреждений (после `@OptIn(ExperimentalCoroutinesApi)` в DayViewModel).
