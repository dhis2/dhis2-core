#!/usr/bin/env bash
# What DHIS2, Postgres, and Maven and Gatling used and waited for during a Gatling run, from
# cgroup v2: the cores each used, and the share of the time one of its threads waited for a CPU,
# for I/O or for memory (PSI "some"). Run from the directory of docker-compose.yml.
#
#   scripts/resources.sh snapshot                      prints the counters now
#   scripts/resources.sh diff <before> <after>         prints resources.env from two snapshots
#   scripts/resources.sh warnings <run directory>      prints each wait above the threshold
#   scripts/resources.sh table <run directory>...      prints a Markdown table, a column per run
#   scripts/resources.sh summary <run directory>...    prints the warnings and the table, collapsed,
#                                                      for the GitHub job summary
set -euo pipefail

# A wait above this share of the run's time is flagged: its timings may include waiting
MAX_WAIT_PERCENT=1

usage() {
  sed -n '2,11p' "$0"
  exit 1
}

# cgroup <part>: the cgroup directory of the compose service web or db, or for gatling the one of
# this script, which runs mvn and Gatling: on the GitHub runner, the runner's service
cgroup() {
  local dirs=() dir id
  if [ "$1" = gatling ]; then
    dirs=("/sys/fs/cgroup$(sed -n 's/^0:://p' /proc/self/cgroup 2>/dev/null || true)")
  else
    id=$(docker compose ps --quiet "$1" 2>/dev/null | head -n 1 || true)
    [ -n "$id" ] || return 1
    # Docker's systemd cgroup driver, then cgroupfs
    dirs=("/sys/fs/cgroup/system.slice/docker-$id.scope" "/sys/fs/cgroup/docker/$id")
  fi
  for dir in "${dirs[@]}"; do
    if [ -f "$dir/cpu.stat" ]; then
      echo "$dir"
      return 0
    fi
  done
  return 1
}

snapshot() {
  # Linux only: elsewhere, such as macOS, resources.env stays empty
  [ -f /proc/stat ] || return 0
  echo "time_ns $(date +%s%N)"
  # Every CPU's busy time: user, nice, system, irq and softirq, in USER_HZ
  awk -v hz="$(getconf CLK_TCK)" '$1 == "cpu" {
    printf "host.cpu_usec %.0f\n", ($2 + $3 + $4 + $7 + $8) * 1000000 / hz; exit
  }' /proc/stat
  local part dir files resource
  for part in web db gatling; do
    dir=$(cgroup "$part") || continue
    files=("$dir/cpu.stat")
    for resource in cpu io memory; do
      if [ -f "$dir/$resource.pressure" ]; then
        files+=("$dir/$resource.pressure")
      fi
    done
    # The "some" line's total: microseconds during which a thread of the part waited
    awk -v part="$part" '
      FILENAME ~ /cpu\.stat$/ && $1 == "usage_usec" { print part ".cpu_usec", $2 }
      FILENAME ~ /\.pressure$/ && $1 == "some" {
        resource = FILENAME
        sub(/.*\//, "", resource)
        sub(/\.pressure$/, "", resource)
        sub("total=", "", $5)
        print part "." resource "_wait_usec", $5
      }' "${files[@]}"
  done
}

diff_snapshots() {
  echo "# What each part used and waited for during mvn gatling:test (see scripts/resources.sh)"
  awk '
    NR == FNR { before[$1] = $2; next }
    $1 in before { d[$1] = $2 - before[$1] }
    function cores(part) {
      if (part ".cpu_usec" in d)
        printf "%s_CORES=%.2f\n", toupper(part), d[part ".cpu_usec"] / 1e6 / s
    }
    function wait(part, resource) {
      if (part "." resource "_wait_usec" in d)
        printf "%s_%s_WAIT_PERCENT=%.2f\n", toupper(part), toupper(resource),
          d[part "." resource "_wait_usec"] / 1e4 / s
    }
    END {
      if (!("time_ns" in d))
        exit
      s = d["time_ns"] / 1e9
      printf "DURATION_SECONDS=%.1f\n", s
      cores("web"); wait("web", "cpu"); wait("web", "memory")
      cores("db"); wait("db", "cpu"); wait("db", "io"); wait("db", "memory")
      cores("gatling"); wait("gatling", "cpu")
      cores("host")
    }' "$1" "$2"
}

# value <run directory> <key>: the key's value in its resources.env, or nothing
value() {
  [ -f "$1/resources.env" ] || return 0
  sed -n "s/^$2=//p" "$1/resources.env" | head -n 1
}

warnings() {
  # One line per wait above MAX_WAIT_PERCENT, and one naming the parts whose waits are missing
  [ -f "$1/resources.env" ] || return 0
  awk -F= -v max="$MAX_WAIT_PERCENT" '
    { v[$1] = $2 }
    function check(key, part, who, what) {
      if (!(key in v)) {
        if (index(missing, part) == 0)
          missing = missing (missing == "" ? "" : ", ") part
      } else if (v[key] > max) {
        printf "%s waited for %s %.1f%% of the time\n", who, what, v[key]
      }
    }
    END {
      check("WEB_CPU_WAIT_PERCENT", "DHIS2", "a DHIS2 thread", "a CPU")
      check("DB_CPU_WAIT_PERCENT", "Postgres", "a Postgres process", "a CPU")
      check("GATLING_CPU_WAIT_PERCENT", "Maven and Gatling", "a Maven or Gatling thread", "a CPU")
      check("DB_IO_WAIT_PERCENT", "Postgres", "a Postgres process", "I/O")
      check("WEB_MEMORY_WAIT_PERCENT", "DHIS2", "a DHIS2 thread", "memory")
      check("DB_MEMORY_WAIT_PERCENT", "Postgres", "a Postgres process", "memory")
      if (missing != "")
        printf "waits of %s not measured: no cgroup v2 counters\n", missing
    }' "$1/resources.env"
}

# label <run directory>: the part of its name after Gatling's timestamp, such as baseline
label() {
  local name
  name=$(basename "$1")
  if [[ $name =~ -[0-9]{17}-(.+)$ ]]; then
    echo "${BASH_REMATCH[1]}"
  else
    echo "$name"
  fi
}

table() {
  local dirs=("$@") header="| |" separator="|---|" dir
  for dir in "${dirs[@]}"; do
    header+=" $(label "$dir") |"
    separator+="---|"
  done
  echo "$header"
  echo "$separator"
  local title key unit line v
  while IFS='|' read -r title key unit; do
    line="| $title |"
    for dir in "${dirs[@]}"; do
      v=$(value "$dir" "$key")
      if [ -n "$v" ]; then
        line+=" $v$unit |"
      else
        line+=" n/a |"
      fi
    done
    echo "$line"
  done <<'EOF'
Duration|DURATION_SECONDS| s
DHIS2: cores used|WEB_CORES|
DHIS2: waited for a CPU|WEB_CPU_WAIT_PERCENT|%
DHIS2: waited for memory|WEB_MEMORY_WAIT_PERCENT|%
Postgres: cores used|DB_CORES|
Postgres: waited for a CPU|DB_CPU_WAIT_PERCENT|%
Postgres: waited for I/O|DB_IO_WAIT_PERCENT|%
Postgres: waited for memory|DB_MEMORY_WAIT_PERCENT|%
Maven and Gatling: cores used|GATLING_CORES|
Maven and Gatling: waited for a CPU|GATLING_CPU_WAIT_PERCENT|%
Host: cores used|HOST_CORES|
EOF
  echo ""
  echo "Maven and Gatling: the cgroup of run-simulation.sh, on CI the GitHub runner's service,"
  echo "which includes the runner agent. Locally your terminal's."
}

summary() {
  local dirs=() dir warning found=false
  for dir in "$@"; do
    if [ -f "$dir/resources.env" ]; then
      dirs+=("$dir")
    fi
  done
  [ ${#dirs[@]} -gt 0 ] || return 0

  echo "**Resources during the measured run$([ ${#dirs[@]} -gt 1 ] && echo s):**"
  echo ""
  for dir in "${dirs[@]}"; do
    while IFS= read -r warning; do
      echo "* ⚠️ **$(label "$dir")**: $warning"
      found=true
    done < <(warnings "$dir")
  done
  if [ "$found" = false ]; then
    echo "No part waited for a CPU, I/O or memory more than ${MAX_WAIT_PERCENT}% of the time."
  fi
  echo ""
  echo "<details>"
  echo "<summary>What each part used and waited for</summary>"
  echo ""
  table "${dirs[@]}"
  echo ""
  echo "</details>"
  echo ""
}

[ $# -gt 0 ] || usage
command="$1"
shift
case "$command" in
  snapshot) snapshot ;;
  diff) [ $# -eq 2 ] || usage; diff_snapshots "$@" ;;
  warnings) [ $# -eq 1 ] || usage; warnings "$1" ;;
  table) [ $# -gt 0 ] || usage; table "$@" ;;
  summary) summary "$@" ;;
  *) usage ;;
esac
