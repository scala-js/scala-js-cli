#! /bin/sh

set -e

# Assembles the CLI tools for a given Scala binary version.

if [ $# -lt 2 ]; then
    echo "Usage: $(basename $0) <scala.js full version> <base scala full version>" >&2
    exit 1
fi

SCALAJS_VER=$1

BASEVER=$2
case $BASEVER in
    2.11.*)
        BINVER="2.11"
        ;;
    2.12.*)
        BINVER="2.12"
        ;;
    2.13.*)
        BINVER="2.13"
        ;;
    *)
        echo "Invalid Scala version $BINVER" >&2
        exit 2
esac

# Build and lay out the contents of the archives
sbt \
    "clean" \
    "++$BASEVER!" \
    "set scalaJSVersion in ThisBuild := \"$SCALAJS_VER\"" \
    "cliPack" \
    || exit $?

# Base Scala.js project directory.
BASE="$(dirname $0)/.."

# Aritfact name (no extension).
NAME=scalajs_$BINVER-$SCALAJS_VER

# Target directories
TRG_BASE="$BASE/pack"
TRG_VER="$TRG_BASE/$NAME"

# Tar and zip the whole thing up
(
    cd $TRG_BASE
    tar cfz $NAME.tgz $NAME

    if [ -f $NAME.zip ]; then rm $NAME.zip; fi
    zip -r $NAME.zip -r $NAME
)
