# OpenTracks: Developer information

## Craft new releases

1. Get next _version code_ (main): `git rev-list HEAD --count main`
2. Decide on _version name_ (semantic versioning)
3. Manually update _version code_ and _version name_ in `build.gradle`
4. Create changelog (`_version code_.txt`)
5. Create commit with all changes
6. Tag newly create commit with _version name_ (e.g., v3.2.1)
7. Push commits and tags to public repository
8. Add changelog to tag (Github releases)

## F-Droid
Application id: de.dennisguse.opentracks

It if it properly tagged F-Droid takes care of building the app and publishing.
Usually, it takes two days before the update is published (build, signed, and published). 

The recent build activity can be found [here](https://monitor.f-droid.org/builds/build)

## Google Play Store
Application id: `de.dennisguse.opentracks.playstore`

Requirement: `gem install fastlane`

1. Build signed apk
   (requires keystore)
2. Upload signed apk to [Google Play Console](https://play.google.com/apps/publish)
3. Update store meta data (e.g., description and screenshots)
   `fastlane metadata`
   (requires API key configured in `Appfile`)

Supported languages: https://support.google.com/googleplay/android-developer/table/4419860

## Translations (localization)

Translations of the OpenTracks are handled in
Weblate: [https://hosted.weblate.org/projects/opentracks/](https://hosted.weblate.org/projects/opentracks)
.