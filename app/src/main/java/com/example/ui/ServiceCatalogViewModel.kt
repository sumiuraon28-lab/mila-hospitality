package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CaseStudy
import com.example.data.ConsultancyService
import com.example.data.HospitalityRole
import com.example.data.MilaCatalog
import com.example.data.PublicCmsRepository
import com.example.data.ServiceCatalogRepository
import com.example.data.SitePageContent
import com.example.data.SupabaseDatabaseClient
import com.example.data.Testimonial
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
    private val _supabaseVacancies = MutableStateFlow<List<HospitalityRole>>(emptyList())
    private val _hasLoadedSupabaseVacancies = MutableStateFlow(false)
    private val _isRefreshingVacancies = MutableStateFlow(false)
    val isRefreshingVacancies: StateFlow<Boolean> = _isRefreshingVacancies.asStateFlow()
    private val _supabasePortfolio = MutableStateFlow<List<CaseStudy>>(emptyList())
    private val _supabaseTestimonials = MutableStateFlow<List<Testimonial>>(emptyList())
    private val _websiteContent = MutableStateFlow<List<SitePageContent>>(emptyList())
    val websiteContent: StateFlow<List<SitePageContent>> = _websiteContent.asStateFlow()

    init {
        refreshSupabasePublicCatalog()
    }

    fun refreshSupabasePublicCatalog() {
        if (!supabaseDb.isConfigured()) return
        viewModelScope.launch {
            supabaseDb.fetchPublishedServices().onSuccess { if (it.isNotEmpty()) _supabaseServices.value = it }
            _isRefreshingVacancies.value = true
            supabaseDb.fetchPublishedVacancies().onSuccess {
                _supabaseVacancies.value = it
                _hasLoadedSupabaseVacancies.value = true
            }
            _isRefreshingVacancies.value = false
            supabaseDb.fetchPublishedPortfolio().onSuccess { if (it.isNotEmpty()) _supabasePortfolio.value = it }
            supabaseDb.fetchPublishedTestimonials().onSuccess { if (it.isNotEmpty()) _supabaseTestimonials.value = it }
            supabaseDb.fetchPublishedWebsiteContent().onSuccess { if (it.isNotEmpty()) _websiteContent.value = it }
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

    val services: StateFlow<List<ConsultancyService>> = combine(
        repository?.observePublishedServices() ?: flowOf(MilaCatalog.services),
        _supabaseServices
    ) { firestoreList, supabaseList ->
        if (supabaseList.isNotEmpty()) supabaseList else firestoreList
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
        _supabasePortfolio
    ) { firestoreList, supabaseList ->
        if (supabaseList.isNotEmpty()) supabaseList else firestoreList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MilaCatalog.caseStudies
    )

    val testimonials: StateFlow<List<Testimonial>> = combine(
        publicCmsRepository?.observePublishedTestimonials() ?: flowOf(MilaCatalog.testimonials),
        _supabaseTestimonials
    ) { firestoreList, supabaseList ->
        if (supabaseList.isNotEmpty()) supabaseList else firestoreList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MilaCatalog.testimonials
    )
}
