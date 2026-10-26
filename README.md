# Quote Vomit

An Android home-screen widget that shows a random quote from history every hour.
Not inspirational. On purpose.

## What it does
- Grabs a random page from **Wikiquote** every hour and picks a random quote from it
- Each quote gets a random **pretty, totally unrelated photo** behind it (from Lorem Picsum), or a pastel gradient when offline
- About 1 in 3 quotes comes from a built-in stash of rude, petty lines from history
- **Tap the widget** to reroll instantly
- Offline? Falls back to the stash ("What, you egg!")
- Keeps working after your phone restarts

## How to run it
1. Install **Android Studio** (free) on your laptop.
2. Unzip this folder, then in Android Studio choose **File → Open** and pick the `QuoteVomit` folder.
3. Let it sync. If it says the Gradle wrapper is missing or asks to upgrade, accept the suggestion.
4. On your phone, turn on **Developer options** (Settings → About phone → tap *Build number* 7 times),
   then turn on **USB debugging**.
5. Plug your phone in and press the green **Run ▶** button.
6. Long-press your home screen → **Widgets** → **Quote Vomit** → drag it on.

## Want a shareable APK instead?
In Android Studio: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
Copy the `.apk` to your phone and open it to install.

## Files worth reading
| File | What it does |
|---|---|
| `QuoteRepository.kt` | Fetches from Wikiquote, cleans the wiki markup, holds the backup stash |
| `BackgroundRepository.kt` | Downloads the pretty background photo, rounds its corners, picks offline gradients |
| `QuoteWorker.kt` | The hourly background job (WorkManager) |
| `QuoteWidget.kt` | The widget: draws the quote, handles taps |
| `MainActivity.kt` | The app screen with a "Vomit me a quote" button |

## Ideas to extend it
- Add your own favorites to the `stash` list in `QuoteRepository.kt`
- Change `1, TimeUnit.HOURS` in `QuoteWorker.kt` (Android won't go below 15 minutes)
- Restrict quotes to a category, like only playwrights or only ancient philosophers
