#!/usr/bin/env bash
set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"

MOCK_ENABLED="false"
INCLUDE_PATTERNS=()
EXCLUDE_PATTERNS=()
PASSTHROUGH_ARGS=()
REQUESTED_SERVICES=()
COMPOSE_SERVICES=()

usage() {
  cat <<'EOF'
Usage:
  docker/egm-compose.sh COMMAND [OPTIONS] [SERVICES...]

Commands:
  build       Run docker compose build. If SERVICES are passed, they are used as-is.
  up          Run docker compose up for the resolved service set.
  config      Print the resolved service set without starting containers.
  down        Run docker compose down.
  ps          Run docker compose ps.
  logs        Run docker compose logs.

Selection options:
  mock=true|false          Include mock-* services only when true. Default: false.
  include=PATTERN[,..]     Include services or glob patterns after exclusions.
  exclude=PATTERN[,..]     Exclude services or glob patterns.

Pattern notes:
  Docker Compose service names use hyphens. Dotted patterns are accepted and
  normalized, so srv.* also matches srv-*.

Examples:
  docker/egm-compose.sh build --no-cache srv-cnm-services gui-rcc-manager gui-cnm-manager
  docker/egm-compose.sh up mock=false
  docker/egm-compose.sh config mock=false 'exclude=srv.*,bpm.*' include=srv-cnm-services
  docker/egm-compose.sh up mock=false 'exclude=srv.*,bpm.*' include=srv-cnm-services
EOF
}

die() {
  echo "egm-compose: $*" >&2
  exit 1
}

compose() {
  docker compose -f "${COMPOSE_FILE}" "$@"
}

normalize_pattern() {
  local value="$1"
  echo "${value//./-}"
}

add_csv_patterns() {
  local target="$1"
  local csv="$2"
  local item
  IFS=',' read -ra items <<< "${csv}"
  for item in "${items[@]}"; do
    [[ -n "${item}" ]] || continue
    if [[ "${target}" == "include" ]]; then
      INCLUDE_PATTERNS+=("$(normalize_pattern "${item}")")
    else
      EXCLUDE_PATTERNS+=("$(normalize_pattern "${item}")")
    fi
  done
}

matches_any_pattern() {
  local service="$1"
  shift
  local pattern
  for pattern in "$@"; do
    if [[ "${service}" == ${pattern} ]]; then
      return 0
    fi
  done
  return 1
}

all_services() {
  if ((${#COMPOSE_SERVICES[@]} == 0)); then
    local service
    while IFS= read -r service; do
      [[ -n "${service}" ]] || continue
      COMPOSE_SERVICES+=("${service}")
    done < <(compose config --services)
  fi
  printf '%s\n' "${COMPOSE_SERVICES[@]}"
}

service_exists() {
  local candidate
  candidate="$(normalize_pattern "$1")"
  local service
  while IFS= read -r service; do
    [[ "${service}" != "${candidate}" ]] || return 0
  done < <(all_services)
  return 1
}

available_services_message() {
  echo "available services:"
  all_services | sed 's/^/  - /'
}

append_unique_service() {
  local service
  service="$(normalize_pattern "$1")"
  local existing
  for existing in "${REQUESTED_SERVICES[@]}"; do
    [[ "${existing}" != "${service}" ]] || return 0
  done
  REQUESTED_SERVICES+=("${service}")
}

resolve_services() {
  local selected=()
  local service
  while IFS= read -r service; do
    [[ -n "${service}" ]] || continue

    if [[ "${MOCK_ENABLED}" != "true" && "${service}" == mock-* ]]; then
      continue
    fi

    if ((${#EXCLUDE_PATTERNS[@]} > 0)) && matches_any_pattern "${service}" "${EXCLUDE_PATTERNS[@]}"; then
      continue
    fi

    selected+=("${service}")
  done < <(all_services)

  local include_pattern
  for include_pattern in "${INCLUDE_PATTERNS[@]}"; do
    local matched="false"
    while IFS= read -r service; do
      if [[ "${service}" == ${include_pattern} ]]; then
        matched="true"
        local exists="false"
        local selected_service
        for selected_service in "${selected[@]}"; do
          if [[ "${selected_service}" == "${service}" ]]; then
            exists="true"
            break
          fi
        done
        [[ "${exists}" == "true" ]] || selected+=("${service}")
      fi
    done < <(all_services)
    [[ "${matched}" == "true" ]] || die "include pattern matched no services: ${include_pattern}"
  done

  printf '%s\n' "${selected[@]}"
}

parse_args() {
  local arg
  for arg in "$@"; do
    case "${arg}" in
      mock=true)
        MOCK_ENABLED="true"
        ;;
      mock=false)
        MOCK_ENABLED="false"
        ;;
      include=*)
        add_csv_patterns "include" "${arg#include=}"
        ;;
      exclude=*)
        add_csv_patterns "exclude" "${arg#exclude=}"
        ;;
      --help|-h)
        usage
        exit 0
        ;;
      --*)
        PASSTHROUGH_ARGS+=("${arg}")
        ;;
      *)
        append_unique_service "${arg}"
        ;;
    esac
  done
}

validate_requested_services() {
  local service
  for service in "${REQUESTED_SERVICES[@]}"; do
    if ! service_exists "${service}"; then
      echo "egm-compose: unknown docker compose service: ${service}" >&2
      available_services_message >&2
      exit 1
    fi
  done
}

load_resolved_services() {
  local service
  REQUESTED_SERVICES=()
  while IFS= read -r service; do
    [[ -n "${service}" ]] || continue
    REQUESTED_SERVICES+=("${service}")
  done < <(resolve_services)
}

if (($# == 0)); then
  usage
  exit 0
fi

COMMAND="$1"
shift

case "${COMMAND}" in
  --help|-h|help)
    usage
    exit 0
    ;;
  build)
    parse_args "$@"
    validate_requested_services
    if ((${#REQUESTED_SERVICES[@]} > 0)); then
      compose build "${PASSTHROUGH_ARGS[@]}" "${REQUESTED_SERVICES[@]}"
    else
      load_resolved_services
      compose build "${PASSTHROUGH_ARGS[@]}" "${REQUESTED_SERVICES[@]}"
    fi
    ;;
  up)
    parse_args "$@"
    validate_requested_services
    if ((${#REQUESTED_SERVICES[@]} == 0)); then
      load_resolved_services
    fi
    compose up "${PASSTHROUGH_ARGS[@]}" --no-deps "${REQUESTED_SERVICES[@]}"
    ;;
  config)
    parse_args "$@"
    if ((${#REQUESTED_SERVICES[@]} == 0)); then
      load_resolved_services
    fi
    printf '%s\n' "${REQUESTED_SERVICES[@]}"
    ;;
  down|ps|logs)
    compose "${COMMAND}" "$@"
    ;;
  *)
    die "unknown command: ${COMMAND}"
    ;;
esac
