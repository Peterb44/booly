package acr.browser.lightning.ssl

import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import android.content.Context
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Unit tests for persistent SSL warning decisions. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [SDK_VERSION])
class SessionSslWarningPreferencesTest {

    private lateinit var preferences: PersistentSslWarningPreferences

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("ssl_site_decisions", Context.MODE_PRIVATE)
            .edit().clear().commit()
        preferences = PersistentSslWarningPreferences(context)
    }

    @Test
    fun `remembered approval is available for the same hostname across paths`() {
        assertThat(preferences.recallBehaviorForDomain("https://test.com")).isNull()

        preferences.rememberBehaviorForDomain(
            "https://test.com/12345",
            SslWarningPreferences.Behavior.PROCEED
        )

        assertThat(preferences.recallBehaviorForDomain("https://test.com/another-page"))
            .isEqualTo(SslWarningPreferences.Behavior.PROCEED)
    }

    @Test
    fun `approval does not apply to a different hostname or subdomain`() {
        preferences.rememberBehaviorForDomain(
            "https://test.com/path",
            SslWarningPreferences.Behavior.PROCEED
        )

        assertThat(preferences.recallBehaviorForDomain("https://other.com")).isNull()
        assertThat(preferences.recallBehaviorForDomain("https://sub.test.com")).isNull()
    }

    @Test
    fun `invalid URLs are ignored`() {
        preferences.rememberBehaviorForDomain(
            "test.com",
            SslWarningPreferences.Behavior.PROCEED
        )

        assertThat(preferences.recallBehaviorForDomain("test.com")).isNull()
    }
}
