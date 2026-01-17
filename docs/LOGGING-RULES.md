# Logging Rules / Правила логирования

## Уровни логирования

| Level | Когда использовать | Пример |
|-------|-------------------|--------|
| **ERROR** | Ошибки, исключения, сбои | `log.e { "confirmImport FAILED: $message" }` |
| **WARN** | Потенциальные проблемы, восстановимые ситуации | `log.w { "Account not found, using default" }` |
| **INFO** | Важные бизнес-события, начало/конец операций | `log.i { "confirmImport START: count=118" }` |
| **DEBUG** | Детали для отладки, промежуточные значения | `log.d { "Processing step 2/5" }` |
| **VERBOSE** | Очень детально, per-item логи | `log.v { "Saving tx 45/118: Starbucks" }` |

## Формат сообщений

```
[METHOD_NAME] ACTION: key1=value1, key2=value2
```

### Примеры:

```kotlin
// Вход в метод
log.i { "confirmImport START: selectedCount=118, accountId=$accountId" }

// Промежуточный шаг
log.d { "confirmImport: Step 2/4 - Deduplicating..." }

// Успешное завершение
log.i { "confirmImport SUCCESS: saved=115, skipped=3" }

// Ошибка
log.e(exception) { "confirmImport FAILED: ${exception.message}" }

// Предупреждение
log.w { "confirmImport: currentPreview is NULL, aborting" }
```

## Обязательное логирование

### ViewModel методы:
1. `START` - вход в публичный метод с параметрами
2. `SUCCESS` или `FAILED` - результат операции

### UseCase методы:
1. `START` - вход с ключевыми параметрами
2. Каждый шаг процесса (Step 1/N, Step 2/N...)
3. `SUCCESS` или `FAILED` - результат

### Repository методы:
1. `START` для write-операций
2. `SUCCESS`/`FAILED` для write-операций

## Запрещено

- Эмодзи в логах
- println() вместо Logger
- Логи без контекста ("error occurred")
- Логи с чувствительными данными (пароли, токены)

## Теги

Каждый класс использует свой тег:
```kotlin
private val log = Logger.withTag("ClassName")
```
