#!/usr/bin/env bash
# Prints the resources.env of one or more Gatling run directories as a Markdown table, a column per
# run, as written by run-simulation.sh: what each part used and waited for during the run. With
# --warnings, prints each run's warnings as a Markdown list instead, or a sentence if there are none.
#
#   scripts/resources-table.sh [--warnings] <run directory>...
set -euo pipefail

warnings_only=false
if [ "${1:-}" = "--warnings" ]; then
  warnings_only=true
  shift
fi

if [ $# -eq 0 ]; then
  sed -n '2,6p' "$0"
  exit 1
fi

# value <run directory> <key>: the key's value in its resources.env, or nothing
value() {
  sed -n "s/^$2=\"\{0,1\}\([^\"]*\)\"\{0,1\}$/\1/p" "$1/resources.env" 2>/dev/null | head -n 1
}

# label <run directory>: the part of its name after Gatling's timestamp, such as baseline
label() {
  local name
  name=$(basename "$1")
  name=$(sed 's/^.*-[0-9]\{17\}-\{0,1\}//' <<< "$name")
  echo "${name:-$(basename "$1")}"
}

# row <title> <key> <format> [<quota key>]: a row with the key's value in each run, formatted,
# or n/a in a run whose quota key reads unlimited
row() {
  local title="$1" key="$2" format="$3" quota_key="${4:-}"
  local line="| $title |" dir v
  for dir in "${dirs[@]}"; do
    v=$(value "$dir" "$key")
    if [ -n "$quota_key" ] && [ "$(value "$dir" "$quota_key")" = unlimited ]; then
      line+=" n/a |"
    elif [ -z "$v" ] && [ "$format" = none-if-empty ] && [ -f "$dir/resources.env" ]; then
      line+=" none |"
    elif [ -z "$v" ]; then
      line+=" n/a |"
    else
      case $format in
        gib) line+=" $(awk -v v="$v" 'BEGIN { printf "%.1f GiB", v / 1073741824 }') |" ;;
        mib) line+=" $(awk -v v="$v" 'BEGIN { printf "%.0f MiB", v / 1048576 }') |" ;;
        none-if-empty) line+=" $v |" ;;
        count) line+=" $(awk -v v="$v" 'BEGIN {
          s = sprintf("%d", v); r = ""
          while (length(s) > 3) { r = "," substr(s, length(s) - 2) r; s = substr(s, 1, length(s) - 3) }
          print s r
        }') |" ;;
        *) line+=" $(printf "$format" "$v") |" ;;
      esac
    fi
  done
  echo "$line"
}

dirs=("$@")

if [ "$warnings_only" = true ]; then
  found=false
  for dir in "${dirs[@]}"; do
    w=$(value "$dir" RESOURCES_WARNINGS)
    if [ -n "$w" ]; then
      echo "* ⚠️ **$(label "$dir")**: $w"
      found=true
    fi
  done
  if [ "$found" = false ]; then
    echo "No part waited for a CPU or was held back by its cpus quota, and Postgres didn't wait for I/O."
  fi
  exit 0
fi

header="| |"
separator="|---|"
for dir in "${dirs[@]}"; do
  header+=" $(label "$dir") |"
  separator+="---|"
done

echo "$header"
echo "$separator"
row "Duration" DURATION_S "%s s"
row "DHIS2: cores used" WEB_CORES "%s"
row "DHIS2: waited for a CPU" WEB_CPU_WAIT_PCT "%s%%"
row "DHIS2: held back by its cpus quota" WEB_THROTTLED_MS_PER_S "%s ms/s" WEB_CPUS
row "DHIS2: peak memory" WEB_MEMORY_PEAK_BYTES gib
row "Postgres: cores used" DB_CORES "%s"
row "Postgres: waited for a CPU" DB_CPU_WAIT_PCT "%s%%"
row "Postgres: held back by its cpus quota" DB_THROTTLED_MS_PER_S "%s ms/s" DB_CPUS
row "Postgres: waited for I/O" DB_IO_WAIT_PCT "%s%%"
row "Postgres: peak memory" DB_MEMORY_PEAK_BYTES gib
row "Postgres: blocks found in shared_buffers" PG_HIT_PCT "%s%%"
row "Postgres: blocks read from outside" PG_BLKS_READ count
row "Postgres: temp files" PG_TEMP_BYTES mib
row "Postgres: commits" PG_XACT_COMMIT count
row "Postgres: rows returned" PG_TUP_RETURNED count
row "Gatling: cores used" GATLING_CORES "%s"
row "Gatling: waited for a CPU" GATLING_CPU_WAIT_PCT "%s%%"
row "Host: cores used" HOST_CORES "%s"
row "Host: waited for a CPU" HOST_CPU_WAIT_PCT "%s%%"
row "DHIS2: cpus quota" WEB_CPUS "%s"
row "DHIS2: cpuset" WEB_CPUSET "%s"
row "Postgres: cpus quota" DB_CPUS "%s"
row "Postgres: cpuset" DB_CPUSET "%s"
row "Warnings" RESOURCES_WARNINGS none-if-empty
