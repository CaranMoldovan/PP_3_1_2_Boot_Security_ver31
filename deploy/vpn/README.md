# Личный VPN — VLESS + Reality (TCP)

Установщик приватного VPN-сервера на одного человека. Одна команда на сервере — одна ссылка для тебя.
К Spring Boot приложению из этого репозитория отношения не имеет, ничего в нём не меняет.

**Что поднимается:** Xray-core в Docker, протокол VLESS, транспорт **TCP** с XTLS-Vision,
шифрование **Reality** — сервер маскируется под чужой популярный сайт (по умолчанию `www.microsoft.com`),
свой сертификат и домен не нужны. Снаружи трафик выглядит как обычный HTTPS к этому сайту.

## Что нужно

* VPS с чистой Ubuntu 22.04/24.04 или Debian 12 (подойдёт и RHEL-подобная), root-доступ по SSH;
* открытый наружу TCP-порт `443` (если у провайдера есть внешний firewall — открыть там);
* больше ничего: домен, сертификат, панель не требуются.

## Установка

С локальной машины:

```bash
scp deploy/vpn/install.sh root@СЕРВЕР:/root/
ssh root@СЕРВЕР 'bash /root/install.sh'
```

Или прямо на сервере, если репозиторий там уже склонирован:

```bash
sudo bash deploy/vpn/install.sh
```

Скрипт сам поставит Docker, сгенерирует ключи, поднимет контейнер, включит BBR,
откроет порт в ufw/firewalld и в конце напечатает ссылку и QR-код.

Установка идемпотентна: повторный запуск пересоздаёт конфиг и **выдаёт новую ссылку**
(новые UUID и ключи), старая перестаёт работать.

### Настройки (переменные окружения)

| Переменная | По умолчанию | Зачем |
|---|---|---|
| `PORT` | `443` | порт сервера; поставь `8443`, если 443 занят |
| `SNI` | `www.microsoft.com` | под какой сайт маскируемся (Reality dest) |
| `SERVER_IP` | определяется сам | внешний IP, если автоопределение промахнулось |
| `LABEL` | `MyVPN` | имя профиля в клиенте |
| `BLOCK_TORRENT` | `1` | `0` — не резать bittorrent |
| `XRAY_IMAGE` | `ghcr.io/xtls/xray-core:latest` | зафиксировать версию образа |
| `INSTALL_DIR` | `/opt/xray-vpn` | куда ставить |

Пример:

```bash
sudo PORT=8443 SNI=www.nvidia.com LABEL=Home bash install.sh
```

Требования к `SNI`: чужой сайт с TLS 1.3 и HTTP/2, не заблокированный в твоей стране и не CDN твоего же
провайдера. Скрипт проверяет это и предупреждает, если сайт не подходит.

## Подключение

Ссылка вида `vless://…` вставляется в клиент как есть (или сканируется QR):

* **Android** — v2rayNG, Hiddify
* **iOS** — Streisand, Shadowrocket, V2Box
* **Windows / Linux** — v2rayN, Hiddify, Nekoray
* **macOS** — Hiddify, V2Box, FoXray

Ссылка — это и есть ключ доступа. Кто её получит, тот пользуется твоим сервером: не выкладывай её
в чаты и не коммить в git.

## Управление

Всё лежит в `/opt/xray-vpn`:

```bash
bash /opt/xray-vpn/link.sh              # показать ссылку и QR ещё раз
docker logs -f xray                     # логи
docker compose -f /opt/xray-vpn/docker-compose.yml restart
docker compose -f /opt/xray-vpn/docker-compose.yml pull && \
  docker compose -f /opt/xray-vpn/docker-compose.yml up -d   # обновить Xray
sudo bash /opt/xray-vpn/uninstall.sh    # снести всё
```

Файлы `.env` и `config.json` содержат приватный ключ Reality, права `600`. Бэкап `.env` = бэкап доступа.

## Если не подключается

1. `docker ps` — контейнер `xray` должен быть `Up`; если нет, смотри `docker logs xray`.
2. `ss -ltnp | grep 443` — порт должен слушаться.
3. С локальной машины: `nc -vz СЕРВЕР 443` — если таймаут, порт режет внешний firewall провайдера.
4. Проверь, что в клиенте включён `flow: xtls-rprx-vision`, `fingerprint: chrome` — при вставке ссылки
   это подставляется автоматически.
5. Часы на сервере должны быть точными (`timedatectl`) — Reality чувствителен к рассинхрону.
6. Если провайдер начал резать соединение — смени `SNI` и переустанови:
   `sudo SNI=www.cloudflare.com bash install.sh` (ссылка обновится).
