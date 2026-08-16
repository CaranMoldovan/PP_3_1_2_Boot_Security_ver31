#!/usr/bin/env bash
#
# Компактный установщик того же VPN (VLESS + Reality + TCP), но без Docker:
# ставит Xray из официального репозитория и держит его через systemd.
#
# Рассчитан на то, чтобы его целиком вставили в консоль VM (noVNC в Proxmox),
# когда нет SSH/scp. Функционально равен install.sh, только легче и без Docker.
#
#   sudo bash quick-console.sh
#   PORT=8443 SNI=www.nvidia.com sudo -E bash quick-console.sh
#
set -euo pipefail

PORT="${PORT:-443}"
SNI="${SNI:-www.microsoft.com}"
LABEL="${LABEL:-MyVPN}"
NO_QR="${NO_QR:-0}"

[ "$(id -u)" -eq 0 ] || { echo "Нужен root: sudo bash quick-console.sh" >&2; exit 1; }

if command -v apt-get >/dev/null 2>&1; then
    export DEBIAN_FRONTEND=noninteractive
    apt-get update -qq && apt-get install -y -qq curl openssl qrencode ca-certificates >/dev/null
elif command -v dnf >/dev/null 2>&1; then
    dnf install -y -q curl openssl qrencode ca-certificates >/dev/null
fi

echo "==> Ставлю Xray"
bash -c "$(curl -fsSL https://github.com/XTLS/Xray-install/raw/main/install-release.sh)" @ install

UUID="$(cat /proc/sys/kernel/random/uuid)"
SID="$(openssl rand -hex 8)"
KEYS="$(xray x25519)"
PRIV="$(printf '%s\n' "$KEYS" | sed -n '1s/.*: *//p' | tr -d '[:space:]')"
PUB="$(printf '%s\n' "$KEYS" | sed -n '2s/.*: *//p' | tr -d '[:space:]')"
IP="${SERVER_IP:-$(curl -4fsS --max-time 10 https://api.ipify.org)}"
[ -n "$PRIV" ] && [ -n "$PUB" ] && [ -n "$IP" ] || { echo "Не собрал ключи/IP" >&2; exit 1; }

echo "==> Пишу конфиг"
mkdir -p /usr/local/etc/xray
cat > /usr/local/etc/xray/config.json <<EOF
{
  "log": { "loglevel": "warning" },
  "inbounds": [
    {
      "listen": "0.0.0.0",
      "port": $PORT,
      "protocol": "vless",
      "settings": {
        "clients": [{ "id": "$UUID", "flow": "xtls-rprx-vision" }],
        "decryption": "none"
      },
      "streamSettings": {
        "network": "tcp",
        "security": "reality",
        "realitySettings": {
          "dest": "$SNI:443",
          "serverNames": ["$SNI"],
          "privateKey": "$PRIV",
          "shortIds": ["$SID"]
        }
      },
      "sniffing": { "enabled": true, "destOverride": ["http", "tls", "quic"], "routeOnly": true }
    }
  ],
  "outbounds": [
    { "protocol": "freedom", "tag": "direct" },
    { "protocol": "blackhole", "tag": "block" }
  ],
  "routing": {
    "domainStrategy": "IPIfNonMatch",
    "rules": [
      { "type": "field", "ip": ["geoip:private"], "outboundTag": "block" },
      { "type": "field", "protocol": ["bittorrent"], "outboundTag": "block" }
    ]
  }
}
EOF
chmod 600 /usr/local/etc/xray/config.json

printf 'net.core.default_qdisc = fq\nnet.ipv4.tcp_congestion_control = bbr\n' > /etc/sysctl.d/99-xray-vpn.conf
sysctl --system >/dev/null 2>&1 || true

command -v ufw >/dev/null 2>&1 && ufw status 2>/dev/null | grep -q "Status: active" && ufw allow "$PORT"/tcp >/dev/null || true

systemctl enable xray >/dev/null 2>&1 || true
systemctl restart xray
sleep 2
systemctl is-active --quiet xray || { journalctl -u xray -n 30 --no-pager; echo "Xray не поднялся" >&2; exit 1; }

LINK="vless://${UUID}@${IP}:${PORT}?type=tcp&security=reality&encryption=none&flow=xtls-rprx-vision&fp=chrome&sni=${SNI}&pbk=${PUB}&sid=${SID}#${LABEL}"
echo "$LINK" > /root/vpn-link.txt
chmod 600 /root/vpn-link.txt

echo
echo "=============== ТВОЯ ССЫЛКА (никому не давай) ==============="
echo "$LINK"
echo
if [ "$NO_QR" != "1" ] && command -v qrencode >/dev/null 2>&1; then
    qrencode -t ANSIUTF8 -m 1 "$LINK"
fi
echo "Сохранена в /root/vpn-link.txt"
