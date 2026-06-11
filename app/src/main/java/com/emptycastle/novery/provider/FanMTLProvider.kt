package com.emptycastle.novery.provider

import com.emptycastle.novery.R

/**
 * Provider for FanMTL.com
 * Inherits from WuxiaBoxProvider as they share the same engine.
 */
class FanMTLProvider : WuxiaBoxProvider() {
    override val name = "FanMTL"
    override val mainUrl = "https://www.fanmtl.com"
    override val hasMainPage = true
    override val iconRes: Int = R.drawable.ic_provider_fanmtl
}
