package acr.browser.lightning.search.suggestions

import acr.browser.lightning.R
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.constant.UTF8
import acr.browser.lightning.database.SearchSuggestion
import acr.browser.lightning.di.SuggestionsClient
import acr.browser.lightning.log.Logger
import acr.browser.lightning.resources.ResourceProvider
import kotlinx.coroutines.Deferred
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONArray
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.net.URLDecoder
import java.util.Locale
import javax.inject.Inject

/**
 * Search suggestions provider for Google search engine.
 */
class GoogleSuggestionsModel @Inject constructor(
    @SuggestionsClient okHttpClient: Deferred<@JvmSuppressWildcards OkHttpClient>,
    requestFactory: RequestFactory,
    locale: Locale,
    resourceProvider: ResourceProvider,
    logger: Logger,
    coroutineDispatchers: CoroutineDispatchers,
) : BaseSuggestionsModel(
    okHttpClient,
    requestFactory,
    UTF8,
    locale,
    logger,
    coroutineDispatchers
) {

    private val searchSubtitle = resourceProvider.stringResource(R.string.suggestion)

    // https://suggestqueries.google.com/complete/search?output=toolbar&hl={language}&q={query}
    override fun createQueryUrl(query: String, language: String): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("suggestqueries.google.com")
        .encodedPath("/complete/search")
        .addQueryParameter("output", "chrome")
        .addQueryParameter("hl", if (URLDecoder.decode(query, UTF8).containsArabicText()) "ar" else language)
        .addEncodedQueryParameter("q", query)
        .build()

    @Throws(Exception::class)
    override fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        val json = JSONArray(responseBody.string())
        if (json.length() < 2) return emptyList()
        val suggestions = json.optJSONArray(1) ?: return emptyList()

        return buildList {
            for (index in 0 until suggestions.length()) {
                val suggestion = suggestions.optString(index).trim()
                if (suggestion.isNotEmpty()) {
                    add(SearchSuggestion("$searchSubtitle \"$suggestion\"", suggestion))
                }
            }
        }
    }

    private fun String.containsArabicText(): Boolean = any { character ->
        Character.UnicodeBlock.of(character) in setOf(
            Character.UnicodeBlock.ARABIC,
            Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A,
            Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B
        )
    }
}
