# Glucolog Android — заметки к Этапу C

Импорт/экспорт JSON и CSV. Ссылка добавлена в `android/PLAN.md`.

## Экспорт

- JSON: структура веба, но glucose-пункты `{h, g, source}` (новый формат).
  Имя файла `glucolog-YYYY-MM-DD.json`. Инсулин как в вебе — `{h, b, ba}`
  (пустые `b`/`ba` опускаются), блюда/скалярные поля — `db[date][...]`.
- CSV: колонки `Дата, Время, Тип, Значение, Детали, Источник` — колонка «Источник»
  добавлена в конец, поэтому старый веб-импорт (читает 0–4) разберёт мобильный CSV.
  Правила строк — порт `sidebar.js:175-230`; экранирование — порт `sidebar.js:215-220`.
- Числа в CSV форматируются через `Locale.US` (запятая-разделитель не ломает колонки).
- Сохранение — SAF `CreateDocument` (`application/json`, `text/csv`).

## Импорт

- Автоопределение формата: JSON — наличие `source` у glucose-пунктов; CSV — колонка
  «Источник» в заголовке. Влияет только на текст статуса; разбор идентичен.
- **Глюкоза из файла не импортируется** (решение заказчика): импортируются инсулин,
  приёмы пищи, вода, спорт, шаги, сон, стресс, стул, заметки, выводы. Строки/пункты
  `Глюкоза` в CSV и массив `glucose` в JSON пропускаются.
- JSON: `JsonCodec.import` — порт разбора `db[date][mealKey]` и скаляров (`org.json`,
  платформа, без новых зависимостей). Старый веб-формат (без `source`) разбирается так же.
- CSV: `CsvCodec.import` — порт `parseCSVLine` (`sidebar.js:307`) и `importCSV`
  (`sidebar.js:232`). `,` в числах заменяется на `.`; заголовок определяется по «Дата».
- Валидация перед записью: файл парсится целиком, понятные ошибки («Неверный JSON»,
  «Файл не похож на экспорт Glucolog», «Файл пуст»).

## Стратегии

- `DayRepository.importDays(days, replace)` — транзакция `db.withTransaction`.
- **Заменить**: `deleteAll` по `insulin/meal/day/stool`, затем запись. Глюкоза
  **не затрагивается** (в согласии с «глюкозу не импортируем»).
- **Объединить**: поле-уровневый merge — существующая строка обновляется только по
  пришедшим из файла полям (инсулин по `(date, h)`, блюда по `(date, key)`, `day` —
  copy с `?:` текущими), стул — `insert(IGNORE)`. Новые дни добавляются.
- Диалог выбора стратегии показывается только если в БД есть не-глюкозные данные
  (`hasNonGlucoseData`); иначе импорт сразу.

## Реализация

- `domain/PortableDay.kt` — модель переноса (`GlucosePoint`, `InsulinPoint`, `MealEntry`).
- `data/importexport/DayBuilder.kt` — общий аккумулятор дня для обоих кодеков
  (addInsulin по часу с merge, putMeal/setMealField, addStool дедуп).
- `data/importexport/JsonCodec.kt` — export/import/isNewFormat (org.json).
- `data/importexport/CsvCodec.kt` — export/import/isNewFormat (порт parseCSVLine/importCSV).
- `data/importexport/ImportCoordinator.kt` — выбор JSON/CSV по первому символу `{`,
  `ImportedFile(days, isNewFormat)`; снятие BOM.
- `data/importexport/FileOps.kt` — read/write текста через `ContentResolver`.
- `data/DayRepository.kt` — `allDays()` (снимок по объединению дат всех таблиц),
  `hasNonGlucoseData()`, `importDays(days, replace)`.
- DAO: добавлены `getAll`/`count`/`deleteAll` для `day/glucose/insulin/meal/stool`
  (glucose — только `getAll`, глюкоза не перезаписывается).
- `di/AppContainer.kt` — публичный `appContext`.
- `ui/importexport/ImportExportViewModel.kt` — export/import, диалог стратегии, статусы.
- `ui/importexport/ImportExportScreen.kt` — кнопки экспорта/импорта (SAF), статус, AlertDialog.
- `ui/AppNavHost.kt` — маршрут `"io"`; **нижняя навигация**: табы «День» (`day`) и «Данные»
  (`io`), `NavigationBar` в Column под NavHost (без внешнего Scaffold, чтобы не задвоить
  отступы). Переключение с `launchSingleTop/saveState/restoreState` — состояние табов
  сохраняется. Изначально вход был кнопкой «☰» в шапке `DayScreen` — убрана по отзыву
  пользователя («просто стрелочки»); полноценное меню (тема/туториал/настройки xDrip) — Этап E.

## Задачи Этапа C

- [x] JsonCodec (экспорт с source, импорт старого+нового)
- [x] CsvCodec (порт parseCSVLine/importCSV, колонка Источник)
- [x] ImportCoordinator (детект, валидация)
- [x] Репозиторий: allDays / hasNonGlucoseData / importDays (replace/merge, глюкоза нетронута)
- [x] SAF: экспорт CreateDocument, импорт OpenDocument
- [x] ImportExportScreen + ViewModel + маршрут
- [x] Сборка `./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`
- [ ] Проверка: экспорт/импорт из веба и обратно — на устройстве пользователем

## Правки по отзывам (проверено на устройстве TECNO LI6)

- Переключение табов тупило: `stateIn(SharingStarted.Eagerly)` для `dayData`/`prevAvg`
  в `DayViewModel` — данные горячие, день не пересоздаётся.
- «Белая вспышка» и «подвисание» при переключении — дефолтный crossfade NavHost
  (на тёмной теме фон окна просвечивал белым). Убрано: `enterTransition/exitTransition/
  popEnterTransition/popExitTransition = EnterTransition.None`/`ExitTransition.None`
  в `AppNavHost.kt`. Оба направления переключаются мгновенно, без анимации и вспышки.
- Нижняя панель: `containerColor = MaterialTheme.colorScheme.surface`, индикатор
  активного таба = `primaryContainer`. Проверено пиксельно: фон панели == фон экрана.
- Пустое пространство сверху: `contentWindowInsets = WindowInsets(0,0,0,0)` в
  `DayScreen` и `ImportExportScreen` (шапка на y≈128, сразу под статус-баром).

