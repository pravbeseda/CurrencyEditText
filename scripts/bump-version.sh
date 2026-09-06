#!/usr/bin/env bash
# Bumps VERSION_NAME and VERSION_CODE in gradle.properties.
# Usage: scripts/bump-version.sh patch|minor|major
# Prints "version=<new version>" in GITHUB_OUTPUT format.
set -euo pipefail

increment=${1:?usage: bump-version.sh patch|minor|major}
cd "${0%/*}/.."

current=$(sed -n 's/^VERSION_NAME=//p' gradle.properties)
code=$(sed -n 's/^VERSION_CODE=//p' gradle.properties)

IFS=. read -r major minor patch <<<"$current"
case "$increment" in
    major) major=$((major + 1)); minor=0; patch=0 ;;
    minor) minor=$((minor + 1)); patch=0 ;;
    patch) patch=$((patch + 1)) ;;
    *) echo "unknown increment: $increment" >&2; exit 1 ;;
esac
version="$major.$minor.$patch"

sed -i.bak -e "s/^VERSION_NAME=.*/VERSION_NAME=$version/" \
    -e "s/^VERSION_CODE=.*/VERSION_CODE=$((code + 1))/" gradle.properties
rm -f gradle.properties.bak

echo "version=$version"
