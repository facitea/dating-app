# Lightsail test deployment — 2026-11-03

- Server: geureomuri-server, Seoul ap-northeast-2a, Ubuntu 24.04, 2GB.
- Static IP: 3.34.208.199 (geureomuri-ip).
- Website: https://geureomuri.3.34.208.199.sslip.io
- Admin: https://geureomuri.3.34.208.199.sslip.io/admin/login
- Fresh MariaDB database `geureomuri`. Local members/photos were not migrated.
- Java 17, MariaDB 10.11 Ubuntu package, Nginx, Certbot.

## Persistent state

- DB: /var/lib/mysql
- Private photos: /srv/geureomuri/data/uploads
- App releases: /srv/geureomuri/releases; current symlink: /srv/geureomuri/current
- Private configuration: /etc/geureomuri/app.env and firebase-admin.json
- DB and backend bind to loopback only. Public API goes through Nginx HTTPS.
- Test credit top-ups are enabled for the test deployment (user request, 2026-11-03); disable before production. Exposed phone codes remain disabled.
- Firebase authorized domain includes the temporary hostname. Actual Google user login still requires user testing.
- New email/password accounts now use Firebase verification/reset emails (2026-11-04). Legacy local-account password recovery still needs separate support; see HANDOFF.md.

## Commands (SSH as ubuntu, use sudo)

```sh
sudo systemctl status geureomuri nginx mariadb
sudo journalctl -u geureomuri -f
sudo systemctl restart geureomuri
sudo /usr/local/sbin/geureomuri-backup
sudo systemctl list-timers geureomuri-backup.timer certbot.timer
```

Backups run daily at 03:00 KST (18:00 UTC), in /srv/geureomuri/backups.
An initial backup was also downloaded to the deployment PC. Ongoing off-server backup and retention are NOT configured yet; server-local backups do not cover server loss. Monitor backup disk usage.
Certificates auto-renew through certbot.timer. Keep port 80 open for HTTP validation; website redirects to HTTPS.

## Redeploy without deleting data

Build backend bootJar and frontend on the PC. Web build API uses the current HTTPS origin.
Upload app.jar and web assets into a NEW release directory. Do not copy local .env/.local or uploads.
Back up before deployment, change /srv/geureomuri/current symlink to the new release, restart geureomuri, then verify login.
Never delete /var/lib/mysql, /etc/geureomuri or /srv/geureomuri/data during redeployment.
Database schema currently uses Hibernate update; add versioned migrations before real production.

Android Firebase registration and native login/push are configured. Restore ignored google-services.json and the same debug signing key on a new PC. Actual device Google sign-in/push still require device verification. Current deployment: community-20261112; see changes-2026-11-12.md.

## Template limitations

deploy/lightsail/nginx.conf is the initial HTTP template. The running server has Certbot-managed HTTPS additions. Do not overwrite the active Nginx configuration with this template during routine redeploys. The deployment templates do not constitute a complete one-command fresh-server installer. Existing server configuration should be preserved.

For another PC, see NEW-PC.md. SSH key and tool installation paths are machine-specific; all repository paths in that guide are relative.
