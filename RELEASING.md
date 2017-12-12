# Releasing the CLI

## Version commit

For releases, make sure that the default value for `scalaJSVersion` in the
build matches the `version` of `scalajs-cli`.

## Publish the `scalajs-cli` artifact to Maven Central

This is a normal cross-compiled publish:

    > +publishSigned

## Build and upload the standalone distribution

For each major version of Scala, identify the latest corresponding full version
and run:

    $ ./scripts/assemble-cli.sh <Scala.js full version> <Scala full version>

Then upload the produced archives (in `pack/`) to the website, using `scp`.
