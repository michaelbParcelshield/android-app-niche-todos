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
import androidx.recyclerview.widget.ItemTouchHelper
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
        attachDragToReorder()

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

        fun resolvedSeedTimeForDate(selectedDate: LocalDate): LocalTime {
            val baseSeedTime = seedDateTime.toLocalTime()
            val minTime = if (minDateTime != null && selectedDate == minDateTime.toLocalDate()) {
                minDateTime.toLocalTime()
            } else {
                null
            }
            return if (minTime != null && baseSeedTime.isBefore(minTime)) {
                minTime
            } else {
                baseSeedTime
            }
        }

        fun showTimePicker(selectedDate: LocalDate, seedTime: LocalTime) {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    val selectedDateTime = LocalDateTime.of(
                        selectedDate,
                        LocalTime.of(hour, minute)
                    )
                    when (val validation = DateTimeSelectionValidator.validate(
                        selectedDateTime,
                        minDateTime
                    )) {
                        is DateTimeSelectionValidator.ValidationResult.Valid -> {
                            onSelected(validation.dateTime)
                        }

                        is DateTimeSelectionValidator.ValidationResult.Invalid -> {
                            showEndBeforeStartError()
                            val retrySeed = if (
                                validation.minimumDateTime.toLocalDate() == selectedDate
                            ) {
                                validation.minimumDateTime.toLocalTime()
                            } else {
                                seedTime
                            }
                            showTimePicker(selectedDate, retrySeed)
                        }
                    }
                },
                seedTime.hour,
                seedTime.minute,
                DateFormat.is24HourFormat(this)
            ).show()
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                showTimePicker(selectedDate, resolvedSeedTimeForDate(selectedDate))
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

    private fun showEndBeforeStartError() {
        Toast.makeText(
            this,
            R.string.end_before_start_error,
            Toast.LENGTH_SHORT
        ).show()
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

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_todo)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val text = titleInput.text.toString()
                if (text.isNotBlank()) {
                    viewModel.addTodo(text, startDateTime, endDateTime)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        TitleInputFocusController(
            AlertDialogOnShowRegistrar(dialog),
            TextInputFocusActions(titleInput),
            AlertDialogSoftInputVisibilityController(dialog)
        ).selectTitle(selectAllExistingText = false)

        dialog.show()
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

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.edit_todo)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val text = titleInput.text.toString()
                if (text.isNotBlank()) {
                    viewModel.updateTodo(todo.id, text, startDateTime, endDateTime)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        TitleInputFocusController(
            AlertDialogOnShowRegistrar(dialog),
            TextInputFocusActions(titleInput),
            AlertDialogSoftInputVisibilityController(dialog)
        ).selectTitle(selectAllExistingText = true)

        dialog.show()
    }

    private fun attachDragToReorder() {
        val touchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                if (fromPosition == RecyclerView.NO_POSITION ||
                    toPosition == RecyclerView.NO_POSITION
                ) {
                    return false
                }
                adapter.moveItem(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Swipe disabled
            }

            override fun isLongPressDragEnabled(): Boolean = true

            override fun isItemViewSwipeEnabled(): Boolean = false

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewModel.reorderTodos(adapter.currentItems())
            }
        }
        ItemTouchHelper(touchHelperCallback).attachToRecyclerView(recyclerView)
    }
}
