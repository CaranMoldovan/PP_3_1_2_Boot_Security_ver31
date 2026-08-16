#!/usr/bin/env bash
#
# Личный VPN: Xray-core, VLESS + Reality, транспорт TCP (XTLS-Vision).
# Один пользователь — одна ссылка. Запускать на чистом VPS от root.
#
#   sudo bash install.sh
#
# Переменные окружения (все опциональны):
#   PORT=443                       порт, на котором слушает сервер
#   SNI=www.microsoft.com          чужой сайт, под который маскируемся (Reality dest)
#   SERVER_IP=1.2.3.4              внешний адрес (по умолчанию определяется сам)
#   LABEL=MyVPN                    имя профиля в клиенте
#   XRAY_IMAGE=ghcr.io/xtls/xray-core:latest
#   BLOCK_TORRENT=1                резать bittorrent (1) или нет (0)
#   INSTALL_DIR=/opt/xray-vpn
#   NO_QR=0                        1 — не печатать QR (например, при запуске из CI)
#
set -euo pipefail

PORT="${PORT:-443}"
SNI="${SNI:-www.microsoft.com}"
LABEL="${LABEL:-MyVPN}"
XRAY_IMAGE="${XRAY_IMAGE:-ghcr.io/xtls/xray-core:latest}"
BLOCK_TORRENT="${BLOCK_TORRENT:-1}"
INSTALL_DIR="${INSTALL_DIR:-/opt/xray-vpn}"
NO_QR="${NO_QR:-0}"

log()  { printf '\033[1;32m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[!]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[x]\033[0m %s\n' "$*" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || die "Запускать от root: sudo bash install.sh"
[ "$(uname -s)" = "Linux" ] || die "Скрипт рассчитан на Linux-сервер"

# --------------------------------------------------------------------------
# 1. Базовые пакеты
# --------------------------------------------------------------------------
if command -v apt-get >/dev/null 2>&1; then
    PKG=apt
elif command -v dnf >/dev/null 2>&1; then
    PKG=dnf
elif command -v yum >/dev/null 2>&1; then
    PKG=yum
else
    die "Не нашёл apt/dnf/yum. Поддерживаются Debian/Ubuntu и RHEL-подобные."
fi

log "Ставлю базовые пакеты (curl, openssl, qrencode)"
case "$PKG" in
    apt)
        export DEBIAN_FRONTEND=noninteractive
        apt-get update -qq
        apt-get install -y -qq curl ca-certificates openssl qrencode iproute2 >/dev/null
        ;;
    dnf|yum)
        "$PKG" install -y -q curl ca-certificates openssl qrencode iproute >/dev/null
        ;;
esac

# --------------------------------------------------------------------------
# 2. Docker
# --------------------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
    log "Docker не найден — ставлю через get.docker.com"
    curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker >/dev/null 2>&1 || true
docker info >/dev/null 2>&1 || die "Docker установлен, но не запускается. Проверь: systemctl status docker"

if docker compose version >/dev/null 2>&1; then
    COMPOSE="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE="docker-compose"
else
    die "Нет docker compose. Поставь плагин docker-compose-plugin и запусти скрипт заново."
fi

# --------------------------------------------------------------------------
# 3. Проверки окружения
# --------------------------------------------------------------------------
if ss -Hltn "sport = :$PORT" 2>/dev/null | grep -q .; then
    if [ -d "$INSTALL_DIR" ] && docker ps --format '{{.Names}}' | grep -qx xray; then
        log "Порт $PORT занят нашим же контейнером xray — переустанавливаю поверх"
    else
        die "Порт $PORT уже занят другим процессом. Освободи его или запусти с PORT=8443."
    fi
fi

log "Проверяю, годится ли $SNI как маскировка (нужен TLS 1.3 + HTTP/2)"
if command -v openssl >/dev/null 2>&1; then
    tls_out="$(echo | timeout 10 openssl s_client -connect "${SNI}:443" -servername "$SNI" -alpn h2 -tls1_3 2>/dev/null || true)"
    if echo "$tls_out" | grep -q "TLSv1.3"; then
        echo "$tls_out" | grep -q "ALPN protocol: h2" \
            && log "$SNI подходит: TLS 1.3 + h2" \
            || warn "$SNI отдаёт TLS 1.3, но без h2 — маскировка будет слабее"
    else
        warn "Не удалось подтвердить TLS 1.3 у $SNI. Продолжаю, но лучше выбрать другой SNI."
    fi
fi

SERVER_IP="${SERVER_IP:-}"
if [ -z "$SERVER_IP" ]; then
    for u in https://api.ipify.org https://ifconfig.me https://icanhazip.com; do
        SERVER_IP="$(curl -4fsS --max-time 8 "$u" 2>/dev/null | tr -d '[:space:]' || true)"
        [ -n "$SERVER_IP" ] && break
    done
fi
[ -n "$SERVER_IP" ] || die "Не смог определить внешний IP. Запусти с SERVER_IP=<твой_ip>"
log "Внешний адрес сервера: $SERVER_IP"

# --------------------------------------------------------------------------
# 4. Ключи и идентификаторы
# --------------------------------------------------------------------------
mkdir -p "$INSTALL_DIR"
cd "$INSTALL_DIR"

log "Тяну образ $XRAY_IMAGE"
docker pull -q "$XRAY_IMAGE" >/dev/null

UUID="$(cat /proc/sys/kernel/random/uuid)"
SHORT_ID="$(openssl rand -hex 8)"

# xray x25519 печатает две строки вида "<подпись>: <значение>";
# названия подписей менялись между версиями, поэтому берём значения по позиции.
keys_out="$(docker run --rm "$XRAY_IMAGE" x25519 2>/dev/null \
    || docker run --rm --entrypoint xray "$XRAY_IMAGE" x25519)"
PRIVATE_KEY="$(printf '%s\n' "$keys_out" | sed -n '1s/.*: *//p' | tr -d '[:space:]')"
PUBLIC_KEY="$(printf '%s\n' "$keys_out" | sed -n '2s/.*: *//p' | tr -d '[:space:]')"
[ -n "$PRIVATE_KEY" ] && [ -n "$PUBLIC_KEY" ] || die "Не смог сгенерировать пару ключей Reality"

# --------------------------------------------------------------------------
# 5. Конфиг Xray
# --------------------------------------------------------------------------
torrent_rule=""
if [ "$BLOCK_TORRENT" = "1" ]; then
    torrent_rule='
      { "type": "field", "protocol": ["bittorrent"], "outboundTag": "block" },'
fi

log "Пишу конфиг $INSTALL_DIR/config.json"
cat > config.json <<EOF
{
  "log": { "loglevel": "warning" },
  "inbounds": [
    {
      "tag": "vless-reality",
      "listen": "0.0.0.0",
      "port": $PORT,
      "protocol": "vless",
      "settings": {
        "clients": [
          { "id": "$UUID", "flow": "xtls-rprx-vision", "email": "owner" }
        ],
        "decryption": "none"
      },
      "streamSettings": {
        "network": "tcp",
        "security": "reality",
        "realitySettings": {
          "show": false,
          "dest": "$SNI:443",
          "xver": 0,
          "serverNames": ["$SNI"],
          "privateKey": "$PRIVATE_KEY",
          "shortIds": ["$SHORT_ID"]
        }
      },
      "sniffing": {
        "enabled": true,
        "destOverride": ["http", "tls", "quic"],
        "routeOnly": true
      }
    }
  ],
  "outbounds": [
    { "protocol": "freedom", "tag": "direct" },
    { "protocol": "blackhole", "tag": "block" }
  ],
  "routing": {
    "domainStrategy": "IPIfNonMatch",
    "rules": [
      { "type": "field", "ip": ["geoip:private"], "outboundTag": "block" },$torrent_rule
      { "type": "field", "network": "tcp,udp", "outboundTag": "direct" }
    ]
  },
  "dns": {
    "servers": ["1.1.1.1", "8.8.8.8", "localhost"]
  }
}
EOF

cat > docker-compose.yml <<EOF
services:
  xray:
    image: $XRAY_IMAGE
    container_name: xray
    restart: unless-stopped
    network_mode: host
    volumes:
      - ./config.json:/etc/xray/config.json:ro
    command: ["run", "-config", "/etc/xray/config.json"]
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
EOF

cat > .env <<EOF
# Состояние установки. Файл содержит приватный ключ — не публиковать.
SERVER_IP=$SERVER_IP
PORT=$PORT
SNI=$SNI
LABEL=$LABEL
UUID=$UUID
SHORT_ID=$SHORT_ID
PUBLIC_KEY=$PUBLIC_KEY
PRIVATE_KEY=$PRIVATE_KEY
XRAY_IMAGE=$XRAY_IMAGE
EOF
chmod 600 .env config.json

# Вспомогательные скрипты рядом с конфигом: показать ссылку и снести всё.
cat > link.sh <<'EOS'
#!/usr/bin/env bash
# Печатает ссылку подключения и QR-код.
set -euo pipefail
cd "$(dirname "$(readlink -f "$0")")"
# shellcheck disable=SC1091
. ./.env
LINK="vless://${UUID}@${SERVER_IP}:${PORT}?type=tcp&security=reality&encryption=none&flow=xtls-rprx-vision&fp=chrome&sni=${SNI}&pbk=${PUBLIC_KEY}&sid=${SHORT_ID}#${LABEL}"
echo "$LINK"
echo
if [ "${NO_QR:-0}" != "1" ] && command -v qrencode >/dev/null 2>&1; then
    qrencode -t ANSIUTF8 -m 1 "$LINK"
fi
EOS

cat > uninstall.sh <<'EOS'
#!/usr/bin/env bash
# Полностью удаляет VPN: контейнер, конфиги, ключи.
set -euo pipefail
DIR="$(dirname "$(readlink -f "$0")")"
cd "$DIR"
(docker compose down 2>/dev/null || docker-compose down 2>/dev/null) || true
rm -f /etc/sysctl.d/99-xray-vpn.conf
cd /
rm -rf "$DIR"
echo "VPN удалён."
EOS

chmod +x link.sh uninstall.sh

# --------------------------------------------------------------------------
# 6. Сеть: BBR + фаервол
# --------------------------------------------------------------------------
log "Включаю BBR"
cat > /etc/sysctl.d/99-xray-vpn.conf <<'EOF'
net.core.default_qdisc = fq
net.ipv4.tcp_congestion_control = bbr
net.ipv4.tcp_fastopen = 3
EOF
sysctl --system >/dev/null 2>&1 || true

if command -v ufw >/dev/null 2>&1 && ufw status 2>/dev/null | grep -q "Status: active"; then
    log "ufw активен — открываю $PORT/tcp"
    ufw allow "$PORT"/tcp >/dev/null
elif command -v firewall-cmd >/dev/null 2>&1 && firewall-cmd --state >/dev/null 2>&1; then
    log "firewalld активен — открываю $PORT/tcp"
    firewall-cmd --permanent --add-port="$PORT"/tcp >/dev/null
    firewall-cmd --reload >/dev/null
else
    warn "Локальный фаервол не активен. Если у провайдера есть внешний firewall — открой там TCP/$PORT."
fi

# --------------------------------------------------------------------------
# 7. Запуск
# --------------------------------------------------------------------------
log "Запускаю Xray"
$COMPOSE up -d --force-recreate >/dev/null

sleep 3
if ! docker ps --format '{{.Names}}' | grep -qx xray; then
    docker logs xray 2>&1 | tail -30 || true
    die "Контейнер xray не поднялся, логи выше"
fi
if ! ss -Hltn "sport = :$PORT" 2>/dev/null | grep -q .; then
    docker logs xray 2>&1 | tail -30 || true
    die "Xray запущен, но порт $PORT не слушается — смотри логи выше"
fi
log "Xray работает, порт $PORT слушается"

# --------------------------------------------------------------------------
# 8. Ссылка
# --------------------------------------------------------------------------
LINK="vless://${UUID}@${SERVER_IP}:${PORT}?type=tcp&security=reality&encryption=none&flow=xtls-rprx-vision&fp=chrome&sni=${SNI}&pbk=${PUBLIC_KEY}&sid=${SHORT_ID}#${LABEL}"

echo
echo "=============================================================="
echo " Готово. Твоя личная ссылка (только для тебя, никому не давай):"
echo "=============================================================="
echo
echo "$LINK"
echo
if [ "$NO_QR" != "1" ] && command -v qrencode >/dev/null 2>&1; then
    qrencode -t ANSIUTF8 -m 1 "$LINK"
    echo
fi
echo "Ссылка сохранена: $INSTALL_DIR/.env  (показать снова: bash $INSTALL_DIR/link.sh)"
echo "Клиенты: v2rayNG (Android), Streisand/Shadowrocket (iOS), Hiddify/v2rayN (Windows/macOS/Linux)."
echo
