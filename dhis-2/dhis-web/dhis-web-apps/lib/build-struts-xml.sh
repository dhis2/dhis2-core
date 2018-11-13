#!/usr/bin/env bash

function save_struts {
    local itemXml=$1
    local targetStruts=$2
                
    local pattern="<!-- Apps pulled from NPM -->"
    sed -i -e'
    /'"$pattern"'/ a\
    '"$itemXml"'
    ' "$targetStruts"
}

function struts_item {
    local name=$1
    echo "<package name=\"${name}\" extends=\"dhis-web-commons\" namespace=\"/${name}\"><action name=\"index\" class=\"org.hisp.dhis.commons.action.NoAction\"><result name=\"success\" type=\"redirect\">index.html</result></action></package>"
}

