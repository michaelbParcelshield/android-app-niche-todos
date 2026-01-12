// ABOUTME: RecyclerView adapter for displaying todo items
// Handles binding todo data to item views and user interaction callbacks
package com.example.niche_todos

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class TodoAdapter(
    private val onToggleComplete: (String) -> Unit,
    private val onEdit: (Todo) -> Unit,
    private val onDelete: (String) -> Unit,
    private val onAddSubtask: (String) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private val todos: MutableList<Todo> = mutableListOf()
    private var highlightedPosition: Int = RecyclerView.NO_POSITION
    private var depthMap: Map<String, Int> = emptyMap()

    fun submitList(newTodos: List<Todo>) {
        todos.clear()
        todos.addAll(newTodos)
        depthMap = buildDepthMap(newTodos)
        notifyDataSetChanged()
    }

    private fun buildDepthMap(todoList: List<Todo>): Map<String, Int> {
        val todoById = todoList.associateBy { it.id }
        val depths = mutableMapOf<String, Int>()

        fun computeDepth(todo: Todo): Int {
            if (depths.containsKey(todo.id)) {
                return depths[todo.id]!!
            }
            val parentId = todo.parentId
            val depth = if (parentId == null) {
                0
            } else {
                val parent = todoById[parentId]
                if (parent != null) computeDepth(parent) + 1 else 0
            }
            depths[todo.id] = depth
            return depth
        }

        for (todo in todoList) {
            computeDepth(todo)
        }
        return depths
    }

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
        private val cardView: View = itemView
        private val contentLayout: View = itemView.findViewById(R.id.content_layout)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_completed)
        private val textView: TextView = itemView.findViewById(R.id.text_todo)
        private val startDateView: TextView = itemView.findViewById(R.id.text_start_date)
        private val endDateView: TextView = itemView.findViewById(R.id.text_end_date)
        private val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)
        private val addSubtaskButton: ImageButton = itemView.findViewById(R.id.button_add_subtask)
        private val dateTimeFormatter = TodoDateTimeFormatter()
        private val basePaddingPx = contentLayout.paddingLeft

        fun bind(todo: Todo, depth: Int, isHighlighted: Boolean) {
            textView.text = todo.title
            checkBox.isChecked = todo.isCompleted
            val notSetLabel = itemView.context.getString(R.string.date_time_not_set)
            val startLabel = itemView.context.getString(R.string.start_date_time)
            val endLabel = itemView.context.getString(R.string.end_date_time)
            startDateView.text = dateTimeFormatter.formatLabel(
                startLabel,
                todo.startDateTime,
                notSetLabel
            )
            endDateView.text = dateTimeFormatter.formatLabel(
                endLabel,
                todo.endDateTime,
                notSetLabel
            )

            // Apply indentation based on nesting depth
            val density = itemView.context.resources.displayMetrics.density
            val indentPx = (depth * INDENT_PER_LEVEL_DP * density).toInt()
            contentLayout.setPadding(basePaddingPx + indentPx, contentLayout.paddingTop, contentLayout.paddingRight, contentLayout.paddingBottom)

            // Apply highlight for nest target
            if (isHighlighted) {
                cardView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.nest_highlight))
            } else {
                cardView.setBackgroundColor(Color.TRANSPARENT)
            }

            // Apply strikethrough when completed
            if (todo.isCompleted) {
                textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Update content description for accessibility
            val completionStatus = if (todo.isCompleted) "completed" else "not completed"
            textView.contentDescription = "${todo.title}, $completionStatus"

            checkBox.setOnClickListener {
                onToggleComplete(todo.id)
            }

            editButton.setOnClickListener {
                onEdit(todo)
            }

            deleteButton.setOnClickListener {
                onDelete(todo.id)
            }

            addSubtaskButton.setOnClickListener {
                onAddSubtask(todo.id)
            }
        }
    }

    private companion object {
        const val INDENT_PER_LEVEL_DP = 24
    }
}
