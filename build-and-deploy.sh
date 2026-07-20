#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cleanup_gui_artifacts() {
  echo "Cleaning GUI node_modules and dist directories..."
  find "${SCRIPT_DIR}" -maxdepth 2 \( -path "${SCRIPT_DIR}/gui.*/node_modules" -o -path "${SCRIPT_DIR}/gui.*/dist" \) -prune -exec rm -rf {} +
}

build_maven_modules() {
  echo "Running Maven clean install..."
  mvn clean install
}

build_docker_images() {
  echo "Building selected Docker images without cache..."
  "${SCRIPT_DIR}/docker/egm-compose.sh" build --no-cache srv-cnm-services srv-iidm-transformer gui-rcc-manager gui-cnm-manager
}

start_runtime() {
  echo "Starting runtime without mock services, BPM services, and non-CNM/non-CSA/non-IIDM srv services..."
  "${SCRIPT_DIR}/docker/egm-compose.sh" up mock=false 'exclude=srv.*,bpm.*' include=srv-cnm-services,srv-iidm-transformer,srv-csa-services
}

cleanup_gui_artifacts
build_maven_modules
build_docker_images
start_runtime
