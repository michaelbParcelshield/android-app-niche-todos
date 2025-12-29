// ABOUTME: Main activity for todo list app
// Manages RecyclerView, ViewModel, and user interactions for todos
package com.example.niche_todos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.Toast
import com.example.niche_todos.databinding.ActivityMainBinding
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.Locale

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

    private fun formatDateTime(dateTime: LocalDateTime?): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)
        return dateTime?.format(formatter) ?: getString(R.string.date_time_not_set)
    }

    private fun showDateTimePicker(
        initialDateTime: LocalDateTime?,
        minDateTime: LocalDateTime? = null,
        onSelected: (LocalDateTime) -> Unit
    ) {
        val seedDateTime = initialDateTime ?: minDateTime ?: LocalDateTime.now()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val selectedDateTime = LocalDateTime.of(
                            selectedDate,
                            LocalTime.of(hour, minute)
                        )
                        if (minDateTime != null && selectedDateTime.isBefore(minDateTime)) {
                            Toast.makeText(
                                this,
                                R.string.end_before_start_error,
                                Toast.LENGTH_SHORT
                            ).show()
                            showDateTimePicker(minDateTime, minDateTime, onSelected)
                        } else {
                            onSelected(selectedDateTime)
                        }
                    },
                    seedDateTime.hour,
                    seedDateTime.minute,
                    DateFormat.is24HourFormat(this)
                ).show()
            },
            seedDateTime.year,
            seedDateTime.monthValue - 1,
            seedDateTime.dayOfMonth
        )
        minDateTime?.let { min ->
            val minDateMillis = min.toLocalDate()
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            datePickerDialog.datePicker.minDate = minDateMillis
        }
        datePickerDialog.show()
    }

    private fun setupStartButtonHandler(
        button: Button,
        startValue: TextView,
        endValue: TextView,
        currentStart: () -> LocalDateTime?,
        currentEnd: () -> LocalDateTime?,
        onStartUpdated: (LocalDateTime) -> Unit,
        onEndUpdated: (LocalDateTime) -> Unit
    ) {
        button.setOnClickListener {
            val previousStart = currentStart()
            val previousEnd = currentEnd()
            showDateTimePicker(previousStart) { selected ->
                onStartUpdated(selected)
                startValue.text = formatDateTime(selected)
                val adjustedEnd = DateRangeAdjuster.shiftEndKeepingDuration(
                    previousStart,
                    previousEnd,
                    selected
                )
                onEndUpdated(adjustedEnd)
                endValue.text = formatDateTime(adjustedEnd)
            }
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_todo, null)
        val titleInput: TextInputEditText = dialogView.findViewById(R.id.input_title)
        val startButton: Button = dialogView.findViewById(R.id.button_start)
        val endButton: Button = dialogView.findViewById(R.id.button_end)
        val startValue: TextView = dialogView.findViewById(R.id.text_start_value)
        val endValue: TextView = dialogView.findViewById(R.id.text_end_value)

        val (defaultStart, defaultEnd) = viewModel.defaultDateRange()

        var startDateTime: LocalDateTime? = defaultStart
        var endDateTime: LocalDateTime? = defaultEnd

        startValue.text = formatDateTime(startDateTime)
        endValue.text = formatDateTime(endDateTime)

        setupStartButtonHandler(
            startButton,
            startValue,
            endValue,
            { startDateTime },
            { endDateTime },
            { startDateTime = it },
            { endDateTime = it }
        )

        endButton.setOnClickListener {
            showDateTimePicker(endDateTime ?: startDateTime, minDateTime = startDateTime) { selected ->
                endDateTime = selected
                endValue.text = formatDateTime(selected)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.add_todo)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val text = titleInput.text.toString()
                if (text.isNotBlank()) {
                    viewModel.addTodo(text, startDateTime, endDateTime)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditDialog(todo: Todo) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_todo, null)
        val titleInput: TextInputEditText = dialogView.findViewById(R.id.input_title)
        val startButton: Button = dialogView.findViewById(R.id.button_start)
        val endButton: Button = dialogView.findViewById(R.id.button_end)
        val startValue: TextView = dialogView.findViewById(R.id.text_start_value)
        val endValue: TextView = dialogView.findViewById(R.id.text_end_value)

        var startDateTime: LocalDateTime? = todo.startDateTime
        var endDateTime: LocalDateTime? = todo.endDateTime

        titleInput.setText(todo.title)
        startValue.text = formatDateTime(startDateTime)
        endValue.text = formatDateTime(endDateTime)

        setupStartButtonHandler(
            startButton,
            startValue,
            endValue,
            { startDateTime },
            { endDateTime },
            { startDateTime = it },
            { endDateTime = it }
        )

        endButton.setOnClickListener {
            showDateTimePicker(endDateTime ?: startDateTime, minDateTime = startDateTime) { selected ->
                endDateTime = selected
                endValue.text = formatDateTime(selected)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_todo)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val text = titleInput.text.toString()
                if (text.isNotBlank()) {
                    viewModel.updateTodo(todo.id, text, startDateTime, endDateTime)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
