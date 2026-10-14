#!/usr/bin/env bash
# Run as the SSH user on Ubuntu before ./deploy.sh all. Never edits app secrets.
set -Eeuo pipefail
umask 077
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOMAIN="${SWIFT_DOMAIN:-swift-app.duckdns.org}"
SERVICE_USER="${SWIFT_DEPLOY_USER:-$(id -un)}"
[[ "$DOMAIN" =~ ^[a-z0-9][a-z0-9.-]+[a-z0-9]$ ]] || { echo 'Invalid domain' >&2; exit 2; }
[[ "$ROOT_DIR" =~ ^/[a-zA-Z0-9_./-]+$ && "$SERVICE_USER" =~ ^[a-z_][a-z0-9_-]*$ ]] || exit 2
[[ "$(uname -s)" == Linux ]] || { echo 'Run bootstrap on the Ubuntu server.' >&2; exit 2; }
sudo -n true
if ! command -v nginx >/dev/null || ! command -v certbot >/dev/null; then
    sudo apt-get update -qq
    sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y nginx certbot python3-certbot-nginx rsync curl
fi
command -v java >/dev/null
docker compose version >/dev/null
# Docker's PostgreSQL user must read this non-secret, single-file bind mount.
chmod 644 "$ROOT_DIR/docker/postgres/init-databases.sql"
mkdir -p "$ROOT_DIR/deployment/runtime" "$ROOT_DIR/deployment/backups"
chmod 700 "$ROOT_DIR/deployment/runtime" "$ROOT_DIR/deployment/backups"
sudo install -d -o "$SERVICE_USER" -g "$SERVICE_USER" -m 700 /var/lib/swiftapp /var/lib/swiftapp/driver-documents
env_file="$ROOT_DIR/deployment/runtime/service.env"
if [[ ! -e "$env_file" && -f "$ROOT_DIR/.env" ]]; then
    install -m 600 "$ROOT_DIR/.env" "$env_file"
fi
touch "$env_file"
chmod 600 "$env_file"
append_default() {
    if ! grep -q "^$1=" "$env_file"; then printf '%s=%s\n' "$1" "$2" >> "$env_file"; fi
}
append_default SERVER_ADDRESS 127.0.0.1
append_default DRIVER_DOCUMENTS_STORAGE_DIR /var/lib/swiftapp/driver-documents
firebase_source="$ROOT_DIR/noticationservice/src/main/resources/static/notification/swift-app-service-account.json"
firebase_target="$ROOT_DIR/deployment/runtime/firebase-service-account.json"
if [[ -f "$firebase_source" ]]; then
    if [[ -f "$firebase_target" ]] && ! cmp -s "$firebase_source" "$firebase_target"; then
        cp -p "$firebase_target" "$firebase_target.$(date -u +%Y%m%dT%H%M%SZ).bak"
    fi
    install -m 600 "$firebase_source" "$firebase_target"
fi
if [[ -f "$firebase_target" ]]; then append_default FIREBASE_CREDENTIALS "file:$firebase_target"; fi
render() { sed -e "s|@@ROOT@@|$ROOT_DIR|g" -e "s|@@USER@@|$SERVICE_USER|g" -e "s|@@DOMAIN@@|$DOMAIN|g" "$1"; }
render "$ROOT_DIR/deployment/swiftapp@.service.template" | sudo tee /etc/systemd/system/swiftapp@.service >/dev/null
sudo chmod 644 /etc/systemd/system/swiftapp@.service
sudo systemctl daemon-reload
sudo install -d -m 755 /var/www/letsencrypt /etc/nginx/snippets
sudo install -m 644 "$ROOT_DIR/deployment/nginx-locations.conf" /etc/nginx/snippets/swiftapp-locations.conf
sudo install -m 644 "$ROOT_DIR/deployment/nginx-headers.conf" /etc/nginx/snippets/swiftapp-headers.conf
printf '%s\n' 'map $http_upgrade $swiftapp_connection_upgrade {' '    default upgrade;' "    '' close;" '}' | sudo tee /etc/nginx/conf.d/swiftapp-upgrade.conf >/dev/null
if sudo test -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem"; then
    template="$ROOT_DIR/deployment/nginx-https.conf.template"
else
    template="$ROOT_DIR/deployment/nginx-http.conf.template"
fi
if sudo test -f /etc/nginx/sites-available/swiftapp; then
    sudo cp -p /etc/nginx/sites-available/swiftapp "$ROOT_DIR/deployment/backups/nginx.$(date -u +%Y%m%dT%H%M%SZ).conf"
fi
render "$template" | sudo tee /etc/nginx/sites-available/swiftapp >/dev/null
sudo chmod 644 /etc/nginx/sites-available/swiftapp
sudo ln -sfn /etc/nginx/sites-available/swiftapp /etc/nginx/sites-enabled/swiftapp
sudo nginx -t
sudo systemctl enable --now nginx
sudo systemctl reload nginx
if ! sudo test -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem"; then
    certbot_args=(certonly --webroot -w /var/www/letsencrypt -d "$DOMAIN" --non-interactive --agree-tos)
    if [[ -n "${SWIFT_TLS_EMAIL:-}" ]]; then certbot_args+=(--email "$SWIFT_TLS_EMAIL");
    else certbot_args+=(--register-unsafely-without-email); fi
    sudo certbot "${certbot_args[@]}"
fi
render "$ROOT_DIR/deployment/nginx-https.conf.template" | sudo tee /etc/nginx/sites-available/swiftapp >/dev/null
sudo nginx -t
sudo systemctl reload nginx
sudo install -d -m 755 /etc/letsencrypt/renewal-hooks/deploy
printf '%s\n' '#!/bin/sh' 'nginx -t && systemctl reload nginx' | sudo tee /etc/letsencrypt/renewal-hooks/deploy/swiftapp-nginx >/dev/null
sudo chmod 755 /etc/letsencrypt/renewal-hooks/deploy/swiftapp-nginx
sudo systemctl enable --now certbot.timer
printf 'Bootstrap ready: https://%s/swagger-ui.html and wss://%s/ws-native\n' "$DOMAIN" "$DOMAIN"
