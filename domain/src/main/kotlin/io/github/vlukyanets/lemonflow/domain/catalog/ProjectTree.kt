package io.github.vlukyanets.lemonflow.domain.catalog

import io.github.vlukyanets.lemonflow.domain.model.ProjectId

/**
 * Nested projects (spec: CAT-2, CAT-7). [parents] maps every project to its parent, or to
 * null for a top-level project.
 */
class ProjectTree(private val parents: Map<ProjectId, ProjectId?>) {

    /** CAT-2: a project can't move under itself or one of its descendants. */
    fun canMove(project: ProjectId, newParent: ProjectId?): Boolean {
        val seen = HashSet<ProjectId>()
        var current = newParent
        while (current != null) {
            if (current == project) return false
            check(seen.add(current)) { "Project tree already contains a cycle at $current" }
            current = parents[current]
        }
        return true
    }

    /** CAT-7: the project and its whole subtree. */
    fun subtree(root: ProjectId): Set<ProjectId> {
        val children = parents.entries
            .filter { it.value != null }
            .groupBy({ it.value!! }, { it.key })
        val result = LinkedHashSet<ProjectId>()
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (result.add(next)) queue.addAll(children[next].orEmpty())
        }
        return result
    }

    /** Path from the top-level project down to [project]. */
    fun path(project: ProjectId): List<ProjectId> {
        val path = ArrayDeque<ProjectId>()
        var current: ProjectId? = project
        while (current != null) {
            check(current !in path) { "Project tree already contains a cycle at $current" }
            path.addFirst(current)
            current = parents[current]
        }
        return path
    }
}
