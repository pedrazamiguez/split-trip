package es.pedrazamiguez.splittrip.data.firebase.messaging.handler.impl

import android.content.Context
import es.pedrazamiguez.splittrip.data.firebase.R
import es.pedrazamiguez.splittrip.domain.constant.NotificationChannelId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ScheduledExpenseEffectiveHandler")
class ScheduledExpenseEffectiveHandlerTest {

    private val context: Context = mockk()
    private val handler = ScheduledExpenseEffectiveHandler(context)

    @Test
    @DisplayName("handle creates correct NotificationContent with all data fields")
    fun `handle creates correct NotificationContent with all data fields`() {
        every { context.getString(R.string.notification_scheduled_effective_title) } returns "Payment due today"
        every { context.getString(R.string.notification_scheduled_effective_body) } returns
            "Your scheduled expense is due today."

        val data = mapOf(
            "groupId" to "group1",
            "expenseId" to "exp1",
            "deepLink" to "splittrip://expense/exp1"
        )

        val result = handler.handle(data)

        assertEquals("Payment due today", result.title)
        assertEquals("Your scheduled expense is due today.", result.body)
        assertEquals(NotificationChannelId.EXPENSES, result.channelId)
        assertEquals("group1", result.groupId)
        assertEquals("splittrip://expense/exp1", result.deepLink)
        assertNotNull(result.notificationId)
    }

    @Test
    @DisplayName("handle produces stable notification ID for same group and expense")
    fun `handle produces stable notification ID for same group and expense`() {
        every { context.getString(R.string.notification_scheduled_effective_title) } returns "title"
        every { context.getString(R.string.notification_scheduled_effective_body) } returns "body"

        val data = mapOf("groupId" to "group1", "expenseId" to "exp1")

        val result1 = handler.handle(data)
        val result2 = handler.handle(data)

        assertEquals(result1.notificationId, result2.notificationId)
    }

    @Test
    @DisplayName("handle falls back to generated deep link when not provided")
    fun `handle falls back to generated deep link when not provided`() {
        every { context.getString(R.string.notification_scheduled_effective_title) } returns "title"
        every { context.getString(R.string.notification_scheduled_effective_body) } returns "body"

        val data = mapOf("groupId" to "group1", "expenseId" to "exp1")

        val result = handler.handle(data)

        assertEquals("splittrip://groups/group1/expenses/exp1", result.deepLink)
    }

    @Test
    @DisplayName("handle uses null deepLink when groupId or expenseId is missing")
    fun `handle uses null deepLink when groupId or expenseId is missing`() {
        every { context.getString(R.string.notification_scheduled_effective_title) } returns "title"
        every { context.getString(R.string.notification_scheduled_effective_body) } returns "body"

        val data = mapOf("groupId" to "group1")

        val result = handler.handle(data)

        assertNull(result.deepLink)
    }
}
