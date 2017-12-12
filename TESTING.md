# Testing the standalone distribution

TODO: We should automate this process

For each major Scala version on a *NIX distro and a Windows distro:

1. Download packaged Scala from scala-lang.org
2. Build a CLI distribution (see [the release process](./RELEASING.md))
3. Unpack Scala and Scala.js distro
4. Add `bin/` directories of both distributions to path (`export PATH=$PATH:<scala path>/bin:<scala.js path>/bin`)
5. Create a temporary directory and do:

        mkdir bin
        echo '
          object Foo {
            def main(args: Array[String]): Unit = {
              println(s"asdf ${1 + 1}")
              new A
            }

            class A
          }
        ' > foo.scala
        scalajsc -d bin foo.scala

        scalajsp bin/Foo$.sjsir
        # Verify output
        scalajsp bin/Foo\$A.sjsir
        # Verify output

        scalajsld -o test.js -mm Foo.main bin
        # Verify output

        node test.js # Or your favorite thing to run JS

        # Expect "asdf 2"
