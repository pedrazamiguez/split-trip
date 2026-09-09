# SplitTrip Documentation Hub

Welcome to the central technical documentation and architectural knowledge base for **SplitTrip**, a multi-module offline-first Android application built with Kotlin, Jetpack Compose, Material 3, Room, Firestore, and Koin.

---

## 📚 Documentation Hierarchy

```
docs/
├── domain/                 → Living business invariants, ubiquitous language & models
├── architecture/
│   ├── adrs/               → Formal MADR Architectural Decision Records
│   └── patterns/           → Structural patterns, Clean Arch, Offline-First & MVI
├── design-system/          → Horizon Narrative tokens, glassmorphism & Compose guides
├── engineering/            → Code quality, static analysis, validation & releases
├── release/                → Google Play Store compliance, metadata & assets
├── privacy-policy.html     → Public web-hosted Privacy Policy (bilingual)
└── ai/                     → Multi-agent architecture, MCP integrations & SDD taxonomy
```

---

## 🏛️ 1. Architecture & Decision Records

### Architectural Decision Records (MADRs)
* [ADR-0001: True Offline-First Architecture & Reusable Sync Delegates](architecture/adrs/0001-offline-first-sync.md)
* [ADR-0002: Strict BigDecimal Math for Financial Calculations](architecture/adrs/0002-bigdecimal-math.md)
* [ADR-0003: Multi-Module Clean Architecture & Visibility Rules](architecture/adrs/0003-clean-architecture.md)
* [ADR-0004: MVI Pattern, Stateless Screens, and Handler Decomposition](architecture/adrs/0004-mvi-state-management.md)
* [ADR-0005: Mandatory Debounced Modifiers for UI Navigation & Actions](architecture/adrs/0005-debounced-ui-interactions.md)
* [ADR-0006: Koin Dependency Injection Standards & Variable Naming](architecture/adrs/0006-koin-di-conventions.md)
* [ADR-0007: Specialized Multi-Agent and Skill-Driven Development Ecosystem](architecture/adrs/0007-multi-agent-sdd-ecosystem.md)

### Architectural Patterns
* [Offline-First Architecture & Sync Delegates](architecture/patterns/offline-first.md)
* [Modularity, Dependency Graph & Visibility Rules](architecture/patterns/modularity-and-visibility.md)
* [MVI & Stateless Screens Architecture](architecture/patterns/mvi-and-stateless-screens.md)
* [Data Mapping Strategy (Entity, Document, DTO & UiMapper)](architecture/patterns/data-mapping-strategy.md)
* [Navigation, Routing & Controller Hierarchy](architecture/patterns/navigation-and-routing.md)
* [Core Services, Calculators & Component Catalog](architecture/patterns/core-services-catalog.md)
* [Architecture Diagrams & Data Flows](architecture/patterns/architecture-diagrams.md)

---

## 💼 2. Domain & Business Invariants

* [Ubiquitous Language & Domain Glossary](domain/ubiquitous-language.md)
* [Multi-Currency Snapshot Model & Exchange Rates](domain/multi-currency-and-snapshots.md)
* [Group Pocket Balance Model & Zero-Sum Invariants](domain/group-pockets-and-balances.md)
* [Cash Tranches, FIFO Pools & Withdrawal Tracking](domain/cash-tranches-and-fifo.md)
* [Add-Ons, Proportional Splits & Remainder Distribution](domain/add-ons-and-splits.md)
* [Subunits & Group Composition Trees](domain/subunits-and-composition.md)
* [Settlement Proposals, Acknowledgments & Consensus State Machine](domain/settlements-and-consensus.md)
* [User Reconciliation & Member Linking](domain/user-reconciliation.md)

---

## 🎨 3. Design System & UI

* [Horizon Narrative Design Language](design-system/horizon-narrative-design.md)
* [Tone of Voice & Verbal Identity Guide](design-system/tone-of-voice.md)
* [UX Patterns & Interaction Guidelines](design-system/ux-guidelines.md)
* [Compose Drop Shadows, Elevation & Clipping Guide](design-system/compose-shadows-and-clipping.md)
* [Tabler Icons Maintenance & Vector Script](design-system/tabler-icons.md)
* [Efficient Jetpack Compose Previews](design-system/compose-previews.md)

---

## 🛠️ 4. Engineering Standards & Quality

* [Code Quality, Static Analysis, Konsist & SonarQube Exclusions](engineering/quality-and-static-analysis.md)
* [Domain Input Validation Rules](engineering/input-validation.md)
* [Structured Logging Guidelines](engineering/logging-guidelines.md)
* [Branching, Versioning & Release Strategy](engineering/branching-and-releases.md)
* [Gradle Version Catalog Maintenance](engineering/version-catalog.md)
* [Firebase Remote Config Management & Deployment](engineering/remote-config-management.md)

---

## 🤖 5. AI Tooling & Multi-Agent Ecosystem

* [AI Development Ecosystem & MCP Overview](ai/ai-ecosystem.md)
* [Specialized Agent Personas & Phased SDD Pipeline](ai/agent-roles-and-taxonomy.md)
* [Code Intelligence Tools (Codebase Memory & Graphify)](ai/code-intelligence-tools.md)
* [Headroom Proxy Context Compression Evaluation](ai/headroom-evaluation.md)

---

## 🚀 6. Release & Store Distribution

* [Google Play Store Listing Metadata, Data Safety & Asset Specs](release/store-listing-metadata.md)
* [Public Web Privacy Policy Page](privacy-policy.html)
