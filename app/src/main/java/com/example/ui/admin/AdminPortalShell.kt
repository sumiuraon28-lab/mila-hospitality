package com.example.ui.admin

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AdminModule
import com.example.data.AdminUser
import com.example.data.CandidateProfile
import com.example.ui.AdminViewModel
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoal
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaCharcoalElevated
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaGoldMuted
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone
import kotlinx.coroutines.launch

@Composable
fun AdminPortalShell(
    actor: AdminUser,
    viewModel: AdminViewModel,
    onExitToPublicSite: () -> Unit,
    onSignOut: () -> Unit
) {
    val selectedModule by viewModel.selectedModule.collectAsStateWithLifecycle()

    BackHandler {
        if (selectedModule != AdminModule.DASHBOARD_OVERVIEW) {
            viewModel.selectModule(AdminModule.DASHBOARD_OVERVIEW)
        } else {
            onExitToPublicSite()
        }
    }
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val inquiries by viewModel.inquiries.collectAsStateWithLifecycle()
    val services by viewModel.services.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val vacancies by viewModel.vacancies.collectAsStateWithLifecycle()
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val applications by viewModel.applications.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val sitePages by viewModel.sitePages.collectAsStateWithLifecycle()
    val portfolioItems by viewModel.portfolioItems.collectAsStateWithLifecycle()
    val testimonials by viewModel.testimonials.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminInvites by viewModel.adminInvites.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val notificationControls by viewModel.notificationControls.collectAsStateWithLifecycle()
    val deliveryLogs by viewModel.deliveryLogs.collectAsStateWithLifecycle()
    val deviceTokens by viewModel.deviceTokens.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val handleDownloadCv: (CandidateProfile) -> Unit = { candidate ->
        viewModel.downloadCandidateCv(context, candidate) { downloaded ->
            runCatching {
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(downloaded.shareableUri, downloaded.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(viewIntent, "Open ${downloaded.fileName}").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        }
    }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            snackbarHostState.showSnackbar(feedback!!)
            viewModel.clearFeedback()
        }
    }

    val allowedModules = remember(actor.role) {
        AdminModule.entries.filter { it in actor.resolvedRole.allowedModules() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminSidebarContent(
                actor = actor,
                modules = allowedModules,
                selectedModule = selectedModule,
                onSelectModule = {
                    viewModel.selectModule(it)
                    scope.launch { drawerState.close() }
                },
                onExitToPublicSite = {
                    scope.launch { drawerState.close() }
                    onExitToPublicSite()
                },
                onSignOut = onSignOut
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val showPermanentSidebar = maxWidth >= 840.dp

            Scaffold(
                containerColor = MilaCharcoal,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MilaCharcoalElevated,
                            contentColor = MilaIvory,
                            actionColor = MilaChampagneGold
                        )
                    }
                },
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MilaCharcoalDeep)
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (!showPermanentSidebar) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("admin_menu_button")
                                    ) {
                                        Icon(Icons.Rounded.Menu, "Open admin modules", tint = MilaChampagneGold)
                                    }
                                }
                                Icon(
                                    Icons.Rounded.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MilaChampagneGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "MILA ADMIN PORTAL",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MilaChampagneGold,
                                            letterSpacing = 1.4.sp
                                        )
                                    )
                                    Text(
                                        "${actor.resolvedRole.displayLabel} • ${selectedModule.title}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted),
                                        maxLines = 1
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.refreshData() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("admin_refresh_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh dashboard data",
                                        tint = MilaChampagneGold
                                    )
                                }
                                TextButton(
                                    onClick = onSignOut,
                                    modifier = Modifier.testTag("admin_top_sign_out_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ExitToApp,
                                        contentDescription = "Sign out of Admin Portal",
                                        tint = MilaTerracotta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Sign Out", color = MilaTerracotta)
                                }
                            }
                        }
                        if (isBusy) {
                            LinearProgressIndicator(
                                color = MilaChampagneGold,
                                trackColor = MilaCharcoalSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        HorizontalDivider(color = MilaGoldSubtle)
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showPermanentSidebar) {
                        Box(
                            modifier = Modifier
                                .width(270.dp)
                                .fillMaxHeight()
                                .background(MilaCharcoalDeep)
                                .border(BorderStroke(0.5.dp, MilaGoldSubtle))
                        ) {
                            AdminSidebarBody(
                                actor = actor,
                                modules = allowedModules,
                                selectedModule = selectedModule,
                                onSelectModule = viewModel::selectModule,
                                onExitToPublicSite = onExitToPublicSite,
                                onSignOut = onSignOut
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedModule) {
                            AdminModule.DASHBOARD_OVERVIEW -> AdminDashboardOverviewModule(
                                actor = actor,
                                analytics = analytics,
                                recentLogs = activityLogs,
                                onSelectModule = viewModel::selectModule
                            )
                            AdminModule.CLIENT_MANAGEMENT -> AdminClientManagementModule(
                                actor = actor,
                                clients = clients,
                                onUpdateClient = viewModel::updateClient,
                                onDeleteClient = viewModel::deleteClient
                            )
                            AdminModule.CLIENT_ENQUIRIES -> AdminClientEnquiriesModule(
                                actor = actor,
                                inquiries = inquiries,
                                consultants = adminUsers,
                                onUpdateInquiry = viewModel::updateInquiry,
                                onDeleteInquiry = viewModel::deleteInquiry
                            )
                            AdminModule.SERVICE_MANAGEMENT -> AdminServicesModule(
                                actor = actor,
                                services = services,
                                onSaveService = viewModel::saveService,
                                onDeleteService = viewModel::deleteService
                            )
                            AdminModule.SERVICE_CATEGORIES -> AdminServiceCategoriesModule(
                                actor = actor,
                                categories = categories,
                                onSaveCategory = viewModel::saveCategory,
                                onDeleteCategory = viewModel::deleteCategory
                            )
                            AdminModule.JOB_VACANCY_MANAGEMENT -> AdminVacanciesModule(
                                actor = actor,
                                vacancies = vacancies,
                                onSaveVacancy = { viewModel.saveVacancy(it) },
                                onDeleteVacancy = viewModel::deleteVacancy
                            )
                            AdminModule.CANDIDATE_MANAGEMENT -> AdminCandidatesModule(
                                actor = actor,
                                candidates = candidates,
                                onUpdateCandidate = viewModel::updateCandidate,
                                onDeleteCandidate = viewModel::deleteCandidate,
                                onToggleAlerts = viewModel::toggleCandidateAlerts,
                                onDownloadCv = handleDownloadCv
                            )
                            AdminModule.CV_DATABASE -> AdminCvDatabaseModule(
                                actor = actor,
                                candidates = candidates,
                                onDownloadCv = handleDownloadCv,
                                onPurgeCv = viewModel::clearCandidateCv
                            )
                            AdminModule.JOB_APPLICATIONS, AdminModule.RECRUITMENT_STATUS_MANAGEMENT ->
                                AdminApplicationsAndStatusModule(
                                    actor = actor,
                                    module = selectedModule,
                                    applications = applications,
                                    candidates = candidates,
                                    onUpdateStatus = { app, st, notify -> viewModel.updateApplicationStatus(app, st, notify) },
                                    onDownloadCv = handleDownloadCv,
                                    onDeleteApplication = viewModel::deleteApplication
                                )
                            AdminModule.NOTIFICATION_MANAGEMENT -> AdminNotificationsCenterModule(
                                actor = actor,
                                controls = notificationControls,
                                vacancies = vacancies,
                                candidates = candidates,
                                deviceTokens = deviceTokens,
                                notifications = notifications,
                                deliveryLogs = deliveryLogs,
                                onSaveControls = viewModel::saveNotificationControls,
                                onDispatchMatchedVacancy = { viewModel.dispatchMatchedVacancyAlerts(it) },
                                onSendDirectNotification = viewModel::sendNotification,
                                onDeleteNotification = viewModel::deleteNotification
                            )
                            AdminModule.WEBSITE_CONTENT_MANAGEMENT -> AdminWebsiteContentModule(
                                actor = actor,
                                pages = sitePages,
                                onSavePage = viewModel::saveSitePage,
                                onDeletePage = viewModel::deleteSitePage
                            )
                            AdminModule.PORTFOLIO_MANAGEMENT -> AdminPortfolioModule(
                                actor = actor,
                                itemsList = portfolioItems,
                                onSaveItem = viewModel::savePortfolioItem,
                                onDeleteItem = viewModel::deletePortfolioItem
                            )
                            AdminModule.TESTIMONIALS -> AdminTestimonialsModule(
                                actor = actor,
                                testimonials = testimonials,
                                onSaveTestimonial = viewModel::saveTestimonial,
                                onDeleteTestimonial = viewModel::deleteTestimonial
                            )
                            AdminModule.TEAM_MANAGEMENT -> AdminTeamManagementModule(
                                actor = actor,
                                team = teamMembers,
                                onSaveMember = viewModel::saveTeamMember,
                                onDeleteMember = viewModel::deleteTeamMember
                            )
                            AdminModule.REPORTS_AND_ANALYTICS -> AdminReportsAnalyticsModule(
                                actor = actor,
                                analytics = analytics
                            )
                            AdminModule.ADMIN_USER_MANAGEMENT -> AdminUsersModule(
                                actor = actor,
                                adminUsers = adminUsers,
                                adminInvites = adminInvites,
                                onInviteAdmin = viewModel::inviteAdmin,
                                onRevokeInvite = viewModel::revokeInvite,
                                onUpdateAdmin = viewModel::updateAdminRoleAndStatus
                            )
                            AdminModule.SETTINGS -> AdminSettingsModule(
                                actor = actor,
                                settings = settings,
                                onSaveSettings = viewModel::savePlatformSettings
                            )
                            AdminModule.ACTIVITY_LOGS -> AdminActivityLogsModule(logs = activityLogs)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSidebarContent(
    actor: AdminUser,
    modules: List<AdminModule>,
    selectedModule: AdminModule,
    onSelectModule: (AdminModule) -> Unit,
    onExitToPublicSite: () -> Unit,
    onSignOut: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MilaCharcoalDeep,
        drawerContentColor = MilaIvory,
        modifier = Modifier.width(300.dp)
    ) {
        AdminSidebarBody(actor, modules, selectedModule, onSelectModule, onExitToPublicSite, onSignOut)
    }
}

@Composable
private fun AdminSidebarBody(
    actor: AdminUser,
    modules: List<AdminModule>,
    selectedModule: AdminModule,
    onSelectModule: (AdminModule) -> Unit,
    onExitToPublicSite: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_crest_1795637231719),
                contentDescription = "MILA Crest",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, MilaGoldMuted, MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "MILA EXECUTIVE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MilaChampagneGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                )
                StatusPill(label = actor.resolvedRole.displayLabel, tone = StatusTone.GOLD)
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MilaGoldSubtle, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(6.dp))

        modules.groupBy { it.group }.forEach { (group, groupMods) ->
            Text(
                text = group.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.2.sp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
            groupMods.forEach { mod ->
                NavigationDrawerItem(
                    label = { Text("${mod.number.toString().padStart(2, '0')}. ${mod.title}") },
                    selected = selectedModule == mod,
                    onClick = { onSelectModule(mod) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MilaCharcoalElevated,
                        selectedTextColor = MilaChampagneGold,
                        unselectedContainerColor = Color.Transparent,
                        unselectedTextColor = MilaIvoryMuted
                    ),
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 1.dp)
                        .testTag("admin_mod_${mod.name}")
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MilaGoldSubtle, modifier = Modifier.padding(horizontal = 16.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            TextButton(
                onClick = onExitToPublicSite,
                modifier = Modifier.testTag("admin_exit_public_button")
            ) {
                Text("Switch to Member App", color = MilaChampagneGold)
            }
            TextButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("admin_drawer_sign_out_button")
            ) {
                Text("Sign Out of Admin Portal", color = MilaTerracotta)
            }
            Text(
                text = actor.email,
                style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone)
            )
        }
    }
}
