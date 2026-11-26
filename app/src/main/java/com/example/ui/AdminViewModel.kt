package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityLog
import com.example.data.AdminCmsRepository
import com.example.data.AdminInvite
import com.example.data.AdminModule
import com.example.data.AdminRepository
import com.example.data.AdminRole
import com.example.data.AdminSessionState
import com.example.data.AdminSettings
import com.example.data.AdminUser
import com.example.data.ApplicationStatus
import com.example.data.CandidateProfile
import com.example.data.ClientProfile
import com.example.data.ConsultationInquiry
import com.example.data.InquiryStatus
import com.example.data.JobApplication
import com.example.data.JobNotification
import com.example.data.JobVacancyRecord
import com.example.data.PortfolioItemRecord
import com.example.data.RealAnalyticsSnapshot
import com.example.data.ServiceCategoryRecord
import com.example.data.ServiceOffering
import com.example.data.SitePageContent
import com.example.data.TeamMemberRecord
import com.example.data.TestimonialRecord
import com.example.notifications.CandidateDeviceToken
import com.example.notifications.NotificationControlSettings
import com.example.notifications.NotificationDeliveryLog
import com.example.notifications.NotificationDispatchRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepository: AdminRepository? = null,
    private val cmsRepository: AdminCmsRepository? = null,
    private val notificationRepository: NotificationDispatchRepository? = null
) : ViewModel() {

    private val _sessionState = MutableStateFlow<AdminSessionState>(AdminSessionState.NotAdmin)
    val sessionState: StateFlow<AdminSessionState> = _sessionState.asStateFlow()

    private val _selectedModule = MutableStateFlow(AdminModule.DASHBOARD_OVERVIEW)
    val selectedModule: StateFlow<AdminModule> = _selectedModule.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    private val _clients = MutableStateFlow<List<ClientProfile>>(emptyList())
    val clients: StateFlow<List<ClientProfile>> = _clients.asStateFlow()

    private val _inquiries = MutableStateFlow<List<ConsultationInquiry>>(emptyList())
    val inquiries: StateFlow<List<ConsultationInquiry>> = _inquiries.asStateFlow()

    private val _services = MutableStateFlow<List<ServiceOffering>>(emptyList())
    val services: StateFlow<List<ServiceOffering>> = _services.asStateFlow()

    private val _categories = MutableStateFlow<List<ServiceCategoryRecord>>(emptyList())
    val categories: StateFlow<List<ServiceCategoryRecord>> = _categories.asStateFlow()

    private val _vacancies = MutableStateFlow<List<JobVacancyRecord>>(emptyList())
    val vacancies: StateFlow<List<JobVacancyRecord>> = _vacancies.asStateFlow()

    private val _candidates = MutableStateFlow<List<CandidateProfile>>(emptyList())
    val candidates: StateFlow<List<CandidateProfile>> = _candidates.asStateFlow()

    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    val applications: StateFlow<List<JobApplication>> = _applications.asStateFlow()

    private val _notifications = MutableStateFlow<List<JobNotification>>(emptyList())
    val notifications: StateFlow<List<JobNotification>> = _notifications.asStateFlow()

    private val _sitePages = MutableStateFlow<List<SitePageContent>>(emptyList())
    val sitePages: StateFlow<List<SitePageContent>> = _sitePages.asStateFlow()

    private val _portfolioItems = MutableStateFlow<List<PortfolioItemRecord>>(emptyList())
    val portfolioItems: StateFlow<List<PortfolioItemRecord>> = _portfolioItems.asStateFlow()

    private val _testimonials = MutableStateFlow<List<TestimonialRecord>>(emptyList())
    val testimonials: StateFlow<List<TestimonialRecord>> = _testimonials.asStateFlow()

    private val _teamMembers = MutableStateFlow<List<TeamMemberRecord>>(emptyList())
    val teamMembers: StateFlow<List<TeamMemberRecord>> = _teamMembers.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<AdminUser>>(emptyList())
    val adminUsers: StateFlow<List<AdminUser>> = _adminUsers.asStateFlow()

    private val _adminInvites = MutableStateFlow<List<AdminInvite>>(emptyList())
    val adminInvites: StateFlow<List<AdminInvite>> = _adminInvites.asStateFlow()

    private val _settings = MutableStateFlow(AdminSettings())
    val settings: StateFlow<AdminSettings> = _settings.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs.asStateFlow()

    private val _notificationControls = MutableStateFlow(NotificationControlSettings())
    val notificationControls: StateFlow<NotificationControlSettings> = _notificationControls.asStateFlow()

    private val _deliveryLogs = MutableStateFlow<List<NotificationDeliveryLog>>(emptyList())
    val deliveryLogs: StateFlow<List<NotificationDeliveryLog>> = _deliveryLogs.asStateFlow()

    private val _deviceTokens = MutableStateFlow<List<CandidateDeviceToken>>(emptyList())
    val deviceTokens: StateFlow<List<CandidateDeviceToken>> = _deviceTokens.asStateFlow()

    val analytics: StateFlow<RealAnalyticsSnapshot> = combine(
        combine(_clients, _inquiries, _services, _vacancies, _candidates) { cl, inq, svc, vac, cand ->
            listOf(cl, inq, svc, vac, cand)
        },
        combine(_applications, _portfolioItems, _testimonials, _teamMembers, _adminUsers) { app, port, test, team, adm ->
            listOf(app, port, test, team, adm)
        },
        _activityLogs
    ) { firstBatch, secondBatch, logs ->
        @Suppress("UNCHECKED_CAST")
        val clientsList = firstBatch[0] as List<ClientProfile>
        @Suppress("UNCHECKED_CAST")
        val inquiriesList = firstBatch[1] as List<ConsultationInquiry>
        @Suppress("UNCHECKED_CAST")
        val servicesList = firstBatch[2] as List<ServiceOffering>
        @Suppress("UNCHECKED_CAST")
        val vacanciesList = firstBatch[3] as List<JobVacancyRecord>
        @Suppress("UNCHECKED_CAST")
        val candidatesList = firstBatch[4] as List<CandidateProfile>
        @Suppress("UNCHECKED_CAST")
        val applicationsList = secondBatch[0] as List<JobApplication>
        @Suppress("UNCHECKED_CAST")
        val portfolioList = secondBatch[1] as List<PortfolioItemRecord>
        @Suppress("UNCHECKED_CAST")
        val testimonialsList = secondBatch[2] as List<TestimonialRecord>
        @Suppress("UNCHECKED_CAST")
        val teamList = secondBatch[3] as List<TeamMemberRecord>
        @Suppress("UNCHECKED_CAST")
        val adminsList = secondBatch[4] as List<AdminUser>

        RealAnalyticsSnapshot(
            totalClients = clientsList.size,
            totalEnquiries = inquiriesList.size,
            activeEnquiries = inquiriesList.count { it.status != InquiryStatus.WITHDRAWN.name },
            enquiriesByStatus = inquiriesList.groupingBy { it.resolvedStatus.label }.eachCount(),
            enquiriesByService = inquiriesList.groupingBy { it.resolvedServiceType.label }.eachCount(),
            publishedServicesCount = servicesList.count { it.published },
            totalServicesCount = servicesList.size,
            totalVacanciesCount = vacanciesList.size,
            publishedVacanciesCount = vacanciesList.count { it.published },
            totalCandidates = candidatesList.size,
            candidatesWithCv = candidatesList.count { it.hasCvUploaded },
            jobAlertsEnabledCount = candidatesList.count { it.jobAlertsEnabled },
            totalApplications = applicationsList.size,
            applicationsByStatus = applicationsList.groupingBy { it.resolvedStatus.label }.eachCount(),
            publishedPortfolioCount = portfolioList.count { it.published },
            publishedTestimonialsCount = testimonialsList.count { it.published },
            publishedTeamCount = teamList.count { it.published },
            activeAdminsCount = adminsList.count { it.active },
            activityLogsCount = logs.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RealAnalyticsSnapshot())

    private val observerJobs = mutableListOf<Job>()
    private var lastVerifiedUid: String? = null
    private var supabaseVerifiedAdmin: AdminUser? = null

    fun verifyUserAccess(user: FirebaseUser?, forceRefresh: Boolean = false) {
        if (user == null) {
            stopObservers()
            lastVerifiedUid = null
            val verifiedSupabase = supabaseVerifiedAdmin
            _sessionState.value = if (verifiedSupabase != null && verifiedSupabase.active) {
                AdminSessionState.Authorized(verifiedSupabase)
            } else {
                AdminSessionState.NotAdmin
            }
            return
        }
        if (!forceRefresh && lastVerifiedUid == user.uid && _sessionState.value is AdminSessionState.Authorized) {
            return
        }
        val adminRepo = adminRepository
        if (adminRepo == null) {
            stopObservers()
            val verifiedSupabase = supabaseVerifiedAdmin
            _sessionState.value = if (verifiedSupabase != null && verifiedSupabase.active) {
                AdminSessionState.Authorized(verifiedSupabase)
            } else {
                AdminSessionState.NotAdmin
            }
            return
        }
        lastVerifiedUid = user.uid
        _sessionState.value = AdminSessionState.Loading
        viewModelScope.launch {
            val result = adminRepo.verifyAndResolveAdmin(user)
            val resolved = result.getOrNull()
            if (resolved != null && resolved.active) {
                _sessionState.value = AdminSessionState.Authorized(resolved)
                val allowed = resolved.resolvedRole.allowedModules()
                if (_selectedModule.value !in allowed) {
                    _selectedModule.value = AdminModule.DASHBOARD_OVERVIEW
                }
                startRoleObservers(resolved)
                cmsRepository?.seedCmsDefaultsIfEmpty(resolved)
            } else {
                stopObservers()
                val verifiedSupabase = supabaseVerifiedAdmin
                _sessionState.value = if (verifiedSupabase != null && verifiedSupabase.active) {
                    AdminSessionState.Authorized(verifiedSupabase)
                } else {
                    AdminSessionState.NotAdmin
                }
            }
        }
    }

    fun authorizeVerifiedSupabaseAdmin(admin: AdminUser) {
        if (!admin.active) {
            _sessionState.value = AdminSessionState.NotAdmin
            return
        }
        supabaseVerifiedAdmin = admin
        _sessionState.value = AdminSessionState.Authorized(admin)
        val allowed = admin.resolvedRole.allowedModules()
        if (_selectedModule.value !in allowed) {
            _selectedModule.value = AdminModule.DASHBOARD_OVERVIEW
        }
    }

    fun clearSupabaseAdminSession() {
        supabaseVerifiedAdmin = null
        if (lastVerifiedUid == null) {
            stopObservers()
            _sessionState.value = AdminSessionState.NotAdmin
        }
    }

    fun selectModule(module: AdminModule) {
        val current = (_sessionState.value as? AdminSessionState.Authorized)?.adminUser ?: return
        if (module in current.resolvedRole.allowedModules()) {
            _selectedModule.value = module
        }
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    private fun startRoleObservers(actor: AdminUser) {
        stopObservers()
        val adminRepo = adminRepository ?: return
        val cmsRepo = cmsRepository ?: return
        val notifRepo = notificationRepository ?: return
        val role = actor.resolvedRole

        observerJobs += viewModelScope.launch {
            adminRepo.observeActivityLogs().collect { _activityLogs.value = it }
        }
        if (role == AdminRole.SUPER_ADMIN || role == AdminRole.REPORTING_ADMIN) {
            observerJobs += viewModelScope.launch { adminRepo.observeClients().collect { _clients.value = it } }
            observerJobs += viewModelScope.launch { adminRepo.observeAdminUsers().collect { _adminUsers.value = it } }
            observerJobs += viewModelScope.launch { adminRepo.observeSettings().collect { _settings.value = it } }
        }
        if (role == AdminRole.SUPER_ADMIN) {
            observerJobs += viewModelScope.launch { adminRepo.observeAdminInvites().collect { _adminInvites.value = it } }
        }
        if (role == AdminRole.SUPER_ADMIN || role == AdminRole.REPORTING_ADMIN || role == AdminRole.CONSULTANT) {
            observerJobs += viewModelScope.launch { adminRepo.observeInquiries(actor).collect { _inquiries.value = it } }
        }
        if (role == AdminRole.SUPER_ADMIN || role == AdminRole.RECRUITMENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
            observerJobs += viewModelScope.launch { cmsRepo.observeAllVacancies().collect { _vacancies.value = it } }
            observerJobs += viewModelScope.launch { adminRepo.observeCandidates().collect { _candidates.value = it } }
            observerJobs += viewModelScope.launch { adminRepo.observeApplications().collect { _applications.value = it } }
            observerJobs += viewModelScope.launch { adminRepo.observeNotifications().collect { _notifications.value = it } }
            observerJobs += viewModelScope.launch { notifRepo.observeControls().collect { _notificationControls.value = it } }
            observerJobs += viewModelScope.launch { notifRepo.observeDeliveryLogs().collect { _deliveryLogs.value = it } }
            observerJobs += viewModelScope.launch { notifRepo.observeAllDeviceTokens().collect { _deviceTokens.value = it } }
        }
        if (role == AdminRole.SUPER_ADMIN || role == AdminRole.CONTENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
            observerJobs += viewModelScope.launch { cmsRepo.observeAllServices().collect { _services.value = it } }
            observerJobs += viewModelScope.launch { cmsRepo.observeAllCategories().collect { _categories.value = it } }
            observerJobs += viewModelScope.launch { cmsRepo.observeSitePages().collect { _sitePages.value = it } }
            observerJobs += viewModelScope.launch { cmsRepo.observePortfolioItems().collect { _portfolioItems.value = it } }
            observerJobs += viewModelScope.launch { cmsRepo.observeTestimonials().collect { _testimonials.value = it } }
            observerJobs += viewModelScope.launch { cmsRepo.observeTeamMembers().collect { _teamMembers.value = it } }
        }
    }

    private fun stopObservers() {
        observerJobs.forEach { it.cancel() }
        observerJobs.clear()
    }

    private fun currentActor(): AdminUser? =
        (_sessionState.value as? AdminSessionState.Authorized)?.adminUser

    private fun launchMutation(successMsg: String, block: suspend (AdminUser) -> Result<Unit>) {
        val actor = currentActor() ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = block(actor)
            _isBusy.value = false
            _feedback.value = if (res.isSuccess) successMsg else (res.exceptionOrNull()?.message ?: "Operation failed.")
        }
    }

    private fun missingBackend(): Result<Unit> =
        Result.failure(IllegalStateException("Firebase admin backend is not available in this build."))

    fun updateClient(client: ClientProfile) = launchMutation("Updated ${client.companyName}.") {
        adminRepository?.updateClientProfileAdmin(it, client) ?: missingBackend()
    }

    fun deleteClient(client: ClientProfile) = launchMutation("Removed client record.") {
        adminRepository?.deleteClientProfileAdmin(it, client) ?: missingBackend()
    }

    fun updateInquiry(
        inquiry: ConsultationInquiry,
        status: InquiryStatus,
        notes: String,
        consultant: AdminUser?
    ) = launchMutation("Updated enquiry ${inquiry.propertyName}.") {
        adminRepository?.updateInquiryAdmin(it, inquiry, status, notes, consultant) ?: missingBackend()
    }

    fun deleteInquiry(inquiry: ConsultationInquiry) = launchMutation("Deleted enquiry.") {
        adminRepository?.deleteInquiryAdmin(it, inquiry) ?: missingBackend()
    }

    fun saveService(service: ServiceOffering) = launchMutation("Saved service ${service.title}.") {
        cmsRepository?.saveService(it, service) ?: missingBackend()
    }

    fun deleteService(service: ServiceOffering) = launchMutation("Deleted service ${service.title}.") {
        cmsRepository?.deleteService(it, service) ?: missingBackend()
    }

    fun saveCategory(cat: ServiceCategoryRecord) = launchMutation("Saved category ${cat.title}.") {
        cmsRepository?.saveCategory(it, cat) ?: missingBackend()
    }

    fun deleteCategory(cat: ServiceCategoryRecord) = launchMutation("Deleted category.") {
        cmsRepository?.deleteCategory(it, cat) ?: missingBackend()
    }

    fun saveVacancy(vacancy: JobVacancyRecord, supabaseAccessToken: String? = null) {
        val actor = currentActor() ?: return
        val cmsRepo = cmsRepository
        if (cmsRepo == null) {
            _feedback.value = "Firebase admin backend is not available in this build."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val saveRes = cmsRepo.saveVacancy(actor, vacancy)
            if (saveRes.isFailure) {
                _isBusy.value = false
                _feedback.value = saveRes.exceptionOrNull()?.message ?: "Failed to save vacancy."
                return@launch
            }
            val controls = _notificationControls.value
            val notifRepo = notificationRepository
            if (notifRepo != null && vacancy.published && controls.autoDispatchOnPublish && _candidates.value.isNotEmpty()) {
                val dispatchRes = notifRepo.dispatchMatchedVacancyNotifications(
                    actor = actor,
                    vacancy = vacancy,
                    candidates = _candidates.value,
                    deviceTokens = _deviceTokens.value,
                    controls = controls,
                    supabaseAccessToken = supabaseAccessToken
                )
                _isBusy.value = false
                dispatchRes.fold(
                    onSuccess = { summary ->
                        _feedback.value = "Published ${vacancy.title} • Auto-notified ${summary.eligibleCount} matched candidate(s) (${summary.excludedCount} ineligible skipped)."
                    },
                    onFailure = {
                        _feedback.value = "Saved vacancy ${vacancy.title} (auto-dispatch skipped: ${it.localizedMessage})."
                    }
                )
            } else {
                _isBusy.value = false
                _feedback.value = "Saved vacancy ${vacancy.title}."
            }
        }
    }

    fun dispatchMatchedVacancyAlerts(vacancy: JobVacancyRecord, supabaseAccessToken: String? = null) {
        val actor = currentActor() ?: return
        val notifRepo = notificationRepository
        if (notifRepo == null) {
            _feedback.value = "Firebase admin backend is not available in this build."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = notifRepo.dispatchMatchedVacancyNotifications(
                actor = actor,
                vacancy = vacancy,
                candidates = _candidates.value,
                deviceTokens = _deviceTokens.value,
                controls = _notificationControls.value,
                supabaseAccessToken = supabaseAccessToken
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { summary ->
                    _feedback.value = "Notified ${summary.eligibleCount} eligible candidate(s) for ${summary.vacancyTitle} (${summary.excludedCount} excluded)."
                },
                onFailure = {
                    _feedback.value = it.localizedMessage ?: "Dispatch failed."
                }
            )
        }
    }

    fun saveNotificationControls(controls: NotificationControlSettings) =
        launchMutation("Saved notification channel & matching controls.") {
            notificationRepository?.saveControls(it, controls) ?: missingBackend()
        }

    fun deleteVacancy(vacancy: JobVacancyRecord) = launchMutation("Deleted vacancy.") {
        cmsRepository?.deleteVacancy(it, vacancy) ?: missingBackend()
    }

    fun toggleCandidateAlerts(candidate: CandidateProfile) = launchMutation("Updated alert preference for ${candidate.fullName}.") {
        adminRepository?.toggleCandidateAlertsAdmin(it, candidate) ?: missingBackend()
    }

    fun clearCandidateCv(candidate: CandidateProfile) = launchMutation("Cleared CV vault record for ${candidate.fullName}.") {
        adminRepository?.removeCandidateCvAdmin(it, candidate) ?: missingBackend()
    }

    fun updateApplicationStatus(
        app: JobApplication,
        status: ApplicationStatus,
        notifyCandidate: Boolean,
        supabaseAccessToken: String? = null
    ) = launchMutation("Updated application to ${status.label}.") { actor ->
        val adminRepo = adminRepository ?: return@launchMutation missingBackend()
        val controls = _notificationControls.value
        val shouldNotify = notifyCandidate && controls.enableApplicationStatusAlerts
        val res = adminRepo.updateApplicationStatusAdmin(actor, app, status, shouldNotify)
        if (res.isSuccess && shouldNotify) {
            notificationRepository?.logApplicationStatusDispatch(
                actor = actor,
                application = app,
                newStatus = status,
                deviceTokens = _deviceTokens.value,
                controls = controls,
                supabaseAccessToken = supabaseAccessToken
            )
        }
        res
    }

    fun deleteApplication(app: JobApplication) = launchMutation("Deleted application.") {
        adminRepository?.deleteApplicationAdmin(it, app) ?: missingBackend()
    }

    fun sendNotification(
        targetUserId: String,
        roleId: String,
        title: String,
        message: String,
        department: String,
        location: String
    ) = launchMutation("Dispatched job notification.") {
        adminRepository?.sendCandidateNotificationAdmin(it, targetUserId, roleId, title, message, department, location)
            ?: missingBackend()
    }

    fun deleteNotification(notification: JobNotification) = launchMutation("Deleted notification.") {
        adminRepository?.deleteNotificationAdmin(it, notification) ?: missingBackend()
    }

    fun saveSitePage(page: SitePageContent) = launchMutation("Updated page ${page.pageTitle}.") {
        cmsRepository?.saveSitePage(it, page) ?: missingBackend()
    }

    fun deleteSitePage(page: SitePageContent) = launchMutation("Deleted site page.") {
        cmsRepository?.deleteSitePage(it, page) ?: missingBackend()
    }

    fun savePortfolioItem(item: PortfolioItemRecord) = launchMutation("Saved portfolio item ${item.propertyName}.") {
        cmsRepository?.savePortfolioItem(it, item) ?: missingBackend()
    }

    fun deletePortfolioItem(item: PortfolioItemRecord) = launchMutation("Deleted portfolio item.") {
        cmsRepository?.deletePortfolioItem(it, item) ?: missingBackend()
    }

    fun saveTestimonial(item: TestimonialRecord) = launchMutation("Saved testimonial.") {
        cmsRepository?.saveTestimonial(it, item) ?: missingBackend()
    }

    fun deleteTestimonial(item: TestimonialRecord) = launchMutation("Deleted testimonial.") {
        cmsRepository?.deleteTestimonial(it, item) ?: missingBackend()
    }

    fun saveTeamMember(member: TeamMemberRecord) = launchMutation("Saved team profile ${member.fullName}.") {
        cmsRepository?.saveTeamMember(it, member) ?: missingBackend()
    }

    fun deleteTeamMember(member: TeamMemberRecord) = launchMutation("Deleted team profile.") {
        cmsRepository?.deleteTeamMember(it, member) ?: missingBackend()
    }

    fun inviteAdmin(email: String, displayName: String, role: AdminRole) = launchMutation("Saved admin invite for $email.") {
        adminRepository?.saveAdminInvite(it, AdminInvite(email = email, displayName = displayName, role = role.name, active = true))
            ?: missingBackend()
    }

    fun revokeInvite(invite: AdminInvite) = launchMutation("Revoked invite.") {
        adminRepository?.deleteAdminInvite(it, invite) ?: missingBackend()
    }

    fun updateAdminRoleAndStatus(target: AdminUser, role: AdminRole, active: Boolean) = launchMutation("Updated ${target.email}.") {
        adminRepository?.updateAdminUser(it, target.copy(role = role.name, active = active)) ?: missingBackend()
    }

    fun savePlatformSettings(settings: AdminSettings) = launchMutation("Saved platform settings.") {
        adminRepository?.saveSettings(it, settings) ?: missingBackend()
    }
}
