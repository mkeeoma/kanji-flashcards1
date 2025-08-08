# Kanji Flashcards (Android, Jetpack Compose)

An offline flashcard app for common daily-use Japanese kanji with a simple spaced repetition review flow.

## Build steps (Android Studio)
1. Open Android Studio. Choose **Open** and select this project folder.
2. Let Gradle sync and download dependencies.
3. Connect your OnePlus 11 (enable Developer Options + USB debugging) or use an emulator.
4. From the **Build** menu, choose **Build > Build Bundle(s)/APK(s) > Build APK(s)**.
5. The built APK will be in `app/build/outputs/apk/debug/app-debug.apk` (or release).
6. Install the APK on your device. You may need to allow installs from unknown sources.

## Usage
- Tap the card to flip between Kanji and details (meanings/readings/examples).
- Select a response: Again/Hard/Good/Easy to schedule future reviews (simplified SRS).
- Progress is stored locally via DataStore.

## Notes
- Dataset is a small starter list focused on everyday kanji.
- You can expand `app/src/main/assets/kanji.json` with more entries following the same schema.