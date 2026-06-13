package es.pedrazamiguez.splittrip.core.logging.sanitizer

fun String.maskEmail(): String {
    val emailRegex = """^([^@]+)@([^@]+)$""".toRegex()
    val match = emailRegex.matchEntire(this) ?: return this
    val (localPart, domainPart) = match.destructured

    val maskedLocal = when {
        localPart.length <= 1 -> "$localPart***"
        localPart.length == 2 -> "${localPart.first()}***"
        else -> "${localPart.first()}***${localPart.last()}"
    }

    val domainName = domainPart.substringBeforeLast('.')
    val tld = domainPart.substringAfterLast('.')
    val maskedDomainName = when {
        domainName.length <= 1 -> "$domainName***"
        domainName.length == 2 -> "${domainName.first()}***"
        else -> "${domainName.first()}***${domainName.last()}"
    }

    val maskedDomain = if (domainPart.contains('.')) {
        "$maskedDomainName.$tld"
    } else {
        maskedDomainName
    }

    return "$maskedLocal@$maskedDomain"
}
