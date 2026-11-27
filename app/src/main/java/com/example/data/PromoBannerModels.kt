package com.example.data

const val PROMO_BANNER_KEY_PREFIX = "banner_"

enum class PromoBannerTheme(val key: String, val label: String) {
    GOLD("gold", "Champagne Gold"),
    EMERALD("emerald", "Emerald Launch"),
    OBSIDIAN("obsidian", "Royal Obsidian"),
    TERRACOTTA("terracotta", "Terracotta Spotlight");

    companion object {
        fun fromKey(raw: String?): PromoBannerTheme =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) } ?: GOLD
    }
}

data class PromoBanner(
    val id: String,
    val pageKey: String,
    val tagLabel: String,
    val eyebrow: String,
    val headline: String,
    val subheadline: String,
    val detailsCopy: String,
    val imageUrl: String?,
    val theme: PromoBannerTheme,
    val showAutoPopup: Boolean,
    val actionRoute: String,
    val ctaLabel: String,
    val published: Boolean
)

fun SitePageContent.isPromoBanner(): Boolean =
    pageKey.startsWith(PROMO_BANNER_KEY_PREFIX, ignoreCase = true)

fun SitePageContent.toPromoBanner(): PromoBanner {
    val configParts = heroImageKey
        .split("|")
        .mapNotNull { segment ->
            val idx = segment.indexOf(':')
            if (idx > 0) {
                segment.substring(0, idx).trim().lowercase() to segment.substring(idx + 1).trim()
            } else {
                null
            }
        }
        .toMap()

    val theme = PromoBannerTheme.fromKey(configParts["theme"] ?: heroImageKey)
    val showPopup = configParts["popup"]?.equals("on", ignoreCase = true) == true
    val action = configParts["action"].orEmpty().ifBlank { "contact_us" }

    return PromoBanner(
        id = id.ifBlank { pageKey },
        pageKey = pageKey,
        tagLabel = pageTitle.ifBlank { "ANNOUNCEMENT" },
        eyebrow = eyebrow,
        headline = headline.ifBlank { pageTitle },
        subheadline = subheadline,
        detailsCopy = bodyCopy.ifBlank { subheadline },
        imageUrl = heroImageUrl?.takeIf { it.isNotBlank() && it != "null" },
        theme = theme,
        showAutoPopup = showPopup,
        actionRoute = action,
        ctaLabel = ctaLabel.ifBlank { "Explore Details" },
        published = published
    )
}
