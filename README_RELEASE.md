# OpenTracks: Developer information

## Craft new releases

1. Execute use `RELEASE.sh`.
2. Push to Github
3. Create release on Github (incl. changelog)
   This actually creates a tag that is later used by F-Droid
4. Pull from Github
5. Build reproducible release and attach to release
6. ... wait until reproducibility is verified by F-Droid

Two releases will be created:
* Application id: `de.dennisguse.opentracks` (irreproducible)
* Application id: `de.dennisguse.opentracks.playstore` (reproducible)

For F-Droid the build status can be found [here](https://monitor.f-droid.org/builds/build).

## Reproducible builds

A lot of detailed information can be found [here](https://f-droid.org/docs/Reproducible_Builds/)

Technical requirements:
* Must be build using OpenJDK17

## Translations (localization)

Translations of the OpenTracks are handled in
Weblate: [https://hosted.weblate.org/projects/opentracks/](https://hosted.weblate.org/projects/opentracks).

## Google Play Store
Supported languages: https://support.google.com/googleplay/android-developer/table/4419860