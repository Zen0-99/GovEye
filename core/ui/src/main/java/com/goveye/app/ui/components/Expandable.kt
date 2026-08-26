package com.goveye.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

/**
 * Holder for expand/collapse state, usable across composables.
 *
 * Wraps a [MutableState] so the state can be backed by [rememberSaveable]
 * for configuration-change survival. Use [rememberExpandState] to create
 * an instance.
 */
class ExpandState internal constructor(private val state: MutableState<Boolean>) {
    var expanded: Boolean
        get() = state.value
        set(value) {
            state.value = value
        }
    fun toggle() {
        state.value = !state.value
    }
    fun expand() {
        state.value = true
    }
    fun collapse() {
        state.value = false
    }
}

/**
 * Creates an [ExpandState] that survives configuration changes.
 */
@Composable
fun rememberExpandState(initial: Boolean = false): ExpandState {
    val state = rememberSaveable { mutableStateOf(initial) }
    return remember(state) { ExpandState(state) }
}

/**
 * Shared expand/collapse animation wrapper.
 *
 * Wraps [content] in [AnimatedVisibility] with the app-standard
 * `expandVertically` / `shrinkVertically` transitions — pure height morph,
 * no fade, matching the convention used across the app.
 *
 * Use inside any card that shows extra detail when expanded:
 *
 * ```
 * ExpandableContent(state = state) {
 *     // extra detail rows
 * }
 * ```
 */
@Composable
fun ExpandableContent(state: ExpandState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = state.expanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * A clickable modifier that toggles [state] on tap, with a subtle ripple
 * matching the app convention (primary at 8% alpha).
 *
 * Apply to the card's Surface modifier so the whole card is tappable:
 *
 * ```
 * Surface(modifier = Modifier.expandable(state)) { ... }
 * ```
 */
@Composable
fun Modifier.expandable(state: ExpandState, enabled: Boolean = true): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        enabled = enabled,
        onClick = { state.toggle() }
    )
}

/**
 * Clickable modifier with the app-standard subtle ripple (primary at 8% alpha).
 *
 * Use on any card Surface that needs a tap action:
 *
 * ```
 * Surface(modifier = Modifier.cardClickable(onClick = onClick)) { ... }
 * ```
 */
@Composable
fun Modifier.cardClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        onClick = onClick
    )
}

/**
 * Convenience wrapper that combines [Column] + [expandable] + [ExpandableContent].
 *
 * Renders [collapsedContent] always, and [expandedContent] only when [state]
 * is expanded, with the standard height-morph animation.
 */
@Composable
fun ExpandableColumn(
    state: ExpandState,
    modifier: Modifier = Modifier,
    collapsedContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().expandable(state)) {
        collapsedContent()
        ExpandableContent(state = state) {
            expandedContent()
        }
    }
}
