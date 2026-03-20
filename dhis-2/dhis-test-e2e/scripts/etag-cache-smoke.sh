#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_MANIFEST="${SCRIPT_DIR}/../src/test/resources/cache/cache-routes.csv"

BASE_URL=""
USERNAME=""
PASSWORD=""
MANIFEST=""
COVERAGE=""

declare -a CLASSIFICATIONS=()
declare -a PATTERNS=()
declare -a PROBE_PATHS=()

usage() {
  cat <<'EOF'
Usage:
  etag-cache-smoke.sh --base-url <api-base-url> --username <user> --password <pass> [--manifest <file>]
  etag-cache-smoke.sh --base-url <api-base-url> --username <user> --password <pass> --coverage <coverage.csv> [--manifest <file>]

Examples:
  etag-cache-smoke.sh --base-url http://localhost:8080/api --username admin --password district
  etag-cache-smoke.sh --base-url http://localhost:8080/api --username admin --password district --coverage coverage.csv

Notes:
  - Pass the API base URL, typically ending in /api
  - The target server must run with cache.api.etag.enabled=on
  - Invalidation probes also require sql.dml.observer.enabled=on
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="$2"
      shift 2
      ;;
    --username)
      USERNAME="$2"
      shift 2
      ;;
    --password)
      PASSWORD="$2"
      shift 2
      ;;
    --manifest)
      MANIFEST="$2"
      shift 2
      ;;
    --coverage)
      COVERAGE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "${BASE_URL}" || -z "${USERNAME}" || -z "${PASSWORD}" ]]; then
  usage
  exit 1
fi

if [[ -z "${MANIFEST}" ]]; then
  MANIFEST="${DEFAULT_MANIFEST}"
fi

if [[ ! -f "${MANIFEST}" ]]; then
  echo "Manifest not found: ${MANIFEST}" >&2
  exit 1
fi

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "${value}"
}

load_manifest() {
  while IFS=, read -r classification pattern probe_path; do
    classification="$(trim "${classification:-}")"
    pattern="$(trim "${pattern:-}")"
    probe_path="$(trim "${probe_path:-}")"

    [[ -z "${classification}" ]] && continue
    [[ "${classification}" == "classification" ]] && continue
    [[ "${classification}" == \#* ]] && continue

    CLASSIFICATIONS+=("${classification}")
    PATTERNS+=("${pattern}")
    PROBE_PATHS+=("${probe_path}")
  done < "${MANIFEST}"
}

normalize_path() {
  local raw="$1"
  local base_path
  if [[ "${raw}" =~ ^https?:// ]]; then
    printf '%s' "${raw}"
    return
  fi

  raw="/${raw#/}"
  base_path="$(printf '%s' "${BASE_URL}" | sed -E 's#https?://[^/]+##')"

  if [[ -n "${base_path}" && "${base_path}" != "${BASE_URL}" ]]; then
    if [[ "${raw}" == "${base_path}" ]]; then
      raw="/"
    elif [[ "${raw}" == "${base_path}/"* ]]; then
      raw="/${raw#"${base_path}/"}"
    fi
  fi

  printf '%s' "${raw}"
}

classify_path() {
  local path="$1"
  local i

  for i in "${!PATTERNS[@]}"; do
    if [[ -n "${PATTERNS[$i]}" && "${path}" == ${PATTERNS[$i]} ]]; then
      printf '%s' "${CLASSIFICATIONS[$i]}"
      return
    fi
  done

  printf '%s' "suspect"
}

STATUS=""
ETAG=""
VARY=""
CACHE_CONTROL=""
BODY_BYTES=0
COOKIE_JAR="$(mktemp)"

cleanup() {
  rm -f "${COOKIE_JAR}"
}

trap cleanup EXIT

session_login() {
  local login_url response
  login_url="${BASE_URL%/}/auth/login"
  response="$(curl -sS -c "${COOKIE_JAR}" -H "Content-Type: application/json" \
    -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}" "${login_url}")"

  if [[ "${response}" != *'"loginStatus":"SUCCESS"'* ]]; then
    echo "Authentication failed against ${login_url}: ${response}" >&2
    exit 1
  fi
}

http_request() {
  local method="$1"
  local path="$2"
  local etag_header="${3:-}"
  local headers_file body_file url

  headers_file="$(mktemp)"
  body_file="$(mktemp)"
  url="${path}"
  if [[ ! "${url}" =~ ^https?:// ]]; then
    url="${BASE_URL%/}${path}"
  fi

  if [[ -n "${etag_header}" ]]; then
    curl -sS -b "${COOKIE_JAR}" -c "${COOKIE_JAR}" -X "${method}" -H "If-None-Match: ${etag_header}" -D "${headers_file}" -o "${body_file}" "${url}" >/dev/null
  else
    curl -sS -b "${COOKIE_JAR}" -c "${COOKIE_JAR}" -X "${method}" -D "${headers_file}" -o "${body_file}" "${url}" >/dev/null
  fi

  STATUS="$(awk '/^HTTP/{code=$2} END{print code}' "${headers_file}")"
  ETAG="$(awk -F': ' 'BEGIN{IGNORECASE=1} /^ETag:/{print $2}' "${headers_file}" | tail -n 1 | tr -d '\r')"
  VARY="$(awk -F': ' 'BEGIN{IGNORECASE=1} /^Vary:/{print $2}' "${headers_file}" | tail -n 1 | tr -d '\r')"
  CACHE_CONTROL="$(awk -F': ' 'BEGIN{IGNORECASE=1} /^Cache-Control:/{print $2}' "${headers_file}" | tail -n 1 | tr -d '\r')"
  BODY_BYTES="$(wc -c < "${body_file}" | tr -d ' ')"

  rm -f "${headers_file}" "${body_file}"
}

probe_path() {
  local classification="$1"
  local path="$2"
  local normalized first_status first_etag second_status second_etag result

  normalized="$(normalize_path "${path}")"
  result="OK"

  http_request "GET" "${normalized}"
  first_status="${STATUS}"
  first_etag="${ETAG}"

  case "${classification}" in
    cached)
      if [[ "${first_status}" != "200" || -z "${first_etag}" || -z "${VARY}" ]]; then
        result="MISMATCH"
      else
        http_request "GET" "${normalized}" "${first_etag}"
        second_status="${STATUS}"
        second_etag="${ETAG}"
        if [[ "${second_status}" != "304" || "${second_etag}" != "${first_etag}" || "${BODY_BYTES}" != "0" ]]; then
          result="MISMATCH"
        fi
      fi
      printf '%-8s %-9s %-10s etag=%-36s vary=%-24s cache-control=%s %s\n' \
        "${result}" "${classification}" "${first_status}/${second_status:-NA}" "${first_etag:-<none>}" "${VARY:-<none>}" "${CACHE_CONTROL:-<none>}" "${normalized}"
      ;;
    uncached)
      if [[ -n "${first_etag}" || -n "${VARY}" ]]; then
        result="MISMATCH"
      fi
      printf '%-8s %-9s %-10s etag=%-36s vary=%-24s cache-control=%s %s\n' \
        "${result}" "${classification}" "${first_status}" "${first_etag:-<none>}" "${VARY:-<none>}" "${CACHE_CONTROL:-<none>}" "${normalized}"
      ;;
    *)
      printf '%-8s %-9s %-10s etag=%-36s vary=%-24s cache-control=%s %s\n' \
        "INFO" "${classification}" "${first_status}" "${first_etag:-<none>}" "${VARY:-<none>}" "${CACHE_CONTROL:-<none>}" "${normalized}"
      ;;
  esac
}

load_manifest
session_login

if [[ -n "${COVERAGE}" ]]; then
  if [[ ! -f "${COVERAGE}" ]]; then
    echo "Coverage file not found: ${COVERAGE}" >&2
    exit 1
  fi

  declare -A COVERAGE_PATHS=()
  while IFS= read -r line; do
    [[ -z "${line}" ]] && continue
    method="${line%%,*}"
    rest="${line#*,}"
    occurrences="${rest##*,}"
    url="${rest%,*}"
    if [[ "${method}" == "GET" ]]; then
      COVERAGE_PATHS["${url}"]="${occurrences}"
    fi
  done < "${COVERAGE}"

  for path in "${!COVERAGE_PATHS[@]}"; do
    classification="$(classify_path "$(normalize_path "${path}")")"
    probe_path "${classification}" "${path}"
  done
else
  for i in "${!CLASSIFICATIONS[@]}"; do
    if [[ -n "${PROBE_PATHS[$i]}" ]]; then
      probe_path "${CLASSIFICATIONS[$i]}" "${PROBE_PATHS[$i]}"
    fi
  done
fi
