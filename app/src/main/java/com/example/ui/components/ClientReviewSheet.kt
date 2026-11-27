package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone

@Composable
fun ClientReviewSheet(
    canSubmitDirectReview: Boolean,
    defaultReviewerName: String,
    isSubmitting: Boolean,
    onSubmit: (reviewerName: String, engagementContext: String, rating: Int, quote: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var reviewerName by remember(defaultReviewerName) { mutableStateOf(defaultReviewerName) }
    var engagementContext by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SHARE YOUR FEEDBACK",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MilaChampagneGold,
                letterSpacing = 1.6.sp
            )
        )
        Text(
            text = "Write a Client Review",
            style = MaterialTheme.typography.headlineMedium.copy(color = MilaIvory)
        )
        Text(
            text = "Share your experience working with MILA Hospitality Consultant. Reviews are verified by our team before appearing in the app.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted)
        )

        if (!canSubmitDirectReview) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalDeep),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Please sign in with Email OTP to publish a verified review directly to the MILA app.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MilaWarmStone),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Text(
                text = "YOUR RATING",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.2.sp
                )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (1..5).forEach { star ->
                    val filled = star <= rating
                    IconButton(
                        onClick = { rating = star },
                        modifier = Modifier.testTag("review_star_$star")
                    ) {
                        Icon(
                            imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = "Rate $star out of 5 stars",
                            tint = if (filled) MilaChampagneGold else MilaWarmStone,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Text(
                    text = "$rating / 5",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MilaIvory,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            MilaFormField(
                value = reviewerName,
                onValueChange = { if (it.length <= 120) reviewerName = it },
                label = "Your Name / Role *",
                placeholder = "e.g., Arjun Mehta • Managing Partner",
                testTag = "input_review_name"
            )
            MilaFormField(
                value = engagementContext,
                onValueChange = { if (it.length <= 160) engagementContext = it },
                label = "Property / Engagement Context (Optional)",
                placeholder = "e.g., Boutique Resort Pre-Opening • Goa",
                testTag = "input_review_context"
            )
            MilaFormField(
                value = quote,
                onValueChange = { if (it.length <= 1200) quote = it },
                label = "Your Review *",
                placeholder = "Share how MILA helped with your hospitality project or career...",
                minLines = 4,
                testTag = "input_review_quote"
            )

            validationError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            GoldFilledButton(
                text = if (isSubmitting) "Submitting Review..." else "Submit for Review",
                onClick = {
                    val cleanName = reviewerName.trim()
                    val cleanQuote = quote.trim()
                    when {
                        cleanName.length < 2 -> validationError = "Please enter your name or role."
                        cleanQuote.length < 10 -> validationError = "Please write at least 10 characters for your review."
                        else -> {
                            validationError = null
                            onSubmit(cleanName, engagementContext.trim(), rating, cleanQuote)
                        }
                    }
                },
                enabled = !isSubmitting,
                testTag = "submit_review_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
