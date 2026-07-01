package es.pedrazamiguez.splittrip.domain.exception

class CannotRemoveMemberException(val reason: Reason) :
    Exception("Cannot remove member from group: ${reason.code}") {

    enum class Reason(val code: String) {
        NOT_A_MEMBER("not_a_member"),
        IS_CREATOR("is_creator"),
        LAST_MEMBER("last_member"),
        USER_NOT_IN_BALANCES("user_not_in_balances"),
        NON_ZERO_BALANCE("non_zero_balance")
    }
}
