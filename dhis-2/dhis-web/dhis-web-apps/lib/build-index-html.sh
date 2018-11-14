#!/usr/bin/env bash

function save_index {
    local itemHtml=$1
    local targetIndex=$2
    local targetBak="${targetIndex}.bak"

    local pattern="<!-- INJECT HTML HERE -->"

    sed '/'"$pattern"'/ a\
    '"$itemHtml"'
    ' "$targetIndex" > "$targetBak"
    mv "$targetBak" "$targetIndex"
}

function save_build_info {
    local targetIndex=$1
    local targetBak="${targetIndex}.bak"

    local sha=$(git rev-parse HEAD)
    local created=$(date)

    local info="<p>${created}<br>${sha}</p>"

    local pattern="<!-- INJECT BUILD INFO HERE -->"

    sed '/'"$pattern"'/ a\
    '"$info"'
    ' "$targetIndex" > "$targetBak"
    mv "$targetBak" "$targetIndex"
}

function list_item {
    local name=$1
    echo "<li><a href=\"../${name}\">${name}</a></li>"
}
