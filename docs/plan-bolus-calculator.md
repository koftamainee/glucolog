# План: Болюсный калькулятор (расчёт инсулина на пищу)

> Документ описывает реализацию функции «расчёт инсулина на еду» в Android-приложении
> `com.koftamainee.glucolog`. Функция повторяет логику таблицы `android/ХЕ.xlsx`,
> но удобнее: список еды на приём пищи, общие ХЕ и БЖЕ, автоматический учёт параметров
> (пользователь вручную вводит только **целевую глюкозу (ЦГ)** и **список еды**).

---

## 1. Общая концепция

Отдельная вкладка **«Калькулятор»** в нижней навигации (3-я, рядом с «День» и «Настройки»).
Внутри вкладки — три подвкладки (TabRow):

1. **Расчёт** — главный экран: список блюд на приём пищи, параметры, итог, кнопки
   «Вставить еду в день» и «Записать болюс».
2. **Продукты** — управление локальной базой продуктов (ручное добавление, редактирование,
   удаление) + **CSV экспорт/импорт еды** (отдельно от журнала).
3. **Рецепты** — создание составного «рецепта» из уже существующих продуктов
   (сначала локальные, затем — результаты OpenFoodFacts) с расчётом БЖУ на 100 г
   из ингредиентов.

Источник готовых продуктов извне: **OpenFoodFacts** (открытый API, без ключа).
Локальная БД стартует **пустой** (без сида из `ХЕ.xlsx`).

---

## 2. Математика (доменный слой)

Всё считается чистой функцией без Android-зависимостей → юнит-тестируется.

### 2.1. Входные данные

- БЖУ на 100 г продукта/рецепта: `kcal` (ккал), `proteins` (г), `fats` (г), `carbs` (г).
- Масса порции каждого блюда `m` (г).
- Параметры пользователя (см. §3.3):
  - `dailyDose` — суточная доза инсулина (ед.), дефолт **20**;
  - `targetGlucose` (ЦГ) — целевая глюкоза, дефолт **5.0** ммоль/л;
  - `actualGlucose` (АГ) — актуальная гликемия, авто из xdrip, ручной фолбэк;
  - `activeInsulin` (АИ) — активный инсулин, авто из журнала болюсов, ручной фолбэк.

### 2.2. Производные параметры

```
УГ   (углеводный коэффициент)  = 12 / (500 / dailyDose)
ФЧИ  (фактор чувствительности) = 100 / dailyDose
```

Проверка дефолта: `dailyDose = 20` → `УГ = 12/(500/20) = 0.48`, `ФЧИ = 100/20 = 5.0`
(совпадает со значениями P3 и P4 в `ХЕ.xlsx`).

### 2.3. Расчёт по каждому блюду (для отображения строк)

```
ХЕ   = carbs * m / 100 / 12
БЖЕ  = (kcal - carbs * 4) / 100 * m / 100        // МАСШТАБИРУЕТСЯ НА ПОРЦИЮ
```

> **Важно.** В `ХЕ.xlsx` колонка «Инс. БЖЕ» игнорирует массу порции (считает БЖЕ на 100 г,
> даже если съедено 300 г) — это недочёт таблицы. В приложении БЖЕ умножается на
> `m / 100`, т.е. корректно масштабируется на порцию.

### 2.4. Итог по приёму пищи (несколько блюд)

Сначала **суммируются** ХЕ и БЖЕ всех блюд списка:

```
ХЕ_общ   = Σ ХЕ_i
БЖЕ_общ  = Σ БЖЕ_i
```

Затем уже **один раз** применяются факторы:

```
Инс. на ХЕ   = ХЕ_общ  * УГ / ФЧИ
Инс. на БЖЕ  = БЖЕ_общ * УГ / ФЧИ
Коррекция    = (АГ - ЦГ) / ФЧИ
Итог         = Инс. на ХЕ + Инс. на БЖЕ + Коррекция - АИ
```

Все результаты округляются до **2 знаков после запятой** (при отображении — «как в калькуляторе»).

### 2.5. Что показывать пользователю

В карточке «Итог» отображаются все четыре ключевых числа (суммы по всему приёму пищи):

1. **БЖЕ** (сумма) — ед.;
2. **ХЕ** (сумма) — ед.;
3. **Инсулин на БЖЕ** — ед.;
4. **Инсулин на ХЕ** — ед.;

и дополнительно:

5. **Коррекция** `(АГ − ЦГ)/ФЧИ` — ед. (со знаком);
6. **Активный инсулин (АИ)** — ед. (вычитается);
7. **Итоговый болюс** = Инс.ХЕ + Инс.БЖЕ + Коррекция − АИ (крупно, выделено).

### 2.6. Список блюд в «Расчёте»

Для каждого блюда в списке показывается строка:

- название блюда (продукт/рецепт + источник `local`/`remote`);
- масса порции — редактируемое поле (префилл: `portionMass` продукта);
- ХЕ блюда и БЖЕ блюда (пересчитываются в реальном времени при изменении массы);
- кнопка удаления из списка.

---

## 3. Данные

### 3.1. Room: новые сущности

В `data/db/` по образцу `MealEntity.kt`/`InsulinEntity.kt`:

```kotlin
@Entity(tableName = "product")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcal: Float,       // на 100 г
    val proteins: Float,   // на 100 г
    val fats: Float,       // на 100 г
    val carbs: Float,      // на 100 г
    val portionMass: Int,  // г, префилл массы порции
    val note: String?,     // примечание
    val source: String,    // "manual" | "remote"
)

@Entity(tableName = "recipe")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "recipe_ingredient",
    indices = [Index(value = ["recipeId", "productId"])],
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val productId: Long,
    val mass: Int,         // г
)
```

`FoodDao` (или три DAO) — Room-запросы:
- продукты: `observeAll()` (Flow, сортировка по имени), `upsert`, `delete`, `observeById`;
- рецепты: `observeAll()`, `upsert`, `delete` (каскадно удаляет ингредиенты);
- ингредиенты: `observeByRecipe(recipeId)` (Flow), `insert`, `delete`, `observeByProduct(productId)` (для каскадного удаления продукта).

Расчёт БЖУ рецепта на 100 г выполняется в `ProductRepository` (см. §4) при выборке рецепта:
для каждого ингредиента `БЖУ_ингр_на_100г * mass` → сумма по всем ингредиентам → делим на
суммарную массу рецепта и ×100. Если у продукта-ингредиента неполные БЖУ — они берутся как есть.

### 3.2. Room: миграция v2 → v3

В `AppDatabase.kt`: `version = 3`, добавить `MIGRATION_2_3`:

```sql
CREATE TABLE IF NOT EXISTS `product` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `name` TEXT NOT NULL,
  `kcal` REAL NOT NULL,
  `proteins` REAL NOT NULL,
  `fats` REAL NOT NULL,
  `carbs` REAL NOT NULL,
  `portionMass` INTEGER NOT NULL,
  `note` TEXT,
  `source` TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS `recipe` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `name` TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS `recipe_ingredient` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `recipeId` INTEGER NOT NULL,
  `productId` INTEGER NOT NULL,
  `mass` INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS `index_recipe_ingredient_recipeId_productId`
  ON `recipe_ingredient` (`recipeId`, `productId`);
```

### 3.3. SettingsDataStore: новые ключи

Добавить (паттерн как `targetRange`):

| Ключ | Тип | Дефолт | Назначение |
|---|---|---|---|
| `daily_dose` | Float | 20.0 | суточная доза инсулина → УГ и ФЧИ |
| `target_glucose` | Float | 5.0 | целевая глюкоза (ЦГ) для коррекции |

Свойства:
- `dailyDose: Flow<Float>`, `suspend fun setDailyDose(v: Float)`;
- `targetGlucose: Flow<Float>`, `suspend fun setTargetGlucose(v: Float)`.

УГ и ФЧИ в UI показываются **read-only**, вычисляются из `dailyDose` в `BolusCalculator`.

---

## 4. Repository и клиент API

### 4.1. `ProductRepository` (data/)

Обёртка над `FoodDao` + клиент OpenFoodFacts + расчёт рецептов:
- `observeProducts(): Flow<List<ProductEntity>>`;
- `observeRecipes(): Flow<List<RecipeWithMacros>>` (рецепт + рассчитанные БЖУ на 100 г
  + суммарная масса порции по умолчанию);
- `addProduct(...)`, `updateProduct(...)`, `deleteProduct(id)` (удаляет и ингредиенты рецептов);
- `addRecipe(name, ingredients: List<Pair<productId, mass>>)`, `updateRecipe`, `deleteRecipe(id)`;
- `saveRemoteProduct(product: ProductEntity)` — сохранить найденный в OpenFoodFacts продукт
  в локальную БД (`source = "remote"`), чтобы в следующий раз он был в локальном списке.

### 4.2. `OpenFoodFactsClient` (data/)

По образцу `XdripWebClient.kt` (HttpURLConnection + Gson — уже в зависимостях, без новых библиотек):

- Поиск по названию:
  ```
  GET https://world.openfoodfacts.org/api/v2/search?search_terms=<q>&countries_tags_en=russia&page_size=20&fields=product_name,brands,nutriments,code
  ```
- Маппинг ответа `product["nutriments"]`:
  - `energy-kcal_100g` → `kcal`;
  - `proteins_100g` → `proteins`;
  - `carbohydrates_100g` → `carbs`;
  - `fat_100g` → `fats`;
  - `product_name` + `brands` → `name`;
  - `code` (штрихкод) → в `note` (источник «из OpenFoodFacts»).
- Пропускать продукты без углеводов/калорий (`carbs` отсутствует) — их нельзя использовать
  для ХЕ.
- `source = "remote"`, `portionMass = 0` (префилл не задан, пользователь вводит массу сам).
- Таймауты: connect 3000 мс / read 5000 мс, `withContext(Dispatchers.IO)`.
- Ошибки сети пробрасываются как исключение → в UI состояние «не удалось найти / нет сети».

---

## 5. UI

### 5.1. Навигация

`AppNavHost.kt`:
- Добавить маршрут `"bolus"` и 3-ю вкладку в `BottomNav`:
  `BottomTab("bolus", "Калькулятор", Icons.Filled.Calculate)`.
- `BolusScreen(viewModel = ...)` подключается как `composable("bolus")`.

### 5.2. `ui/bolus/BolusScreen.kt` + `BolusViewModel.kt`

`BolusViewModel` (factory из `AppContainer`): зависимости — `ProductRepository`,
`SettingsDataStore`, `DayRepository`, `XdripStatusProvider`.

Состояние (MutableStateFlow / data class):
- `foodItems: List<FoodItem>` — выбранные блюда (productId | recipeId, name, БЖУ на 100 г,
  масса, source);
- `targetGlucose: String`, `actualGlucose: String`, `activeInsulin: String` (строки полей,
  с дефолтами из настроек/xdrip/журнала);
- `dailyDose`, `УГ`, `ФЧИ` (read-only, из настроек);
- `search: SearchState` — запрос, результаты (локальные + remote), загрузка/ошибка;
- `lastBolus / lastMealInserted` — флаги/сообщения для уведомлений.

Логика:
- `addLocalProduct(id)`, `addRecipe(id)`, `addRemoteProduct(dto)`,
  `setMass(index, grams)`, `removeItem(index)`;
- `setTargetGlucose/Actual/Active` — сохраняют ЦГ в настройки, АГ/АИ локально;
- `refreshActualGlucose()` — из `XdripStatusProvider.status.lastValue` (если подключен);
- `refreshActiveInsulin()` — из сегодняшних болюсов через `DayRepository`:
  для каждого болюса в `h` со значением `v`: `остаток = v * (1 − 0.2 * (now − h))`,
  `>= 0`, суммировать; ручной ввод перекрывает авто;
- `search(query)` — debounce 400 мс, параллельно: фильтр локальных + `OpenFoodFactsClient.search`;
- `insertMealIntoDay()` и `logBolus()` — см. §6;
- `clearList()` — очистить после записи.

### 5.3. Подвкладка «Расчёт»

Структура `LazyColumn`:

1. **Карточка «Параметры»**
   - ЦГ — редактируемое поле (Decimal), сохраняется в настройки;
   - АГ — поле, авто-заполняется из xdrip при открытии (с пометкой «из xdrip»),
     редактируемое, кнопка обновить;
   - АИ — поле, авто-заполняется из журнала болюсов (пометка «авто»), редактируемое;
   - УГ и ФЧИ — read-only текст (вычислены из суточной дозы); рядом ссылка/подсказка
     «суточная доза: N ед.».
2. **Карточка «Еда»**
   - список `FoodItem` (название + источник, поле массы, ХЕ/БЖЕ строки, удаление);
   - кнопка «Добавить» → диалог поиска.
3. **Карточка «Итог»**
   - блок из 4 чисел: **БЖЕ**, **ХЕ**, **Инс. на БЖЕ**, **Инс. на ХЕ** (по 2 знака);
   - строка «Коррекция» и строка «− АИ»;
   - крупно **«Итоговый болюс: N ед.»**;
   - кнопки:
     - **«Вставить еду в день»** (вторичная) — создаёт приём пищи в журнале дня;
     - **«Записать болюс»** (основная) — пишет болюс в журнал дня;
     - обе активны только при непустом списке еды / итоге > 0.

### 5.4. Диалог поиска (`SearchDialog`)

- Поле ввода с debounce;
- Результаты: группа «Мои продукты» (локальные, source manual/remote), группа «Рецепты»,
  группа «OpenFoodFacts» (помечены бейджем источника);
- Каждый элемент: название, БЖУ кратко (ккал/Б/Ж/У на 100 г); тап → добавляет в список
  с дефолтной массой (`portionMass` или 100);
- Состояния: пусто, загрузка (спиннер), ошибка сети (текст + «повторить»).

### 5.5. Подвкладка «Продукты» (`ProductEditorScreen`)

- Список локальных продуктов (LazyColumn), карточка: название, ккал/Б/Ж/У на 100 г,
  масса порции, note, кнопки «редактировать» / «удалить»;
- FAB / кнопка «Добавить продукт» → форма:
  - название, ккал, белки, жиры, углеводы (на 100 г), масса порции (г), примечание;
  - сохранение → `ProductRepository.addProduct/updateProduct`;
- **CSV**: кнопки «Экспорт CSV» и «Импорт CSV» (см. §7).

### 5.6. Подвкладка «Рецепты» (`RecipeEditorScreen`)

- Список рецептов: название, БЖУ на 100 г (рассчитано), суммарная масса, «редактировать»/«удалить»;
- Создание/редактирование:
  - поле «Название»;
  - список ингредиентов: продукт + масса (г), удаление;
  - кнопка «Добавить ингредиент» → диалог выбора существующего продукта (локальные, затем remote);
  - живьём показываются рассчитанные БЖУ на 100 г и итоговые ХЕ/БЖЕ для суммарной массы;
  - сохранение → `ProductRepository.addRecipe/updateRecipe`.

---

## 6. Интеграция с основным экраном дня

Кнопки в «Итоге» используют существующие механизмы `DayRepository` и записывают данные
в **журнал текущего дня** (`LocalDate.now()`).

### 6.1. «Вставить еду в день»

Создаёт/обновляет запись приёма пищи (`MealEntity`, см. `MealsSection`/`DayRepository.setMealField`):

- `time` = текущее время (`currentTimeString()`);
- `food` = форматированный список: `«Коврижка (142 г); пастила (32 г)»`
  (название + масса через `; `);
- `carbs` = суммарные углеводы всех блюд списка = `Σ (carbs_i / 100 * m_i)`, округлить до целого
  (в `MealEntity.carbs: Int?`);
- `hunger` — не трогаем (null).

Пользователь после вставки может открыть экран «День» и увидеть приём пищи в
секции «Приёмы пищи» с заполненным текстом и углеводами.

### 6.2. «Записать болюс»

`repo.setBolus(date = today, h = now, value = итог)` — паттерн `BolusCard` из
`InsulinSection.kt`. Итог берётся с округлением до 2 знаков, только если > 0.

### 6.3. Поведение после записи

- Список еды очищается, масса/параметры сбрасываются к дефолтам;
- Показывается snackbar/toast: «Еда добавлена в день», «Болюс записан: N ед.»;
- Навигация на экран дня не обязательна (можно остаться во вкладке), при желании —
  кнопка/линк «Открыть день» (`navController.navigate("day")`).

---

## 7. CSV экспорт/импорт еды (отдельно от журнала)

Новый `data/importexport/FoodCsvCodec.kt` (формат не пересекается с `CsvCodec.kt` журнала).

### 7.1. Формат

Разделитель `;`, первая строка — заголовок. Строки двух типов:

```
Тип;Название;Ккал;Белки;Жиры;Углеводы;МассаПорции;Примечание;Источник
ПРОДУКТ;Коврижка;368;5.5;6;73;142;;manual
РЕЦЕПТ;Торт птичье молоко;423.46;3.18;24.98;46.48;100;испечённый дома;manual
ИНГРЕДИЕНТ;<recipeName>;<productName>;<mass>
```

- `ПРОДУКТ` — запись продукта;
- `РЕЦЕПТ` — рецепт + его рассчитанные БЖУ на 100 г;
- `ИНГРЕДИЕНТ` — связь рецепт→продукт с массой (следует сразу за своим рецептом).

### 7.2. Экспорт

- Кнопка «Экспорт CSV» → `ActivityResultContracts.CreateDocument("text/csv")`
  (паттерн `ImportExportScreen.kt`);
- Пишутся все продукты и все рецепты с ингредиентами;
- Числа — через точку (`Locale.US`), примечания экранируются `"` (паттерн `CsvCodec.escapeCell`).

### 7.3. Импорт

- Кнопка «Импорт CSV» → `OpenDocument` (можно задать MIME `text/csv`);
- Парсинг, затем транзакция в `FoodDao`: upsert продуктов, рецептов, ингредиентов.
  Продукты ищутся по имени (дубликаты — обновляются значения БЖУ);
- Ошибки формата → понятное сообщение (как `ImportCoordinator.parse`).

---

## 8. Список файлов

### Создать

```
android/app/src/main/java/com/koftamainee/glucolog/domain/BolusCalculator.kt      // чистая математика
android/app/src/main/java/com/koftamainee/glucolog/data/db/ProductEntity.kt
android/app/src/main/java/com/koftamainee/glucolog/data/db/RecipeEntity.kt
android/app/src/main/java/com/koftamainee/glucolog/data/db/RecipeIngredientEntity.kt
android/app/src/main/java/com/koftamainee/glucolog/data/db/FoodDao.kt
android/app/src/main/java/com/koftamainee/glucolog/data/ProductRepository.kt
android/app/src/main/java/com/koftamainee/glucolog/data/OpenFoodFactsClient.kt
android/app/src/main/java/com/koftamainee/glucolog/data/importexport/FoodCsvCodec.kt
android/app/src/main/java/com/koftamainee/glucolog/ui/bolus/BolusScreen.kt
android/app/src/main/java/com/koftamainee/glucolog/ui/bolus/BolusViewModel.kt
android/app/src/main/java/com/koftamainee/glucolog/ui/bolus/SearchDialog.kt
android/app/src/main/java/com/koftamainee/glucolog/ui/bolus/ProductEditorScreen.kt
android/app/src/main/java/com/koftamainee/glucolog/ui/bolus/RecipeEditorScreen.kt
android/app/src/test/java/com/koftamainee/glucolog/BolusCalculatorTest.kt         // JVM unit-тесты
```

### Изменить

```
android/app/src/main/java/com/koftamainee/glucolog/data/db/AppDatabase.kt         // version 3 + миграция 2→3
android/app/src/main/java/com/koftamainee/glucolog/data/SettingsDataStore.kt     // dailyDose, targetGlucose
android/app/src/main/java/com/koftamainee/glucolog/di/AppContainer.kt            // ProductRepository, OpenFoodFactsClient
android/app/src/main/java/com/koftamainee/glucolog/ui/AppNavHost.kt              // маршрут "bolus" + 3-я вкладка
```

`DayRepository`, `MealEntity`, `CsvCodec`, `ImportCoordinator` — **не меняются** (используются как есть).

---

## 9. Тестирование

### 9.1. Юнит-тесты `BolusCalculatorTest.kt`

- ХЕ одного блюда: Коврижка, `carbs=73`, `m=142` → `ХЕ = 73*142/100/12 = 8.6383…` → 8.64;
- БЖЕ с масштабированием: `kcal=368, carbs=73, m=142` →
  `(368 − 73*4)/100 * 142/100 = 0.76*1.42 = 1.0792` → 1.08 (в таблице без масштаба было 0.76);
- Суммирование нескольких блюд: ХЕ_общ/БЖЕ_общ = сумме строк;
- Инс. на ХЕ и на БЖЕ при `dailyDose=20` (УГ 0.48, ФЧИ 5);
- Коррекция: `(АГ − ЦГ)/ФЧИ`, отрицательные значения;
- АИ вычитается;
- Итог округляется до 2 знаков;
- `dailyDose=0` и пустой список → корректные значения / защита от деления на 0.

### 9.2. Сборка и проверка

```
cd android && ./gradlew :app:assembleDebug
```

(в CI также `:app:testDebugUnitTest` — при наличии настроек тестов).

---

## 10. Порядок работ

1. `BolusCalculator` + юнит-тесты (сверка с данными из `ХЕ.xlsx`).
2. Room: сущности, `FoodDao`, миграция 2→3; `SettingsDataStore` (+2 ключа);
   `ProductRepository` (включая расчёт БЖУ рецептов).
3. `OpenFoodFactsClient` (поиск, маппинг, таймауты).
4. `AppContainer` + `AppNavHost` (3-я вкладка, маршрут).
5. UI: подвкладки, диалог поиска, редакторы продуктов и рецептов.
6. «Расчёт»: список еды, параметры, итог (4 числа + коррекция/АИ/итог).
7. Интеграция с днём: «Вставить еду в день» (MealEntity) и «Записать болюс» (InsulinEntity).
8. CSV экспорт/импорт еды.
9. Сборка `assembleDebug`, фикс ошибок.

---

## 11. Крайние случаи и решения

- **Пустая БД** на старте: пользователь добавляет продукты вручную, создаёт рецепты или
  ищет через OpenFoodFacts (импорт в локальную БД кнопкой/при выборе).
- **Продукт без БЖУ из OpenFoodFacts** (нет `carbs`) — исключается из результатов поиска;
  если выбран продукт с `kcal=0` — БЖЕ считается 0.
- **Рецепт из рецептов** (вложенность) — в v1 не поддерживается: ингредиенты рецепта — только
  продукты (локальные или из OpenFoodFacts). При необходимости — отдельная задача.
- **Рецепт без ингредиентов / продукт с нулевой массой** — блокировка сохранения с подсказкой.
- **Итог ≤ 0** (АИ и коррекция сильно гасят еду) — болюс не записывается, показывается «0 ед.»;
  пользователь может скорректировать АГ/АИ/ЦГ вручную.
- **АГ без xdrip** — поле пустое, пользователь вводит вручную; если пусто — в формулу
  подставляется ЦГ (коррекция = 0) с пометкой.
- **АИ без болюсов** — авто-значение 0, пользователь может ввести вручную.

---

## 12. Связь с требованиями пользователя

- ✅ «считаем общее ХЕ и БЖЕ, потом заталкиваем в формулу» — §2.4;
- ✅ «отображать все 4 числа — БЖЕ, ХЕ, инсулин на БЖЕ, инсулин на ХЕ» — §2.5;
- ✅ «кнопка автоматически вставить еду в основной экран дня» — §6.1;
- ✅ «отдельная вкладка для расчёта, список блюд, автоматические параметры, пользователь
  вводит только ЦГ и список еды» — §1, §5.3;
- ✅ «еда из открытого источника с API» — §4.2 (OpenFoodFacts);
- ✅ «вкладка добавления в локальную БД: ручной режим (БЖУ на 100 г + масса) и составной
  рецепт из существующих сущностей» — §5.5, §5.6;
- ✅ «CSV экспорт/импорт именно еды отдельно от глюкозы» — §7.

---

## 13. Ход выполнения (журнал)

1. **BolusCalculator + юнит-тесты — ГОТОВО.**
   - `domain/BolusCalculator.kt`: `FoodNutrients`, `FoodPortion`, `BolusResult`,
     `calculate(items, dailyDose, targetGlucose, actualGlucose, activeInsulin)`;
     `breadUnits()` и `bje()` округляют до 2 знаков (для согласованности сумм строк
     с итогом); итог считается из округлённых компонентов; защита от `dailyDose <= 0`.
   - `app/src/test/.../BolusCalculatorTest.kt`: 12 тестов, все зелёные
     (`./gradlew :app:testDebugUnitTest`).
   - Для тестов в `gradle/libs.versions.toml` добавлен `junit 4.13.2`, в
     `app/build.gradle.kts` — `testImplementation(libs.junit)`.
   - Замечание: `sumOf(Float)` даёт overload-ambiguity в Kotlin 2.2 — заменено на `fold`.

2. **Room + настройки + ProductRepository — ГОТОВО.**
   - `ProductEntity`, `RecipeEntity`, `RecipeIngredientEntity`, `FoodDao`
     (products/recipes/ingredients, синхронные + Flow, каскадные удаления).
   - `AppDatabase` → version 3, `MIGRATION_2_3` (3 таблицы + индекс), `foodDao()`.
   - `SettingsDataStore`: `dailyDose` (дефолт 20), `targetGlucose` (дефолт 5.0).
   - `domain/RecipeInfo` (БЖУ рецепта на 100 г, пересчитывается из ингредиентов).
   - `ProductRepository`: CRUD продуктов/рецептов, `saveRemoteProduct`, расчёт рецептов
     через `combine` трёх Flow, `importFood` (по именам продуктов, с транзакцией,
     для CSV-импорта), каскад при удалении продукта/рецепта.

3. **OpenFoodFactsClient — ГОТОВО.**
   - `data/OpenFoodFactsClient.kt`: `RemoteFood(name, kcal, proteins, fats, carbs, note)`;
     поиск `GET world.openfoodfacts.org/api/v2/search?search_terms=<q>&countries_tags_en=russia&page_size=20&fields=product_name,brands,nutriments,code`;
     `HttpURLConnection` + Gson (как `XdripWebClient`), `Dispatchers.IO`;
     таймауты connect 5000/read 8000 мс, `User-Agent`; ккал с фолбэком
     (`energy-kcal_100g` → `energy_100g`/4.184 → из макросов); продукты без
     углеводов отбрасываются; HTTP≠200 → `OpenFoodFactsHttpException`.

4. **Вкладка «Калькулятор» (ViewModel + UI + навигация) — ГОТОВО.**
   - `ui/bolus/BolusViewModel.kt`: состояние (список еды, ЦГ/АГ/АИ, библиотека,
     поиск), авто-параметры из настроек и xdrip, АИ из болюсов дня (−20%/час, до 5 ч),
     `writeMeal` (одна `MealEntity` с time=сейчас, food «Название (г); …», carbs),
     `writeBolus`, CRUD продуктов/рецептов, поиск в OpenFoodFacts (debounce 400 мс).
   - `ui/bolus/BolusScreen.kt`: TabRow «Расчёт / Продукты / Рецепты»; расчёт итога
     по `BolusCalculator.calculate`; диалоги: добавление еды (локальные + OFF),
     новый продукт, новый рецепт (ингредиенты из библиотеки); кнопки
     «Вставить еду в день», «Записать болюс», «Очистить».
   - `ui/AppNavHost.kt`: маршрут `"bolus"`, 3-я вкладка нижней навигации (иконка
     `Icons.Filled.Calculate`).
   - `di/AppContainer.kt`: подключены `ProductRepository`, `OpenFoodFactsClient`.
   - Сборка: `./gradlew :app:assembleDebug` и `:app:testDebugUnitTest` — SUCCESS.

5. **CSV экспорт/импорт еды — ГОТОВО.**
   - `data/importexport/FoodCsvCodec.kt`: формат §7 (разделитель `;`, типы
     ПРОДУКТ / РЕЦЕПТ / ИНГРЕДИЕНТ), числа через точку (`Locale.US`), при импорте
     принимается и запятая; парсер с экранированием `"` (по образцу `CsvCodec`).
   - `ProductRepository.getAllRecipeIngredients()`; `BolusViewModel.exportFoodCsv(uri)`
     и `importFoodCsv(uri)` (через `FileOps`, merge-семантика `importFood(replace=false)`).
   - В подвкладке «Продукты»: кнопки «Экспорт CSV» / «Импорт CSV» (`CreateDocument`/
     `OpenDocument`, паттерн `ImportExportScreen`).
   - `app/src/test/.../FoodCsvCodecTest.kt`: 5 тестов round-trip (экспорт→импорт,
     привязка ингредиентов, импорт с ингредиентами, ошибки формата, запятая как
     десятичный разделитель).
   - Итог: `:app:testDebugUnitTest` — 17 тестов зелёные, `:app:assembleDebug` — SUCCESS.

6. **УГ/ФЧИ из журнала, единый поиск, без рецептов — ГОТОВО.**
   - `BolusCalculator.calculate(items, ug, fchi, targetGlucose, actualGlucose, activeInsulin)`
     (вместо `dailyDose`); `carbohydrateCoefficient(tdd)`/`insulinSensitivityFactor(tdd)` остаются.
   - **TDD** в `BolusViewModel`: за последние 7 дней сумма болюсов / число дней с болюсом > 0
     (минимум 1 день); авто-УГ = 12×tdd/500, авто-ФЧИ = 100/tdd; пересчёт по `observeDay(now)`.
     Если авто и данных нет — расчёт заблокирован, подсказка «укажите УГ/ФЧИ вручную».
     УГ и ФЧИ — строки с чекбоксом «Авто» (как АГ/АИ). `dailyDose` удалён из `SettingsDataStore`.
   - **Итог**: убраны строки «Коррекция» и «Активный инсулин»; БЖЕ, ХЕ, Углеводы г,
     Инс. на БЖЕ, Инс. на ХЕ отдельно; итог по прежней формуле.
   - **Рецепты удалены**: `RecipeEntity`, `RecipeIngredientEntity`, `domain/RecipeInfo`,
     рецептные методы `FoodDao`/`ProductRepository`, `RecipesTab`, `RecipeEditorDialog`.
     `AppDatabase` → version 4, `MIGRATION_3_4` (DROP recipe, recipe_ingredient).
   - Подвкладка «Продукты» переименована в **«Еда»**: добавление, экспорт/импорт CSV.
     `FoodCsvCodec` — только ПРОДУКТ (РЕЦЕПТ/ИНГРЕДИЕНТ убраны), `FoodCsvCodecTest` переписан.
   - **Ручное добавление продукта**: поле «Ккал» убрано, ккал = Б×4 + Ж×9 + У×4.
   - **Единый поиск** в «Добавить еду»: пустой запрос → локальные + популярные российские OFF
     (`sort_by=popularity&countries_tags_en=russia`); ввод → локальные `contains` + OFF
     `search_terms`; локальные выше, дедуп по имени; бейджи «Мои»/«OpenFoodFacts».
   - `OpenFoodFactsClient`: метод `popular()`, ретраи (4 попытки, задержки 0/800/2500 мс)
     на `IOException` и HTTP 429/5xx, таймауты 3000/5000, сообщение
     «Сервис перегружен, попробуйте ещё раз».
   - Итог: `:app:testDebugUnitTest` — 17 тестов зелёные, `:app:assembleDebug` — SUCCESS.


