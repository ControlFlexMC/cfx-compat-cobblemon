#!/usr/bin/env bash
# Fetch the two Cobblemon 1.5.2 production jars used as compile-only dependencies.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
LIBS_DIR="${PROJECT_DIR}/libs"
mkdir -p "${LIBS_DIR}"

FORGE_NAME="Cobblemon-forge-1.5.2+1.20.1.jar"
FABRIC_NAME="Cobblemon-fabric-1.5.2+1.20.1.jar"
FORGE_URL="https://cdn.modrinth.com/data/MdwFAVRL/versions/vm5zUZAg/Cobblemon-forge-1.5.2%2B1.20.1.jar"
FABRIC_URL="https://cdn.modrinth.com/data/MdwFAVRL/versions/EVozVxCq/Cobblemon-fabric-1.5.2%2B1.20.1.jar"
FORGE_SHA1="02601f79f0c442b75463538da7a3c7f260fda42e"
FABRIC_SHA1="2465e767f2659240f1c16549e602cb6e8b9145db"

force=false
if [[ "${1:-}" == "--force" ]]; then force=true; fi

sha1_of() {
    if command -v shasum >/dev/null 2>&1; then
        shasum -a 1 "$1" | awk '{print $1}'
    else
        sha1sum "$1" | awk '{print $1}'
    fi
}

install_jar() {
    local name="$1" url="$2" sha1="$3" local_source="${4:-}"
    local out="${LIBS_DIR}/${name}"

    if [[ -n "${local_source}" ]]; then
        if [[ ! -f "${local_source}" ]]; then
            echo "missing local source: ${local_source}" >&2
            exit 1
        fi
        cp "${local_source}" "${out}"
    elif [[ -f "${out}" && "${force}" == false && "$(sha1_of "${out}")" == "${sha1}" ]]; then
        echo "ok ${name}"
        return
    else
        echo "downloading ${name}"
        curl -L --fail --retry 3 --connect-timeout 20 -o "${out}.tmp" "${url}"
        mv "${out}.tmp" "${out}"
    fi

    local got
    got="$(sha1_of "${out}")"
    if [[ "${got}" != "${sha1}" ]]; then
        rm -f "${out}"
        echo "SHA-1 mismatch for ${name}: expected ${sha1}, got ${got}" >&2
        exit 1
    fi
    echo "ok ${name}"
}

install_jar "${FORGE_NAME}" "${FORGE_URL}" "${FORGE_SHA1}" "${COBBLEMON_FORGE_JAR:-}"
install_jar "${FABRIC_NAME}" "${FABRIC_URL}" "${FABRIC_SHA1}" "${COBBLEMON_FABRIC_JAR:-}"
