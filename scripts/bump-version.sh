#!/usr/bin/env bash
# Bumps the library version in gradle.properties and dependencies.gradle.
# Usage: scripts/bump-version.sh patch|minor|major
# Prints "version=<new version>" in GITHUB_OUTPUT format.
set -euo pipefail

increment=${1:?usage: bump-version.sh patch|minor|major}
cd "${0%/*}/.."

current=$(sed -n 's/^VERSION_NAME=//p' gradle.properties)
code=$(sed -n 's/^ *publishVersionCode *: *\([0-9]*\).*/\1/p' dependencies.gradle)

IFS=. read -r major minor patch <<<"$current"
case "$increment" in
    major) major=$((major + 1)); minor=0; patch=0 ;;
    minor) minor=$((minor + 1)); patch=0 ;;
    patch) patch=$((patch + 1)) ;;
    *) echo "unknown increment: $increment" >&2; exit 1 ;;
esac
version="$major.$minor.$patch"

sed -i.bak "s/^VERSION_NAME=.*/VERSION_NAME=$version/" gradle.properties
sed -i.bak -e "s/^\( *publishVersion *: *\)'.*'/\1'$version'/" \
    -e "s/^\( *publishVersionCode *: *\)[0-9]*/\1$((code + 1))/" dependencies.gradle
rm -f gradle.properties.bak dependencies.gradle.bak

echo "version=$version"
