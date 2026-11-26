#!/usr/bin/env bash
# Runs the Robolectric tests against build/classes (run ../build.sh first).
set -euo pipefail
cd "$(dirname "$0")"
unset _JAVA_OPTIONS
rm -rf tclasses && mkdir tclasses
CP="../build/classes:$(ls lib/*.jar | tr '\n' ':')"
javac -nowarn -encoding UTF-8 -cp "$CP:../sdk/android35.jar" -d tclasses $(find src -name '*.java')
java --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED -Drobolectric.offline=true -Drobolectric.dependency.dir=androidall -DpdfOut="$PWD/out" \
  -cp "tclasses:cfg:$CP:../sdk/android23-res.jar" -DpdfIn=/tmp/claude-0/pdft/in.pdf org.junit.runner.JUnitCore ${TESTS:-com.inspectorsgadget.app.AppTest com.inspectorsgadget.app.PdfCompressorTest}
