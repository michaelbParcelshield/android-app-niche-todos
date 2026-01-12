// ABOUTME: Utility functions for todo hierarchy operations.
// ABOUTME: Provides ordering, depth calculation, and cycle detection for nested todos.
package com.example.niche_todos

object TodoHierarchyUtils {

    data class ExclusionContext(
        val todos: List<Todo>,
        val excludedPosition: Int
    )

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
     * Returns the parentId when the item at childPosition is the first child in list order.
     */
    fun parentIdForFirstChild(todos: List<Todo>, childPosition: Int): String? {
        if (childPosition !in todos.indices) return null
        val child = todos[childPosition]
        val parentId = child.parentId ?: return null
        val parentPosition = todos.indexOfFirst { it.id == parentId }
        if (parentPosition == -1) return null
        return if (childPosition == parentPosition + 1) parentId else null
    }

    /**
     * Returns the parentId when the item at childPosition is the first child in list order.
     * The child position is provided relative to the unfiltered list.
     */
    fun parentIdForFirstChild(context: ExclusionContext, childPosition: Int): String? {
        val adjustedPosition = adjustPosition(childPosition, context.excludedPosition) ?: return null
        return parentIdForFirstChild(context.todos, adjustedPosition)
    }

    /**
     * Returns the parentId when the target item is immediately followed by its first child.
     */
    fun parentIdForTrailingEdge(todos: List<Todo>, targetPosition: Int): String? {
        if (targetPosition !in todos.indices) return null
        val nextPosition = targetPosition + 1
        if (nextPosition !in todos.indices) return null
        val target = todos[targetPosition]
        val next = todos[nextPosition]
        val nextParentId = next.parentId ?: return null
        return if (nextParentId == target.id) target.id else null
    }

    /**
     * Returns the parentId when the target item is immediately followed by its first child.
     * The target position is provided relative to the unfiltered list.
     */
    fun parentIdForTrailingEdge(context: ExclusionContext, targetPosition: Int): String? {
        val adjustedPosition = adjustPosition(targetPosition, context.excludedPosition) ?: return null
        return parentIdForTrailingEdge(context.todos, adjustedPosition)
    }

    /**
     * Returns the parentId when the child position is first child, ignoring the excluded todo.
     */
    fun parentIdForFirstChildExcluding(
        todos: List<Todo>,
        childPosition: Int,
        excludedId: String?
    ): String? {
        if (excludedId == null) {
            return parentIdForFirstChild(todos, childPosition)
        }
        val excludedPosition = todos.indexOfFirst { it.id == excludedId }
        if (excludedPosition == -1) {
            return parentIdForFirstChild(todos, childPosition)
        }
        if (childPosition == excludedPosition) return null
        val adjustedPosition = if (excludedPosition < childPosition) {
            childPosition - 1
        } else {
            childPosition
        }
        val filteredTodos = todos.filter { it.id != excludedId }
        return parentIdForFirstChild(filteredTodos, adjustedPosition)
    }

    /**
     * Returns the parentId when the target item is followed by its first child, ignoring the excluded todo.
     */
    fun parentIdForTrailingEdgeExcluding(
        todos: List<Todo>,
        targetPosition: Int,
        excludedId: String?
    ): String? {
        if (excludedId == null) {
            return parentIdForTrailingEdge(todos, targetPosition)
        }
        val excludedPosition = todos.indexOfFirst { it.id == excludedId }
        if (excludedPosition == -1) {
            return parentIdForTrailingEdge(todos, targetPosition)
        }
        if (targetPosition == excludedPosition) return null
        val adjustedPosition = if (excludedPosition < targetPosition) {
            targetPosition - 1
        } else {
            targetPosition
        }
        val filteredTodos = todos.filter { it.id != excludedId }
        return parentIdForTrailingEdge(filteredTodos, adjustedPosition)
    }

    /**
     * Returns the parentId when the gap is between parent and its first child, ignoring the excluded todo.
     */
    fun parentIdForGapExcluding(
        todos: List<Todo>,
        upperId: String,
        lowerId: String,
        excludedId: String?
    ): String? {
        val context = buildExclusionContext(todos, excludedId)
        return parentIdForGap(context, upperId, lowerId)
    }

    /**
     * Returns the parentId when the gap is between parent and its first child.
     */
    fun parentIdForGap(context: ExclusionContext, upperId: String, lowerId: String): String? {
        val upperIndex = context.todos.indexOfFirst { it.id == upperId }
        val lowerIndex = context.todos.indexOfFirst { it.id == lowerId }
        if (upperIndex == -1 || lowerIndex == -1) return null
        if (lowerIndex != upperIndex + 1) return null
        val lowerTodo = context.todos[lowerIndex]
        return if (lowerTodo.parentId == upperId) upperId else null
    }

    fun buildExclusionContext(todos: List<Todo>, excludedId: String?): ExclusionContext {
        if (excludedId == null) {
            return ExclusionContext(todos, -1)
        }
        val excludedPosition = todos.indexOfFirst { it.id == excludedId }
        if (excludedPosition == -1) {
            return ExclusionContext(todos, -1)
        }
        return ExclusionContext(todos.filter { it.id != excludedId }, excludedPosition)
    }

    private fun adjustPosition(position: Int, excludedPosition: Int): Int? {
        if (excludedPosition == -1) return position
        if (position == excludedPosition) return null
        return if (position > excludedPosition) position - 1 else position
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
