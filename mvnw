#!/usr/bin/env sh
set -eu

MAVEN_VERSION=3.9.11
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CACHE_DIR="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION"
ARCHIVE="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION-bin.tar.gz"
URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -x "$CACHE_DIR/bin/mvn" ]; then
  mkdir -p "$BASE_DIR/.mvn"
  echo "Preparing the project Maven $MAVEN_VERSION runtime..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$URL"
  else
    echo "Neither curl nor wget is available, so the project Maven runtime cannot be downloaded." >&2
    exit 1
  fi
  tar -xzf "$ARCHIVE" -C "$BASE_DIR/.mvn"
  rm -f "$ARCHIVE"
fi

exec "$CACHE_DIR/bin/mvn" "$@"
