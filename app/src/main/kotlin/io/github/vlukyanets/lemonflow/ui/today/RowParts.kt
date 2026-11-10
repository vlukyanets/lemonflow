package io.github.vlukyanets.lemonflow.ui.today

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

/** What a Today row can do; every action is a single tap (SC-003). */
internal class TodayActions(
    val toggleDone: (TodayRowUi) -> Unit,
    val changeAmount: (HabitId, Int) -> Unit,
    val toggleTimer: (TodayRowUi) -> Unit
)

/** Test tag of the lifebuoy next to a habit name (a Baseline mark on a normal day). */
const val MIN_TAG = "today-min-tag"

/** Test tag of the filled check on a finished, paused timer. */
const val TIMER_DONE = "today-timer-done"

/**
 * A row's meta line; [minimum] is the part of [text] highlighted when the habit is done at
 * its minimum.
 */
internal class RowMeta(val text: String, val minimum: String)

/** One Today row: a leading control, the name block that opens the editor, trailing controls. */
@Composable
internal fun HabitRowFrame(
    row: TodayRowUi,
    meta: RowMeta,
    openEditor: (String) -> Unit,
    leading: @Composable () -> Unit,
    trailing: @Composable RowScope.() -> Unit
) {
    val background = if (row.running) LemonFlow.colors.runBg else MaterialTheme.colorScheme.surface
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .background(background)
            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        leading()
        NameBlock(
            row.habit,
            Meta(meta.text, meta.minimum.takeIf { row.minTag }),
            Modifier.weight(1f).clickable { openEditor(row.habit.id.value) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { trailing() }
    }
}

/** The meta line and, when the habit is done at its minimum, the part to highlight. */
private class Meta(val text: String, val highlight: String?)

/**
 * The name keeps one line (an ellipsis rather than an extra line or a broken word); the
 * meta line carries the "minimum" state, so a row never grows a line for a tag.
 */
@Composable
private fun NameBlock(habit: Habit, meta: Meta, modifier: Modifier) {
    Column(
        modifier.heightIn(min = 48.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                habit.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (habit.isCore) {
                Icon(
                    painterResource(R.drawable.ic_core),
                    contentDescription = stringResource(R.string.core_habit),
                    tint = LemonFlow.colors.muted,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (meta.highlight != null) {
                // "Minimum done" (FR-009): a lifebuoy - the rescue minimum - not a text tag
                // that would need its own line.
                Icon(
                    painterResource(R.drawable.ic_lifebuoy),
                    contentDescription = stringResource(R.string.today_min_tag),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).testTag(MIN_TAG)
                )
            }
        }
        MetaLine(meta)
    }
}

/** The meta line; at the minimum, the minimum part is highlighted as well (FR-009). */
@Composable
private fun MetaLine(meta: Meta) {
    val highlight = meta.highlight
    val start = highlight?.let { meta.text.indexOf(it) } ?: -1
    val text = buildAnnotatedString {
        append(meta.text)
        if (highlight != null && start >= 0) {
            addStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                ),
                start,
                start + highlight.length
            )
        }
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = LemonFlow.colors.muted)
}

/** A 48dp square button with a stroke icon or a short text, e.g. "-", "+" or "+1". */
@Composable
internal fun SquareButton(
    label: String,
    onClick: () -> Unit,
    @DrawableRes icon: Int? = null,
    text: String? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .border(1.dp, LemonFlow.colors.line, shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp))
        }
        if (text != null) Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** How a round mark looks: done, minimum done, open, or above an Avoid limit. */
internal enum class RoundLook { DONE, MINIMUM, OPEN, OVER }

/** The 48dp round badge used by Check and Avoid rows. */
@Composable
internal fun RoundBadge(
    look: RoundLook,
    modifier: Modifier = Modifier,
    content: @Composable (Color) -> Unit
) {
    val blue = MaterialTheme.colorScheme.primary
    val (fill, border, fg) = when (look) {
        RoundLook.DONE -> Triple(blue, null, MaterialTheme.colorScheme.onPrimary)
        RoundLook.MINIMUM -> Triple(LemonFlow.colors.blueSoft, blue, blue)
        RoundLook.OPEN -> Triple(MaterialTheme.colorScheme.surface, LemonFlow.colors.ring, blue)
        RoundLook.OVER -> Triple(LemonFlow.colors.surface2, null, LemonFlow.colors.muted)
    }
    Box(
        // Clip before the caller's modifier: it carries the click, and its press ripple must
        // be round, not a flash of the 48dp square.
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(modifier)
            .background(fill)
            .let { if (border != null) it.border(2.dp, border, CircleShape) else it },
        contentAlignment = Alignment.Center
    ) { content(fg) }
}

/** A progress ring filling towards today's goal (Count and Timer). */
@Composable
internal fun ProgressRing(fraction: Float, modifier: Modifier = Modifier) {
    val track = LemonFlow.colors.blueSoft
    val fill = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxSize()) {
        val stroke = 4.dp.toPx()
        val inset = stroke / 2 + 2.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        drawArc(track, 0f, FULL_CIRCLE, false, topLeft, arcSize, style = Stroke(stroke))
        if (fraction > 0f) {
            drawArc(
                fill,
                START_ANGLE,
                FULL_CIRCLE * fraction.coerceAtMost(1f),
                false,
                topLeft,
                arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
internal fun CheckIcon(tint: Color, size: Int = 22) {
    Icon(
        painterResource(R.drawable.ic_check),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size.dp)
    )
}

private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f
