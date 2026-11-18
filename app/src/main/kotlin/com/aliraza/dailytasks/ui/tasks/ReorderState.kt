package com.aliraza.dailytasks.ui.tasks

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Drag to reorder inside a LazyColumn. The dragged row follows the finger; when its centre crosses a
 * neighbour the two swap in [order], and the offset is corrected so the row stays under the finger.
 */
@Stable
class ReorderState(private val listState: LazyListState) {
    var order by mutableStateOf(emptyList<String>())
        private set
    var dragging by mutableStateOf<String?>(null)
        private set
    var offset by mutableFloatStateOf(0f)
        private set
    private var before = emptyList<String>()

    /** Takes the latest order from the database, unless a drag is under way. */
    fun sync(keys: List<String>) {
        if (dragging == null) order = keys
    }

    fun start(key: String) {
        before = order
        dragging = key
        offset = 0f
    }

    fun drag(dy: Float) {
        offset += dy
        val key = dragging ?: return
        val visible = listState.layoutInfo.visibleItemsInfo
        val current = visible.firstOrNull { it.key == key } ?: return
        val centre = (current.offset + offset + current.size / 2f).toInt()
        val target = visible.firstOrNull { info ->
            info.key != key && info.key in order && centre in info.offset until info.offset + info.size
        } ?: return
        val from = order.indexOf(key)
        val to = order.indexOf(target.key)
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        offset += current.offset - target.offset
    }

    /** Ends the drag and returns the order before and after it. */
    fun end(): Pair<List<String>, List<String>> {
        dragging = null
        offset = 0f
        return before to order
    }
}

/** Makes the element a drag handle for the row [key]. */
fun Modifier.dragHandle(state: ReorderState, key: String, onDrop: (List<String>, List<String>) -> Unit): Modifier =
    pointerInput(key) {
        detectDragGestures(
            onDragStart = { state.start(key) },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount.y)
            },
            onDragEnd = { state.end().let { (before, after) -> onDrop(before, after) } },
            onDragCancel = { state.end() },
        )
    }
