package com.example.notifications

import com.example.data.CandidateAvailability
import com.example.data.CandidateProfile
import com.example.data.HospitalityDepartment
import com.example.data.JobVacancyRecord
import com.example.data.RoleCategory

/**
 * Deterministic candidate-to-vacancy eligibility engine for MILA Talent.
 *
 * Strictly prevents mass broadcasting of irrelevant vacancies by requiring
 * every candidate to pass five eligibility checks:
 *   1. Active job-seeking status (alerts enabled & actively open to roles).
 *   2. Department alignment (vacancy category mapped to candidate department / designation).
 *   3. Preferred location alignment (preferred job locations, Pan-India, or home region).
 *   4. Experience alignment (meets minimum required years within admin tolerance).
 *   5. Job preferences & skills alignment (skills/preferences overlap scoring).
 */
object JobMatchingEngine {

    private val panIndiaKeywords = listOf(
        "pan-india",
        "pan india",
        "all india",
        "anywhere in india",
        "open to relocate",
        "relocation open",
        "flexible"
    )

    fun evaluateAllCandidates(
        vacancy: JobVacancyRecord,
        candidates: List<CandidateProfile>,
        controls: NotificationControlSettings = NotificationControlSettings()
    ): List<CandidateMatchResult> {
        if (!vacancy.published) {
            return candidates.map { cand ->
                CandidateMatchResult(
                    candidate = cand,
                    eligible = false,
                    matchScore = 0,
                    matchedCriteria = emptyList(),
                    exclusionReasons = listOf("Vacancy is unpublished")
                )
            }
        }
        return candidates.map { candidate ->
            evaluateCandidate(vacancy, candidate, controls)
        }.sortedByDescending { if (it.eligible) 1000 + it.matchScore else it.matchScore }
    }

    fun findEligibleCandidates(
        vacancy: JobVacancyRecord,
        candidates: List<CandidateProfile>,
        controls: NotificationControlSettings = NotificationControlSettings()
    ): List<CandidateMatchResult> =
        evaluateAllCandidates(vacancy, candidates, controls).filter { it.eligible }

    fun evaluateCandidate(
        vacancy: JobVacancyRecord,
        candidate: CandidateProfile,
        controls: NotificationControlSettings = NotificationControlSettings()
    ): CandidateMatchResult {
        val matched = mutableListOf<String>()
        val excluded = mutableListOf<String>()
        var score = 0

        // 1. Active Job-Seeking Status Gate
        val isActivelySeeking = candidate.jobAlertsEnabled &&
            !candidate.availability.equals("NOT_ACTIVELY_LOOKING", ignoreCase = true)
        if (!isActivelySeeking) {
            excluded += if (!candidate.jobAlertsEnabled) {
                "Job alerts paused by candidate"
            } else {
                "Candidate marked not actively looking"
            }
        } else {
            val availabilityNote = when (candidate.resolvedAvailability) {
                CandidateAvailability.IMMEDIATE -> "Immediately available"
                CandidateAvailability.WITHIN_15_DAYS -> "Active seeker (15 days)"
                CandidateAvailability.WITHIN_30_DAYS -> "Active seeker (30 days)"
                CandidateAvailability.WITHIN_60_DAYS -> "Active seeker (60 days)"
                CandidateAvailability.OPEN_TO_OPPORTUNITIES -> "Open to select mandates"
            }
            matched += availabilityNote
            score += if (candidate.resolvedAvailability == CandidateAvailability.IMMEDIATE) 20 else 15
        }

        // 2. Department Match Gate
        val allowedDepartments = compatibleDepartmentsFor(vacancy.resolvedCategory)
        val candDepartment = candidate.resolvedDepartment
        val designationLower = candidate.currentDesignation.lowercase()
        val skillsLower = candidate.skills.lowercase()
        val departmentMatches = candDepartment in allowedDepartments ||
            categoryKeywordsFor(vacancy.resolvedCategory).any { kw ->
                designationLower.contains(kw) || skillsLower.contains(kw)
            }
        if (departmentMatches) {
            matched += "Department: ${candDepartment.label}"
            score += 30
        } else if (controls.requireDepartmentMatch) {
            excluded += "Department mismatch (${candDepartment.label} vs ${vacancy.resolvedCategory.displayLabel})"
        }

        // 3. Preferred Location Match Gate
        val locationMatches = matchesPreferredLocation(vacancy, candidate)
        if (locationMatches) {
            matched += "Location: ${vacancy.cityRegion.ifBlank { vacancy.location }}"
            score += 25
        } else if (controls.requireLocationMatch) {
            excluded += "Preferred location mismatch (${candidate.preferredJobLocations.ifBlank { candidate.location }})"
        }

        // 4. Experience Requirement Gate
        val tolerance = controls.experienceToleranceYears.coerceIn(0L, 5L)
        val effectiveMin = (vacancy.minExperienceYears - tolerance).coerceAtLeast(0L)
        val experienceMatches = candidate.totalExperienceYears >= effectiveMin
        if (experienceMatches) {
            matched += "Experience: ${candidate.totalExperienceYears} yrs (req ${vacancy.minExperienceYears}+)"
            score += if (candidate.totalExperienceYears >= vacancy.minExperienceYears) 20 else 12
        } else {
            excluded += "Experience below threshold (${candidate.totalExperienceYears} yrs < $effectiveMin yrs)"
        }

        // 5. Job Preferences & Skills Overlap Bonus (and soft preference check)
        val skillHits = vacancy.desiredSkills.count { reqSkill ->
            val clean = reqSkill.trim().lowercase()
            clean.length >= 2 && (skillsLower.contains(clean) || designationLower.contains(clean))
        }
        if (skillHits > 0) {
            matched += "Skills & preferences matched ($skillHits/${vacancy.desiredSkills.size})"
            score += (skillHits * 5).coerceAtMost(15)
        } else if (vacancy.urgent && candidate.resolvedAvailability == CandidateAvailability.IMMEDIATE) {
            matched += "Urgent mandate + immediate availability"
            score += 5
        }

        return CandidateMatchResult(
            candidate = candidate,
            eligible = excluded.isEmpty(),
            matchScore = score.coerceIn(0, 100),
            matchedCriteria = matched,
            exclusionReasons = excluded
        )
    }

    private fun compatibleDepartmentsFor(category: RoleCategory): Set<HospitalityDepartment> = when (category) {
        RoleCategory.ALL -> HospitalityDepartment.entries.toSet()
        RoleCategory.HOSTESS -> setOf(HospitalityDepartment.HOSTESS, HospitalityDepartment.GRE, HospitalityDepartment.FNB_SERVICE)
        RoleCategory.GRE -> setOf(HospitalityDepartment.GRE, HospitalityDepartment.HOSTESS, HospitalityDepartment.ROOMS)
        RoleCategory.BARTENDER -> setOf(HospitalityDepartment.BARTENDER, HospitalityDepartment.MIXOLOGY, HospitalityDepartment.FNB_SERVICE)
        RoleCategory.HOUSEKEEPING -> setOf(HospitalityDepartment.HOUSEKEEPING_WELLNESS, HospitalityDepartment.ROOMS)
        RoleCategory.DRIVER -> setOf(HospitalityDepartment.DRIVER)
        RoleCategory.STORE_KEEPER -> setOf(HospitalityDepartment.STORE_KEEPER, HospitalityDepartment.STORE_MANAGER)
        RoleCategory.STORE_MANAGER -> setOf(HospitalityDepartment.STORE_MANAGER, HospitalityDepartment.STORE_KEEPER)
        RoleCategory.MAINTENANCE -> setOf(HospitalityDepartment.MAINTENANCE)
        RoleCategory.EXECUTIVE -> setOf(
            HospitalityDepartment.EXECUTIVE,
            HospitalityDepartment.PRE_OPENING
        )
        RoleCategory.PRE_OPENING -> setOf(
            HospitalityDepartment.PRE_OPENING,
            HospitalityDepartment.EXECUTIVE,
            HospitalityDepartment.FNB_SERVICE,
            HospitalityDepartment.CULINARY,
            HospitalityDepartment.MIXOLOGY,
            HospitalityDepartment.ROOMS
        )
        RoleCategory.CULINARY -> setOf(HospitalityDepartment.CULINARY)
        RoleCategory.MIXOLOGY -> setOf(
            HospitalityDepartment.MIXOLOGY,
            HospitalityDepartment.BARTENDER,
            HospitalityDepartment.FNB_SERVICE
        )
        RoleCategory.FNB_SERVICE -> setOf(
            HospitalityDepartment.FNB_SERVICE,
            HospitalityDepartment.HOSTESS,
            HospitalityDepartment.BARTENDER,
            HospitalityDepartment.MIXOLOGY
        )
        RoleCategory.ROOMS -> setOf(
            HospitalityDepartment.ROOMS,
            HospitalityDepartment.GRE,
            HospitalityDepartment.HOUSEKEEPING_WELLNESS
        )
    }

    private fun categoryKeywordsFor(category: RoleCategory): List<String> = when (category) {
        RoleCategory.ALL -> emptyList()
        RoleCategory.HOSTESS -> listOf("hostess", "host", "reception", "seating", "dining")
        RoleCategory.GRE -> listOf("gre", "guest relations", "guest relation", "front desk", "concierge")
        RoleCategory.BARTENDER -> listOf("bartender", "bar", "mixolog", "cocktail", "beverage")
        RoleCategory.HOUSEKEEPING -> listOf("housekeeping", "house keeping", "room attendant", "laundry", "hk")
        RoleCategory.DRIVER -> listOf("driver", "chauffeur", "valet", "transport", "fleet")
        RoleCategory.STORE_KEEPER -> listOf("store keeper", "storekeeper", "inventory", "receiving", "purchasing")
        RoleCategory.STORE_MANAGER -> listOf("store manager", "stores", "inventory", "procurement", "supply")
        RoleCategory.MAINTENANCE -> listOf("maintenance", "maintainance", "engineering", "technician", "hvac", "electrician")
        RoleCategory.EXECUTIVE -> listOf("general manager", "gm", "hotel manager", "cluster", "vp", "director")
        RoleCategory.PRE_OPENING -> listOf("pre-opening", "pre opening", "opening", "launch")
        RoleCategory.CULINARY -> listOf("chef", "culinary", "kitchen", "pastry", "bakery", "gastronomy")
        RoleCategory.MIXOLOGY -> listOf("mixolog", "bartender", "bar", "beverage", "sommelier", "cocktail")
        RoleCategory.FNB_SERVICE -> listOf("f&b", "food & beverage", "restaurant manager", "banquet", "cafe", "café", "hostess")
        RoleCategory.ROOMS -> listOf("front office", "rooms", "concierge", "guest", "gre", "housekeeping", "butler")
    }

    private fun matchesPreferredLocation(
        vacancy: JobVacancyRecord,
        candidate: CandidateProfile
    ): Boolean {
        val preferredRaw = candidate.preferredJobLocations.trim().lowercase()
        val currentLocRaw = candidate.location.trim().lowercase()
        if (panIndiaKeywords.any { preferredRaw.contains(it) }) return true

        val vacancyTokens = buildSet {
            vacancy.cityRegion.split(",", "/", "-", "•").forEach { token ->
                val clean = token.trim().lowercase()
                if (clean.length >= 3) add(clean)
            }
            vacancy.location.split(",", "/", "-", "•").forEach { token ->
                val clean = token.trim().lowercase()
                if (clean.length >= 3) add(clean)
            }
        }
        if (vacancyTokens.isEmpty()) return true

        val candidateTokens = (preferredRaw.split(",", "/", ";", "|", "•") + currentLocRaw.split(",", "/", ";", "|"))
            .map { it.trim() }
            .filter { it.length >= 3 }

        return candidateTokens.any { candTok ->
            vacancyTokens.any { vacTok ->
                candTok.contains(vacTok) || vacTok.contains(candTok)
            }
        }
    }
}
