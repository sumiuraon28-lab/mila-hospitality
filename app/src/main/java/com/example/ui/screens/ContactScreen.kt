package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ContactDetails
import com.example.data.MilaCatalog
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.SectionHeading
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaWarmStone

@Composable
fun ContactUsScreen(
    contactDetails: ContactDetails = ContactDetails(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val safeEmail = contactDetails.email.trim()
    val safePhone = contactDetails.phone.trim()

    val openDialer: () -> Unit = {
        val dialTarget = safePhone.filter { it.isDigit() || it == '+' }
        if (dialTarget.isNotBlank()) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialTarget")))
            }
        }
    }

    val openEmail: () -> Unit = {
        if (safeEmail.contains("@")) {
            runCatching {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:${Uri.encode(safeEmail)}")
                    putExtra(Intent.EXTRA_SUBJECT, "MILA Hospitality Consultancy Enquiry")
                }
                context.startActivity(intent)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            SectionHeading(
                eyebrow = "Contact Us",
                title = "Connect With MILA Partners",
                subtitle = "Connect with MILA Consultancy, MILA Talent, and MILA Academy & Expertise across India, South Asia, GCC, and international markets.",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "DIRECT PARTNER CONTACT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.5.sp
                        )
                    )

                    if (safePhone.isNotBlank()) {
                        Surface(
                            color = MilaCharcoalDeep,
                            border = BorderStroke(1.dp, MilaGoldSubtle),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = openDialer)
                                .testTag("contact_phone_row")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Phone,
                                    contentDescription = "Call MILA Hospitality",
                                    tint = MilaChampagneGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Phone",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone)
                                    )
                                    Text(
                                        text = safePhone,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MilaIvory,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (safeEmail.isNotBlank()) {
                        Surface(
                            color = MilaCharcoalDeep,
                            border = BorderStroke(1.dp, MilaGoldSubtle),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = openEmail)
                                .testTag("contact_email_row")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Email,
                                    contentDescription = "Email MILA Hospitality",
                                    tint = MilaChampagneGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Email",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone)
                                    )
                                    Text(
                                        text = safeEmail,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = MilaIvory,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (safePhone.isBlank() && safeEmail.isBlank()) {
                        Text(
                            text = "Direct phone and email details will be updated soon by the MILA partner desk.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MilaWarmStone)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (safePhone.isNotBlank()) {
                                GoldFilledButton(
                                    text = "Call Now",
                                    onClick = openDialer,
                                    testTag = "contact_call_button",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (safeEmail.isNotBlank()) {
                                GoldOutlineButton(
                                    text = "Email Us",
                                    onClick = openEmail,
                                    testTag = "contact_email_button",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalDeep),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "REGIONAL COVERAGE & ENQUIRY DESK",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MilaChampagneGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = contactDetails.responseWindow,
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaIvory)
                        )
                    }
                    if (contactDetails.deskNote.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = MilaChampagneGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = contactDetails.deskNote,
                                style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                            )
                        }
                    }
                    HorizontalDivider(color = MilaGoldSubtle)
                    MilaCatalog.serviceRegions.forEach { (region, detail) ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = MilaChampagneGold,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(16.dp)
                            )
                            Column {
                                Text(
                                    text = region,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MilaIvory,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
