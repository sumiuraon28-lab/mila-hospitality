package com.example

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.ContactMail
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.ContactMail
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.SupabaseAuthClient
import com.example.auth.SupabaseAuthValidator
import com.example.auth.SupabaseCandidateSession
import com.example.auth.SupabaseSession
import com.example.auth.SupabaseTalentClient
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.firebaseAuthOrNull
import com.example.auth.signOut
import com.example.data.CaseStudy
import com.example.data.ClientProfile
import com.example.data.ConsultancyService
import com.example.data.ConsultationServiceType
import com.example.data.ContactDetails
import com.example.data.EnquiryConfirmation
import com.example.data.HospitalityRole
import com.example.data.MilaCatalog
import com.example.data.MilaRepository
import com.example.data.PromoBanner
import com.example.data.PublicCmsRepository
import com.example.data.ServiceCatalogRepository
import com.example.data.Testimonial
import com.example.notifications.MilaNotificationChannels
import com.example.notifications.PushTokenManager
import com.example.ui.MandatesViewModel
import com.example.ui.ServiceCatalogViewModel
import com.example.ui.UiState
import com.example.ui.components.ConsultationRequestSheet
import com.example.ui.components.DrawerNavEntry
import com.example.ui.components.JobApplicationSheet
import com.example.ui.components.MilaBrandHeader
import com.example.ui.components.MilaDrawerSheetContent
import com.example.ui.screens.AboutUsScreen
import com.example.ui.screens.AdvisoryScreen
import com.example.ui.screens.AtelierScreen
import com.example.ui.screens.ClientLoginScreen
import com.example.ui.screens.ContactUsScreen
import com.example.ui.screens.EnquiryConfirmationDialog
import com.example.ui.screens.GoogleMobilePromptDialog
import com.example.ui.screens.IndustriesWeServeScreen
import com.example.ui.screens.JobSeekerLoginScreen
import com.example.ui.screens.MilaAuthEntryScreen
import com.example.ui.screens.OurExpertiseScreen
import com.example.ui.screens.OurPortfolioScreen
import com.example.ui.screens.ServiceDetailScreen
import com.example.ui.screens.TalentScreen
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoal
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaCharcoalElevated
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaHospitalityTheme
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaWarmStone
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_ABOUT = "about_us"
const val ROUTE_SERVICES = "our_services"
const val ROUTE_SERVICE_DETAIL_BASE = "service_detail"
const val ROUTE_SERVICE_DETAIL = "service_detail/{serviceSlug}"
const val ROUTE_INDUSTRIES = "industries_we_serve"
const val ROUTE_EXPERTISE = "our_expertise"
const val ROUTE_PORTFOLIO = "our_portfolio"
const val ROUTE_CAREERS = "careers"
const val ROUTE_CONTACT = "contact_us"
const val ROUTE_CLIENT_LOGIN = "client_login"
const val ROUTE_JOB_SEEKER_LOGIN = "job_seeker_login"

private data class MilaPrimaryTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val primaryTabs = listOf(
    MilaPrimaryTab(ROUTE_HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    MilaPrimaryTab(ROUTE_SERVICES, R.string.nav_services, Icons.Rounded.BusinessCenter, Icons.Outlined.BusinessCenter, "nav_services"),
    MilaPrimaryTab(ROUTE_PORTFOLIO, R.string.nav_portfolio, Icons.Rounded.CollectionsBookmark, Icons.Outlined.CollectionsBookmark, "nav_portfolio"),
    MilaPrimaryTab(ROUTE_CAREERS, R.string.nav_careers, Icons.Rounded.Work, Icons.Outlined.WorkOutline, "nav_careers"),
    MilaPrimaryTab(ROUTE_CONTACT, R.string.nav_contact, Icons.Rounded.ContactMail, Icons.Outlined.ContactMail, "nav_contact")
)

private fun buildDrawerDirectory(services: List<ConsultancyService>): List<DrawerNavEntry> {
    val staticTop = listOf(
        DrawerNavEntry(ROUTE_HOME, "Home", "MILA Overview", Icons.Outlined.Home),
        DrawerNavEntry(ROUTE_ABOUT, "About Us", "MILA Overview", Icons.Outlined.Info),
        DrawerNavEntry(ROUTE_SERVICES, "Our Services (MILA Consultancy)", "Three Business Divisions", Icons.Outlined.BusinessCenter),
        DrawerNavEntry(ROUTE_CAREERS, "Careers (MILA Talent)", "Three Business Divisions", Icons.Outlined.WorkOutline),
        DrawerNavEntry(ROUTE_EXPERTISE, "Our Expertise (MILA Academy)", "Three Business Divisions", Icons.Outlined.WorkspacePremium),
        DrawerNavEntry(ROUTE_INDUSTRIES, "Industries We Serve", "Sectors & Portfolio", Icons.Outlined.Apartment),
        DrawerNavEntry(ROUTE_PORTFOLIO, "Our Portfolio", "Sectors & Portfolio", Icons.Outlined.CollectionsBookmark)
    )
    val serviceEntries = services.map { svc ->
        val icon = when (svc.type) {
            ConsultationServiceType.HOSPITALITY_PRE_OPENING -> Icons.Outlined.Apartment
            ConsultationServiceType.BAR_BEVERAGE_CONSULTANCY -> Icons.Outlined.WorkspacePremium
            ConsultationServiceType.HOSPITALITY_TRAINING -> Icons.Outlined.Info
            ConsultationServiceType.RECRUITMENT_TALENT_ACQUISITION -> Icons.Outlined.PersonSearch
            ConsultationServiceType.BUSINESS_CONCEPT_DEVELOPMENT -> Icons.Outlined.CollectionsBookmark
            else -> Icons.Outlined.BusinessCenter
        }
        DrawerNavEntry(
            route = "$ROUTE_SERVICE_DETAIL_BASE/${svc.slug}",
            label = "${svc.numberBadge}. ${svc.title}",
            group = "Our Services (${services.size})",
            icon = icon
        )
    }
    return staticTop + serviceEntries + listOf(
        DrawerNavEntry(ROUTE_CONTACT, "Contact Us", "Connect", Icons.Outlined.ContactMail)
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MilaNotificationChannels.ensureChannelsCreated(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            MilaHospitalityTheme {
                MilaAppRoot()
            }
        }
    }
}

private fun firestoreOrNull(context: Context): FirebaseFirestore? = runCatching {
    val databaseId = context.getString(R.string.firestore_database_id)
    FirebaseFirestore.getInstance(databaseId)
}.getOrNull()

@Composable
fun MilaAppRoot() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val supabaseAuthClient = remember(context) { SupabaseAuthClient.getInstance(context) }
    val supabaseSession by supabaseAuthClient.session.collectAsStateWithLifecycle()
    val pendingDraftProfile by supabaseAuthClient.pendingDraftProfile.collectAsStateWithLifecycle()
    val supabaseTalentClient = remember { SupabaseTalentClient() }
    val supabaseCandidateSession by supabaseTalentClient.session.collectAsStateWithLifecycle()
    val pendingDraftCandidate by supabaseTalentClient.pendingDraftCandidate.collectAsStateWithLifecycle()
    val firebaseAuth = remember { firebaseAuthOrNull() }
    val currentUser by remember(firebaseAuth) {
        firebaseAuth?.authStateFlow() ?: flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = firebaseAuth?.currentUser)

    LaunchedEffect(Unit) {
        supabaseAuthClient.restoreSavedSession()
        if (supabaseAuthClient.session.value == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    // Silently keep the Supabase session token fresh whenever the user returns from another app
    DisposableEffect(lifecycleOwner, supabaseAuthClient) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (supabaseAuthClient.hasSavedSession()) {
                    scope.launch {
                        supabaseAuthClient.restoreSavedSession()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(supabaseSession?.userId, supabaseSession?.accessToken) {
        val active = supabaseSession
        if (active != null) {
            supabaseTalentClient.syncFromAuthSession(active)
        }
    }

    val catalogViewModel: ServiceCatalogViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = firestoreOrNull(app)
                ServiceCatalogViewModel(
                    repository = db?.let { ServiceCatalogRepository(it) },
                    publicCmsRepository = db?.let { PublicCmsRepository(it) }
                )
            }
        }
    )
    val services by catalogViewModel.services.collectAsStateWithLifecycle()
    val vacancies by catalogViewModel.vacancies.collectAsStateWithLifecycle()
    val isRefreshingVacancies by catalogViewModel.isRefreshingVacancies.collectAsStateWithLifecycle()
    val portfolio by catalogViewModel.portfolio.collectAsStateWithLifecycle()
    val testimonials by catalogViewModel.testimonials.collectAsStateWithLifecycle()
    val promoBanners by catalogViewModel.promoBanners.collectAsStateWithLifecycle()
    val contactDetails by catalogViewModel.contactDetails.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, catalogViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                catalogViewModel.refreshServices()
                catalogViewModel.refreshPortfolio()
                catalogViewModel.refreshTestimonials()
                catalogViewModel.refreshBanners()
                catalogViewModel.refreshContactDetails()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isAuthenticated = currentUser != null || supabaseSession != null || supabaseCandidateSession != null

    val handleMemberSignOut: () -> Unit = {
        scope.launch {
            supabaseAuthClient.signOut()
            supabaseTalentClient.signOut()
        }
        signOut(credentialManager = credentialManager, scope = scope) {}
    }

    if (!isAuthenticated) {
        MilaAuthEntryScreen(
            supabaseAuthClient = supabaseAuthClient,
            supabaseTalentClient = supabaseTalentClient
        )
    } else {
        val user = currentUser
        val firebaseDb = remember(context) { firestoreOrNull(context) }
        if (user != null && firebaseDb != null) {
            key(user.uid) {
                val mandatesViewModel: MandatesViewModel = viewModel(
                    key = user.uid,
                    factory = viewModelFactory {
                        initializer {
                            MandatesViewModel(MilaRepository(firebaseDb), user.uid)
                        }
                    }
                )
                LaunchedEffect(user.uid, pendingDraftProfile) {
                    val draft = pendingDraftProfile
                    if (draft != null) {
                        mandatesViewModel.saveClientProfile(
                            ClientProfile(
                                fullName = draft.fullName.ifBlank { user.displayName.orEmpty() },
                                email = draft.email.ifBlank { user.email.orEmpty() },
                                mobileNumber = draft.mobileNumber,
                                companyName = draft.companyName,
                                designation = draft.designation,
                                city = draft.city,
                                businessCategory = draft.businessCategory.name
                            )
                        ) {
                            supabaseAuthClient.clearPendingDraftProfile()
                        }
                    }
                }
                LaunchedEffect(user.uid, pendingDraftCandidate) {
                    val draft = pendingDraftCandidate
                    if (draft != null) {
                        mandatesViewModel.saveCandidateProfile(
                            draft.copy(
                                userId = user.uid,
                                fullName = draft.fullName.ifBlank { user.displayName.orEmpty() },
                                email = draft.email.ifBlank { user.email.orEmpty() }
                            )
                        ) {
                            supabaseTalentClient.clearPendingDraftCandidate()
                        }
                    }
                }
                MilaAppShell(
                    currentUser = user,
                    supabaseSession = supabaseSession,
                    supabaseAuthClient = supabaseAuthClient,
                    supabaseCandidateSession = supabaseCandidateSession,
                    supabaseTalentClient = supabaseTalentClient,
                    services = services,
                    onRefreshServices = { catalogViewModel.refreshServices() },
                    vacancies = vacancies,
                    isRefreshingVacancies = isRefreshingVacancies,
                    onRefreshVacancies = { catalogViewModel.refreshVacancies() },
                    portfolio = portfolio,
                    onRefreshPortfolio = { catalogViewModel.refreshPortfolio() },
                    testimonials = testimonials,
                    onRefreshTestimonials = { catalogViewModel.refreshTestimonials() },
                    onSubmitReview = { accessToken, userId, name, email, ctx, rating, quote ->
                        catalogViewModel.submitClientReview(accessToken, userId, name, email, ctx, rating, quote)
                    },
                    promoBanners = promoBanners,
                    onRefreshBanners = { catalogViewModel.refreshBanners() },
                    contactDetails = contactDetails,
                    onRefreshContactDetails = { catalogViewModel.refreshContactDetails() },
                    shouldShowAutoPopup = { id -> catalogViewModel.shouldShowAutoPopup(id) },
                    onAutoPopupShown = { id -> catalogViewModel.markAutoPopupShown(id) },
                    onSignOut = handleMemberSignOut,
                    mandatesViewModel = mandatesViewModel
                )
            }
        } else {
            MilaAppShell(
                currentUser = null,
                supabaseSession = supabaseSession,
                supabaseAuthClient = supabaseAuthClient,
                supabaseCandidateSession = supabaseCandidateSession,
                supabaseTalentClient = supabaseTalentClient,
                services = services,
                onRefreshServices = { catalogViewModel.refreshServices() },
                vacancies = vacancies,
                isRefreshingVacancies = isRefreshingVacancies,
                onRefreshVacancies = { catalogViewModel.refreshVacancies() },
                portfolio = portfolio,
                onRefreshPortfolio = { catalogViewModel.refreshPortfolio() },
                testimonials = testimonials,
                onRefreshTestimonials = { catalogViewModel.refreshTestimonials() },
                onSubmitReview = { accessToken, userId, name, email, ctx, rating, quote ->
                    catalogViewModel.submitClientReview(accessToken, userId, name, email, ctx, rating, quote)
                },
                promoBanners = promoBanners,
                onRefreshBanners = { catalogViewModel.refreshBanners() },
                contactDetails = contactDetails,
                onRefreshContactDetails = { catalogViewModel.refreshContactDetails() },
                shouldShowAutoPopup = { id -> catalogViewModel.shouldShowAutoPopup(id) },
                onAutoPopupShown = { id -> catalogViewModel.markAutoPopupShown(id) },
                onSignOut = handleMemberSignOut,
                mandatesViewModel = null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilaAppShell(
    currentUser: FirebaseUser?,
    supabaseSession: SupabaseSession?,
    supabaseAuthClient: SupabaseAuthClient,
    supabaseCandidateSession: SupabaseCandidateSession?,
    supabaseTalentClient: SupabaseTalentClient,
    services: List<ConsultancyService>,
    onRefreshServices: () -> Unit,
    vacancies: List<HospitalityRole>,
    isRefreshingVacancies: Boolean,
    onRefreshVacancies: () -> Unit,
    portfolio: List<CaseStudy>,
    onRefreshPortfolio: () -> Unit,
    testimonials: List<Testimonial>,
    onRefreshTestimonials: () -> Unit,
    onSubmitReview: suspend (
        accessToken: String,
        userId: String,
        reviewerName: String,
        reviewerEmail: String,
        engagementContext: String,
        rating: Int,
        quote: String
    ) -> Result<Unit>,
    promoBanners: List<PromoBanner>,
    onRefreshBanners: () -> Unit,
    contactDetails: ContactDetails,
    onRefreshContactDetails: () -> Unit,
    shouldShowAutoPopup: (String) -> Boolean,
    onAutoPopupShown: (String) -> Unit,
    onSignOut: () -> Unit,
    mandatesViewModel: MandatesViewModel?
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pushTokenManager = remember(context) {
        PushTokenManager(firestoreOrNull(context))
    }

    LaunchedEffect(currentUser?.uid, supabaseCandidateSession?.userId) {
        if (currentUser != null || supabaseCandidateSession != null) {
            pushTokenManager.registerAndroidToken(
                firebaseUid = currentUser?.uid,
                supabaseUserId = supabaseCandidateSession?.userId,
                supabaseAccessToken = supabaseCandidateSession?.accessToken,
                pushEnabled = true,
                emailEnabled = false
            )
        }
    }

    var consultationSheetType by remember { mutableStateOf<ConsultationServiceType?>(null) }
    var applyingRole by remember { mutableStateOf<HospitalityRole?>(null) }

    val feedbackMessage = mandatesViewModel?.feedbackMessage?.collectAsStateWithLifecycle()?.value
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            snackbarHostState.showSnackbar(feedbackMessage)
            mandatesViewModel.clearFeedback()
        }
    }

    val savedRolesState = mandatesViewModel?.savedRolesState?.collectAsStateWithLifecycle()?.value
    val applicationsState = mandatesViewModel?.applicationsState?.collectAsStateWithLifecycle()?.value
    val clientProfileState = mandatesViewModel?.clientProfileState?.collectAsStateWithLifecycle()?.value
    val candidateProfileState = mandatesViewModel?.candidateProfileState?.collectAsStateWithLifecycle()?.value
    val latestConfirmation = mandatesViewModel?.latestEnquiryConfirmation?.collectAsStateWithLifecycle()?.value
    val isMandateSubmitting = mandatesViewModel?.isSubmitting?.collectAsStateWithLifecycle()?.value ?: false
    var isSupabaseSubmitting by remember { mutableStateOf(false) }
    var supabaseEnquiryConfirmation by remember { mutableStateOf<EnquiryConfirmation?>(null) }
    val isSubmitting = isMandateSubmitting || isSupabaseSubmitting

    val supabaseSavedRoles by supabaseTalentClient.mySavedRoles.collectAsStateWithLifecycle()
    val supabaseApplications by supabaseTalentClient.myApplications.collectAsStateWithLifecycle()

    val clientProfile = (clientProfileState as? UiState.Success)?.data
    val candidateProfile = (candidateProfileState as? UiState.Success)?.data
    var googleMobilePromptDismissed by remember(currentUser?.uid) { mutableStateOf(false) }
    var isSavingGoogleMobile by remember { mutableStateOf(false) }
    var googleMobileError by remember { mutableStateOf<String?>(null) }

    val existingGooglePhone = remember(clientProfile?.mobileNumber) {
        clientProfile?.mobileNumber?.let { SupabaseAuthValidator.normalizePhoneE164(it) }
    }

    val shouldPromptGoogleMobile = remember(
        currentUser?.uid,
        supabaseSession?.userId,
        clientProfileState,
        existingGooglePhone,
        googleMobilePromptDismissed
    ) {
        currentUser != null &&
            supabaseSession == null &&
            existingGooglePhone == null &&
            clientProfileState !is UiState.Loading &&
            !googleMobilePromptDismissed
    }

    val savedRoleIds: Set<String> = remember(savedRolesState, supabaseSavedRoles) {
        val firestoreIds = (savedRolesState as? UiState.Success)?.data?.map { it.roleId }?.toSet() ?: emptySet()
        val supabaseIds = supabaseSavedRoles.map { it.roleId }.toSet()
        firestoreIds + supabaseIds
    }
    val appliedRoleIds: Set<String> = remember(applicationsState, supabaseApplications) {
        val firestoreIds = (applicationsState as? UiState.Success)?.data
            ?.filter { it.status != "WITHDRAWN" }
            ?.map { it.roleId }
            ?.toSet() ?: emptySet()
        val supabaseIds = supabaseApplications
            .filter { it.status != "WITHDRAWN" }
            .map { it.roleId }
            .toSet()
        firestoreIds + supabaseIds
    }

    fun navigateToRoute(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isClientSignedIn = currentUser != null || supabaseSession != null || supabaseCandidateSession != null
    val userInitial = remember(clientProfile, candidateProfile, supabaseSession, supabaseCandidateSession, currentUser) {
        val source = clientProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: candidateProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: supabaseSession?.metadata?.fullName?.takeIf { it.isNotBlank() }
            ?: supabaseCandidateSession?.profile?.fullName?.takeIf { it.isNotBlank() }
            ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: supabaseSession?.email?.takeIf { it.isNotBlank() }
            ?: supabaseCandidateSession?.email?.takeIf { it.isNotBlank() }
            ?: currentUser?.email
        source?.trim()?.take(2)?.uppercase()
    }

    val drawerDirectory = remember(services) { buildDrawerDirectory(services) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MilaDrawerSheetContent(
                entries = drawerDirectory,
                currentRoute = currentRoute,
                onSelectRoute = { route ->
                    scope.launch { drawerState.close() }
                    navigateToRoute(route)
                },
                onSignOut = {
                    scope.launch { drawerState.close() }
                    onSignOut()
                }
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MilaCharcoal,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MilaCharcoalElevated,
                            contentColor = MilaIvory,
                            actionColor = MilaChampagneGold
                        )
                    }
                },
                topBar = {
                    MilaBrandHeader(
                        isSignedIn = isClientSignedIn,
                        userInitial = userInitial,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onAccountClick = {
                            if (supabaseCandidateSession != null && supabaseSession == null) {
                                navigateToRoute(ROUTE_JOB_SEEKER_LOGIN)
                            } else {
                                navigateToRoute(ROUTE_CLIENT_LOGIN)
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MilaCharcoalDeep,
                            tonalElevation = 0.dp
                        ) {
                            primaryTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToRoute(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MilaChampagneGold,
                                        selectedTextColor = MilaChampagneGold,
                                        indicatorColor = MilaCharcoalElevated,
                                        unselectedIconColor = MilaWarmStone,
                                        unselectedTextColor = MilaWarmStone
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MilaCharcoalDeep,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            primaryTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                val label = stringResource(tab.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToRoute(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MilaChampagneGold,
                                        selectedTextColor = MilaChampagneGold,
                                        indicatorColor = MilaCharcoalElevated,
                                        unselectedIconColor = MilaWarmStone,
                                        unselectedTextColor = MilaWarmStone
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_HOME,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(ROUTE_HOME) {
                                var isSubmittingReview by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    onRefreshServices()
                                    onRefreshPortfolio()
                                    onRefreshBanners()
                                    onRefreshTestimonials()
                                    onRefreshContactDetails()
                                }
                                val reviewerDefaultName = clientProfile?.fullName?.takeIf { it.isNotBlank() }
                                    ?: supabaseSession?.metadata?.fullName?.takeIf { it.isNotBlank() && !it.equals("MILA Guest", ignoreCase = true) }
                                    ?: currentUser?.displayName.orEmpty()
                                AtelierScreen(
                                    services = services,
                                    openRoles = vacancies,
                                    portfolioItems = portfolio,
                                    testimonials = testimonials,
                                    promoBanners = promoBanners,
                                    shouldShowAutoPopup = shouldShowAutoPopup,
                                    onAutoPopupShown = onAutoPopupShown,
                                    canWriteReview = supabaseSession != null,
                                    defaultReviewerName = reviewerDefaultName,
                                    isSubmittingReview = isSubmittingReview,
                                    onSubmitReview = { name, contextText, rating, quote, onSuccess ->
                                        val activeSession = supabaseSession
                                        if (activeSession == null) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Please sign in with Email OTP to submit a review.")
                                            }
                                        } else {
                                            scope.launch {
                                                isSubmittingReview = true
                                                val res = onSubmitReview(
                                                    activeSession.accessToken,
                                                    activeSession.userId,
                                                    name,
                                                    activeSession.email,
                                                    contextText,
                                                    rating,
                                                    quote
                                                )
                                                isSubmittingReview = false
                                                res.fold(
                                                    onSuccess = {
                                                        onSuccess()
                                                        snackbarHostState.showSnackbar("Thank you! Your review has been submitted for MILA team approval.")
                                                    },
                                                    onFailure = { err ->
                                                        snackbarHostState.showSnackbar(err.localizedMessage ?: "Could not publish review right now.")
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    contactDetails = contactDetails,
                                    onOpenServiceDetail = { service ->
                                        navigateToRoute("$ROUTE_SERVICE_DETAIL_BASE/${service.slug}")
                                    },
                                    onOpenConsultation = { serviceType ->
                                        consultationSheetType = serviceType
                                    },
                                    onNavigateRoute = { route -> navigateToRoute(route) }
                                )
                            }
                            composable(ROUTE_ABOUT) {
                                AboutUsScreen(
                                    onContactUs = { navigateToRoute(ROUTE_CONTACT) },
                                    onViewExpertise = { navigateToRoute(ROUTE_EXPERTISE) }
                                )
                            }
                            composable(ROUTE_SERVICES) {
                                LaunchedEffect(Unit) {
                                    onRefreshServices()
                                }
                                AdvisoryScreen(
                                    services = services,
                                    onOpenServiceDetail = { service ->
                                        navigateToRoute("$ROUTE_SERVICE_DETAIL_BASE/${service.slug}")
                                    },
                                    onRequestMandate = { serviceType ->
                                        consultationSheetType = serviceType
                                    }
                                )
                            }
                            composable(ROUTE_SERVICE_DETAIL) { backStackEntry ->
                                LaunchedEffect(Unit) {
                                    onRefreshServices()
                                }
                                val slug = backStackEntry.arguments?.getString("serviceSlug").orEmpty()
                                val selectedService = MilaCatalog.serviceBySlug(slug, services)
                                ServiceDetailScreen(
                                    service = selectedService,
                                    allServices = services,
                                    onSelectServicePage = { target ->
                                        navigateToRoute("$ROUTE_SERVICE_DETAIL_BASE/${target.slug}")
                                    },
                                    onEnquireNow = { serviceType ->
                                        consultationSheetType = serviceType
                                    },
                                    onBackToCatalogue = { navigateToRoute(ROUTE_SERVICES) }
                                )
                            }
                            composable(ROUTE_INDUSTRIES) {
                                IndustriesWeServeScreen(
                                    onRequestMandate = { serviceType ->
                                        consultationSheetType = serviceType
                                    },
                                    onViewPortfolio = { navigateToRoute(ROUTE_PORTFOLIO) }
                                )
                            }
                            composable(ROUTE_EXPERTISE) {
                                OurExpertiseScreen(
                                    onRequestMandate = { serviceType ->
                                        consultationSheetType = serviceType
                                    }
                                )
                            }
                            composable(ROUTE_PORTFOLIO) {
                                LaunchedEffect(Unit) {
                                    onRefreshPortfolio()
                                }
                                OurPortfolioScreen(
                                    caseStudies = portfolio,
                                    onCommissionSimilar = { serviceType ->
                                        consultationSheetType = serviceType
                                    }
                                )
                            }
                            composable(ROUTE_CAREERS) {
                                LaunchedEffect(Unit) {
                                    onRefreshVacancies()
                                }
                                TalentScreen(
                                    savedRoleIds = savedRoleIds,
                                    appliedRoleIds = appliedRoleIds,
                                    onToggleSaveRole = { role, currentlySaved ->
                                        when {
                                            mandatesViewModel != null -> {
                                                mandatesViewModel.toggleSavedRole(role, currentlySaved)
                                                if (supabaseCandidateSession != null) {
                                                    scope.launch { supabaseTalentClient.toggleSavedJob(role, currentlySaved) }
                                                }
                                            }
                                            supabaseCandidateSession != null || supabaseSession != null -> {
                                                scope.launch {
                                                    if (supabaseCandidateSession == null && supabaseSession != null) {
                                                        supabaseTalentClient.syncFromAuthSession(supabaseSession)
                                                    }
                                                    supabaseTalentClient.toggleSavedJob(role, currentlySaved).fold(
                                                        onSuccess = {
                                                            snackbarHostState.showSnackbar(
                                                                if (currentlySaved) "Removed from saved roles." else "Saved ${role.title}."
                                                            )
                                                        },
                                                        onFailure = { err ->
                                                            snackbarHostState.showSnackbar(err.localizedMessage ?: "Unable to update saved role.")
                                                        }
                                                    )
                                                }
                                            }
                                            else -> {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Sign in at Job Seeker Login to shortlist roles.")
                                                }
                                                navigateToRoute(ROUTE_JOB_SEEKER_LOGIN)
                                            }
                                        }
                                    },
                                    onApplyRole = { role -> applyingRole = role },
                                    onHireTalentMandate = {
                                        consultationSheetType = ConsultationServiceType.RECRUITMENT_TALENT_ACQUISITION
                                    },
                                    onOpenJobSeekerPortal = { navigateToRoute(ROUTE_JOB_SEEKER_LOGIN) },
                                    jobAlertsEnabled = (candidateProfile ?: supabaseCandidateSession?.profile)?.jobAlertsEnabled == true,
                                    roles = vacancies,
                                    isRefreshing = isRefreshingVacancies,
                                    onRefreshRoles = onRefreshVacancies
                                )
                            }
                            composable(ROUTE_CONTACT) {
                                LaunchedEffect(Unit) {
                                    onRefreshContactDetails()
                                }
                                ContactUsScreen(contactDetails = contactDetails)
                            }
                            composable(ROUTE_CLIENT_LOGIN) {
                                ClientLoginScreen(
                                    currentUser = currentUser,
                                    supabaseSession = supabaseSession,
                                    supabaseAuthClient = supabaseAuthClient,
                                    viewModel = mandatesViewModel,
                                    onAuthSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Welcome to MILA Hospitality. Explore our consultancy services.")
                                        }
                                        navigateToRoute(ROUTE_HOME)
                                    },
                                    onAuthError = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    onNotify = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    onExploreServices = { navigateToRoute(ROUTE_SERVICES) },
                                    onBrowseHome = { navigateToRoute(ROUTE_HOME) },
                                    onCommissionConsultation = { serviceType ->
                                        consultationSheetType = serviceType
                                    },
                                    onGoToJobSeekerPortal = { navigateToRoute(ROUTE_JOB_SEEKER_LOGIN) },
                                    onSignOut = onSignOut
                                )
                            }
                            composable(ROUTE_JOB_SEEKER_LOGIN) {
                                JobSeekerLoginScreen(
                                    currentUser = currentUser,
                                    supabaseCandidateSession = supabaseCandidateSession,
                                    supabaseTalentClient = supabaseTalentClient,
                                    viewModel = mandatesViewModel,
                                    onAuthSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Welcome to the MILA Talent Job Seeker Portal.")
                                        }
                                    },
                                    onAuthError = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    onNotify = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    onExploreCareers = { navigateToRoute(ROUTE_CAREERS) },
                                    onApplyRole = { role -> applyingRole = role },
                                    onGoToClientPortal = { navigateToRoute(ROUTE_CLIENT_LOGIN) },
                                    onSyncNotificationChannels = { pushEnabled, emailEnabled ->
                                        scope.launch {
                                            pushTokenManager.registerAndroidToken(
                                                firebaseUid = currentUser?.uid,
                                                supabaseUserId = supabaseCandidateSession?.userId,
                                                supabaseAccessToken = supabaseCandidateSession?.accessToken,
                                                pushEnabled = pushEnabled,
                                                emailEnabled = emailEnabled
                                            )
                                            snackbarHostState.showSnackbar(
                                                "Alert channels saved (Push: ${if (pushEnabled) "On" else "Off"}, Email: ${if (emailEnabled) "On" else "Off"})."
                                            )
                                        }
                                    },
                                    onSignOut = onSignOut
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    consultationSheetType?.let { serviceType ->
        ModalBottomSheet(
            onDismissRequest = { consultationSheetType = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MilaCharcoalSurface,
            contentColor = MilaIvory
        ) {
            ConsultationRequestSheet(
                currentUser = currentUser,
                initialServiceType = serviceType,
                isSubmitting = isSubmitting,
                onAuthSuccess = {
                    scope.launch { snackbarHostState.showSnackbar("Signed in. Complete your business requirements below.") }
                },
                onAuthError = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                },
                onSubmit = { inquiry ->
                    if (mandatesViewModel != null) {
                        mandatesViewModel.submitInquiry(inquiry) {
                            consultationSheetType = null
                        }
                        if (supabaseSession != null) {
                            scope.launch { supabaseAuthClient.submitServiceEnquiry(inquiry) }
                        }
                    } else if (supabaseSession != null) {
                        scope.launch {
                            isSupabaseSubmitting = true
                            val res = supabaseAuthClient.submitServiceEnquiry(inquiry)
                            isSupabaseSubmitting = false
                            res.fold(
                                onSuccess = { confirmation ->
                                    consultationSheetType = null
                                    supabaseEnquiryConfirmation = confirmation
                                },
                                onFailure = { err ->
                                    snackbarHostState.showSnackbar(err.localizedMessage ?: "Failed to submit enquiry.")
                                }
                            )
                        }
                    }
                },
                clientProfile = clientProfile,
                supabaseSession = supabaseSession
            )
        }
    }

    val activeConfirmation = latestConfirmation ?: supabaseEnquiryConfirmation
    activeConfirmation?.let { confirmation ->
        EnquiryConfirmationDialog(
            confirmation = confirmation,
            onExploreServices = { navigateToRoute(ROUTE_SERVICES) },
            onDismiss = {
                mandatesViewModel?.dismissEnquiryConfirmation()
                supabaseEnquiryConfirmation = null
            }
        )
    }

    applyingRole?.let { role ->
        ModalBottomSheet(
            onDismissRequest = { applyingRole = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MilaCharcoalSurface,
            contentColor = MilaIvory
        ) {
            JobApplicationSheet(
                role = role,
                currentUser = currentUser,
                isSubmitting = isSubmitting,
                onAuthSuccess = {
                    scope.launch { snackbarHostState.showSnackbar("Signed in. You can now submit your application.") }
                },
                onAuthError = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                },
                onSubmit = { application ->
                    if (mandatesViewModel != null) {
                        if (supabaseCandidateSession != null) {
                            scope.launch { supabaseTalentClient.syncJobApplication(application) }
                        }
                        mandatesViewModel.submitApplication(application) {
                            applyingRole = null
                            navigateToRoute(ROUTE_JOB_SEEKER_LOGIN)
                        }
                    } else if (supabaseCandidateSession != null || supabaseSession != null) {
                        scope.launch {
                            isSupabaseSubmitting = true
                            if (supabaseCandidateSession == null && supabaseSession != null) {
                                supabaseTalentClient.syncFromAuthSession(supabaseSession)
                            }
                            val res = supabaseTalentClient.syncJobApplication(application)
                            isSupabaseSubmitting = false
                            res.fold(
                                onSuccess = {
                                    applyingRole = null
                                    snackbarHostState.showSnackbar("Application submitted to MILA Talent.")
                                    navigateToRoute(ROUTE_JOB_SEEKER_LOGIN)
                                },
                                onFailure = { err ->
                                    snackbarHostState.showSnackbar(err.localizedMessage ?: "Failed to submit application.")
                                }
                            )
                        }
                    }
                },
                candidateProfile = candidateProfile,
                supabaseCandidateSession = supabaseCandidateSession,
                supabaseSession = supabaseSession,
                supabaseTalentClient = supabaseTalentClient,
                onCvUploaded = { meta ->
                    val baseProfile = candidateProfile ?: supabaseCandidateSession?.profile
                    if (baseProfile != null) {
                        val updated = baseProfile.copy(
                            cvFileName = meta.fileName,
                            cvMimeType = meta.mimeType,
                            cvFileSizeBytes = meta.fileSizeBytes,
                            cvStoragePath = meta.storagePath,
                            cvUploadedAtMillis = meta.uploadedAtMillis
                        )
                        if (mandatesViewModel != null && currentUser != null) {
                            mandatesViewModel.attachCvToProfile(baseProfile, meta)
                        }
                        if (supabaseCandidateSession != null) {
                            scope.launch { supabaseTalentClient.syncCandidateProfile(updated) }
                        }
                    }
                    scope.launch { snackbarHostState.showSnackbar("CV '${meta.fileName}' attached.") }
                }
            )
        }
    }

    if (shouldPromptGoogleMobile && currentUser != null) {
        val initialGoogleName = remember(currentUser.uid, clientProfile?.fullName, currentUser.displayName) {
            clientProfile?.fullName?.trim()?.takeIf { it.length >= 2 }
                ?: currentUser.displayName?.trim().orEmpty()
        }
        GoogleMobilePromptDialog(
            initialName = initialGoogleName,
            userEmail = currentUser.email.orEmpty(),
            isSaving = isSavingGoogleMobile,
            errorMessage = googleMobileError,
            onSave = { cleanName, normalizedMobile ->
                scope.launch {
                    isSavingGoogleMobile = true
                    googleMobileError = null
                    val baseProfile = clientProfile ?: ClientProfile(
                        fullName = cleanName,
                        email = currentUser.email.orEmpty(),
                        mobileNumber = normalizedMobile,
                        companyName = "MILA Hospitality Member",
                        designation = "Client",
                        city = "India"
                    )
                    mandatesViewModel?.saveClientProfile(
                        baseProfile.copy(
                            fullName = cleanName,
                            email = currentUser.email.orEmpty().ifBlank { baseProfile.email },
                            mobileNumber = normalizedMobile
                        )
                    ) {
                        isSavingGoogleMobile = false
                        googleMobilePromptDismissed = true
                        scope.launch {
                            snackbarHostState.showSnackbar("Welcome, $cleanName! Mobile number saved.")
                        }
                    }
                    if (mandatesViewModel == null) {
                        isSavingGoogleMobile = false
                        googleMobilePromptDismissed = true
                    }
                }
            },
            onSkip = { googleMobilePromptDismissed = true }
        )
    }
}
