package es.pedrazamiguez.splittrip.core.common.presentation

import android.content.Context
import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UiTextTest {

    // ── DynamicString ─────────────────────────────────────────────────────

    @Nested
    inner class DynamicStringTests {

        @Test
        fun `DynamicString stores the value`() {
            val uiText = UiText.DynamicString("Hello")
            assertEquals("Hello", uiText.value)
        }

        @Test
        fun `DynamicString asString returns value directly without context`() {
            val context = mockk<Context>()
            val uiText = UiText.DynamicString("Direct text")

            assertEquals("Direct text", uiText.asString(context))
        }

        @Test
        fun `two DynamicStrings with same value are equal`() {
            val a = UiText.DynamicString("same")
            val b = UiText.DynamicString("same")
            assertEquals(a, b)
        }

        @Test
        fun `two DynamicStrings with different values are not equal`() {
            val a = UiText.DynamicString("one")
            val b = UiText.DynamicString("two")
            assertNotEquals(a, b)
        }
    }

    // ── StringResource ────────────────────────────────────────────────────

    @Nested
    inner class StringResourceTests {

        private val resId = 42
        private val resId2 = 99

        @Test
        fun `StringResource stores resId`() {
            val uiText = UiText.StringResource(resId)
            assertEquals(resId, uiText.resId)
        }

        @Test
        fun `StringResource stores args`() {
            val uiText = UiText.StringResource(resId, "arg1", 42)
            assertEquals(2, uiText.args.size)
            assertEquals("arg1", uiText.args[0])
            assertEquals(42, uiText.args[1])
        }

        @Test
        fun `asString resolves via context getString with args`() {
            val context = mockk<Context>()
            every { context.getString(resId, "name") } returns "Hello, name!"
            val uiText = UiText.StringResource(resId, "name")

            assertEquals("Hello, name!", uiText.asString(context))
        }

        @Test
        fun `asString resolves nested UiText arguments recursively`() {
            val context = mockk<Context>()
            val innerResId = 100
            val nestedUiText = UiText.StringResource(innerResId)

            every { context.getString(innerResId) } returns "world"
            every { context.getString(resId, "hello", "world") } returns "Message: hello world"

            val uiText = UiText.StringResource(resId, "hello", nestedUiText)

            assertEquals("Message: hello world", uiText.asString(context))
        }

        // equals

        @Test
        fun `equals returns true when same instance`() {
            val uiText = UiText.StringResource(resId)
            assertTrue(uiText == uiText)
        }

        @Test
        fun `equals returns true for same resId and same args`() {
            val a = UiText.StringResource(resId, "x")
            val b = UiText.StringResource(resId, "x")
            assertEquals(a, b)
        }

        @Test
        fun `equals returns false when resId differs`() {
            val a = UiText.StringResource(resId)
            val b = UiText.StringResource(resId2)
            assertNotEquals(a, b)
        }

        @Test
        fun `equals returns false when args differ`() {
            val a = UiText.StringResource(resId, "x")
            val b = UiText.StringResource(resId, "y")
            assertNotEquals(a, b)
        }

        @Test
        fun `equals returns false when other is not StringResource`() {
            val a = UiText.StringResource(resId)
            val other: Any = "not a StringResource"
            assertFalse(a == other)
        }

        // hashCode

        @Test
        fun `hashCode is consistent for same resId and args`() {
            val a = UiText.StringResource(resId, "x")
            val b = UiText.StringResource(resId, "x")
            assertEquals(a.hashCode(), b.hashCode())
        }

        @Test
        fun `hashCode differs for different resIds`() {
            val a = UiText.StringResource(resId)
            val b = UiText.StringResource(resId2)
            assertNotEquals(a.hashCode(), b.hashCode())
        }
    }

    // ── PluralResource ────────────────────────────────────────────────────

    @Nested
    inner class PluralResourceTests {

        private val resId = 77
        private val resId2 = 88

        @Test
        fun `asString resolves via resources getQuantityString without args`() {
            val resources = mockk<Resources>()
            val context = mockk<Context> {
                every { this@mockk.resources } returns resources
            }
            every { resources.getQuantityString(resId, 1) } returns "1 item"

            val uiText = UiText.PluralResource(resId, 1)

            assertEquals("1 item", uiText.asString(context))
        }

        @Test
        fun `asString resolves via resources getQuantityString with plain and nested UiText args`() {
            val resources = mockk<Resources>()
            val context = mockk<Context> {
                every { this@mockk.resources } returns resources
            }
            val nested = UiText.DynamicString("PDF")
            every { resources.getQuantityString(resId, 2, "2", "PDF") } returns "Quedan 2 PDF"

            val uiText = UiText.PluralResource(resId, 2, "2", nested)

            assertEquals("Quedan 2 PDF", uiText.asString(context))
        }

        @Test
        fun `equals and hashCode compare resId quantity and args`() {
            val same1 = UiText.PluralResource(resId, 1, "1")
            val same2 = UiText.PluralResource(resId, 1, "1")
            val diffQty = UiText.PluralResource(resId, 2, "1")
            val diffRes = UiText.PluralResource(resId2, 1, "1")
            val diffArgs = UiText.PluralResource(resId, 1, "2")

            assertTrue(same1 == same1)
            assertEquals(same1, same2)
            assertEquals(same1.hashCode(), same2.hashCode())
            assertNotEquals(same1, diffQty)
            assertNotEquals(same1, diffRes)
            assertNotEquals(same1, diffArgs)
            assertFalse(same1.equals("not a PluralResource"))
        }
    }
}
