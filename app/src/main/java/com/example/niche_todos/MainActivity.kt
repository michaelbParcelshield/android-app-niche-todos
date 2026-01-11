// ABOUTME: Main activity for todo list app
// Manages RecyclerView, ViewModel, and user interactions for todos
package com.example.niche_todos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.Toast
import com.example.niche_todos.databinding.ActivityMainBinding
import com.google.android.material.textfield.TextInputEditText
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.Locale
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TodoViewModel
    private lateinit var backendStatusViewModel: BackendStatusViewModel
    private lateinit var adapter: TodoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var healthCheckButton: Button
    private lateinit var healthStatusText: TextView
    private lateinit var googleSignInButton: Button
    private lateinit var authStatusText: TextView
    private lateinit var backendEndpointSwitch: SwitchCompat
    private lateinit var backendEndpointSelector: BackendEndpointSelector
    private lateinit var googleSignInFacade: GoogleSignInFacade
    private val googleSignInResultHandler = GoogleSignInResultHandler()
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleSignInResult(result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        backendEndpointSelector = BackendEndpointSelector(applicationContext)
        val endpointMode = resolveEndpointMode()
        val repositories = buildRepositories(endpointMode)
        viewModel = ViewModelProvider(
            this,
            TodoViewModelFactory(repositories.todoRepository)
        ).get(
            BackendEndpointViewModelKeys.todoKey(endpointMode),
            TodoViewModel::class.java
        )
        backendStatusViewModel = buildBackendStatusViewModel(repositories, endpointMode)

        recyclerView = findViewById(R.id.recycler_todos)
        emptyStateText = findViewById(R.id.text_empty_state)
        healthCheckButton = findViewById(R.id.button_health_check)
        healthStatusText = findViewById(R.id.text_health_status)
        googleSignInButton = findViewById(R.id.button_google_sign_in)
        authStatusText = findViewById(R.id.text_auth_status)
        backendEndpointSwitch = findViewById(R.id.switch_backend_endpoint)
        googleSignInFacade = MainActivityDependencies.googleSignInFacadeFactory(
            this,
            getString(R.string.google_web_client_id)
        )

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
        backendStatusViewModel.healthStatus.observe(this) { status ->
            renderHealthStatus(status)
        }
        backendStatusViewModel.authStatus.observe(this) { status ->
            renderAuthStatus(status)
            if (status is AuthStatus.Success) {
                viewModel.refreshTodos()
            }
        }

        binding.fab.setOnClickListener {
            showAddDialog()
        }

        healthCheckButton.setOnClickListener {
            backendStatusViewModel.runHealthCheck()
        }

        googleSignInButton.setOnClickListener {
            startGoogleSignIn()
        }

        configureBackendEndpointSwitch()
        viewModel.refreshTodos()
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

    private fun startGoogleSignIn() {
        backendStatusViewModel.startSignIn()
        googleSignInLauncher.launch(googleSignInFacade.createSignInIntent())
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
                val normalizedTitle = TodoTitleValidator.normalizedTitleOrNull(titleInput.text)
                if (normalizedTitle != null) {
                    viewModel.addTodo(normalizedTitle, startDateTime, endDateTime)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.configureTitleInputBehavior(
            titleInput = titleInput,
            selectAllExistingText = false
        )

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
                val normalizedTitle = TodoTitleValidator.normalizedTitleOrNull(titleInput.text)
                if (normalizedTitle != null) {
                    viewModel.updateTodo(todo.id, normalizedTitle, startDateTime, endDateTime)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.configureTitleInputBehavior(
            titleInput = titleInput,
            selectAllExistingText = true
        )

        dialog.show()
    }

    private fun AlertDialog.configureTitleInputBehavior(
        titleInput: TextInputEditText,
        selectAllExistingText: Boolean
    ) {
        val onShowRegistrar = AlertDialogOnShowRegistrar(this)
        onShowRegistrar.setOnShowListener(DialogInterface.OnShowListener {
            configureSaveButtonState(titleInput)
        })
        TitleInputFocusController(
            onShowRegistrar,
            TextInputFocusActions(titleInput),
            AlertDialogSoftInputVisibilityController(this)
        ).selectTitle(selectAllExistingText)
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

    private fun buildBackendStatusViewModel(
        repositories: BackendRepositoryBundle,
        endpointMode: BackendEndpointMode
    ): BackendStatusViewModel {
        val factory = BackendStatusViewModelFactory(repositories)
        return ViewModelProvider(this, factory).get(
            BackendEndpointViewModelKeys.statusKey(endpointMode),
            BackendStatusViewModel::class.java
        )
    }

    private fun buildRepositories(endpointMode: BackendEndpointMode): BackendRepositoryBundle {
        val endpoints = if (endpointMode == BackendEndpointMode.Cloud) {
            buildEndpoints(
                healthUrlResId = R.string.backend_health_url_cloud,
                authUrlResId = R.string.backend_auth_url_cloud,
                todosUrlResId = R.string.backend_todos_url_cloud
            )
        } else {
            buildEndpoints(
                healthUrlResId = R.string.backend_health_url,
                authUrlResId = R.string.backend_auth_url,
                todosUrlResId = R.string.backend_todos_url
            )
        }
        return MainActivityDependencies.repositoryFactory(applicationContext, endpoints)
    }

    private fun resolveEndpointMode(): BackendEndpointMode {
        return if (isDebugBuild() && !backendEndpointSelector.useCloud()) {
            BackendEndpointMode.Local
        } else {
            BackendEndpointMode.Cloud
        }
    }

    private fun buildEndpoints(
        healthUrlResId: Int,
        authUrlResId: Int,
        todosUrlResId: Int
    ): BackendEndpoints {
        return BackendEndpoints(
            healthUrl = URL(getString(healthUrlResId)),
            authUrl = URL(getString(authUrlResId)),
            todosUrl = URL(getString(todosUrlResId))
        )
    }

    private fun configureBackendEndpointSwitch() {
        if (!isDebugBuild()) {
            backendEndpointSwitch.visibility = View.GONE
            return
        }
        backendEndpointSwitch.visibility = View.VISIBLE
        backendEndpointSwitch.setOnCheckedChangeListener(null)
        backendEndpointSwitch.isChecked = backendEndpointSelector.useCloud()
        backendEndpointSwitch.setOnCheckedChangeListener { _, isChecked ->
            backendEndpointSelector.setUseCloud(isChecked)
            recreate()
        }
    }

    private fun isDebugBuild(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun renderHealthStatus(status: HealthStatus) {
        healthStatusText.text = when (status) {
            HealthStatus.Idle -> getString(R.string.health_check_initial)
            HealthStatus.InProgress -> getString(R.string.health_check_in_progress)
            is HealthStatus.Success -> getString(R.string.health_check_success, status.statusCode)
            is HealthStatus.Failure -> {
                val details = status.message?.takeIf { it.isNotBlank() }
                if (details == null) {
                    getString(R.string.health_check_failure)
                } else {
                    getString(R.string.health_check_failure_with_details, details)
                }
            }
        }
    }

    private fun renderAuthStatus(status: AuthStatus) {
        googleSignInButton.isEnabled = status !is AuthStatus.Authenticating
        authStatusText.text = when (status) {
            AuthStatus.SignedOut -> getString(R.string.auth_status_initial)
            AuthStatus.SigningIn -> getString(R.string.auth_status_signing_in)
            AuthStatus.Authenticating -> getString(R.string.auth_status_authenticating)
            AuthStatus.MissingIdToken -> getString(R.string.auth_status_missing_id_token)
            is AuthStatus.Success -> getString(R.string.auth_status_success, status.statusCode)
            is AuthStatus.Failure -> {
                val details = status.message?.takeIf { it.isNotBlank() }
                if (details == null) {
                    getString(R.string.auth_status_failure)
                } else {
                    getString(R.string.auth_status_failure_with_details, details)
                }
            }
        }
    }

    @VisibleForTesting
    internal fun handleGoogleSignInResult(resultCode: Int, data: Intent?) {
        val outcome = googleSignInResultHandler.resolve(
            resultCode = resultCode,
            idToken = googleSignInFacade.extractIdToken(data)
        )
        when (outcome) {
            GoogleSignInOutcome.Cancelled -> backendStatusViewModel.reportSignInFailure()
            GoogleSignInOutcome.MissingIdToken -> backendStatusViewModel.reportMissingIdToken()
            is GoogleSignInOutcome.Success ->
                backendStatusViewModel.authenticateWithGoogle(outcome.idToken)
        }
    }
}
