package com.expensetracker.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** A distinctive page introduction shared by the app's primary destinations. */
@Composable
fun PremiumPageIntro(
    eyebrow: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.primaryContainer,
        tonalElevation = 1.dp,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 18.dp)
                    .clip(CircleShape)
                    .background(colors.onPrimaryContainer.copy(alpha = 0.055f))
                    .padding(34.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 21.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onPrimaryContainer,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
        }
    }
}
