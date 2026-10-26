# Quote Vomit

An Android home-screen widget that works like a game loading screen: every hour it shows
a random quote, useless fact, bit of Wikipedia trivia, or fake game tip, on a pretty, unrelated photo.

## What it does
- Every hour it rolls a random card: a **Wikiquote** quote, a **useless fact**, **Wikipedia** "did you know" trivia, a fake **loading-screen tip**, or a rude old quote
- Each quote gets a random **pretty, totally unrelated photo** behind it (from Lorem Picsum), or a pastel gradient when offline
- **Tap the widget** to reroll instantly
- Offline? Falls back to built-in tips and rude old quotes ("What, you egg!")
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
| `QuoteRepository.kt` | Rolls the random card: Wikiquote, useless facts, Wikipedia, tips, and the rude stash |
| `BackgroundRepository.kt` | Downloads the pretty background photo, rounds its corners, picks offline gradients |
| `QuoteWorker.kt` | The hourly background job (WorkManager) |
| `QuoteWidget.kt` | The widget: draws the quote, handles taps |
| `MainActivity.kt` | The app screen with a "Vomit me a quote" button |

## Ideas to extend it
- Add your own lines to `tips` or `rudeStash` in `QuoteRepository.kt`, or change the odds in `roll()`
- Change `1, TimeUnit.HOURS` in `QuoteWorker.kt` (Android won't go below 15 minutes)
- Restrict quotes to a category, like only playwrights or only ancient philosophers
