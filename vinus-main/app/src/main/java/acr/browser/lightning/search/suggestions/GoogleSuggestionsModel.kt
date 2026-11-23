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
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
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
        .addQueryParameter("output", "toolbar")
        .addQueryParameter("hl", language)
        .addQueryParameter("ie", "utf-8")
        .addQueryParameter("oe", "utf-8")
        .addEncodedQueryParameter("q", query)
        .build()

    @Throws(Exception::class)
    override fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        // The server may answer in a legacy encoding (for example windows-1256 for Arabic) even
        // when asked for UTF-8, so detect the encoding instead of assuming one.
        val xml = decodeSuggestionBytes(responseBody.bytes(), responseBody.contentType()?.charset())
        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }.newPullParser()
        parser.setInput(StringReader(xml))

        val suggestions = mutableListOf<SearchSuggestion>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "suggestion" == parser.name) {
                val suggestion = parser.getAttributeValue(null, "data")
                if (!suggestion.isNullOrBlank()) {
                    suggestions.add(SearchSuggestion("$searchSubtitle \"$suggestion\"", suggestion))
                }
            }
            eventType = parser.next()
        }

        return suggestions
    }

    private fun decodeSuggestionBytes(bytes: ByteArray, declared: Charset?): String {
        val candidates = mutableListOf<Charset>(Charsets.UTF_8)
        if (declared != null && declared != Charsets.ISO_8859_1 && declared != Charsets.US_ASCII) {
            candidates.add(declared)
        }
        runCatching { Charset.forName("windows-1256") }.getOrNull()?.let { candidates.add(it) }
        candidates.add(Charsets.ISO_8859_1)

        for (charset in candidates) {
            try {
                return charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString()
            } catch (e: CharacterCodingException) {
                // Try the next candidate.
            }
        }
        return String(bytes, Charsets.UTF_8)
    }
}
