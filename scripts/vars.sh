set -e

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

# Base Scala.js project directory.
TRG_BASE="$(realpath $(dirname $0)/../pack)"

SJS_ARTIFACT_NAME=scalajs_$BINVER-$SCALAJS_VER
