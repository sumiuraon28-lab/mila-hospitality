package com.example.ui

import android.content.Context
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
import com.example.data.PrivateCvLocalVault
import com.example.data.RealAnalyticsSnapshot
import com.example.data.ServiceCategoryRecord
import com.example.data.ServiceOffering
import com.example.data.SitePageContent
import com.example.data.SupabaseAdminRepository
import com.example.data.SupabaseAdminSession
import com.example.data.TeamMemberRecord
import com.example.data.TestimonialRecord
import com.example.notifications.CandidateDeviceToken
import com.example.notifications.JobMatchingEngine
import com.example.notifications.NotificationControlSettings
import com.example.notifications.NotificationDeliveryLog
import com.example.notifications.NotificationDispatchRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val notificationRepository: NotificationDispatchRepository? = null,
    private val supabaseAdminRepository: SupabaseAdminRepository = SupabaseAdminRepository()
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
    private var supabaseAdminSession: SupabaseAdminSession? = null

    fun activeSupabaseAdminToken(): String? = supabaseAdminSession?.accessToken

    fun verifyUserAccess(user: FirebaseUser?, forceRefresh: Boolean = false) {
        if (user == null) {
            stopObservers()
            lastVerifiedUid = null
            val verifiedSupabase = supabaseAdminSession?.admin
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
            val verifiedSupabase = supabaseAdminSession?.admin
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
                val verifiedSupabase = supabaseAdminSession?.admin
                _sessionState.value = if (verifiedSupabase != null && verifiedSupabase.active) {
                    AdminSessionState.Authorized(verifiedSupabase)
                } else {
                    AdminSessionState.NotAdmin
                }
            }
        }
    }

    fun authorizeVerifiedSupabaseAdmin(session: SupabaseAdminSession) {
        val admin = session.admin
        if (!admin.active) {
            _sessionState.value = AdminSessionState.NotAdmin
            return
        }
        stopObservers()
        supabaseAdminSession = session
        _sessionState.value = AdminSessionState.Authorized(admin)
        val allowed = admin.resolvedRole.allowedModules()
        if (_selectedModule.value !in allowed) {
            _selectedModule.value = AdminModule.DASHBOARD_OVERVIEW
        }
        refreshSupabaseAdminData()
    }

    fun clearSupabaseAdminSession() {
        supabaseAdminSession = null
        if (lastVerifiedUid == null) {
            stopObservers()
            clearCachedAdminData()
            _sessionState.value = AdminSessionState.NotAdmin
        }
    }

    fun refreshData() {
        val supaSession = supabaseAdminSession
        if (supaSession != null) {
            refreshSupabaseAdminData(showSuccessToast = true)
        } else {
            _feedback.value = "Live data is synchronized in real time."
        }
    }

    private fun refreshSupabaseAdminData(showSuccessToast: Boolean = false) {
        val session = supabaseAdminSession ?: return
        val token = session.accessToken
        val role = session.admin.resolvedRole

        viewModelScope.launch {
            _isBusy.value = true
            try {
                coroutineScope {
                    val logsDef = async { supabaseAdminRepository.fetchActivityLogs(token) }
                    val clientsDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchClients(token) }
                    } else null
                    val adminsDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchAdminUsers(token) }
                    } else null
                    val settingsDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchPlatformSettings(token) }
                    } else null
                    val invitesDef = if (role == AdminRole.SUPER_ADMIN) {
                        async { supabaseAdminRepository.fetchAdminInvites(token) }
                    } else null
                    val inquiriesDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.REPORTING_ADMIN || role == AdminRole.CONSULTANT) {
                        async { supabaseAdminRepository.fetchInquiries(token) }
                    } else null
                    val vacanciesDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.RECRUITMENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchVacancies(token) }
                    } else null
                    val candidatesDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.RECRUITMENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchCandidates(token) }
                    } else null
                    val appsDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.RECRUITMENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchApplications(token) }
                    } else null
                    val notifDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.RECRUITMENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchNotifications(token) }
                    } else null
                    val notifCtrlDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.RECRUITMENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchNotificationControls(token) }
                    } else null
                    val tokensDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.RECRUITMENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchDeviceTokens(token) }
                    } else null
                    val servicesDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.CONTENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchServices(token) }
                    } else null
                    val catsDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.CONTENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchCategories(token) }
                    } else null
                    val pagesDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.CONTENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchSitePages(token) }
                    } else null
                    val portfolioDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.CONTENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchPortfolioItems(token) }
                    } else null
                    val testDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.CONTENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchTestimonials(token) }
                    } else null
                    val teamDef = if (role == AdminRole.SUPER_ADMIN || role == AdminRole.CONTENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                        async { supabaseAdminRepository.fetchTeamMembers(token) }
                    } else null

                    logsDef.await().onSuccess { _activityLogs.value = it }
                    clientsDef?.await()?.onSuccess { _clients.value = it }
                    adminsDef?.await()?.onSuccess { _adminUsers.value = it }
                    settingsDef?.await()?.onSuccess { s -> if (s != null) _settings.value = s }
                    invitesDef?.await()?.onSuccess { _adminInvites.value = it }
                    inquiriesDef?.await()?.onSuccess { _inquiries.value = it }
                    vacanciesDef?.await()?.onSuccess { _vacancies.value = it }
                    candidatesDef?.await()?.onSuccess { _candidates.value = it }
                    appsDef?.await()?.onSuccess { _applications.value = it }
                    notifDef?.await()?.onSuccess { _notifications.value = it }
                    notifCtrlDef?.await()?.onSuccess { c -> if (c != null) _notificationControls.value = c }
                    tokensDef?.await()?.onSuccess { _deviceTokens.value = it }
                    servicesDef?.await()?.onSuccess { _services.value = it }
                    catsDef?.await()?.onSuccess { _categories.value = it }
                    pagesDef?.await()?.onSuccess { _sitePages.value = it }
                    portfolioDef?.await()?.onSuccess { _portfolioItems.value = it }
                    testDef?.await()?.onSuccess { _testimonials.value = it }
                    teamDef?.await()?.onSuccess { _teamMembers.value = it }
                }
                if (showSuccessToast) {
                    _feedback.value = "Dashboard refreshed with latest live records."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun clearCachedAdminData() {
        _clients.value = emptyList()
        _inquiries.value = emptyList()
        _services.value = emptyList()
        _categories.value = emptyList()
        _vacancies.value = emptyList()
        _candidates.value = emptyList()
        _applications.value = emptyList()
        _notifications.value = emptyList()
        _sitePages.value = emptyList()
        _portfolioItems.value = emptyList()
        _testimonials.value = emptyList()
        _teamMembers.value = emptyList()
        _adminUsers.value = emptyList()
        _adminInvites.value = emptyList()
        _activityLogs.value = emptyList()
        _deliveryLogs.value = emptyList()
        _deviceTokens.value = emptyList()
        _selectedModule.value = AdminModule.DASHBOARD_OVERVIEW
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

    private fun launchDualMutation(
        successMsg: String,
        supabaseAction: suspend (SupabaseAdminSession) -> Result<Unit>,
        firestoreAction: suspend (AdminUser) -> Result<Unit>
    ) {
        val actor = currentActor() ?: return
        val supaSession = supabaseAdminSession
        viewModelScope.launch {
            _isBusy.value = true
            val res = if (supaSession != null) {
                supabaseAction(supaSession)
            } else {
                firestoreAction(actor)
            }
            _isBusy.value = false
            _feedback.value = if (res.isSuccess) successMsg else (res.exceptionOrNull()?.message ?: "Operation failed.")
            if (res.isSuccess && supaSession != null) {
                refreshSupabaseAdminData()
            }
        }
    }

    private fun missingBackend(): Result<Unit> =
        Result.failure(IllegalStateException("Admin backend session is not active. Please sign in again."))

    fun updateClient(client: ClientProfile) = launchDualMutation(
        successMsg = "Updated ${client.companyName}.",
        supabaseAction = { supabaseAdminRepository.updateClient(it, client) },
        firestoreAction = { adminRepository?.updateClientProfileAdmin(it, client) ?: missingBackend() }
    )

    fun deleteClient(client: ClientProfile) = launchDualMutation(
        successMsg = "Removed client record.",
        supabaseAction = { supabaseAdminRepository.deleteClient(it, client) },
        firestoreAction = { adminRepository?.deleteClientProfileAdmin(it, client) ?: missingBackend() }
    )

    fun updateInquiry(
        inquiry: ConsultationInquiry,
        status: InquiryStatus,
        notes: String,
        consultant: AdminUser?
    ) = launchDualMutation(
        successMsg = "Updated enquiry ${inquiry.propertyName}.",
        supabaseAction = { supabaseAdminRepository.updateInquiry(it, inquiry, status, notes, consultant) },
        firestoreAction = { adminRepository?.updateInquiryAdmin(it, inquiry, status, notes, consultant) ?: missingBackend() }
    )

    fun deleteInquiry(inquiry: ConsultationInquiry) = launchDualMutation(
        successMsg = "Deleted enquiry.",
        supabaseAction = { supabaseAdminRepository.deleteInquiry(it, inquiry) },
        firestoreAction = { adminRepository?.deleteInquiryAdmin(it, inquiry) ?: missingBackend() }
    )

    fun saveService(service: ServiceOffering) = launchDualMutation(
        successMsg = "Saved service ${service.title}.",
        supabaseAction = { supabaseAdminRepository.saveService(it, service) },
        firestoreAction = { cmsRepository?.saveService(it, service) ?: missingBackend() }
    )

    fun deleteService(service: ServiceOffering) = launchDualMutation(
        successMsg = "Deleted service ${service.title}.",
        supabaseAction = { supabaseAdminRepository.deleteService(it, service) },
        firestoreAction = { cmsRepository?.deleteService(it, service) ?: missingBackend() }
    )

    fun saveCategory(cat: ServiceCategoryRecord) = launchDualMutation(
        successMsg = "Saved category ${cat.title}.",
        supabaseAction = { supabaseAdminRepository.saveCategory(it, cat) },
        firestoreAction = { cmsRepository?.saveCategory(it, cat) ?: missingBackend() }
    )

    fun deleteCategory(cat: ServiceCategoryRecord) = launchDualMutation(
        successMsg = "Deleted category.",
        supabaseAction = { supabaseAdminRepository.deleteCategory(it, cat) },
        firestoreAction = { cmsRepository?.deleteCategory(it, cat) ?: missingBackend() }
    )

    fun saveVacancy(vacancy: JobVacancyRecord, supabaseAccessToken: String? = null) {
        val actor = currentActor() ?: return
        val supaSession = supabaseAdminSession
        if (supaSession != null) {
            viewModelScope.launch {
                _isBusy.value = true
                val res = supabaseAdminRepository.saveVacancy(supaSession, vacancy)
                if (res.isFailure) {
                    _isBusy.value = false
                    _feedback.value = res.exceptionOrNull()?.message ?: "Failed to save vacancy."
                    return@launch
                }
                val controls = _notificationControls.value
                var autoMsg = "Saved vacancy ${vacancy.title}."
                if (vacancy.published && controls.autoDispatchOnPublish && _candidates.value.isNotEmpty()) {
                    val matched = JobMatchingEngine.findEligibleCandidates(vacancy, _candidates.value, controls)
                    matched.forEach { m ->
                        if (m.candidate.userId.isNotBlank()) {
                            supabaseAdminRepository.sendNotification(
                                session = supaSession,
                                targetUserId = m.candidate.userId,
                                roleId = vacancy.id.ifBlank { vacancy.title },
                                title = "New Role Match: ${vacancy.title}",
                                message = "${vacancy.propertyTier} • ${vacancy.location}. ${vacancy.compensation}",
                                department = vacancy.resolvedCategory.displayLabel,
                                location = vacancy.location
                            )
                        }
                    }
                    autoMsg = "Published ${vacancy.title} • Notified ${matched.size} matched candidate(s)."
                }
                _isBusy.value = false
                _feedback.value = autoMsg
                refreshSupabaseAdminData()
            }
            return
        }

        val cmsRepo = cmsRepository
        if (cmsRepo == null) {
            _feedback.value = "Admin backend session is not active. Please sign in again."
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
        val supaSession = supabaseAdminSession
        if (supaSession != null) {
            viewModelScope.launch {
                _isBusy.value = true
                val evaluated = JobMatchingEngine.evaluateAllCandidates(vacancy, _candidates.value, _notificationControls.value)
                val eligible = evaluated.filter { it.eligible }
                eligible.forEach { m ->
                    if (m.candidate.userId.isNotBlank()) {
                        supabaseAdminRepository.sendNotification(
                            session = supaSession,
                            targetUserId = m.candidate.userId,
                            roleId = vacancy.id.ifBlank { vacancy.title },
                            title = "Matched Opening: ${vacancy.title}",
                            message = "${vacancy.propertyTier} • ${vacancy.location}. ${vacancy.summary}",
                            department = vacancy.resolvedCategory.displayLabel,
                            location = vacancy.location
                        )
                    }
                }
                _isBusy.value = false
                _feedback.value = "Notified ${eligible.size} eligible candidate(s) for ${vacancy.title} (${evaluated.size - eligible.size} excluded)."
                refreshSupabaseAdminData()
            }
            return
        }

        val notifRepo = notificationRepository
        if (notifRepo == null) {
            _feedback.value = "Admin backend session is not active. Please sign in again."
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

    fun saveNotificationControls(controls: NotificationControlSettings) = launchDualMutation(
        successMsg = "Saved notification channel & matching controls.",
        supabaseAction = { supabaseAdminRepository.saveNotificationControls(it, controls) },
        firestoreAction = { notificationRepository?.saveControls(it, controls) ?: missingBackend() }
    )

    fun deleteVacancy(vacancy: JobVacancyRecord) = launchDualMutation(
        successMsg = "Deleted vacancy.",
        supabaseAction = { supabaseAdminRepository.deleteVacancy(it, vacancy) },
        firestoreAction = { cmsRepository?.deleteVacancy(it, vacancy) ?: missingBackend() }
    )

    fun updateCandidate(candidate: CandidateProfile) = launchDualMutation(
        successMsg = "Updated candidate ${candidate.fullName}.",
        supabaseAction = { supabaseAdminRepository.updateCandidate(it, candidate) },
        firestoreAction = { adminRepository?.updateCandidateProfileAdmin(it, candidate) ?: missingBackend() }
    )

    fun deleteCandidate(candidate: CandidateProfile) = launchDualMutation(
        successMsg = "Deleted candidate ${candidate.fullName}.",
        supabaseAction = { supabaseAdminRepository.deleteCandidate(it, candidate) },
        firestoreAction = { adminRepository?.deleteCandidateProfileAdmin(it, candidate) ?: missingBackend() }
    )

    fun toggleCandidateAlerts(candidate: CandidateProfile) = launchDualMutation(
        successMsg = "Updated alert preference for ${candidate.fullName}.",
        supabaseAction = { supabaseAdminRepository.toggleCandidateAlerts(it, candidate) },
        firestoreAction = { adminRepository?.toggleCandidateAlertsAdmin(it, candidate) ?: missingBackend() }
    )

    fun clearCandidateCv(candidate: CandidateProfile) = launchDualMutation(
        successMsg = "Cleared CV vault record for ${candidate.fullName}.",
        supabaseAction = { supabaseAdminRepository.clearCandidateCv(it, candidate) },
        firestoreAction = { adminRepository?.removeCandidateCvAdmin(it, candidate) ?: missingBackend() }
    )

    fun downloadCandidateCv(
        context: Context,
        candidate: CandidateProfile,
        onReadyToOpen: (PrivateCvLocalVault.SavedAdminCvDownload) -> Unit
    ) {
        if (currentActor() == null) return
        val fileName = candidate.cvFileName?.takeIf { it.isNotBlank() }
            ?: candidate.cvStoragePath?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "${candidate.fullName.ifBlank { "candidate" }}_CV.pdf"
        val mimeType = candidate.cvMimeType?.takeIf { it.isNotBlank() } ?: "application/pdf"
        val storagePath = candidate.cvStoragePath

        viewModelScope.launch {
            _isBusy.value = true
            try {
                var cloudError: String? = null
                val supaSession = supabaseAdminSession
                var bytes: ByteArray? = null

                if (supaSession != null && !storagePath.isNullOrBlank()) {
                    val cloudResult = supabaseAdminRepository.downloadPrivateCvBytes(
                        session = supaSession,
                        candidate = candidate,
                        storagePath = storagePath
                    )
                    cloudResult.fold(
                        onSuccess = { bytes = it },
                        onFailure = { cloudError = it.localizedMessage }
                    )
                }

                if (bytes == null) {
                    bytes = PrivateCvLocalVault.readOwnerOnlyCopy(
                        context = context,
                        userId = candidate.userId,
                        storagePath = storagePath
                    )
                }

                val resolvedBytes = bytes
                if (resolvedBytes == null || resolvedBytes.isEmpty()) {
                    _feedback.value = cloudError
                        ?: "CV file for ${candidate.fullName} is not available in private storage yet."
                    return@launch
                }

                val savedResult = PrivateCvLocalVault.saveDownloadedCvForAdmin(
                    context = context,
                    fileName = fileName,
                    mimeType = mimeType,
                    bytes = resolvedBytes
                )
                savedResult.fold(
                    onSuccess = { downloaded ->
                        _feedback.value = if (downloaded.savedToDeviceDownloads) {
                            "Downloaded ${downloaded.fileName} to Downloads/MILA_CVs."
                        } else {
                            "Downloaded ${downloaded.fileName} and ready to view."
                        }
                        onReadyToOpen(downloaded)
                    },
                    onFailure = { err ->
                        _feedback.value = err.localizedMessage ?: "Unable to save CV on device."
                    }
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun updateApplicationStatus(
        app: JobApplication,
        status: ApplicationStatus,
        notifyCandidate: Boolean,
        supabaseAccessToken: String? = null
    ) = launchDualMutation(
        successMsg = "Updated application to ${status.label}.",
        supabaseAction = {
            val shouldNotify = notifyCandidate && _notificationControls.value.enableApplicationStatusAlerts
            supabaseAdminRepository.updateApplicationStatus(it, app, status, shouldNotify)
        },
        firestoreAction = { actor ->
            val adminRepo = adminRepository ?: return@launchDualMutation missingBackend()
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
    )

    fun deleteApplication(app: JobApplication) = launchDualMutation(
        successMsg = "Deleted application.",
        supabaseAction = { supabaseAdminRepository.deleteApplication(it, app) },
        firestoreAction = { adminRepository?.deleteApplicationAdmin(it, app) ?: missingBackend() }
    )

    fun sendNotification(
        targetUserId: String,
        roleId: String,
        title: String,
        message: String,
        department: String,
        location: String
    ) = launchDualMutation(
        successMsg = "Dispatched job notification.",
        supabaseAction = {
            supabaseAdminRepository.sendNotification(it, targetUserId, roleId, title, message, department, location)
        },
        firestoreAction = {
            adminRepository?.sendCandidateNotificationAdmin(it, targetUserId, roleId, title, message, department, location)
                ?: missingBackend()
        }
    )

    fun deleteNotification(notification: JobNotification) = launchDualMutation(
        successMsg = "Deleted notification.",
        supabaseAction = { supabaseAdminRepository.deleteNotification(it, notification) },
        firestoreAction = { adminRepository?.deleteNotificationAdmin(it, notification) ?: missingBackend() }
    )

    fun saveSitePage(page: SitePageContent) = launchDualMutation(
        successMsg = "Updated page ${page.pageTitle}.",
        supabaseAction = { supabaseAdminRepository.saveSitePage(it, page) },
        firestoreAction = { cmsRepository?.saveSitePage(it, page) ?: missingBackend() }
    )

    fun deleteSitePage(page: SitePageContent) = launchDualMutation(
        successMsg = "Deleted site page.",
        supabaseAction = { supabaseAdminRepository.deleteSitePage(it, page) },
        firestoreAction = { cmsRepository?.deleteSitePage(it, page) ?: missingBackend() }
    )

    fun savePortfolioItem(item: PortfolioItemRecord) = launchDualMutation(
        successMsg = "Saved portfolio item ${item.propertyName}.",
        supabaseAction = { supabaseAdminRepository.savePortfolioItem(it, item) },
        firestoreAction = { cmsRepository?.savePortfolioItem(it, item) ?: missingBackend() }
    )

    fun deletePortfolioItem(item: PortfolioItemRecord) = launchDualMutation(
        successMsg = "Deleted portfolio item.",
        supabaseAction = { supabaseAdminRepository.deletePortfolioItem(it, item) },
        firestoreAction = { cmsRepository?.deletePortfolioItem(it, item) ?: missingBackend() }
    )

    fun saveTestimonial(item: TestimonialRecord) = launchDualMutation(
        successMsg = "Saved testimonial.",
        supabaseAction = { supabaseAdminRepository.saveTestimonial(it, item) },
        firestoreAction = { cmsRepository?.saveTestimonial(it, item) ?: missingBackend() }
    )

    fun deleteTestimonial(item: TestimonialRecord) = launchDualMutation(
        successMsg = "Deleted testimonial.",
        supabaseAction = { supabaseAdminRepository.deleteTestimonial(it, item) },
        firestoreAction = { cmsRepository?.deleteTestimonial(it, item) ?: missingBackend() }
    )

    fun saveTeamMember(member: TeamMemberRecord) = launchDualMutation(
        successMsg = "Saved team profile ${member.fullName}.",
        supabaseAction = { supabaseAdminRepository.saveTeamMember(it, member) },
        firestoreAction = { cmsRepository?.saveTeamMember(it, member) ?: missingBackend() }
    )

    fun deleteTeamMember(member: TeamMemberRecord) = launchDualMutation(
        successMsg = "Deleted team profile.",
        supabaseAction = { supabaseAdminRepository.deleteTeamMember(it, member) },
        firestoreAction = { cmsRepository?.deleteTeamMember(it, member) ?: missingBackend() }
    )

    fun inviteAdmin(email: String, displayName: String, role: AdminRole) = launchDualMutation(
        successMsg = "Saved admin invite for $email.",
        supabaseAction = {
            supabaseAdminRepository.saveAdminInvite(it, AdminInvite(email = email, displayName = displayName, role = role.name, active = true))
        },
        firestoreAction = {
            adminRepository?.saveAdminInvite(it, AdminInvite(email = email, displayName = displayName, role = role.name, active = true))
                ?: missingBackend()
        }
    )

    fun revokeInvite(invite: AdminInvite) = launchDualMutation(
        successMsg = "Revoked invite.",
        supabaseAction = { supabaseAdminRepository.deleteAdminInvite(it, invite) },
        firestoreAction = { adminRepository?.deleteAdminInvite(it, invite) ?: missingBackend() }
    )

    fun updateAdminRoleAndStatus(target: AdminUser, role: AdminRole, active: Boolean) = launchDualMutation(
        successMsg = "Updated ${target.email}.",
        supabaseAction = { supabaseAdminRepository.updateAdminUser(it, target.copy(role = role.name, active = active)) },
        firestoreAction = { adminRepository?.updateAdminUser(it, target.copy(role = role.name, active = active)) ?: missingBackend() }
    )

    fun savePlatformSettings(settings: AdminSettings) = launchDualMutation(
        successMsg = "Saved platform settings.",
        supabaseAction = { supabaseAdminRepository.savePlatformSettings(it, settings) },
        firestoreAction = { adminRepository?.saveSettings(it, settings) ?: missingBackend() }
    )
}
