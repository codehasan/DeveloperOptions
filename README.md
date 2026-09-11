<p align="center">
  <img src="images/logo.svg" alt="Developer Options logo" width="120" height="120">
</p>

<h1 align="center">Developer Options</h1>

A tiny Android app that opens **Developer options** in one tap.

- Already enabled? It opens the screen directly.
- Not yet? It shows the exact steps for your phone brand (which *About* screen
  to open, which field to tap 7 times).

No permissions, no network, no analytics.

## Building

Needs the Android SDK and JDK 11+.

```bash
git clone https://github.com/codehasan/DeveloperOptions.git
cd DeveloperOptions
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

### Release builds (optional)

Only if you're signing your own build:

```bash
cp app/keystore.properties.example app/keystore.properties
# fill in your keystore details, then:
./gradlew assembleRelease
```

`keystore.properties` and `*.jks` are gitignored — keep them out of git. If the
file is missing, signing is just skipped.

## Project layout

- `app/src/main/java/io/github/codehasan/developeroptions/`
  - `MainActivity.kt` — entry point
  - `DevOptions.kt` — intent resolution and per-brand hints
  - `OsDetection.kt` — manufacturer / OS detection
  - `DisabledActivity.kt` — the how-to screen

## Not working on your phone?

The intents are hardcoded per brand, so a device that isn't covered yet may not
open directly. To get it added:

- **If you can dig in:** find the right activity class in your Settings app and
  open a PR.
- **Otherwise:** [open an issue](https://github.com/codehasan/DeveloperOptions/issues)
  with your **brand, model, and Android version**. Back up your Settings APK,
  upload it somewhere public, and link it. A screen recording of you enabling
  Developer options by hand helps a lot.

## Contributing

Pull Requests are welcome — especially new brands. See `strings.xml` and `DevOptions.kt`.

## License

[MIT](LICENSE)
