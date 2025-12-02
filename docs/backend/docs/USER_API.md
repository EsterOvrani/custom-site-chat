# 👤 User API Documentation

[← Back to Main README](../README.md)

---

## Overview

The User module provides endpoints for retrieving user information.

**Base Path:** `/api/users`

**Authentication:** Required (JWT Bearer token)

---

## Table of Contents

1. [Get Current User](#1-get-current-user)
2. [Get All Users (Admin)](#2-get-all-users-admin)

---

## 1. Get Current User

**Endpoint:** `GET /api/users/me`

**Authentication:** Required

Returns the authenticated user's complete profile.

### Flow Diagram

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant Sec as SecurityContext

    C->>S: GET /api/users/me
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>Sec: Extract user from context
    Sec-->>S: User object
    S-->>C: 200 OK + User details
```

### Request

```http
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "profilePictureUrl": "https://lh3.googleusercontent.com/...",
  "authProvider": "GOOGLE",
  "googleId": "103547991597142817347",
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-01-15T10:00:00",
  "collectionName": "user_1_a7b3f2e1",
  "collectionSecretKey": "sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "collectionCreatedAt": "2025-01-15T10:05:00",
  "embedCode": "<!-- Custom Site Chat Widget -->\n<script>...",
  "enabled": true
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | User's unique ID |
| `username` | String | Unique username |
| `email` | String | User's email address |
| `firstName` | String | First name |
| `lastName` | String | Last name |
| `profilePictureUrl` | String | Profile picture URL (Google users) |
| `authProvider` | Enum | `LOCAL` or `GOOGLE` |
| `googleId` | String | Google account ID (if OAuth) |
| `createdAt` | DateTime | Account creation timestamp |
| `updatedAt` | DateTime | Last update timestamp |
| `collectionName` | String | Qdrant collection name |
| `collectionSecretKey` | String | Secret key for Query API |
| `collectionCreatedAt` | DateTime | Collection creation timestamp |
| `embedCode` | String | Widget embed code |
| `enabled` | Boolean | Account verification status |

---

## 2. Get All Users (Admin)

**Endpoint:** `GET /api/users/`

**Authentication:** Required (Admin role)

**Note:** Currently returns all users without role checking. In production, this should be restricted to admin users only.

### Flow Diagram

```mermaid
sequenceDiagram
    participant C as Client (Admin)
    participant S as Server
    participant DB as Database

    C->>S: GET /api/users/
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>DB: SELECT * FROM users
    DB-->>S: All users
    S-->>C: 200 OK + List of users
```

### Request

```http
GET /api/users/
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

```json
[
  {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "profilePictureUrl": null,
    "authProvider": "LOCAL",
    "createdAt": "2025-01-15T10:00:00",
    "enabled": true
  },
  {
    "id": 2,
    "username": "jane_smith",
    "email": "jane@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "profilePictureUrl": "https://lh3.googleusercontent.com/...",
    "authProvider": "GOOGLE",
    "googleId": "108234567890123456789",
    "createdAt": "2025-01-16T09:30:00",
    "enabled": true
  },
  {
    "id": 3,
    "username": "bob_wilson",
    "email": "bob@example.com",
    "firstName": "Bob",
    "lastName": "Wilson",
    "authProvider": "LOCAL",
    "createdAt": "2025-01-16T11:15:00",
    "enabled": false
  }
]
```

---

## Common Errors

### 401 Unauthorized

```json
{
  "success": false,
  "errorCode": "INVALID_TOKEN",
  "message": "טוקן לא תקין או שפג תוקפו",
  "timestamp": "2025-01-15T10:30:00"
}
```

**Solution:** Ensure the JWT token is valid and not expired.

---

## Usage Examples

### JavaScript (Fetch API)

```javascript
// Get current user
async function getCurrentUser(token) {
  const response = await fetch('http://localhost:8080/api/users/me', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (!response.ok) {
    throw new Error('Failed to fetch user');
  }
  
  return await response.json();
}

// Usage
const user = await getCurrentUser('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...');
console.log(user.username);
console.log(user.email);
```

### Python (Requests)

```python
import requests

def get_current_user(token):
    response = requests.get(
        'http://localhost:8080/api/users/me',
        headers={'Authorization': f'Bearer {token}'}
    )
    response.raise_for_status()
    return response.json()

# Usage
user = get_current_user('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...')
print(user['username'])
print(user['email'])
```

### cURL

```bash
# Get current user
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Get all users (admin)
curl -X GET http://localhost:8080/api/users/ \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Notes

### Security Considerations

1. **Sensitive Data:** The `/me` endpoint returns sensitive information including:
   - `collectionSecretKey` - Keep this private!
   - `embedCode` - Contains the secret key

2. **Admin Endpoint:** The `/api/users/` endpoint should be restricted to admin users in production.

3. **Password Field:** The password field is never returned in API responses (it's hashed in the database).

### Profile Pictures

- Google OAuth users automatically get their Google profile picture
- Local users don't have profile pictures by default
- The `profilePictureUrl` field will be `null` for local users

### Account Status

The `enabled` field indicates if the user has verified their email:
- `true` - Account is verified and active
- `false` - Account pending email verification

---

[← Back to Main README](../README.md)
