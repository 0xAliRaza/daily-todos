package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliraza.dailytodos.ui.theme.tokens

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

/**
 * The app's one snackbar look: 52 dp on the inverse neutral, radius 14, the message naming the task and
 * the action in the inverse accent. An optional [Notice.icon] sits at the start, a [Notice.detail] under.
 */
@Composable
fun NoticeCard(data: SnackbarData) {
    val visuals = data.visuals
    val notice = visuals as? Notice
    val tokens = MaterialTheme.tokens
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tokens.inverse,
        contentColor = tokens.onInverse,
        shadowElevation = 6.dp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp).widthIn(max = 560.dp),
    ) {
        Row(
            Modifier.heightIn(min = 52.dp).padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            notice?.icon?.let { Icon(it, null, tint = tokens.inverseAction, modifier = Modifier.size(20.dp)) }
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                notice?.detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.onInverse.copy(alpha = DETAIL_ALPHA),
                    )
                }
            }
            visuals.actionLabel?.let { label ->
                Box(
                    Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = data::performAction)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = tokens.inverseAction, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

private const val DETAIL_ALPHA = 0.75f
