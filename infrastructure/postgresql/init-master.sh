#!/usr/bin/env bash
set -eu

: "${REPL_USER:=repuser}"
: "${REPL_PASS:=secretPassword}"

echo "host    replication     ${REPL_USER}     0.0.0.0/0     md5" >> "$PGDATA/pg_hba.conf"

psql -v ON_ERROR_STOP=1 --username postgres \
     --set=REPL_USER="$REPL_USER" --set=REPL_PASS="$REPL_PASS" <<'PSQL'
CREATE ROLE :"REPL_USER" WITH REPLICATION LOGIN PASSWORD :'REPL_PASS';
ALTER SYSTEM SET listen_addresses = '*';
SELECT pg_reload_conf(); -- применить изменения в конфиге (необходимы права superuser)
PSQL