package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * What the app's snackbar shows: a [message], an optional quieter [detail] line under it, an optional
 * [icon] in a round badge, and an optional action such as Undo.
 */
class Notice(
    override val message: String,
    val detail: String? = null,
    val icon: ImageVector? = null,
    override val actionLabel: String? = null,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
}

/** The app's one snackbar look: a floating rounded card on the opposite theme, icon, words, action. */
@Composable
fun NoticeCard(data: SnackbarData) {
    val visuals = data.visuals
    val notice = visuals as? Notice
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.inverseSurface,
        contentColor = scheme.inverseOnSurface,
        shadowElevation = 8.dp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).widthIn(max = 560.dp),
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            notice?.icon?.let { NoticeBadge(it) }
            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                Text(visuals.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                notice?.detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.inverseOnSurface.copy(alpha = DETAIL_ALPHA),
                    )
                }
            }
            visuals.actionLabel?.let { label ->
                TextButton(onClick = data::performAction) {
                    Text(label, color = scheme.inversePrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun NoticeBadge(icon: ImageVector) {
    val accent = MaterialTheme.colorScheme.inversePrimary
    Box(
        Modifier.size(36.dp).background(accent.copy(alpha = BADGE_ALPHA), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
    }
}

private const val DETAIL_ALPHA = 0.75f
private const val BADGE_ALPHA = 0.18f
