package com.example

import com.example.auth.CandidateRegistrationRequest
import com.example.auth.CandidateValidator
import com.example.auth.ClientRegistrationRequest
import com.example.auth.SupabaseAuthClient
import com.example.auth.SupabaseAuthValidator
import com.example.auth.SupabaseClientMetadata
import com.example.auth.SupabaseTalentClient
import com.example.data.AdminInvite
import com.example.data.AdminModule
import com.example.data.AdminRole
import com.example.data.ApplicationStatus
import com.example.data.BusinessCategory
import com.example.data.CandidateAvailability
import com.example.data.CandidateProfile
import com.example.data.ClientProfile
import com.example.data.ConsultationInquiry
import com.example.data.ConsultationServiceType
import com.example.data.EnquiryConfirmation
import com.example.data.ExperienceFilter
import com.example.data.HospitalityDepartment
import com.example.data.InquiryStatus
import com.example.data.JobNotification
import com.example.data.JobVacancyRecord
import com.example.data.MilaCatalog
import com.example.data.RoleCategory
import com.example.data.ServiceOffering
import com.example.data.SupabaseConfig
import com.example.notifications.JobMatchingEngine
import com.example.notifications.NotificationControlSettings
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun milaCatalog_containsExpectedHeritageMetricsAndEightServiceCategories() {
        assertEquals(4, MilaCatalog.impactMetrics.size)
        assertEquals("15+", MilaCatalog.impactMetrics[0].figure)
        assertEquals("30+", MilaCatalog.impactMetrics[1].figure)
        assertEquals("20+", MilaCatalog.impactMetrics[2].figure)
        assertEquals("3×", MilaCatalog.impactMetrics[3].figure)
        assertEquals(3, MilaCatalog.divisions.size)
        assertEquals(3, MilaCatalog.testimonials.size)
        assertTrue(MilaCatalog.openRoles.isNotEmpty())
        assertEquals(8, MilaCatalog.services.size)
        assertEquals(8, ConsultationServiceType.entries.size)
        MilaCatalog.services.forEach { service ->
            assertTrue(service.title.isNotBlank())
            assertTrue(service.detailedDescription.isNotBlank())
            assertTrue(service.servicesIncluded.isNotEmpty())
            assertTrue(service.ownerBenefits.isNotEmpty())
            assertTrue(service.deliverables.isNotEmpty())
        }
    }

    @Test
    fun consultationServiceType_legacyAliasesDeserializeSafely() {
        assertEquals(
            ConsultationServiceType.HOSPITALITY_PRE_OPENING,
            ConsultationServiceType.fromRaw("PRE_OPENING")
        )
        assertEquals(
            ConsultationServiceType.RESTAURANT_CAFE_DEVELOPMENT,
            ConsultationServiceType.fromRaw("CAFE_RESTAURANT_SETUP")
        )
        assertEquals(
            ConsultationServiceType.BAR_BEVERAGE_CONSULTANCY,
            ConsultationServiceType.fromRaw("BEVERAGE_MIXOLOGY")
        )
        assertEquals(
            ConsultationServiceType.RECRUITMENT_TALENT_ACQUISITION,
            ConsultationServiceType.fromRaw("EXECUTIVE_SEARCH")
        )
        assertEquals(
            ConsultationServiceType.OPERATIONS_MANAGEMENT,
            ConsultationServiceType.fromRaw("OPERATIONAL_AUDIT")
        )
    }

    @Test
    fun consultationInquiry_toCreateMap_excludesIdAndNullOptionalFields() {
        val inquiry = ConsultationInquiry(
            id = "should_not_be_serialized",
            propertyName = "Heritage Resort Project",
            city = "Mumbai",
            serviceType = ConsultationServiceType.HOSPITALITY_PRE_OPENING.name,
            scaleSummary = "90 Keys",
            contactName = "Managing Partner",
            contactEmail = "partner@hospitality.in",
            contactPhone = "+91 98200 11223",
            briefNotes = "   "
        )
        val payload = inquiry.toCreateMap("uid_123")
        assertFalse(payload.containsKey("id"))
        assertFalse(payload.containsKey("briefNotes"))
        assertEquals("uid_123", payload["userId"])
        assertEquals("SUBMITTED", payload["status"])
    }

    @Test
    fun serviceOffering_toAdminMap_excludesIdAndPreservesCmsFields() {
        val offering = ServiceOffering(
            id = "hospitality-pre-opening-consultancy",
            serviceType = ConsultationServiceType.HOSPITALITY_PRE_OPENING.name,
            sortOrder = 1L,
            slug = "hospitality-pre-opening-consultancy",
            divisionName = "MILA Consultancy",
            title = "Hospitality Pre-Opening Consultancy",
            subtitle = "Turnkey Critical Path",
            summary = "Pre-opening advisory across India.",
            detailedDescription = "End-to-end pre-opening stewardship from handover through launch.",
            servicesIncluded = listOf("Critical path governance"),
            ownerBenefits = listOf("Faster opening readiness"),
            deliverables = listOf("Pre-opening tracker"),
            typicalTimeline = "4 – 12 Months",
            imageKey = "palace_resort",
            imageUrl = null,
            published = true
        )
        val map = offering.toAdminMap()
        assertFalse(map.containsKey("id"))
        assertFalse(map.containsKey("imageUrl"))
        assertEquals(true, map["published"])
        assertEquals(1L, map["sortOrder"])
    }

    @Test
    fun clientProfile_toCreateAndUpdateMaps_preservePrivacyAndExcludeIdAndPassword() {
        val profile = ClientProfile(
            id = "primary",
            fullName = "Aarav Mehta",
            email = "aarav@courtyardgroup.in",
            mobileNumber = "+91 98200 11223",
            companyName = "Courtyard Hospitality Pvt Ltd",
            designation = "Managing Director",
            city = "Mumbai",
            businessCategory = BusinessCategory.LUXURY_HOTEL_RESORT.name
        )
        val createMap = profile.toCreateMap("uid_client_1")
        assertFalse(createMap.containsKey("id"))
        assertFalse(createMap.containsKey("password"))
        assertEquals("uid_client_1", createMap["userId"])
        assertEquals("LUXURY_HOTEL_RESORT", createMap["businessCategory"])

        val updateMap = profile.toUpdateMap()
        assertFalse(updateMap.containsKey("id"))
        assertFalse(updateMap.containsKey("userId"))
        assertFalse(updateMap.containsKey("createdAt"))
        assertFalse(updateMap.containsKey("password"))
        assertEquals("Mumbai", updateMap["city"])
    }

    @Test
    fun supabaseAuthValidator_validatesAllEightRegistrationFieldsAndPasswordStrength() {
        val validReq = ClientRegistrationRequest(
            fullName = "Rohan Kapoor",
            email = "rohan@ateliercafe.in",
            mobileNumber = "+91 98200 55443",
            companyName = "Atelier Hospitality",
            designation = "Founder",
            city = "Bengaluru",
            businessCategory = BusinessCategory.CAFE_ROASTERY_BAKERY,
            password = "MilaPass123"
        )
        assertNull(SupabaseAuthValidator.validateRegistration(validReq))

        val weakPasswordReq = validReq.copy(password = "short")
        assertNotNull(SupabaseAuthValidator.validateRegistration(weakPasswordReq))

        val missingCompanyReq = validReq.copy(companyName = " ")
        assertNotNull(SupabaseAuthValidator.validateRegistration(missingCompanyReq))

        assertNull(SupabaseAuthValidator.validateResetEmail("rohan@ateliercafe.in"))
        assertNotNull(SupabaseAuthValidator.validateResetEmail("not-an-email"))

        val profileMeta = SupabaseClientMetadata(
            fullName = validReq.fullName,
            email = validReq.email,
            mobileNumber = validReq.mobileNumber,
            companyName = validReq.companyName,
            designation = validReq.designation,
            city = validReq.city,
            businessCategory = validReq.businessCategory
        )
        assertNull(SupabaseAuthValidator.validateProfileUpdate(profileMeta))
    }

    @Test
    fun supabaseAuthClient_detectsPlaceholdersVsConfiguredCredentials() {
        val unconfigured = SupabaseAuthClient(
            supabaseUrl = "YOUR_SUPABASE_URL",
            supabaseAnonKey = "YOUR_SUPABASE_ANON_KEY"
        )
        assertFalse(unconfigured.isConfigured())

        val configured = SupabaseAuthClient(
            supabaseUrl = "https://exampleproject.supabase.co",
            supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sample"
        )
        assertTrue(configured.isConfigured())

        val confirmation = EnquiryConfirmation(
            referenceCode = "MILA-AB12CD",
            serviceType = ConsultationServiceType.BAR_BEVERAGE_CONSULTANCY,
            propertyName = "Atelier Rooftop",
            city = "Goa",
            scaleSummary = "90 Covers",
            contactName = "Rohan Kapoor",
            contactEmail = "rohan@ateliercafe.in"
        )
        assertEquals("MILA-AB12CD", confirmation.referenceCode)
    }

    @Test
    fun candidateProfile_toCreateMap_enforcesPrivateCvPathAndExcludesPublicLinksOrPasswords() {
        val validPrivateProfile = CandidateProfile(
            id = "primary",
            userId = "uid_cand_9",
            fullName = "Kabir Malhotra",
            email = "kabir@hospitality.in",
            mobileNumber = "+91 98111 22334",
            location = "Mumbai, Maharashtra",
            currentDesignation = "Executive Sous Chef",
            department = HospitalityDepartment.CULINARY.name,
            totalExperienceYears = 9L,
            currentEmployer = "Urban Luxury Hotel",
            previousEmployers = "Heritage Palace Resort",
            education = "IHM Pusa",
            skills = "Pre-Opening SOPs, Modern Indian Gastronomy, HACCP",
            preferredJobLocations = "Mumbai, Goa, Bengaluru",
            expectedSalary = "24 LPA",
            availability = CandidateAvailability.WITHIN_30_DAYS.name,
            noticePeriod = "30 Days",
            jobAlertsEnabled = true,
            cvFileName = "Kabir_Malhotra_CV.pdf",
            cvMimeType = "application/pdf",
            cvFileSizeBytes = 250_000L,
            cvStoragePath = "uid_cand_9/cv_1700000000_Kabir_Malhotra_CV.pdf"
        )
        val createMap = validPrivateProfile.toCreateMap("uid_cand_9")
        assertFalse(createMap.containsKey("id"))
        assertFalse(createMap.containsKey("password"))
        assertEquals("uid_cand_9/cv_1700000000_Kabir_Malhotra_CV.pdf", createMap["cvStoragePath"])
        assertEquals(true, createMap["jobAlertsEnabled"])

        val rejectedPublicProfile = validPrivateProfile.copy(
            cvStoragePath = "https://public.example.com/storage/v1/object/public/candidate-cvs/leaked.pdf"
        )
        val filteredMap = rejectedPublicProfile.toCreateMap("uid_cand_9")
        assertFalse(filteredMap.containsKey("cvStoragePath"))
    }

    @Test
    fun candidateValidator_validatesFifteenFieldsCvTypesAndPrivatePaths() {
        val validReq = CandidateRegistrationRequest(
            fullName = "Ananya Verma",
            email = "ananya@hospitality.in",
            mobileNumber = "+91 98200 77889",
            location = "New Delhi NCR",
            currentDesignation = "General Manager",
            department = HospitalityDepartment.EXECUTIVE,
            totalExperienceYears = 12L,
            currentEmployer = "Boutique Heritage Property",
            previousEmployers = "Luxury Resort Group",
            education = "WGSHA Manipal",
            skills = "Pre-Opening Leadership, P&L Ownership",
            preferredJobLocations = "New Delhi NCR, Udaipur",
            expectedSalary = "40 LPA",
            availability = CandidateAvailability.WITHIN_60_DAYS,
            noticePeriod = "60 Days",
            password = "TalentPass123"
        )
        assertNull(CandidateValidator.validateRegistration(validReq))
        assertNotNull(CandidateValidator.validateRegistration(validReq.copy(expectedSalary = " ")))
        assertNotNull(CandidateValidator.validateRegistration(validReq.copy(totalExperienceYears = 75L)))

        assertNull(CandidateValidator.validateCvFile("resume.pdf", "application/pdf", 512_000L))
        assertNull(CandidateValidator.validateCvFile("resume.doc", "application/msword", 512_000L))
        assertNull(
            CandidateValidator.validateCvFile(
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                512_000L
            )
        )
        assertNotNull(CandidateValidator.validateCvFile("malware.exe", "application/octet-stream", 1024L))
        assertNotNull(CandidateValidator.validateCvFile("huge.pdf", "application/pdf", 12L * 1024L * 1024L))

        val storageKey = CandidateValidator.buildPrivateStoragePath("uid_42", "my resume (final).docx", 170000L)
        assertFalse(storageKey.contains("http"))
        assertFalse(storageKey.contains("/object/public/"))
        assertTrue(storageKey.startsWith("uid_42/cv_170000_"))
    }

    @Test
    fun experienceFilterAndJobNotification_workAsExpected() {
        assertTrue(ExperienceFilter.MID.matches(4))
        assertFalse(ExperienceFilter.MID.matches(8))
        assertTrue(ExperienceFilter.SENIOR.matches(7))
        assertTrue(ExperienceFilter.EXECUTIVE.matches(12))
        assertEquals(ApplicationStatus.SHORTLISTED, ApplicationStatus.fromRaw("SHORTLISTED"))

        val notification = JobNotification(
            id = "alert_role_1",
            userId = "uid_cand_9",
            roleId = "role_gm_udaipur",
            title = "Vacancy Match: Pre-Opening General Manager",
            message = "Udaipur heritage resort search.",
            department = "Pre-Opening",
            location = "Udaipur, Rajasthan",
            isRead = false
        )
        val map = notification.toCreateMap("uid_cand_9")
        assertFalse(map.containsKey("id"))
        assertEquals("role_gm_udaipur", map["roleId"])
        assertEquals(false, map["isRead"])

        val unconfigured = SupabaseTalentClient("YOUR_SUPABASE_URL", "YOUR_SUPABASE_ANON_KEY")
        assertFalse(unconfigured.isConfigured())
        val configured = SupabaseTalentClient("https://talentproject.supabase.co", "anon-key-sample")
        assertTrue(configured.isConfigured())
    }

    @Test
    fun adminRoles_enforceStrictModuleIsolationAndReadOnlyReporting() {
        assertEquals(19, AdminModule.entries.size)
        assertEquals(AdminModule.entries.toSet(), AdminRole.SUPER_ADMIN.allowedModules())
        assertTrue(AdminRole.SUPER_ADMIN.canMutate(AdminModule.ADMIN_USER_MANAGEMENT))
        assertTrue(AdminRole.SUPER_ADMIN.canMutate(AdminModule.SETTINGS))

        assertTrue(AdminRole.REPORTING_ADMIN.isReadOnly)
        assertEquals(
            setOf(
                AdminModule.DASHBOARD_OVERVIEW,
                AdminModule.REPORTS_AND_ANALYTICS,
                AdminModule.ACTIVITY_LOGS
            ),
            AdminRole.REPORTING_ADMIN.allowedModules()
        )
        AdminModule.entries.forEach { module ->
            assertFalse(AdminRole.REPORTING_ADMIN.canMutate(module))
        }

        assertEquals(
            setOf(
                AdminModule.DASHBOARD_OVERVIEW,
                AdminModule.CLIENT_ENQUIRIES,
                AdminModule.ACTIVITY_LOGS
            ),
            AdminRole.CONSULTANT.allowedModules()
        )
        assertTrue(AdminRole.CONSULTANT.canMutate(AdminModule.CLIENT_ENQUIRIES))
        assertFalse(AdminRole.CONSULTANT.canMutate(AdminModule.CANDIDATE_MANAGEMENT))

        val recruitMods = AdminRole.RECRUITMENT_ADMIN.allowedModules()
        assertTrue(AdminModule.JOB_VACANCY_MANAGEMENT in recruitMods)
        assertTrue(AdminModule.CV_DATABASE in recruitMods)
        assertTrue(AdminModule.JOB_APPLICATIONS in recruitMods)
        assertFalse(AdminModule.WEBSITE_CONTENT_MANAGEMENT in recruitMods)
        assertFalse(AdminRole.RECRUITMENT_ADMIN.canMutate(AdminModule.CLIENT_MANAGEMENT))

        val contentMods = AdminRole.CONTENT_ADMIN.allowedModules()
        assertTrue(AdminModule.SERVICE_MANAGEMENT in contentMods)
        assertTrue(AdminModule.PORTFOLIO_MANAGEMENT in contentMods)
        assertTrue(AdminModule.TEAM_MANAGEMENT in contentMods)
        assertFalse(AdminModule.CV_DATABASE in contentMods)
        assertFalse(AdminRole.CONTENT_ADMIN.canMutate(AdminModule.ADMIN_USER_MANAGEMENT))
    }

    @Test
    fun adminModels_normalizeInvitesAndMapVacanciesAndStatuses() {
        assertEquals("partner@milahospitality.in", AdminInvite.normalizeEmailKey("  Partner@MilaHospitality.IN "))
        assertEquals(InquiryStatus.CONVERTED, InquiryStatus.fromRaw("CONVERTED"))
        assertEquals(ApplicationStatus.OFFER_EXTENDED, ApplicationStatus.fromRaw("OFFER_EXTENDED"))

        val role = MilaCatalog.openRoles.first()
        val record = JobVacancyRecord.fromHospitalityRole(role, published = true)
        assertEquals(role.id, record.id)
        assertEquals(role.title, record.title)
        assertEquals(true, record.toAdminMap()["published"])
    }

    @Test
    fun supabaseConfig_rejectsServiceRoleKeysPlaceholdersHttpAndPublicCvLinks() {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val anonPayload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"role":"anon","iss":"supabase"}""".toByteArray())
        val servicePayload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"role":"service_role","iss":"supabase"}""".toByteArray())
        val fakeAnonJwt = "$header.$anonPayload.signature"
        val fakeServiceJwt = "$header.$servicePayload.signature"

        assertFalse(SupabaseConfig.looksLikeServiceRoleKey(fakeAnonJwt))
        assertTrue(SupabaseConfig.looksLikeServiceRoleKey(fakeServiceJwt))

        assertTrue(SupabaseConfig.isConfigured("https://mila-hosp.supabase.co", fakeAnonJwt))
        assertFalse(SupabaseConfig.isConfigured("https://mila-hosp.supabase.co", fakeServiceJwt))
        assertFalse(SupabaseConfig.isConfigured("http://mila-hosp.supabase.co", fakeAnonJwt))
        assertFalse(SupabaseConfig.isConfigured("YOUR_SUPABASE_URL", "YOUR_SUPABASE_ANON_KEY"))

        // Private CV path validation
        assertTrue(SupabaseConfig.isSafePrivateCvPath("uid_42/cv_1700000_resume.pdf"))
        assertFalse(SupabaseConfig.isSafePrivateCvPath("https://project.supabase.co/storage/v1/object/public/candidate-cvs/a.pdf"))
        assertFalse(SupabaseConfig.isSafePrivateCvPath("../other_user/cv.pdf"))
        assertFalse(SupabaseConfig.isSafePrivateCvPath("/leading/slash.pdf"))
        assertFalse(SupabaseConfig.isSafePrivateCvPath(""))

        // Query value encoding
        assertEquals("a%20b%26c%3Dd", SupabaseConfig.encodeQueryValue("a b&c=d"))
    }

    @Test
    fun jobMatchingEngine_strictlyMatchesEligibleCandidatesAndExcludesIrrelevantOnes() {
        val mixologyVacancy = JobVacancyRecord(
            id = "vac_mix_mumbai",
            title = "Head Mixologist & Bar Program Director",
            propertyTier = "Luxury Rooftop Bar",
            location = "Mumbai, Maharashtra",
            cityRegion = "Mumbai",
            compensation = "INR 26–32 LPA",
            experienceRequired = "7+ Years",
            minExperienceYears = 7L,
            category = RoleCategory.MIXOLOGY.name,
            summary = "Lead craft cocktail program and bar P&L.",
            desiredSkills = listOf("Craft Mixology", "Bar P&L", "Pre-Opening"),
            urgent = true,
            published = true
        )

        val eligibleBarLead = CandidateProfile(
            userId = "cand_eligible",
            fullName = "Rohan Kapoor",
            email = "rohan@bar.in",
            location = "Pune",
            currentDesignation = "Head Mixologist",
            department = HospitalityDepartment.MIXOLOGY.name,
            totalExperienceYears = 8L,
            skills = "Craft Mixology, Bar P&L, Beverage Training",
            preferredJobLocations = "Mumbai, Goa",
            availability = CandidateAvailability.IMMEDIATE.name,
            jobAlertsEnabled = true
        )

        val panIndiaEligible = eligibleBarLead.copy(
            userId = "cand_pan_india",
            fullName = "Meera Nair",
            location = "Kochi",
            preferredJobLocations = "Pan-India",
            totalExperienceYears = 6L // within default 1-year tolerance of 7 yrs
        )

        val inactiveAlertsCandidate = eligibleBarLead.copy(
            userId = "cand_paused",
            fullName = "Paused Seeker",
            jobAlertsEnabled = false
        )

        val wrongDepartmentCandidate = eligibleBarLead.copy(
            userId = "cand_culinary",
            fullName = "Chef Vikram",
            currentDesignation = "Executive Pastry Chef",
            department = HospitalityDepartment.CULINARY.name,
            skills = "Artisan Pastry, Viennoiserie"
        )

        val wrongLocationCandidate = eligibleBarLead.copy(
            userId = "cand_jaipur_only",
            fullName = "Arjun Singh",
            location = "Jaipur",
            preferredJobLocations = "Jaipur, Udaipur"
        )

        val juniorExperienceCandidate = eligibleBarLead.copy(
            userId = "cand_junior",
            fullName = "Junior Bartender",
            totalExperienceYears = 2L
        )

        val all = listOf(
            eligibleBarLead,
            panIndiaEligible,
            inactiveAlertsCandidate,
            wrongDepartmentCandidate,
            wrongLocationCandidate,
            juniorExperienceCandidate
        )

        val controls = NotificationControlSettings(
            requireDepartmentMatch = true,
            requireLocationMatch = true,
            experienceToleranceYears = 1L
        )

        val results = JobMatchingEngine.evaluateAllCandidates(mixologyVacancy, all, controls)
        val eligibleIds = results.filter { it.eligible }.map { it.candidate.userId }.toSet()

        assertEquals(setOf("cand_eligible", "cand_pan_india"), eligibleIds)
        assertTrue(results.first { it.candidate.userId == "cand_eligible" }.matchScore >= 80)

        // Verify strict 0-year tolerance excludes the 6-year candidate
        val strictResults = JobMatchingEngine.findEligibleCandidates(
            mixologyVacancy,
            all,
            controls.copy(experienceToleranceYears = 0L)
        )
        assertEquals(setOf("cand_eligible"), strictResults.map { it.candidate.userId }.toSet())

        // Verify unpublished vacancy dispatches to ZERO candidates
        val draftResults = JobMatchingEngine.findEligibleCandidates(
            mixologyVacancy.copy(published = false),
            all,
            controls
        )
        assertTrue(draftResults.isEmpty())
    }
}
