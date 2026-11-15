package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.features.group.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemberRemovalErrorUiTextTest {

    @Test
    fun `maps NON_ZERO_BALANCE to balance error resource`() {
        assertEquals(
            UiText.StringResource(R.string.group_remove_member_error_balance),
            CannotRemoveMemberException.Reason.NON_ZERO_BALANCE.toUiText()
        )
    }

    @Test
    fun `maps IS_CREATOR to admin error resource`() {
        assertEquals(
            UiText.StringResource(R.string.group_remove_member_error_admin),
            CannotRemoveMemberException.Reason.IS_CREATOR.toUiText()
        )
    }

    @Test
    fun `maps LAST_MEMBER to last member error resource`() {
        assertEquals(
            UiText.StringResource(R.string.group_remove_member_error_last_member),
            CannotRemoveMemberException.Reason.LAST_MEMBER.toUiText()
        )
    }

    @Test
    fun `maps NOT_A_MEMBER and USER_NOT_IN_BALANCES to generic removal error resource`() {
        assertEquals(
            UiText.StringResource(R.string.group_error_remove_member_failed),
            CannotRemoveMemberException.Reason.NOT_A_MEMBER.toUiText()
        )
        assertEquals(
            UiText.StringResource(R.string.group_error_remove_member_failed),
            CannotRemoveMemberException.Reason.USER_NOT_IN_BALANCES.toUiText()
        )
    }
}
