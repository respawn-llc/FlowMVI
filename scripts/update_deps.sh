#!/bin/bash
set -euo pipefail

release=$(curl -fsSL https://services.gradle.org/versions/current)
version=$(jq -er '.version' <<< "$release")
checksum=$(jq -er '.checksum' <<< "$release")
# Run again with the new distribution to regenerate the wrapper itself.
wrapper_args=(wrapper --gradle-version="$version" --gradle-distribution-sha256-sum="$checksum")
./gradlew "${wrapper_args[@]}"
./gradlew "${wrapper_args[@]}"

./gradlew versionCatalogUpdate --no-configuration-cache
./gradlew versionCatalogUpdateSample --no-configuration-cache

cache_dir="$(pwd)/docs/.npm-cache"

(
  cd docs
  npm_config_cache="$cache_dir" npx npm-check-updates -u
  npm_config_cache="$cache_dir" npm install
  npm_config_cache="$cache_dir" npm outdated || true
)
