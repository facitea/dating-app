#!/bin/bash
set -euo pipefail
umask 077
exec 9>/run/geureomuri-backup.lock
flock -n 9 || exit 0
stamp=$(date -u +%Y%m%dT%H%M%SZ)
dest=/srv/geureomuri/backups/$stamp
mkdir -p "$dest"
mariadb-dump --single-transaction --routines --events geureomuri | gzip > "$dest/database.sql.gz"
tar --exclude=uploads/selfie-review --exclude=uploads/photo-history -czf "$dest/uploads.tar.gz" -C /srv/geureomuri/data uploads
tar -czf "$dest/config.tar.gz" -C / etc/geureomuri etc/nginx/sites-available/geureomuri etc/systemd/system/geureomuri.service
sha256sum "$dest"/*.gz > "$dest/SHA256SUMS"
echo "$dest"
