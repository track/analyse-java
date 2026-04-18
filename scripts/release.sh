#!/bin/bash
set -e

VERSION=$(grep '^version=' gradle.properties | cut -d'=' -f2)
if [ -z "$VERSION" ]; then
  echo "Could not read version from gradle.properties"
  exit 1
fi

echo "Building analyse v${VERSION}..."
./gradlew build --no-daemon -q

OUTPUT="analyse-${VERSION}.zip"
JARS=(
  "modules/spigot/build/libs/analyse-spigot-${VERSION}.jar"
  "modules/bungeecord/build/libs/analyse-bungeecord-${VERSION}.jar"
  "modules/velocity/build/libs/analyse-velocity-${VERSION}.jar"
  "modules/hytale/build/libs/analyse-hytale-${VERSION}.jar"
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
