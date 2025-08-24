# OpenTracks Data API

## Reason
OpenTracks by itself only provides basic features for data visualization and also lacks functionality for analytics.
By itself OpenTracks provides a basic dashboard that can show summary of a track (while recording as well as afterwards).
Although this might be (mostly) sufficient, it has some limitations.
This includes limited configurability (e.g., ordering of shown elements) and the lack of internet access.
In fact, the latter is considered a feature but severely limits the implementation presenting a map (at least in an easy way).

For the time being, the OpenTracks Data API can be used to implement mainly custom dashboards.

The Data API is disabled by default to protect the user's privacy, but it can easily be enabled in the settings.

## Implementation
OpenTracks by default does not expose any data to 3rd-party applications.
On the request of the user (i.e., pressing a button), an Intent is created that exposes the `Track` data (it's summary), the `TrackPoint`s, and the `Waypoint`s of one or more selected tracks.
For each, one URI is provided.
These URIs notify observers if on content changes (e.g., a new `TrackPoint` was added).

Limitations:
* only tracks selected by the user are exposed,
* `Waypoint`s are not exposed,
* access is only granted temporarily and automatically revoked (see `FLAG_GRANT_READ_URI_PERMISSION`),
* no write access is possible

The Data API can also be started automatically by leveraging Extra fields in the explicit Intent for the Start/Stop API.
An application that's starting a recording with an explicit Intent can add the `STATS_TARGET_PACKAGE` and `STATS_TARGET_CLASS` Extra fields to let OpenTracks know it wants to receive the Data API URIs.
OpenTracks will then (if enabled in the settings) automatically return an explicit Intent to the Package + Class as defined in the received Extra fields, including the Data API URIs and read permissions.

By using explicit Intents for the calls and URIs with specific temporary read permissions, OpenTracks tries to protect against data leaking as much as possible.

All required information can be found in `IntentDashboardUtils.java`. 

A _reference implementation_ is the [OSMDashboard](https://codeberg.org/OpenTracksApp/OSMDashboard).
