#!/usr/bin/env bash
set -euo pipefail

app_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
backend_host="${SWIFTAPP_API_HOST:-}"

cd "$app_dir"
if [[ -n "$backend_host" ]]; then
    exec flutter run "--dart-define=API_HOST=$backend_host" "$@"
fi

# Use ApiConfig's hosted default unless the caller explicitly supplies defines.
exec flutter run "$@"
