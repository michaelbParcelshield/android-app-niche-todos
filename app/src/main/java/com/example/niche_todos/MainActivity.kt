// ABOUTME: Main activity for todo list app
// Manages RecyclerView, ViewModel, and user interactions for todos
package com.example.niche_todos

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import com.example.niche_todos.databinding.ActivityMainBinding
import com.google.android.material.textfield.TextInputEditText
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TodoViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var adapter: TodoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private var hasChildrenIds: Set<String> = emptySet()
    private var collapsedTodoIds: Set<String> = emptySet()
    private lateinit var voiceHeardText: TextView
    private var voicePlayStopMenuItem: MenuItem? = null
    private lateinit var voiceModeBlocker: View
    private lateinit var googleSignInFacade: GoogleSignInFacade
    private lateinit var voiceDrivenModeController: VoiceDrivenModeController
    private var voiceDrivenModeEnabled: Boolean = false
    private var signInLaunched: Boolean = false
    private val googleSignInResultHandler = GoogleSignInResultHandler()
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleSignInResult(result.resultCode, result.data)
    }
    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            voiceDrivenModeEnabled = false
            invalidateOptionsMenu()
            Toast.makeText(this, R.string.voice_driven_permission_denied, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        startVoiceDrivenMode()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val endpointMode = BackendEndpointMode.Cloud
        val repositories = buildRepositories(endpointMode)
        authRepository = repositories.authRepository
        viewModel = ViewModelProvider(
            this,
            TodoViewModelFactory(repositories.todoRepository)
        ).get(
            BackendEndpointViewModelKeys.todoKey(endpointMode),
            TodoViewModel::class.java
        )

        recyclerView = findViewById(R.id.recycler_todos)
        emptyStateText = findViewById(R.id.text_empty_state)
        voiceHeardText = findViewById(R.id.text_voice_heard)
        voiceModeBlocker = findViewById(R.id.view_voice_mode_blocker)
        googleSignInFacade = MainActivityDependencies.googleSignInFacadeFactory(
            this,
            getString(R.string.google_web_client_id)
        )
        voiceDrivenModeController = VoiceDrivenModeController(
            context = this,
            promptTextProvider = { getString(R.string.voice_driven_prompt_check_or_skip) },
            listCompletedTextProvider = { getString(R.string.voice_driven_list_completed) },
            permissionDeniedTextProvider = { getString(R.string.voice_driven_permission_denied) },
            getVisibleTodos = { viewModel.visibleTodos.value.orEmpty() },
            toggleComplete = { id -> viewModel.toggleComplete(id) },
            onUserMessage = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            },
            onListCompleted = {
                runOnUiThread {
                    stopVoiceDrivenMode(reason = "completed")
                }
            },
            onTranscript = { text, isFinal ->
                val prefixText = getString(R.string.voice_driven_heard_prefix, text)
                voiceHeardText.text = prefixText
                // Keep it visible while voice mode is enabled; partial results still show.
                voiceHeardText.visibility = if (voiceDrivenModeEnabled) View.VISIBLE else View.GONE
                voiceHeardText.alpha = if (isFinal) 0.9f else 0.7f
            }
        )

        adapter = TodoAdapter(
            onToggleComplete = { id -> viewModel.toggleComplete(id) },
            onEdit = { todo -> showEditDialog(todo) },
            onDelete = { id -> viewModel.deleteTodo(id) },
            onAddSubtask = { parentId -> showAddSubtaskDialog(parentId) },
            onToggleCollapse = { todoId -> viewModel.toggleCollapsed(todoId) }
        )

        recyclerView.adapter = adapter
        attachDragToReorder()

        viewModel.visibleTodos.observe(this) { visibleTodos ->
            adapter.submitList(visibleTodos, hasChildrenIds, collapsedTodoIds)
            updateEmptyState(visibleTodos.isEmpty())
            voiceDrivenModeController.onVisibleTodosChanged()
        }

        viewModel.hasChildrenIds.observe(this) { ids ->
            hasChildrenIds = ids
            adapter.submitList(
                viewModel.visibleTodos.value.orEmpty(),
                hasChildrenIds,
                collapsedTodoIds
            )
        }

        viewModel.collapsedTodoIds.observe(this) { ids ->
            collapsedTodoIds = ids
            adapter.submitList(
                viewModel.visibleTodos.value.orEmpty(),
                hasChildrenIds,
                collapsedTodoIds
            )
        }

        viewModel.syncError.observe(this) { message ->
            val value = message?.takeIf { it.isNotBlank() } ?: return@observe
            Log.d("VoiceMode", "syncError: $value")
            Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
        }

        binding.fab.setOnClickListener {
            showAddDialog()
        }

        viewModel.refreshTodos()
    }

    override fun onStart() {
        super.onStart()
        ensureSignedIn()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        voicePlayStopMenuItem = menu.findItem(R.id.action_voice_play_stop)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = voicePlayStopMenuItem
        if (item != null) {
            if (voiceDrivenModeEnabled) {
                item.setIcon(R.drawable.ic_stop)
                item.title = getString(R.string.voice_driven_stop_label)
            } else {
                item.setIcon(R.drawable.ic_play)
                item.title = getString(R.string.voice_driven_play_label)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_voice_play_stop -> {
                toggleVoiceDrivenModeFromButton()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toggleVoiceDrivenModeFromButton() {
        if (voiceDrivenModeEnabled) {
            stopVoiceDrivenMode(reason = "user_stop")
            return
        }
        val permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        startVoiceDrivenMode()
    }

    private fun startVoiceDrivenMode() {
        val started = voiceDrivenModeController.start()
        if (!started) {
            voiceDrivenModeEnabled = false
            invalidateOptionsMenu()
            Toast.makeText(this, R.string.voice_driven_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        voiceDrivenModeEnabled = true
        updateVoiceModeUi(enabled = true)
        voiceHeardText.visibility = View.VISIBLE
        invalidateOptionsMenu()
    }

    private fun stopVoiceDrivenMode(reason: String) {
        Log.d("VoiceMode", "stopVoiceDrivenMode reason=$reason")
        voiceDrivenModeEnabled = false
        voiceDrivenModeController.stop()
        updateVoiceModeUi(enabled = false)
        voiceHeardText.visibility = View.GONE
        voiceHeardText.text = ""
        invalidateOptionsMenu()
    }

    private fun updateVoiceModeUi(enabled: Boolean) {
        // Disable interaction while voice mode is running.
        voiceModeBlocker.visibility = if (enabled) View.VISIBLE else View.GONE
        recyclerView.alpha = if (enabled) 0.85f else 1f
        binding.fab.isEnabled = !enabled
        if (enabled) {
            binding.fab.hide()
        } else {
            binding.fab.show()
        }
    }

    override fun onDestroy() {
        voiceDrivenModeController.shutdown()
        super.onDestroy()
    }

    override fun onStop() {
        // Stop when we truly leave the app. Some devices show transient UI for speech recognition.
        if (voiceDrivenModeEnabled) {
            Log.d("VoiceMode", "stopping due to onStop")
            stopVoiceDrivenMode(reason = "onStop")
        }
        super.onStop()
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

    private fun ensureSignedIn() {
        // If we already have backend tokens, just use them.
        val tokens = EncryptedAuthTokenStore(applicationContext).load()
        if (tokens != null) {
            return
        }
        if (signInLaunched) {
            return
        }
        val intent = googleSignInFacade.createSignInIntent()
        if (intent.resolveActivity(packageManager) == null) {
            Log.w("Auth", "Google Sign-In intent cannot be resolved; skipping auto sign-in")
            return
        }
        signInLaunched = true
        googleSignInLauncher.launch(intent)
    }

    private fun showAddDialog() {
        showAddTodoDialog(titleResId = R.string.add_todo, parentId = null)
    }

    private fun showAddTodoDialog(titleResId: Int, parentId: String?) {
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
            .setTitle(titleResId)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val normalizedTitle = TodoTitleValidator.normalizedTitleOrNull(titleInput.text)
                if (normalizedTitle != null) {
                    viewModel.addTodo(normalizedTitle, startDateTime, endDateTime, parentId)
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

    private fun showAddSubtaskDialog(parentId: String) {
        showAddTodoDialog(titleResId = R.string.add_subtask, parentId = parentId)
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
        val dragCallback = TodoDragCallback(
            adapter = adapter,
            onDragComplete = { items -> viewModel.reorderTodos(items) },
            onInvalidDrop = { showCircularReferenceError() },
            canDrag = { collapsedTodoIds.isEmpty() }
        )
        ItemTouchHelper(dragCallback).attachToRecyclerView(recyclerView)
    }

    private fun showCircularReferenceError() {
        Toast.makeText(
            this,
            R.string.circular_reference_error,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun buildRepositories(endpointMode: BackendEndpointMode): BackendRepositoryBundle {
        val endpoints = buildEndpoints(
            healthUrlResId = R.string.backend_health_url_cloud,
            authUrlResId = R.string.backend_auth_url_cloud,
            todosUrlResId = R.string.backend_todos_url_cloud
        )
        return MainActivityDependencies.repositoryFactory(applicationContext, endpoints)
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

    private fun isDebugBuild(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    @VisibleForTesting
    internal fun handleGoogleSignInResult(resultCode: Int, data: Intent?) {
        val outcome = googleSignInResultHandler.resolve(
            resultCode = resultCode,
            idToken = googleSignInFacade.extractIdToken(data)
        )
        when (outcome) {
            GoogleSignInOutcome.Cancelled,
            GoogleSignInOutcome.MissingIdToken -> {
                signInLaunched = false
                Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show()
            }

            is GoogleSignInOutcome.Success -> {
                lifecycleScope.launch {
                    val result = authRepository.exchangeGoogleIdToken(outcome.idToken)
                    when (result) {
                        is AuthResult.Success -> viewModel.refreshTodos()
                        is AuthResult.Failure -> Toast.makeText(
                            this@MainActivity,
                            result.message ?: "Auth failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (result is AuthResult.Failure) {
                        signInLaunched = false
                    }
                }
            }
        }
    }
}
