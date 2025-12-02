# Collection Settings Flow

> Manage collection settings, secret keys, and embed code

---

## 📑 Table of Contents

1. [Overview](#overview)
2. [Load Settings](#load-settings)
3. [Collection Name Format](#collection-name-format)
4. [Secret Key](#secret-key)
5. [Regenerate Secret Key](#regenerate-secret-key)
6. [Copy Secret Key](#copy-secret-key)
7. [Embed Code](#embed-code)
8. [Copy Embed Code](#copy-embed-code)
9. [UI Layout](#ui-layout)
10. [Security Considerations](#security-considerations)

---

## Overview

The Settings page allows users to:
- View collection name (auto-generated)
- View and copy secret API key
- Regenerate secret key (with warning)
- View and copy embed code
- Integrate chat widget on their website

**Key Concepts:**
- **Collection Name:** Unique identifier for user's document collection
- **Secret Key:** API authentication for chat widget
- **Embed Code:** HTML snippet to add widget to website

---

## Load Settings

### Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant SettingsPage
    participant API
    participant Database

    User->>SettingsPage: Navigate to /settings
    SettingsPage->>API: GET /collections/settings
    
    alt Settings exist
        API->>Database: Query settings
        Database-->>API: Return settings
        API-->>SettingsPage: 200 OK + settings
        SettingsPage->>User: Display settings
    else Settings not found (first time)
        API->>Database: Create default settings
        Database-->>API: New settings created
        API-->>SettingsPage: 201 Created + settings
        SettingsPage->>User: Display settings
    end
```

### Auto-Create Logic

**Condition:** If user has no collection settings  
**Action:** Backend automatically creates:
- Collection name: `user_{userId}_{uuid8}`
- Secret key: `sk_{uuid32}`
- Creation timestamp

### Request/Response

**Request:**
```http
GET /collections/settings
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "collection_name": "user_123_a1b2c3d4",
  "secret_key": "sk_7f3e9d2c8b4a1e6f5d9c2b8a4e7f3d1c",
  "created_at": "2024-12-01T10:00:00Z",
  "updated_at": "2024-12-01T10:00:00Z"
}
```

---

## Collection Name Format

### Structure

```
user_{userId}_{uuid8}
```

**Example:**
```
user_123_a1b2c3d4
```

**Components:**
- `user_`: Prefix for all user collections
- `{userId}`: User's database ID (e.g., `123`)
- `{uuid8}`: Random 8-character UUID for uniqueness

### Why This Format?

| Reason | Explanation |
|--------|-------------|
| **Uniqueness** | UUID ensures no collisions |
| **Readability** | User ID makes it traceable |
| **Security** | Not predictable or enumerable |
| **Consistency** | Standard naming convention |

---

## Secret Key

### Format

```
sk_{uuid32}
```

**Example:**
```
sk_7f3e9d2c8b4a1e6f5d9c2b8a4e7f3d1c
```

**Components:**
- `sk_`: Prefix for "Secret Key"
- `{uuid32}`: Random 32-character UUID

### Purpose

The secret key is used to:
- Authenticate chat widget requests
- Link widget queries to user's collection
- Track API usage per user

**Security Note:** While called "secret," it's safe to expose in frontend code since it only grants access to the Query API (read-only).

---

## Regenerate Secret Key

### Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant UI
    participant ConfirmDialog
    participant API
    participant Database

    User->>UI: Click "Regenerate Key"
    UI->>ConfirmDialog: Show warning dialog
    
    alt User confirms
        User->>ConfirmDialog: Click "Yes, Regenerate"
        ConfirmDialog->>API: POST /collections/regenerate-key
        API->>Database: Generate new key
        API->>Database: Update settings
        Database-->>API: Success
        API-->>UI: 200 OK + new key
        UI->>UI: Update displayed key
        UI->>User: Show success message
        Note over UI: Old key is now invalid!
    else User cancels
        User->>ConfirmDialog: Click "Cancel"
        ConfirmDialog->>User: Close dialog
    end
```

### Warning Dialog

```
┌─────────────────────────────────────────┐
│  ⚠️  Regenerate Secret Key?              │
│                                         │
│  This will generate a new secret key    │
│  and invalidate the old one.            │
│                                         │
│  ⚠️ Your embedded chat widgets will     │
│     stop working until you update       │
│     them with the new key!              │
│                                         │
│  Are you sure you want to continue?     │
│                                         │
│  [Cancel]  [Yes, Regenerate]            │
└─────────────────────────────────────────┘
```

### Impact Diagram

```mermaid
flowchart TD
    A[User clicks<br/>Regenerate Key] --> B[Confirm dialog]
    B -->|Cancel| C[No change]
    B -->|Confirm| D[Generate new key]
    
    D --> E[Old key invalidated]
    E --> F[Existing widgets<br/>stop working ❌]
    
    D --> G[New key active]
    G --> H[User must update<br/>embed code ✅]
    
    style E fill:#dc3545,color:#fff
    style G fill:#28a745,color:#fff
```

### When to Regenerate?

| Scenario | Should Regenerate? |
|----------|-------------------|
| Key compromised | ✅ Yes, immediately |
| Suspicious API usage | ✅ Yes, investigate |
| Rotating credentials | ✅ Yes, best practice |
| Just curious | ❌ No, don't break widgets |
| Testing | ✅ Yes, in dev environment |

---

## Copy Secret Key

### Mechanism

```mermaid
sequenceDiagram
    participant User
    participant Button
    participant Clipboard
    participant UI

    User->>Button: Click "Copy Key"
    Button->>Clipboard: navigator.clipboard.writeText(key)
    
    alt Copy success
        Clipboard-->>Button: Success
        Button->>UI: Change icon to ✓
        Button->>UI: Show "Copied!" tooltip
        Button->>Button: Wait 2 seconds
        Button->>UI: Reset icon to 📋
    else Copy failed
        Clipboard-->>Button: Error
        Button->>UI: Show error message
    end
```

### UI States

```
State 1: Default
┌──────────────────────┐
│ Secret Key           │
│ sk_7f3e9d2c8b4a...   │
│ [📋 Copy]            │
└──────────────────────┘

State 2: Copied (2 seconds)
┌──────────────────────┐
│ Secret Key           │
│ sk_7f3e9d2c8b4a...   │
│ [✓ Copied!]          │
└──────────────────────┘

State 3: Back to Default
┌──────────────────────┐
│ Secret Key           │
│ sk_7f3e9d2c8b4a...   │
│ [📋 Copy]            │
└──────────────────────┘
```

### Clipboard API Code

```javascript
const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  } catch (err) {
    console.error('Failed to copy:', err);
    alert('Copy failed. Please copy manually.');
  }
};
```

---

## Embed Code

### Structure

```html
<script>
(function() {
  var script = document.createElement('script');
  script.src = 'https://custom-site-chat.com/chat-widget.js';
  script.async = true;
  script.onload = function() {
    window.ChatWidget.init({
      secretKey: 'sk_7f3e9d2c8b4a1e6f5d9c2b8a4e7f3d1c',
      apiUrl: 'https://api.custom-site-chat.com',
      title: 'Chat with us',
      botName: 'Assistant',
      userAvatar: '👤',
      botAvatar: '🤖'
    });
  };
  document.body.appendChild(script);
})();
</script>
```

### Customization Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `secretKey` | String | **Required** | Your secret API key |
| `apiUrl` | String | Production URL | API endpoint |
| `title` | String | `"Chat with us"` | Widget title |
| `botName` | String | `"Assistant"` | Bot display name |
| `userAvatar` | String | `"👤"` | User message icon |
| `botAvatar` | String | `"🤖"` | Bot message icon |
| `maxHistory` | Number | `10` | Max stored messages |

### Example: Custom Branding

```javascript
window.ChatWidget.init({
  secretKey: 'sk_xxx',
  apiUrl: 'https://api.custom-site-chat.com',
  title: 'Help Center',
  botName: 'Support Bot',
  userAvatar: '😊',
  botAvatar: '🎯'
});
```

---

## Copy Embed Code

### Flow

```mermaid
sequenceDiagram
    participant User
    participant Button
    participant Clipboard
    participant UI

    User->>Button: Click "Copy Embed Code"
    Button->>Clipboard: navigator.clipboard.writeText(embedCode)
    
    alt Copy success
        Clipboard-->>Button: Success
        Button->>UI: Change button text
        Button->>UI: Show "Copied!" message
        Button->>Button: Wait 2 seconds
        Button->>UI: Reset button text
    else Copy failed
        Clipboard-->>Button: Error
        Button->>UI: Show error message
    end
```

### State Management

```javascript
const [copiedEmbed, setCopiedEmbed] = useState(false);

const copyEmbedCode = async () => {
  try {
    await navigator.clipboard.writeText(embedCode);
    setCopiedEmbed(true);
    setTimeout(() => setCopiedEmbed(false), 2000);
  } catch (err) {
    console.error('Copy failed:', err);
  }
};
```

---

## UI Layout

### Desktop View

```
┌──────────────────────────────────────────────┐
│  ⚙️ Collection Settings                      │
├──────────────────────────────────────────────┤
│                                              │
│  Collection Name                             │
│  ┌──────────────────────────────────────┐   │
│  │ user_123_a1b2c3d4                    │   │
│  └──────────────────────────────────────┘   │
│                                              │
│  Secret Key                                  │
│  ┌──────────────────────────────────────┐   │
│  │ sk_7f3e9d2c8b4a1e6f5d9c2b8a4e7f3d1c │   │
│  └──────────────────────────────────────┘   │
│  [📋 Copy]  [🔄 Regenerate]                  │
│                                              │
│  Embed Code                                  │
│  ┌──────────────────────────────────────┐   │
│  │ <script>                             │   │
│  │ (function() {                        │   │
│  │   var script = ...                   │   │
│  │   ...                                │   │
│  │ })();                                │   │
│  │ </script>                            │   │
│  └──────────────────────────────────────┘   │
│  [📋 Copy Embed Code]                        │
│                                              │
│  ℹ️ Paste this code before </body> tag      │
│     in your HTML to enable the chat widget. │
│                                              │
└──────────────────────────────────────────────┘
```

### Mobile View

```
┌─────────────────────────┐
│  ⚙️ Settings             │
├─────────────────────────┤
│                         │
│  Collection Name        │
│  ┌───────────────────┐  │
│  │ user_123_...      │  │
│  └───────────────────┘  │
│                         │
│  Secret Key             │
│  ┌───────────────────┐  │
│  │ sk_7f3e9d...      │  │
│  └───────────────────┘  │
│  [Copy] [Regenerate]    │
│                         │
│  Embed Code             │
│  ┌───────────────────┐  │
│  │ <script>          │  │
│  │ ...               │  │
│  └───────────────────┘  │
│  [Copy Code]            │
│                         │
└─────────────────────────┘
```

---

## Security Considerations

### Secret Key Visibility

```mermaid
flowchart TD
    A[Secret Key] --> B{Where is it used?}
    
    B -->|Frontend settings| C[✅ Safe to show]
    B -->|Embed code| D[✅ Safe to expose]
    B -->|Public website| E[✅ Safe in HTML]
    
    C --> F[Reason: Query API only]
    D --> F
    E --> F
    
    F --> G[No admin operations<br/>No document upload<br/>No user data access]
    
    style C fill:#28a745,color:#fff
    style D fill:#28a745,color:#fff
    style E fill:#28a745,color:#fff
```

### What Secret Key CAN Do

✅ Query user's documents via chat  
✅ Get AI-generated responses  
✅ Access query history (limited)

### What Secret Key CANNOT Do

❌ Upload/delete documents  
❌ Access admin panel  
❌ Modify collection settings  
❌ View other users' data  
❌ Generate new API keys

### API Rate Limiting

| Limit Type | Value | Action |
|------------|-------|--------|
| **Requests/minute** | 60 | Throttle |
| **Requests/day** | 10,000 | Warn user |
| **Concurrent requests** | 5 | Queue |

---

## Error Handling

### Load Settings Error

```mermaid
flowchart TD
    A[Load Settings] --> B{API Response}
    B -->|200 OK| C[✅ Display settings]
    B -->|401 Unauthorized| D[❌ Redirect to login]
    B -->|500 Server Error| E[❌ Show error message]
    
    E --> F[Retry button]
    F --> A
```

### Regenerate Key Error

```mermaid
flowchart TD
    A[Regenerate Key] --> B{API Response}
    B -->|200 OK| C[✅ Update key display]
    B -->|401 Unauthorized| D[❌ Session expired]
    B -->|500 Server Error| E[❌ Show error toast]
    
    D --> F[Redirect to login]
    E --> G[Retry button]
```

---

## Summary

✅ **Auto-create:** Settings created on first visit  
✅ **Collection Name:** `user_{userId}_{uuid8}`  
✅ **Secret Key:** `sk_{uuid32}` for widget auth  
✅ **Regenerate:** New key invalidates old one  
✅ **Embed Code:** Ready-to-use HTML snippet  
✅ **Copy Functions:** One-click clipboard copy  
✅ **Security:** Secret key safe for public exposure

---

[← Back to Main README](../README.md)
