# Проект 3. Incident Radar

CLI-утилита для анализа NGINX-логов. Читает логи (файлы, glob-маски или URL),
разбирает строки, фильтрует по датам и строит отчёт со статистикой.

Одна строка лога выглядит так:

```text
93.180.71.3 - - [17/May/2015:08:05:32 +0000] "GET /downloads/product_1 HTTP/1.1" 304 0 "-" "Debian APT-HTTP/1.3"
```

Из неё утилита достаёт IP, дату, метод, ресурс, код ответа и размер ответа.
А из миллиона таких строк собирает картину: сколько запросов, какие endpoint'ы
популярны, сколько ошибок, какие ответы самые тяжёлые.

Здесь впервые появляются «взрослые» требования: файл может быть гигантским
(читаем потоково), строки бывают битыми (пропускаем, а не падаем), а утилиту
будут запускать скрипты (значит, нужны честные коды возврата).

Задание разбито на три merge request'а. Делай их по очереди.

---

## Быстрый старт

```bash
./gradlew run     # запустить программу
./gradlew check   # тесты и линтеры — ровно то, что проверяет CI
```

Нужен **JDK 25 или новее**. Gradle ставить не нужно — в проекте лежит `gradlew`,
он скачает нужную версию сам.

## Как проверить загрузку логов по HTTP

Для ручной проверки можно раздать готовые сэмплы локальным HTTP-сервером.
Из корня проекта запусти (нужен Python 3):

```bash
python3 -m http.server 8000 --bind 127.0.0.1 --directory samples/logs
```

Оставь этот терминал открытым. По адресу <http://127.0.0.1:8000/> будет список
файлов. В `--path` передавай URL конкретного лога, а не страницы со списком:

```bash
./gradlew run --args='--path http://127.0.0.1:8000/access-2025-03-01.log'
```

Эта проверка имеет смысл после реализации чтения по URL: стартовый шаблон
ещё не обрабатывает аргументы. Остановить HTTP-сервер можно через `Ctrl+C`.

В автоматических тестах сервер должен запускаться и останавливаться самим
тестом, например через Java `HttpServer`. Используй loopback-адрес и порт `0`
(ОС выберет свободный порт), настрой ответы `200` с тестовым логом и `404`,
а в приложение передавай URL с фактическим портом сервера. Останавливай сервер
в `finally` или `@AfterEach`, даже если проверка упала. Тогда тесты смогут
работать в GitLab CI без вручную запущенного сервера и внешних сайтов.

## Где что лежит

| Файл | О чём |
|---|---|
| [TASK.md](TASK.md) | Задание: этапы MR1–MR3, обязательные тесты, баллы |
| [HELP.md](HELP.md) | Работа с шаблоном: установка, команды, IDEA, линтеры, как сдавать |

Свой код складывай в `src/main/java/academy/incidentradar/`.

## Что реализовано в MR1

MR1 закрывает CLI-каркас и слой источников логов. Парсинг, статистика и отчёты — в MR2 и MR3.

### CLI

Поддерживаемые параметры:

| Параметр | Обязателен | Описание |
|---|---|---|
| `--path`, `-p` | да | файл, glob-маска или URL |
| `--output-format`, `-f` | нет (в MR1) | `json` или `markdown` |
| `--output-path`, `-o` | нет (в MR1) | путь для отчёта |
| `--from` | нет | нижняя граница периода (`yyyy-MM-dd`, включительно) |
| `--to` | нет | верхняя граница периода (`yyyy-MM-dd`, включительно) |
| `--help` | — | печатает справку и завершается с кодом 0 |

В MR1 `--output-format` и `--output-path` **необязательны**: отчёт ещё не пишется. Валидатор проверяет их только если задан хотя бы один — тогда оба должны быть заданы, а расширение `--output-path` (`json` → `.json`, `markdown` → `.md`) должно соответствовать формату. В MR3 это станет обязательной парой, и там же появится вывод отчёта в stdout.

### Поддерживаемые источники

| Тип | Как распознаётся | Пример |
|---|---|---|
| Локальный файл | всё, что не URL и не содержит `*`/`?`/`[` | `samples/logs/access-2025-03-01.log` |
| Glob-маска | содержит `*`, `?` или `[` | `samples/logs/access-*.log` |
| URL | начинается с `http://` или `https://` | `http://127.0.0.1:8000/access.log` |

Поддерживаемые расширения локальных файлов: `.log`, `.txt`. Для glob-масок к совпадениям применяется тот же фильтр по расширению — `logs/*` не подхватит `.gz` или `.bak`.

Чтение потоковое во всех трёх случаях:

- `FileLogSource` — `Files.lines(path, UTF_8)`;
- `GlobLogSource` — `Files.lines` для каждого файла через `flatMap`;
- `UrlLogSource` — `HttpResponse.BodyHandlers.ofLines()`.

Полная загрузка файла в память (`Files.readAllLines`) не используется нигде.

### Glob-маска

Резолвер раскрывает маску самостоятельно:

1. по паттерну определяется базовый каталог — префикс до первого метасимвола, приведённый к существующему каталогу (`logs/*.log` → `logs/`, `logs/access-*.log` → `logs/`, `*.log` → `.`);
2. `Files.walk` обходит базовый каталог;
3. `PathMatcher` с `glob:` матчит **относительный** путь от базового каталога. Это важно на Windows: абсолютный `glob:`-паттерн с буквой диска и `\`-escape ведёт себя нестабильно, а относительный паттерн работает одинаково на Windows и Linux;
4. разделители паттерна нормализуются под текущую ОС (пользователь на Windows может писать `--path 'logs/*.log'` — резолвер это поймёт);
5. совпадения фильтруются по расширению и сортируются — порядок файлов детерминирован;
6. пустой результат — `UsageException` (exit 2).

### Коды возврата

| Код | Когда |
|---|---|
| `0` | успех |
| `1` | непредвиденная ошибка (`InternalException`, необработанный `RuntimeException`) |
| `2` | некорректное использование: нет `--path`, файл не найден, glob без совпадений, неподдерживаемое расширение, неверный `--output-format`, `from >= to`, `--output-path` без `--output-format` (и наоборот), несовпадение расширения, URL вернул 4xx/5xx |

Разделение важно для скриптов: `2` — виноват вызывающий, `1` — упала программа.

### Валидация

`ConfigValidator` проверяет инварианты **до** открытия источников:

- `--path` не пустой и не пробельный;
- если задан `--output-format` или `--output-path` — заданы оба, расширение соответствует формату;
- если заданы обе даты — `from < to` строго;
- резолвер источников: файл существует, расширение поддержано, glob не пустой, URL синтаксически валиден и имеет схему `http`/`https`.

Ошибки валидации — `UsageException` → exit 2.

### Архитектура

```
cli/          CliOptions (picocli), OutputFormat
config/       RunConfig (record), ConfigValidator
exception/    IncidentRadarException, UsageException, InternalException
source/       LogSource (интерфейс), FileLogSource, GlobLogSource,
              UrlLogSource, LogSourceResolver
pipeline/     LogLineStream — склейка нескольких источников в один Stream<String>
App           оркестратор: parse → validate → execute, коды возврата
Main          точка входа, вызывает System.exit(App.run(args))
```

`App.run` возвращает `int` и **не** вызывает `System.exit` — это позволяет тестам проверять коды возврата, не убивая JVM. `System.exit` вызывается только в `Main`.

`LogSourceResolver.resolve(String) → List<LogSource>` возвращает список, потому что glob уже сейчас разворачивается в набор файлов, а в бонусах появится несколько `--path` (сравнение периодов). Список длины 1 для файла и URL — частный случай того же контракта.

Ресурсы закрываются через `try-with-resources` в `App.execute`: `LogLineStream` и `Stream<String>`. `LogSource.close()` — точка расширения для источников, держащих ресурс на уровне объекта; текущие три источника ресурсов на уровне объекта не держат.

### Что сознательно не сделано в MR1

- парсинг строк, `LogRecord`, статистика — MR2;
- JSON/Markdown-отчёты, защита от перезаписи output-файла, вывод в stdout — MR3;
- бонусы (adoc, дни недели, сравнение периодов, названия HTTP-кодов, уникальные протоколы) — после MR3.

В `App.execute` сейчас только читается и подсчитывается число строк — это проверяет, что источники работают end-to-end, и не тянет в MR1 недоделанный MR3-код.

### Проверка MR1

```bash
./gradlew spotlessApply
./gradlew test --tests 'academy.incidentradar.mandatory.mr1.*'
./gradlew check
```

Ожидаемо: `CliValidationTest` (5 методов + параметризованный на 3 значения) и `LogSourceTest` (4 метода) зелёные, тесты MR2/MR3 — `@Disabled`.

Ручная проверка на сэмплах:

```bash
# Локальный файл
./gradlew run --args="--path samples/logs/access-2025-03-01.log"

# Glob
./gradlew run --args="--path 'samples/logs/access-*.log'"

# URL (сервер из README шаблона)
python3 -m http.server 8000 --bind 127.0.0.1 --directory samples/logs
./gradlew run --args="--path http://127.0.0.1:8000/access-2025-03-01.log"

# Ожидаемые коды возврата
./gradlew run --args="--path samples/logs/nope.log"                          # 2
./gradlew run --args="--path samples/logs/access-2025-03-01.log --from 2025-03-02 --to 2025-03-01"  # 2
./gradlew run --args="--path samples/logs/access-2025-03-01.log --output-format json --output-path r.md"  # 2
```

## Что реализовано в MR2

MR2 закрывает парсер строки, фильтр по датам и сбор статистики. Отчёты и вывод в файл — в MR3.

### Парсер

Строка лога разбирается регуляркой **той же формы, что валидирует сэмплы в `TemplateSmokeTest`**:

```text
^(\S+) - (\S+) \[([^\]]+)] "(\S+) (\S+) (\S+)" (\d{3}) (\d+) "([^"]*)" "([^"]*)"$
```

Из неё в `LogRecord` (record) попадают: `remoteAddr`, `remoteUser`, `timeLocal`, `method`, `resource`, `protocol`, `status`, `bodyBytesSent`, `httpReferer`, `httpUserAgent`. Поля `"$request"` разбиваются на **три отдельных поля** — `method`, `resource`, `protocol`. `remoteUser = "-"` нормализуется в `null`.

`timeLocal` парсится как `ZonedDateTime` (`dd/MMM/yyyy:HH:mm:ss Z`, `Locale.ENGLISH`), чтобы сохранить offset. Для фильтра и статистики доступен `LogRecord.date()` → `LocalDate`.

**Два независимых вида поломки**, оба описаны в `samples/logs/README.md`:

1. **структурная** — строка не подходит под regex (пустая, произвольный текст, обрезанная, `$status` не число, пустой `$request`);
2. **семантическая** — строка под regex подходит, но `$time_local` не парсится (`[НЕ-ДАТА]`).

Обе возвращают `Optional.empty()` из `LogParser.parse`. Парсер **не бросает исключений и не пишет WARN сам** — это делает `App.execute`, который видит `Optional.empty()` и логирует `skipped malformed log line`. Разделение ответственности: тесты парсера не завязаны на формат лога, тесты CLI проверяют end-to-end поведение.

### Фильтр по датам

`DateFilter.of(from, to) → Predicate<LogRecord>`:

| `--from` | `--to` | Что остаётся |
|---|---|---|
| нет | нет | всё |
| задан | нет | `date >= from` |
| нет | задан | `date <= to` |
| задан | задан | `from <= date <= to` |

Границы **включительные** с обеих сторон (TASK: «период включительно»). Сравнивается `LogRecord.date()` (`LocalDate`), offset не учитывается — на сэмплах проекта это корректно, потому что весь лог в `+0000`.

### Статистика

`StatisticsCollector.collect(Stream<LogRecord>)` возвращает `Statistics`:

```text
Statistics
├── totalRequests      long
├── responseCodes      Map<Integer, Long>  // TreeMap, ключи по возрастанию
├── resources          List<ResourceCount> // top-10, по убыванию count, тай-брейк — лексикографически
└── responseSizeInBytes ResponseSizeStats
    ├── average        double  // округлено до 2 знаков
    ├── max            long
    └── p95            long
```

`ResourceCount` — record `(String resource, long count)`.

**Метод p95 — nearest rank.** Отсортировать размеры по возрастанию, взять элемент с индексом `ceil(0.95 * n) - 1` (0-based). Формула одной строкой:

```java
long p95 = sizes[(int) Math.ceil(0.95 * sizes.length) - 1];
```

На полном наборе `access-2025-03-0*.log` (`n = 3000`) это даёт `5450` — совпадает с таблицей в `samples/logs/README.md`. Линейная интерполяция дала бы `5450.20`; nearest rank выбран потому, что возвращает `long` без дробной части и воспроизводим одной строкой на любом размере выборки. Покрыт тестом `p95MethodIsDocumentedAndStable`.

`average` округляется через `Math.round(x * 100.0) / 100.0`. `max` и `p95` — целые `long`. На пустом наборе все три равны `0`.

`responseCodes` — `TreeMap`, чтобы порядок ключей был детерминирован (в MR3 это даст стабильный JSON/Markdown). `resources` — `TreeMap` → сортировка по `count` вниз, тай-брейк по имени вверх, `limit(10)`.

### Как это связано с CLI

В MR2 `--from` / `--to` уже влияют на результат, но `--output-format` / `--output-path` **по-прежнему необязательны и ни на что не влияют** — отчёта в файл или stdout ещё нет. Сводка статистики пишется в лог (`stderr` через Logback):

```text
INFO  a.i.App - Read 3000 records from 1 source(s); top resource /api/v1/accounts (581); avg 2363.94, max 10891, p95 5450
```

Это осознанно: `stdout` в MR3 станет каналом отчёта, и мешать туда отладочный вывод нельзя. Логи уже настроены в `logback.xml` → `System.err` ещё в MR1.

### Архитектура (дополнения)

```text
model/        LogRecord (record)
parser/       LogParser
filter/       DateFilter
stats/        Statistics, ResourceCount, ResponseSizeStats, StatisticsCollector
```

`App.execute` в MR2:

```text
lines → parse (Optional) → WARN on empty → filter(DateFilter) → StatisticsCollector.collect → LOG.info summary
```

Стрим остаётся ленивым до `toList()` внутри `StatisticsCollector` — материализация нужна только для `p95` (требует сортировки). На 3000 строк это копейки; TASK прямо разрешает не делать «идеально точный streaming p95 для огромных данных».

### Что сознательно не сделано в MR2

- JSON/Markdown-отчёты, запись в файл, защита от перезаписи, вывод отчёта в stdout — MR3;
- бонусы (adoc, дни недели, сравнение периодов, названия HTTP-кодов, уникальные протоколы) — после MR3;
- точный streaming p95 для гигабайтных логов — TASK явно выносит за скобки.

### Проверка MR2

```bash
./gradlew spotlessApply
./gradlew test --tests 'academy.incidentradar.mandatory.mr2.*'
./gradlew check
```

Ожидаемо: `LogParsingTest` (3 метода), `DateFilterTest` (4), `StatisticsTest` (5) — зелёные; тесты MR3 остаются `@Disabled`.

Ручная проверка на сэмплах:

```bash
# Полный набор: total = 3000, top = /api/v1/accounts (581), avg = 2363.94, max = 10891, p95 = 5450
./gradlew run --args="--path 'samples/logs/access-*.log'"

# Только второй день: total = 1500
./gradlew run --args="--path 'samples/logs/access-*.log' --from 2025-03-02"

# Один день: total = 1500 (границы включительные)
./gradlew run --args="--path 'samples/logs/access-*.log' --to 2025-03-01"

# Битые строки: 6 WARN + total = 7, exit 0
./gradlew run --args="--path samples/logs/malformed.log"

# Эталоны из samples/logs/README.md
./gradlew run --args="--path samples/logs/reference-codes.log"   # 200:2, 404:1, 500:3
./gradlew run --args="--path samples/logs/reference-sizes.log"   # avg 300.0, max 500, p95 500
```

Коды возврата в MR2 не меняются — те же `0/1/2`, что и в MR1.

### Решения, которые стоит зафиксировать

**p95 считается методом nearest rank.** Формула: отсортировать размеры по возрастанию, взять элемент с индексом `ceil(0.95 * n) - 1` (0-based). На сэмплах проекта это даёт `5450`, что совпадает с таблицей в `samples/logs/README.md`. Линейная интерполяция дала бы `5450.20`; выбран nearest rank, потому что он не требует дробных значений и воспроизводим на любом размере выборки одной строкой. Метод покрыт тестом `p95MethodIsDocumentedAndStable` в MR2 и описан в javadoc `ResponseSizeStats`.

**Парсер возвращает `Optional`, а не бросает.** Причина — `samples/logs/README.md` фиксирует два независимых вида поломки: структурную (regex не подошёл) и семантическую (regex подошёл, дата не парсится). Оба дают `Optional.empty()`, и оба обрабатываются одинаково — WARN и пропуск. Если бы парсер бросал `InternalException`, «ожидаемый сценарий» из TASK превращался бы в падение процесса на середине гигабайтного файла. WARN пишет `App.execute` — это позволяет тестировать парсер на строках, не проверяя формат лога, а тестировать CLI end-to-end на `malformed.log`.

**`StatisticsCollector` материализует записи.** `p95` требует сортировки размеров, поэтому стрим вычитывается в `List<LogRecord>` (или `long[]` для размеров). На 3000 строк это неощутимо, TASK прямо разрешает не делать идеально точный streaming p95. Если в бонусах появится сценарий с гигабайтными логами, это место — кандидат на замену на t-digest или reservoir sampling, но для MR2 overkill.

**Glob матчится по относительному пути.** Причина — нестабильность абсолютных `glob:`-паттернов на Windows. Это записано в комментарии к `LogSourceResolver.resolveGlob`. (Решение из MR1.)

**URL проверяется на этапе чтения, а не в валидаторе.** `UrlLogSource.lines()` делает `send`, получает статус и бросает `UsageException` на 4xx/5xx. Это даёт exit 2 на 404 без отдельного HEAD-запроса. Минус — сетевой запрос происходит в момент чтения, а не валидации; для MR1–MR2 это нормально, тест `remote404ExitsWithTwo` проходит. (Решение из MR1.)
