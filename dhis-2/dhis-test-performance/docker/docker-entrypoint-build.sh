#!/usr/bin/env bash
# Copyright (c) 2014-2024 Docker PostgreSQL Authors
# SPDX-License-Identifier: MIT
#
# This script is derived from the official PostgreSQL Docker image entrypoint:
# https://github.com/docker-library/postgres/blob/master/14/bookworm/docker-entrypoint.sh
#
# Modified for build-time database initialization only (does not start postgres permanently)

set -Eeo pipefail

# append POSTGRES_HOST_AUTH_METHOD to pg_hba.conf for "host" connections
pg_setup_hba_conf() {
	local auth
	# check the default/configured encryption and use that as the auth method
	auth="$(postgres -C password_encryption)"
	: "${POSTGRES_HOST_AUTH_METHOD:=$auth}"
	{
		printf '\n'
		if [ 'trust' = "$POSTGRES_HOST_AUTH_METHOD" ]; then
			printf '# warning trust is enabled for all connections\n'
			printf '# see https://www.postgresql.org/docs/current/auth-trust.html\n'
		fi
		printf 'host all all all %s\n' "$POSTGRES_HOST_AUTH_METHOD"
	} >> "$PGDATA/pg_hba.conf"
}

# initialize empty PGDATA directory with new database via 'initdb'
docker_init_database_dir() {
	# "initdb" is particular about the current user existing in "/etc/passwd", so we use "nss_wrapper" to fake that if necessary
	local uid; uid="$(id -u)"
	if ! getent passwd "$uid" &> /dev/null; then
		local wrapper
		for wrapper in {/usr,}/lib{/*,}/libnss_wrapper.so; do
			if [ -s "$wrapper" ]; then
				NSS_WRAPPER_PASSWD="$(mktemp)"
				NSS_WRAPPER_GROUP="$(mktemp)"
				export LD_PRELOAD="$wrapper" NSS_WRAPPER_PASSWD NSS_WRAPPER_GROUP
				local gid; gid="$(id -g)"
				printf 'postgres:x:%s:%s:PostgreSQL:%s:/bin/false\n' "$uid" "$gid" "$PGDATA" > "$NSS_WRAPPER_PASSWD"
				printf 'postgres:x:%s:\n' "$gid" > "$NSS_WRAPPER_GROUP"
				break
			fi
		done
	fi

	if [ -n "${POSTGRES_INITDB_WALDIR:-}" ]; then
		set -- --waldir "$POSTGRES_INITDB_WALDIR" "$@"
	fi

	# --pwfile refuses to handle a properly-empty file (hence the "\n")
	eval 'initdb --username="$POSTGRES_USER" --pwfile=<(printf "%s\n" "$POSTGRES_PASSWORD") '"$POSTGRES_INITDB_ARGS"' "$@"'

	# unset/cleanup "nss_wrapper" bits
	if [[ "${LD_PRELOAD:-}" == */libnss_wrapper.so ]]; then
		rm -f "$NSS_WRAPPER_PASSWD" "$NSS_WRAPPER_GROUP"
		unset LD_PRELOAD NSS_WRAPPER_PASSWD NSS_WRAPPER_GROUP
	fi
}

# start socket-only postgresql server for setting up or running scripts
docker_temp_server_start() {
	# internal start of server in order to allow setup using psql client
	# does not listen on external TCP/IP and waits until start finishes
	set -- "$@" -c listen_addresses='' -p "${PGPORT:-5432}"

	PGUSER="${PGUSER:-$POSTGRES_USER}" \
	pg_ctl -D "$PGDATA" \
		-o "$(printf '%q ' "$@")" \
		-w start
}

# stop postgresql server after done setting up user and running scripts
docker_temp_server_stop() {
	PGUSER="${PGUSER:-postgres}" \
	pg_ctl -D "$PGDATA" -m fast -w stop
}

# Execute sql script, passed via stdin
docker_process_sql() {
	local query_runner=( psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --no-password --no-psqlrc )
	if [ -n "$POSTGRES_DB" ]; then
		query_runner+=( --dbname "$POSTGRES_DB" )
	fi

	# shellcheck disable=SC1007,SC2097,SC2098
	PGHOST= PGHOSTADDR= "${query_runner[@]}" "$@"
}

# create initial database
docker_setup_db() {
	local dbAlreadyExists
	dbAlreadyExists="$(
		# shellcheck disable=SC1007,SC2097,SC2098
		POSTGRES_DB= docker_process_sql --dbname postgres --set db="$POSTGRES_DB" --tuples-only <<-'EOSQL'
			SELECT 1 FROM pg_database WHERE datname = :'db' ;
		EOSQL
	)"
	if [ -z "$dbAlreadyExists" ]; then
		# shellcheck disable=SC1007,SC2097,SC2098
		POSTGRES_DB= docker_process_sql --dbname postgres --set db="$POSTGRES_DB" <<-'EOSQL'
			CREATE DATABASE :"db" ;
		EOSQL
		printf '\n'
	fi
}

# Main build initialization function
# This replicates what docker-entrypoint.sh does at runtime, but for build time
build_init() {
	echo "Initializing PostgreSQL database at build time..."

	# Create data directory
	mkdir -p "$PGDATA"
	chown -R postgres:postgres "$PGDATA"
	chmod 700 "$PGDATA"

	docker_init_database_dir

	pg_setup_hba_conf

	# Temporarily modify postgresql.conf for faster build (no fsync/sync writes)
	# Tuning optimized for dump restore:
	# * max_wal_size = 4GB: Sized for largest DB (hmis ~1.3GB compressed -> ~6-13GB uncompressed)
	#   Rule of thumb: 10-25% of uncompressed DB size. Reduces checkpoint frequency from
	#   every ~10s to ~30-60s, reducing I/O contention. Expected 20-30% speedup for large DBs.
	# * checkpoint_timeout = 30min: Prevents time-based checkpoints during restore
	# * maintenance_work_mem = 1GB: Increased from runtime 256MB to speed up index creation
	#   and sorting operations during restore
	{
		echo "fsync = off"
		echo "synchronous_commit = off"
		echo "full_page_writes = off"
		echo "max_wal_size = 4GB"
		echo "checkpoint_timeout = 30min"
		echo "maintenance_work_mem = 1GB"
	} >> "$PGDATA/postgresql.conf"

	docker_temp_server_start

	# Set password for postgres communication
	export PGPASSWORD="${PGPASSWORD:-$POSTGRES_PASSWORD}"

	docker_setup_db

	# Restore dump if provided
	if [ -f /tmp/dump.sql.gz ]; then
		echo "Restoring database dump..."
		gunzip -c /tmp/dump.sql.gz | docker_process_sql -d "$POSTGRES_DB"
		echo "Database dump restored successfully"
	fi

	docker_temp_server_stop
	unset PGPASSWORD

	# Clean up temporary postgresql.conf settings
	sed -i '/fsync =/d' "$PGDATA/postgresql.conf"
	sed -i '/synchronous_commit =/d' "$PGDATA/postgresql.conf"
	sed -i '/full_page_writes =/d' "$PGDATA/postgresql.conf"
	sed -i '/max_wal_size =/d' "$PGDATA/postgresql.conf"
	sed -i '/checkpoint_timeout =/d' "$PGDATA/postgresql.conf"
	sed -i '/maintenance_work_mem =/d' "$PGDATA/postgresql.conf"

	echo "PostgreSQL build init process complete."
}

build_init
