# Basic Todo List App - Design Document

**Date:** 2025-11-09
**Status:** Approved

## Overview

Transform the existing "Niche-Todos" Android app template into a basic todo list application with core task management functionality. The app uses MVVM architecture, Material Design 3 components, and focuses on accessibility.

## Requirements

### Core Features
- Add new todos with text description
- Mark todos as complete/incomplete with checkbox
- Edit existing todo text
- Delete todos from the list

### Constraints
- In-memory only (no persistence - todos reset on app close)
- Single screen UI (remove multi-fragment navigation)
- Material Design 3 components and styling
- Full accessibility support (content descriptions, screen readers)

### Success Criteria
- Users can manage todos through add/edit/delete/complete operations
- UI follows Material Design 3 guidelines
- App is accessible to users with disabilities
- Clean MVVM architecture for maintainability and testability

## Architecture Overview

### MVVM Pattern

**TodoViewModel**
- Manages todo list state using `MutableLiveData<List<Todo>>`
- Handles all business logic for todo operations
- Survives configuration changes (screen rotation)
- Methods: `addTodo()`, `updateTodo()`, `deleteTodo()`, `toggleComplete()`

**MainActivity**
- Single activity hosting the complete todo list UI
- Observes LiveData from ViewModel for reactive updates
- Handles user interactions and dialog management
- Simplified from current two-fragment navigation structure

**TodoAdapter**
- RecyclerView adapter for efficient list rendering
- ViewHolder pattern for view recycling
- Each item displays checkbox, todo text, edit button, delete button
- Callbacks to ViewModel for all user actions

**Todo Data Class**
- Properties: `id: String`, `text: String`, `isCompleted: Boolean`
- ID enables reliable tracking for edit/delete operations
- UUID generated for unique IDs

## Component Details

### TodoViewModel

**State Management:**
```kotlin
private val _todos = MutableLiveData<List<Todo>>(emptyList())
val todos: LiveData<List<Todo>> = _todos
```

**Operations:**
- `addTodo(text: String)` - Creates new todo with UUID, appends to list
- `updateTodo(id: String, newText: String)` - Finds todo by ID, updates text
- `deleteTodo(id: String)` - Removes todo from list
- `toggleComplete(id: String)` - Toggles isCompleted flag

All operations create new list copies (immutability) and post to LiveData.

### MainActivity Data Flow

1. Activity observes `viewModel.todos` LiveData
2. On changes, adapter receives new list and updates UI
3. FAB click opens AlertDialog with EditText for new todo
4. Input validation ensures non-empty, trimmed text
5. User confirms → ViewModel operation → LiveData update → UI refresh

### TodoAdapter Item Layout

**Material Components:**
- MaterialCardView container with elevation
- Material CheckBox for completion status
- TextView for todo text
- IconButton for edit (Material edit icon)
- IconButton for delete (Material delete icon)

**Interactions:**
- CheckBox → `onToggleComplete(todoId)` callback
- Edit button → Opens AlertDialog pre-filled with current text
- Delete button → Calls `viewModel.deleteTodo(todoId)` directly

**ViewHolder Binding:**
- Sets checkbox checked state from `todo.isCompleted`
- Displays todo text
- Wires up click listeners for checkbox, edit, delete

### Layout Structure

**Main Screen:**
- Toolbar at top
- RecyclerView fills screen content area
- FloatingActionButton anchored bottom-right

**AlertDialog (Add/Edit):**
- Title: "Add Todo" or "Edit Todo"
- EditText with hint "Enter todo description"
- Negative button: "Cancel"
- Positive button: "Save" (disabled until valid input)

**Empty State:**
- Centered TextView when list is empty
- Message: "No todos yet. Tap + to add one"

## Accessibility Features

### Content Descriptions
- CheckBox: "Mark todo as complete" or "Mark todo as incomplete"
- Todo text: Includes state, e.g., "Buy milk, completed" / "Buy milk, not completed"
- Edit button: "Edit todo: [todo text]"
- Delete button: "Delete todo: [todo text]"
- FAB: "Add new todo"

### Design Guidelines
- Minimum 48dp touch targets for all interactive elements
- Proper focus order: top-to-bottom through list, FAB last
- AlertDialog titles clearly indicate mode (Add vs Edit)
- Color contrast meets WCAG AA standards via Material theme
- TalkBack navigation fully supported

## Error Handling & Edge Cases

### Validation
- Empty text: AlertDialog positive button disabled until text entered
- Whitespace-only: Input trimmed before validation
- Error feedback via Snackbar or inline dialog message

### Edge Cases
- Long todo text: TextView uses `maxLines=2` with `ellipsize="end"`, expands on tap if needed
- Rapid interactions: Synchronous ViewModel operations prevent race conditions
- Configuration changes: ViewModel retains state through rotation
- Empty list: Show empty state message

### Data Integrity
- UUID generation ensures unique IDs
- Immutable list operations prevent shared state issues
- In-memory only means no database corruption concerns

## Testing Strategy

### Unit Tests (ViewModel)
- Test `addTodo()` adds item to list
- Test `updateTodo()` modifies correct item
- Test `deleteTodo()` removes item from list
- Test `toggleComplete()` flips completion status
- Verify LiveData emissions on each operation

### UI Tests (Espresso)
- Verify FAB opens add dialog
- Add todo and confirm it appears in list
- Toggle checkbox and verify visual state change
- Edit todo and confirm text updates
- Delete todo and confirm removal
- Verify empty state displays when no todos

### Accessibility Tests
- Verify all content descriptions present
- Test TalkBack navigation order
- Verify minimum touch target sizes
- Test keyboard navigation if applicable

## Implementation Changes

### Files to Modify
- `MainActivity.kt` - Simplify to single-screen layout, wire up ViewModel
- `activity_main.xml` - Update to single-screen layout with RecyclerView
- `strings.xml` - Add todo-specific strings and content descriptions

### Files to Create
- `Todo.kt` - Data class
- `TodoViewModel.kt` - ViewModel implementation
- `TodoAdapter.kt` - RecyclerView adapter
- `item_todo.xml` - Layout for individual todo items

### Files to Remove
- `FirstFragment.kt` - Not needed in single-screen design
- `SecondFragment.kt` - Not needed in single-screen design
- `fragment_first.xml` - Remove fragment layouts
- `fragment_second.xml` - Remove fragment layouts
- `nav_graph.xml` - No navigation needed

### Dependencies to Add
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.x.x")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.x.x")
```

## Future Considerations (Out of Scope)

These features are explicitly excluded from this initial version:

- Data persistence (Room, SharedPreferences, cloud)
- Todo categories or tags
- Due dates or reminders
- Priority levels
- Search or filter functionality
- Todo reordering via drag-and-drop
- Undo/redo operations
- Settings screen

The design deliberately keeps the app simple and focused on core todo operations.
