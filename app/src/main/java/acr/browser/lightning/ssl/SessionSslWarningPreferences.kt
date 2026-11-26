package acr.browser.lightning.ssl

import android.content.Context
import androidx.core.net.toUri
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores explicit SSL-warning decisions per exact hostname across app restarts.
 *
 * A site is remembered only after the user checks "Don't ask again for this site"
 * and explicitly chooses to proceed. Trust is limited to that hostname.
 */
@Singleton
class PersistentSslWarningPreferences @Inject constructor(
    context: Context
) : SslWarningPreferences {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override fun recallBehaviorForDomain(url: String?): SslWarningPreferences.Behavior? {
        val host = url.toUriOrNull()?.host?.lowercase() ?: return null
        return when (preferences.getString(KEY_PREFIX + host, null)) {
            BEHAVIOR_PROCEED -> SslWarningPreferences.Behavior.PROCEED
            BEHAVIOR_CANCEL -> SslWarningPreferences.Behavior.CANCEL
            else -> null
        }
    }

    override fun rememberBehaviorForDomain(
        url: String,
        behavior: SslWarningPreferences.Behavior
    ) {
        val host = url.toUriOrNull()?.host?.lowercase() ?: return
        preferences.edit().putString(
            KEY_PREFIX + host,
            when (behavior) {
                SslWarningPreferences.Behavior.PROCEED -> BEHAVIOR_PROCEED
                SslWarningPreferences.Behavior.CANCEL -> BEHAVIOR_CANCEL
            }
        ).apply()
    }

    private fun String.toUriOrNull() = try {
        toUri()
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val PREFERENCES_NAME = "ssl_site_decisions"
        const val KEY_PREFIX = "behavior:"
        const val BEHAVIOR_PROCEED = "proceed"
        const val BEHAVIOR_CANCEL = "cancel"
    }
}
