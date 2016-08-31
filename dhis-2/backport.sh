#!/bin/bash

REV=0
TOREV=0

if [ -z "$1" ]; then
  echo "Usage: $0 rev [to-rev]"
  exit 1
fi

if [ -z "$2" ]; then
  REV=$(($1 - 1))
  TOREV=$(($1))
  else
  REV=$(($1))
  TOREV=$(($2))
fi

echo "Using revision ${REV}..${TOREV}"
bzr revert
echo "Reverted local changes"
bzr up
echo "Updated branch"

# Update lp:dhis2 with local repo for faster merge
bzr merge lp:dhis2 -r ${REV}..${TOREV}
echo "Merged revision ${TOREV}"
echo "To commit changes: bzr commit -m \"R ${TOREV}\""
