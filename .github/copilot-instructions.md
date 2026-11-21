# SplitTrip Copilot Instructions

> [!IMPORTANT]
> **Source of Truth:** The single source of truth for all architectural constraints, project conventions, and agent behaviors is `../AGENTS.md` and the rules in `../.agents/rules/`. Copilot must read `../AGENTS.md` before answering complex queries or generating multi-file changes.

## IDE & Inline Completion Behavior
- **No Pragmatic Patches:** Copilot must refuse to generate standard boilerplate if it violates the Clean Architecture, offline-first, or Jetpack Compose patterns defined in the project.
- **Strict Dependencies:** When autocompleting ViewModel constructors, NEVER suggest `Context`, `LocaleProvider`, or Repositories. Suggest only UseCases, Mappers, and Domain Services.
- **Strict Math:** When completing money math, percentages, or exchange rates, always suggest `BigDecimal`. Never suggest `Double` or `Float`.
- **UI State & Formatting:** When generating presentation code, ensure `UiState` uses `ImmutableList`. Place formatting logic inside Mappers (using `LocaleProvider`), never inside ViewModels.
- **Read-Only Tags & Pills:** NEVER suggest `secondaryContainer`, `secondaryFixed`, or `secondaryFixedDim` for read-only badges, pills, or `PassportTag`s. Use `PassportTag` with semantic `PassportTagStyle` (`INFO`, `SUCCESS`, `WARNING`, `ERROR`, `NEUTRAL`).
- **Testing:** When completing test cases, DO NOT use Kotlin's `assert()`. Always suggest JUnit assertions (`Assert.assertTrue`, etc.) or MockK verifications.

## Copilot Chat Guidelines
- When asked to plan or evaluate a feature, always cross-reference the `.agents/rules/` directory.
- Avoid suggesting manual git operations.
- Always check the line count of the file being edited. If approaching 600 lines, proactively suggest extracting delegates or handlers.
