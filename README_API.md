# OpenTracks Data API

## Reason
OpenTracks by itself only provides basic features for data visualization and also lacks functionality for analytics.
By itself OpenTracks provides a basic dashboard that can show summary of a track (while recording as well as afterwards).
Although this might be (mostly) sufficient, it has some limitations.
This include limited configurability (e.g., ordering of shown elements) and the lack of Internet access.
In fact, the later is considered a feature but severely limits the implementation presenting a map (at least in an easy way). 

For the time being, the OpenTracks Data API can be used to implement mainly custom dashboards.

## Implementation
OpenTracks by default does not expose any data to 3rd-party applications.
On the request of the user (i.e., pressing a button), an Intent is created that exposes the `Track` data (it's summary), the `TrackPoint`s, and the `Waypoint`s of one or more selected tracks.
For each, one URI is provided.
These URIs notify observers if on content changes (e.g., a new `TrackPoint` was added).

Limitations:
* only tracks selected by the user are exposed,
* `Waypoints` are not exposed,
* access is only granted temporarily and automatically revoked (see `FLAG_GRANT_READ_URI_PERMISSION`),
* no write access is possible, and
* neither track recordings can be started, paused, or stopped.

All required information can be found in `IntentDashboardUtils.java`. 

A _reference implementation_ is the [OSMDashboard](https://github.com/OpenTracksApp/OSMDashboard).