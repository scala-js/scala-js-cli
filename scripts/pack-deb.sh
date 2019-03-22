#!/bin/sh

SCALAJS_VER=$1
BASEVER=$2
MAINTAINER="$3"
EMAIL="$4"

case $BASEVER in
    2.11.*)
        BINVER="2.11"
        ;;
    2.12.*)
        BINVER="2.12"
        ;;
    *)
        echo "Invalid Scala version $BINVER" >&2
        exit 2
esac

# Base Scala.js project directory.
BASE="$(dirname $0)/.."

# Aritfact name (no extension).
NAME=scalajs_$BINVER-$SCALAJS_VER

# Target directories
TRG_BASE="$BASE/pack"
TRG_VER="$TRG_BASE/$NAME"

# Remove bat files for debian
for f in "$TRG_VER/bin"/*; do
    if echo "$f" | grep -qse ".bat$"; then
        rm "$f"
    fi
done

mkdir -p "$TRG_VER/usr/local"
mv "$TRG_VER/bin" "$TRG_VER/lib" "$TRG_VER/usr/local"

INSTALL_SIZE=$(ls -lR "$TRG_VER/" | awk '{{SUM += $5}} END {{print SUM}}')

# Create control file
if [ ! -d "$TRG_VER/DEBIAN/" ]; then
    mkdir "$TRG_VER/DEBIAN/"
fi

echo "Package: scalajs
Source: scalajs
#Maintainer: $MAINTAINER <$EMAIL>
Version: $SCALAJS_VER-1
Architecture: all
Installed-Size: $INSTALL_SIZE
Depends: scala
Section: java
Priority: optional
Homepage: http://www.scala-js.org
Description: Scala to Javascript Compiler
 Scala-js is a plugin for the scala compiler which compiles normal scala code to
 Javascript." > "$TRG_VER/DEBIAN/control"

dpkg-deb -b "$TRG_VER"
