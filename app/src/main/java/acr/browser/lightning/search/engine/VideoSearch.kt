package acr.browser.lightning.search.engine

import acr.browser.lightning.R

/**
 * Search video results on YouTube.
 */
class VideoSearch : BaseSearchEngine(
    "file:///android_asset/lightning.png",
    "https://www.youtube.com/results?search_query=",
    R.string.search_engine_video
)
