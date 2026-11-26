package com.example.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class PublicCmsRepository(
    private val db: FirebaseFirestore
) {
    fun observePublishedVacancies(): Flow<List<HospitalityRole>> = flow {
        emit(MilaCatalog.openRoles)
        val path = "jobVacancies"
        db.collection(path)
            .whereEqualTo("published", true)
            .snapshots()
            .map { snapshot ->
                val remote = snapshot.toObjects(
                    JobVacancyRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (remote.isEmpty()) {
                    MilaCatalog.openRoles
                } else {
                    remote.map { it.toHospitalityRole() }
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null) {
                    handleFirestoreError(firestoreError, OperationType.LIST, path)
                }
                emit(MilaCatalog.openRoles)
            }
            .collect { emit(it) }
    }

    fun observePublishedPortfolio(): Flow<List<CaseStudy>> = flow {
        emit(MilaCatalog.caseStudies)
        val path = "portfolioItems"
        db.collection(path)
            .whereEqualTo("published", true)
            .snapshots()
            .map { snapshot ->
                val remote = snapshot.toObjects(
                    PortfolioItemRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (remote.isEmpty()) {
                    MilaCatalog.caseStudies
                } else {
                    remote.sortedBy { it.sortOrder }.map { it.toCaseStudy() }
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null) {
                    handleFirestoreError(firestoreError, OperationType.LIST, path)
                }
                emit(MilaCatalog.caseStudies)
            }
            .collect { emit(it) }
    }

    fun observePublishedTestimonials(): Flow<List<Testimonial>> = flow {
        emit(MilaCatalog.testimonials)
        val path = "testimonials"
        db.collection(path)
            .whereEqualTo("published", true)
            .snapshots()
            .map { snapshot ->
                val remote = snapshot.toObjects(
                    TestimonialRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (remote.isEmpty()) {
                    MilaCatalog.testimonials
                } else {
                    remote.sortedBy { it.sortOrder }.map { it.toTestimonial() }
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null) {
                    handleFirestoreError(firestoreError, OperationType.LIST, path)
                }
                emit(MilaCatalog.testimonials)
            }
            .collect { emit(it) }
    }
}
