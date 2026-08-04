#!/usr/bin/env bash
# Forward Stripe webhooks (via Stripe CLI) to the local app.
# Before first use: stripe login (opens browser), then run this script.
# It prints a "webhook signing secret" — put it in .env as STRIPE_WEBHOOK_SECRET.
set -euo pipefail

STRIPE_BIN="${STRIPE_BIN:-stripe}"

exec "$STRIPE_BIN" listen --forward-to localhost:9898/api/webhook/stripe "$@"
