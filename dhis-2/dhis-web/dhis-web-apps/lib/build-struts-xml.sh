#!/usr/bin/env bash

function save_struts {
    local itemXml=$1
    local targetStruts=$2
    local targetBak="${targetStruts}.bak"
                
    local pattern="<!-- INJECT BUNDLED APPS HERE -->"
    sed '/'"$pattern"'/ a\
    '"$itemXml"'
    ' "$targetStruts" > "$targetBak"
    mv "$targetBak" "$targetStruts"
}

function struts_item {
    local name=$1
    echo "<package name=\"${name}\" extends=\"dhis-web-commons\" namespace=\"/${name}\"><action name=\"index\" class=\"org.hisp.dhis.commons.action.NoAction\"><result name=\"success\" type=\"redirect\">index.html</result></action></package>"
}

