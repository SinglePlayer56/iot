#!/bin/bash
set -e

PGDATA=/var/lib/postgresql/data

: "${MASTER_HOST:localhost}"
: "${REPL_USER:=repuser}"
: "${REPL_PASS:=secretPassword}"

echo "Cleaning PGDATA..."
rm -rf ${PGDATA:?}/*

echo "Running pg_basebackup..."
PGPASSWORD=$REPL_PASS pg_basebackup \
    -h "$MASTER_HOST" \
    -U "$REPL_USER" \
    -D "$PGDATA" \
    -Fp -Xs -P -R

echo "Fixing ownership and permissions..."
chown -R postgres:postgres "$PGDATA"
chmod 700 "$PGDATA"

exec su -s /bin/bash postgres -c "postgres"
