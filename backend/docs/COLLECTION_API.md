# 📦 Collection API Documentation

[← Back to Main README](../README.md)

---

## Overview

The Collection module manages user-specific Qdrant collections and provides embed code for the chat widget.

**Base Path:** `/api/collection`

**Authentication:** Required (JWT Bearer token)

---

## Table of Contents

1. [Get Collection Info](#1-get-collection-info)
2. [Regenerate Secret Key](#2-regenerate-secret-key)
3. [Get Embed Code](#3-get-embed-code)

---

## 1. Get Collection Info

**Endpoint:** `GET /api/collection/info`

**Authentication:** Required

Returns the user's collection details or creates a new collection if none exists.

### Flow Diagram

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant Q as QdrantService

    C->>S: GET /api/collection/info
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Extract current user
    alt User has collection
        S-->>C: 200 OK + Collection details
    else User doesn't have collection
        S->>S: Generate collection name (user_{id}_{uuid8})
        S->>S: Generate secret key (sk_{uuid32})
        S->>Q: Create collection in Qdrant
        Q-->>S: Collection created
        S->>S: Generate embed code
        S->>DB: Save collection details
        DB-->>S: Saved
        S-->>C: 200 OK + Collection details
    end
```

### Request

```http
GET /api/collection/info
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

```json
{
  "success": true,
  "data": {
    "collectionName": "user_1_a7b3f2e1",
    "secretKey": "sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
    "embedCode": "<!-- Custom Site Chat Widget -->\n<script>\n  window.CHAT_WIDGET_SECRET_KEY = 'sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6';\n  window.CHAT_WIDGET_API_URL = 'http://localhost:8080';\n  \n  // ⭐ התאמה אישית (אופציונלי)\n  window.CHAT_WIDGET_TITLE = 'ENTER TITEL OF THE CHAT LIKE: COMPENY NAME'; \n  window.CHAT_WIDGET_BOT_NAME = 'ENTER NAME OF THE BOT USER';\n  window.CHAT_WIDGET_BOT_AVATAR = 'ENTER BOT PROFILE IMAGE OR NULL'; \n  window.CHAT_WIDGET_USER_AVATAR = 'ENTER LINK OF USER PROFILE IMAGE OR NULL'; \n</script>\n<script src=\"http://localhost:3000/chat-widget.js\"></script>\n<!-- End Chat Widget -->",
    "createdAt": "2025-01-15T10:05:00",
    "documentCount": null,
    "totalSize": null
  }
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `collectionName` | String | Unique Qdrant collection name |
| `secretKey` | String | API key for Query endpoint |
| `embedCode` | String | JavaScript code to embed widget |
| `createdAt` | DateTime | Collection creation timestamp |
| `documentCount` | Integer | Number of documents (future) |
| `totalSize` | Long | Total storage size (future) |

---

## 2. Regenerate Secret Key

**Endpoint:** `POST /api/collection/regenerate-key`

**Authentication:** Required

Generates a new secret key and updates the embed code.

⚠️ **Warning:** This will invalidate the old secret key. Update your embedded widget immediately!

### Flow Diagram

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database

    C->>S: POST /api/collection/regenerate-key
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Extract current user
    S->>S: Generate new secret key (UUID)
    S->>S: Generate new embed code
    S->>DB: Update user.collectionSecretKey
    S->>DB: Update user.embedCode
    DB-->>S: Updated
    S-->>C: 200 OK + New collection details
```

### Request

```http
POST /api/collection/regenerate-key
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

```json
{
  "success": true,
  "message": "מפתח חדש נוצר בהצלחה",
  "data": {
    "collectionName": "user_1_a7b3f2e1",
    "secretKey": "sk_NEW_KEY_x9y8z7w6v5u4t3s2r1q0p",
    "embedCode": "<!-- Custom Site Chat Widget -->\n<script>\n  window.CHAT_WIDGET_SECRET_KEY = 'sk_NEW_KEY_x9y8z7w6v5u4t3s2r1q0p';\n  ...\n</script>",
    "createdAt": "2025-01-15T10:05:00"
  }
}
```

### When to Regenerate

✅ **Do regenerate when:**
- Secret key is compromised
- Rotating keys for security
- Testing different configurations

❌ **Don't regenerate:**
- Without updating embedded widget
- During active user sessions
- Frequently (causes downtime)

---

## 3. Get Embed Code

**Endpoint:** `GET /api/collection/embed-code`

**Authentication:** Required

Returns only the embed code snippet.

### Request

```http
GET /api/collection/embed-code
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

```json
{
  "success": true,
  "embedCode": "<!-- Custom Site Chat Widget -->\n<script>\n  window.CHAT_WIDGET_SECRET_KEY = 'sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6';\n  window.CHAT_WIDGET_API_URL = 'http://localhost:8080';\n  \n  // ⭐ התאמה אישית (אופציונלי)\n  window.CHAT_WIDGET_TITLE = 'ENTER TITEL OF THE CHAT LIKE: COMPENY NAME'; \n  window.CHAT_WIDGET_BOT_NAME = 'ENTER NAME OF THE BOT USER';\n  window.CHAT_WIDGET_BOT_AVATAR = 'ENTER BOT PROFILE IMAGE OR NULL'; \n  window.CHAT_WIDGET_USER_AVATAR = 'ENTER LINK OF USER PROFILE IMAGE OR NULL'; \n</script>\n<script src=\"http://localhost:3000/chat-widget.js\"></script>\n<!-- End Chat Widget -->"
}
```

---

## Embed Code Configuration

The generated embed code includes customization options:

```javascript
// Required
window.CHAT_WIDGET_SECRET_KEY = 'sk_...';  // Your secret key
window.CHAT_WIDGET_API_URL = 'http://localhost:8080';  // API endpoint

// Optional - Customize appearance
window.CHAT_WIDGET_TITLE = 'Customer Support';  // Widget header title
window.CHAT_WIDGET_BOT_NAME = 'Support Bot';    // Bot display name
window.CHAT_WIDGET_BOT_AVATAR = 'https://...';  // Bot avatar URL or null
window.CHAT_WIDGET_USER_AVATAR = 'https://...'; // User avatar URL or null
```

### Example Customized Embed

```html
<!-- Custom Site Chat Widget -->
<script>
  window.CHAT_WIDGET_SECRET_KEY = 'sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6';
  window.CHAT_WIDGET_API_URL = 'https://api.mycompany.com';
  
  // Customization
  window.CHAT_WIDGET_TITLE = 'Acme Corp Support';
  window.CHAT_WIDGET_BOT_NAME = 'Acme Assistant';
  window.CHAT_WIDGET_BOT_AVATAR = 'https://mycdn.com/bot-avatar.png';
  window.CHAT_WIDGET_USER_AVATAR = null;  // Use default
</script>
<script src="https://cdn.mycompany.com/chat-widget.js"></script>
<!-- End Chat Widget -->
```

---

## Collection Details

### Collection Naming

Format: `user_{userId}_{uuid8}`

Example: `user_1_a7b3f2e1`

### Secret Key Format

Format: `sk_{uuid32}`

Example: `sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6`

### Qdrant Configuration

Each collection is created with:
- **Vector Size:** 3072 dimensions (text-embedding-3-large)
- **Distance Metric:** Cosine similarity
- **HNSW Parameters:**
  - M: 16
  - EF Construct: 200
  - EF: 128
- **Indexing Threshold:** 10,000 vectors

---

## Common Errors

### 401 Unauthorized

```json
{
  "success": false,
  "errorCode": "INVALID_TOKEN",
  "message": "טוקן לא תקין או שפג תוקפו"
}
```

### 503 Service Unavailable

```json
{
  "success": false,
  "errorCode": "EXTERNAL_SERVICE_ERROR",
  "message": "שגיאה בשירות Vector Database: Connection timeout"
}
```

**Solution:** Check if Qdrant is running and accessible.

---

## Usage Examples

### JavaScript

```javascript
// Get collection info
async function getCollectionInfo(token) {
  const response = await fetch('http://localhost:8080/api/collection/info', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const data = await response.json();
  return data.data;
}

// Regenerate secret key
async function regenerateSecretKey(token) {
  const response = await fetch('http://localhost:8080/api/collection/regenerate-key', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const data = await response.json();
  return data.data;
}

// Usage
const collection = await getCollectionInfo('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...');
console.log('Secret Key:', collection.secretKey);
console.log('Embed Code:', collection.embedCode);

// Regenerate
const newCollection = await regenerateSecretKey('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...');
console.log('New Secret Key:', newCollection.secretKey);
```

### Python

```python
import requests

def get_collection_info(token):
    response = requests.get(
        'http://localhost:8080/api/collection/info',
        headers={'Authorization': f'Bearer {token}'}
    )
    response.raise_for_status()
    return response.json()['data']

def regenerate_secret_key(token):
    response = requests.post(
        'http://localhost:8080/api/collection/regenerate-key',
        headers={'Authorization': f'Bearer {token}'}
    )
    response.raise_for_status()
    return response.json()['data']

# Usage
collection = get_collection_info('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...')
print('Secret Key:', collection['secretKey'])

# Regenerate
new_collection = regenerate_secret_key('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...')
print('New Secret Key:', new_collection['secretKey'])
```

### cURL

```bash
# Get collection info
curl -X GET http://localhost:8080/api/collection/info \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Regenerate secret key
curl -X POST http://localhost:8080/api/collection/regenerate-key \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Get embed code only
curl -X GET http://localhost:8080/api/collection/embed-code \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Security Best Practices

1. **Protect Secret Key:**
   - Never commit to Git
   - Never expose in client-side logs
   - Store securely (environment variables)

2. **Key Rotation:**
   - Rotate keys periodically
   - Rotate immediately if compromised
   - Update all embedded widgets after rotation

3. **Access Control:**
   - Only authenticated users can access collection APIs
   - Each user has one collection
   - Collections are isolated per user

---

[← Back to Main README](../README.md)
