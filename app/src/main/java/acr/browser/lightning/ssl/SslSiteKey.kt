package acr.browser.lightning.ssl

import androidx.core.net.toUri

private val SECOND_LEVEL_LABELS = setOf("co", "com", "org", "net", "gov", "edu", "ac")

/**
 * A key identifying the "site" of a URL: its host without subdomains (for example
 * `cdn.example.com` and `www.example.com` both become `example.com`). IP addresses are kept as is.
 */
fun String?.sslSiteKey(): String? {
    val host = this?.toUri()?.host?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
    if (host.contains(':') || host.all { it.isDigit() || it == '.' }) return host
    val labels = host.split('.')
    if (labels.size <= 2) return host
    val last = labels.last()
    val second = labels[labels.size - 2]
    val take = if (last.length == 2 && second in SECOND_LEVEL_LABELS) 3 else 2
    return labels.takeLast(take).joinToString(".")
}
