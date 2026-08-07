# Glucolog Android — Заметки этапа D (интеграция xDrip)

## Что сделано

- **Зависимости**: `androidx.work:work-runtime-ktx:2.11.1` в `libs.versions.toml`.
  HTTP — платформенный `java.net.HttpURLConnection`, JSON — `org.json` (из платформы),
  новых зависимостей не понадобилось.
- **Манифест** (`AndroidManifest.xml`): `INTERNET`, `RECEIVE_BOOT_COMPLETED`,
  `com.eveningoutpost.dexdrip.permissions.RECEIVE_BG_ESTIMATE` (protectionLevel `normal`,
  выдан автоматически — см. PLAN §1.2); `<receiver>` на `com.eveningoutpost.dexdrip.BgEstimate`.
- **`data/xdrip/XdripBroadcastReceiver.kt`**: приём `BgEstimate`, чтение extras
  `BgEstimate` (double, мг/дл) и `Time` (epoch ms); конвертация `/ 18.016` в ммоль/л;
  дата/час по локальной таймзоне; вставка `addGlucose(..., GlucoseSource.XDRIP)` через
  `goAsync()` + `Dispatchers.IO`. `BgEstimateNoData` и прочие action игнорируются.
  После первого успешного приёма ставится флаг `xdrip_connected`.
- **`data/xdrip/XdripWebClient.kt`**: `GET http://127.0.0.1:17580/sgv.json?count=N`
  (по умолчанию 1000), таймауты 3/5 с; парсинг `sgv` (int, мг/дл) и `date` (epoch ms);
  сортировка по дате/часу; `XdripHttpException` на не-200.
- **`data/xdrip/XdripBackfillWorker.kt`**: `CoroutineWorker`, `fetchSgv(1000)`, вставка всех
  полученных чтений (дедуп через `addGlucose`); тихие фейлы (Result.success
  при ошибке); после успеха флаг `xdrip_connected`. Планируется в `GlucologApp.onCreate`
  как уникальная периодическая работа на 15 мин (`ExistingPeriodicWorkPolicy.KEEP`).
- **`data/xdrip/XdripStatusProvider.kt`**: `XdripStatus(connected, lastDate, lastTime,
  lastValue)` = `combine(observeLastXdrip(), settings.xdripConnected)`.
- **`SettingsDataStore`**: `xdripConnected` (bool).
- **DAO/репозиторий**: `GlucoseDao.observeLastXdrip()` (последний xDrip-пункт),
  `DayRepository.observeLastXdrip()`, `insertXdripReadings()` — вставляет всю выборку,
  дедупликация — существующий `addGlucose` (unique `(date, h, source)`).
- **UI**: `ui/xdrip/XdripSetupScreen.kt` (статус с датой+временем, кнопка
  «Синхронизировать» с `fetchSgv(1000)` + вставкой в БД, инструкция с реальными русскими
  названиями настроек xDrip) + `XdripSetupViewModel`. Вход — кнопка «Настройка xDrip»
  на табе «Данные» (до появления меню в этапе E); маршрут `"xdrip"` в `AppNavHost.kt`.
- **График** (`ui/chart/GlucoseChart.kt`, `domain/ChartModel.kt`): xdrip — только линия
  (без точек на каждые 5 мин); ручная глюкоза — иконка «капля» (`Icons.Filled.WaterDrop`,
  цвет `ChartManual`); приёмы пищи — иконка «тарелка» (`Icons.Filled.RestaurantMenu`,
  цвет `ChartMeal`) на уровне значения 16 по шкале глюкозы
  (нужен `time` у приёма);
  базальный — просто точка (действует ~36 ч); болюс — точка + линейная кривая убывания
  −20% от дозы в час (обнуляется за 5 ч: `dose * (1 − 0.2·Δt)`), считается на лету,
  не хранится в БД, с переходом между днями
  (для хвоста кривой подтягивается инсулин предыдущего дня — `DayData.prevInsulin`).
  Добавлена `material-icons-extended` (иконки WaterDrop/RestaurantMenu).
- **Журнал дня** (`JournalSection`): показывает только ручную глюкозу
  (`source == manual`); удаление, соответственно, доступно только для ручной.

## Решения по ходу

- HTTP и JSON сделаны без новых библиотек (HttpURLConnection + org.json) — минимум
  зависимостей, хватает функционально.
- В Worker на провале возвращаем `Result.success()` — «тихий» фейл по PLAN §D.2,
  периодическая работа просто повторится в следующий цикл.
- В `XdripStatusProvider` время последнего чтения берём из БД (date + h), а не из
  broadcast-extra — единый источник правды.
- `insertXdripReadings` вставляет все полученные чтения, а не только «новее последнего»:
  фильтр «новее» с live-broadcast отбрасывал всю более раннюю историю при бэкфилле
  (диагностировано на устройстве — чтение за 20:21 не попадало, хотя было в `sgv.json`).
  Дедуп по unique `(date, h, source)` безопасен при повторах.

## Диагностика на устройстве (TECNO, xDrip форк Egor)

- Implicit broadcast (`BgEstimate` без `setPackage`) на Android 8+ не доходит до нашего
  receiver даже в foreground. Доставка работает только с `setPackage`
  (соответствует «Установить получателя» = `com.koftamainee.glucolog` в xDrip).
- «Веб служба xDrip» стартует на `127.0.0.1:17580`, отдаёт `sgv.json?count=500`.
  Пока служба молчит (порт не слушает) — broadcast продолжает работать.
- `sgv` в ответе — мг/дл, несмотря на `units_hint: "mmol"`; конвертация `/ 18.016` верная.
- Реальные русские названия настроек (по `~/dev/xDrip`):
  «Настройки интеграций с приложениями» → «Локальная трансляция данных»
  (`broadcast_data_through_intents`), «Совместимая трансляция» (`default_compatible_broadcast`),
  «Установить получателя» (`local_broadcast_specific_package_destination`),
  «Веб служба xDrip» (порт 17580, loopback, секрет не нужен), «Открытая веб служба».
- Ограничение истории: `sgv.json` отдаёт ~последние 1000 чтений (~3.5 дня при шаге 5 мин).
  Дальше накапливается только по мере поступления (broadcast + периодический бэкфилл).
  Кап проверен эмпирически: `count=5000` вернул ровно 1000 записей
  (за 2026-08-04 09:31 … 2026-08-07 20:46). В исходниках: `WebServiceSgv.java:73`
  `count = Math.min(count, 1000)`; `BgReading.latest()` лимита не имеет.
  Объём: ~85 Б/показание (с индексом) → ~25 КБ/день, ~340 КБ за 14-дневный сенсор —
  память не ограничение.
- **Cleartext HTTP**: при targetSdk ≥ 28 Android по умолчанию запрещает `http://` даже к
  loopback. Пока `usesCleartextTraffic`/network security config не были настроены,
  «Синхронизировать» падало в catch («Нет соединения») — порт 17580 при этом слушал.
  Фикс: `res/xml/network_security_config.xml` с `cleartextTrafficPermitted="true"` для
  `127.0.0.1`/`localhost` + `android:networkSecurityConfig` в манифесте.
  Диагностика: `adb logcat` (в catch добавлен `Log.e(TAG, "sync failed", e)`) и проверка
  порта через `toybox nc` — служба отвечала, значит проблема была на стороне приложения.
- Иконки в Canvas: `iconPath(image)` строит `Path` из дерева вектора (`VectorGroup` →
  `Iterable<VectorNode>` + `VectorPath.pathData`; `List<PathNode>.toPath()`) один раз через
  `remember`, затем `drawIcon(path, color, ...)` рисует `drawPath(path, color)` внутри
  `withTransform { translate(center - size/2); scale(size/24) }` — цвет задаётся напрямую,
  `ColorFilter`/`Painter.draw` не нужны (в ui-graphics 1.11 `Painter.draw` — internal,
  а `pathNodes` у `VectorGroup` убран, вместо него `clipPathData` + `children`).
- `combine()` принимает максимум 5 потоков — 6-й (`prevInsulin`) добавляется вторым
  `combine(base, prevDayInsulin) { data, prev -> data.copy(prevInsulin = prev) }`.

## Проверка пользователем (план)

1. Установить xDrip, подключить сенсор.
2. В xDrip: «Настройки интеграций с приложениями» → включить «Локальная трансляция
   данных»; проверить «Совместимая трансляция»; «Установить получателя» =
   `com.koftamainee.glucolog`.
3. Проверить: линия на графике дня, статистика по всем показаниям, журнал без xDrip.
4. Для истории: включить «Веб служба xDrip» и нажать «Синхронизировать» на экране настройки.
