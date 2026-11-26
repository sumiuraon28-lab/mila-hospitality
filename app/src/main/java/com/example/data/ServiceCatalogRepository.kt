package com.example.data

import com.example.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ServiceCatalogRepository(
    private val db: FirebaseFirestore
) {
    fun observePublishedServices(): Flow<List<ConsultancyService>> = flow {
        emit(MilaCatalog.services)
        val path = "services"
        db.collection("services")
            .whereEqualTo("published", true)
            .snapshots()
            .map { snapshot ->
                val remote = snapshot.toObjects(
                    ServiceOffering::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (remote.isEmpty()) {
                    MilaCatalog.services
                } else {
                    mergeWithDefaults(remote)
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                emit(MilaCatalog.services)
            }
            .collect { services ->
                emit(services)
            }
    }

    private fun mergeWithDefaults(remoteItems: List<ServiceOffering>): List<ConsultancyService> {
        val defaultsByType = MilaCatalog.services.associateBy { it.type }
        val mapped = remoteItems.map { remote ->
            val resolvedType = remote.resolvedServiceType
            val fallback = defaultsByType[resolvedType] ?: MilaCatalog.services.first()
            ConsultancyService(
                id = remote.id.ifBlank { fallback.id },
                sortOrder = remote.sortOrder.toInt().coerceIn(1, 100),
                type = resolvedType,
                slug = remote.slug.ifBlank { fallback.slug },
                divisionName = remote.divisionName.ifBlank { fallback.divisionName },
                title = remote.title.ifBlank { fallback.title },
                subtitle = remote.subtitle.ifBlank { fallback.subtitle },
                summary = remote.summary.ifBlank { fallback.summary },
                detailedDescription = remote.detailedDescription.ifBlank { fallback.detailedDescription },
                servicesIncluded = remote.servicesIncluded.ifEmpty { fallback.servicesIncluded },
                ownerBenefits = remote.ownerBenefits.ifEmpty { fallback.ownerBenefits },
                deliverables = remote.deliverables.ifEmpty { fallback.deliverables },
                typicalTimeline = remote.typicalTimeline.ifBlank { fallback.typicalTimeline },
                imageKey = remote.imageKey.ifBlank { fallback.imageKey },
                imageRes = resolveDrawableForKey(remote.imageKey.ifBlank { fallback.imageKey }),
                imageUrl = remote.imageUrl
            )
        }
        return mapped.sortedBy { it.sortOrder }
    }

    companion object {
        fun resolveDrawableForKey(imageKey: String): Int = when (imageKey) {
            "palace_resort" -> R.drawable.img_palace_resort_1795637233184
            "fine_dining" -> R.drawable.img_fine_dining_1795637233362
            "mixology_bar" -> R.drawable.img_mixology_bar_1795637233060
            "hospitality_training" -> R.drawable.img_hospitality_training_1795638556708
            "hero_hotel" -> R.drawable.img_hero_hotel_1795637232836
            "executive_recruitment" -> R.drawable.img_executive_recruitment_1795638556883
            "concept_design" -> R.drawable.img_concept_design_1795638557169
            "management_solutions" -> R.drawable.img_management_solutions_1795638555350
            else -> R.drawable.img_hero_hotel_1795637232836
        }
    }
}
