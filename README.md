# <img src="drawable-svg/LOGO.svg" alt="OpenTracks logo" height="40"></img> OpenTracks: a sport tracker

_OpenTracks_ is a sport tracking application that completely respects your privacy.

[![Awesome Humane Tech](https://raw.githubusercontent.com/humanetech-community/awesome-humane-tech/main/humane-tech-badge.svg?sanitize=true)](https://github.com/humanetech-community/awesome-humane-tech)

<table>
    <tr>
        <th>Free</th>
        <th>Donations</th>
        <th>Translations</th>
    </tr>
    <tr>
        <td align="center">
            <a href="https://f-droid.org/packages/de.dennisguse.opentracks">
                <img alt="Get it on F-Droid" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="60" align="middle">
            </a>
        </td>
        <td align="center">
            <a href="https://liberapay.com/OpenTracks/donate">
                <img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg" height="45" align="middle">
            </a>
        </td>
        <td align="center">
            <a href="https://weblate.bubu1.eu/engage/OpenTracks/">
                <img src="https://weblate.bubu1.eu/widgets/OpenTracks/-/opentracks/svg-badge.svg" alt="Translation status" />
            </a>
        </td>
    </tr>
    <tr>
        <td align="center">
            <img alt="OpenTracks version published on F-Droid" src="https://img.shields.io/f-droid/v/de.dennisguse.opentracks.svg" align="middle" >
        </td>
        <td align="center">
            <a href="bitcoin:bc1qckpls3lpjlcpyxzm66hcy65hnesas9zgacy6yc">
                <img alt="bitcoin:bc1qckpls3lpjlcpyxzm66hcy65hnesas9zgacy6yc" src="promotion/donations-bitcoin.png" height="90" align="middle">
            </a>
        </td align="center">
        <td></td>
    </tr>
    <tr>
        <td></td>
        <td align="center">
            <a href="https://play.google.com/store/apps/details?id=de.dennisguse.opentracks.playstore">
                <img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="60" align="middle">
            </a>
        </td>
        <td></td>
    </tr>
</table>

## Screenshots
<div>
    <img width="23%" src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot1.png">
    <img width="23%" src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot2.png">
    <img width="23%" src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot3.png">
    <img width="23%" src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot4.png">
</div>
<div>
	<img width="23%" src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot5.png">
    <img width="23%" src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot6.png">
	<img width="23%" src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot7.png">
	<img width="23%" src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot8.png">
</div>

## Features
* __Tracking:__ track your sport and outdoor activities
* __Voice announcements__
* __Photos and Markers:__ mark interesting locations while tracking
* __Export:__
  * export tracks either as KMZ (incl. photos), KML, or GPX
  * export automatically after each recording (e.g., to sync)
  * avoid duplication: each exported file contain a random unique identifier (i.e., `opentracks:trackid`)
* __Altitude:__
  * gain/loss via barometric sensor (if present)
  * shown in EGM2008 (above mean sea level); exported as WGS84
* __Bluetooth LE sensors:__
  * heart rate
  * cycling: speed and distance
  * cycling: cadence
  * cycling: power meter
  * running: speed and cadence
  An overview of tested sensors: [README_TESTED_SENSORS.md](README_TESTED_SENSORS.md)

### Gadgetbridge integration

OpenTracks can be used with [Gadgetbridge](https://www.gadgetbridge.org/):
* shows statistics via notification on smart watches (requires Gadgetbridge 0.56.1 or later), and
* Gadgetbridge's GPX exporter generates `opentracks:trackid` to avoid duplication (Gadgetbridge 0.53.0 or later).

### Privacy
* __No Internet access:__ Internet is not used
* __No advertising__
* __No in-app analytics__
* __No use of Google Play Services__

__Only required permission:__
* _ACCESS_FINE_LOCATION_: required to use the GPS.

An overview of Bluetooth LE sensors that are known to work with OpenTracks is in [README_TESTED_SENSORS.md](README_TESTED_SENSORS.md).

## Custom Dashboards (incl. map)
As of v3.3.1, OpenTracks supports custom dashboards.
The reference implementation is [OSMDashboard](https://github.com/OpenTracksApp/OSMDashboard), which presents an OpenStreetMap map (showing the current track, incl. updates).
The developer documentation is in [README_API.md](README_API.md).

Alternatively, recorded tracks can be shared as KMZ/GPX with installed applications (e.g., [OsmAnd](https://play.google.com/store/apps/details?id=net.osmand)).
However, this is rather slow and does not provide updates while recording.

## Project history

_OpenTracks_ is based upon Google _My Tracks app_ ([code](https://code.google.com/archive/p/mytracks/)).
Initially, _My Tracks_ was initially released by Google in 2010 as [open-source software](http://google-latlong.blogspot.fr/2010/05/code-for-my-tracks-is-now-yours.html).
In 2016, [Google decided to discontinue](https://support.google.com/maps/answer/6333516) _My Tracks_ and stopped distributing it via the Google Play store in April 2016.
Then [Plonk42](https://github.com/plonk42) conducted some maintenance work until 2016, so _My Tracks_ could still be used (based upon version _Google's MyTracks_ version 2.0.6).
Plonk42's version is available [here](https://github.com/Plonk42/mytracks).
In 2019, _OpenTracks_ was forked from Plonk42's _My Tracks_ and major rework was conducted.

Rework of _OpenTracks_ included:
* removing Google's analytics code,
* removing integration into Google Drive,
* removing Google Maps integration,
* removing Google Earth integration,
* removing use of Google Play service,
* removing calorie estimation and activity estimation,
* removing support for ANT+ and Classic Bluetooth,
* adding support for Bluetooth LE heart rate sensors,
* removing Protobuf (store sensor data in SQLite columns directly), and
* removing Android Service API for other apps.

Artwork, logos and user interface remained more or less unchanged.

More information about _Google My Tracks_:
* [Wikipedia page](https://en.wikipedia.org/wiki/MyTracks)
* [Google code archive](https://code.google.com/archive/p/mytracks/)