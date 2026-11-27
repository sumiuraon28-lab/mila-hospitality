package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CaseStudy
import com.example.data.ConsultancyService
import com.example.data.ContactDetails
import com.example.data.HospitalityRole
import com.example.data.MilaCatalog
import com.example.data.PromoBanner
import com.example.data.PublicCmsRepository
import com.example.data.ServiceCatalogRepository
import com.example.data.SitePageContent
import com.example.data.SupabaseDatabaseClient
import com.example.data.Testimonial
import com.example.data.isContactDetails
import com.example.data.isPromoBanner
import com.example.data.toContactDetails
import com.example.data.toPromoBanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ServiceCatalogViewModel(
    repository: ServiceCatalogRepository? = null,
    publicCmsRepository: PublicCmsRepository? = null,
    private val supabaseDb: SupabaseDatabaseClient = SupabaseDatabaseClient()
) : ViewModel() {

    private val _supabaseServices = MutableStateFlow<List<ConsultancyService>>(emptyList())
    private val _hasLoadedSupabaseServices = MutableStateFlow(false)
    private val _supabaseVacancies = MutableStateFlow<List<HospitalityRole>>(emptyList())
    private val _hasLoadedSupabaseVacancies = MutableStateFlow(false)
    private val _isRefreshingVacancies = MutableStateFlow(false)
    val isRefreshingVacancies: StateFlow<Boolean> = _isRefreshingVacancies.asStateFlow()
    private val _supabasePortfolio = MutableStateFlow<List<CaseStudy>>(emptyList())
    private val _hasLoadedSupabasePortfolio = MutableStateFlow(false)
    private val _supabaseTestimonials = MutableStateFlow<List<Testimonial>>(emptyList())
    private val _hasLoadedSupabaseTestimonials = MutableStateFlow(false)
    private val _websiteContent = MutableStateFlow<List<SitePageContent>>(emptyList())
    val websiteContent: StateFlow<List<SitePageContent>> = _websiteContent.asStateFlow()
    private val _promoBanners = MutableStateFlow<List<PromoBanner>>(emptyList())
    val promoBanners: StateFlow<List<PromoBanner>> = _promoBanners.asStateFlow()
    private val _contactDetails = MutableStateFlow(ContactDetails())
    val contactDetails: StateFlow<ContactDetails> = _contactDetails.asStateFlow()
    private val shownAutoPopupIds = mutableSetOf<String>()

    init {
        refreshSupabasePublicCatalog()
    }

    fun shouldShowAutoPopup(bannerId: String): Boolean =
        bannerId.isNotBlank() && !shownAutoPopupIds.contains(bannerId)

    fun markAutoPopupShown(bannerId: String) {
        if (bannerId.isNotBlank()) {
            shownAutoPopupIds.add(bannerId)
        }
    }

    fun refreshSupabasePublicCatalog() {
        if (!supabaseDb.isConfigured()) return
        viewModelScope.launch {
            supabaseDb.fetchPublishedServices().onSuccess {
                _supabaseServices.value = it
                _hasLoadedSupabaseServices.value = true
            }
            _isRefreshingVacancies.value = true
            supabaseDb.fetchPublishedVacancies().onSuccess {
                _supabaseVacancies.value = it
                _hasLoadedSupabaseVacancies.value = true
            }
            _isRefreshingVacancies.value = false
            supabaseDb.fetchPublishedPortfolio().onSuccess {
                _supabasePortfolio.value = it
                _hasLoadedSupabasePortfolio.value = true
            }
            supabaseDb.fetchPublishedTestimonials().onSuccess {
                _supabaseTestimonials.value = it
                _hasLoadedSupabaseTestimonials.value = true
            }
            supabaseDb.fetchPublishedWebsiteContent().onSuccess { list ->
                _websiteContent.value = list.filterNot { it.isPromoBanner() || it.isContactDetails() }
                _promoBanners.value = list.filter { it.isPromoBanner() }.map { it.toPromoBanner() }
                list.firstOrNull { it.isContactDetails() }?.let { _contactDetails.value = it.toContactDetails() }
            }
        }
    }

    fun refreshServices() {
        if (!supabaseDb.isConfigured()) return
        viewModelScope.launch {
            supabaseDb.fetchPublishedServices().onSuccess {
                _supabaseServices.value = it
                _hasLoadedSupabaseServices.value = true
            }
        }
    }

    fun refreshPortfolio() {
        if (!supabaseDb.isConfigured()) return
        viewModelScope.launch {
            supabaseDb.fetchPublishedPortfolio().onSuccess {
                _supabasePortfolio.value = it
                _hasLoadedSupabasePortfolio.value = true
            }
        }
    }

    fun refreshBanners() {
        if (!supabaseDb.isConfigured()) return
        viewModelScope.launch {
            supabaseDb.fetchPublishedBanners().onSuccess { banners ->
                _promoBanners.value = banners
            }
        }
    }

    fun refreshContactDetails() {
        if (!supabaseDb.isConfigured()) return
        viewModelScope.launch {
            supabaseDb.fetchPublishedContactDetails().onSuccess { details ->
                if (details != null) {
                    _contactDetails.value = details
                }
            }
        }
    }

    fun refreshVacancies() {
        if (!supabaseDb.isConfigured()) return
        viewModelScope.launch {
            _isRefreshingVacancies.value = true
            supabaseDb.fetchPublishedVacancies().onSuccess {
                _supabaseVacancies.value = it
                _hasLoadedSupabaseVacancies.value = true
            }
            _isRefreshingVacancies.value = false
        }
    }

    fun refreshTestimonials() {
        if (!supabaseDb.isConfigured()) return
        viewModelScope.launch {
            supabaseDb.fetchPublishedTestimonials().onSuccess {
                _supabaseTestimonials.value = it
                _hasLoadedSupabaseTestimonials.value = true
            }
        }
    }

    suspend fun submitClientReview(
        accessToken: String,
        userId: String,
        reviewerName: String,
        reviewerEmail: String,
        engagementContext: String,
        rating: Int,
        quote: String
    ): Result<Unit> {
        val result = supabaseDb.submitClientReview(
            accessToken = accessToken,
            userId = userId,
            reviewerName = reviewerName,
            reviewerEmail = reviewerEmail,
            engagementContext = engagementContext,
            rating = rating,
            quote = quote
        )
        if (result.isSuccess) {
            supabaseDb.fetchPublishedTestimonials().onSuccess {
                _supabaseTestimonials.value = it
                _hasLoadedSupabaseTestimonials.value = true
            }
        }
        return result
    }

    val services: StateFlow<List<ConsultancyService>> = combine(
        repository?.observePublishedServices() ?: flowOf(MilaCatalog.services),
        _supabaseServices,
        _hasLoadedSupabaseServices
    ) { firestoreList, supabaseList, hasLoadedSupabase ->
        if (hasLoadedSupabase) supabaseList else firestoreList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MilaCatalog.services
    )

    val vacancies: StateFlow<List<HospitalityRole>> = combine(
        publicCmsRepository?.observePublishedVacancies() ?: flowOf(MilaCatalog.openRoles),
        _supabaseVacancies,
        _hasLoadedSupabaseVacancies
    ) { firestoreList, supabaseList, hasLoadedSupabase ->
        if (hasLoadedSupabase) supabaseList else firestoreList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MilaCatalog.openRoles
    )

    val portfolio: StateFlow<List<CaseStudy>> = combine(
        publicCmsRepository?.observePublishedPortfolio() ?: flowOf(MilaCatalog.caseStudies),
        _supabasePortfolio,
        _hasLoadedSupabasePortfolio
    ) { firestoreList, supabaseList, hasLoadedSupabase ->
        if (hasLoadedSupabase) supabaseList else firestoreList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MilaCatalog.caseStudies
    )

    val testimonials: StateFlow<List<Testimonial>> = combine(
        publicCmsRepository?.observePublishedTestimonials() ?: flowOf(MilaCatalog.testimonials),
        _supabaseTestimonials,
        _hasLoadedSupabaseTestimonials
    ) { firestoreList, supabaseList, hasLoadedSupabase ->
        if (hasLoadedSupabase) supabaseList else firestoreList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MilaCatalog.testimonials
    )
}
