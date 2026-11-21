package es.pedrazamiguez.splittrip.core.logging

object MonetizationTelemetry {

    object Event {
        const val PAYWALL_VIEWED = "paywall_viewed"
        const val BILLING_INTERVAL_SELECTED = "billing_interval_selected"
        const val PURCHASE_INITIATED = "purchase_initiated"
        const val PURCHASE_COMPLETED = "purchase_completed"
        const val PURCHASE_PENDING = "purchase_pending"
        const val PURCHASE_CANCELED = "purchase_canceled"
        const val PURCHASE_FAILED = "purchase_failed"
        const val RESTORE_PURCHASES_ATTEMPTED = "restore_purchases_attempted"
        const val RESTORE_PURCHASES_SUCCEEDED = "restore_purchases_succeeded"
        const val RESTORE_PURCHASES_EMPTY = "restore_purchases_empty"
        const val RESTORE_PURCHASES_FAILED = "restore_purchases_failed"
        const val MANAGE_SUBSCRIPTION_OPENED = "manage_subscription_opened"

        const val UPGRADE_ROUTED = "upgrade_routed"
        const val FEATURE_GATE_BLOCKED = "feature_gate_blocked"

        const val AD_LOADED = "ad_loaded"
        const val AD_LOAD_FAILED = "ad_load_failed"
        const val AD_ELIGIBILITY_EVALUATED = "ad_eligibility_evaluated"
        const val AD_IMPRESSION = "ad_impression"
        const val AD_DISMISSED = "ad_dismissed"
        const val AD_SHOW_FAILED = "ad_show_failed"
    }

    object Param {
        const val ENTRY_SOURCE = "entry_source"
        const val DESTINATION = "destination"
        const val IS_ANONYMOUS = "is_anonymous"
        const val CURRENT_TIER = "current_tier"
        const val TARGET_TIER = "target_tier"
        const val BILLING_INTERVAL = "billing_interval"
        const val IS_ALREADY_OWNED = "is_already_owned"
        const val FAILURE_STAGE = "failure_stage"
        const val GATE_TYPE = "gate_type"
        const val GATE_NAME = "gate_name"
        const val ENTRY_POINT = "entry_point"
        const val UPGRADE_PROMPTED = "upgrade_prompted"
        const val AD_TYPE = "ad_type"
        const val IS_ELIGIBLE = "is_eligible"
        const val REASON = "reason"
        const val ERROR_CODE = "error_code"
    }

    object EntrySource {
        const val SETTINGS = "settings"
        const val GROUPS_FAB = "groups_fab"
        const val GROUP_CREATION = "group_creation"
        const val GROUP_MEMBER_LIMIT = "group_member_limit"
        const val GROUP_COVER_UPLOAD = "group_cover_upload"
        const val SUBUNIT_MANAGEMENT = "subunit_management"
        const val SUBUNIT_CREATION = "subunit_creation"
        const val AI_RECEIPT_SCANNING = "ai_receipt_scanning"
        const val MANUAL_RECEIPT_ATTACHMENT = "manual_receipt_attachment"
        const val DIRECT = "direct"
    }

    object Destination {
        const val SUBSCRIPTIONS = "subscriptions"
        const val ACCOUNT_STATUS = "account_status"
    }

    object GateType {
        const val FEATURE = "feature"
        const val LIMIT = "limit"
    }

    object GateEntryPoint {
        const val GROUPS_FAB = "groups_fab"
        const val GROUP_CREATE_INIT = "group_create_init"
        const val GROUP_SUBMIT = "group_submit"
        const val GROUP_COVER_LOCKED_PREVIEW = "group_cover_locked_preview"
        const val GROUP_COVER_PICK = "group_cover_pick"
        const val GROUP_MEMBER_SELECT = "group_member_select"
        const val GROUP_MEMBER_SCAN = "group_member_scan"
        const val GROUP_MEMBER_UPSELL = "group_member_upsell"
        const val GROUP_MEMBER_SUBMIT = "group_member_submit"
        const val AI_MODE_TOGGLE = "ai_mode_toggle"
        const val AI_RECEIPT_ATTACH = "ai_receipt_attach"
        const val MANUAL_RECEIPT_ATTACH = "manual_receipt_attach"
        const val SUBUNIT_FAB = "subunit_fab"
        const val SUBUNIT_CREATE_EVENT = "subunit_create_event"
        const val SUBUNIT_CREATE_INIT = "subunit_create_init"
        const val SUBUNIT_SUBMIT = "subunit_submit"
    }

    object AdType {
        const val INTERSTITIAL = "interstitial"
        const val BANNER = "banner"
    }

    object AdEligibilityReason {
        const val ELIGIBLE = "eligible"
        const val ADS_DISABLED = "ads_disabled"
        const val FREQUENCY_CAP = "frequency_cap"
        const val MIN_INTERVAL_COOLDOWN = "min_interval_cooldown"
        const val NO_CACHED_AD = "no_cached_ad"
    }

    object FailureStage {
        const val PRODUCT_UNAVAILABLE = "product_unavailable"
        const val LAUNCH_BILLING_FLOW = "launch_billing_flow"
        const val PURCHASE_UPDATE = "purchase_update"
        const val TIER_PERSISTENCE = "tier_persistence"
        const val AD_LOAD_EXCEPTION = "ad_load_exception"
    }
}
