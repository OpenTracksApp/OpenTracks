#!/bin/bash

set -e

# Folder where svg screenshots are expected.
SRC_DIR="screenshots-svg/"
# Folder where png files will be saved.
DST_DIR="fastlane/metadata/android/en-US/images/phoneScreenshots/"

echo "Exporting svg files to png..."

# Source and destination folder must exist.
if [ ! -d "$SRC_DIR" ]
then
    echo "\nERROR: ${SRC_DIR} doesn't exist."
    exit
fi

if [ ! -d "$DST_DIR" ]
then
    echo "\nERROR: ${DST_DIR} doesn't exist."
    exit
fi

# It needs Inkscape that is the tool used to convert svg to png.
if ! command -v inkscape > /dev/null
then
    echo "\nERROR: inkscape couldn't be found and it's needed to convert screenshots from svg to png."
    exit
fi

# Convert all svg files from source directory.
for path in ${SRC_DIR}/*.svg
do
    filename="${path##*/}"
    basename="${filename%.svg}"
    `inkscape ${path} --export-filename ${DST_DIR}/${basename}.png`
done
