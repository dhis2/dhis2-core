#!/usr/bin/env bash

function save_index {
    local itemHtml=$1
    local targetIndex=$2

    local pattern="<!-- INJECT HTML HERE -->"

    sed -i -e'
    /'"$pattern"'/ a\
    '"$itemHtml"'
    ' "$targetIndex"
}

function save_build_info {
    local targetIndex=$1

    local sha=$(git rev-parse HEAD)
    local created=$(date)

    local info="<p>${created}<br>${sha}</p>"

    local pattern="<!-- INJECT BUILD INFO HERE -->"

    sed -i -e'
    /'"$pattern"'/ a\
    '"$info"'
    ' "$targetIndex"
}

function list_item {
    local name=$1
    echo "<li><a href=\"../${name}\">${name}</a></li>"
}
