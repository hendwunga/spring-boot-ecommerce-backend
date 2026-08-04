#!/usr/bin/env bash
# Generate self-signed cert + key for the dev mock OIDC provider.
# Output: docker/oidc/cert.pem and docker/oidc/key.pem (gitignored, dev only).
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$DIR/docker/oidc"

mkdir -p "$OUT_DIR"

if [[ -f "$OUT_DIR/cert.pem" && -f "$OUT_DIR/key.pem" ]]; then
  echo "cert.pem / key.pem already exist in docker/oidc/ — skipping (remove them to regenerate)"
  exit 0
fi

openssl req -x509 -newkey rsa:2048 \
  -keyout "$OUT_DIR/key.pem" \
  -out "$OUT_DIR/cert.pem" \
  -days 3650 -nodes \
  -subj "/CN=mock-oidc" \
  -addext "subjectAltName=DNS:oidc,DNS:localhost,IP:127.0.0.1"

echo "Generated $OUT_DIR/cert.pem and $OUT_DIR/key.pem"
