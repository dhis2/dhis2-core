#!/usr/bin/env bash

echo 'Patching DHIS2 ...'
curl -X GET https://raw.githubusercontent.com/dhis2/dhis2-server-setup/master/ci/scripts/dhis2-cve-patcher.sh -O
chmod +x dhis2-cve-patcher.sh
./dhis2-cve-patcher.sh

