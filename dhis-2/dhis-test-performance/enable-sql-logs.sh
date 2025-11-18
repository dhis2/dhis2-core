#!/usr/bin/env bash

# enable logging of all queries only on demand as it is expensive
echo "Enabling SQL query logging..."
docker compose exec db psql --username=dhis --quiet --command="ALTER SYSTEM SET log_min_duration_statement = 0;" > /dev/null
docker compose exec db psql --username=dhis --quiet --command="SELECT pg_reload_conf();" > /dev/null

# let postgres create a new log file so we only capture queries related to the test run
docker compose exec db rm -f /var/lib/postgresql/data/log/postgresql.log
docker compose exec db psql --username=dhis --quiet --command="SELECT pg_rotate_logfile();" > /dev/null
