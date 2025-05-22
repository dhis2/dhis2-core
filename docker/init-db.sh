#!/bin/bash
set -e

# Wait for PostgreSQL to be ready
until pg_isready -h localhost -p 5432 -U dhis; do
  echo "Waiting for PostgreSQL to be ready..."
  sleep 2
done

# Create a temporary file to store the dump
TMP_DUMP="/tmp/dump.sql"

# Decompress the dump if it's gzipped
if [ -f /opt/dump/dump.sql.gz ]; then
    echo "Decompressing dump.sql.gz..."
    gunzip -c /opt/dump/dump.sql.gz > $TMP_DUMP
elif [ -f /opt/dump/dump.sql ]; then
    echo "Using existing dump.sql..."
    cp /opt/dump/dump.sql $TMP_DUMP
else
    echo "No dump file found in /opt/dump/"
    exit 1
fi

# Modify the dump to handle errors gracefully
echo "Modifying dump to handle errors gracefully..."
sed -i 's/ALTER TABLE ONLY public.potentialduplicate DROP CONSTRAINT potentialduplicate_lastupdatedby_user;/DO \$\$ BEGIN IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = '\''potentialduplicate_lastupdatedby_user'\'') THEN ALTER TABLE ONLY public.potentialduplicate DROP CONSTRAINT potentialduplicate_lastupdatedby_user; END IF; END \$\$;/' $TMP_DUMP

# Restore the modified dump
echo "Restoring database dump..."
psql -v ON_ERROR_STOP=0 -U dhis -d dhis -f $TMP_DUMP

echo "Database initialization completed." 