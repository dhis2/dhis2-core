#!/usr/bin/env bash





#
## bash setup
#

if test "$BASH" = "" || "$BASH" -uc "a=();true \"\${a[@]}\"" 2>/dev/null; then
    # Bash 4.4, Zsh
    set -euo pipefail
else
    # Bash 4.3 and older chokes on empty arrays with set -u.
    set -eo pipefail
fi
set -x
shopt -s nullglob globstar





#
## setup
#

readonly app_repos=(
    "git@github.com:d2-ci/app-management-app.git"
    "git@github.com:d2-ci/cache-cleaner-app.git"
    "git@github.com:d2-ci/capture-app.git"
    "git@github.com:d2-ci/charts-app.git"
    "git@github.com:d2-ci/core-resource-app.git"
    "git@github.com:d2-ci/dashboards-app.git"
    "git@github.com:d2-ci/data-administration-app.git"
    "git@github.com:d2-ci/data-quality-app.git"
    "git@github.com:d2-ci/data-visualizer-app.git"
    "git@github.com:d2-ci/datastore-app.git"
    "git@github.com:d2-ci/event-capture-app.git"
    "git@github.com:d2-ci/event-charts-app.git"
    "git@github.com:d2-ci/event-reports-app.git"
    "git@github.com:d2-ci/gis-app.git"
    "git@github.com:d2-ci/import-export-app.git"
    "git@github.com:d2-ci/interpretation-app.git"
    "git@github.com:d2-ci/maintenance-app.git"
    "git@github.com:d2-ci/maps-app.git"
    "git@github.com:d2-ci/menu-management-app.git"
    "git@github.com:d2-ci/messaging-app.git"
    "git@github.com:d2-ci/pivot-tables-app.git"
    "git@github.com:d2-ci/scheduler-app.git"
    "git@github.com:d2-ci/settings-app.git"
    "git@github.com:d2-ci/tracker-capture-app.git"
    "git@github.com:d2-ci/translations-app.git"
    "git@github.com:d2-ci/user-app.git"
    "git@github.com:d2-ci/user-profile-app.git"
)

#
## functions
#

function app_name {
    local repo=$1
    local name=$(echo "${repo}" | sed -n "s/^.*d2-ci\/\(.*\)\.git$/\1/p")

    echo "$name"
}

function clone {
    local repo=$1
    local path=$2

    if [[ ! -d "$path" ]]; then
        git clone "${repo}" "${path}"
    else
        pushd "$path"
        git reset HEAD --hard
        git fetch origin HEAD
        git merge
        popd
    fi
}

function clone_all {
    local TEMP="./apps"
    for repo in "${app_repos[@]}"
    do
        local name=$(app_name "$repo")
        clone "$repo" "${TEMP}/${name}"
    done
}


#
## start
#

clone_all
