#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

"${PROJECT_DIR}/tools/fetch-cobblemon.sh"

"${PROJECT_DIR}/gradlew" :forge:build "$@"

JAR="$(ls -t "${PROJECT_DIR}"/forge/build/libs/cfx-compat-cobblemon-*-forge.jar | head -1)"

INSTANCE="${COBBLEMON_1_20_1_FORGE_PATH:-/Users/ifels/.prism/instances/cobblemon-1.20.1-forge/minecraft}"
MODS="${INSTANCE}/mods"
mkdir -p "${MODS}"
rm -f "${MODS}"/cfx-compat-cobblemon-*-forge.jar
cp "${JAR}" "${MODS}/"
if [[ -d "${INSTANCE}/logs" ]]; then
    rm -rf "${INSTANCE:?}/logs/"*
fi
echo "deployed $(basename "${JAR}") to ${MODS}"
