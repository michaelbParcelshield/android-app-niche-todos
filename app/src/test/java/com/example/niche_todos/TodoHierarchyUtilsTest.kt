// ABOUTME: Unit tests for TodoHierarchyUtils.
// ABOUTME: Verifies hierarchy ordering, depth calculation, cycle detection, and reorder item building.
package com.example.niche_todos

import org.junit.Test
import org.junit.Assert.*

class TodoHierarchyUtilsTest {

    private fun makeTodo(
        id: String,
        parentId: String? = null,
        sortOrder: Int = 0
    ): Todo = Todo(
        id = id,
        properties = listOf(
            TodoProperty.Title("Todo $id"),
            TodoProperty.StartDateTime(null),
            TodoProperty.EndDateTime(null)
        ),
        isCompleted = false,
        parentId = parentId,
        sortOrder = sortOrder
    )

    // ========== orderForHierarchy tests ==========

    @Test
    fun orderForHierarchy_emptyList_returnsEmpty() {
        val result = TodoHierarchyUtils.orderForHierarchy(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun orderForHierarchy_flatList_preservesSortOrder() {
        val todos = listOf(
            makeTodo("a", sortOrder = 2),
            makeTodo("b", sortOrder = 0),
            makeTodo("c", sortOrder = 1)
        )

        val result = TodoHierarchyUtils.orderForHierarchy(todos)

        assertEquals(listOf("b", "c", "a"), result.map { it.id })
    }

    @Test
    fun orderForHierarchy_simpleParentChild_childFollowsParent() {
        val todos = listOf(
            makeTodo("parent", sortOrder = 0),
            makeTodo("child", parentId = "parent", sortOrder = 0)
        )

        val result = TodoHierarchyUtils.orderForHierarchy(todos)

        assertEquals(listOf("parent", "child"), result.map { it.id })
    }

    @Test
    fun orderForHierarchy_multiLevelNesting_preOrderTraversal() {
        val todos = listOf(
            makeTodo("grandchild", parentId = "child", sortOrder = 0),
            makeTodo("child", parentId = "parent", sortOrder = 0),
            makeTodo("parent", sortOrder = 0)
        )

        val result = TodoHierarchyUtils.orderForHierarchy(todos)

        assertEquals(listOf("parent", "child", "grandchild"), result.map { it.id })
    }

    @Test
    fun orderForHierarchy_multipleChildrenSorted_respectsSortOrder() {
        val todos = listOf(
            makeTodo("parent", sortOrder = 0),
            makeTodo("child-c", parentId = "parent", sortOrder = 2),
            makeTodo("child-a", parentId = "parent", sortOrder = 0),
            makeTodo("child-b", parentId = "parent", sortOrder = 1)
        )

        val result = TodoHierarchyUtils.orderForHierarchy(todos)

        assertEquals(
            listOf("parent", "child-a", "child-b", "child-c"),
            result.map { it.id }
        )
    }

    @Test
    fun orderForHierarchy_orphanedTodo_treatedAsRoot() {
        // Orphans (parentId references non-existent todo) are treated as roots
        val todos = listOf(
            makeTodo("root", sortOrder = 1),
            makeTodo("orphan", parentId = "nonexistent", sortOrder = 0)
        )

        val result = TodoHierarchyUtils.orderForHierarchy(todos)

        // Orphan has lower sortOrder, so it comes first
        assertEquals(listOf("orphan", "root"), result.map { it.id })
    }

    @Test
    fun orderForHierarchy_circularReference_doesNotCrash() {
        // A -> B -> A (cycle)
        val todos = listOf(
            makeTodo("a", parentId = "b", sortOrder = 0),
            makeTodo("b", parentId = "a", sortOrder = 0)
        )

        // Should not throw or infinite loop
        val result = TodoHierarchyUtils.orderForHierarchy(todos)

        // Both should be present
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "a" })
        assertTrue(result.any { it.id == "b" })
    }

    @Test
    fun orderForHierarchy_complexHierarchy_correctOrder() {
        // Structure:
        // root1 (sort=0)
        //   - child1a (sort=0)
        //   - child1b (sort=1)
        //     - grandchild1b1 (sort=0)
        // root2 (sort=1)
        //   - child2a (sort=0)
        val todos = listOf(
            makeTodo("root2", sortOrder = 1),
            makeTodo("child2a", parentId = "root2", sortOrder = 0),
            makeTodo("root1", sortOrder = 0),
            makeTodo("grandchild1b1", parentId = "child1b", sortOrder = 0),
            makeTodo("child1b", parentId = "root1", sortOrder = 1),
            makeTodo("child1a", parentId = "root1", sortOrder = 0)
        )

        val result = TodoHierarchyUtils.orderForHierarchy(todos)

        assertEquals(
            listOf("root1", "child1a", "child1b", "grandchild1b1", "root2", "child2a"),
            result.map { it.id }
        )
    }

    // ========== buildDepthMap tests ==========

    @Test
    fun buildDepthMap_emptyList_returnsEmpty() {
        val result = TodoHierarchyUtils.buildDepthMap(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun buildDepthMap_flatList_allDepthZero() {
        val todos = listOf(
            makeTodo("a"),
            makeTodo("b"),
            makeTodo("c")
        )

        val result = TodoHierarchyUtils.buildDepthMap(todos)

        assertEquals(0, result["a"])
        assertEquals(0, result["b"])
        assertEquals(0, result["c"])
    }

    @Test
    fun buildDepthMap_nestedItems_correctDepths() {
        val todos = listOf(
            makeTodo("root"),
            makeTodo("child", parentId = "root"),
            makeTodo("grandchild", parentId = "child")
        )

        val result = TodoHierarchyUtils.buildDepthMap(todos)

        assertEquals(0, result["root"])
        assertEquals(1, result["child"])
        assertEquals(2, result["grandchild"])
    }

    @Test
    fun buildDepthMap_orphanedItem_depthZero() {
        val todos = listOf(
            makeTodo("orphan", parentId = "nonexistent")
        )

        val result = TodoHierarchyUtils.buildDepthMap(todos)

        assertEquals(0, result["orphan"])
    }

    @Test
    fun buildDepthMap_cycle_doesNotCrash() {
        // A -> B -> A (cycle)
        val todos = listOf(
            makeTodo("a", parentId = "b"),
            makeTodo("b", parentId = "a")
        )

        // Should not throw or infinite loop
        val result = TodoHierarchyUtils.buildDepthMap(todos)

        // Both should have entries (cycle breaks to depth 0)
        assertNotNull(result["a"])
        assertNotNull(result["b"])
    }

    @Test
    fun buildDepthMap_selfReference_treatedAsRoot() {
        val todos = listOf(
            makeTodo("self", parentId = "self")
        )

        val result = TodoHierarchyUtils.buildDepthMap(todos)

        assertEquals(0, result["self"])
    }

    // ========== wouldCreateCycle tests ==========

    @Test
    fun wouldCreateCycle_unrelatedTodos_returnsFalse() {
        val todos = listOf(
            makeTodo("a"),
            makeTodo("b")
        )

        val result = TodoHierarchyUtils.wouldCreateCycle(
            dragged = todos[0],
            target = todos[1],
            allTodos = todos
        )

        assertFalse(result)
    }

    @Test
    fun wouldCreateCycle_validNesting_returnsFalse() {
        // Moving child under parent is valid
        val todos = listOf(
            makeTodo("parent"),
            makeTodo("child")
        )

        val result = TodoHierarchyUtils.wouldCreateCycle(
            dragged = todos[1],
            target = todos[0],
            allTodos = todos
        )

        assertFalse(result)
    }

    @Test
    fun wouldCreateCycle_parentOntoChild_returnsTrue() {
        val todos = listOf(
            makeTodo("parent"),
            makeTodo("child", parentId = "parent")
        )

        val result = TodoHierarchyUtils.wouldCreateCycle(
            dragged = todos[0], // parent
            target = todos[1],  // child
            allTodos = todos
        )

        assertTrue(result)
    }

    @Test
    fun wouldCreateCycle_grandparentOntoGrandchild_returnsTrue() {
        val todos = listOf(
            makeTodo("grandparent"),
            makeTodo("parent", parentId = "grandparent"),
            makeTodo("child", parentId = "parent")
        )

        val result = TodoHierarchyUtils.wouldCreateCycle(
            dragged = todos[0], // grandparent
            target = todos[2],  // grandchild
            allTodos = todos
        )

        assertTrue(result)
    }

    @Test
    fun wouldCreateCycle_siblingNesting_returnsFalse() {
        val todos = listOf(
            makeTodo("parent"),
            makeTodo("sibling1", parentId = "parent"),
            makeTodo("sibling2", parentId = "parent")
        )

        val result = TodoHierarchyUtils.wouldCreateCycle(
            dragged = todos[1], // sibling1
            target = todos[2],  // sibling2
            allTodos = todos
        )

        assertFalse(result)
    }

    // ========== buildReorderItemsFromCurrentOrder tests ==========

    @Test
    fun buildReorderItemsFromCurrentOrder_preservesParentIds() {
        val todos = listOf(
            makeTodo("parent"),
            makeTodo("child", parentId = "parent")
        )

        val result = TodoHierarchyUtils.buildReorderItemsFromCurrentOrder(todos)

        assertEquals(2, result.size)
        assertEquals("parent", result[0].id)
        assertNull(result[0].parentId)
        assertEquals("child", result[1].id)
        assertEquals("parent", result[1].parentId)
    }

    @Test
    fun buildReorderItemsFromCurrentOrder_assignsSortOrdersPerParent() {
        val todos = listOf(
            makeTodo("root1"),
            makeTodo("root2"),
            makeTodo("child1", parentId = "root1"),
            makeTodo("child2", parentId = "root1")
        )

        val result = TodoHierarchyUtils.buildReorderItemsFromCurrentOrder(todos)

        // Root items get sort orders 0, 1 under null parent
        assertEquals(0, result[0].sortOrder)
        assertEquals(1, result[1].sortOrder)
        // Children of root1 get sort orders 0, 1 under root1
        assertEquals(0, result[2].sortOrder)
        assertEquals(1, result[3].sortOrder)
    }

    // ========== buildReorderItemsWithSiblingDrop tests ==========

    @Test
    fun buildReorderItemsWithSiblingDrop_betweenSiblings_reparentsDraggedTodo() {
        val todos = listOf(
            makeTodo("parent"),
            makeTodo("child-a", parentId = "parent"),
            makeTodo("dragged"),
            makeTodo("child-b", parentId = "parent")
        )

        val result = TodoHierarchyUtils.buildReorderItemsWithSiblingDrop(todos, "dragged")

        val draggedItem = result.find { it.id == "dragged" }
        assertEquals("parent", draggedItem?.parentId)
    }

    @Test
    fun buildReorderItemsWithSiblingDrop_betweenRootSiblings_unnestsDraggedTodo() {
        val todos = listOf(
            makeTodo("root-a"),
            makeTodo("dragged", parentId = "root-b"),
            makeTodo("root-b")
        )

        val result = TodoHierarchyUtils.buildReorderItemsWithSiblingDrop(todos, "dragged")

        val draggedItem = result.find { it.id == "dragged" }
        assertNull(draggedItem?.parentId)
    }

    @Test
    fun buildReorderItemsWithSiblingDrop_cycleDetected_preservesCurrentParent() {
        val todos = listOf(
            makeTodo("sibling-a", parentId = "child"),
            makeTodo("parent"),
            makeTodo("sibling-b", parentId = "child"),
            makeTodo("child", parentId = "parent")
        )

        val result = TodoHierarchyUtils.buildReorderItemsWithSiblingDrop(todos, "parent")

        val draggedItem = result.find { it.id == "parent" }
        assertNull(draggedItem?.parentId)
    }

    // ========== findSiblingDropTarget tests ==========

    @Test
    fun findSiblingDropTarget_atStart_returnsNull() {
        val todos = listOf(
            makeTodo("dragged"),
            makeTodo("child", parentId = "parent")
        )

        val result = TodoHierarchyUtils.findSiblingDropTarget(todos, "dragged")

        assertNull(result)
    }

    @Test
    fun findSiblingDropTarget_atEnd_returnsNull() {
        val todos = listOf(
            makeTodo("child", parentId = "parent"),
            makeTodo("dragged")
        )

        val result = TodoHierarchyUtils.findSiblingDropTarget(todos, "dragged")

        assertNull(result)
    }

    @Test
    fun findSiblingDropTarget_mismatchedParents_returnsNull() {
        val todos = listOf(
            makeTodo("left", parentId = "parent-a"),
            makeTodo("dragged"),
            makeTodo("right", parentId = "parent-b")
        )

        val result = TodoHierarchyUtils.findSiblingDropTarget(todos, "dragged")

        assertNull(result)
    }

    @Test
    fun findSiblingDropTarget_betweenSiblings_returnsSharedParent() {
        val todos = listOf(
            makeTodo("left", parentId = "parent"),
            makeTodo("dragged"),
            makeTodo("right", parentId = "parent")
        )

        val result = TodoHierarchyUtils.findSiblingDropTarget(todos, "dragged")

        assertEquals("parent", result?.parentId)
    }

    // ========== buildReorderItemsWithNesting tests ==========

    @Test
    fun buildReorderItemsWithNesting_setsNewParentId() {
        val todos = listOf(
            makeTodo("parent"),
            makeTodo("child")
        )

        val result = TodoHierarchyUtils.buildReorderItemsWithNesting(
            todos = todos,
            draggedId = "child",
            newParentId = "parent"
        )

        val childItem = result.find { it.id == "child" }
        assertEquals("parent", childItem?.parentId)
    }

    @Test
    fun buildReorderItemsWithNesting_preservesOtherParentIds() {
        val todos = listOf(
            makeTodo("parent"),
            makeTodo("existing-child", parentId = "parent"),
            makeTodo("new-child")
        )

        val result = TodoHierarchyUtils.buildReorderItemsWithNesting(
            todos = todos,
            draggedId = "new-child",
            newParentId = "parent"
        )

        val existingChild = result.find { it.id == "existing-child" }
        assertEquals("parent", existingChild?.parentId)
    }

    // ========== buildReorderItemsWithUnnesting tests ==========

    @Test
    fun buildReorderItemsWithUnnesting_setsParentIdToNull() {
        val todos = listOf(
            makeTodo("parent"),
            makeTodo("child", parentId = "parent")
        )

        val result = TodoHierarchyUtils.buildReorderItemsWithUnnesting(
            todos = todos,
            draggedId = "child"
        )

        val childItem = result.find { it.id == "child" }
        assertNull(childItem?.parentId)
    }

    @Test
    fun buildReorderItemsWithUnnesting_preservesOtherParentIds() {
        val todos = listOf(
            makeTodo("parent"),
            makeTodo("child1", parentId = "parent"),
            makeTodo("child2", parentId = "parent")
        )

        val result = TodoHierarchyUtils.buildReorderItemsWithUnnesting(
            todos = todos,
            draggedId = "child1"
        )

        val child1 = result.find { it.id == "child1" }
        val child2 = result.find { it.id == "child2" }
        assertNull(child1?.parentId)
        assertEquals("parent", child2?.parentId)
    }
}
