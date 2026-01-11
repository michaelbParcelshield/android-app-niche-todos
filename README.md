<!-- ABOUTME: Quickstart notes for the android-app-niche-todos project. -->
<!-- ABOUTME: Documents Google sign-in setup and credential expectations. -->
# Android App Niche Todos

## Google sign-in setup
- Create an **Android OAuth client** for package `com.example.niche_todos` and add the SHA-1
  fingerprints for both debug and release builds.
- Keep the **Web client ID** in `app/src/main/res/values/strings.xml` as `google_web_client_id`.
- The backend must use the same **Web client ID** in `Auth:Google:ClientId` for audience validation.
- Do not store the client secret in the Android app.

### Finding SHA-1 fingerprints
- Run `./gradlew signingReport` and copy the SHA-1 values under `debug` and `release`.
- Or use Android Studio: Gradle tool window -> `android` -> `signingReport`.
- If you use Play App Signing, add the Play Console **App signing** SHA-1 as well.

### Verifying in Google Cloud Console
- Open Google Cloud Console -> APIs & Services -> Credentials.
- Under **OAuth 2.0 Client IDs**, select the Android client for this app.
- Confirm the package name and that the SHA-1 values from `signingReport` are present.
