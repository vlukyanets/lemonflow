package io.github.vlukyanets.lemonflow.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

/**
 * Space the floating tab bar covers at the bottom of the screen. Scrolling screens end
 * their content with a spacer this tall, so the last item can scroll out from under it.
 */
val LocalBottomBarSpace = compositionLocalOf { 0.dp }

/** One tab of the floating bar: its label (already resolved) and icon. */
internal class TabItem(val label: String, @DrawableRes val icon: Int, val selected: Boolean)

/**
 * A pill that wraps its tabs instead of spanning the screen: each tab is as wide as its
 * label, so a long label ("Налаштування") keeps its size on one line (SC-007). When the
 * labels can't fit even without side padding, the bar shows icons only. The selected-tab
 * highlight slides to its tab.
 */
@Composable
internal fun FloatingTabBar(
    items: List<TabItem>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val labelStyle = tabLabelStyle()
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val labels = items.map { it.label }
        val available = constraints.maxWidth - with(density) { (BAR_PADDING * 2).roundToPx() }
        val showLabels = remember(labels, labelStyle, density, available) {
            val widths = labels.map {
                measurer.measure(it, labelStyle, maxLines = 1, softWrap = false).size.width
            }
            labelsFit(
                labelWidths = widths,
                minTab = with(density) { TAB_MIN_WIDTH.roundToPx() },
                minSide = with(density) { TAB_MIN_SIDE_PADDING.roundToPx() },
                available = available
            )
        }
        Surface(
            shape = RoundedCornerShape(percent = 50),
            // Opaque: text scrolling underneath showed through a translucent bar.
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, LemonFlow.colors.line),
            shadowElevation = 6.dp
        ) {
            TabRow(items, showLabels, labelStyle, onSelect)
        }
    }
}

@Composable
private fun TabRow(
    items: List<TabItem>,
    showLabels: Boolean,
    labelStyle: TextStyle,
    onSelect: (Int) -> Unit
) {
    val bounds = remember(items.size) {
        mutableStateListOf<TabBounds>().apply { repeat(items.size) { add(TabBounds()) } }
    }
    val selected = items.indexOfFirst { it.selected }
    Box(Modifier.padding(BAR_PADDING)) {
        bounds.getOrNull(selected)?.takeIf { it.width > 0 }?.let { target ->
            SlidingPill(target)
        }
        WrapRow(Modifier.selectableGroup()) {
            items.forEachIndexed { i, item ->
                TabButton(
                    item = item,
                    showLabel = showLabels,
                    labelStyle = labelStyle,
                    onClick = { onSelect(i) },
                    modifier = Modifier.onPlaced {
                        bounds[i] = TabBounds(
                            it.positionInParent().x.toInt(),
                            it.size.width,
                            it.size.height
                        )
                    }
                )
            }
        }
    }
}

private data class TabBounds(val x: Int = 0, val width: Int = 0, val height: Int = 0)

/** The selected-tab highlight; it moves and resizes to the selected tab. */
@Composable
private fun SlidingPill(target: TabBounds) {
    val x by animateIntAsState(target.x, label = "pill x")
    val width by animateIntAsState(target.width, label = "pill width")
    val density = LocalDensity.current
    Box(
        Modifier
            .offset { IntOffset(x, 0) }
            .size(with(density) { width.toDp() }, with(density) { target.height.toDp() })
            .background(LemonFlow.colors.blueSoft, RoundedCornerShape(percent = 50))
    )
}

@Composable
private fun TabButton(
    item: TabItem,
    showLabel: Boolean,
    labelStyle: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val content by animateColorAsState(
        if (item.selected) MaterialTheme.colorScheme.primary else LemonFlow.colors.muted,
        label = "tab content"
    )
    Column(
        modifier
            // The press ripple takes the pill's shape instead of flashing a rectangle.
            .clip(RoundedCornerShape(percent = 50))
            .selectable(selected = item.selected, role = Role.Tab, onClick = onClick)
            .widthIn(min = TAB_MIN_WIDTH)
            .heightIn(min = TAB_MIN_HEIGHT)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        Icon(
            painterResource(item.icon),
            // Without a visible label the icon carries the tab's name for screen readers.
            contentDescription = if (showLabel) null else item.label,
            tint = content,
            modifier = Modifier.size(if (showLabel) 22.dp else 24.dp)
        )
        if (showLabel) {
            Text(
                item.label,
                style = labelStyle,
                color = content,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun tabLabelStyle(): TextStyle =
    MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, lineHeight = 16.sp)

/**
 * Children side by side; the row is as wide as they are. Each child gets its content width
 * plus up to [TAB_SIDE_PADDING] on both sides; when space is short the side padding
 * shrinks first, and only then do the children shrink in proportion.
 */
@Composable
private fun WrapRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val bare = measurables.map { it.maxIntrinsicWidth(constraints.maxHeight) }
        val spare = constraints.maxWidth - bare.sum()
        val widths = if (spare >= 0 && measurables.isNotEmpty()) {
            val side = minOf(TAB_SIDE_PADDING.roundToPx(), spare / (2 * measurables.size))
            bare.map { it + 2 * side }
        } else {
            shareWidth(bare, constraints.maxWidth)
        }
        val placeables = measurables.mapIndexed { i, measurable ->
            measurable.measure(
                Constraints(
                    minWidth = widths[i],
                    maxWidth = widths[i],
                    maxHeight = constraints.maxHeight
                )
            )
        }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(widths.sum(), height) {
            var x = 0
            placeables.forEach {
                it.placeRelative(x, (height - it.height) / 2)
                x += it.width
            }
        }
    }
}

/**
 * Whether tabs with these label widths fit in [available] pixels with at least [minSide]
 * on each side of every label (text touching the highlight's edge looks broken); a tab is
 * never narrower than [minTab].
 */
internal fun labelsFit(labelWidths: List<Int>, minTab: Int, minSide: Int, available: Int): Boolean =
    labelWidths.sumOf { maxOf(it + 2 * minSide, minTab) } <= available

/**
 * Splits [available] pixels between slots that need [natural] pixels each. Spare width is
 * shared equally; when space is short every slot shrinks in proportion to its need. The
 * result always sums to [available].
 */
internal fun shareWidth(natural: List<Int>, available: Int): List<Int> {
    if (natural.isEmpty()) return emptyList()
    val total = natural.sum()
    return if (total in 1..available) {
        val extra = available - total
        natural.mapIndexed { i, need ->
            need + extra / natural.size + if (i < extra % natural.size) 1 else 0
        }
    } else if (total > 0) {
        val scaled = natural.map { (it.toLong() * available / total).toInt() }.toMutableList()
        val largest = scaled.indices.maxBy { scaled[it] }
        scaled[largest] += available - scaled.sum()
        scaled
    } else {
        natural.indices.map { i ->
            available / natural.size + if (i < available % natural.size) 1 else 0
        }
    }
}

/** Gap between the bar and the screen edges / gesture bar. */
internal val BAR_MARGIN: Dp = 12.dp
private val BAR_PADDING = 4.dp
private val TAB_MIN_WIDTH = 56.dp
private val TAB_MIN_HEIGHT = 52.dp
private val TAB_SIDE_PADDING = 14.dp
private val TAB_MIN_SIDE_PADDING = 6.dp
