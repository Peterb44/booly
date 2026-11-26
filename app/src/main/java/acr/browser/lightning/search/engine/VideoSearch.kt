package acr.browser.lightning.search.engine

import acr.browser.lightning.R

/**
 * Search video results on YouTube.
 */
class VideoSearch : BaseSearchEngine(
    "file:///android_asset/lightning.png",
    "https://www.google.com/search?tbm=vid&q=",
    R.string.search_engine_video
)
