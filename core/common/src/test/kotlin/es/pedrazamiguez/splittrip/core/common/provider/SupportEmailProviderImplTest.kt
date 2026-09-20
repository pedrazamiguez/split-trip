package es.pedrazamiguez.splittrip.core.common.provider

import es.pedrazamiguez.splittrip.core.common.R
import es.pedrazamiguez.splittrip.core.common.provider.impl.SupportEmailProviderImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SupportEmailProviderImplTest {

    private val appMetadataProvider = mockk<AppMetadataProvider>()
    private val resourceProvider = mockk<ResourceProvider>()
    private val supportEmailAddressProvider = mockk<SupportEmailAddressProvider>()

    private lateinit var provider: SupportEmailProviderImpl

    @BeforeEach
    fun setUp() {
        every { appMetadataProvider.appVersionName } returns "1.2.3"
        every { appMetadataProvider.androidReleaseVersion } returns "14"
        every { appMetadataProvider.androidVersion } returns "34"
        every { appMetadataProvider.deviceModel } returns "Google Pixel 8"

        every { resourceProvider.getString(R.string.support_email_system_info_header) } returns
            "System Info (Do not modify):"
        every { resourceProvider.getString(R.string.support_email_app_version, "1.2.3") } returns "App Version: 1.2.3"
        every { resourceProvider.getString(R.string.support_email_os_version, "14", "34") } returns
            "OS Version: Android 14 (API 34)"
        every { resourceProvider.getString(R.string.support_email_device, "Google Pixel 8") } returns
            "Device: Google Pixel 8"

        every { supportEmailAddressProvider.getSupportEmailAddress() } returns "test-support@splittrip.eu"

        provider = SupportEmailProviderImpl(
            appMetadataProvider = appMetadataProvider,
            resourceProvider = resourceProvider,
            supportEmailAddressProvider = supportEmailAddressProvider
        )
    }

    @Test
    fun `buildBugReportEmail compiles correct support email data`() {
        // Arrange
        every { resourceProvider.getString(R.string.support_email_subject) } returns "SplitTrip Bug Report"

        // Act
        val email = provider.buildBugReportEmail()

        // Assert
        assertEquals("test-support@splittrip.eu", email.recipient)
        assertEquals("SplitTrip Bug Report", email.subject)

        // Verify body content format
        val expectedBodyPart = """
            System Info (Do not modify):
            App Version: 1.2.3
            OS Version: Android 14 (API 34)
            Device: Google Pixel 8
        """.trimIndent()

        assertTrue(email.body.contains(expectedBodyPart))
    }

    @Test
    fun `buildFeatureSuggestionEmail compiles correct support email data with template and metadata`() {
        // Arrange
        every { resourceProvider.getString(R.string.support_email_feature_subject) } returns "SplitTrip Feature Request"
        every { resourceProvider.getString(R.string.support_email_feature_body_template) } returns
            "What feature would you like to see?\n\n\nHow would this feature help you on your trips?\n\n"

        // Act
        val email = provider.buildFeatureSuggestionEmail()

        // Assert
        assertEquals("test-support@splittrip.eu", email.recipient)
        assertEquals("SplitTrip Feature Request", email.subject)

        val expectedTemplatePart = "What feature would you like to see?"
        val expectedSystemInfoPart = """
            System Info (Do not modify):
            App Version: 1.2.3
            OS Version: Android 14 (API 34)
            Device: Google Pixel 8
        """.trimIndent()

        assertTrue(email.body.contains(expectedTemplatePart))
        assertTrue(email.body.contains(expectedSystemInfoPart))
    }

    @Test
    fun `buildContactSupportEmail compiles correct support email data with template and metadata`() {
        // Arrange
        every { resourceProvider.getString(R.string.support_email_contact_subject) } returns "SplitTrip Support Request"
        every { resourceProvider.getString(R.string.support_email_contact_body_template) } returns
            "How can we help you?\n\n\nWhat happened or what questions do you have?\n\n"

        // Act
        val email = provider.buildContactSupportEmail()

        // Assert
        assertEquals("test-support@splittrip.eu", email.recipient)
        assertEquals("SplitTrip Support Request", email.subject)

        val expectedTemplatePart = "How can we help you?"
        val expectedSystemInfoPart = """
            System Info (Do not modify):
            App Version: 1.2.3
            OS Version: Android 14 (API 34)
            Device: Google Pixel 8
        """.trimIndent()

        assertTrue(email.body.contains(expectedTemplatePart))
        assertTrue(email.body.contains(expectedSystemInfoPart))
    }
}
