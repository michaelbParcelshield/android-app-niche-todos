// ABOUTME: RecyclerView adapter for displaying todo items
// Handles binding todo data to item views and user interaction callbacks
package com.example.niche_todos

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class TodoAdapter(
    private val onToggleComplete: (String) -> Unit,
    private val onEdit: (Todo) -> Unit,
    private val onDelete: (String) -> Unit,
    private val onAddSubtask: (String) -> Unit,
    private val onToggleCollapse: (String) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private val todos: MutableList<Todo> = mutableListOf()
    private var highlightedPosition: Int = RecyclerView.NO_POSITION
    private var depthMap: Map<String, Int> = emptyMap()
    private var hasChildrenIds: Set<String> = emptySet()
    private var collapsedIds: Set<String> = emptySet()

    fun submitList(
        newTodos: List<Todo>,
        hasChildrenIds: Set<String>,
        collapsedIds: Set<String>
    ) {
        todos.clear()
        todos.addAll(newTodos)
        depthMap = buildDepthMap(newTodos)
        this.hasChildrenIds = hasChildrenIds
        this.collapsedIds = collapsedIds
        notifyDataSetChanged()
    }

    private fun buildDepthMap(todoList: List<Todo>): Map<String, Int> =
        TodoHierarchyUtils.buildDepthMap(todoList)

    fun getItem(position: Int): Todo = todos[position]

    fun setNestHighlight(position: Int) {
        val previousHighlight = highlightedPosition
        highlightedPosition = position
        if (previousHighlight != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousHighlight)
        }
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position)
        }
    }

    fun clearHighlights() {
        if (highlightedPosition != RecyclerView.NO_POSITION) {
            val previous = highlightedPosition
            highlightedPosition = RecyclerView.NO_POSITION
            notifyItemChanged(previous)
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex ||
            fromIndex !in todos.indices ||
            toIndex !in todos.indices
        ) {
            return
        }
        val todo = todos.removeAt(fromIndex)
        todos.add(toIndex, todo)
        notifyItemMoved(fromIndex, toIndex)
    }

    fun currentItems(): List<Todo> = todos.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = todos[position]
        val depth = depthMap[todo.id] ?: 0
        val isHighlighted = position == highlightedPosition
        holder.bind(todo, depth, isHighlighted)
    }

    override fun getItemCount(): Int = todos.size

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val contentLayout: View = itemView.findViewById(R.id.content_layout)
        private val nestingBar: View = itemView.findViewById(R.id.view_nesting_bar)
        private val nestedDivider: View = itemView.findViewById(R.id.view_nested_divider)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_completed)
        private val textView: TextView = itemView.findViewById(R.id.text_todo)
        private val startDateView: TextView = itemView.findViewById(R.id.text_start_date)
        private val endDateView: TextView = itemView.findViewById(R.id.text_end_date)
        private val collapseButton: ImageButton = itemView.findViewById(R.id.button_toggle_collapse)
        private val moreButton: ImageButton = itemView.findViewById(R.id.button_more)
        private val dateTimeFormatter = TodoDateTimeFormatter()
        private val baseContentPaddingLeftPx = contentLayout.paddingLeft
        private val baseContentPaddingTopPx = contentLayout.paddingTop
        private val baseContentPaddingRightPx = contentLayout.paddingRight
        private val baseContentPaddingBottomPx = contentLayout.paddingBottom
        private val baseCardLayoutParams: ViewGroup.MarginLayoutParams =
            (cardView.layoutParams as ViewGroup.MarginLayoutParams).let { ViewGroup.MarginLayoutParams(it) }
        private val baseCardCornerRadius = cardView.radius
        private val baseCardStrokeWidth = cardView.strokeWidth
        private val baseUseCompatPadding = cardView.useCompatPadding

        fun bind(todo: Todo, depth: Int, isHighlighted: Boolean) {
            textView.text = todo.title
            checkBox.isChecked = todo.isCompleted
            val notSetLabel = itemView.context.getString(R.string.date_time_not_set)
            val startLabel = itemView.context.getString(R.string.start_date_time)
            val endLabel = itemView.context.getString(R.string.end_date_time)
            val startDateText = dateTimeFormatter.formatLabel(
                startLabel,
                todo.startDateTime,
                notSetLabel
            )
            val endDateText = dateTimeFormatter.formatLabel(
                endLabel,
                todo.endDateTime,
                notSetLabel
            )
            startDateView.text = "$startDateText  •  $endDateText"
            endDateView.visibility = View.GONE

            val isNested = depth > 0
            applyNestingVisualStyle(isNested, depth)

            // Apply highlight for nest target
            val normalBackgroundColor = resolveThemeColor(com.google.android.material.R.attr.colorSurface, Color.WHITE)
            val highlightBackgroundColor = resolveThemeColor(
                com.google.android.material.R.attr.colorSecondaryContainer,
                Color.parseColor("#EDF2FF")
            )
            if (isHighlighted) {
                cardView.setCardBackgroundColor(highlightBackgroundColor)
            } else {
                cardView.setCardBackgroundColor(normalBackgroundColor)
                // No item outlines; list is separated by dividers instead.
                cardView.strokeWidth = 0
            }

            val baseTextAlpha = if (isHighlighted) 0.98f else 1f
            textView.alpha = baseTextAlpha
            startDateView.alpha = baseTextAlpha
            endDateView.alpha = baseTextAlpha

            val hasChildren = hasChildrenIds.contains(todo.id)
            if (!hasChildren) {
                // Keep left alignment stable; rows without children still reserve space for the caret.
                collapseButton.visibility = View.INVISIBLE
                collapseButton.setOnClickListener(null)
            } else {
                val isCollapsed = collapsedIds.contains(todo.id)
                collapseButton.visibility = View.VISIBLE
                collapseButton.setImageResource(
                    if (isCollapsed) R.drawable.ic_expand_more else R.drawable.ic_expand_less
                )
                collapseButton.contentDescription = itemView.context.getString(
                    if (isCollapsed) {
                        R.string.content_desc_expand_todo
                    } else {
                        R.string.content_desc_collapse_todo
                    }
                )
                collapseButton.setOnClickListener {
                    onToggleCollapse(todo.id)
                }
            }

            // Apply strikethrough when completed
            if (todo.isCompleted) {
                textView.alpha *= 0.6f
                startDateView.alpha *= 0.65f
                endDateView.alpha *= 0.65f
                textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Update content description for accessibility
            val completionStatus = if (todo.isCompleted) "completed" else "not completed"
            textView.contentDescription = "${todo.title}, $completionStatus"

            checkBox.setOnClickListener { onToggleComplete(todo.id) }
            moreButton.setOnClickListener { showRowMenu(todo) }
            itemView.setOnClickListener { onToggleComplete(todo.id) }
        }

        private fun showRowMenu(todo: Todo) {
            val popup = PopupMenu(itemView.context, moreButton, Gravity.END)
            popup.menuInflater.inflate(R.menu.menu_todo_item, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_add_subtask -> onAddSubtask(todo.id)
                    R.id.action_edit -> onEdit(todo)
                    R.id.action_delete -> onDelete(todo.id)
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            popup.show()
        }

        private fun applyNestingVisualStyle(isNested: Boolean, depth: Int) {
            val density = itemView.context.resources.displayMetrics.density
            fun dp(dp: Int): Int = (dp * density).toInt()

            val lp = (cardView.layoutParams as ViewGroup.MarginLayoutParams)
            if (!isNested) {
                lp.leftMargin = baseCardLayoutParams.leftMargin
                lp.topMargin = baseCardLayoutParams.topMargin
                lp.rightMargin = baseCardLayoutParams.rightMargin
                lp.bottomMargin = baseCardLayoutParams.bottomMargin
                cardView.layoutParams = lp

                cardView.radius = baseCardCornerRadius
                cardView.strokeWidth = baseCardStrokeWidth
                cardView.useCompatPadding = baseUseCompatPadding
                nestingBar.visibility = View.GONE
                nestedDivider.visibility = View.VISIBLE

                // Restore the full "tile" layout for top-level items.
                moreButton.visibility = View.VISIBLE
                moreButton.alpha = 1f
                startDateView.visibility = View.VISIBLE
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                textView.setTypeface(textView.typeface, Typeface.BOLD)
                contentLayout.setPadding(
                    baseContentPaddingLeftPx,
                    baseContentPaddingTopPx,
                    baseContentPaddingRightPx,
                    baseContentPaddingBottomPx
                )
                return
            }

            // Nested items render more like a dense list: no card chrome, smaller padding,
            // and indented with a faint guide bar.
            val indentPx = dp(NESTED_INDENT_BASE_DP + (depth - 1) * NESTED_INDENT_PER_LEVEL_DP)
            lp.leftMargin = baseCardLayoutParams.leftMargin + indentPx
            lp.topMargin = 0
            lp.rightMargin = baseCardLayoutParams.rightMargin
            lp.bottomMargin = 0
            cardView.layoutParams = lp

            cardView.radius = 0f
            cardView.strokeWidth = 0
            cardView.useCompatPadding = false
            nestingBar.visibility = View.VISIBLE
            nestedDivider.visibility = View.VISIBLE

            // Keep actions available for nested items too (edit/delete/add-subtask), matching top-level behavior.
            moreButton.visibility = View.VISIBLE
            moreButton.alpha = 0.85f
            startDateView.visibility = View.GONE
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            textView.setTypeface(textView.typeface, Typeface.NORMAL)
            contentLayout.setPadding(dp(10), dp(6), dp(10), dp(6))
        }

        private fun resolveThemeColor(attributeResId: Int, fallback: Int): Int {
            val typedValue = TypedValue()
            val theme = itemView.context.theme
            return if (theme.resolveAttribute(attributeResId, typedValue, true)) {
                if (typedValue.resourceId != 0) {
                    ContextCompat.getColor(itemView.context, typedValue.resourceId)
                } else {
                    typedValue.data
                }
            } else {
                fallback
            }
        }
    }

    private companion object {
        const val NESTED_INDENT_BASE_DP = 14
        const val NESTED_INDENT_PER_LEVEL_DP = 16
    }
}
