#!/bin/bash
set -e

BASE="http://localhost:8080"
PASS=0
FAIL=0

check() {
    local desc="$1" code="$2" body="$3"
    if [ "$code" -ge 200 ] && [ "$code" -lt 300 ]; then
        echo "  [OK] $desc (HTTP $code)"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] $desc (HTTP $code) $body"
        FAIL=$((FAIL + 1))
    fi
}

echo "=== Taxi Service Integration Test ==="
echo ""

TS=$(date +%s)

# 1. Register passenger
echo "--- 1. Register passenger ---"
RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"p${TS}@t.com\",\"password\":\"123\",\"role\":\"PASSENGER\",\"name\":\"Ivan\",\"phone\":\"+79130000001\"}")
BODY=$(echo "$RESP" | sed '$d')
CODE=$(echo "$RESP" | tail -1)
check "Register passenger" "$CODE" "$BODY"
P_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "  Token: ${P_TOKEN:0:30}..."

# 2. Register driver
echo "--- 2. Register driver ---"
RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"d${TS}@t.com\",\"password\":\"123\",\"role\":\"DRIVER\",\"name\":\"Petr\",\"phone\":\"+79130000002\",\"licenseNumber\":\"LIC${TS}\"}")
BODY=$(echo "$RESP" | sed '$d')
CODE=$(echo "$RESP" | tail -1)
check "Register driver" "$CODE" "$BODY"
D_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "  Token: ${D_TOKEN:0:30}..."

if [ -z "$P_TOKEN" ] || [ -z "$D_TOKEN" ]; then
    echo "FATAL: Failed to get tokens"
    exit 1
fi

# 3. Create trip
echo "--- 3. Create trip ---"
RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/trips" \
  -H "Authorization: Bearer $P_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"origin":"55.0302,82.9204","destination":"55.0415,82.9346"}')
BODY=$(echo "$RESP" | sed '$d')
CODE=$(echo "$RESP" | tail -1)
check "Create trip" "$CODE" "$BODY"
TRIP_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "  Trip ID: $TRIP_ID"

if [ -z "$TRIP_ID" ]; then
    echo "FATAL: Failed to create trip, aborting further tests"
    echo "  Response: $BODY"
    exit 1
fi

# 4. Driver accepts trip
echo "--- 4. Driver accepts trip (IN_PROGRESS) ---"
RESP=$(curl -s -w '\n%{http_code}' -X PATCH "$BASE/trips/$TRIP_ID/status" \
  -H "Authorization: Bearer $D_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}')
BODY=$(echo "$RESP" | sed '$d')
CODE=$(echo "$RESP" | tail -1)
check "Accept trip" "$CODE" "$BODY"

# 5. Driver completes trip
echo "--- 5. Driver completes trip (COMPLETED) ---"
RESP=$(curl -s -w '\n%{http_code}' -X PATCH "$BASE/trips/$TRIP_ID/status" \
  -H "Authorization: Bearer $D_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"status":"COMPLETED"}')
BODY=$(echo "$RESP" | sed '$d')
CODE=$(echo "$RESP" | tail -1)
check "Complete trip" "$CODE" "$BODY"

# 6. Passenger rates trip
echo "--- 6. Passenger rates trip ---"
RESP=$(curl -s -w '\n%{http_code}' -X POST "$BASE/trips/$TRIP_ID/rate" \
  -H "Authorization: Bearer $P_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"rating":5}')
BODY=$(echo "$RESP" | sed '$d')
CODE=$(echo "$RESP" | tail -1)
check "Rate trip" "$CODE" "$BODY"

# 7. Trip history
echo "--- 7. Trip history ---"
RESP=$(curl -s -w '\n%{http_code}' "$BASE/trips" \
  -H "Authorization: Bearer $P_TOKEN")
BODY=$(echo "$RESP" | sed '$d')
CODE=$(echo "$RESP" | tail -1)
check "Trip history" "$CODE" "$BODY"

# 8. Driver stats
echo "--- 8. Driver stats ---"
RESP=$(curl -s -w '\n%{http_code}' "$BASE/trips/stats?date=2026-05-11" \
  -H "Authorization: Bearer $D_TOKEN")
BODY=$(echo "$RESP" | sed '$d')
CODE=$(echo "$RESP" | tail -1)
check "Driver stats" "$CODE" "$BODY"

# 9. Notifications
echo "--- 9. Notifications ---"
RESP=$(curl -s -w '\n%{http_code}' "$BASE/notifications" \
  -H "Authorization: Bearer $D_TOKEN")
BODY=$(echo "$RESP" | sed '$d')
CODE=$(echo "$RESP" | tail -1)
check "Notifications" "$CODE" "$BODY"

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
