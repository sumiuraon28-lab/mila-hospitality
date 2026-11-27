package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.ClientAuthOutcome
import com.example.auth.SupabaseAuthClient
import com.example.auth.SupabaseCandidateSession
import com.example.auth.SupabaseClientMetadata
import com.example.auth.SupabaseSession
import com.example.auth.SupabaseTalentClient
import com.example.data.BusinessCategory
import com.example.data.CandidateProfile
import com.example.data.ClientProfile
import com.example.data.ConsultationInquiry
import com.example.data.ConsultationServiceType
import com.example.data.HospitalityRole
import com.example.data.MilaCatalog
import com.example.ui.MandatesViewModel
import com.example.ui.UiState
import com.example.ui.components.AuthGateCard
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.SectionHeading
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalBorder
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaEmerald
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun ClientLoginScreen(
    currentUser: FirebaseUser?,
    supabaseSession: SupabaseSession?,
    supabaseAuthClient: SupabaseAuthClient,
    viewModel: MandatesViewModel?,
    @Suppress("UNUSED_PARAMETER") onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onNotify: (String) -> Unit,
    onExploreServices: () -> Unit,
    onBrowseHome: () -> Unit,
    onCommissionConsultation: (ConsultationServiceType) -> Unit,
    @Suppress("UNUSED_PARAMETER") onGoToJobSeekerPortal: () -> Unit,
    onSignOut: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var editingInquiry by remember { mutableStateOf<ConsultationInquiry?>(null) }
    var isSavingQuickDetails by remember { mutableStateOf(false) }

    val inquiriesState = viewModel?.inquiriesState?.collectAsStateWithLifecycle()?.value
    val supabaseEnquiries by supabaseAuthClient.myEnquiries.collectAsStateWithLifecycle()
    val clientProfileState = viewModel?.clientProfileState?.collectAsStateWithLifecycle()?.value
    val isSavingProfile = viewModel?.isSubmitting?.collectAsStateWithLifecycle()?.value ?: false
    val firestoreProfile = (clientProfileState as? UiState.Success)?.data

    val initialProfileMetadata = remember(firestoreProfile, supabaseSession, currentUser) {
        when {
            firestoreProfile != null -> SupabaseClientMetadata(
                fullName = firestoreProfile.fullName,
                email = firestoreProfile.email,
                mobileNumber = firestoreProfile.mobileNumber,
                companyName = firestoreProfile.companyName,
                designation = firestoreProfile.designation,
                city = firestoreProfile.city,
                businessCategory = firestoreProfile.resolvedBusinessCategory
            )
            supabaseSession != null -> supabaseSession.metadata
            else -> SupabaseClientMetadata(
                fullName = currentUser?.displayName.orEmpty(),
                email = currentUser?.email.orEmpty(),
                mobileNumber = "",
                companyName = "",
                designation = "",
                city = "",
                businessCategory = BusinessCategory.LUXURY_HOTEL_RESORT
            )
        }
    }

    val cleanDisplayName = remember(initialProfileMetadata.fullName) {
        initialProfileMetadata.fullName.trim().takeUnless {
            it.isBlank() || it.equals("MILA Guest", ignoreCase = true) || it.equals("MILA Member", ignoreCase = true)
        }.orEmpty()
    }
    val cleanDisplayEmail = remember(initialProfileMetadata.email) {
        initialProfileMetadata.email.trim().takeUnless {
            it.isBlank() || it.endsWith("@phone.mila.local", ignoreCase = true)
        }.orEmpty()
    }

    var quickName by remember(cleanDisplayName) { mutableStateOf(cleanDisplayName) }
    var quickMobile by remember(initialProfileMetadata.mobileNumber) { mutableStateOf(initialProfileMetadata.mobileNumber) }
    var quickEmail by remember(cleanDisplayEmail) { mutableStateOf(cleanDisplayEmail) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PortalUserHeader(
                eyebrow = "MILA HOSPITALITY • MY ACCOUNT",
                user = currentUser,
                testTag = "client_sign_out_button",
                displayNameOverride = cleanDisplayName.ifBlank { "MILA Member" },
                emailOverride = initialProfileMetadata.mobileNumber.ifBlank { cleanDisplayEmail },
                onAdditionalSignOut = {
                    if (onSignOut != null) {
                        onSignOut()
                    } else {
                        scope.launch { supabaseAuthClient.signOut() }
                    }
                }
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GoldFilledButton(
                    text = "Explore Services",
                    onClick = onExploreServices,
                    testTag = "client_explore_services_button",
                    modifier = Modifier.weight(1f)
                )
                GoldOutlineButton(
                    text = "Ask for Service",
                    onClick = { onCommissionConsultation(ConsultationServiceType.HOSPITALITY_PRE_OPENING) },
                    testTag = "client_new_mandate_button",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            GoldOutlineButton(
                text = "Back to MILA Home",
                onClick = onBrowseHome,
                testTag = "client_browse_home_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaCharcoalBorder),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "YOUR DETAILS",
                        style = MaterialTheme.typography.labelMedium.copy(color = MilaChampagneGold)
                    )
                    Text(
                        text = "Keep your name and mobile number updated so MILA partners can reach you quickly for service requests.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                    )
                    ClientTextField(
                        value = quickName,
                        onValueChange = { if (it.length <= 80) quickName = it },
                        label = "Your Name *",
                        placeholder = "Enter your name",
                        testTag = "profile_full_name"
                    )
                    ClientTextField(
                        value = quickMobile,
                        onValueChange = { if (it.length <= 20) quickMobile = it },
                        label = "Mobile Number *",
                        placeholder = "+91 98200 00000",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                        testTag = "profile_mobile"
                    )
                    ClientTextField(
                        value = quickEmail,
                        onValueChange = { if (it.length <= 120) quickEmail = it },
                        label = "Email (Optional)",
                        placeholder = "name@company.in",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        testTag = "profile_email"
                    )
                    GoldFilledButton(
                        text = if (isSavingProfile || isSavingQuickDetails) "Saving..." else "Save Details",
                        enabled = !isSavingProfile && !isSavingQuickDetails,
                        testTag = "save_client_profile_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (quickName.trim().length < 2) {
                                onAuthError("Please enter your name.")
                                return@GoldFilledButton
                            }
                            if (quickMobile.trim().length < 7) {
                                onAuthError("Please enter a valid mobile number.")
                                return@GoldFilledButton
                            }
                            scope.launch {
                                isSavingQuickDetails = true
                                var failed = false
                                if (supabaseSession != null) {
                                    supabaseAuthClient.updateQuickMemberDetails(
                                        fullName = quickName.trim(),
                                        mobileNumber = quickMobile.trim(),
                                        optionalEmail = quickEmail.trim()
                                    ).onFailure { err ->
                                        failed = true
                                        onAuthError(err.localizedMessage ?: "Failed to save details.")
                                    }
                                }
                                if (viewModel != null && currentUser != null) {
                                    viewModel.saveClientProfile(
                                        ClientProfile(
                                            fullName = quickName.trim(),
                                            email = quickEmail.trim().ifBlank { currentUser.email.orEmpty() },
                                            mobileNumber = quickMobile.trim(),
                                            companyName = initialProfileMetadata.companyName.ifBlank { "MILA Hospitality Member" },
                                            designation = initialProfileMetadata.designation.ifBlank { "Client" },
                                            city = initialProfileMetadata.city.ifBlank { "India" },
                                            businessCategory = initialProfileMetadata.businessCategory.name
                                        )
                                    )
                                } else if (!failed) {
                                    onNotify("Details updated.")
                                }
                                isSavingQuickDetails = false
                            }
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = "Your Service Enquiries",
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
        }

        if (viewModel != null && currentUser != null) {
            when (val state = inquiriesState) {
                is UiState.Loading, null -> item { PortalLoadingIndicator() }
                is UiState.Error -> item { PortalErrorCard(state.message) }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        item {
                            PortalEmptyCard(
                                icon = Icons.Rounded.BusinessCenter,
                                title = "No Service Enquiries Yet",
                                body = "Explore our 8 hospitality consultancy services and request a consultation anytime.",
                                ctaLabel = "Explore Services",
                                ctaTag = "client_empty_commission",
                                onCta = onExploreServices
                            )
                        }
                    } else {
                        items(state.data, key = { it.id }) { inquiry ->
                            ClientInquiryCard(
                                inquiry = inquiry,
                                onEditNotes = { editingInquiry = inquiry },
                                onWithdraw = { viewModel.withdrawInquiry(inquiry.id) },
                                onDelete = { viewModel.deleteInquiry(inquiry.id) }
                            )
                        }
                    }
                }
            }
        } else if (supabaseSession != null) {
            if (supabaseEnquiries.isEmpty()) {
                item {
                    PortalEmptyCard(
                        icon = Icons.Rounded.BusinessCenter,
                        title = "No Service Enquiries Yet",
                        body = "Explore our 8 hospitality consultancy services and request a consultation anytime.",
                        ctaLabel = "Explore Services",
                        ctaTag = "client_empty_commission",
                        onCta = onExploreServices
                    )
                }
            } else {
                items(supabaseEnquiries, key = { it.id }) { inquiry ->
                    ClientInquiryCard(
                        inquiry = inquiry,
                        onEditNotes = { editingInquiry = inquiry },
                        onWithdraw = {
                            scope.launch {
                                supabaseAuthClient.withdrawEnquiry(inquiry.id).fold(
                                    onSuccess = { onNotify("Enquiry withdrawn.") },
                                    onFailure = { onAuthError(it.localizedMessage ?: "Failed to withdraw enquiry.") }
                                )
                            }
                        },
                        onDelete = {
                            scope.launch {
                                supabaseAuthClient.deleteEnquiry(inquiry.id).fold(
                                    onSuccess = { onNotify("Enquiry deleted.") },
                                    onFailure = { onAuthError(it.localizedMessage ?: "Failed to delete enquiry.") }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    editingInquiry?.let { inquiry ->
        PortalEditBriefDialog(
            initialNotes = inquiry.briefNotes.orEmpty(),
            onDismiss = { editingInquiry = null },
            onSave = { notes ->
                if (viewModel != null && currentUser != null) {
                    viewModel.updateInquiryNotes(inquiry.id, notes) {
                        editingInquiry = null
                    }
                } else if (supabaseSession != null) {
                    scope.launch {
                        supabaseAuthClient.updateEnquiryNotes(inquiry.id, notes).fold(
                            onSuccess = {
                                onNotify("Enquiry brief updated.")
                                editingInquiry = null
                            },
                            onFailure = { onAuthError(it.localizedMessage ?: "Unable to update enquiry brief.") }
                        )
                    }
                }
            }
        )
    }
}

private enum class CandidateSection(val label: String) {
    PROFILE_CV("Profile & CV"),
    APPLICATIONS_SAVED("Applications & Saved"),
    NOTIFICATIONS("Alerts")
}

@Composable
fun JobSeekerLoginScreen(
    currentUser: FirebaseUser?,
    supabaseCandidateSession: SupabaseCandidateSession?,
    supabaseTalentClient: SupabaseTalentClient,
    viewModel: MandatesViewModel?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onNotify: (String) -> Unit,
    onExploreCareers: () -> Unit,
    onApplyRole: (HospitalityRole) -> Unit,
    onGoToClientPortal: () -> Unit,
    onSyncNotificationChannels: ((pushEnabled: Boolean, emailEnabled: Boolean) -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isAuthBusy by remember { mutableStateOf(false) }

    val candidateProfileState = viewModel?.candidateProfileState?.collectAsStateWithLifecycle()?.value
    val applicationsState = viewModel?.applicationsState?.collectAsStateWithLifecycle()?.value
    val savedRolesState = viewModel?.savedRolesState?.collectAsStateWithLifecycle()?.value
    val notificationsState = viewModel?.jobNotificationsState?.collectAsStateWithLifecycle()?.value
    val supabaseApps by supabaseTalentClient.myApplications.collectAsStateWithLifecycle()
    val supabaseSaved by supabaseTalentClient.mySavedRoles.collectAsStateWithLifecycle()
    val supabaseAlerts by supabaseTalentClient.myNotifications.collectAsStateWithLifecycle()
    val isSaving = viewModel?.isSubmitting?.collectAsStateWithLifecycle()?.value ?: false

    val firestoreProfile = (candidateProfileState as? UiState.Success)?.data
    val resolvedProfile = remember(firestoreProfile, supabaseCandidateSession, currentUser) {
        when {
            firestoreProfile != null -> firestoreProfile
            supabaseCandidateSession != null -> supabaseCandidateSession.profile
            else -> CandidateProfile(
                userId = currentUser?.uid.orEmpty(),
                fullName = currentUser?.displayName.orEmpty(),
                email = currentUser?.email.orEmpty(),
                jobAlertsEnabled = true
            )
        }
    }
    val activeUserId = currentUser?.uid?.takeIf { it.isNotBlank() }
        ?: supabaseCandidateSession?.userId.orEmpty().ifBlank { "candidate_local" }

    val unreadAlertsCount = remember(notificationsState, supabaseAlerts, currentUser) {
        if (currentUser != null) {
            (notificationsState as? UiState.Success)?.data?.count { !it.isRead } ?: 0
        } else {
            supabaseAlerts.count { !it.isRead }
        }
    }
    var selectedSection by remember { mutableStateOf(CandidateSection.PROFILE_CV) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PortalUserHeader(
                eyebrow = "MILA TALENT • JOB SEEKER PROFILE",
                user = currentUser,
                testTag = "job_seeker_sign_out_button",
                displayNameOverride = resolvedProfile.fullName.ifBlank { "Job Seeker" },
                emailOverride = resolvedProfile.mobileNumber.ifBlank {
                    resolvedProfile.email.takeUnless { it.endsWith("@phone.mila.local", ignoreCase = true) }.orEmpty()
                },
                onAdditionalSignOut = {
                    if (onSignOut != null) {
                        onSignOut()
                    } else {
                        scope.launch { supabaseTalentClient.signOut() }
                    }
                }
            )
        }

        item {
            GoldFilledButton(
                text = "I AM LOOKING FOR A JOB",
                onClick = onExploreCareers,
                testTag = "portal_primary_looking_for_job_button",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CandidateSection.entries) { section ->
                    val selected = selectedSection == section
                    val label = if (section == CandidateSection.NOTIFICATIONS && unreadAlertsCount > 0) {
                        "Alerts ($unreadAlertsCount)"
                    } else {
                        section.label
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { selectedSection = section },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MilaChampagneGold,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MilaCharcoalSurface,
                            labelColor = MilaIvoryMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MilaCharcoalBorder,
                            selectedBorderColor = MilaChampagneGold
                        ),
                        modifier = Modifier.testTag("candidate_tab_${section.name}")
                    )
                }
            }
        }

        when (selectedSection) {
            CandidateSection.PROFILE_CV -> {
                item {
                    PrivateCvVaultCard(
                        userId = activeUserId,
                        profile = resolvedProfile,
                        supabaseTalentClient = supabaseTalentClient,
                        onCvUploaded = { meta ->
                            val updated = resolvedProfile.copy(
                                cvFileName = meta.fileName,
                                cvMimeType = meta.mimeType,
                                cvFileSizeBytes = meta.fileSizeBytes,
                                cvStoragePath = meta.storagePath,
                                cvUploadedAtMillis = meta.uploadedAtMillis
                            )
                            if (viewModel != null && currentUser != null) {
                                viewModel.attachCvToProfile(resolvedProfile, meta)
                            } else {
                                onNotify("CV '${meta.fileName}' uploaded successfully.")
                            }
                            if (supabaseCandidateSession != null) {
                                scope.launch { supabaseTalentClient.syncCandidateProfile(updated) }
                            }
                        },
                        onCvDeleted = {
                            val cleared = resolvedProfile.copy(
                                cvFileName = null,
                                cvMimeType = null,
                                cvFileSizeBytes = null,
                                cvStoragePath = null,
                                cvUploadedAtMillis = null
                            )
                            if (viewModel != null && currentUser != null) {
                                viewModel.removeCvFromProfile(resolvedProfile)
                            } else {
                                onNotify("CV removed.")
                            }
                            if (supabaseCandidateSession != null) {
                                scope.launch { supabaseTalentClient.syncCandidateProfile(cleared) }
                            }
                        },
                        onError = onAuthError
                    )
                }

                item {
                    CandidateProfileEditorCard(
                        initialProfile = resolvedProfile,
                        isSaving = isSaving || isAuthBusy,
                        onSaveProfile = { updated ->
                            scope.launch {
                                isAuthBusy = true
                                val fallbackEmail = updated.email.ifBlank {
                                    supabaseCandidateSession?.email?.ifBlank { null }
                                        ?: currentUser?.email?.ifBlank { null }
                                        ?: "${updated.mobileNumber.filter { it.isDigit() }.ifBlank { "member" }}@phone.mila.local"
                                }
                                val profileToSave = updated.copy(email = fallbackEmail)
                                var syncFailed = false
                                if (supabaseCandidateSession != null) {
                                    supabaseTalentClient.syncCandidateProfile(profileToSave).onFailure { err ->
                                        syncFailed = true
                                        onAuthError(err.localizedMessage ?: "Failed to save profile.")
                                    }
                                }
                                if (viewModel != null && currentUser != null) {
                                    viewModel.saveCandidateProfile(profileToSave)
                                } else if (!syncFailed) {
                                    onNotify("Profile & experience saved.")
                                }
                                isAuthBusy = false
                            }
                        }
                    )
                }

                item {
                    IAmLookingForJobAlertsCard(
                        profile = resolvedProfile,
                        onToggleAlerts = { enabled ->
                            val updated = resolvedProfile.copy(jobAlertsEnabled = enabled)
                            if (viewModel != null && currentUser != null) {
                                viewModel.toggleJobAlerts(resolvedProfile, enabled)
                            } else {
                                onNotify(if (enabled) "Job alerts activated." else "Job alerts paused.")
                            }
                            if (supabaseCandidateSession != null) {
                                scope.launch { supabaseTalentClient.syncCandidateProfile(updated) }
                            }
                        },
                        onBrowseVacancies = onExploreCareers,
                        onUpdateChannelPreferences = onSyncNotificationChannels
                    )
                }
            }

                CandidateSection.APPLICATIONS_SAVED -> {
                    item {
                        Text(
                            text = "Application Status Tracker",
                            style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
                        )
                    }
                    if (viewModel != null && currentUser != null) {
                        when (val state = applicationsState) {
                            is UiState.Loading, null -> item { PortalLoadingIndicator() }
                            is UiState.Error -> item { PortalErrorCard(state.message) }
                            is UiState.Success -> {
                                if (state.data.isEmpty()) {
                                    item {
                                        PortalEmptyCard(
                                            icon = Icons.Rounded.WorkOutline,
                                            title = "No Applications Submitted Yet",
                                            body = "Explore our hospitality mandates across Mumbai, New Delhi, Udaipur, Goa, and Bengaluru.",
                                            ctaLabel = "Browse Open Careers",
                                            ctaTag = "job_seeker_empty_browse",
                                            onCta = onExploreCareers
                                        )
                                    }
                                } else {
                                    items(state.data, key = { it.id }) { app ->
                                        CandidateApplicationCard(
                                            application = app,
                                            onWithdraw = { viewModel.withdrawApplication(app.id) },
                                            onDelete = { viewModel.deleteApplication(app.id) }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (supabaseCandidateSession != null) {
                        if (supabaseApps.isEmpty()) {
                            item {
                                PortalEmptyCard(
                                    icon = Icons.Rounded.WorkOutline,
                                    title = "No Applications Submitted Yet",
                                    body = "Explore our hospitality mandates across India and apply directly with your private CV.",
                                    ctaLabel = "Browse Open Careers",
                                    ctaTag = "job_seeker_empty_browse",
                                    onCta = onExploreCareers
                                )
                            }
                        } else {
                            items(supabaseApps, key = { it.id }) { app ->
                                CandidateApplicationCard(
                                    application = app,
                                    onWithdraw = {
                                        scope.launch {
                                            supabaseTalentClient.withdrawApplication(app.id).fold(
                                                onSuccess = { onNotify("Application withdrawn.") },
                                                onFailure = { onAuthError(it.localizedMessage ?: "Failed to withdraw application.") }
                                            )
                                        }
                                    },
                                    onDelete = {
                                        scope.launch {
                                            supabaseTalentClient.withdrawApplication(app.id).fold(
                                                onSuccess = { onNotify("Application withdrawn.") },
                                                onFailure = { onAuthError(it.localizedMessage ?: "Failed to withdraw application.") }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Saved / Shortlisted Roles",
                            style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
                        )
                    }
                    if (viewModel != null && currentUser != null) {
                        when (val state = savedRolesState) {
                            is UiState.Loading, null -> item { PortalLoadingIndicator() }
                            is UiState.Error -> item { PortalErrorCard(state.message) }
                            is UiState.Success -> {
                                if (state.data.isEmpty()) {
                                    item {
                                        PortalEmptyCard(
                                            icon = Icons.Rounded.Bookmark,
                                            title = "No Saved Roles Yet",
                                            body = "Bookmark roles in Careers to shortlist and apply when ready.",
                                            ctaLabel = "Explore Careers",
                                            ctaTag = "shortlist_empty_browse",
                                            onCta = onExploreCareers
                                        )
                                    }
                                } else {
                                    items(state.data, key = { it.id }) { saved ->
                                        CandidateSavedRoleCard(
                                            saved = saved,
                                            onApply = {
                                                MilaCatalog.openRoles.firstOrNull { it.id == saved.roleId }?.let(onApplyRole)
                                            },
                                            onRemove = {
                                                MilaCatalog.openRoles.firstOrNull { it.id == saved.roleId }?.let {
                                                    viewModel.toggleSavedRole(it, currentlySaved = true)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (supabaseCandidateSession != null) {
                        if (supabaseSaved.isEmpty()) {
                            item {
                                PortalEmptyCard(
                                    icon = Icons.Rounded.Bookmark,
                                    title = "No Saved Roles Yet",
                                    body = "Bookmark roles in Careers to shortlist and apply when ready.",
                                    ctaLabel = "Explore Careers",
                                    ctaTag = "shortlist_empty_browse",
                                    onCta = onExploreCareers
                                )
                            }
                        } else {
                            items(supabaseSaved, key = { it.id }) { saved ->
                                CandidateSavedRoleCard(
                                    saved = saved,
                                    onApply = {
                                        MilaCatalog.openRoles.firstOrNull { it.id == saved.roleId }?.let(onApplyRole)
                                    },
                                    onRemove = {
                                        MilaCatalog.openRoles.firstOrNull { it.id == saved.roleId }?.let { role ->
                                            scope.launch {
                                                supabaseTalentClient.toggleSavedJob(role, currentlySaved = true)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                CandidateSection.NOTIFICATIONS -> {
                    item {
                        IAmLookingForJobAlertsCard(
                            profile = resolvedProfile,
                            onToggleAlerts = { enabled ->
                                val updated = resolvedProfile.copy(jobAlertsEnabled = enabled)
                                if (viewModel != null && currentUser != null) {
                                    viewModel.toggleJobAlerts(resolvedProfile, enabled)
                                } else {
                                    onNotify(if (enabled) "Job alerts activated." else "Job alerts paused.")
                                }
                                if (supabaseCandidateSession != null) {
                                    scope.launch { supabaseTalentClient.syncCandidateProfile(updated) }
                                }
                            },
                            onBrowseVacancies = onExploreCareers,
                            onUpdateChannelPreferences = onSyncNotificationChannels
                        )
                    }
                    if (viewModel != null && currentUser != null) {
                        when (val state = notificationsState) {
                            is UiState.Loading, null -> item { PortalLoadingIndicator() }
                            is UiState.Error -> item { PortalErrorCard(state.message) }
                            is UiState.Success -> {
                                if (state.data.isEmpty()) {
                                    item {
                                        PortalEmptyCard(
                                            icon = Icons.Rounded.NotificationsActive,
                                            title = "No Job Notifications Yet",
                                            body = "Activate Job Alerts above and save your preferred locations to receive matching hospitality vacancy alerts.",
                                            ctaLabel = "Browse Careers",
                                            ctaTag = "notifications_empty_browse",
                                            onCta = onExploreCareers
                                        )
                                    }
                                } else {
                                    items(state.data, key = { it.id }) { notification ->
                                        JobNotificationCard(
                                            notification = notification,
                                            onMarkRead = { viewModel.markJobNotificationRead(notification.id) },
                                            onViewCareers = {
                                                if (!notification.isRead) {
                                                    viewModel.markJobNotificationRead(notification.id)
                                                }
                                                onExploreCareers()
                                            },
                                            onDelete = { viewModel.deleteJobNotification(notification.id) }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (supabaseCandidateSession != null) {
                        if (supabaseAlerts.isEmpty()) {
                            item {
                                PortalEmptyCard(
                                    icon = Icons.Rounded.NotificationsActive,
                                    title = "No Job Notifications Yet",
                                    body = "Activate Job Alerts above and save your preferred locations to receive matching hospitality vacancy alerts.",
                                    ctaLabel = "Browse Careers",
                                    ctaTag = "notifications_empty_browse",
                                    onCta = onExploreCareers
                                )
                            }
                        } else {
                            items(supabaseAlerts, key = { it.id }) { notification ->
                                JobNotificationCard(
                                    notification = notification,
                                    onMarkRead = {
                                        scope.launch { supabaseTalentClient.markNotificationRead(notification.id) }
                                    },
                                    onViewCareers = {
                                        if (!notification.isRead) {
                                            scope.launch { supabaseTalentClient.markNotificationRead(notification.id) }
                                        }
                                        onExploreCareers()
                                    },
                                    onDelete = {
                                        scope.launch { supabaseTalentClient.markNotificationRead(notification.id) }
                                    }
                                )
                            }
                        }
                }
            }
        }
    }
}
