ABOUTME: Repository guidelines for android-app-niche-todos contributors.
ABOUTME: Explains structure, workflows, and collaboration expectations for this project.
# Repository Guidelines

## Project Structure & Module Organization
- `app/` houses the single Android module; Kotlin sources live in `app/src/main/java/com/example/niche_todos` with ViewModel, adapter, and formatter classes grouped together.
- XML resources and layouts (for example `app/src/main/res/layout/dialog_todo.xml`) live under `app/src/main/res`, while configuration stays in `app/src/main/AndroidManifest.xml`.
- JVM unit tests target domain and ViewModel logic in `app/src/test/java/com/example/niche_todos`; mirror any package you add so tests sit beside production classes.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` builds a debuggable APK against the SDK 36 toolchain with view binding enabled.
- `./gradlew testDebugUnitTest` runs the JUnit/InstantTaskExecutorRule suite (see `TodoViewModelTest`) locally on the JVM.
- `./gradlew lint` and `./gradlew connectedDebugAndroidTest` cover static analysis and device tests; run the latter only when an emulator or device is attached.

## Coding Style & Naming Conventions
- Kotlin code uses 4-space indentation, expression bodies when short, and immutable `val`s whenever practical.
- Keep file-level ABOUTME comments (see `MainActivity.kt`) and describe WHAT/WHY above complex code blocks rather than inline narration.
- Name UI elements after their roles (`TodoAdapter`, `TodoDateTimeFormatter`); stick with the `com.example.niche_todos` package and prefer descriptive sealed classes (for example `TodoProperty`).

## Testing Guidelines
- Write a failing test beside each feature before implementation, exercising real LiveData and formatting logic rather than mocks.
- Favor deterministic inputs (use fixed `LocalDateTime.of(...)` values as in existing tests) and assert both model state and derived properties.
- Do not delete flaky tests; isolate issues with targeted runs such as `./gradlew testDebugUnitTest --tests com.example.niche_todos.TodoViewModelTest`.

## Commit & Pull Request Guidelines
- Follow the concise, imperative voice already present in history (`Add auth plan`, `Add Android CI workflow`), keeping one logical change per commit.
- PRs must state intent, list validation commands, and link any Jira issue; include emulator screenshots or logs whenever UI changes are visible.
- Use CI-friendly branches (`feature/...`, `ci-...`) and keep `.devcontainer/.secrets` out of git; rerun Gradle tasks locally before requesting review.

## Security & Configuration Tips
- Secrets load automatically from `/home/node/.env-secrets`; never commit or echo them in logs, and treat `GITHUB_TOKEN`, `AZURE_DEVOPS_PAT`, and database credentials as read-only.
- Use the preconfigured `gh` and `az` CLIs to verify authentication (`gh auth status`, `az devops configure --list`) before running release automation or scripts that touch external services.
