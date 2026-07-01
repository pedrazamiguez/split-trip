package es.pedrazamiguez.splittrip.domain.exception

class CannotRemoveMemberException(reason: String) :
    Exception("Cannot remove member from group: $reason")
