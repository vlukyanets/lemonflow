package io.github.vlukyanets.lemonflow.ui

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue

/**
 * FR-017: every clickable node has a touch target of at least 48x48dp and something for a
 * screen reader to announce (its own or merged text, or a content description).
 */
fun ComposeContentTestRule.assertAllClickablesAccessible() {
    val minPx = with(density) { 48.dp.toPx() } - TOLERANCE_PX
    val nodes = onAllNodes(hasClickAction()).fetchSemanticsNodes()
    assertTrue("expected at least one clickable node", nodes.isNotEmpty())
    nodes.forEach { node ->
        val bounds = node.touchBoundsInRoot
        assertTrue(
            "touch target ${bounds.width}x${bounds.height}px below 48dp for '${node.label()}'",
            bounds.width >= minPx && bounds.height >= minPx
        )
        assertTrue("clickable without label: ${node.config}", node.label().isNotBlank())
    }
}

private fun SemanticsNode.label(): String {
    val description = config.getOrNull(
        SemanticsProperties.ContentDescription
    ).orEmpty().joinToString(" ")
    val text = config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString(" ") { it.text }
    return "$description $text".trim()
}

private const val TOLERANCE_PX = 0.5f

/**
 * A phone tall enough that whole screens fit without scrolling, so off-screen nodes (which
 * report 0x0 bounds) do not hide real layout problems.
 */
const val TALL_PHONE = "w411dp-h1600dp-xxhdpi"
