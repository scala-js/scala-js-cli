#! /bin/sh

# Tests the CLI tools.

. "$(dirname $0)/vars.sh"

TEST_DIR="$(realpath $(dirname $0)/../cli-test)"

rm -rf $TEST_DIR
mkdir $TEST_DIR
cd $TEST_DIR

# Fetch Scala
SCALA_ARTIFACT_NAME=scala-$BASEVER
wget "https://downloads.lightbend.com/scala/$BASEVER/$SCALA_ARTIFACT_NAME.tgz"
tar xf $SCALA_ARTIFACT_NAME.tgz

# Fetch Scala.js
cp $TRG_BASE/$SJS_ARTIFACT_NAME.tgz .
tar xf $SJS_ARTIFACT_NAME.tgz

# Setup PATH
PATH=$PATH:$TEST_DIR/$SCALA_ARTIFACT_NAME/bin:$TEST_DIR/$SJS_ARTIFACT_NAME/bin

fail() {
    echo "$1" >&2
    exit 2
}

# Actual test.
mkdir bin
cat > foo.scala <<'EOF'
object Foo {
  def main(args: Array[String]): Unit = {
    println(s"asdf ${1 + 1}")
    new A
  }

  class A
}
EOF

scalajsc -d bin foo.scala

scalajsp bin/Foo$.sjsir > out
test -s out || fail "scalajsp bin/Foo$.sjsir: empty output"

scalajsp bin/Foo\$A.sjsir > out
test -s out || fail "scalajsp bin/Foo\$A.sjsir: empty output"

scalajsld -s -o test.js -mm Foo.main bin 2> test_stderr.txt
grep -Fxq "Warning: using a single file as output (--output) is deprecated since Scala.js 1.3.0. Use --outputDir instead." test_stderr.txt \
  || fail "expected warning. Got: $(cat test_stderr.txt)"
test -s test.js || fail "scalajsld: empty output"
test -s test.js.map || fail "scalajsld: empty source map"

node test.js > got-legacy.run
cat > want-legacy.run <<EOF
asdf 2
EOF

diff got-legacy.run want-legacy.run

mkdir test-output
scalajsld -s --outputDir test-output --moduleSplitStyle SmallestModules --moduleKind CommonJSModule -mm Foo.main bin
test "$(ls test-output/*.js| wc -w)" -gt "1" || fail "scalajsld: produced single js output file"

node test-output/main.js > got.run
cat > want.run <<EOF
asdf 2
EOF

diff got.run want.run
