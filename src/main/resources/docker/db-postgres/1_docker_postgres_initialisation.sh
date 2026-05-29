#!/bin/bash

set -e
set -u

if [ -n "$POSTGRES_EXTRA_DATABASES" ]; then
  echo "Creating extra databases: $POSTGRES_EXTRA_DATABASES"
  for db in $(echo $POSTGRES_EXTRA_DATABASES | tr ',' ' '); do
    echo "  Creating database '$db'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" \
      -c "SELECT 1 FROM pg_database WHERE datname = '$db'" \
      | grep -q 1 || psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" \
      -c "CREATE DATABASE $db"
  done
  echo "Extra databases created"
fi
