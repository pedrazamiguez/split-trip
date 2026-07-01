package es.pedrazamiguez.splittrip.domain.exception

class CannotLeaveGroupException(reason: String) :
    Exception("Cannot leave group: $reason")
