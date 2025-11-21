#!/usr/bin/env bash
# Tail PostgreSQL query logs via docker exec

docker exec -it demo-osiv-db-1 tail -f /var/lib/postgresql/data/log/postgresql.log
