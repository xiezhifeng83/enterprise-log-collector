#!/bin/bash

# Keycloak Realm and Client Setup Script
# This script configures Keycloak for the log collection system

set -e

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASS="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM_NAME="log-monitor"
CLIENT_ID="log-collector-client"

echo "Setting up Keycloak realm and client..."
echo "Keycloak URL: $KEYCLOAK_URL"

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to be ready..."
until curl -sf "${KEYCLOAK_URL}/health" > /dev/null; do
    echo "  Waiting for Keycloak..."
    sleep 5
done
echo "Keycloak is ready!"

# Get admin access token
echo "Getting admin access token..."
TOKEN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASS}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli")

ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "Failed to get access token"
    exit 1
fi

echo "Access token obtained"

# Create realm
echo "Creating realm: $REALM_NAME..."
curl -s -X POST "${KEYCLOAK_URL}/admin/realms" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "'"${REALM_NAME}"'",
    "enabled": true,
    "displayName": "Log Monitor Realm"
  }' || echo "Realm may already exist"

# Create client
echo "Creating client: $CLIENT_ID..."
curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/clients" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "'"${CLIENT_ID}"'",
    "enabled": true,
    "publicClient": false,
    "serviceAccountsEnabled": true,
    "directAccessGrantsEnabled": true,
    "standardFlowEnabled": true,
    "redirectUris": ["http://localhost:8080/*"],
    "webOrigins": ["http://localhost:8080"],
    "protocol": "openid-connect"
  }' || echo "Client may already exist"

# Create roles
echo "Creating roles..."
for ROLE in ADMIN OPERATOR VIEWER; do
    curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "'"${ROLE}"'",
        "description": "'"${ROLE}"' role for log collector"
      }' || echo "Role $ROLE may already exist"
done

# Create test users
echo "Creating test users..."

# Admin user
curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testadmin",
    "enabled": true,
    "email": "admin@logcollector.com",
    "credentials": [{
      "type": "password",
      "value": "admin123",
      "temporary": false
    }],
    "realmRoles": ["ADMIN"]
  }' || echo "User testadmin may already exist"

# Operator user
curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testoperator",
    "enabled": true,
    "email": "operator@logcollector.com",
    "credentials": [{
      "type": "password",
      "value": "operator123",
      "temporary": false
    }],
    "realmRoles": ["OPERATOR"]
  }' || echo "User testoperator may already exist"

# Viewer user
curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testviewer",
    "enabled": true,
    "email": "viewer@logcollector.com",
    "credentials": [{
      "type": "password",
      "value": "viewer123",
      "temporary": false
    }],
    "realmRoles": ["VIEWER"]
  }' || echo "User testviewer may already exist"

echo ""
echo "✅ Keycloak setup complete!"
echo ""
echo "Realm: $REALM_NAME"
echo "Client ID: $CLIENT_ID"
echo ""
echo "Test Users:"
echo "  Admin: testadmin / admin123"
echo "  Operator: testoperator / operator123"
echo "  Viewer: testviewer / viewer123"
echo ""
echo "Access Keycloak admin console at: ${KEYCLOAK_URL}"
