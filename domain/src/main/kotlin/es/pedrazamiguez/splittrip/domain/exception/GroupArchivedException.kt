package es.pedrazamiguez.splittrip.domain.exception

class GroupArchivedException(val groupId: String) :
    Exception("Cannot modify an archived group.")
