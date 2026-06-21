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
    keep {
        // Prevent deleting versions only referenced programmatically in build-logic scripts
        keepUnusedVersions.set(true)
    }
}
```

### Key Configurations:
1. **`sortByKey.set(true)`**: Tells the plugin to keep entries in `libs.versions.toml` sorted alphabetically under `[versions]`, `[libraries]`, and `[plugins]`.
2. **`keep { keepUnusedVersions.set(true) }`**: **Critical.** By default, the plugin removes any version in the `[versions]` block that isn't directly referenced in the `[libraries]` or `[plugins]` blocks of the TOML file. Because some versions (e.g. `jacoco`, `ktlint`) are referenced programmatically in custom Gradle precompiled script plugins inside `:build-logic` rather than the TOML itself, this setting prevents them from being deleted as "unused".

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
