package com.example.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminCmsRepository(
    private val db: FirebaseFirestore,
    private val adminRepository: AdminRepository
) {
    fun observeAllServices(): Flow<List<ServiceOffering>> = observeSorted(
        "services", ServiceOffering::class.java
    ) { it.sortOrder }

    fun observeAllCategories(): Flow<List<ServiceCategoryRecord>> = observeSorted(
        "serviceCategories", ServiceCategoryRecord::class.java
    ) { it.sortOrder }

    fun observeAllVacancies(): Flow<List<JobVacancyRecord>> = observeSorted(
        "jobVacancies", JobVacancyRecord::class.java
    ) { if (it.urgent) 0L else 1L }

    fun observeSitePages(): Flow<List<SitePageContent>> = observeSorted(
        "siteContent", SitePageContent::class.java
    ) { it.pageKey }

    fun observePortfolioItems(): Flow<List<PortfolioItemRecord>> = observeSorted(
        "portfolioItems", PortfolioItemRecord::class.java
    ) { it.sortOrder }

    fun observeTestimonials(): Flow<List<TestimonialRecord>> = observeSorted(
        "testimonials", TestimonialRecord::class.java
    ) { it.sortOrder }

    fun observeTeamMembers(): Flow<List<TeamMemberRecord>> = observeSorted(
        "teamMembers", TeamMemberRecord::class.java
    ) { it.sortOrder }

    suspend fun seedCmsDefaultsIfEmpty(actor: AdminUser) {
        if (actor.resolvedRole.isReadOnly) return
        val role = actor.resolvedRole
        if (role == AdminRole.SUPER_ADMIN || role == AdminRole.CONTENT_ADMIN) {
            seedIfEmpty("services") {
                ServiceCatalogDefaults.services.forEach { svc ->
                    val offering = ServiceOffering(
                        id = svc.slug,
                        serviceType = svc.type.name,
                        sortOrder = svc.sortOrder.toLong(),
                        slug = svc.slug,
                        divisionName = svc.divisionName,
                        title = svc.title,
                        subtitle = svc.subtitle,
                        summary = svc.summary,
                        detailedDescription = svc.detailedDescription,
                        servicesIncluded = svc.servicesIncluded,
                        ownerBenefits = svc.ownerBenefits,
                        deliverables = svc.deliverables,
                        typicalTimeline = svc.typicalTimeline,
                        imageKey = svc.imageKey,
                        imageUrl = svc.imageUrl,
                        published = true
                    )
                    db.collection("services").document(svc.slug).set(offering.toAdminMap()).await()
                }
            }
            seedIfEmpty("serviceCategories") {
                AdminDefaults.defaultServiceCategories.forEach { cat ->
                    db.collection("serviceCategories").document(cat.id).set(cat.toAdminMap()).await()
                }
            }
            seedIfEmpty("siteContent") {
                AdminDefaults.defaultSitePages.forEach { page ->
                    db.collection("siteContent").document(page.id).set(page.toAdminMap()).await()
                }
            }
            seedIfEmpty("portfolioItems") {
                AdminDefaults.defaultPortfolioItems.forEach { item ->
                    db.collection("portfolioItems").document(item.id).set(item.toAdminMap()).await()
                }
            }
            seedIfEmpty("testimonials") {
                AdminDefaults.defaultTestimonials.forEach { item ->
                    db.collection("testimonials").document(item.id).set(item.toAdminMap()).await()
                }
            }
            seedIfEmpty("teamMembers") {
                AdminDefaults.defaultTeamMembers.forEach { member ->
                    db.collection("teamMembers").document(member.id).set(member.toAdminMap()).await()
                }
            }
        }
        if (role == AdminRole.SUPER_ADMIN || role == AdminRole.RECRUITMENT_ADMIN) {
            seedIfEmpty("jobVacancies") {
                AdminDefaults.defaultVacancies.forEach { job ->
                    db.collection("jobVacancies").document(job.id).set(job.toAdminMap()).await()
                }
            }
        }
        if (role == AdminRole.SUPER_ADMIN) {
            runCatching {
                val settingsRef = db.collection("adminSettings").document("platform")
                if (!settingsRef.get().await().exists()) {
                    settingsRef.set(AdminSettings().toAdminMap()).await()
                }
            }
        }
    }

    suspend fun saveService(actor: AdminUser, item: ServiceOffering): Result<Unit> = saveDoc(
        actor, AdminModule.SERVICE_MANAGEMENT, "services",
        item.id.ifBlank { item.slug.ifBlank { genId("svc") } }, item.title, item.toAdminMap()
    )

    suspend fun deleteService(actor: AdminUser, item: ServiceOffering): Result<Unit> =
        deleteDoc(actor, AdminModule.SERVICE_MANAGEMENT, "services", item.id, item.title)

    suspend fun saveCategory(actor: AdminUser, item: ServiceCategoryRecord): Result<Unit> = saveDoc(
        actor, AdminModule.SERVICE_CATEGORIES, "serviceCategories",
        item.id.ifBlank { genId("cat") }, item.title, item.toAdminMap()
    )

    suspend fun deleteCategory(actor: AdminUser, item: ServiceCategoryRecord): Result<Unit> =
        deleteDoc(actor, AdminModule.SERVICE_CATEGORIES, "serviceCategories", item.id, item.title)

    suspend fun saveVacancy(actor: AdminUser, item: JobVacancyRecord): Result<Unit> = saveDoc(
        actor, AdminModule.JOB_VACANCY_MANAGEMENT, "jobVacancies",
        item.id.ifBlank { genId("role") }, item.title, item.toAdminMap()
    )

    suspend fun deleteVacancy(actor: AdminUser, item: JobVacancyRecord): Result<Unit> =
        deleteDoc(actor, AdminModule.JOB_VACANCY_MANAGEMENT, "jobVacancies", item.id, item.title)

    suspend fun saveSitePage(actor: AdminUser, item: SitePageContent): Result<Unit> = saveDoc(
        actor, AdminModule.WEBSITE_CONTENT_MANAGEMENT, "siteContent",
        item.id.ifBlank { "page_${item.pageKey.lowercase().replace(Regex("[^a-z0-9_]"), "_")}" },
        item.pageTitle, item.toAdminMap()
    )

    suspend fun deleteSitePage(actor: AdminUser, item: SitePageContent): Result<Unit> =
        deleteDoc(actor, AdminModule.WEBSITE_CONTENT_MANAGEMENT, "siteContent", item.id, item.pageTitle)

    suspend fun savePortfolioItem(actor: AdminUser, item: PortfolioItemRecord): Result<Unit> = saveDoc(
        actor, AdminModule.PORTFOLIO_MANAGEMENT, "portfolioItems",
        item.id.ifBlank { genId("case") }, item.propertyName, item.toAdminMap()
    )

    suspend fun deletePortfolioItem(actor: AdminUser, item: PortfolioItemRecord): Result<Unit> =
        deleteDoc(actor, AdminModule.PORTFOLIO_MANAGEMENT, "portfolioItems", item.id, item.propertyName)

    suspend fun saveTestimonial(actor: AdminUser, item: TestimonialRecord): Result<Unit> = saveDoc(
        actor, AdminModule.TESTIMONIALS, "testimonials",
        item.id.ifBlank { genId("test") }, item.attributionRole, item.toAdminMap()
    )

    suspend fun deleteTestimonial(actor: AdminUser, item: TestimonialRecord): Result<Unit> =
        deleteDoc(actor, AdminModule.TESTIMONIALS, "testimonials", item.id, item.attributionRole)

    suspend fun saveTeamMember(actor: AdminUser, item: TeamMemberRecord): Result<Unit> = saveDoc(
        actor, AdminModule.TEAM_MANAGEMENT, "teamMembers",
        item.id.ifBlank { genId("team") }, item.fullName, item.toAdminMap()
    )

    suspend fun deleteTeamMember(actor: AdminUser, item: TeamMemberRecord): Result<Unit> =
        deleteDoc(actor, AdminModule.TEAM_MANAGEMENT, "teamMembers", item.id, item.fullName)

    private suspend fun saveDoc(
        actor: AdminUser,
        module: AdminModule,
        collection: String,
        docId: String,
        label: String,
        payload: Map<String, Any>
    ): Result<Unit> = try {
        require(actor.resolvedRole.canMutate(module)) { "Not authorized to modify ${module.title}." }
        db.collection(collection).document(docId).set(payload).await()
        adminRepository.logActivity(actor, module, "SAVE", docId, "Saved $label")
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "$collection/$docId")
        Result.failure(e)
    }

    private suspend fun deleteDoc(
        actor: AdminUser,
        module: AdminModule,
        collection: String,
        docId: String,
        label: String
    ): Result<Unit> = try {
        require(actor.resolvedRole.canMutate(module)) { "Not authorized to delete from ${module.title}." }
        db.collection(collection).document(docId).delete().await()
        adminRepository.logActivity(actor, module, "DELETE", docId, "Deleted $label")
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "$collection/$docId")
        Result.failure(e)
    }

    private suspend fun seedIfEmpty(collection: String, seeder: suspend () -> Unit) {
        runCatching {
            val snap = db.collection(collection).limit(1).get().await()
            if (snap.isEmpty) seeder()
        }
    }

    private fun <T : Any, R : Comparable<R>> observeSorted(
        collection: String,
        clazz: Class<T>,
        selector: (T) -> R
    ): Flow<List<T>> = flow {
        db.collection(collection)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy(selector)
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    private fun genId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(10)}"
}
