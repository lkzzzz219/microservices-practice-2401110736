#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
for tool in curl python3; do
  command -v "$tool" >/dev/null || { echo "Missing command: $tool" >&2; exit 1; }
done
USER_URL="${USER_URL:-http://localhost:8081}"
PRODUCT_URL="${PRODUCT_URL:-http://localhost:8082}"
ORDER_URL="${ORDER_URL:-http://localhost:8083}"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
json() { python3 -c 'import json,sys; v=json.load(sys.stdin); print(v[sys.argv[1]])' "$1"; }
expect() {
  local expected="$1" method="$2" url="$3" data="${4:-}" code
  local args=(-sS --connect-timeout 5 --max-time 20 -o "$tmp/body" -w '%{http_code}' -X "$method")
  if [[ -n "$data" ]]; then args+=(-H 'Content-Type: application/json' --data "$data"); fi
  code="$(curl "${args[@]}" "$url")"
  printf '%s %s -> HTTP %s\n' "$method" "$url" "$code" >&2
  cat "$tmp/body" >&2; echo >&2
  [[ "$code" == "$expected" ]] || { echo "Expected HTTP $expected" >&2; exit 1; }
  cat "$tmp/body"
}
assert_field() {
  local actual
  actual="$(printf '%s' "$1" | json "$2")"
  [[ "$actual" == "$3" ]] || { echo "Expected $2=$3, got $actual" >&2; exit 1; }
}
count_orders() { expect 200 GET "$ORDER_URL/api/orders?buyerId=$buyer" | python3 -c 'import json,sys; print(len(json.load(sys.stdin)))'; }
echo '=== Health checks ==='
for url in "$USER_URL" "$PRODUCT_URL" "$ORDER_URL"; do
  ready=false
  for ((attempt=0; attempt<60; attempt++)); do
    if curl -fsS --max-time 3 "$url/actuator/health" > "$tmp/health"; then ready=true; break; fi
    sleep 2
  done
  [[ "$ready" == true ]] || { echo "Service not ready: $url"; exit 1; }
  assert_field "$(cat "$tmp/health")" status UP
  echo "$url/actuator/health -> HTTP 200 $(cat "$tmp/health")"
done
echo '=== Create users and product ==='
seller="$(expect 201 POST "$USER_URL/api/users" '{"displayName":"smoke-seller"}' | json id)"
buyer="$(expect 201 POST "$USER_URL/api/users" '{"displayName":"smoke-buyer"}' | json id)"
create_product() { expect 201 POST "$PRODUCT_URL/api/items" "{\"sellerId\":\"$seller\",\"title\":\"Smoke Book\",\"description\":\"week06\",\"price\":12.50}" | json id; }
item="$(create_product)"
echo '=== Reserve, duplicate rejection, cancel, reorder, complete ==='
order="$(expect 201 POST "$ORDER_URL/api/orders" "{\"itemId\":\"$item\",\"buyerId\":\"$buyer\"}")"
oid="$(printf '%s' "$order" | json id)"
assert_field "$order" status CREATED
assert_field "$order" itemId "$item"
assert_field "$order" buyerId "$buyer"
assert_field "$(expect 200 GET "$PRODUCT_URL/api/items/$item")" status RESERVED
expect 409 POST "$ORDER_URL/api/orders" "{\"itemId\":\"$item\",\"buyerId\":\"$buyer\"}" >/dev/null
[[ "$(count_orders)" == 1 ]]
assert_field "$(expect 200 POST "$ORDER_URL/api/orders/$oid/cancel")" status CANCELLED
assert_field "$(expect 200 GET "$PRODUCT_URL/api/items/$item")" status ON_SALE
oid="$(expect 201 POST "$ORDER_URL/api/orders" "{\"itemId\":\"$item\",\"buyerId\":\"$buyer\"}" | json id)"
assert_field "$(expect 200 POST "$ORDER_URL/api/orders/$oid/complete")" status COMPLETED
assert_field "$(expect 200 GET "$PRODUCT_URL/api/items/$item")" status SOLD
echo '=== Missing dependency resource: orders must remain unchanged ==='
before="$(count_orders)"
expect 404 POST "$ORDER_URL/api/orders" "{\"itemId\":\"missing-item\",\"buyerId\":\"$buyer\"}" >/dev/null
expect 404 POST "$ORDER_URL/api/orders" "{\"itemId\":\"$item\",\"buyerId\":\"missing-user\"}" >/dev/null
[[ "$(expect 200 GET "$ORDER_URL/api/orders?buyerId=missing-user" | python3 -c 'import json,sys; print(len(json.load(sys.stdin)))')" == 0 ]]
[[ "$(count_orders)" == "$before" ]]
echo "PASS: missing resources returned 404; order count remains $before"
echo '=== Self purchase: product must remain ON_SALE ==='
item2="$(create_product)"
expect 409 POST "$ORDER_URL/api/orders" "{\"itemId\":\"$item2\",\"buyerId\":\"$seller\"}" >/dev/null
assert_field "$(expect 200 GET "$PRODUCT_URL/api/items/$item2")" status ON_SALE
if [[ "${1:-}" == '--with-outage' ]]; then
  command -v docker >/dev/null
  restore() { docker compose start product-service >/dev/null; rm -rf "$tmp"; }
  trap restore EXIT
  docker compose stop product-service
  expect 503 POST "$ORDER_URL/api/orders" "{\"itemId\":\"$item2\",\"buyerId\":\"$buyer\"}" >/dev/null
  [[ "$(count_orders)" == "$before" ]]
  docker compose start product-service
  for ((attempt=0; attempt<60; attempt++)); do
    if curl -fsS --max-time 3 "$PRODUCT_URL/actuator/health" >/dev/null; then break; fi
    sleep 2
  done
  assert_field "$(expect 200 GET "$PRODUCT_URL/api/items/$item2")" status ON_SALE
  echo "PASS: dependency outage returned 503; order count remains $before; product remains ON_SALE"
fi
echo 'PASS: health, cross-service lifecycle, conflicts and missing resources'
