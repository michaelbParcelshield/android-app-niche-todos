// ABOUTME: Utility functions for todo hierarchy operations.
// ABOUTME: Provides ordering, depth calculation, and cycle detection for nested todos.
package com.example.niche_todos

object TodoHierarchyUtils {

    /**
     * Orders todos for hierarchical display using pre-order traversal.
     * Children appear directly after their parent, sorted by sortOrder.
     */
    fun orderForHierarchy(todos: List<Todo>): List<Todo> {
        if (todos.isEmpty()) return emptyList()

        val lookup = todos.associateBy { it.id }
        val childrenByParent = todos
            .filter { it.parentId != null }
            .groupBy { it.parentId!! }
            .mapValues { (_, children) ->
                children.sortedWith(compareBy({ it.sortOrder }, { it.id }))
            }

        val roots = todos
            .filter { it.parentId == null || !lookup.containsKey(it.parentId) }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))

        val ordered = mutableListOf<Todo>()
        val visited = mutableSetOf<String>()

        fun appendPreOrder(todo: Todo) {
            if (!visited.add(todo.id)) return
            ordered.add(todo)
            childrenByParent[todo.id]?.forEach { child ->
                appendPreOrder(child)
            }
        }

        roots.forEach { root -> appendPreOrder(root) }

        // Add any orphaned todos not visited
        todos.sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .filter { visited.add(it.id) }
            .forEach { ordered.add(it) }

        return ordered
    }

    /**
     * Builds a map of todo ID to nesting depth.
     * Root-level todos have depth 0, their children have depth 1, etc.
     * Handles cycles by treating cycle members as roots (depth 0).
     */
    fun buildDepthMap(todos: List<Todo>): Map<String, Int> {
        val todoById = todos.associateBy { it.id }
        val depths = mutableMapOf<String, Int>()

        fun computeDepth(todo: Todo, visiting: MutableSet<String>): Int {
            if (depths.containsKey(todo.id)) {
                return depths[todo.id]!!
            }
            if (!visiting.add(todo.id)) {
                // Cycle detected, treat as root and cache immediately
                depths[todo.id] = 0
                return 0
            }
            val parentId = todo.parentId
            val depth = if (parentId == null) {
                0
            } else {
                val parent = todoById[parentId]
                if (parent != null) {
                    val parentDepth = computeDepth(parent, visiting)
                    // If we detected a cycle (our depth was set to 0), don't add 1
                    if (depths.containsKey(todo.id)) {
                        return depths[todo.id]!!
                    }
                    parentDepth + 1
                } else {
                    0
                }
            }
            depths[todo.id] = depth
            return depth
        }

        for (todo in todos) {
            computeDepth(todo, mutableSetOf())
        }
        return depths
    }

    /**
     * Checks if making `dragged` a child of `target` would create a cycle.
     * Returns true if target is already a descendant of dragged.
     */
    fun wouldCreateCycle(dragged: Todo, target: Todo, allTodos: List<Todo>): Boolean {
        val todoById = allTodos.associateBy { it.id }
        var current: Todo? = target
        while (current != null) {
            if (current.id == dragged.id) {
                return true
            }
            current = current.parentId?.let { todoById[it] }
        }
        return false
    }

    /**
     * Builds reorder items from current list order, preserving parentIds.
     */
    fun buildReorderItemsFromCurrentOrder(todos: List<Todo>): List<ReorderTodoItem> {
        val sortOrderByParent = mutableMapOf<String?, Int>()
        return todos.map { todo ->
            val parentKey = todo.parentId
            val sortOrder = sortOrderByParent.getOrDefault(parentKey, 0)
            sortOrderByParent[parentKey] = sortOrder + 1
            ReorderTodoItem(
                id = todo.id,
                parentId = todo.parentId,
                sortOrder = sortOrder
            )
        }
    }

    /**
     * Builds reorder items with one todo nested under a new parent.
     */
    fun buildReorderItemsWithNesting(
        todos: List<Todo>,
        draggedId: String,
        newParentId: String
    ): List<ReorderTodoItem> {
        val sortOrderByParent = mutableMapOf<String?, Int>()
        return todos.map { todo ->
            val parentKey = if (todo.id == draggedId) newParentId else todo.parentId
            val sortOrder = sortOrderByParent.getOrDefault(parentKey, 0)
            sortOrderByParent[parentKey] = sortOrder + 1
            ReorderTodoItem(
                id = todo.id,
                parentId = parentKey,
                sortOrder = sortOrder
            )
        }
    }

    /**
     * Builds reorder items with one todo unnested to root level.
     */
    fun buildReorderItemsWithUnnesting(
        todos: List<Todo>,
        draggedId: String
    ): List<ReorderTodoItem> {
        val sortOrderByParent = mutableMapOf<String?, Int>()
        return todos.map { todo ->
            val parentKey = if (todo.id == draggedId) null else todo.parentId
            val sortOrder = sortOrderByParent.getOrDefault(parentKey, 0)
            sortOrderByParent[parentKey] = sortOrder + 1
            ReorderTodoItem(
                id = todo.id,
                parentId = parentKey,
                sortOrder = sortOrder
            )
        }
    }
}
