#! /bin/sh

# Assembles the CLI tools for a given Scala binary version.

. "$(dirname $0)/vars.sh"

# Build and lay out the contents of the archives
sbt \
    "clean" \
    "++$BASEVER!" \
    "set scalaJSVersion in ThisBuild := \"$SCALAJS_VER\"" \
    "cliPack" \
    || exit $?

# Tar and zip the whole thing up
(
    cd $TRG_BASE
    tar cfz $SJS_ARTIFACT_NAME.tgz $SJS_ARTIFACT_NAME

    if [ -f $SJS_ARTIFACT_NAME.zip ]; then rm $SJS_ARTIFACT_NAME.zip; fi
    zip -r $SJS_ARTIFACT_NAME.zip -r $SJS_ARTIFACT_NAME
)
