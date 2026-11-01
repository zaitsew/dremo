# Dremo privacy policy

Last updated: 1 November 2026

Dremo is a calm clock and ambience screen for Android TV. It is open source:
<https://github.com/zaitsew/dremo>.

## What data Dremo collects

None. Dremo has no accounts, no analytics, no advertising, and no
crash-reporting service. The developer receives no data from the app.

## What leaves your TV

Only the optional weather. If you pick a city in Settings and leave the weather
line on, Dremo asks Open-Meteo (<https://open-meteo.com/>) for the forecast of
that city every 30 minutes while a scene is on screen:

- When you search for a city, the text you type is sent to Open-Meteo's
  geocoding service to get a list of matching places.
- When it fetches the forecast, Dremo sends the latitude and longitude of the
  city you chose. This is the location of a city you picked, not your own
  location. The app asks for no location permission and cannot read your
  position.

Like any web server, Open-Meteo sees the IP address the request comes from, and
its own privacy policy applies to that traffic. Dremo sends no identifier and no
device data with the requests. If you do not pick a city, Dremo makes no network
request at all.

## What stays on your TV

- **Settings:** clock format, temperature unit, chosen city, sound, volume, night
  schedule and screensaver scene. Clearing the app's storage in the Android TV
  settings, or uninstalling Dremo, deletes them.
- **The last forecast,** so the weather line shows at once when the app opens.

## Weather data

Weather data by [Open-Meteo.com](https://open-meteo.com/), licensed
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).

## Permissions

- `INTERNET` and `ACCESS_NETWORK_STATE`: to fetch the weather.
- The screensaver service is protected by the system permission
  `BIND_DREAM_SERVICE`, so only Android itself can start it.

## Children

Dremo does not collect personal information from anyone, including children.

## Changes

If this policy changes, the new version is published at this address with a new date.

## Contact

Open an issue at <https://github.com/zaitsew/dremo/issues>.
