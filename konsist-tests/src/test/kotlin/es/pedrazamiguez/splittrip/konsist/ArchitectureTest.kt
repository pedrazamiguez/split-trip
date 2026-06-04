package es.pedrazamiguez.splittrip.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Architecture Rules")
class ArchitectureTest {

    // Scope limited to production code only (no test sources)
    private val projectProductionScope by lazy {
        Konsist.scopeFromProduction()
    }

    private val domainProductionScope by lazy {
        Konsist.scopeFromProduction("domain")
    }

    @Nested
    @DisplayName("Naming Conventions")
    inner class NamingConventions {

        @Test
        @DisplayName("Top-level domain service interfaces must end with 'Service'")
        fun `domain services must end with Service`() {
            domainProductionScope
                .interfaces()
                .filter { it.resideInPackage("..domain.service..") }
                .filter { !it.resideInPackage("..addon..") }
                .filter { it.isTopLevel }
                .assertTrue { it.hasNameEndingWith("Service") }
        }

        @Test
        @DisplayName("Top-level use case classes must end with 'UseCase'")
        fun `use cases must end with UseCase`() {
            domainProductionScope
                .classes()
                .filter { it.resideInPackage("..domain.usecase..") }
                .filter { it.isTopLevel }
                // Exclude the .support subpackage — it holds result/attribution types and
                // distribution helpers that are co-located for cohesion but are NOT use cases
                // (e.g. WithdrawalResult, ExpenseResult, BalanceAttribution helpers).
                .filter { !it.resideInPackage("..domain.usecase..support") }
                // Exclude the .strategy subpackage — it holds Strategy-pattern implementations
                // (e.g. PersistExpenseStrategy, BasePersistExpenseStrategy) that are internal
                // collaborators of use cases, not use cases themselves.
                .filter { !it.resideInPackage("..domain.usecase..strategy") }
                // Exclude the .factory subpackage — it holds Factory classes that create strategy
                // instances (e.g. PersistExpenseStrategyFactory). Factories use their own suffix.
                .filter { !it.resideInPackage("..domain.usecase..factory") }
                .assertTrue { it.hasNameEndingWith("UseCase") }
        }

        @Test
        @DisplayName("Factory packages must exist and classes residing in them must end with 'Factory'")
        fun `classes residing in factory packages must end with Factory`() {
            val factoryClasses = (projectProductionScope.classes() + projectProductionScope.interfaces())
                .filter { it.resideInPackage("..domain.usecase..factory..") }
                .filter { it.isTopLevel }

            Assertions.assertTrue(
                factoryClasses.isNotEmpty(),
                "Factory package does not exist or has no classes"
            )

            factoryClasses.assertTrue { it.hasNameEndingWith("Factory") }
        }

        @Test
        @DisplayName("Strategy packages must exist and classes residing in them must end with 'Strategy'")
        fun `classes residing in strategy packages must end with Strategy`() {
            val strategyClasses = (projectProductionScope.classes() + projectProductionScope.interfaces())
                .filter { it.resideInPackage("..domain.usecase..strategy..") }
                .filter { it.isTopLevel }

            Assertions.assertTrue(
                strategyClasses.isNotEmpty(),
                "Strategy package does not exist or has no classes"
            )

            strategyClasses.assertTrue { it.hasNameEndingWith("Strategy") }
        }

        @Test
        @DisplayName("Repository interfaces must end with 'Repository'")
        fun `repository interfaces must end with Repository`() {
            domainProductionScope
                .interfaces()
                .filter { it.resideInPackage("..domain.repository..") }
                .assertTrue { it.hasNameEndingWith("Repository") }
        }

        @Test
        @DisplayName("Top-level ViewModel classes must end with 'ViewModel'")
        fun `viewmodels must end with ViewModel`() {
            projectProductionScope
                .classes()
                .filter { it.resideInPackage("..presentation.viewmodel") }
                .filter { it.isTopLevel }
                // Exclude data classes — they are parameter bundles (e.g. BalancesUseCases),
                // not ViewModels
                .filter { !it.hasDataModifier }
                .assertTrue { it.hasNameEndingWith("ViewModel") }
        }

        @Test
        @DisplayName("Top-level event handler interfaces must end with 'EventHandler'")
        fun `event handlers must end with EventHandler`() {
            projectProductionScope
                .interfaces()
                .filter { it.resideInPackage("..presentation.viewmodel.handler..") }
                .filter { it.isTopLevel }
                // Only check interfaces that already signal "handler" intent by name
                // Sealed callback/action types (e.g. PostConfigAction) live here too and are exempt
                .filter { it.hasNameContaining("Handler") }
                .assertTrue { it.hasNameEndingWith("EventHandler") }
        }

        @Test
        @DisplayName("Presentation-layer mapper classes must end with 'UiMapper' or 'UiMapperImpl'")
        fun `presentation layer mappers must end with UiMapper or UiMapperImpl`() {
            projectProductionScope
                .classes()
                .filter { it.resideInPackage("..presentation.mapper..") }
                .filter { it.isTopLevel }
                .assertTrue { it.hasNameEndingWith("UiMapper") || it.hasNameEndingWith("UiMapperImpl") }
        }

        @Test
        @DisplayName("Cloud data sources must end with 'DataSource'")
        fun `cloud data sources must end with DataSource`() {
            domainProductionScope
                .interfaces()
                .filter { it.resideInPackage("..domain.datasource..") }
                .assertTrue { it.hasNameEndingWith("DataSource") }
        }
    }

    @Nested
    @DisplayName("ViewModel Dependency Rules")
    inner class ViewModelDependencyRules {

        @Test
        @DisplayName("ViewModels must NOT import Repository classes directly")
        fun `viewmodels must not import repositories`() {
            projectProductionScope
                .classes()
                .withNameEndingWith("ViewModel")
                .assertTrue {
                    it.containingFile.imports.none { import ->
                        import.name.contains(".repository.")
                    }
                }
        }

        @Test
        @DisplayName("ViewModels must NOT import data layer packages")
        fun `viewmodels must not import data layer`() {
            projectProductionScope
                .classes()
                .withNameEndingWith("ViewModel")
                .assertTrue {
                    it.containingFile.imports.none { import ->
                        import.name.contains("splittrip.data.")
                    }
                }
        }

        @Test
        @DisplayName("ViewModels must NOT import Android Context")
        fun `viewmodels must not import context`() {
            projectProductionScope
                .classes()
                .withNameEndingWith("ViewModel")
                .assertTrue {
                    it.containingFile.imports.none { import ->
                        import.name == "android.content.Context"
                    }
                }
        }

        @Test
        @DisplayName("ViewModels must NOT import LocaleProvider")
        fun `viewmodels must not import locale provider`() {
            projectProductionScope
                .classes()
                .withNameEndingWith("ViewModel")
                .assertTrue {
                    it.containingFile.imports.none { import ->
                        import.name.contains("LocaleProvider")
                    }
                }
        }
    }

    @Nested
    @DisplayName("Handler Isolation Rules")
    inner class HandlerIsolationRules {

        @Test
        @DisplayName("Event handlers must NOT depend on other event handlers")
        fun `event handlers must not depend on other event handlers`() {
            projectProductionScope
                .classes()
                .withNameEndingWith("EventHandler")
                .filter { it.resideInPackage("..handler..") }
                .assertTrue { clazz ->
                    val constructorParamTypes = clazz.constructors.flatMap { constructor ->
                        constructor.parameters.map { it.type.name }
                    }
                    constructorParamTypes.none { it.endsWith("EventHandler") }
                }
        }
    }

    @Nested
    @DisplayName("Feature Module Isolation")
    inner class FeatureModuleIsolation {

        @Test
        @DisplayName("Feature modules must NOT import from data layer")
        fun `feature modules must not import from data layer`() {
            projectProductionScope
                .files
                .filter { it.hasPackage("..features..") }
                .assertTrue {
                    it.imports.none { import ->
                        import.name.contains("splittrip.data.")
                    }
                }
        }

        @Test
        @DisplayName("Feature modules must NOT import from other feature modules")
        fun `feature modules must not import from other features`() {
            val featurePackages = listOf(
                "features.authentication",
                "features.balance",
                "features.contribution",
                "features.expense",
                "features.group",
                "features.main",
                "features.onboarding",
                "features.profile",
                "features.settings",
                "features.subunit",
                "features.withdrawal"
            )

            projectProductionScope
                .files
                .filter { file ->
                    featurePackages.any { file.hasPackage("..$it..") }
                }
                .assertTrue { file ->
                    // Determine which feature this file belongs to
                    val ownFeature = featurePackages.firstOrNull {
                        file.hasPackage("..$it..")
                    }
                    // Verify it doesn't import from any OTHER feature
                    file.imports.none { import ->
                        featurePackages
                            .filter { it != ownFeature }
                            .any { otherFeature ->
                                import.name.contains(otherFeature)
                            }
                    }
                }
        }

        @Test
        @DisplayName("Features parent module must NOT contain production source files")
        fun `features parent module must not contain source files`() {
            val parentModuleFiles = projectProductionScope
                .files
                .filter { it.projectPath.startsWith("features/src/") }

            Assertions.assertTrue(
                parentModuleFiles.isEmpty(),
                "Features parent module (:features) must not contain production source files. " +
                    "It is a pure dependency-aggregation module. " +
                    "Found: ${parentModuleFiles.map { it.projectPath }}"
            )
        }
    }

    @Nested
    @DisplayName("Domain Layer Purity")
    inner class DomainLayerPurity {

        @Test
        @DisplayName("Domain module must NOT import Android framework classes")
        fun `domain module must not import android framework`() {
            domainProductionScope
                .files
                .assertTrue {
                    it.imports.none { import ->
                        import.name.startsWith("android.") ||
                            import.name.startsWith("androidx.")
                    }
                }
        }

        @Test
        @DisplayName("Domain module must NOT import data layer")
        fun `domain module must not import data layer`() {
            domainProductionScope
                .files
                .assertTrue {
                    it.imports.none { import ->
                        import.name.contains("splittrip.data.")
                    }
                }
        }

        @Test
        @DisplayName("Domain services must NOT contain formatting methods")
        fun `domain services must not contain formatting methods`() {
            val forbiddenMethodNames = listOf(
                "formatShareForInput",
                "formatAmountForDisplay",
                "formatNumberForDisplay",
                "formatForInput",
                "formatForDisplay"
            )

            domainProductionScope
                .classes()
                .filter { it.resideInPackage("..domain.service..") }
                .assertTrue { clazz ->
                    clazz.functions().none { func ->
                        forbiddenMethodNames.any { forbidden ->
                            func.name.contains(forbidden, ignoreCase = true)
                        }
                    }
                }
        }
    }

    @Nested
    @DisplayName("Screen Statelessness")
    inner class ScreenStatelessness {

        @Test
        @DisplayName("Screen composables must NOT directly import ViewModel classes")
        fun `screen files must not import viewmodel`() {
            projectProductionScope
                .files
                .filter { it.name.endsWith("Screen") }
                // MainScreen is the root navigation orchestrator — it hosts ViewModels
                // by design and is exempt from the pure-screen statelessness rule
                .filter { it.name != "MainScreen" }
                .assertTrue {
                    it.imports.none { import ->
                        import.name.endsWith("ViewModel") ||
                            import.name.contains("koinViewModel")
                    }
                }
        }
    }

    @Nested
    @DisplayName("File Size Limits")
    inner class FileSizeLimits {

        private val maxProductionFileLines = 600

        @Test
        @DisplayName("Production source files must not exceed 600 lines")
        fun `production files must not exceed max line threshold`() {
            projectProductionScope
                .files
                .assertTrue {
                    val lineCount = it.text.lines().size
                    lineCount <= maxProductionFileLines
                }
        }
    }

    @Nested
    @DisplayName("Kotlin Language Constraints")
    inner class KotlinLanguageConstraints {

        /**
         * The `..<` (rangeUntil) operator — stabilised in Kotlin 1.9 — is not yet
         * supported by SonarQube's Kotlin parser (as of scanner 7.2.x), causing
         * entire files to be skipped during analysis. Use `until` instead, which
         * is functionally identical for integer ranges and universally supported.
         */
        @Test
        @DisplayName("Production code must use 'until' instead of '..<' (rangeUntil operator)")
        fun `source code must not use rangeUntil operator`() {
            projectProductionScope
                .files
                .assertTrue { file ->
                    !file.text.contains("..<")
                }
        }
    }

    @Nested
    @DisplayName("Compose Constraints")
    inner class ComposeConstraints {

        @Test
        @DisplayName(
            "Presentation files containing composables must only contain a single composable matching the file name"
        )
        fun `presentation files containing composables must only contain a single composable matching the file name`() {
            projectProductionScope
                .files
                .filter { it.hasPackage("..presentation..") }
                .filter { !it.projectPath.contains("src/debug") }
                .filter { !it.projectPath.contains("core/design-system") }
                .filter { !it.hasPackage("..preview..") }
                .filter { file ->
                    file.functions(includeNested = true).any { func ->
                        func.annotations.any { it.name == "Composable" }
                    }
                }
                .filter { file ->
                    // MainScreen is a root nav host orchestrator and is exempt
                    file.name != "MainScreen"
                }
                .assertTrue { file ->
                    val composables = file.functions(includeNested = true).filter { func ->
                        func.annotations.any { it.name == "Composable" }
                    }
                    val isCompliant = composables.size == 1 && composables.first().name == file.name
                    if (!isCompliant) {
                        println(
                            "Violation: ${file.name} in ${file.projectPath} " +
                                "has ${composables.size} composables: " +
                                "${composables.map { it.name }}"
                        )
                    }
                    isCompliant
                }
        }
    }
}
