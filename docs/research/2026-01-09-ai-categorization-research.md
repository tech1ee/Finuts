# Исследование: AI Категоризация транзакций в финансовых приложениях

**Дата:** 2026-01-09
**Тема:** Автоматическая категоризация, обучение на корректировках, AI-ассистент

---

## 1. Автоматическая категоризация: Лучшие практики индустрии

### 1.1 Многоуровневая система категоризации

**Plaid Enrich API** (индустриальный стандарт):
- 16 основных категорий + 104 подкатегории
- 98% точность категоризации
- Named Entity Recognition для извлечения merchant name
- MCC (Merchant Category Codes) для базовой классификации

**Источник:** [Plaid Enrich](https://plaid.com/products/enrich/)

### 1.2 Методы автоматической категоризации

| Метод | Точность | Стоимость | Применение |
|-------|----------|-----------|------------|
| **MCC коды** | 70-80% | Бесплатно | Базовый слой |
| **Keyword matching** | 80-85% | Бесплатно | Известные merchants |
| **ML модели** | 90-95% | Средняя | Массовая обработка |
| **LLM** | 95-98% | Высокая | Сложные случаи |

**Источник:** [DocuClipper](https://www.docuclipper.com/blog/automatic-transaction-categorization/)

### 1.3 Multi-Label категоризация

FinAPI демонстрирует подход с множественными метками:
- Транзакция "Автокредит" получает labels: `Loan` + `Mobility`
- Позволяет детальный анализ по разным измерениям

**Источник:** [finAPI Categorization](https://www.finapi.io/en/products/data-intelligence/categorization/)

---

## 2. Пользовательские корректировки и обучение

### 2.1 Copilot Money: Золотой стандарт

**Архитектура:**
- Персональная ML модель для КАЖДОГО пользователя
- Активируется после проверки 30 транзакций
- Применяет предсказания только при высокой уверенности
- Пользователям нужно перекатегоризировать ~20% транзакций

**Факторы обучения:**
- Название транзакции (merchant)
- Сумма
- День недели
- Какая карта использована
- История корректировок

**Источник:** [Copilot Intelligence](https://help.copilot.money/en/articles/8182433-copilot-intelligence-for-spending)

### 2.2 Механизмы запоминания корректировок

#### A. Rule-Based System (PocketSmith)
```
Правило: "AMAZON" → Shopping
Применяется ко всем будущим транзакциям автоматически
```
**Источник:** [PocketSmith Auto-Categorize](https://learn.pocketsmith.com/article/255-auto-categorize-transactions)

#### B. Merchant Mapping Table
```sql
CREATE TABLE user_merchant_mapping (
    user_id TEXT,
    merchant_pattern TEXT,  -- "MAGNUM", "GLOVO*"
    category_id TEXT,
    confidence FLOAT,
    created_at TIMESTAMP,
    source TEXT  -- 'user_correction', 'rule', 'ml'
);
```

#### C. Active Learning Pipeline
```
1. Система предлагает категорию
2. Пользователь подтверждает/исправляет
3. Исправление → новый training sample
4. Периодическое дообучение модели
5. A/B тест новой модели
```

### 2.3 Collaborative Filtering

**Подход похожих пользователей:**
- Группировка пользователей по поведению
- Если User A и User B похоже категоризируют MAGNUM
- То новому User C предложить категорию на основе A и B

**Применение в финтех:**
- Cosine Similarity для sparse data
- Matrix Factorization для embeddings
- User-based vs Item-based (merchant-based)

**Источник:** [IBM Collaborative Filtering](https://www.ibm.com/think/topics/collaborative-filtering)

---

## 3. Детекция recurring транзакций

### 3.1 Методы детекции

**Plaid Recurring Transactions:**
- Группирует по description, amount, cadence
- "Зрелый" stream = минимум 3 повторения
- Разделяет на inflow/outflow streams

**Subaio:**
- 98.7% точность детекции
- 0.044% false positive rate
- Crowdsourcing для улучшения (пользователи флагают)

**Источник:** [Subaio Detection](https://subaio.com/subaio-explained/how-does-subaio-detect-recurring-payments)

### 3.2 Технические сложности

| Проблема | Решение |
|----------|---------|
| Разные даты биллинга | Fuzzy date matching (±3 дня) |
| Вариация суммы | Допуск ±10% |
| Habitual vs Subscription | Исключить gas/groceries/coffee |
| Merchant name variations | Normalization + ML |

---

## 4. AI-интеграция: Расширенные возможности

### 4.1 Текущая реализация в Finuts

```
Tier 1: Rule-based (100+ merchants) ✅ DONE
Tier 2: GPT-4o-mini (pending)
Tier 3: GPT-4o (pending)
```

### 4.2 Возможности для расширения

#### A. Proactive Insights & Alerts

**ChatGiraffe.ai подход:**
- Real-time мониторинг лимитов по категориям
- Уведомление при приближении к лимиту
- Детекция аномалий:
  - Транзакция значительно больше обычной
  - Необычная частота покупок
  - Новый неизвестный merchant

**Источник:** [ChatGiraffe AI Alerts](https://www.chatgiraffe.ai/article/red-flags-ai-alerts-for-abnormal-spending-over-budget)

#### B. Natural Language Interface (Cleo style)

**Примеры запросов:**
- "Сколько я потратил на еду в этом месяце?"
- "Покажи все подписки"
- "Где я могу сэкономить?"
- "Сравни траты за январь и февраль"

**Технология:**
- LLM для понимания intent
- SQL/query generation
- Персонализированные ответы

**Источник:** [Finance AI Chatbots](https://kaopiz.com/en/articles/finance-ai-chatbots/)

#### C. Predictive Analytics

**Возможности:**
- Прогноз баланса на конец месяца
- Предупреждение о предстоящих крупных платежах
- Cash flow forecasting
- Сезонность расходов

#### D. Smart Budget Suggestions

**Алгоритм:**
1. Анализ 3-6 месяцев истории
2. Выявление паттернов по категориям
3. Предложение реалистичных лимитов
4. Адаптация при изменении поведения

#### E. Subscription Optimization

**Фичи:**
- Детекция всех подписок
- Выявление неиспользуемых (no related transactions)
- Сравнение с альтернативами
- Одно-кнопочная отмена (deep link)

---

## 5. Рекомендации для Finuts

### 5.1 Краткосрочные улучшения (следующие итерации)

| Фича | ICE Score | Описание |
|------|-----------|----------|
| **User Correction Learning** | 576 | Запоминать корректировки для будущих транзакций |
| **Category Rules UI** | 480 | UI для создания правил "X → Category Y" |
| **Recurring Detection** | 504 | Автодетекция подписок |
| **Spending Alerts** | 432 | Push при превышении лимита |

### 5.2 Среднесрочные (Tier 2/3 AI)

| Фича | ICE Score | Описание |
|------|-----------|----------|
| **LLM Categorization** | 336 | GPT-4o-mini для неизвестных merchants |
| **Natural Language Query** | 280 | "Сколько на еду?" |
| **Anomaly Detection** | 320 | Unusual spending alerts |

### 5.3 Долгосрочные (AI Assistant)

| Фича | Описание |
|------|----------|
| **Predictive Cash Flow** | Прогноз на 30/60/90 дней |
| **Smart Budget Advisor** | Персональные рекомендации |
| **Subscription Manager** | Полное управление подписками |
| **Financial Health Score** | Интегральная метрика здоровья |

---

## 6. Архитектура системы обучения

### 6.1 Предлагаемая схема базы данных

```sql
-- User category corrections
CREATE TABLE category_corrections (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    original_category_id TEXT,
    corrected_category_id TEXT NOT NULL,
    merchant_name TEXT,
    merchant_normalized TEXT,  -- Normalized for matching
    amount REAL,
    created_at TIMESTAMP,
    applied_count INTEGER DEFAULT 0  -- How many times rule was used
);

-- Learned merchant mappings
CREATE TABLE learned_merchants (
    id TEXT PRIMARY KEY,
    user_id TEXT,  -- NULL = global
    merchant_pattern TEXT NOT NULL,  -- Regex or exact
    category_id TEXT NOT NULL,
    confidence FLOAT,
    source TEXT,  -- 'user', 'ml', 'collaborative', 'rule'
    sample_count INTEGER,
    last_used_at TIMESTAMP
);

-- Recurring transaction patterns
CREATE TABLE recurring_patterns (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    merchant_pattern TEXT,
    expected_amount REAL,
    amount_variance REAL,  -- Допустимое отклонение
    frequency_days INTEGER,  -- 30 = monthly
    frequency_variance INTEGER,  -- ±3 days
    next_expected_date DATE,
    category_id TEXT,
    is_subscription BOOLEAN,
    status TEXT  -- 'active', 'paused', 'cancelled'
);
```

### 6.2 Pipeline обработки корректировок

```kotlin
// Domain layer
class LearnFromCorrectionUseCase(
    private val correctionRepository: CorrectionRepository,
    private val merchantMappingRepository: MerchantMappingRepository
) {
    suspend fun learn(
        transactionId: String,
        originalCategory: String?,
        correctedCategory: String,
        merchantName: String
    ) {
        // 1. Save correction
        correctionRepository.save(
            Correction(
                originalCategory = originalCategory,
                correctedCategory = correctedCategory,
                merchantName = merchantName
            )
        )

        // 2. Check if we should create a rule
        val similarCorrections = correctionRepository
            .findSimilar(merchantName, correctedCategory)

        if (similarCorrections.size >= 2) {
            // Create learned mapping
            merchantMappingRepository.upsert(
                LearnedMerchant(
                    merchantPattern = normalizeForPattern(merchantName),
                    categoryId = correctedCategory,
                    confidence = calculateConfidence(similarCorrections),
                    source = "user"
                )
            )
        }
    }
}
```

---

## 7. Sources

### Transaction Categorization
- [Uncat - Mastering Transaction Categorization](https://www.uncat.com/blog/how-to-categorize-transactions-for-bank-and-credit-card-statements)
- [DocuClipper - Automatic Transaction Categorization](https://www.docuclipper.com/blog/automatic-transaction-categorization/)
- [PocketSmith - Auto-Categorize Transactions](https://learn.pocketsmith.com/article/255-auto-categorize-transactions)
- [finAPI Categorization](https://www.finapi.io/en/products/data-intelligence/categorization/)

### Plaid & Data Enrichment
- [Plaid Enrich API](https://plaid.com/products/enrich/)
- [Plaid Transaction Enrichment Engine](https://plaid.com/blog/transaction-enrichment-engine/)
- [Plaid Categorization Taxonomy Update](https://plaid.com/blog/transactions-categorization-taxonomy/)

### Machine Learning & User Corrections
- [Copilot Intelligence for Spending](https://help.copilot.money/en/articles/8182433-copilot-intelligence-for-spending)
- [Money with Katie - Copilot Review](https://moneywithkatie.com/copilot-review-a-budgeting-app-that-finally-gets-it-right/)
- [IBM - Collaborative Filtering](https://www.ibm.com/think/topics/collaborative-filtering)

### Recurring Transactions
- [Subaio - Recurring Payment Detection](https://subaio.com/subaio-explained/how-does-subaio-detect-recurring-payments)
- [Plaid - Recurring Transactions](https://plaid.com/blog/recurring-transactions/)
- [Meniga - Recurrent Payments](https://www.meniga.com/resources/recurring-payments/)

### AI & Insights
- [ChatGiraffe - AI Alerts for Spending](https://www.chatgiraffe.ai/article/red-flags-ai-alerts-for-abnormal-spending-over-budget)
- [Finance AI Chatbots Guide](https://kaopiz.com/en/articles/finance-ai-chatbots/)
- [GPTBots - Finance AI Chatbot](https://www.gptbots.ai/blog/finance-ai-chatbot)
- [Britannica Money - AI for Budgeting](https://www.britannica.com/money/ai-for-saving-and-budgeting)
