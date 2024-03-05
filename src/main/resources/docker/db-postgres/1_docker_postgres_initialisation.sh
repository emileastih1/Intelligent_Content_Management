#!/bin/bash

set -e
set -u

function create_user_and_database() {
  local database=$(echo $1 | cut -d',' -f1)
  local owner=$(echo $1 | cut -d',' -f2)
  local schema=$(echo $1 | cut -d',' -f3)
  echo "Creating user, database '$database', and schema '$schema' for owner '$owner'"
  psql -U postgres -c 'select 1' -d $database &>dev/null || psql -U postgres -tc 'create database '$database
	psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
	    CREATE USER $owner WITH PASSWORD 'changeme';
	    ALTER DATABASE $database OWNER TO $owner;
	    GRANT ALL ON DATABASE $database TO $owner;
	    \c $database $owner;
      CREATE SCHEMA IF NOT EXISTS $schema;
      ALTER ROLE $owner SET search_path TO $schema, public;
EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
	echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
	for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr '|' ' '); do
		create_user_and_database $db
	done
	echo "Multiple databases created"
fi