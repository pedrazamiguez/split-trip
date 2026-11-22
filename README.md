# SplitTrip

[![Build & Test](https://github.com/pedrazamiguez/split-trip/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/pedrazamiguez/split-trip/actions/workflows/build-and-test.yml)
[![Static Analysis](https://github.com/pedrazamiguez/split-trip/actions/workflows/static-analysis.yml/badge.svg)](https://github.com/pedrazamiguez/split-trip/actions/workflows/static-analysis.yml)
[![Coverage & Architecture](https://github.com/pedrazamiguez/split-trip/actions/workflows/coverage-and-architecture.yml/badge.svg)](https://github.com/pedrazamiguez/split-trip/actions/workflows/coverage-and-architecture.yml)

## Overview

**SplitTrip** is a modular Android application built as a reliable shared-group companion for everyday life and multi-currency trips abroad. Whether you are a couple, flatmates, a household, a family, or a group of friends, it helps you organise shared expenses, track group pockets, calculate member positions, and synchronise data across devices.

Built with modern Android practices, including **Jetpack Compose**, **Clean Architecture**, and **Offline-First** principles, the app serves as a reference for scalable, multi-module Android development.

### ✨ Key Features

* **User Authentication**: Sign in with Email/Password, Google, or Guest mode via Firebase Authentication.
* **Shared Groups & Subunits**: Create and join groups with invite codes, organise couples or families into subunits, and finalise groups when done.
* **Group Pockets & Cash Tracking**: Manage virtual group pockets, member contributions, and FIFO cash withdrawals.
* **Smart Expense Tracking & AI Receipt Scanning**: Split expenses by equal amounts, exact amounts, percentages, or shares, attach add-ons, and scan receipts.
* **Multi-Currency Support**: Automatic exchange-rate sync handled server-side via Firebase Cloud Functions and Open Exchange Rates, backed by exact `BigDecimal` math.
* **Your Position & Settlement Consensus**: Clear member balances and collaborative payment agreements (propose payment, confirm receipt, or request review).
* **Spending Insights**: Visual breakdowns of group and member spending.
* **Offline-First Sync**: Full offline functionality powered by Room as the single source of truth, with automatic background Firestore synchronisation.
* **Push Notifications**: Instant updates when members record expenses or propose settlements.

## 🛠️ Tech Stack

* **Language**: [Kotlin](https://kotlinlang.org/) (100%)
* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3, Horizon Narrative design system)
* **Architecture**: Clean Architecture + MVVM + MVI patterns
* **Dependency Injection**: [Koin](https://insert-koin.io/)
* **Asynchronous Programming**: Coroutines & Flow
* **Local Data**: [Room Database](https://developer.android.com/training/data-storage/room) (Single Source of Truth)
* **Backend (BaaS)**: Firebase (Auth, Firestore, Cloud Functions, Cloud Messaging, Remote Config, Crashlytics)
* **Navigation**: Jetpack Navigation Compose (Feature-based modular navigation)
* **Code Quality**: Detekt, Ktlint, CPD, JaCoCo, Konsist, CodeQL

## 📂 Project Structure

The app follows a strict **Multi-Module** architecture to ensure separation of concerns and faster build times:

* **`:app`**: Application entry point, Koin DI wiring, and `AppNavHost`.
* **`:core`**: Shared foundational modules:
  * **`:core:common`**: Utilities, constants, providers (`LocaleProvider`, `ResourceProvider`), and DataStore preferences.
  * **`:core:design-system`**: Horizon Narrative UI components, themes, routes, `NavigationProvider`, `TabGraphContributor`, and `ScreenUiProvider`.
  * **`:core:logging`**: Structured logging utilities.
  * **`:core:performance`**: Performance monitoring utilities.
* **`:data`**: Offline-first repository implementations and sync delegates:
  * **`:data:local`**: Room database entities, DAOs, and DataStore.
  * **`:data:firebase`**: Firestore, Auth, and Firebase cloud data sources.
* **`:domain`**: Pure Kotlin business logic (models, repository interfaces, use cases, domain services, calculators).
* **`:features`**: Standalone feature modules (`authentication`, `balances`, `contributions`, `expenses`, `groups`, `main-entry`, `onboarding`, `profile`, `settings`, `settlements`, `subunits`, `withdrawals`).
* **`:konsist-tests`**: Automated architecture tests enforcing module visibility, naming conventions, and the 600-line file limit.
* **`build-logic/`**: Gradle convention plugins shared across modules.
* **`functions/`**: Firebase Cloud Functions (including scheduled Open Exchange Rates synchronisation in `functions/src/triggers/scheduled/syncExchangeRates.ts`).

## 🚀 Setup Instructions

### Prerequisites

* Android Studio Ladybug or newer.
* JDK 21.
* A Firebase Project with Authentication and Firestore enabled.

### Steps

1. **Clone the Repository**:

```bash
git clone https://github.com/pedrazamiguez/split-trip.git
cd split-trip
```

2. **Run the bootstrap script** (installs git hooks, scaffolds `local.properties`, and checks prerequisites):

```bash
make setup
```

> Run `make help` to see all available targets.

3. **Firebase Configuration**:

* Go to the [Firebase Console](https://console.firebase.google.com).
* Create a project and add an Android app (package: `es.pedrazamiguez.splittrip`).
* Download `google-services.json` and place it in the `app/` directory.

> Open Exchange Rates integration runs server-side via Firebase Cloud Functions (`functions/src/triggers/scheduled/syncExchangeRates.ts` using `defineSecret("OER_APP_ID")`), so no Open Exchange Rates API key is needed for local Android builds.

4. **Build & Run**:

* Sync Gradle files in Android Studio.
* Select the `app` configuration and run on an Emulator (API 26+ recommended).

### Verify your setup

```bash
make doctor      # checks JDK 21, app/google-services.json, sdk.dir in local.properties, pre-commit hook, AI taxonomy files, and local AI tooling
make fast-check  # fast incremental check: Andaluz string generation (when available), Remote Config validation, ktlint, detekt, lintDebug, Konsist tests, and >= 80% JaCoCo line coverage
make check       # full cold quality gate from a clean state mirroring CI
```

## 🧪 Testing

The project uses a comprehensive testing strategy:

* **Unit Tests**: JUnit 5 & MockK for Domain and Data layers.

```bash
./gradlew test

```

* **UI Tests**: Compose Testing for Screens and Navigation flows.

```bash
./gradlew connectedAndroidTest

```

## 📐 Architecture & Patterns

This project adheres to the **"Strict Visibility"** principle:

1. **Features** cannot see other **Features** (they communicate via `:domain`).
2. **Features** cannot see **Data** implementation details (only Domain interfaces).
3. **App** module is the only one that sees everything to wire up the Dependency Injection.

**Key Patterns:**

* **Navigation Discovery**: Features expose `NavigationProvider` interfaces so the App module can "
  plug them in" dynamically.
* **Repository Pattern**: Mediates between Local (Room) and Cloud (Firestore) data sources.
* **ScreenUiProvider**: Decouples the Main Activity's Scaffold (TopBar/FAB) from individual screens.

## Licence

This project is proprietary. See the [LICENSE](LICENSE) file for full terms.

© 2026 Andrés Pedraza Míguez. All Rights Reserved.
