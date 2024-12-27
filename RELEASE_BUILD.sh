#!/bin/bash

set -e

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64/

JAVA_VERSION=`java --version`
if [[ $JAVA_VERSION != "openjdk 17."* ]]; then
  echo "OpenJDK version should be 17.X"
  exit -1;
fi

RELEASE_STORE_PASSWORD=
read -s -p "Enter store password: " RELEASE_STORE_PASSWORD
echo ""
read -s -p "Enter key alias: " RELEASE_KEY_ALIAS
echo ""
read -s -p "Enter key password: " RELEASE_KEY_PASSWORD
echo ""

./gradlew --no-configuration-cache \
  clean \
  assembleReproducibleRelease \
  -Drelease_store_file=`ls ../*.jks` \
  -Drelease_store_file="../keystore_opentracks.jks" \
  -Drelease_store_password="$RELEASE_STORE_PASSWORD" \
  -Drelease_key_alias="$RELEASE_KEY_ALIAS" \
  -Drelease_key_password="$RELEASE_KEY_PASSWORD"
