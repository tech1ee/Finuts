# Finuts Roadmap

**Last Updated:** 2026-01-09
**Total Tests:** 859 (540 shared + 319 composeApp)
**Plan File:** [async-cooking-bird.md](~/.claude/plans/async-cooking-bird.md)

---

## Development Phases

### Phase 1: MVP (Weeks 1-8)
**Status**: ✅ Complete (95%)

| Milestone | Target | Status |
|-----------|--------|--------|
| M0: Project Setup | Week 1-2 | ✅ Complete |
| M1: Core Data & Navigation | Week 3-4 | ✅ Complete |
| M2: Transactions UI | Week 5-6 | ✅ Complete |
| M3: Budget & Reports | Week 7-8 | ✅ Complete |

**Features**:
- [x] KMP Project initialized
- [x] Design system created (Tabler Icons, theme tokens)
- [x] Account management (CRUD, archive, multi-currency)
- [x] Transaction CRUD (list, detail, add/edit, delete)
- [x] Categories (13 defaults + custom)
- [x] Category Management UI (Settings → Categories)
- [x] Budgets (full CRUD with progress tracking)
- [x] Quick add transaction (<30s target met)
- [x] Settings with DataStore preferences
- [x] Basic reports/charts (KoalaPlot donut chart)
- [x] Transfers (double-entry accounting)
- [x] Empty states for Dashboard sections

### Phase 2: AI Features (Weeks 9-14)
**Status**: ⏳ In Progress (65%)

| Milestone | Target | Status |
|-----------|--------|--------|
| M4: Android Release | Week 9-10 | ⏳ Pending |
| M5: AI Features | Week 11-12 | ⏳ In Progress |
| M6: iOS Release | Week 13-14 | ⏳ Pending |

**Features**:
- [x] AI categorization core (3-tier: rules + GPT-4o-mini + GPT-4o)
- [x] AI categorization integration (import flow + automatic category assignment)
- [ ] AI categorization UI (manual review/override)
- [ ] AI insights
- [x] Bank import parsers (CSV, OFX, QIF)
- [x] Bank import file picker (PDF + images for OCR)
- [ ] Bank import orchestrator and UI
- [ ] Google Play launch
- [ ] App Store launch

### Phase 3: Advanced (Weeks 15-20)
**Status**: ⏳ Pending

| Milestone | Target | Status |
|-----------|--------|--------|
| M7: Advanced Features | Week 15-18 | ⏳ Pending |
| M8: v1.0 Release | Week 19-20 | ⏳ Pending |

**Features**:
- [ ] Financial goals
- [ ] Receipt OCR
- [ ] Export (CSV, PDF)
- [ ] Push notifications
- [ ] Performance optimization

---

## Current Focus

**Milestone**: M5 - AI Features
**Current Iteration**: 16 - Android Release Prep
**Previous Iteration**: 15 - Import Feature 100% Complete ✅

### Completed Iterations
| # | Name | Date | Tests Added |
|---|------|------|-------------|
| 1-5 | Budget Domain/Data/Presentation | 2026-01-01 | +139 |
| 6 | Code Cleanup (refactoring) | 2026-01-02 | - |
| 7 | Budget UI Layer | 2026-01-02 | +26 |
| 8 | Transaction Detail/Edit | 2026-01-02 | +30 |
| 9 | Basic Reports/Charts | 2026-01-03 | +44 |
| 10 | Transfers (double-entry) | 2026-01-03 | +48 |
| 11 | AI Categorization (Tier 1) | 2026-01-04 | +78 |
| 12 | Bank Import Parsers | 2026-01-05/06 | +178 |
| 13 | Category Management UI | 2026-01-08 | 0 (TDD gap) |
| 14 | Category Testing (TDD) | 2026-01-08 | +44 |
| 15 | Import Feature 100% | 2026-01-09 | +4 |

### Next Iterations
- **Iteration 16**: Android Release Prep (testing, polish, Play Store)
- **Iteration 17**: AI Insights
- **Iteration 18**: iOS Release Prep
- **Iteration 19**: Bank Import UI polish

### Technology Decisions
| Feature | Technology | ADR |
|---------|------------|-----|
| Charts | KoalaPlot 0.10.4 | [ADR-005](decisions/005-koalaplot-charts.md) |
| AI Tier 1 | Rule-based + MerchantDB | Implemented |
| AI Tier 2 | GPT-4o-mini / Claude Haiku | Research |
| AI Tier 3 | GPT-4o / Claude Sonnet | Research |
| Bank Import | 4-tier: Format→Parsers→ML→LLM | Research |

---

## PRD Feature Mapping

### Phase 1: MVP Features

| PRD Feature | Status | Implementation |
|-------------|--------|----------------|
| Account management | ✅ Complete | AccountsScreen, AccountDetail, AddEditAccount |
| Transaction CRUD | ✅ Complete | TransactionsScreen, TransactionDetail, AddEditTransaction |
| Categories | ✅ Complete | 13 default categories + CategoryRepository |
| Budgets | ✅ Complete | BudgetsScreen, BudgetDetail, AddEditBudget |
| Basic reports | ✅ Complete | ReportsScreen, CategoryDonutChart (KoalaPlot) |
| Transfers | ✅ Complete | AddTransferScreen, double-entry accounting |
| Quick add (<30s) | ✅ Complete | QuickAddSheet modal |
| Multi-currency | ✅ Complete | KZT, USD, EUR, RUB support |
| Offline-first | ✅ Complete | Room database, local storage |

### Phase 2: AI Features (In Progress)

| PRD Feature | Status | Notes |
|-------------|--------|-------|
| AI categorization | ✅ Core done | 3-tier implemented, UI pending |
| AI insights | ⏳ Pending | Financial recommendations |
| Bank statement import | ⏳ 50% done | Parsers complete, orchestrator/UI pending |

### Phase 3: Advanced (Pending)

| PRD Feature | Status | Notes |
|-------------|--------|-------|
| Financial goals | ⏳ Pending | Savings goals tracking |
| Receipt OCR | ⏳ Pending | ML Kit / Vision Framework |
| Export (CSV, PDF) | ⏳ Pending | Report export |

---

## Architecture Compliance

| Rule | Status |
|------|--------|
| File < 200 lines | ✅ All compliant |
| TDD approach | ✅ Followed |
| No fakes in prod | ✅ Verified |
| SOLID principles | ✅ Applied |
| Clean Architecture | ✅ Layers separated |
| Test coverage >65% | ✅ Estimated 70%+ |

---

## Test Coverage

| Module | Tests | Status |
|--------|-------|--------|
| shared/ | 536 | ✅ |
| composeApp/ | 319 | ✅ |
| **Total** | **855** | ✅ |

### By Feature
| Feature | Tests |
|---------|-------|
| Accounts | 47 |
| Transactions | 69 |
| Budgets | 68 |
| Reports | 44 |
| Transfers | 48 |
| AI Categorization | 78 |
| Bank Import | 178 |
| Dashboard | 16 |
| Settings | 13 |
| Categories | 32 |
| UI Components | 38 |

---

## Success Metrics (Year 1)

| Metric | Target |
|--------|--------|
| MAU | 50,000 |
| Retention D30 | >40% |
| Premium Conversion | 5-8% |
| MRR | $5,000-7,500 |
| Rating | >4.5★ |
