package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.PrivateCvMetadata
import com.example.data.CandidateProfile
import com.example.data.ClientProfile
import com.example.data.ConsultationInquiry
import com.example.data.EnquiryConfirmation
import com.example.data.HospitalityRole
import com.example.data.JobApplication
import com.example.data.JobNotification
import com.example.data.MilaCatalog
import com.example.data.MilaRepository
import com.example.data.SavedRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MandatesViewModel(
    private val repository: MilaRepository,
    private val currentUserId: String
) : ViewModel() {

    val inquiriesState: StateFlow<UiState<List<ConsultationInquiry>>> =
        repository.observeConsultationInquiries(currentUserId)
            .map<List<ConsultationInquiry>, UiState<List<ConsultationInquiry>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing consultation inquiries", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load consultation mandates."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val applicationsState: StateFlow<UiState<List<JobApplication>>> =
        repository.observeJobApplications(currentUserId)
            .map<List<JobApplication>, UiState<List<JobApplication>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing job applications", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load recruitment applications."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val savedRolesState: StateFlow<UiState<List<SavedRole>>> =
        repository.observeSavedRoles(currentUserId)
            .map<List<SavedRole>, UiState<List<SavedRole>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing saved roles", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load shortlisted roles."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val clientProfileState: StateFlow<UiState<ClientProfile?>> =
        repository.observeClientProfile(currentUserId)
            .map<ClientProfile?, UiState<ClientProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing client profile", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load client profile."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val candidateProfileState: StateFlow<UiState<CandidateProfile?>> =
        repository.observeCandidateProfile(currentUserId)
            .map<CandidateProfile?, UiState<CandidateProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing candidate profile", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load candidate profile."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val jobNotificationsState: StateFlow<UiState<List<JobNotification>>> =
        repository.observeJobNotifications(currentUserId)
            .map<List<JobNotification>, UiState<List<JobNotification>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing job notifications", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load job notifications."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _latestEnquiryConfirmation = MutableStateFlow<EnquiryConfirmation?>(null)
    val latestEnquiryConfirmation: StateFlow<EnquiryConfirmation?> = _latestEnquiryConfirmation.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun dismissEnquiryConfirmation() {
        _latestEnquiryConfirmation.value = null
    }

    fun saveClientProfile(
        profile: ClientProfile,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.saveClientProfile(profile)
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Client profile saved securely."
                    onSuccess()
                },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Unable to save client profile."
                }
            )
        }
    }

    fun submitInquiry(
        inquiry: ConsultationInquiry,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.submitConsultationInquiry(inquiry)
            _isSubmitting.value = false
            result.fold(
                onSuccess = { docId ->
                    val refCode = "MILA-${docId.take(6).uppercase()}"
                    _latestEnquiryConfirmation.value = EnquiryConfirmation(
                        referenceCode = refCode,
                        serviceType = inquiry.resolvedServiceType,
                        propertyName = inquiry.propertyName.trim(),
                        city = inquiry.city.trim(),
                        scaleSummary = inquiry.scaleSummary.trim(),
                        contactName = inquiry.contactName.trim(),
                        contactEmail = inquiry.contactEmail.trim()
                    )
                    _feedbackMessage.value = "Enquiry confirmed ($refCode). A senior MILA partner will respond within 1 business day."
                    onSuccess()
                },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Failed to submit consultation mandate."
                }
            )
        }
    }

    fun updateInquiryNotes(inquiryId: String, notes: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.updateConsultationNotes(inquiryId, notes)
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Mandate brief updated."
                    onSuccess()
                },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Unable to update brief."
                }
            )
        }
    }

    fun withdrawInquiry(inquiryId: String) {
        viewModelScope.launch {
            val result = repository.withdrawConsultationInquiry(inquiryId)
            result.fold(
                onSuccess = { _feedbackMessage.value = "Consultation mandate withdrawn." },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Unable to withdraw mandate."
                }
            )
        }
    }

    fun deleteInquiry(inquiryId: String) {
        viewModelScope.launch {
            val result = repository.deleteConsultationInquiry(inquiryId)
            result.fold(
                onSuccess = { _feedbackMessage.value = "Mandate removed from dossier." },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Unable to remove mandate."
                }
            )
        }
    }

    fun submitApplication(
        application: JobApplication,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.submitJobApplication(application)
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Application filed for ${application.roleTitle}."
                    repository.createJobNotification(
                        notificationId = "app_${application.roleId}",
                        notification = JobNotification(
                            userId = currentUserId,
                            roleId = application.roleId,
                            title = "Application Submitted: ${application.roleTitle}",
                            message = "MILA Talent received your dossier for ${application.roleTitle} (${application.location}). Status: Application Submitted.",
                            department = "MILA Talent Application",
                            location = application.location,
                            isRead = false
                        )
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Failed to submit application."
                }
            )
        }
    }

    fun saveCandidateProfile(
        profile: CandidateProfile,
        triggerAlertsSync: Boolean = true,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.saveCandidateProfile(profile)
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Candidate profile saved securely."
                    if (triggerAlertsSync && profile.jobAlertsEnabled) {
                        publishMatchedJobAlerts(profile)
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Unable to save candidate profile."
                }
            )
        }
    }

    fun toggleJobAlerts(currentProfile: CandidateProfile, enabled: Boolean) {
        val updated = currentProfile.copy(jobAlertsEnabled = enabled)
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.saveCandidateProfile(updated)
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    if (enabled) {
                        publishMatchedJobAlerts(updated)
                        _feedbackMessage.value = "Job alerts activated. Matching mandates added to Notifications."
                    } else {
                        _feedbackMessage.value = "Job alerts deactivated."
                    }
                },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Unable to update job alert preference."
                }
            )
        }
    }

    fun attachCvToProfile(currentProfile: CandidateProfile, metadata: PrivateCvMetadata) {
        val updated = currentProfile.copy(
            cvFileName = metadata.fileName,
            cvMimeType = metadata.mimeType,
            cvFileSizeBytes = metadata.fileSizeBytes,
            cvStoragePath = metadata.storagePath,
            cvUploadedAtMillis = metadata.uploadedAtMillis
        )
        viewModelScope.launch {
            val result = repository.saveCandidateProfile(updated)
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "CV '${metadata.fileName}' saved to private storage."
                },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Failed to save CV metadata."
                }
            )
        }
    }

    fun removeCvFromProfile(currentProfile: CandidateProfile) {
        val cleared = currentProfile.copy(
            cvFileName = null,
            cvMimeType = null,
            cvFileSizeBytes = null,
            cvStoragePath = null,
            cvUploadedAtMillis = null
        )
        viewModelScope.launch {
            val result = repository.saveCandidateProfile(cleared)
            result.fold(
                onSuccess = { _feedbackMessage.value = "CV deleted from private storage." },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Unable to delete CV."
                }
            )
        }
    }

    fun markJobNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markJobNotificationRead(notificationId)
        }
    }

    fun deleteJobNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteJobNotification(notificationId)
        }
    }

    private suspend fun publishMatchedJobAlerts(profile: CandidateProfile) {
        val preferredTokens = profile.preferredJobLocations
            .split(",", ";", "/")
            .map { it.trim().lowercase() }
            .filter { it.length >= 3 }

        val deptLabel = profile.resolvedDepartment.label.lowercase()
        val matched = MilaCatalog.openRoles.filter { role ->
            val locMatch = preferredTokens.any { token ->
                role.location.lowercase().contains(token) ||
                    role.cityRegion.lowercase().contains(token)
            }
            val deptMatch = deptLabel.contains(role.category.displayLabel.lowercase().take(5)) ||
                role.category.name.equals(profile.department, ignoreCase = true)
            locMatch || deptMatch
        }.ifEmpty { MilaCatalog.openRoles.take(2) }

        matched.take(3).forEach { role ->
            repository.createJobNotification(
                notificationId = "alert_${role.id}",
                notification = JobNotification(
                    userId = currentUserId,
                    roleId = role.id,
                    title = "Vacancy Match: ${role.title}",
                    message = "${role.propertyTier} in ${role.location} (${role.experienceRequired}). Matches your MILA Talent alert profile.",
                    department = role.category.displayLabel,
                    location = role.location,
                    isRead = false
                )
            )
        }
    }

    fun withdrawApplication(applicationId: String) {
        viewModelScope.launch {
            val result = repository.withdrawJobApplication(applicationId)
            result.fold(
                onSuccess = { _feedbackMessage.value = "Application withdrawn." },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Unable to withdraw application."
                }
            )
        }
    }

    fun deleteApplication(applicationId: String) {
        viewModelScope.launch {
            val result = repository.deleteJobApplication(applicationId)
            result.fold(
                onSuccess = { _feedbackMessage.value = "Application removed from dossier." },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Unable to remove application."
                }
            )
        }
    }

    fun toggleSavedRole(role: HospitalityRole, currentlySaved: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleSavedRole(role, currentlySaved)
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = if (currentlySaved) {
                        "Removed ${role.title} from shortlist."
                    } else {
                        "Shortlisted ${role.title}."
                    }
                },
                onFailure = { error ->
                    _feedbackMessage.value = error.localizedMessage ?: "Unable to update shortlist."
                }
            )
        }
    }

    private companion object {
        const val TAG = "MandatesVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
