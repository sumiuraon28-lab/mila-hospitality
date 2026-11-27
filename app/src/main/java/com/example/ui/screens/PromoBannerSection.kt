package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PromoBanner
import com.example.data.PromoBannerTheme
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaCharcoalElevated
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaEmerald
import com.example.ui.theme.MilaGoldMuted
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private val bannerImageClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
}

private data class BannerPalette(
    val accent: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val badgeBg: Color
)

private fun paletteForTheme(theme: PromoBannerTheme): BannerPalette = when (theme) {
    PromoBannerTheme.GOLD -> BannerPalette(
        accent = MilaChampagneGold,
        gradientStart = Color(0xFF2B2418),
        gradientEnd = Color(0xFF141715),
        badgeBg = Color(0x33D4AF7A)
    )
    PromoBannerTheme.EMERALD -> BannerPalette(
        accent = MilaEmerald,
        gradientStart = Color(0xFF122720),
        gradientEnd = Color(0xFF111614),
        badgeBg = Color(0x334CAF7D)
    )
    PromoBannerTheme.OBSIDIAN -> BannerPalette(
        accent = MilaChampagneGold,
        gradientStart = Color(0xFF1E2228),
        gradientEnd = Color(0xFF0E1110),
        badgeBg = Color(0x33E3C796)
    )
    PromoBannerTheme.TERRACOTTA -> BannerPalette(
        accent = MilaTerracotta,
        gradientStart = Color(0xFF2C1B17),
        gradientEnd = Color(0xFF151312),
        badgeBg = Color(0x33D97B66)
    )
}

@Composable
private fun rememberPromoBannerBitmap(imageUrl: String?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = imageUrl) {
        value = null
        val src = imageUrl?.trim().orEmpty()
        if (src.isBlank()) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = when {
                    src.startsWith("data:image/", ignoreCase = true) -> {
                        val comma = src.indexOf(',')
                        if (comma <= 0) return@runCatching null
                        Base64.decode(src.substring(comma + 1), Base64.DEFAULT)
                    }
                    src.startsWith("https://", ignoreCase = true) || src.startsWith("http://", ignoreCase = true) -> {
                        val req = Request.Builder().url(src).get().build()
                        bannerImageClient.newCall(req).execute().use { res ->
                            if (!res.isSuccessful) return@runCatching null
                            res.body?.bytes()
                        }
                    }
                    else -> null
                } ?: return@runCatching null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bitmap
}

@Composable
fun TopPromoBannerSection(
    banners: List<PromoBanner>,
    onOpenBannerPopup: (PromoBanner) -> Unit,
    onFallbackAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var poppedIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        poppedIn = true
    }

    AnimatedVisibility(
        visible = poppedIn,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
            scaleIn(
                initialScale = 0.94f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        modifier = modifier.fillMaxWidth()
    ) {
        if (banners.isEmpty()) {
            val defaultSpotlight = PromoBanner(
                id = "mila_default_spotlight",
                pageKey = "banner_default_spotlight",
                tagLabel = "UPCOMING PROJECTS & ALERTS",
                eyebrow = "MILA HOSPITALITY • FEATURED SPOTLIGHT",
                headline = "New Openings & Upcoming Hospitality Projects",
                subheadline = "Luxury hotel pre-openings, flagship restaurants, and exclusive announcements from MILA Hospitality.",
                detailsCopy = "Stay tuned for upcoming luxury hotel pre-openings, craft cocktail lounges, and pan-India hospitality launches managed by MILA Hospitality Consultant. Tap below to enquire about launching your next hospitality venture with our advisory team.",
                imageUrl = null,
                theme = PromoBannerTheme.GOLD,
                showAutoPopup = false,
                actionRoute = "contact_us",
                ctaLabel = "Enquire Now",
                published = true
            )
            PromoBannerCard(
                banner = defaultSpotlight,
                onClick = { onOpenBannerPopup(defaultSpotlight) }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (banners.size == 1) {
                    PromoBannerCard(
                        banner = banners.first(),
                        onClick = { onOpenBannerPopup(banners.first()) }
                    )
                } else {
                    val pagerState = rememberPagerState(pageCount = { banners.size })
                    HorizontalPager(
                        state = pagerState,
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val banner = banners[page]
                        PromoBannerCard(
                            banner = banner,
                            indexBadge = "${page + 1} / ${banners.size}",
                            onClick = { onOpenBannerPopup(banner) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(banners.size) { idx ->
                            val selected = pagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .height(6.dp)
                                    .width(if (selected) 22.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) MilaChampagneGold else MilaWarmStone.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoBannerCard(
    banner: PromoBanner,
    onClick: () -> Unit,
    indexBadge: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = paletteForTheme(banner.theme)
    val imageBitmap = rememberPromoBannerBitmap(banner.imageUrl)

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.2.dp, palette.accent.copy(alpha = 0.65f)),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalElevated),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_promo_banner_${banner.pageKey}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(palette.gradientStart, palette.gradientEnd)
                    )
                )
        ) {
            if (imageBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = banner.headline,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x55000000),
                                        Color.Transparent,
                                        palette.gradientEnd.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BannerTagChip(label = banner.tagLabel, accent = palette.accent, bg = Color(0xCC121614))
                        if (indexBadge != null) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xAA121614),
                                border = BorderStroke(1.dp, MilaGoldMuted)
                            ) {
                                Text(
                                    text = indexBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(color = MilaIvory),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (imageBitmap == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BannerTagChip(label = banner.tagLabel, accent = palette.accent, bg = palette.badgeBg)
                        if (indexBadge != null) {
                            Text(
                                text = indexBadge,
                                style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone)
                            )
                        }
                    }
                }

                if (banner.eyebrow.isNotBlank()) {
                    Text(
                        text = banner.eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = palette.accent,
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = banner.headline,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MilaIvory,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (banner.subheadline.isNotBlank()) {
                    Text(
                        text = banner.subheadline,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tap to view details",
                            style = MaterialTheme.typography.labelMedium.copy(color = MilaWarmStone)
                        )
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = palette.accent,
                        modifier = Modifier.clickable(onClick = onClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = banner.ctaLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MilaCharcoalDeep,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = banner.ctaLabel,
                                tint = MilaCharcoalDeep,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerTagChip(
    label: String,
    accent: Color,
    bg: Color
) {
    Surface(
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Campaign,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
            )
        }
    }
}

@Composable
fun PromoBannerPopupDialog(
    banner: PromoBanner,
    onDismiss: () -> Unit,
    onActionClick: (PromoBanner) -> Unit
) {
    val palette = paletteForTheme(banner.theme)
    val imageBitmap = rememberPromoBannerBitmap(banner.imageUrl)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.5.dp, palette.accent.copy(alpha = 0.8f)),
            colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 520.dp)
                .testTag("promo_banner_popup_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(palette.gradientStart, palette.gradientEnd)
                            )
                        )
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = banner.headline,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 340.dp)
                                .background(MilaCharcoalDeep)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(64.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BannerTagChip(label = banner.tagLabel, accent = palette.accent, bg = Color(0xCC121614))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xCC121614),
                            border = BorderStroke(1.dp, MilaGoldMuted)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("promo_popup_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close announcement",
                                    tint = MilaIvory
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (banner.eyebrow.isNotBlank()) {
                        Text(
                            text = banner.eyebrow.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = palette.accent,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Text(
                        text = banner.headline,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MilaIvory,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    if (banner.subheadline.isNotBlank()) {
                        Text(
                            text = banner.subheadline,
                            style = MaterialTheme.typography.bodyLarge.copy(color = MilaChampagneGold)
                        )
                    }

                    if (banner.detailsCopy.isNotBlank() && banner.detailsCopy != banner.subheadline) {
                        Text(
                            text = banner.detailsCopy,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MilaIvoryMuted,
                                lineHeight = 22.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (banner.actionRoute != "popup_only") {
                        GoldFilledButton(
                            text = banner.ctaLabel,
                            onClick = { onActionClick(banner) },
                            testTag = "promo_popup_action_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    GoldOutlineButton(
                        text = "Close",
                        onClick = onDismiss,
                        testTag = "promo_popup_dismiss_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
