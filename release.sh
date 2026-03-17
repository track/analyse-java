#!/bin/bash
set -e

VERSION=$(grep '^version=' gradle.properties | cut -d'=' -f2)
if [ -z "$VERSION" ]; then
  echo "Could not read version from gradle.properties"
  exit 1
fi

echo "Building serverstats v${VERSION}..."
./gradlew build --no-daemon -q

OUTPUT="serverstats-${VERSION}.zip"
JARS=(
  "paper/build/libs/serverstats-paper-${VERSION}.jar"
  "bungeecord/build/libs/serverstats-bungeecord-${VERSION}.jar"
  "velocity/build/libs/serverstats-velocity-${VERSION}.jar"
)

FOUND=()
for jar in "${JARS[@]}"; do
  if [ -f "$jar" ]; then
    FOUND+=("$jar")
  else
    echo "Warning: $jar not found, skipping"
  fi
done

if [ ${#FOUND[@]} -eq 0 ]; then
  echo "No jars found, aborting"
  exit 1
fi

# Zip the jars (flat, no directory structure)
zip -j "$OUTPUT" "${FOUND[@]}"

echo ""
echo "Created ${OUTPUT} with ${#FOUND[@]} plugin(s):"
for jar in "${FOUND[@]}"; do
  echo "  - $(basename "$jar")"
done
