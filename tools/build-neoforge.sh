#!/usr/bin/env bash
# Build the NeoForge (MC 1.21.1) artifact for cfx-compat-cobblemon.
#
# Usage:
#   ./tools/build-neoforge.sh
#   ./tools/build-neoforge.sh --release
#   ./tools/build-neoforge.sh --clean
#
# Environment:
#   MC_1_21_1_NEOFORGE_PATH  instance dir (the one containing mods/); unset -> skip deploy
#   RELEASE_DEPLOY_PATH      extra copy dest on --release; unset -> WARNING
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

RELEASE=false
CLEAN=false
EXTRA_ARGS=()

for arg in "$@"; do
    case "$arg" in
        --release) RELEASE=true ;;
        --clean)   CLEAN=true ;;
        *)         EXTRA_ARGS+=("$arg") ;;
    esac
done

mod_version=$(grep -E '^mod_version=' gradle.properties | cut -d= -f2)
mc_ver=$(grep -E '^minecraft_version=' gradle.properties | cut -d= -f2)
NEW_JAR="${PROJECT_DIR}/neoforge/build/libs/cfx-compat-cobblemon-${mod_version}-mc${mc_ver}-neoforge.jar"

echo "==> NeoForge build: cfx-compat-cobblemon ${mod_version} (MC ${mc_ver})"

if $CLEAN; then
    echo "==> Cleaning first..."
    ./gradlew clean
fi

gradle_cmd=(./gradlew :neoforge:build)
if [[ ${#EXTRA_ARGS[@]} -gt 0 ]]; then gradle_cmd+=("${EXTRA_ARGS[@]}"); fi
echo "==> Running: ${gradle_cmd[*]}"
"${gradle_cmd[@]}"

if [[ ! -f "${NEW_JAR}" ]]; then
    echo "JAR not found: ${NEW_JAR}"
    exit 1
fi

echo ""
echo "Build complete: $(basename "${NEW_JAR}")"

MC_INSTANCE="${MC_1_21_1_NEOFORGE_PATH:-}"
MODS_DIR="${MC_INSTANCE}/mods"

if [[ -z "${MC_INSTANCE}" ]]; then
    echo "MC_1_21_1_NEOFORGE_PATH is not set; skipping instance deploy"
    echo "   artifact: ${NEW_JAR}"
else
    LOGS_DIR="${MC_INSTANCE}/logs"
    if [[ -d "${LOGS_DIR}" ]]; then
        rm -rf "${LOGS_DIR:?}"/*
        echo "  cleared logs: ${LOGS_DIR}"
    fi

    if [[ -d "${MODS_DIR}" ]]; then
        rm -f "${MODS_DIR}"/cfx-compat-cobblemon-*-mc*-neoforge.jar
        echo "  removed old cfx-compat-cobblemon NeoForge jars"
    fi

    mkdir -p "${MODS_DIR}"
    cp "${NEW_JAR}" "${MODS_DIR}/"
    echo "  deployed: ${MODS_DIR}/$(basename "${NEW_JAR}")"
fi

if $RELEASE; then
    if [[ -n "${RELEASE_DEPLOY_PATH:-}" ]]; then
        mkdir -p "${RELEASE_DEPLOY_PATH}"
        cp "${NEW_JAR}" "${RELEASE_DEPLOY_PATH}/"
        echo "  release copy: ${RELEASE_DEPLOY_PATH}/$(basename "${NEW_JAR}")"
    fi
else
    echo "RELEASE_DEPLOY_PATH is not set; skipping release copy"
fi
