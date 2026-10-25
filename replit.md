# SO2 Match Tracker

Нативный Android-дневник матчей Standoff 2 с локальным хранением, статистикой и резервными копиями.

## Run & Operate

- `pnpm --filter @workspace/so2-match-tracker run dev` — run the Replit project-information preview
- `pnpm run typecheck` — check the workspace TypeScript packages
- `cd artifacts/so2-match-tracker/android && ./gradlew :app:assembleDebug` — build the Android APK on a computer with JDK 17 and Android SDK 36
- Android-specific setup, build, and installation instructions are in `artifacts/so2-match-tracker/android/README.md`

## Stack

- Android: Kotlin, Jetpack Compose, Room
- Minimum Android version: Android 14 (API 34); target SDK: Android 16 (API 36)
- Replit project shell: pnpm workspace; the Expo artifact preview is informational and is not the native Android app

## Where things live

- `artifacts/so2-match-tracker/android/` — complete native Android source project
- `artifacts/so2-match-tracker/android/app/src/main/java/com/so2matchtracker/data/` — Room entity, DAO, database, and JSON backup validation
- `artifacts/so2-match-tracker/android/app/src/main/java/com/so2matchtracker/domain/` — match categories and statistics calculations
- `artifacts/so2-match-tracker/android/app/src/main/java/com/so2matchtracker/ui/` — Compose screens for entry, history, analytics, and backup
- `artifacts/so2-match-tracker/android/README.md` — build and sideload instructions

## Architecture decisions

- Match records stay on-device in Room; the app does not require an API, account, or network permission.
- Imports validate the whole JSON file before insertion and only add IDs that are not already stored.
- Room's destructive migration fallback is intentionally disabled; future schema changes must use explicit migrations.

## Product

Record match outcomes and K/D inputs quickly, then review history, aggregate statistics, category frequencies, and period-based trends.

## User preferences

- Keep the app in Russian, local-only, and focused on quick personal match analysis.

## Gotchas

- The Replit container has no Java/JDK or Android SDK, so an APK and on-device installation test cannot be produced here.
- The Replit preview is an informational screen; native Compose UI is only available after building and running the Android project.
- Preserve Room data with explicit migrations when the database schema changes.

## Pointers

- See `artifacts/so2-match-tracker/android/README.md` for the exact APK build steps and current verification limits.
