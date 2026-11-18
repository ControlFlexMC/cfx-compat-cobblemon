#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

"${PROJECT_DIR}/tools/fetch-cobblemon.sh"

"${PROJECT_DIR}/gradlew" :forge:build "$@"

JAR="$(ls -t "${PROJECT_DIR}"/forge/build/libs/cfx-compat-cobblemon-*-forge.jar | head -1)"

INSTANCE="${MC_1_20_1_FORGE_PATH:-}"
if [[ -z "${INSTANCE}" ]]; then
    echo "MC_1_20_1_FORGE_PATH is not set; artifact left at ${JAR}"
    exit 0
fi

MODS="${INSTANCE}/mods"
mkdir -p "${MODS}"
rm -f "${MODS}"/cfx-compat-cobblemon-*-forge.jar
cp "${JAR}" "${MODS}/"
if [[ -d "${INSTANCE}/logs" ]]; then
    rm -rf "${INSTANCE:?}/logs/"*
fi
echo "deployed $(basename "${JAR}") to ${MODS}"
