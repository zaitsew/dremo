# Dremo

An ad-free, open-source calm screen for Android TV and Google TV. When the TV
is on and nobody is watching, Dremo shows a big clock or a slow ambience scene
(rain on a window, a fireplace, an aurora, a night sky, ocean waves, a lava
lamp) with a soft sound that the app itself synthesizes. No ads, no accounts, no
tracking, and no video or audio files: every picture and sound is generated in
code, so there are no asset licences either.

Built for the living room: large type, full D-pad control, night dimming and
pixel shifting to be kind to the panel. English, Spanish and Russian.

The name comes from the Russian "дремать" (to doze) and "dream", the name of
Android's screensaver API.

## Quick start

```sh
git clone https://github.com/zaitsew/dremo && cd dremo
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # your Android SDK path
./gradlew :app:assembleDebug                                   # needs JDK 17 or newer
adb connect <tv-ip>:5555                                       # TV: Settings > Developer options > Network debugging
adb install -r app/build/outputs/apk/debug/app-debug.apk       # then open Dremo from the TV launcher
```

## What it does

- **Clocks:** big digital, flip, minimal analog and a word clock (English and
  Spanish). Date line and an optional weather line.
- **Ambience:** six scenes drawn live on a Compose canvas, at a modest particle
  count so a Mi Box S-class TV keeps 30 fps or more.
- **Sound:** white, pink and brown noise, rain, a crackling fire and ocean swell,
  generated into an `AudioTrack` in real time. Each scene has a default sound;
  volume has ten steps.
- **Sleep timer:** 15, 30, 60 or 90 minutes. The sound fades over 30 seconds and
  the screen turns into a dim clock.
- **Screen care:** the picture shifts a few pixels every minute, and a night
  schedule (23:00 to 07:00 by default) dims the screen to about 40 percent.
  The screen is kept on only while a scene runs.
- **Screensaver:** a `DreamService`. Settings > Use as screensaver shows the
  `adb` command that selects it and the command that restores the default.
- **Weather (optional):** pick a city; the forecast comes from Open-Meteo.

## How it works

| Piece | Where |
|---|---|
| Pure logic (sleep timer, night schedule, pixel shift, word clock, clock text) | `app/src/main/kotlin/com/fabriquefutur/dremo/core`, `clock` |
| Sound synthesis and the `AudioTrack` writer | `audio/` |
| Scenes: preallocated particles, pre-rendered soft sprites, no per-frame allocation | `scene/` |
| Weather: Open-Meteo client, parser, cache, refresh policy | `weather/` |
| Settings in DataStore | `data/` |
| Screens (Compose for TV) and the screensaver | `ui/`, `dream/` |

Scenes run a frame loop capped at their frame rate and invalidate only the
draw phase, so recomposition stays out of the hot path. The weather forecast is
always fetched in Celsius and converted when shown.

## Privacy

No analytics, no ads, no crash-reporting SDK, no accounts, and no Google Play
Services. The only network traffic is the optional weather: when you pick a
city, its coordinates go to Open-Meteo, which needs no key. The app asks for
no location permission. Settings and the last forecast stay on the TV. See
[docs/privacy.md](docs/privacy.md).

Weather data by [Open-Meteo.com](https://open-meteo.com/) (CC BY 4.0).

## Develop

```sh
export JAVA_HOME=~/.local/jdk-21.0.12.1+1/Contents/Home   # or any JDK 17+
./check.sh            # unit tests, lint, debug APK
./check.sh usage      # prints telemetry=none
swift tools/make-banner.swift   # redraws the banner, icon and store graphics (macOS)
```

The Play listing texts are in [docs/play/listing](docs/play/listing); the
Play Console answers are in [docs/PLAY-STORE.md](docs/PLAY-STORE.md); the plan
is in [docs/ROADMAP.md](docs/ROADMAP.md).

## License

MIT, see [LICENSE](LICENSE). Copyright Fabrique Futur.
