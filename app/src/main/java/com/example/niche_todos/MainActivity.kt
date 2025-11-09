// ABOUTME: Main activity for todo list app
// Manages RecyclerView, ViewModel, and user interactions for todos
package com.example.niche_todos

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.example.niche_todos.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this)[TodoViewModel::class.java]

        recyclerView = findViewById(R.id.recycler_todos)
        emptyStateText = findViewById(R.id.text_empty_state)

        adapter = TodoAdapter(
            onToggleComplete = { id -> viewModel.toggleComplete(id) },
            onEdit = { todo -> showEditDialog(todo) },
            onDelete = { id -> viewModel.deleteTodo(id) }
        )

        recyclerView.adapter = adapter

        viewModel.todos.observe(this) { todos ->
            adapter.submitList(todos)
            updateEmptyState(todos.isEmpty())
        }

        binding.fab.setOnClickListener {
            showAddDialog()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerView.visibility = View.GONE
            emptyStateText.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE
        }
    }

    private fun showAddDialog() {
        val editText = EditText(this).apply {
            hint = getString(R.string.todo_hint)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.add_todo)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                val text = editText.text.toString()
                if (text.isNotBlank()) {
                    viewModel.addTodo(text)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditDialog(todo: Todo) {
        val editText = EditText(this).apply {
            setText(todo.text)
            hint = getString(R.string.todo_hint)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_todo)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                val text = editText.text.toString()
                if (text.isNotBlank()) {
                    viewModel.updateTodo(todo.id, text)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}