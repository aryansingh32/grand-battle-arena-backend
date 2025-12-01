#!/bin/bash

# 1. Create a test user
echo "Creating test user..."
USER_RESP=$(curl -s -X POST http://localhost:10000/api/users/me \
  -H "Content-Type: application/json" \
  -d '{"firebaseUserUID":"test_user_history_1","email":"history_test@example.com","userName":"HistoryTester"}')
echo "User Response: $USER_RESP"

# 2. Create a test tournament
echo "Creating test tournament..."
TOURN_RESP=$(curl -s -X POST http://localhost:10000/api/tournaments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test_admin_token" \
  -d '{
    "name": "History Test Tournament",
    "game": "PUBG",
    "map": "Erangel",
    "teamSize": "SOLO",
    "maxPlayers": 100,
    "entryFee": 10,
    "prizePool": 1000,
    "startTime": "2025-12-31T20:00:00"
  }')
echo "Tournament Response: $TOURN_RESP"
TOURN_ID=$(echo $TOURN_RESP | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "Tournament ID: $TOURN_ID"

if [ -z "$TOURN_ID" ]; then
  echo "Failed to create tournament. Exiting."
  exit 1
fi

# 3. Book a slot for the user (to link UID to Player Name)
echo "Booking slot..."
curl -s -X POST "http://localhost:10000/api/tournaments/$TOURN_ID/slots/1/book" \
  -H "Content-Type: application/json" \
  -d '{"firebaseUserUID":"test_user_history_1","playerName":"HistoryTester"}'

# 4. Update Scoreboard
echo "Updating scoreboard..."
curl -s -X PUT "http://localhost:10000/api/tournaments/$TOURN_ID/scoreboard" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test_admin_token" \
  -d '{
    "scoreboard": [
      {
        "playerName": "HistoryTester",
        "teamName": "Solo",
        "kills": 5,
        "placement": 1,
        "coinsEarned": 500,
        "firebaseUserUID": "test_user_history_1"
      }
    ]
  }'

# 5. Check User History
echo "Checking user history..."
curl -s -X GET "http://localhost:10000/api/users/test_user_history_1/history" \
  -H "Content-Type: application/json"
