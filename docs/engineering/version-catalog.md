# Version Catalog Maintenance & Dependency Updates

SplitTrip uses a centralized Gradle **Version Catalog** located in [libs.versions.toml](../gradle/libs.versions.toml) to manage all dependency coordinates, versions, and plugins. 

To automate the checking and updating of these dependencies to their latest stable versions, we use the **Version Catalog Update Plugin** by *littlerobots* (`nl.littlerobots.version-catalog-update`).

---

## ⚙️ How it Works

The plugin provides a Gradle task that queries maven repositories for newer versions of your declared libraries and plugins.

- **On-Demand Execution**: The dependency update task is **never** executed automatically during normal builds (like `gradle assembleDebug` or `gradle build`). It must be executed explicitly by a developer.
- **Alphabetical Sorting**: To keep the version catalog tidy and deterministic, the plugin automatically re-sorts all keys alphabetically in `libs.versions.toml` upon run.
- **In-Place Updates**: The plugin directly rewrites `gradle/libs.versions.toml` with the new version strings.

---

## 🚀 Running the Update Task

To check for updates and update the catalog file directly, run:

```bash
./gradlew versionCatalogUpdate
```

> [!NOTE]
> Under the hood, this task reads the current versions, contacts remote repositories (Maven Central, Google Maven, etc.), resolves the latest stable release for each artifact, and updates the version strings in `libs.versions.toml`.

---

## 🛠️ Configuration & Rules

The plugin configuration is located in the root [build.gradle.kts](../build.gradle.kts):

```kotlin
versionCatalogUpdate {
    sortByKey.set(true)
    pin {
        versions.add("kotlin")
        versions.add("ksp")
    }
    keep {
        // Prevent deleting versions only referenced programmatically in build-logic scripts
        keepUnusedVersions.set(true)
    }
}
```

### Key Configurations:
1. **`sortByKey.set(true)`**: Tells the plugin to keep entries in `libs.versions.toml` sorted alphabetically under `[versions]`, `[libraries]`, and `[plugins]`.
2. **`pin { versions.add(...) }`**: Pins specific dependencies so that automated update tasks will not bump them even when newer stable releases exist on Maven repositories.
3. **`keep { keepUnusedVersions.set(true) }`**: **Critical.** By default, the plugin removes any version in the `[versions]` block that isn't directly referenced in the `[libraries]` or `[plugins]` blocks of the TOML file. Because some versions (e.g. `jacoco`, `ktlint`) are referenced programmatically in custom Gradle precompiled script plugins inside `:build-logic` rather than the TOML itself, this setting prevents them from being deleted as "unused".

### 📌 Pinned Versions & Constraints

Certain dependencies are intentionally pinned in `versionCatalogUpdate` to maintain compatibility with external CI analysis tools and strict compiler integrations:

#### 1. Kotlin & CodeQL Compatibility Constraint
GitHub's automated security analysis workflow ([codeql.yml](../../.github/workflows/codeql.yml)) uses the CodeQL CLI action (`github/codeql-action/analyze`). CodeQL's Java/Kotlin extractor lags behind bleeding-edge Kotlin compiler releases.
- When Kotlin was upgraded to `2.4.20` in PR #1651 (and previously in #1637), the CodeQL workflow failed with:
  ```text
  Kotlin version 2.4.20 is too recent. CodeQL currently supports versions below 2.4.20
  ```
- Because CodeQL is a mandatory security gate on `develop` and pull requests, `kotlin` is pinned to `2.4.10` until CodeQL releases a bundle with official support for Kotlin `2.4.20+`.

#### 2. KSP Coupling
Kotlin Symbol Processing (`ksp`) has strict compiler plugin coupling and must match the exact Kotlin version in lockstep (e.g. Kotlin `2.4.10` pairs with KSP `2.3.11`).
- Allowing `ksp` to auto-bump to `2.3.12` (built against Kotlin `2.4.20`) while Kotlin is held back causes compiler plugin mismatches during annotation processing (Room DAOs, etc.).
- Therefore, `ksp` is pinned alongside `kotlin` until both can be safely upgraded together.

#### 3. Future Upgrade Protocol
Before unpinning or bumping `kotlin` and `ksp`:
1. **Check CodeQL Release Notes**: Verify that the latest GitHub CodeQL Action bundle includes support for the target Kotlin version (e.g., CodeQL CLI release notes or official Kotlin support matrix).
2. **Local Verification**:
   - Temporarily remove the `pin` entries in `build.gradle.kts` (or manually bump `kotlin` and `ksp` in `gradle/libs.versions.toml`).
   - Run `make fast-check` and full compilation (`./gradlew assembleDebug test`).
3. **CI Security Verification**:
   - Push to a branch and ensure the `CodeQL` workflow passes green before removing the version pin on `develop`.

---

## 🤖 Automated Weekly Updates

To ensure dependencies, security patches, and bug fixes do not lag behind, dependency updates are automated via a GitHub Actions workflow defined in [update-version-catalog.yml](../../.github/workflows/update-version-catalog.yml).

### Cadence & Triggers
- **Scheduled Run**: Executes every Monday at **08:00 UTC** (`0 8 * * 1`).
- **Manual Trigger**: Can be dispatched on-demand at any time from GitHub Actions (`workflow_dispatch`).

### Workflow Mechanics
1. **Source of Truth**: Checks out the latest `develop` branch.
2. **Execution**: Executes `./gradlew versionCatalogUpdate`.
3. **Change Detection**: Uses `git diff --quiet gradle/libs.versions.toml` to inspect whether updates were made. If no updates are found, the job terminates cleanly without creating branches or PRs.
4. **Branch & Pull Request**:
   - Commits changes to the dedicated branch `internal/version-catalog-update`.
   - Force pushes to update the branch on `origin`.
   - If an open Pull Request for `internal/version-catalog-update` already exists, it is automatically refreshed with the latest changes.
   - If no open Pull Request exists, it opens a new PR targeting `develop` with the `config` label.

> [!NOTE]
> Manual, on-demand updates via `make catalog-update` or `./gradlew versionCatalogUpdate` remain fully supported whenever a developer needs to upgrade dependencies immediately.

---

## 📋 Review & Merging Checklist

When reviewing automated dependency PRs opened by the bot, follow this verification checklist before approving and merging:

1. **Verify CI Status**: Ensure all continuous integration workflows (`Build and Test Android App`, `Static Analysis`, `Coverage & Architecture Rules`) pass green.
2. **Review Dependency Changes**:
   - Inspect the diff in `gradle/libs.versions.toml`.
   - Confirm version bumps are standard and no required pins/overrides were unintentionally altered.
3. **Local Compilation & Smoke Check (Optional/Recommended for Major Bumps)**:
   ```bash
   git fetch origin
   git checkout internal/version-catalog-update
   ./gradlew assembleDebug
   make fast-check
   ```
4. **Breaking Changes & Deprecations**: Check release notes or migration guides for any library that underwent major or minor version bumps with breaking API changes.
5. **Merge**: Once checks pass, merge into `develop`.

---

## 📚 Best Practices for Dependency Upgrades

When performing dependency upgrades, always follow this workflow to prevent build breakages and test regressions:

### 1. Integrate with `develop`
Before starting, ensure your local branch is fully up-to-date with `develop`:
```bash
git fetch origin
git merge origin/develop
```

### 2. Run the Update Task
Execute the update command:
```bash
./gradlew versionCatalogUpdate
```

### 3. Verify Compilation
Verify that the project compiles with the new version coordinates:
```bash
./gradlew assembleDebug
```
*Pay close attention to compiler plugin compatibilities (e.g. matching `ksp` and Kotlin versions).*

### 4. Run Quality Gates & Tests
Ensure all code formatting, static analysis, and unit tests are passing before committing:
```bash
make check
```
*(This triggers Andaluz localization, detekt check, ktlint format/check, Konsist architecture tests, and the entire JUnit test suite).*

### 5. Review Diff
Review the updated file with `git diff gradle/libs.versions.toml` to verify no critical versions were accidentally removed or pinned incorrectly.
