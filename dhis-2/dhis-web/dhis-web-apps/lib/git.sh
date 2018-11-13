#!/usr/bin/env bash

function app_branch_name {
    local repo=$1
    local RHS=${repo#*\#}
    if [[ "$repo" == "$RHS" ]]; then
        echo "master"
    else
        echo "$RHS"
    fi
}

function app_name {
    local repo=$1
    local name=$(echo "${repo}" | sed -n "s/^.*d2-ci\/\(.*\)\.git$/\1/p")

    echo "$name"
}

function clone {
    local repo=$1
    local path=$2

    local branch=$(app_branch_name "$repo")

    if [[ ! -d "$path" ]]; then
        git clone --depth 1 -b "$branch" "$repo" "$path"
    else
        pushd "$path"
        git reset HEAD --hard
        git fetch origin "$branch"
        git checkout "$branch"
        git merge
        popd
    fi
}
