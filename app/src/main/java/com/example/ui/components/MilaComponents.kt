package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.GoogleSignInButton
import com.example.data.AdminUser
import com.example.data.ImpactMetric
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalBorder
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaCharcoalElevated
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaEmerald
import com.example.ui.theme.MilaGoldMuted
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone

data class DrawerNavEntry(
    val route: String,
    val label: String,
    val group: String,
    val icon: ImageVector
)

@Composable
fun MilaBrandHeader(
    isSignedIn: Boolean,
    userInitial: String?,
    onMenuClick: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("header_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Open navigation menu",
                    tint = MilaChampagneGold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .testTag("brand_crest_container")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_crest_1795637231719),
                    contentDescription = "MILA Hospitality Crest",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialTheme.shapes.small)
                        .border(1.dp, MilaGoldMuted, MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.brand_title),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.4.sp,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.brand_tagline),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MilaIvoryMuted,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onAccountClick,
                shape = CircleShape,
                color = if (isSignedIn) MilaCharcoalElevated else MilaCharcoalSurface,
                border = BorderStroke(
                    1.dp,
                    if (isSignedIn) MilaChampagneGold else MilaCharcoalBorder
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("header_account_button")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSignedIn) (userInitial ?: "PORTAL") else "LOGIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSignedIn) MilaChampagneGold else MilaIvoryMuted,
                            letterSpacing = 1.1.sp
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.75.dp, color = MilaGoldSubtle)
    }
}

@Composable
fun MilaAuthHeader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_brand_crest_container")
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_crest_1795637231719),
                contentDescription = "MILA Hospitality Crest",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, MilaGoldMuted, MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.brand_title),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.6.sp,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.brand_tagline),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MilaIvoryMuted,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                label = "MEMBERS",
                tone = StatusTone.GOLD
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.75.dp, color = MilaGoldSubtle)
    }
}

@Composable
fun MilaDrawerSheetContent(
    entries: List<DrawerNavEntry>,
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
    onSignOut: (() -> Unit)? = null
) {
    ModalDrawerSheet(
        drawerContainerColor = MilaCharcoalDeep,
        drawerContentColor = MilaIvory,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_crest_1795637231719),
                    contentDescription = "MILA Crest",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(MaterialTheme.shapes.small)
                        .border(1.dp, MilaGoldMuted, MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MILA HOSPITALITY",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.6.sp
                        )
                    )
                    Text(
                        text = "Creating Excellence",
                        style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MilaGoldSubtle, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(8.dp))

            val grouped = entries.groupBy { it.group }
            grouped.forEach { (groupName, groupEntries) ->
                Text(
                    text = groupName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                groupEntries.forEach { entry ->
                    val selected = currentRoute == entry.route
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.titleSmall
                            )
                        },
                        selected = selected,
                        onClick = { onSelectRoute(entry.route) },
                        icon = {
                            Icon(
                                imageVector = entry.icon,
                                contentDescription = entry.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MilaCharcoalElevated,
                            selectedIconColor = MilaChampagneGold,
                            selectedTextColor = MilaChampagneGold,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MilaWarmStone,
                            unselectedTextColor = MilaIvoryMuted
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .testTag("drawer_${entry.route}")
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (onSignOut != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MilaGoldSubtle, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(6.dp))
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "Sign Out",
                            style = MaterialTheme.typography.titleSmall
                        )
                    },
                    selected = false,
                    onClick = onSignOut,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Sign Out",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = MilaTerracotta,
                        unselectedTextColor = MilaTerracotta
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .testTag("drawer_sign_out")
                )
            }
        }
    }
}

@Composable
fun SectionHeading(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = MilaChampagneGold,
                letterSpacing = 1.8.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MilaIvory
            )
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MilaIvoryMuted
                )
            )
        }
    }
}

@Composable
fun LuxuryMetricTile(
    metric: ImpactMetric,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = metric.figure,
                style = MaterialTheme.typography.displayMedium.copy(
                    color = MilaChampagneGold,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = metric.label,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MilaIvory,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = metric.detail,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MilaWarmStone
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GoldFilledButton(
    text: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MilaChampagneGold,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MilaCharcoalElevated,
            disabledContentColor = MilaWarmStone
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
fun GoldOutlineButton(
    text: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MilaChampagneGold),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MilaChampagneGold
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier
) {
    val (bg, fg, border) = when (tone) {
        StatusTone.GOLD -> Triple(Color(0x26D6BD96), MilaChampagneGold, MilaGoldMuted)
        StatusTone.EMERALD -> Triple(Color(0x266FA082), MilaEmerald, MilaEmerald)
        StatusTone.MUTED -> Triple(Color(0x269C968B), MilaWarmStone, MilaCharcoalBorder)
        StatusTone.ALERT -> Triple(Color(0x26C96A5B), MilaTerracotta, MilaTerracotta)
    }
    Surface(
        modifier = modifier,
        color = bg,
        border = BorderStroke(0.75.dp, border),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(color = fg, letterSpacing = 0.9.sp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

enum class StatusTone {
    GOLD, EMERALD, MUTED, ALERT
}

@Composable
fun AuthGateCard(
    title: String,
    subtitle: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = "Secure Client & Candidate Portal",
                    tint = MilaChampagneGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "SECURE PORTAL ACCESS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.6.sp
                    )
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted)
            )
            Spacer(modifier = Modifier.height(4.dp))
            GoogleSignInButton(
                onAuthSuccess = onAuthSuccess,
                onAuthError = onAuthError
            )
        }
    }
}
