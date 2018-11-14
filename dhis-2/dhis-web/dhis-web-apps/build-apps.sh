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
shopt -s nullglob





#
## setup
#

readonly TARGET=$1
readonly ARTIFACT=$2

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
## helpers
#

source ./lib/git.sh
source ./lib/build-index-html.sh
source ./lib/build-struts-xml.sh

#
## functions
#

function blacklisted {
    local name=$1

    local blacklist=(
        'core-resource-app'
        'user-profile-app'
    )

    for e in "${blacklist[@]}"
    do
        [[ "$e" == "$name" ]] && return 0;
    done
    return 1
}

function app_pkg_name {
    app_dir=$1
    app_pkg="${app_dir}/package.json"

    while read a b ; do 
        [ "$a" = '"name":' ] && { b="${b%\"*}" ; echo "${b#\"}" ; break ; }
    done < "$app_pkg"
}

function clone_all {
    local TEMP="${TARGET}/apps"

    for repo in "${app_repos[@]}"
    do
        local name=$(app_name "$repo")
        clone "$repo" "${TEMP}/${name}"
    done
}

function build_index {
    local templatePath="./src/main/webapp/dhis-web-apps/template.html"
    local targetIndex="${TARGET}/${ARTIFACT}/$ARTIFACT/index.html" 

    mkdir -p "${TARGET}/${ARTIFACT}/$ARTIFACT/"
    cp "$templatePath" "$targetIndex"

    for dir in $TARGET/apps/*/
    do
        local pkg_name=$(app_pkg_name "$dir")
        local name=${pkg_name//-app/}

        local item=$(list_item "dhis-web-${name}")
        save_index "$item" "$targetIndex"
    done

    save_build_info "$targetIndex" 
}

function build_struts {
    local templatePath="./src/main/resources/struts.xml"
    local targetStruts="${TARGET}/classes/struts.xml"  

    mkdir -p "${TARGET}/classes"
    cp "$templatePath" "$targetStruts"

    for dir in $TARGET/apps/*/
    do
        local pkg_name=$(app_pkg_name "$dir")

        if blacklisted "$pkg_name"; then
            echo "blacklisted ${pkg_name} from struts.xml"
            continue
        fi

        local name=${pkg_name//-app/}

        local item=$(struts_item "dhis-web-${name}")
        save_struts "$item" "$targetStruts"
    done
}

function copy_apps {
    for dir in $TARGET/apps/*/
    do
        local pkg_name=$(app_pkg_name "$dir")
        local name=${pkg_name//-app/}

        local src="$dir"
        local dest="${TARGET}/${ARTIFACT}/dhis-web-${name}"
        mkdir -p "$dest"

        #cp --archive --verbose "$src" "$dest"
        pushd "$dir"
        git archive HEAD | tar -x -C "$dest"
        popd
    done
}

function main {
    clone_all
    build_index 
    build_struts
    copy_apps
}





#
## start
#

if [[ ! -d "${TARGET}/apps" ]]; then
    main
else
    echo "Using existing apps, use 'mvn clean install' to update apps"
fi
