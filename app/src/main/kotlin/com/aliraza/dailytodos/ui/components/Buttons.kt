package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliraza.dailytodos.ui.theme.tokens

/** The colours of a button: its fill and what is drawn on it. */
data class ButtonColors(val container: Color, val content: Color)

/** The app's button colours, one per role, so screens never pick button colours by hand. */
object AppButtons {
    /** The one main action on a screen: solid accent. */
    @Composable
    fun primary() = with(MaterialTheme.tokens) { ButtonColors(accent, onAccent) }

    /** The main action on a break: solid break green. */
    @Composable
    fun rest() = with(MaterialTheme.tokens) { ButtonColors(rest, onRest) }

    /** A secondary action that still belongs to the accent: accent container. */
    @Composable
    fun tonal() = with(MaterialTheme.tokens) { ButtonColors(accentContainer, onAccentContainer) }

    /** A quiet secondary action: the neutral fill. */
    @Composable
    fun neutral() = with(MaterialTheme.tokens) { ButtonColors(fill, ink) }

    /** Deleting: the neutral fill with danger text. */
    @Composable
    fun danger() = with(MaterialTheme.tokens) { ButtonColors(fill, danger) }
}

/** A 48 dp pill button, 15 sp bold, with an optional leading [icon]. */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    colors: ButtonColors,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    Row(
        modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(colors.container.copy(alpha = colors.container.alpha * alpha))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        val ink = colors.content.copy(alpha = alpha)
        icon?.let {
            Icon(it, null, tint = ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = ink, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) =
    AppButton(text, onClick, AppButtons.primary(), modifier, icon)

@Composable
fun TonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) =
    AppButton(text, onClick, AppButtons.tonal(), modifier, icon)

private const val DISABLED_ALPHA = 0.38f
