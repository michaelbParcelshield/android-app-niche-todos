// ABOUTME: Pure helper to choose the next todo for voice-driven mode.
// ABOUTME: Walks top-level (parentId == null) unchecked todos in visible list order with wraparound.
package com.example.niche_todos

object VoiceDrivenTodoNavigator {
    /**
     * Returns the next top-level unchecked todo after [lastTodoId] in [visibleTodos] order.
     * Wraps to the top when reaching the end. Nested todos (parentId != null) are ignored.
     *
     * If there are no top-level unchecked todos, returns null.
     */
    fun nextTopLevelUnchecked(
        visibleTodos: List<Todo>,
        lastTodoId: String?
    ): Todo? {
        val candidates = visibleTodos.filter { it.parentId == null && !it.isCompleted }
        if (candidates.isEmpty()) {
            return null
        }

        if (lastTodoId == null) {
            return candidates.first()
        }

        val lastIndex = candidates.indexOfFirst { it.id == lastTodoId }
        if (lastIndex == -1) {
            return candidates.first()
        }

        // Try after the last index first, then wrap to the beginning.
        return candidates.drop(lastIndex + 1).firstOrNull() ?: candidates.first()
    }
}

