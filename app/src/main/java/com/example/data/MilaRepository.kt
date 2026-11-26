package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.tasks.await

class MilaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google to access your MILA client & talent dossier.")

    fun observeConsultationInquiries(userId: String): Flow<List<ConsultationInquiry>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/consultationInquiries"
        emitAll(
            db.collection("users").document(userId).collection("consultationInquiries")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ConsultationInquiry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeJobApplications(userId: String): Flow<List<JobApplication>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/jobApplications"
        emitAll(
            db.collection("users").document(userId).collection("jobApplications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        JobApplication::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSavedRoles(userId: String): Flow<List<SavedRole>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/savedRoles"
        emitAll(
            db.collection("users").document(userId).collection("savedRoles")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavedRole::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeClientProfile(userId: String): Flow<ClientProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/clientProfiles/primary"
        emitAll(
            db.collection("users").document(userId)
                .collection("clientProfiles").document("primary")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ClientProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveClientProfile(profile: ClientProfile): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/clientProfiles/primary"
        return try {
            val uid = requireUserId()
            require(profile.fullName.trim().length in 2..100) {
                "Full name must be 2 to 100 characters."
            }
            require(profile.email.trim().length in 5..120 && profile.email.contains("@")) {
                "Please enter a valid email address."
            }
            require(profile.mobileNumber.trim().length in 7..25) {
                "Mobile number must be 7 to 25 characters."
            }
            require(profile.companyName.trim().length in 2..120) {
                "Company name must be 2 to 120 characters."
            }
            require(profile.designation.trim().length in 2..80) {
                "Designation must be 2 to 80 characters."
            }
            require(profile.city.trim().length in 2..80) {
                "City must be 2 to 80 characters."
            }

            val docRef = db.collection("users").document(uid)
                .collection("clientProfiles").document("primary")
            docPath = docRef.path
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(profile.toUpdateMap()).await()
            } else {
                docRef.set(profile.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitConsultationInquiry(inquiry: ConsultationInquiry): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/consultationInquiries"
        return try {
            val uid = requireUserId()
            require(inquiry.propertyName.trim().length in 2..120) {
                "Property or venture name must be 2 to 120 characters."
            }
            require(inquiry.city.trim().length in 2..80) {
                "City must be 2 to 80 characters."
            }
            require(inquiry.scaleSummary.trim().length in 1..80) {
                "Please specify project scale (e.g., 80 Keys or 60 Covers)."
            }
            require(inquiry.contactName.trim().length in 2..100) {
                "Contact name must be 2 to 100 characters."
            }
            require(inquiry.contactEmail.trim().length in 5..120 && inquiry.contactEmail.contains("@")) {
                "Please enter a valid business email address."
            }
            require(inquiry.contactPhone.trim().length in 7..25) {
                "Please enter a valid phone number."
            }
            require((inquiry.briefNotes?.trim()?.length ?: 0) <= 1000) {
                "Project brief cannot exceed 1000 characters."
            }

            val docRef = db.collection("users").document(uid)
                .collection("consultationInquiries").document()
            docPath = docRef.path
            docRef.set(inquiry.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateConsultationNotes(inquiryId: String, briefNotes: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/consultationInquiries/$inquiryId"
        return try {
            val uid = requireUserId()
            require(inquiryId.isNotBlank()) { "Inquiry ID must not be blank" }
            val trimmed = briefNotes.trim()
            require(trimmed.length <= 1000) { "Project brief cannot exceed 1000 characters." }

            val docRef = db.collection("users").document(uid)
                .collection("consultationInquiries").document(inquiryId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "briefNotes" to trimmed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun withdrawConsultationInquiry(inquiryId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/consultationInquiries/$inquiryId"
        return try {
            val uid = requireUserId()
            require(inquiryId.isNotBlank()) { "Inquiry ID must not be blank" }
            val docRef = db.collection("users").document(uid)
                .collection("consultationInquiries").document(inquiryId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to InquiryStatus.WITHDRAWN.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteConsultationInquiry(inquiryId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/consultationInquiries/$inquiryId"
        return try {
            val uid = requireUserId()
            require(inquiryId.isNotBlank()) { "Inquiry ID must not be blank" }
            val docRef = db.collection("users").document(uid)
                .collection("consultationInquiries").document(inquiryId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitJobApplication(application: JobApplication): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/jobApplications"
        return try {
            val uid = requireUserId()
            require(application.roleId.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
                "Invalid role identifier."
            }
            require(application.candidateName.trim().length in 2..100) {
                "Full name must be 2 to 100 characters."
            }
            require(application.candidateEmail.trim().length in 5..120 && application.candidateEmail.contains("@")) {
                "Please enter a valid email address."
            }
            require(application.candidatePhone.trim().length in 7..25) {
                "Please enter a valid phone number."
            }
            require(application.yearsExperience in 0L..60L) {
                "Years of experience must be between 0 and 60."
            }
            require(application.currentProperty.trim().length in 2..120) {
                "Current property or group must be 2 to 120 characters."
            }
            require((application.coverNote?.trim()?.length ?: 0) <= 1000) {
                "Executive note cannot exceed 1000 characters."
            }

            val docRef = db.collection("users").document(uid)
                .collection("jobApplications").document()
            docPath = docRef.path
            docRef.set(application.toCreateMap(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun withdrawJobApplication(applicationId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/jobApplications/$applicationId"
        return try {
            val uid = requireUserId()
            require(applicationId.isNotBlank()) { "Application ID must not be blank" }
            val docRef = db.collection("users").document(uid)
                .collection("jobApplications").document(applicationId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to ApplicationStatus.WITHDRAWN.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteJobApplication(applicationId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/jobApplications/$applicationId"
        return try {
            val uid = requireUserId()
            require(applicationId.isNotBlank()) { "Application ID must not be blank" }
            val docRef = db.collection("users").document(uid)
                .collection("jobApplications").document(applicationId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleSavedRole(role: HospitalityRole, currentlySaved: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/savedRoles/${role.id}"
        return try {
            val uid = requireUserId()
            require(role.id.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) { "Invalid role identifier." }
            val docRef = db.collection("users").document(uid)
                .collection("savedRoles").document(role.id)
            docPath = docRef.path
            if (currentlySaved) {
                docRef.delete().await()
            } else {
                val saved = SavedRole(
                    userId = uid,
                    roleId = role.id,
                    roleTitle = role.title,
                    location = role.location,
                    compensation = role.compensation,
                    category = role.category.displayLabel
                )
                docRef.set(saved.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (currentlySaved) OperationType.DELETE else OperationType.CREATE,
                docPath
            )
            Result.failure(e)
        }
    }

    fun observeCandidateProfile(userId: String): Flow<CandidateProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/candidateProfiles/primary"
        emitAll(
            db.collection("users").document(userId)
                .collection("candidateProfiles").document("primary")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            CandidateProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveCandidateProfile(profile: CandidateProfile): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/candidateProfiles/primary"
        return try {
            val uid = requireUserId()
            require(profile.fullName.trim().length in 2..100) { "Full name must be 2 to 100 characters." }
            require(profile.email.trim().length in 5..120 && profile.email.contains("@")) {
                "Please enter a valid email address."
            }
            require(profile.mobileNumber.trim().length in 7..25) { "Mobile number must be 7 to 25 characters." }
            require(profile.location.trim().length in 2..80) { "Location must be 2 to 80 characters." }
            require(profile.currentDesignation.trim().length in 2..100) { "Current designation is required." }
            require(profile.totalExperienceYears in 0L..60L) { "Total experience must be 0 to 60 years." }
            require(profile.currentEmployer.trim().isNotEmpty()) { "Current employer is required." }
            require(profile.education.trim().length in 2..200) { "Education is required." }
            require(profile.skills.trim().length in 2..400) { "Skills are required." }
            require(profile.preferredJobLocations.trim().length in 2..200) { "Preferred locations are required." }
            require(profile.expectedSalary.trim().length in 2..80) { "Expected salary is required." }
            require(profile.noticePeriod.trim().length in 2..80) { "Notice period is required." }
            require(profile.cvStoragePath?.startsWith("http") != true) {
                "CV storage path must never be a public HTTP download URL."
            }

            val docRef = db.collection("users").document(uid)
                .collection("candidateProfiles").document("primary")
            docPath = docRef.path
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(profile.toUpdateMap()).await()
            } else {
                docRef.set(profile.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeJobNotifications(userId: String): Flow<List<JobNotification>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/jobNotifications"
        emitAll(
            db.collection("users").document(userId).collection("jobNotifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        JobNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createJobNotification(notificationId: String, notification: JobNotification): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/jobNotifications/$notificationId"
        return try {
            val uid = requireUserId()
            require(notificationId.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) { "Invalid notification ID." }
            val docRef = db.collection("users").document(uid)
                .collection("jobNotifications").document(notificationId)
            docPath = docRef.path
            docRef.set(notification.toCreateMap(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markJobNotificationRead(notificationId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/jobNotifications/$notificationId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("jobNotifications").document(notificationId)
            docPath = docRef.path
            docRef.update("isRead", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteJobNotification(notificationId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/jobNotifications/$notificationId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("jobNotifications").document(notificationId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
