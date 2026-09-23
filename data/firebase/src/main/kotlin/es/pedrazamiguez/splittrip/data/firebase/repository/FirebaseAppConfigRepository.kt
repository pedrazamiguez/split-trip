package es.pedrazamiguez.splittrip.data.firebase.repository

import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import es.pedrazamiguez.splittrip.data.firebase.R
import es.pedrazamiguez.splittrip.domain.model.DeveloperInfo
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class FirebaseAppConfigRepository(
    private val remoteConfig: FirebaseRemoteConfig
) : AppConfigRepository {

    private val gson = Gson()

    private val _defaultCurrencyCode = MutableStateFlow(DEFAULT_CURRENCY)
    override val defaultCurrencyCode: StateFlow<String> = _defaultCurrencyCode.asStateFlow()

    private val _balanceComputationDebounceMs = MutableStateFlow(DEFAULT_BALANCE_DEBOUNCE_MS)
    override val balanceComputationDebounceMs: StateFlow<Long> = _balanceComputationDebounceMs.asStateFlow()

    private val _maxMembersPerGroup = MutableStateFlow(DEFAULT_MAX_MEMBERS_PER_GROUP)
    override val maxMembersPerGroup: StateFlow<Int> = _maxMembersPerGroup.asStateFlow()

    private val _subscriptionGatingEnabled = MutableStateFlow(DEFAULT_SUBSCRIPTION_GATING_ENABLED)
    override val subscriptionGatingEnabled: StateFlow<Boolean> = _subscriptionGatingEnabled.asStateFlow()

    private val _maxOwnedGroupsFree = MutableStateFlow(DEFAULT_MAX_OWNED_GROUPS_FREE)
    override val maxOwnedGroupsFree: StateFlow<Int> = _maxOwnedGroupsFree.asStateFlow()

    private val _maxOwnedGroupsPro = MutableStateFlow(DEFAULT_MAX_OWNED_GROUPS_PRO)
    override val maxOwnedGroupsPro: StateFlow<Int> = _maxOwnedGroupsPro.asStateFlow()

    private val _maxMembersPerGroupFree = MutableStateFlow(DEFAULT_MAX_MEMBERS_PER_GROUP_FREE)
    override val maxMembersPerGroupFree: StateFlow<Int> = _maxMembersPerGroupFree.asStateFlow()

    private val _maxMembersPerGroupPro = MutableStateFlow(DEFAULT_MAX_MEMBERS_PER_GROUP_PRO)
    override val maxMembersPerGroupPro: StateFlow<Int> = _maxMembersPerGroupPro.asStateFlow()

    private val _aiReceiptMonthlyLimitFree = MutableStateFlow(DEFAULT_AI_RECEIPT_MONTHLY_LIMIT_FREE)
    override val aiReceiptMonthlyLimitFree: StateFlow<Int> = _aiReceiptMonthlyLimitFree.asStateFlow()

    private val _aiReceiptMonthlyLimitPro = MutableStateFlow(DEFAULT_AI_RECEIPT_MONTHLY_LIMIT_PRO)
    override val aiReceiptMonthlyLimitPro: StateFlow<Int> = _aiReceiptMonthlyLimitPro.asStateFlow()

    private val _extractedDateMaxFutureDays = MutableStateFlow(DEFAULT_EXTRACTED_DATE_MAX_FUTURE_DAYS)
    override val extractedDateMaxFutureDays: StateFlow<Int> = _extractedDateMaxFutureDays.asStateFlow()

    private val _supportEmailAddress = MutableStateFlow(DEFAULT_SUPPORT_EMAIL)
    override val supportEmailAddress: StateFlow<String> = _supportEmailAddress.asStateFlow()

    private val _settlementNudgeRateLimitHours = MutableStateFlow(DEFAULT_SETTLEMENT_NUDGE_RATE_LIMIT_HOURS)
    override val settlementNudgeRateLimitHours: StateFlow<Long> = _settlementNudgeRateLimitHours.asStateFlow()

    private val _ocrSafetyFalsePositivesBlacklist = MutableStateFlow(DEFAULT_OCR_SAFETY_FALSE_POSITIVES_BLACKLIST)
    override val ocrSafetyFalsePositivesBlacklist: StateFlow<List<String>> =
        _ocrSafetyFalsePositivesBlacklist.asStateFlow()

    private val _developerInfo = MutableStateFlow(DEFAULT_DEVELOPER_INFO)
    override val developerInfo: StateFlow<DeveloperInfo> = _developerInfo.asStateFlow()

    private val _adsEnabled = MutableStateFlow(DEFAULT_ADS_ENABLED)
    override val adsEnabled: StateFlow<Boolean> = _adsEnabled.asStateFlow()

    private val _admobTestModeEnabled = MutableStateFlow(DEFAULT_ADMOB_TEST_MODE_ENABLED)
    override val admobTestModeEnabled: StateFlow<Boolean> = _admobTestModeEnabled.asStateFlow()

    private val _admobBannerAdUnitId = MutableStateFlow(DEFAULT_BANNER_AD_UNIT_ID)
    override val admobBannerAdUnitId: StateFlow<String> = _admobBannerAdUnitId.asStateFlow()

    private val _admobInterstitialAdUnitId = MutableStateFlow(DEFAULT_INTERSTITIAL_AD_UNIT_ID)
    override val admobInterstitialAdUnitId: StateFlow<String> = _admobInterstitialAdUnitId.asStateFlow()

    private val _adInterstitialActionFrequency = MutableStateFlow(DEFAULT_INTERSTITIAL_ACTION_FREQUENCY)
    override val adInterstitialActionFrequency: StateFlow<Int> = _adInterstitialActionFrequency.asStateFlow()

    private val _adInterstitialMinIntervalSeconds = MutableStateFlow(DEFAULT_INTERSTITIAL_MIN_INTERVAL_SECONDS)
    override val adInterstitialMinIntervalSeconds: StateFlow<Long> = _adInterstitialMinIntervalSeconds.asStateFlow()

    init {
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        updateFlowsFromConfig()
        setupRealTimeUpdateListener()
    }

    override suspend fun fetchConfiguration(): Boolean {
        return try {
            val updated = remoteConfig.fetchAndActivate().await()
            if (updated) {
                Timber.d("Firebase Remote Config: Fetch and activate successful.")
                updateFlowsFromConfig()
            }
            updated
        } catch (e: Exception) {
            Timber.e(e, "Firebase Remote Config: Fetch failed.")
            false
        }
    }

    private fun setupRealTimeUpdateListener() {
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Timber.d("Firebase Remote Config real-time update: keys=${configUpdate.updatedKeys}")
                remoteConfig.activate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        updateFlowsFromConfig()
                    }
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Timber.w(error, "Firebase Remote Config real-time update error.")
            }
        })
    }

    private fun updateFlowsFromConfig() {
        updateGeneralConfigFlows()
        updateAdvertisingFlows()
        updateTierLimitFlows()
        updateOcrAndDeveloperFlows()
    }

    private fun updateGeneralConfigFlows() {
        _defaultCurrencyCode.value =
            remoteConfig.getString("default_currency_code").takeIf { it.isNotBlank() } ?: DEFAULT_CURRENCY
        val debounce = remoteConfig.getLong("balance_computation_debounce_ms")
        _balanceComputationDebounceMs.value = if (debounce > 0) debounce else DEFAULT_BALANCE_DEBOUNCE_MS
        val maxMembers = remoteConfig.getLong("max_members_per_group").toInt()
        _maxMembersPerGroup.value = if (maxMembers > 0) maxMembers else DEFAULT_MAX_MEMBERS_PER_GROUP
        val maxFutureDays = remoteConfig.getLong("extracted_date_max_future_days").toInt()
        _extractedDateMaxFutureDays.value =
            if (maxFutureDays > 0) maxFutureDays else DEFAULT_EXTRACTED_DATE_MAX_FUTURE_DAYS
        _supportEmailAddress.value =
            remoteConfig.getString("support_email_address").takeIf { it.isNotBlank() } ?: DEFAULT_SUPPORT_EMAIL
        val nudgeLimitHours = remoteConfig.getLong("settlement_nudge_rate_limit_hours")
        _settlementNudgeRateLimitHours.value =
            if (nudgeLimitHours > 0) nudgeLimitHours else DEFAULT_SETTLEMENT_NUDGE_RATE_LIMIT_HOURS
    }

    private fun updateAdvertisingFlows() {
        val adsEnabledStr = remoteConfig.getString("ads_enabled").trim()
        _adsEnabled.value = if (adsEnabledStr.isNotBlank()) {
            remoteConfig.getBoolean("ads_enabled")
        } else {
            DEFAULT_ADS_ENABLED
        }

        val testModeStr = remoteConfig.getString("admob_test_mode_enabled").trim()
        val isTestMode = if (testModeStr.isNotBlank()) {
            remoteConfig.getBoolean("admob_test_mode_enabled")
        } else {
            DEFAULT_ADMOB_TEST_MODE_ENABLED
        }
        _admobTestModeEnabled.value = isTestMode

        val configuredBannerId =
            remoteConfig.getString("admob_banner_ad_unit_id").takeIf { it.isNotBlank() } ?: DEFAULT_BANNER_AD_UNIT_ID
        val configuredInterstitialId =
            remoteConfig.getString("admob_interstitial_ad_unit_id").takeIf { it.isNotBlank() }
                ?: DEFAULT_INTERSTITIAL_AD_UNIT_ID

        _admobBannerAdUnitId.value = if (isTestMode) SAMPLE_BANNER_AD_UNIT_ID else configuredBannerId
        _admobInterstitialAdUnitId.value = if (isTestMode) SAMPLE_INTERSTITIAL_AD_UNIT_ID else configuredInterstitialId

        val actionFrequency = remoteConfig.getLong("ad_interstitial_action_frequency").toInt()
        _adInterstitialActionFrequency.value =
            if (actionFrequency > 0) actionFrequency else DEFAULT_INTERSTITIAL_ACTION_FREQUENCY
        val minIntervalSeconds = remoteConfig.getLong("ad_interstitial_min_interval_seconds")
        _adInterstitialMinIntervalSeconds.value =
            if (minIntervalSeconds > 0) minIntervalSeconds else DEFAULT_INTERSTITIAL_MIN_INTERVAL_SECONDS
    }

    private fun updateTierLimitFlows() {
        val gatingStr = remoteConfig.getString("subscription_gating_enabled").trim()
        _subscriptionGatingEnabled.value = if (gatingStr.isNotBlank()) {
            remoteConfig.getBoolean("subscription_gating_enabled")
        } else {
            DEFAULT_SUBSCRIPTION_GATING_ENABLED
        }

        val maxOwnedFree = remoteConfig.getLong("max_owned_groups_free").toInt()
        _maxOwnedGroupsFree.value = if (maxOwnedFree > 0) maxOwnedFree else DEFAULT_MAX_OWNED_GROUPS_FREE

        val maxOwnedPro = remoteConfig.getLong("max_owned_groups_pro").toInt()
        _maxOwnedGroupsPro.value = if (maxOwnedPro > 0) maxOwnedPro else DEFAULT_MAX_OWNED_GROUPS_PRO

        val maxMembersFree = remoteConfig.getLong("max_members_per_group_free").toInt()
        _maxMembersPerGroupFree.value = if (maxMembersFree > 0) maxMembersFree else DEFAULT_MAX_MEMBERS_PER_GROUP_FREE

        val maxMembersPro = remoteConfig.getLong("max_members_per_group_pro").toInt()
        _maxMembersPerGroupPro.value = if (maxMembersPro > 0) maxMembersPro else DEFAULT_MAX_MEMBERS_PER_GROUP_PRO

        val aiLimitFree = remoteConfig.getLong("ai_receipt_monthly_limit_free").toInt()
        _aiReceiptMonthlyLimitFree.value = if (aiLimitFree >= 0) aiLimitFree else DEFAULT_AI_RECEIPT_MONTHLY_LIMIT_FREE

        val aiLimitPro = remoteConfig.getLong("ai_receipt_monthly_limit_pro").toInt()
        _aiReceiptMonthlyLimitPro.value = if (aiLimitPro > 0) aiLimitPro else DEFAULT_AI_RECEIPT_MONTHLY_LIMIT_PRO
    }

    private fun updateOcrAndDeveloperFlows() {
        val blacklistStr = remoteConfig.getString("ocr_safety_false_positives_blacklist")
        _ocrSafetyFalsePositivesBlacklist.value = if (blacklistStr.isNotBlank()) {
            blacklistStr.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        } else {
            DEFAULT_OCR_SAFETY_FALSE_POSITIVES_BLACKLIST
        }
        val developerInfoJson = remoteConfig.getString("developer_info_json")
        _developerInfo.value = parseDeveloperInfo(developerInfoJson)
    }

    private fun parseDeveloperInfo(jsonStr: String): DeveloperInfo {
        if (jsonStr.isBlank()) return DEFAULT_DEVELOPER_INFO
        return try {
            val dto = gson.fromJson(jsonStr, DeveloperInfoDto::class.java)
            dto?.toDomain() ?: DEFAULT_DEVELOPER_INFO
        } catch (e: Exception) {
            Timber.w(e, "Firebase Remote Config: Failed to parse developer_info_json.")
            DEFAULT_DEVELOPER_INFO
        }
    }

    companion object {
        private const val DEFAULT_CURRENCY = "EUR"
        private const val DEFAULT_BALANCE_DEBOUNCE_MS = 300L
        private const val DEFAULT_MAX_MEMBERS_PER_GROUP = 20
        private const val DEFAULT_SUBSCRIPTION_GATING_ENABLED = true
        private const val DEFAULT_MAX_OWNED_GROUPS_FREE = 1
        private const val DEFAULT_MAX_OWNED_GROUPS_PRO = 100
        private const val DEFAULT_MAX_MEMBERS_PER_GROUP_FREE = 4
        private const val DEFAULT_MAX_MEMBERS_PER_GROUP_PRO = 20
        private const val DEFAULT_AI_RECEIPT_MONTHLY_LIMIT_FREE = 0
        private const val DEFAULT_AI_RECEIPT_MONTHLY_LIMIT_PRO = 100
        private const val DEFAULT_EXTRACTED_DATE_MAX_FUTURE_DAYS = 30
        private const val DEFAULT_SUPPORT_EMAIL = "support@splittrip.eu"
        private const val DEFAULT_SETTLEMENT_NUDGE_RATE_LIMIT_HOURS = 24L
        private val DEFAULT_OCR_SAFETY_FALSE_POSITIVES_BLACKLIST = listOf("razor", "private", "toothbrushes")
        private const val DEFAULT_ADS_ENABLED = true
        private const val DEFAULT_ADMOB_TEST_MODE_ENABLED = false
        private const val SAMPLE_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val SAMPLE_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val DEFAULT_BANNER_AD_UNIT_ID = "ca-app-pub-9638507020441461/2775424807"
        private const val DEFAULT_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9638507020441461/6643262487"
        private const val DEFAULT_INTERSTITIAL_ACTION_FREQUENCY = 2
        private const val DEFAULT_INTERSTITIAL_MIN_INTERVAL_SECONDS = 90L

        private const val LANG_EN = "en"
        private const val LANG_ES = "es"
        private const val LANG_ES_RAN = "es-rAN"

        val DEFAULT_DEVELOPER_INFO = DeveloperInfo(
            name = "Andrés Pedraza Míguez",
            avatarUrl = "",
            githubUrl = "https://github.com/pedrazamiguez",
            splitTripRepoUrl = "https://github.com/pedrazamiguez/split-trip",
            linkedinUrl = "https://www.linkedin.com/in/pedrazamiguez",
            portfolioUrl = "https://pedrazamiguez.github.io",
            roleMap = mapOf(
                LANG_EN to "Senior Java & Kotlin Engineer",
                LANG_ES to "Ingeniero Senior de Java y Kotlin",
                LANG_ES_RAN to "Inheniero Senior de Java y Kotlin"
            ),
            bioMap = mapOf(
                LANG_EN to "Senior Backend Engineer with over 14 years of experience designing scalable systems, " +
                    "now leveraging a strong hybrid skill set in Java (Backend) and Kotlin (Mobile/Android). " +
                    "Proven track record working as a Remote Contractor for UK-based companies, delivering " +
                    "high-quality solutions in English-speaking environments.\n\n" +
                    "Specialises in Java 21, Spring Boot, and Hexagonal Architecture, with recent hands-on " +
                    "leadership in Android Native (Jetpack Compose). Known for stepping into complex, " +
                    "legacy environments to refactor code, mentor senior peers, and drive delivery. " +
                    "Passionate about software craftsmanship, automated testing, and solving critical " +
                    "business problems across the full stack.",
                LANG_ES to "Ingeniero Backend Senior con más de 14 años de experiencia diseñando sistemas " +
                    "escalables, aprovechando un sólido conjunto de habilidades híbridas en Java (Backend) y " +
                    "Kotlin (Móvil/Android). Trayectoria contrastada trabajando como contratista remoto para " +
                    "empresas del Reino Unido, ofreciendo soluciones de alta calidad en entornos " +
                    "angloparlantes.\n\n" +
                    "Especializado en Java 21, Spring Boot y Arquitectura Hexagonal, con liderazgo práctico " +
                    "reciente en proyectos de Android Nativo (Jetpack Compose). Reconocido por incorporarse a " +
                    "entornos heredados y complejos para refactorizar código, mentorizar a compañeros y " +
                    "acelerar entregas. Apasionado por la excelencia en el desarrollo de software, los tests " +
                    "automatizados y la resolución de problemas de negocio críticos en todo el stack.",
                LANG_ES_RAN to "Inheniero Backend Senior con mâh de 14 añô de êpperiençia diçeñando çîttemâ " +
                    "êccalablê, aprobexando un çólido conhunto de abilidadê íbridâ en Java (Backend) y " +
                    "Kotlin (Móbî/Androîh). Trayêttoria contrâttá trabahando como contratîtta remoto pa " +
                    "empreçâ del Reino Unío, ofreçiendo çoluçionê de arta calidá en entônnô anglopâl-lantê.\n\n" +
                    "Êppeçialiçao en Java 21, Spring Boot y Arquitêttura Êççagonâh, con liderâggo práttico " +
                    "reçiente en proyêttô de Android Natibo (Jetpack Compose). Reconoçío por incorporarçe a " +
                    "entônnô eredáô y complehô pa refâttoriçâh código, mentoriçâh a compañerô y açelerâh " +
                    "entregâ. Apaçionao por la êççelençia en er deçarroyo de çôttware, lô têtts " +
                    "automatiçáô y la reçoluçión de problemâ de negoçio críticô en tó er stack."
            ),
            creditsMap = mapOf(
                LANG_EN to "SplitTrip is a modular Android application designed for travelers to manage shared " +
                    "expenses efficiently. It allows users to create expense groups, track spending in multiple " +
                    "currencies, calculate debts, and sync data across devices.\n\n" +
                    "Built with modern Android practices—including Jetpack Compose, Clean Architecture, and " +
                    "Offline-First principles—the app serves as a reference for scalable, multi-module Android " +
                    "development.",
                LANG_ES to "SplitTrip es una aplicación modular de Android diseñada para que los viajeros gestionen " +
                    "gastos compartidos de forma eficiente. Permite crear grupos de gastos, registrar pagos en " +
                    "múltiples monedas, calcular deudas y sincronizar datos entre dispositivos.\n\n" +
                    "Construida con las prácticas modernas de Android —incluyendo Jetpack Compose, Clean " +
                    "Architecture y principios Offline-First—, la aplicación sirve como referencia para el " +
                    "desarrollo escalable y multimódulo en Android.",
                LANG_ES_RAN to "SplîTTrîh êh una aplicaçión modulâh de Androîh diçeñá pa que lô biaherô hêttionen " +
                    "gâttô compartíô de forma efiçiente. Permite creâh grupô de gâttô, rehîttrâh pagô en " +
                    "múrtiplê monedâ, carculâh deudâ y çincroniçâh datô entre dîppoçitibô.\n\n" +
                    "Côttruida con lâ prátticâ modênnâ de Androîh —incluyendo Hêppack Compoçe, Clean " +
                    "Architecture y prinçipiô Ôffline-Firtt—, la aplicaçión çirbe como referençia pa er " +
                    "deçarroyo êccalable y murtimódulo en Androîh."
            ),
            copyrightMap = mapOf(
                LANG_EN to "© 2026 Andrés Pedraza Míguez.\nAll rights reserved.",
                LANG_ES to "© 2026 Andrés Pedraza Míguez.\nTodos los derechos reservados.",
                LANG_ES_RAN to "© 2026 Andrés Pedraza Míguez.\nTôh lô derexô reçerbaô."
            )
        )
    }
}

private data class DeveloperInfoDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("github_url") val githubUrl: String? = null,
    @SerializedName("splittrip_repo_url") val splitTripRepoUrl: String? = null,
    @SerializedName("linkedin_url") val linkedinUrl: String? = null,
    @SerializedName("portfolio_url") val portfolioUrl: String? = null,
    @SerializedName("role_map") val roleMap: Map<String, String>? = null,
    @SerializedName("bio_map") val bioMap: Map<String, String>? = null,
    @SerializedName("credits_map") val creditsMap: Map<String, String>? = null,
    @SerializedName("copyright_map") val copyrightMap: Map<String, String>? = null
) {
    fun toDomain(): DeveloperInfo = DeveloperInfo(
        name = name ?: FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.name,
        avatarUrl = avatarUrl ?: FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.avatarUrl,
        githubUrl = githubUrl ?: FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.githubUrl,
        splitTripRepoUrl = splitTripRepoUrl ?: FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.splitTripRepoUrl,
        linkedinUrl = linkedinUrl ?: FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.linkedinUrl,
        portfolioUrl = portfolioUrl ?: FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.portfolioUrl,
        roleMap = roleMap ?: FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.roleMap,
        bioMap = bioMap ?: FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.bioMap,
        creditsMap = creditsMap ?: FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.creditsMap,
        copyrightMap = copyrightMap ?: FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.copyrightMap
    )
}
