#!/usr/bin/env bash
# Creates ignored local secrets without changing any application YAML/properties.
set -euo pipefail
umask 077
backend_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[[ ! -e "$backend_dir/.env" ]] || { printf '.env already exists; keeping it unchanged.\n'; exit 0; }
command -v openssl >/dev/null
temporary="$(mktemp "$backend_dir/.env.XXXXXX")"
trap 'rm -f "$temporary"' EXIT
while IFS= read -r line || [[ -n "$line" ]]; do
    case "$line" in
        JWT_SECRET_KEY=) printf 'JWT_SECRET_KEY=%s\n' "$(openssl rand -base64 32)" ;;
        DB_PASSWORD=) printf 'DB_PASSWORD=%s\n' "$(openssl rand -hex 24)" ;;
        *) printf '%s\n' "$line" ;;
    esac
done < "$backend_dir/.env.example" > "$temporary"
# A concurrent initializer must never overwrite an existing private environment.
ln "$temporary" "$backend_dir/.env"
chmod 600 "$backend_dir/.env"
printf 'Created backend/.env with fresh local secrets. Values were not printed.\n'

