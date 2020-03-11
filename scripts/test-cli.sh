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

scalajsld -o test.js -mm Foo.main bin
test -s test.js || fail "scalajsld: empty output"

node test.js > got.run
cat > want.run <<EOF
asdf 2
EOF

diff got.run want.run
