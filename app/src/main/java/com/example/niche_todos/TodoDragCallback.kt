// ABOUTME: Custom ItemTouchHelper.Callback for hierarchical todo drag-and-drop.
// Supports reordering, nesting (making child), and unnesting via hover zone detection.
package com.example.niche_todos

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

enum class DropMode {
    REORDER,
    NEST,
    UNNEST
}

class TodoDragCallback(
    private val adapter: TodoAdapter,
    private val onDragComplete: (List<ReorderTodoItem>) -> Unit,
    private val onInvalidDrop: () -> Unit
) : ItemTouchHelper.Callback() {

    private var draggedItemId: String? = null
    private var currentTargetPosition: Int = RecyclerView.NO_POSITION
    private var dropMode: DropMode = DropMode.REORDER
    private var lastHighlightedPosition: Int = RecyclerView.NO_POSITION

    private companion object {
        // Middle 50% of item height triggers nesting; outer 25% on each edge triggers reorder
        const val NEST_ZONE_START = 0.25f
        const val NEST_ZONE_END = 0.75f
        // Distance from RecyclerView top/bottom edge to trigger unnesting to root level
        const val EDGE_THRESHOLD_DP = 50f
        // Visual feedback for dragged item
        const val DRAGGING_ELEVATION = 8f
        const val DEFAULT_ELEVATION = 2f
        const val DRAGGING_ALPHA = 0.9f
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                draggedItemId = adapter.getItem(position).id
            }
            viewHolder.itemView.elevation = DRAGGING_ELEVATION
            viewHolder.itemView.alpha = DRAGGING_ALPHA
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
            val draggedView = viewHolder.itemView
            val dragCenterY = draggedView.top + dY + draggedView.height / 2f

            var foundTarget = false
            var newTargetPosition = RecyclerView.NO_POSITION
            var newDropMode = DropMode.REORDER

            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                val childHolder = recyclerView.getChildViewHolder(child)
                val childPosition = childHolder.adapterPosition

                if (childPosition == RecyclerView.NO_POSITION ||
                    childPosition == viewHolder.adapterPosition
                ) {
                    continue
                }

                if (dragCenterY >= child.top && dragCenterY <= child.bottom) {
                    val itemHeight = child.height.toFloat()
                    val relativeY = dragCenterY - child.top
                    val middleStart = itemHeight * NEST_ZONE_START
                    val middleEnd = itemHeight * NEST_ZONE_END

                    newTargetPosition = childPosition
                    foundTarget = true

                    if (relativeY in middleStart..middleEnd) {
                        newDropMode = DropMode.NEST
                    } else {
                        newDropMode = DropMode.REORDER
                    }
                    break
                }
            }

            if (!foundTarget) {
                val density = recyclerView.context.resources.displayMetrics.density
                val edgeThreshold = EDGE_THRESHOLD_DP * density
                if (dragCenterY < edgeThreshold || dragCenterY > recyclerView.height - edgeThreshold) {
                    newDropMode = DropMode.UNNEST
                }
            }

            currentTargetPosition = newTargetPosition
            dropMode = newDropMode

            if (newDropMode == DropMode.NEST && newTargetPosition != RecyclerView.NO_POSITION) {
                if (lastHighlightedPosition != newTargetPosition) {
                    adapter.setNestHighlight(newTargetPosition)
                    lastHighlightedPosition = newTargetPosition
                }
            } else {
                if (lastHighlightedPosition != RecyclerView.NO_POSITION) {
                    adapter.clearHighlights()
                    lastHighlightedPosition = RecyclerView.NO_POSITION
                }
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition
        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
            return false
        }

        if (dropMode == DropMode.REORDER) {
            adapter.moveItem(fromPosition, toPosition)
            return true
        }
        return false
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        viewHolder.itemView.elevation = DEFAULT_ELEVATION
        viewHolder.itemView.alpha = 1f
        adapter.clearHighlights()

        val draggedId = draggedItemId
        if (draggedId == null) {
            resetState()
            return
        }

        val todos = adapter.currentItems()
        val draggedTodo = todos.find { it.id == draggedId }
        if (draggedTodo == null) {
            resetState()
            return
        }

        when (dropMode) {
            DropMode.NEST -> {
                if (currentTargetPosition in todos.indices) {
                    val targetTodo = todos[currentTargetPosition]

                    if (wouldCreateCycle(draggedTodo, targetTodo, todos)) {
                        onInvalidDrop()
                    } else {
                        val items = buildReorderItemsWithNesting(
                            todos,
                            draggedTodo.id,
                            targetTodo.id
                        )
                        onDragComplete(items)
                    }
                }
            }
            DropMode.UNNEST -> {
                if (draggedTodo.parentId != null) {
                    val items = buildReorderItemsWithUnnesting(todos, draggedTodo.id)
                    onDragComplete(items)
                } else {
                    val items = buildReorderItemsFromCurrentOrder(todos)
                    onDragComplete(items)
                }
            }
            DropMode.REORDER -> {
                val items = buildReorderItemsFromCurrentOrder(todos)
                onDragComplete(items)
            }
        }

        resetState()
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Swipe disabled
    }

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = false

    private fun resetState() {
        draggedItemId = null
        currentTargetPosition = RecyclerView.NO_POSITION
        dropMode = DropMode.REORDER
        lastHighlightedPosition = RecyclerView.NO_POSITION
    }

    private fun wouldCreateCycle(dragged: Todo, target: Todo, allTodos: List<Todo>): Boolean =
        TodoHierarchyUtils.wouldCreateCycle(dragged, target, allTodos)

    private fun buildReorderItemsFromCurrentOrder(todos: List<Todo>): List<ReorderTodoItem> =
        TodoHierarchyUtils.buildReorderItemsFromCurrentOrder(todos)

    private fun buildReorderItemsWithNesting(
        todos: List<Todo>,
        draggedId: String,
        newParentId: String
    ): List<ReorderTodoItem> =
        TodoHierarchyUtils.buildReorderItemsWithNesting(todos, draggedId, newParentId)

    private fun buildReorderItemsWithUnnesting(
        todos: List<Todo>,
        draggedId: String
    ): List<ReorderTodoItem> =
        TodoHierarchyUtils.buildReorderItemsWithUnnesting(todos, draggedId)
}
