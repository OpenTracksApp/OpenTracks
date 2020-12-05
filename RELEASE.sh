#!/bin/bash

set -e

CHANGELOG_DIR="fastlane/metadata/android/en-US/changelogs/"

echo "Checking main branch"
git checkout main
VERSIONID=$(git rev-list HEAD --count main)
CHANGELOG_FILE="$CHANGELOG_DIR/$VERSIONID.txt"

git diff --exit-code || echo -e "\nWARNING WARNING WARNING WARNING WARNING: this branch contains uncommit changes."

#TODO Execute all tests: we are destroying data, running on a real device.
# ./gradlew connectedCheck

# Changelog
echo -e "\nPrevious changelog\n------------"
PREVIOUS_CHANGELOG=$(find "$CHANGELOG_DIR" -iname "[[:digit:]]**.txt" | sort -n | tail -1)
sed 's/^/>/' "$PREVIOUS_CHANGELOG"

#For F-Droid, a version starts with a 'v'
read -p $'\n\nPlease enter the versionname (e.g., v3.9.5):\n> v' VERSIONNAME
VERSIONNAME="v$VERSIONNAME"

cp "$CHANGELOG_DIR/next_release.txt" "$CHANGELOG_FILE"
sed -i "s/(versionName)/$VERSIONNAME/" "$CHANGELOG_FILE"

echo -e "\nGit commit since last release\n------------"
git log "$(git describe --tags --abbrev=0)..HEAD" --no-merges --oneline

echo -e "\nPlease edit the changelog for the new release"
gedit -w "$CHANGELOG_FILE"
git add "$CHANGELOG_FILE"

echo "Updating build.gradle"
sed -i s/versionCode\ .*/versionCode\ "$VERSIONID"/g build.gradle
sed -i s/versionName\ .*/versionName\ "\"$VERSIONNAME\""/g build.gradle

git diff

COMMIT_MESSAGE="Release: $VERSIONNAME"
echo -e "\nCommit message will be: '$COMMIT_MESSAGE'"

echo -e "\nPress ENTER, if everything looks good."
read

git add -u
git commit -m "$COMMIT_MESSAGE"

echo -e "Next steps:\n1. Push to Github\n2. Create release on Github: https://github.com/OpenTracksApp/OpenTracks/releases"