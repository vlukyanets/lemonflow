package io.github.vlukyanets.lemonflow.domain.catalog

import io.github.vlukyanets.lemonflow.domain.projectId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectTreeTest {

    // work -> backend -> api ; hobby
    private val work = projectId("work")
    private val backend = projectId("backend")
    private val api = projectId("api")
    private val hobby = projectId("hobby")
    private val tree = ProjectTree(mapOf(work to null, backend to work, api to backend, hobby to null))

    @Test
    fun `project can move under an unrelated project or to the top level`() {
        assertTrue(tree.canMove(api, hobby))
        assertTrue(tree.canMove(backend, null))
    }

    @Test
    fun `project can't move under itself or its descendants`() {
        assertFalse(tree.canMove(work, work))
        assertFalse(tree.canMove(work, api))
        assertFalse(tree.canMove(backend, api))
    }

    @Test
    fun `subtree includes the project and every level below it`() {
        assertEquals(setOf(work, backend, api), tree.subtree(work))
        assertEquals(setOf(hobby), tree.subtree(hobby))
    }

    @Test
    fun `path runs from the top-level project down`() {
        assertEquals(listOf(work, backend, api), tree.path(api))
    }
}
