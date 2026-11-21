package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.ui.theme.tokens

/**
 * The app's one bottom sheet: radius 28 on the sheet colour, a visible 36 × 4 handle, and it opens to
 * its full content height instead of stopping half way through a row. The task sheet and the date
 * sheet both use it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    state: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.tokens
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = tokens.sheet,
        contentColor = tokens.ink,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = tokens.scrimAlpha),
        tonalElevation = 0.dp,
        dragHandle = { SheetHandle() },
        content = content,
    )
}

/** The sheet's handle: 36 × 4 in ink-3, 10 dp from the top. */
@Composable
fun SheetHandle() {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(width = 36.dp, height = 4.dp)
                .background(MaterialTheme.tokens.ink3, RoundedCornerShape(2.dp)),
        )
    }
}
