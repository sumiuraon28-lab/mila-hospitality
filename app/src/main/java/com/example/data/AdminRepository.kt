package com.example.data

import com.google.firebase.auth.FirebaseUser
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
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminRepository(
    private val db: FirebaseFirestore
) {
    suspend fun verifyAndResolveAdmin(user: FirebaseUser): Result<AdminUser?> {
        val adminDocRef = db.collection("adminUsers").document(user.uid)
        return try {
            val snap = adminDocRef.get().await()
            if (snap.exists()) {
                val admin = snap.toObject(
                    AdminUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                return Result.success(admin?.takeIf { it.active })
            }

            val emailLower = user.email?.trim()?.lowercase().orEmpty()
            val canBootstrapOwner = emailLower == OWNER_BOOTSTRAP_EMAIL && user.isEmailVerified
            if (canBootstrapOwner) {
                val ownerAdmin = AdminUser(
                    uid = user.uid,
                    email = emailLower,
                    displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "MILA Principal Admin",
                    role = AdminRole.SUPER_ADMIN.name,
                    active = true
                )
                adminDocRef.set(ownerAdmin.toCreateMap()).await()
                val created = adminDocRef.get().await().toObject(
                    AdminUser::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                return Result.success(created)
            }

            if (emailLower.isNotBlank() && user.isEmailVerified) {
                val inviteSnap = db.collection("adminInvites").document(emailLower).get().await()
                if (inviteSnap.exists()) {
                    val invite = inviteSnap.toObject(
                        AdminInvite::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (invite != null && invite.active) {
                        val newAdmin = AdminUser(
                            uid = user.uid,
                            email = emailLower,
                            displayName = invite.displayName.ifBlank { user.displayName ?: "MILA Admin" },
                            role = invite.resolvedRole.name,
                            active = true
                        )
                        adminDocRef.set(newAdmin.toCreateMap()).await()
                        val created = adminDocRef.get().await().toObject(
                            AdminUser::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        return Result.success(created)
                    }
                }
            }

            Result.success(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Result.success(null)
            } else {
                handleFirestoreError(e, OperationType.GET, adminDocRef.path)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logActivity(
        actor: AdminUser,
        module: AdminModule,
        action: String,
        targetId: String,
        summary: String
    ) {
        if (actor.resolvedRole.isReadOnly) return
        val logId = "log_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        runCatching {
            val log = ActivityLog(
                actorUid = actor.uid,
                actorEmail = actor.email,
                actorRole = actor.resolvedRole.name,
                module = module.name,
                action = action,
                targetId = targetId.ifBlank { "record" },
                summary = summary
            )
            db.collection("activityLogs").document(logId).set(log.toCreateMap()).await()
        }
    }

    fun observeClients(): Flow<List<ClientProfile>> = observeCollectionGroup(
        group = "clientProfiles",
        clazz = ClientProfile::class.java,
        sortSelector = { it.updatedAt?.seconds ?: 0L }
    )

    fun observeInquiries(actor: AdminUser): Flow<List<ConsultationInquiry>> = flow {
        val role = actor.resolvedRole
        val baseQuery = if (role == AdminRole.CONSULTANT) {
            db.collectionGroup("consultationInquiries")
                .whereEqualTo("assignedConsultantUid", actor.uid)
        } else {
            db.collectionGroup("consultationInquiries")
        }
        baseQuery.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ConsultationInquiry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    fun observeCandidates(): Flow<List<CandidateProfile>> = observeCollectionGroup(
        group = "candidateProfiles",
        clazz = CandidateProfile::class.java,
        sortSelector = { it.updatedAt?.seconds ?: 0L }
    )

    fun observeApplications(): Flow<List<JobApplication>> = observeCollectionGroup(
        group = "jobApplications",
        clazz = JobApplication::class.java,
        sortSelector = { it.createdAt?.seconds ?: 0L }
    )

    fun observeNotifications(): Flow<List<JobNotification>> = observeCollectionGroup(
        group = "jobNotifications",
        clazz = JobNotification::class.java,
        sortSelector = { it.createdAt?.seconds ?: 0L }
    )

    fun observeAdminUsers(): Flow<List<AdminUser>> = observeRootCollection(
        collection = "adminUsers",
        clazz = AdminUser::class.java,
        sortSelector = { it.updatedAt?.seconds ?: 0L }
    )

    fun observeAdminInvites(): Flow<List<AdminInvite>> = observeRootCollection(
        collection = "adminInvites",
        clazz = AdminInvite::class.java,
        sortSelector = { it.updatedAt?.seconds ?: 0L }
    )

    fun observeActivityLogs(): Flow<List<ActivityLog>> = observeRootCollection(
        collection = "activityLogs",
        clazz = ActivityLog::class.java,
        sortSelector = { it.createdAt?.seconds ?: 0L }
    )

    fun observeSettings(): Flow<AdminSettings> = flow {
        db.collection("adminSettings").document("platform")
            .snapshots()
            .map { snap ->
                snap.toObject(
                    AdminSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: AdminSettings()
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(AdminSettings())
            }
            .collect { emit(it) }
    }

    suspend fun updateInquiryAdmin(
        actor: AdminUser,
        inquiry: ConsultationInquiry,
        newStatus: InquiryStatus,
        consultantNotes: String,
        assignedConsultant: AdminUser?
    ): Result<Unit> = runAdminWrite("users/${inquiry.userId}/consultationInquiries/${inquiry.id}") {
        require(actor.resolvedRole.canMutate(AdminModule.CLIENT_ENQUIRIES)) { "Not authorized." }
        val ref = db.collection("users").document(inquiry.userId)
            .collection("consultationInquiries").document(inquiry.id)
        val payload = if (actor.resolvedRole == AdminRole.CONSULTANT) {
            mapOf(
                "status" to newStatus.name,
                "consultantNotes" to consultantNotes.trim().take(2000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            val map = mutableMapOf<String, Any?>(
                "status" to newStatus.name,
                "consultantNotes" to consultantNotes.trim().take(2000),
                "assignedConsultantUid" to assignedConsultant?.uid,
                "assignedConsultantEmail" to assignedConsultant?.email,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            map.filterValues { it != null } as Map<String, Any>
        }
        ref.update(payload).await()
        logActivity(
            actor = actor,
            module = AdminModule.CLIENT_ENQUIRIES,
            action = "UPDATE_ENQUIRY",
            targetId = inquiry.id,
            summary = "${inquiry.propertyName}: ${newStatus.label}" +
                (assignedConsultant?.let { " • Assigned ${it.displayName}" } ?: "")
        )
    }

    suspend fun deleteInquiryAdmin(actor: AdminUser, inquiry: ConsultationInquiry): Result<Unit> =
        runAdminWrite("users/${inquiry.userId}/consultationInquiries/${inquiry.id}") {
            require(actor.resolvedRole == AdminRole.SUPER_ADMIN) { "Super Admin required." }
            db.collection("users").document(inquiry.userId)
                .collection("consultationInquiries").document(inquiry.id)
                .delete().await()
            logActivity(actor, AdminModule.CLIENT_ENQUIRIES, "DELETE", inquiry.id, "Removed enquiry ${inquiry.propertyName}")
        }

    suspend fun updateClientProfileAdmin(actor: AdminUser, client: ClientProfile): Result<Unit> =
        runAdminWrite("users/${client.userId}/clientProfiles/${client.id}") {
            require(actor.resolvedRole == AdminRole.SUPER_ADMIN) { "Super Admin required." }
            db.collection("users").document(client.userId)
                .collection("clientProfiles").document(client.id.ifBlank { "primary" })
                .update(client.toUpdateMap()).await()
            logActivity(actor, AdminModule.CLIENT_MANAGEMENT, "UPDATE", client.userId, "Updated client ${client.companyName}")
        }

    suspend fun deleteClientProfileAdmin(actor: AdminUser, client: ClientProfile): Result<Unit> =
        runAdminWrite("users/${client.userId}/clientProfiles/${client.id}") {
            require(actor.resolvedRole == AdminRole.SUPER_ADMIN) { "Super Admin required." }
            db.collection("users").document(client.userId)
                .collection("clientProfiles").document(client.id.ifBlank { "primary" })
                .delete().await()
            logActivity(actor, AdminModule.CLIENT_MANAGEMENT, "DELETE", client.userId, "Deleted client ${client.companyName}")
        }

    suspend fun updateApplicationStatusAdmin(
        actor: AdminUser,
        application: JobApplication,
        newStatus: ApplicationStatus,
        notifyCandidate: Boolean
    ): Result<Unit> = runAdminWrite("users/${application.userId}/jobApplications/${application.id}") {
        require(actor.resolvedRole.canMutate(AdminModule.RECRUITMENT_STATUS_MANAGEMENT)) { "Not authorized." }
        db.collection("users").document(application.userId)
            .collection("jobApplications").document(application.id)
            .update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        if (notifyCandidate) {
            val notifId = "status_${application.roleId}_${System.currentTimeMillis()}"
            val notif = JobNotification(
                userId = application.userId,
                roleId = application.roleId,
                title = "Application Update: ${application.roleTitle}",
                message = "Your candidacy status is now: ${newStatus.label}.",
                department = "MILA Talent",
                location = application.location,
                isRead = false
            )
            db.collection("users").document(application.userId)
                .collection("jobNotifications").document(notifId)
                .set(notif.toCreateMap(application.userId)).await()
        }
        logActivity(
            actor,
            AdminModule.RECRUITMENT_STATUS_MANAGEMENT,
            "STATUS_CHANGE",
            application.id,
            "${application.candidateName} (${application.roleTitle}) -> ${newStatus.label}"
        )
    }

    suspend fun deleteApplicationAdmin(actor: AdminUser, application: JobApplication): Result<Unit> =
        runAdminWrite("users/${application.userId}/jobApplications/${application.id}") {
            require(actor.resolvedRole.canMutate(AdminModule.JOB_APPLICATIONS)) { "Not authorized." }
            db.collection("users").document(application.userId)
                .collection("jobApplications").document(application.id).delete().await()
            logActivity(actor, AdminModule.JOB_APPLICATIONS, "DELETE", application.id, "Removed application for ${application.candidateName}")
        }

    suspend fun toggleCandidateAlertsAdmin(actor: AdminUser, candidate: CandidateProfile): Result<Unit> =
        runAdminWrite("users/${candidate.userId}/candidateProfiles/${candidate.id}") {
            require(actor.resolvedRole.canMutate(AdminModule.CANDIDATE_MANAGEMENT)) { "Not authorized." }
            val updated = candidate.copy(jobAlertsEnabled = !candidate.jobAlertsEnabled)
            db.collection("users").document(candidate.userId)
                .collection("candidateProfiles").document(candidate.id.ifBlank { "primary" })
                .update(updated.toUpdateMap()).await()
            logActivity(
                actor,
                AdminModule.CANDIDATE_MANAGEMENT,
                "TOGGLE_ALERTS",
                candidate.userId,
                "${candidate.fullName}: alerts=${updated.jobAlertsEnabled}"
            )
        }

    suspend fun removeCandidateCvAdmin(actor: AdminUser, candidate: CandidateProfile): Result<Unit> =
        runAdminWrite("users/${candidate.userId}/candidateProfiles/${candidate.id}") {
            require(actor.resolvedRole.canMutate(AdminModule.CV_DATABASE)) { "Not authorized." }
            val updated = candidate.copy(
                cvFileName = null,
                cvMimeType = null,
                cvFileSizeBytes = null,
                cvStoragePath = null,
                cvUploadedAtMillis = null
            )
            db.collection("users").document(candidate.userId)
                .collection("candidateProfiles").document(candidate.id.ifBlank { "primary" })
                .update(updated.toUpdateMap()).await()
            logActivity(actor, AdminModule.CV_DATABASE, "PURGE_CV", candidate.userId, "Cleared private CV metadata for ${candidate.fullName}")
        }

    suspend fun sendCandidateNotificationAdmin(
        actor: AdminUser,
        targetUserId: String,
        roleId: String,
        title: String,
        message: String,
        department: String,
        location: String
    ): Result<Unit> = runAdminWrite("users/$targetUserId/jobNotifications") {
        require(actor.resolvedRole.canMutate(AdminModule.NOTIFICATION_MANAGEMENT)) { "Not authorized." }
        val notifId = "adm_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val notif = JobNotification(
            userId = targetUserId,
            roleId = roleId.ifBlank { "general_talent_alert" },
            title = title,
            message = message,
            department = department.ifBlank { "MILA Talent" },
            location = location.ifBlank { "Pan-India" },
            isRead = false
        )
        db.collection("users").document(targetUserId)
            .collection("jobNotifications").document(notifId)
            .set(notif.toCreateMap(targetUserId)).await()
        logActivity(actor, AdminModule.NOTIFICATION_MANAGEMENT, "DISPATCH", notifId, "Notified $targetUserId: $title")
    }

    suspend fun deleteNotificationAdmin(actor: AdminUser, notification: JobNotification): Result<Unit> =
        runAdminWrite("users/${notification.userId}/jobNotifications/${notification.id}") {
            require(actor.resolvedRole.canMutate(AdminModule.NOTIFICATION_MANAGEMENT)) { "Not authorized." }
            db.collection("users").document(notification.userId)
                .collection("jobNotifications").document(notification.id).delete().await()
            logActivity(actor, AdminModule.NOTIFICATION_MANAGEMENT, "DELETE", notification.id, "Deleted alert ${notification.title}")
        }

    suspend fun saveAdminInvite(actor: AdminUser, invite: AdminInvite): Result<Unit> =
        runAdminWrite("adminInvites/${AdminInvite.normalizeEmailKey(invite.email)}") {
            require(actor.resolvedRole == AdminRole.SUPER_ADMIN) { "Super Admin required." }
            val key = AdminInvite.normalizeEmailKey(invite.email)
            require(key.length in 5..120 && key.contains("@")) { "Enter a valid staff email address." }
            db.collection("adminInvites").document(key)
                .set(invite.copy(email = key).toCreateMap(actor.uid)).await()
            logActivity(actor, AdminModule.ADMIN_USER_MANAGEMENT, "INVITE", key, "Invited $key as ${invite.resolvedRole.displayLabel}")
        }

    suspend fun deleteAdminInvite(actor: AdminUser, invite: AdminInvite): Result<Unit> =
        runAdminWrite("adminInvites/${invite.id}") {
            require(actor.resolvedRole == AdminRole.SUPER_ADMIN) { "Super Admin required." }
            val docId = invite.id.ifBlank { AdminInvite.normalizeEmailKey(invite.email) }
            db.collection("adminInvites").document(docId).delete().await()
            logActivity(actor, AdminModule.ADMIN_USER_MANAGEMENT, "REVOKE_INVITE", docId, "Revoked invite for ${invite.email}")
        }

    suspend fun updateAdminUser(actor: AdminUser, target: AdminUser): Result<Unit> =
        runAdminWrite("adminUsers/${target.uid}") {
            require(actor.resolvedRole == AdminRole.SUPER_ADMIN) { "Super Admin required." }
            db.collection("adminUsers").document(target.uid)
                .update(target.toUpdateMap(target.createdAt)).await()
            logActivity(
                actor,
                AdminModule.ADMIN_USER_MANAGEMENT,
                "UPDATE_ROLE",
                target.uid,
                "${target.email} -> ${target.resolvedRole.displayLabel} (active=${target.active})"
            )
        }

    suspend fun saveSettings(actor: AdminUser, settings: AdminSettings): Result<Unit> =
        runAdminWrite("adminSettings/platform") {
            require(actor.resolvedRole == AdminRole.SUPER_ADMIN) { "Super Admin required." }
            db.collection("adminSettings").document("platform").set(settings.toAdminMap()).await()
            logActivity(actor, AdminModule.SETTINGS, "UPDATE_SETTINGS", "platform", "Updated platform configuration")
        }

    private fun <T : Any> observeCollectionGroup(
        group: String,
        clazz: Class<T>,
        sortSelector: (T) -> Long
    ): Flow<List<T>> = flow {
        db.collectionGroup(group)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending(sortSelector)
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    private fun <T : Any> observeRootCollection(
        collection: String,
        clazz: Class<T>,
        sortSelector: (T) -> Long
    ): Flow<List<T>> = flow {
        db.collection(collection)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending(sortSelector)
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    private suspend inline fun runAdminWrite(path: String, crossinline block: suspend () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
