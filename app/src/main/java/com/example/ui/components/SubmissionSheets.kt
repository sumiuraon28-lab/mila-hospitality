package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.SupabaseCandidateSession
import com.example.auth.SupabaseSession
import com.example.data.CandidateProfile
import com.example.data.ClientProfile
import com.example.data.ConsultationInquiry
import com.example.data.ConsultationServiceType
import com.example.data.HospitalityRole
import com.example.data.JobApplication
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalBorder
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaEmerald
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsultationRequestSheet(
    currentUser: FirebaseUser?,
    initialServiceType: ConsultationServiceType,
    isSubmitting: Boolean,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onSubmit: (ConsultationInquiry) -> Unit,
    clientProfile: ClientProfile? = null,
    supabaseSession: SupabaseSession? = null,
    scrollable: Boolean = true
) {
    var selectedService by remember(initialServiceType) { mutableStateOf(initialServiceType) }
    var propertyName by remember(clientProfile, supabaseSession) {
        mutableStateOf(
            clientProfile?.companyName?.takeIf { it.isNotBlank() }
                ?: supabaseSession?.metadata?.companyName.orEmpty()
        )
    }
    var city by remember(clientProfile, supabaseSession) {
        mutableStateOf(
            clientProfile?.city?.takeIf { it.isNotBlank() }
                ?: supabaseSession?.metadata?.city.orEmpty()
        )
    }
    var scaleSummary by remember { mutableStateOf("") }
    var contactName by remember(currentUser, clientProfile, supabaseSession) {
        mutableStateOf(
            clientProfile?.fullName?.takeIf { it.isNotBlank() }
                ?: supabaseSession?.metadata?.fullName?.takeIf { it.isNotBlank() }
                ?: currentUser?.displayName.orEmpty()
        )
    }
    var contactEmail by remember(currentUser, clientProfile, supabaseSession) {
        mutableStateOf(
            clientProfile?.email?.takeIf { it.isNotBlank() }
                ?: supabaseSession?.email?.takeIf { it.isNotBlank() }
                ?: currentUser?.email.orEmpty()
        )
    }
    var contactPhone by remember(clientProfile, supabaseSession) {
        mutableStateOf(
            clientProfile?.mobileNumber?.takeIf { it.isNotBlank() }
                ?: supabaseSession?.metadata?.mobileNumber.orEmpty()
        )
    }
    var briefNotes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val baseModifier = if (scrollable) {
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    } else {
        Modifier.fillMaxWidth()
    }

    Column(
        modifier = baseModifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SERVICE ENQUIRY FORM",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MilaChampagneGold,
                letterSpacing = 1.6.sp
            )
        )
        Text(
            text = "Submit Your Business Requirements",
            style = MaterialTheme.typography.headlineMedium.copy(color = MilaIvory)
        )
        Text(
            text = "Choose any of the eight MILA service categories below and share your hospitality venture requirements for a partner response.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted)
        )

        if (currentUser == null && supabaseSession == null) {
            AuthGateCard(
                title = "Sign In to Submit Your Enquiry",
                subtitle = "Sign in with Google or your MILA Client account to select your service category and submit your business requirements securely.",
                onAuthSuccess = onAuthSuccess,
                onAuthError = onAuthError
            )
        } else {
            Text(
                text = "SELECT SERVICE REQUIRED (8 CATEGORIES)",
                style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ConsultationServiceType.entries.forEachIndexed { index, type ->
                    val selected = selectedService == type
                    val num = (index + 1).toString().padStart(2, '0')
                    FilterChip(
                        selected = selected,
                        onClick = { selectedService = type },
                        label = { Text("$num. ${type.label}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MilaChampagneGold,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MilaCharcoalDeep,
                            labelColor = MilaIvoryMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MilaCharcoalBorder,
                            selectedBorderColor = MilaChampagneGold
                        ),
                        modifier = Modifier.testTag("enquiry_service_${type.name}")
                    )
                }
            }

            MilaFormField(
                value = propertyName,
                onValueChange = { if (it.length <= 120) propertyName = it },
                label = "Property / Business / Venture Name *",
                placeholder = "e.g., Heritage Resort or Café Concept",
                testTag = "input_property_name"
            )
            MilaFormField(
                value = city,
                onValueChange = { if (it.length <= 80) city = it },
                label = "City / State *",
                placeholder = "e.g., Mumbai, Goa, Udaipur",
                testTag = "input_city"
            )
            MilaFormField(
                value = scaleSummary,
                onValueChange = { if (it.length <= 80) scaleSummary = it },
                label = "Project Scale (Keys, Covers, or Team Size) *",
                placeholder = "e.g., 85 Keys or 70 Covers",
                testTag = "input_scale"
            )
            HorizontalDivider(color = MilaGoldSubtle)
            MilaFormField(
                value = contactName,
                onValueChange = { if (it.length <= 100) contactName = it },
                label = "Stakeholder Name *",
                placeholder = "Full Name",
                testTag = "input_contact_name"
            )
            MilaFormField(
                value = contactEmail,
                onValueChange = { if (it.length <= 120) contactEmail = it },
                label = "Business Email *",
                placeholder = "name@company.in",
                keyboardType = KeyboardType.Email,
                testTag = "input_contact_email"
            )
            MilaFormField(
                value = contactPhone,
                onValueChange = { if (it.length <= 25) contactPhone = it },
                label = "Phone Number *",
                placeholder = "+91 98200 00000",
                keyboardType = KeyboardType.Phone,
                testTag = "input_contact_phone"
            )
            MilaFormField(
                value = briefNotes,
                onValueChange = { if (it.length <= 1000) briefNotes = it },
                label = "Business Requirements & Target Timeline *",
                placeholder = "Describe your project stage, operational goals, and launch timeline...",
                minLines = 3,
                testTag = "input_brief_notes"
            )

            validationError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            GoldFilledButton(
                text = if (isSubmitting) "Submitting Enquiry..." else "Submit Business Enquiry",
                enabled = !isSubmitting,
                testTag = "submit_mandate_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    validationError = validateInquiry(
                        propertyName, city, scaleSummary, contactName, contactEmail, contactPhone, briefNotes
                    )
                    if (validationError == null) {
                        onSubmit(
                            ConsultationInquiry(
                                propertyName = propertyName.trim(),
                                city = city.trim(),
                                serviceType = selectedService.name,
                                scaleSummary = scaleSummary.trim(),
                                contactName = contactName.trim(),
                                contactEmail = contactEmail.trim(),
                                contactPhone = contactPhone.trim(),
                                briefNotes = briefNotes.trim().takeIf { it.isNotEmpty() }
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun JobApplicationSheet(
    role: HospitalityRole,
    currentUser: FirebaseUser?,
    isSubmitting: Boolean,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onSubmit: (JobApplication) -> Unit,
    candidateProfile: CandidateProfile? = null,
    supabaseCandidateSession: SupabaseCandidateSession? = null,
    scrollable: Boolean = true
) {
    val activeProfile = candidateProfile ?: supabaseCandidateSession?.profile
    var candidateName by remember(currentUser, activeProfile) {
        mutableStateOf(
            activeProfile?.fullName?.takeIf { it.isNotBlank() }
                ?: currentUser?.displayName.orEmpty()
        )
    }
    var candidateEmail by remember(currentUser, activeProfile) {
        mutableStateOf(
            activeProfile?.email?.takeIf { it.isNotBlank() }
                ?: currentUser?.email.orEmpty()
        )
    }
    var candidatePhone by remember(activeProfile) {
        mutableStateOf(activeProfile?.mobileNumber.orEmpty())
    }
    var yearsExpText by remember(activeProfile) {
        val yrs = activeProfile?.totalExperienceYears ?: 0L
        mutableStateOf(if (yrs > 0L) yrs.toString() else "")
    }
    var currentProperty by remember(activeProfile) {
        mutableStateOf(activeProfile?.currentEmployer.orEmpty())
    }
    var coverNote by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val attachedCvName = activeProfile?.cvFileName?.takeIf { it.isNotBlank() }

    val baseModifier = if (scrollable) {
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    } else {
        Modifier.fillMaxWidth()
    }

    Column(
        modifier = baseModifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "EXECUTIVE CANDIDATE APPLICATION",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MilaChampagneGold,
                letterSpacing = 1.6.sp
            )
        )
        Text(
            text = role.title,
            style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
        )
        Text(
            text = "${role.location} • ${role.compensation}",
            style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold)
        )

        if (currentUser == null && supabaseCandidateSession == null) {
            AuthGateCard(
                title = "Sign In to Submit Your Candidacy",
                subtitle = "MILA executive applications are linked to your verified account for real-time status tracking and private CV attachment.",
                onAuthSuccess = onAuthSuccess,
                onAuthError = onAuthError
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalDeep),
                border = BorderStroke(1.dp, if (attachedCvName != null) MilaEmerald else MilaCharcoalBorder),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = if (attachedCvName != null) MilaEmerald else MilaWarmStone,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (attachedCvName != null) {
                            "Private CV attached from vault: $attachedCvName (no public link)"
                        } else {
                            "No private CV uploaded yet — attach PDF, DOC, or DOCX in Job Seeker Portal -> Profile & CV."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (attachedCvName != null) MilaIvory else MilaWarmStone
                        )
                    )
                }
            }

            MilaFormField(
                value = candidateName,
                onValueChange = { if (it.length <= 100) candidateName = it },
                label = "Full Name *",
                placeholder = "Candidate Name",
                testTag = "apply_input_name"
            )
            MilaFormField(
                value = candidateEmail,
                onValueChange = { if (it.length <= 120) candidateEmail = it },
                label = "Email Address *",
                placeholder = "candidate@hospitality.in",
                keyboardType = KeyboardType.Email,
                testTag = "apply_input_email"
            )
            MilaFormField(
                value = candidatePhone,
                onValueChange = { if (it.length <= 25) candidatePhone = it },
                label = "Phone Number *",
                placeholder = "+91 98111 00000",
                keyboardType = KeyboardType.Phone,
                testTag = "apply_input_phone"
            )
            MilaFormField(
                value = yearsExpText,
                onValueChange = { input ->
                    if (input.length <= 2 && input.all { it.isDigit() }) {
                        yearsExpText = input
                    }
                },
                label = "Years of Hospitality Experience *",
                placeholder = "e.g., 10",
                keyboardType = KeyboardType.Number,
                testTag = "apply_input_experience"
            )
            MilaFormField(
                value = currentProperty,
                onValueChange = { if (it.length <= 120) currentProperty = it },
                label = "Current / Most Recent Property or Group *",
                placeholder = "e.g., Luxury Hotel or Restaurant Group",
                testTag = "apply_input_property"
            )
            MilaFormField(
                value = coverNote,
                onValueChange = { if (it.length <= 1000) coverNote = it },
                label = "Pre-Opening / Leadership Highlights (Optional)",
                placeholder = "Key P&L achievements, openings led, or milestones...",
                minLines = 3,
                testTag = "apply_input_note"
            )

            validationError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            GoldFilledButton(
                text = if (isSubmitting) "Submitting Application..." else "Submit Application",
                enabled = !isSubmitting,
                testTag = "submit_application_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val years = yearsExpText.toLongOrNull()
                    validationError = validateApplication(
                        candidateName, candidateEmail, candidatePhone, years, currentProperty
                    )
                    if (validationError == null && years != null) {
                        onSubmit(
                            JobApplication(
                                roleId = role.id,
                                roleTitle = role.title,
                                location = role.location,
                                candidateName = candidateName.trim(),
                                candidateEmail = candidateEmail.trim(),
                                candidatePhone = candidatePhone.trim(),
                                yearsExperience = years,
                                currentProperty = currentProperty.trim(),
                                coverNote = coverNote.trim().takeIf { it.isNotEmpty() },
                                cvFileName = attachedCvName
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun MilaFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    testTag: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MilaWarmStone) },
        placeholder = { Text(placeholder, color = MilaWarmStone) },
        minLines = minLines,
        maxLines = if (minLines > 1) 6 else 1,
        singleLine = minLines == 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MilaChampagneGold,
            unfocusedBorderColor = MilaCharcoalBorder,
            focusedContainerColor = MilaCharcoalDeep,
            unfocusedContainerColor = MilaCharcoalDeep,
            focusedTextColor = MilaIvory,
            unfocusedTextColor = MilaIvory
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

private fun validateInquiry(
    propertyName: String,
    city: String,
    scaleSummary: String,
    contactName: String,
    contactEmail: String,
    contactPhone: String,
    briefNotes: String
): String? {
    return when {
        propertyName.trim().length < 2 -> "Please enter your property or business name (min 2 characters)."
        city.trim().length < 2 -> "Please enter the property city."
        scaleSummary.trim().isEmpty() -> "Please specify project scale (e.g., 80 Keys or 65 Covers)."
        contactName.trim().length < 2 -> "Please enter a contact name."
        !contactEmail.contains("@") || contactEmail.trim().length < 5 -> "Please enter a valid business email."
        contactPhone.trim().length < 7 -> "Please enter a valid contact phone number."
        briefNotes.trim().length < 5 -> "Please share your business requirements (min 5 characters)."
        else -> null
    }
}

private fun validateApplication(
    name: String,
    email: String,
    phone: String,
    years: Long?,
    currentProperty: String
): String? {
    return when {
        name.trim().length < 2 -> "Please enter your full name."
        !email.contains("@") || email.trim().length < 5 -> "Please enter a valid email address."
        phone.trim().length < 7 -> "Please enter a valid phone number."
        years == null || years !in 0L..60L -> "Please enter valid years of experience (0–60)."
        currentProperty.trim().length < 2 -> "Please enter your current or most recent property."
        else -> null
    }
}
