#!/usr/bin/env bash
set -euo pipefail

AVAIL="/etc/nginx/sites-available"
ENABLED="/etc/nginx/sites-enabled"
TARGET="$AVAIL/carhartt_config"
LINK="$ENABLED/default"

# 1) nginx 없으면 설치(최초 1회)
if ! command -v nginx >/dev/null 2>&1; then
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update -y
    apt-get install -y nginx
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y nginx
    systemctl enable --now nginx || true
  fi
  systemctl enable nginx || true
fi

# 2) nginx.conf에 sites-enabled include 보장(없으면 추가)
if ! grep -q "sites-enabled" /etc/nginx/nginx.conf; then
  sed -i '/http {/a \    include /etc/nginx/sites-enabled/*;' /etc/nginx/nginx.conf
fi

# 3) 심볼릭 링크: sites-enabled/default -> sites-available/carhartt_config
#    (이미 있으면 갱신, 파일이었으면 제거 후 링크로 교체)
if [ ! -f "$TARGET" ]; then
  echo "[ERROR] $TARGET 가 존재해야 합니다." >&2
  exit 1
fi
rm -f "$LINK"
ln -s "$TARGET" "$LINK"

# 4) (옵션) 원하면 리로드 — 기본은 비활성
# nginx -t && systemctl reload nginx || true
echo "[OK] nginx setup done (no reload by default)"