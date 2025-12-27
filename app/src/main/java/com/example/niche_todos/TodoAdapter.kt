// ABOUTME: RecyclerView adapter for displaying todo items
// Handles binding todo data to item views and user interaction callbacks
package com.example.niche_todos

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TodoAdapter(
    private val onToggleComplete: (String) -> Unit,
    private val onEdit: (Todo) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private var todos: List<Todo> = emptyList()

    fun submitList(newTodos: List<Todo>) {
        todos = newTodos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(todos[position])
    }

    override fun getItemCount(): Int = todos.size

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_completed)
        private val textView: TextView = itemView.findViewById(R.id.text_todo)
        private val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)

        fun bind(todo: Todo) {
            textView.text = todo.title
            checkBox.isChecked = todo.isCompleted

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
        }
    }
}
