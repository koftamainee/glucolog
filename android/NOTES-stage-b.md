# Glucolog Android — заметки к Этапу B

График дня и статистика. Ссылка добавлена в `android/PLAN.md`.

## Переносы из веба

- `chart.js` → `ui/chart/GlucoseChart.kt` (Compose Canvas). Константы и функции масштаба:
  `MIN_G=1`, `MAX_G=17.5`, `RANGE_LO=4`, `RANGE_HI=8`, `MAX_INS=MAX_G`;
  `toX(h)=h/24*W`, `toY(g)=H-(g-MIN_G)/(MAX_G-MIN_G)*H`, `toYIns(v)=H-v/MAX_INS*H`.
- `render.js:41-80` `glucoseStats` → `domain/Stats.kt` (`statsOf`, `fmt1`). PopSD, тренд ±0.3.
- Легенда и подписи X — порт `.chart-legend` / `.chart-xlabels` из `web/css/chart.css`
  (0:00…24:00).

## Отклонения от веба (по PLAN §2.2)

1. Линия глюкозы рисуется **только по xDrip**; ручные замеры — отдельные точки.
   В вебе линия идёт по всем точкам и все подписаны. Здесь: на xDrip-точках подписи НЕТ,
   на ручных — всегда есть (значение). Crosshair тоже новый (в вебе его нет).
2. Статистика считается по всем показаниям дня (любой `source`) — как и в вебе.
   Тренд сравнивает среднее дня со средним вчера (`glucoseDao.observeAvg`).

## Реализация

- `domain/ChartModel.kt`: `ChartModel.from(DayData)` → серии `line` (xdrip), `manual`,
  `bolus`, `basal` (все отсортированы по `h`).
- `domain/Stats.kt`: `statsOf(points, prevAvg)` → `DayStats?` (null при пустом дне).
- `ui/day/DayViewModel.kt`: добавлен `prevAvg` (Flow среднего вчерашней глюкозы).
- `ui/day/DayScreen.kt`: `statsOf(day.glucose.map{g}, prevAvg)` → `GlucoseSection`.
- `ui/day/sections/GlucoseSection.kt`: чарт + легенда + подписи X + строка статистики
  (n, ↓min ↑max, Øavg, SD, ↓hypo, ↑hyper, тренд ↑/↓/→) + форма ввода (было).
- Цвета чарта: светлая/тёмная из `web/css/variables.css` (см. `ui/theme/Color.kt`).
  Crosshair-тултип: чёрный фон 70%, текст темы.

## Особенности

- Порядок отрисовки как в вебе: болюс → базальный → линия глюкозы → ручные точки.
- Подписи оси Y: 2,4,…,16 (в Kotlin `(2..17 step 2)` даёт 2..16 — совпадает с вебом).
- Вода/остальное не менялись.

## Задачи Этапа B

- [x] `Stats.kt` (n/min/max/avg/SD/hypo/hyper/тренд)
- [x] `ChartModel.kt` (линия xdrip / ручные точки / болюс / базальный)
- [x] `GlucoseChart` (сетка, полоса 4–8, оси, линия, точки, crosshair)
- [x] Интеграция в DayScreen + легенда + статистика
- [x] Сборка `./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`
- [ ] Проверка совпадения с вебом — на устройстве пользователем

## Правки по отзывам пользователя (этап B, раунд 2)

- **Корень бага «ввод не работает» + «голод не меняется»:** `MealDao`/`InsulinDao`
  использовали `@Insert(IGNORE)`, а `DayRepository.setMealField`/`setInsulinType`
  всегда вызывали `insert(current.copy(...))` — при существующей строке (autogen id > 0)
  апдейт молча отбрасывался. Исправлено: добавлен `@Upsert`, репозиторий пишет через
  `upsert` (первое поле в карточке еды сохранялось, остальные — нет; инсулин на
  существующий час тоже не обновлялся).
- **Время:** новый компонент `ui/components/TimeField.kt` — readOnly поле + M3
  `TimePickerDialog` (24h). Применён к глюкозе, болюсу, базальному, времени приёма пищи,
  «Лёг»/«Встал`. Формат записи `HH:MM` — совместим с `timeToFloat`/`calcSleepDuration`.
- **Вода:** 8 кружков 36dp в один Row не помещались (последний «сжат») → `FlowRow`
  `maxItemsInEachRow=4`, кружки 44dp.
- **Стресс:** повторный тап на выбранную опцию снимает выбор (`DayViewModel.setStress(String?)`,
  null очищает поле).
- **Лейбл/кнопка:** в форме глюкозы «ммоль/л» → «ед.»; время 120→96dp; `PrimaryAddButton`
  — `maxLines=1`, убраны жёсткие `width(130.dp)`, чтобы «Добавить» не переносился
  («Добавит/ь»).
- **Счётчик гипо/гипер → зоны графика:** `Stats.kt` — вместо кол-ва точек (<4 и >10)
  считается число серий подряд идущих точек ниже/выше планки (отклонение от веба,
  где считается кол-во точек). Разрыв по времени серию НЕ дробит (по решению пользователя).
  Точки приходят уже по времени (`GlucoseDao` `ORDER BY h`). Min/max/avg/SD/тренд не менялись.
- **Поле времени (клик):** `Modifier.clickable` на `OutlinedTextField` не срабатывает
  (поле глотает клик — известный баг). `TimeField` переведён на прозрачный `Box`
  поверх поля с `matchParentSize().clickable`.
- **Ввод текста в еде:** было «лагает/не стирается» — поле без локального состояния,
  каждый символ писался в БД асинхронно (гонка корутин) и значение приходило из Room-флоу,
  сбрасывая текст/курсор. Новый `ui/components/DebouncedOutlinedTextField.kt` (локальное
  состояние + флаг dirty + запись через 500мс паузы, как в `NotesSection`); применён
  к «Приём пищи», «Физ. ощущения», «Эмоции».
- Сборка после правок — `BUILD SUCCESSFUL`.
