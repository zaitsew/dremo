# Google Play: what to paste

Everything Play Console asks for before Dremo's first release, in the order it
asks. Written for **0.1.0 (1)**. The answers come from the code: the manifest asks
for `INTERNET` and `ACCESS_NETWORK_STATE` only (plus the system-protected
`BIND_DREAM_SERVICE` binding on the screensaver service), and the app has no SDK
besides AndroidX, Compose for TV, DataStore, OkHttp and kotlinx.serialization.

## 1. Account, package, signing

- Account: the **Fabrique Futur** organisation account (the one Loco and Iron U ship
  from). It is exempt from the 12-testers-for-14-days rule of personal accounts, so
  the order is internal testing, then production.
- Package `com.fabriquefutur.dremo` (permanent).
- **Upload key** `~/.dremo/android-upload.jks`, password in
  `~/.dremo/android-signing.properties`. Both live outside the repo and are
  created by the owner, not by the build. Play App Signing re-signs with Google's
  key; if the upload key is lost, Play Console, App integrity, "Request upload key reset".
- Build: `./gradlew :app:bundleRelease`, output `app/build/outputs/bundle/release/app-release.aab`.
  `versionCode` rises with every upload.
- Upload: `~/ops/bin/play upload --package com.fabriquefutur.dremo --aab <aab> --track internal`.
  The service account needs "Release to testing tracks" (and "Manage store
  presence" for listing images) on this app. While the app is still a draft, a
  listing commit also needs "Edit and delete draft apps"; without it Play answers 403.

## 2. Create app

| field | value |
|---|---|
| App name | `Dremo: Calm TV Screen` |
| Default language | English (United States), `en-US` |
| App or game | App |
| Free or paid | Free (permanent once published) |

## 3. Store listing

Languages: `en-US` (default), `es-ES`, `ru-RU`; the app speaks the same three.
Category **Personalization** (alternative: **Entertainment**). Contact email: the
Fabrique Futur support address. Website: `https://zaitsew.github.io/dremo/`.

### Texts

One folder per language in [`docs/play/listing/`](play/listing/), in the
fastlane-supply layout: `title.txt` (at most 30), `short_description.txt` (at most 80),
`full_description.txt` (at most 4000). Those files are the source of truth; edit
them, not a copy. Titles: `Dremo: Calm TV Screen`, `Dremo: Pantalla de calma`,
`Dremo: спокойный экран ТВ`.

### Graphics

All in `docs/play/`, drawn by `swift tools/make-banner.swift`.

| asset | size | file |
|---|---|---|
| App icon | 512 x 512 PNG | `icon-512.png` |
| Feature graphic | 1024 x 500 | `feature-graphic-1024x500.png` |
| Android TV banner | 1280 x 720 | `tv-banner-1280x720.png` |
| TV screenshots | 1920 x 1080, 1 to 8 per language | `screenshots/<locale>/` |

Screenshots are in `docs/play/screenshots/<locale>/`. Play still requires 2 to 8
phone screenshots on the default listing, even for a TV-only app; the phone slot
accepts 16:9, so `tools/play-listing.sh` uploads the same TV screenshots there.

Upload texts and graphics for every language with `tools/play-listing.sh`
(add `--dry-run` first).

## 4. Form factors

Console path `/app/4973750218896816751/advanced-distribution?tab=releaseTypes` (`advanced-settings` redirects home), **Add form factor**, Android TV, then opt in. The TV listing
uses the TV banner and TV screenshots. Remove "Phone" if Play added it: the
manifest requires leanback, so phones cannot install it anyway. Play reviews TV
apps against the TV app quality guidelines: D-pad navigation everywhere, no
touch-only controls, a leanback banner, readable text at TV distance, and a
screen that stays on only while the app is doing something visible (Dremo keeps
the screen on only while a scene runs).

## 5. App content

| section | answer |
|---|---|
| Privacy policy | `https://zaitsew.github.io/dremo/privacy` |
| App access | All functionality is available without special access |
| Ads | No, the app contains no ads |
| Content rating | Questionnaire category **"All other app types"**; answer No to every content question. Expected: PEGI 3 / Everyone |
| Target audience | 18 and over (keeps the app out of the Families program; nothing in it targets children) |
| News app | No |
| Data safety | See below |
| Government app | No |
| Financial features | None |
| Health | None |
| Advertising ID | No (the app does not use it) |
| Foreground services, background location, exact alarms | None declared |

### Data safety

Derived from the code. Weather is optional and off until the user picks a city.

- Does your app collect or share any of the required user data types? **Yes, one: approximate location, only when the user picks a city.**
  - Data type: **Location, Approximate location.**
  - Collected: yes (it leaves the device). Shared: no. (The coordinates go to a
    service provider, Open-Meteo, that processes them to return a forecast; Play
    treats that as collection, not sharing, because it is a service provider acting
    for the app.)
  - Is this data processed ephemerally? **Yes.** Dremo does not store it on a
    server of its own; the city is kept on the TV only.
  - Required or optional: **optional.** The app works without it.
  - Purpose: **App functionality.**
  - It is not a precise location, it is not the device's location, and no
    location permission is declared. It is the coordinates of a city the user
    chose from a list.
- Is all user data encrypted in transit? **Yes** (HTTPS only).
- Can users request that data is deleted? The developer holds none. Clearing the
  app's storage or uninstalling removes the chosen city. Account deletion: not
  applicable (no accounts).
- Search text typed in the city search also goes to Open-Meteo's geocoding
  service. It is not stored by Dremo and is not linked to any identifier; declare
  it under the same approximate-location answer if the Console asks.
- No identifiers, no analytics, no crash data, no advertising data are sent.

If the owner prefers the simplest reading, "No data collected" is also defensible
because nothing reaches the developer, but the honest declaration above is what
the code does, and it is the one to use.

## 6. Release order

1. Internal testing track, tester list with the owner's Google account (the one signed in on the TV).
2. The owner opens the opt-in link once, then installs from the TV's Play Store.
3. Production, after a day of use without a blocking problem. Play reviews the
   first production release and the TV form factor (allow up to 7 days).

### Release notes

Every release after 0.1.0 ships its own notes. Put one file per listing language in
`docs/whats-new/<version>/` (`en-US.txt`, `es-ES.txt`, `ru-RU.txt`, at most 500 characters each)
and pass `--notes-dir docs/whats-new/<version>/` to `~/ops/bin/play`. Write one line per change a
user would notice, from the PRs merged since the last release. Never use a generic line such as
"Bug fixes and improvements" (POLICY section 15). `tools/play-listing.sh` uploads listing text and
graphics only, not release notes.

## 7. Screensaver note for reviewers

Dremo declares a `DreamService`. It runs only when the user selects it as the
screensaver (Settings, "Use as screensaver", shows the `adb` command). The
screensaver shows the cached forecast only and never fetches; sound is off by default.

## 8. Submission history

| date | version | state |
|---|---|---|
| 2026-11-01 | 0.1.0 (1) | first production release, sent for review, full rollout |
