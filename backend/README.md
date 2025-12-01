# Custom Site Chat - API Flow Documentation

## Table of Contents
1. [Authentication APIs](#authentication-apis)
2. [User APIs](#user-apis)
3. [Collection APIs](#collection-apis)
4. [Document APIs](#document-apis)
5. [Query APIs](#query-apis)

---

## Authentication APIs

### 1. Google OAuth Login
**Endpoint:** `POST /auth/google`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant G as Google OAuth
    participant DB as Database
    participant J as JWT Service

    C->>G: User clicks "Sign in with Google"
    G-->>C: Returns credential (ID token)
    C->>S: POST /auth/google {credential}
    S->>G: Verify ID token
    G-->>S: Token valid + user info
    S->>DB: Check if user exists by email
    alt User exists
        DB-->>S: User found
    else New user
        S->>DB: Create new user
        S->>S: Generate username & temp password
        S->>Email: Send credentials email
        DB-->>S: User created
    end
    S->>J: Generate JWT token
    J-->>S: JWT token
    S-->>C: 200 OK + {token, user, expiresIn}
```

**Request:**
```json
POST /auth/google
{
  "credential": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}
```

**Response:**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600000,
  "user": {
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "profilePictureUrl": "https://lh3.googleusercontent.com/..."
  }
}
```

---

### 2. Email & Password Login
**Endpoint:** `POST /auth/login`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant Auth as AuthManager
    participant DB as Database
    participant J as JWT Service
    participant Col as CollectionService

    C->>S: POST /auth/login {email, password}
    S->>DB: Find user by email
    DB-->>S: User found
    S->>S: Check if user.enabled == true
    alt Not verified
        S-->>C: 401 Unauthorized (User not verified)
    else Verified
        S->>Auth: Authenticate(email, password)
        Auth-->>S: Authentication successful
        S->>J: Generate JWT
        J-->>S: JWT token
        S->>Col: Get or create collection
        Col-->>S: Collection info
        S-->>C: 200 OK + {token, user, collection}
    end
```

**Request:**
```json
POST /auth/login
{
  "email": "john@example.com",
  "password": "mySecurePassword123"
}
```

**Response:**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600000,
  "user": {
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe"
  },
  "collection": {
    "hasCollection": true,
    "collectionName": "user_1_a7b3f2e1"
  }
}
```

---

### 3. Registration (Sign Up)
**Endpoint:** `POST /auth/signup` or `POST /auth/register`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant E as EmailService

    C->>S: POST /auth/signup {email, password, username, firstName, lastName}
    S->>DB: Check if email exists
    alt Email exists & verified
        DB-->>S: User found (enabled=true)
        S-->>C: 409 Conflict (Email already exists)
    else Email exists & not verified
        DB-->>S: User found (enabled=false)
        S->>DB: Delete old user
        S->>S: Continue registration
    else Email doesn't exist
        S->>S: Continue registration
    end
    S->>DB: Check if username exists
    alt Username exists & verified
        DB-->>S: User found (enabled=true)
        S-->>C: 409 Conflict (Username exists)
    else Username exists & not verified
        S->>DB: Delete old user
        S->>S: Continue registration
    end
    S->>S: Generate 6-digit verification code
    S->>S: Hash password with BCrypt
    S->>DB: Create user (enabled=false)
    DB-->>S: User created
    S->>E: Send verification email
    E-->>S: Email sent
    S-->>C: 200 OK {message, email}
```

**Request:**
```json
POST /auth/signup
{
  "email": "john@example.com",
  "password": "mySecurePassword123",
  "username": "john_doe",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:**
```json
{
  "success": true,
  "message": "קוד אימות נשלח למייל. המשתמש ייווצר רק לאחר אימות הקוד.",
  "email": "john@example.com"
}
```

---

### 4. Email Verification (By Code)
**Endpoint:** `POST /auth/verify`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant J as JWT Service

    C->>S: POST /auth/verify {email, verificationCode}
    S->>DB: Find user by email
    DB-->>S: User found
    alt Already verified
        S-->>C: 400 Bad Request (Already verified)
    else Not verified
        S->>S: Check if code matches & not expired
        alt Code invalid or expired
            S-->>C: 400 Bad Request (Invalid/Expired code)
        else Code valid
            S->>DB: Set user.enabled = true
            S->>DB: Clear verification code
            DB-->>S: User updated
            S->>J: Generate JWT
            J-->>S: JWT token
            S-->>C: 200 OK + {token, user}
        end
    end
```

**Request:**
```json
POST /auth/verify
{
  "email": "john@example.com",
  "verificationCode": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "החשבון אומת בהצלחה!",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600000,
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe"
  }
}
```

---

### 5. Resend Verification Code
**Endpoint:** `POST /auth/resend?email={email}`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant E as EmailService

    C->>S: POST /auth/resend?email=john@example.com
    S->>DB: Find user by email
    DB-->>S: User found
    alt Already verified
        S-->>C: 400 Bad Request (Already verified)
    else Not verified
        S->>S: Generate new 6-digit code
        S->>DB: Update verification code & expiry
        DB-->>S: User updated
        S->>E: Send new verification email
        E-->>S: Email sent
        S-->>C: 200 OK {message}
    end
```

**Request:**
```http
POST /auth/resend?email=john@example.com
```

**Response:**
```json
{
  "success": true,
  "message": "קוד אימות נשלח מחדש"
}
```

---

### 6. Forgot Password (Request Reset Code)
**Endpoint:** `POST /auth/forgot-password`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant E as EmailService

    C->>S: POST /auth/forgot-password {email}
    S->>DB: Find user by email
    alt User not found
        DB-->>S: User not found
        S-->>C: 404 Not Found
    else User found
        DB-->>S: User found
        S->>S: Generate 6-digit reset code
        S->>DB: Save resetPasswordCode & expiry (15 min)
        DB-->>S: Saved
        S->>E: Send password reset email
        E-->>S: Email sent
        S-->>C: 200 OK {message}
    end
```

**Request:**
```json
POST /auth/forgot-password
{
  "email": "john@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "קוד איפוס סיסמה נשלח למייל שלך"
}
```

---

### 7. Verify Reset Code
**Endpoint:** `POST /auth/verify-reset-code`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database

    C->>S: POST /auth/verify-reset-code {email, resetCode}
    S->>DB: Find user by email
    DB-->>S: User found
    S->>S: Check if resetCode matches & not expired
    alt Code valid
        S-->>C: 200 OK {verified: true}
    else Code invalid or expired
        S-->>C: 400 Bad Request {verified: false}
    end
```

**Request:**
```json
POST /auth/verify-reset-code
{
  "email": "john@example.com",
  "resetCode": "123456"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "קוד האיפוס אומת בהצלחה",
  "verified": true
}
```

**Response (Failure):**
```json
{
  "success": false,
  "error": "קוד איפוס לא תקין או שפג תוקפו",
  "verified": false
}
```

---

### 8. Set New Password
**Endpoint:** `POST /auth/set-new-password`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database

    C->>S: POST /auth/set-new-password {email, newPassword}
    S->>DB: Find user by email
    DB-->>S: User found
    alt No reset code found
        S-->>C: 400 Bad Request (No valid reset code)
    else Reset code exists
        S->>S: Hash new password with BCrypt
        S->>DB: Update password
        S->>DB: Clear reset code & temp password
        DB-->>S: Updated
        S-->>C: 200 OK {message}
    end
```

**Request:**
```json
POST /auth/set-new-password
{
  "email": "john@example.com",
  "newPassword": "myNewSecurePassword456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "הסיסמה שונתה בהצלחה! כעת תוכל להתחבר עם הסיסמה החדשה"
}
```

---

### 9. Check Authentication Status
**Endpoint:** `GET /auth/status`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant Sec as SecurityContext

    C->>S: GET /auth/status
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>Sec: Get authentication from context
    Sec-->>S: Authentication object
    alt Authenticated
        S->>S: Extract User from principal
        S-->>C: 200 OK {authenticated: true, user}
    else Not authenticated
        S-->>C: 200 OK {authenticated: false}
    end
```

**Request:**
```http
GET /auth/status
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (Authenticated):**
```json
{
  "success": true,
  "authenticated": true,
  "user": {
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "profilePictureUrl": ""
  }
}
```

**Response (Not Authenticated):**
```json
{
  "success": true,
  "authenticated": false
}
```

---

### 10. Logout
**Endpoint:** `POST /auth/logout`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant Sec as SecurityContext

    C->>S: POST /auth/logout
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>Sec: Clear security context
    Sec-->>S: Context cleared
    S-->>C: 200 OK {message}
    Note over C: Client should delete JWT token
```

**Request:**
```http
POST /auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

### 11. Check Username Availability
**Endpoint:** `GET /auth/check-username/{username}`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database

    C->>S: GET /auth/check-username/john_doe
    S->>DB: Find username (only enabled=true)
    alt Username exists (verified user)
        DB-->>S: User found
        S-->>C: 200 OK {available: false}
    else Username doesn't exist or not verified
        DB-->>S: Not found
        S-->>C: 200 OK {available: true}
    end
```

**Request:**
```http
GET /auth/check-username/john_doe
```

**Response:**
```json
{
  "available": false
}
```

---

### 12. Check Email Availability
**Endpoint:** `GET /auth/check-email/{email}`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database

    C->>S: GET /auth/check-email/john@example.com
    S->>DB: Find email (only enabled=true)
    alt Email exists (verified user)
        DB-->>S: User found
        S-->>C: 200 OK {available: false}
    else Email doesn't exist or not verified
        DB-->>S: Not found
        S-->>C: 200 OK {available: true}
    end
```

**Request:**
```http
GET /auth/check-email/john@example.com
```

**Response:**
```json
{
  "available": false
}
```

---

## User APIs

### 1. Get Current User Details
**Endpoint:** `GET /api/users/me`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant Sec as SecurityContext

    C->>S: GET /api/users/me
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>Sec: Extract user from security context
    Sec-->>S: User object
    S-->>C: 200 OK + User details
```

**Request:**
```http
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "profilePictureUrl": null,
  "authProvider": "LOCAL",
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-01-15T10:00:00",
  "collectionName": "user_1_a7b3f2e1",
  "collectionSecretKey": "sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "collectionCreatedAt": "2025-01-15T10:05:00",
  "enabled": true
}
```

---

### 2. Get All Users (Admin)
**Endpoint:** `GET /api/users/`

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

**Request:**
```http
GET /api/users/
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
[
  {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "enabled": true
  },
  {
    "id": 2,
    "username": "jane_smith",
    "email": "jane@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "enabled": true
  }
]
```

---

## Collection APIs

### 1. Get Collection Info
**Endpoint:** `GET /api/collection/info`

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
        S->>S: Generate collection name
        S->>S: Generate secret key
        S->>Q: Create collection in Qdrant
        Q-->>S: Collection created
        S->>S: Generate embed code
        S->>DB: Save collection details to user
        DB-->>S: Saved
        S-->>C: 200 OK + Collection details
    end
```

**Request:**
```http
GET /api/collection/info
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "success": true,
  "data": {
    "collectionName": "user_1_a7b3f2e1",
    "secretKey": "sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
    "embedCode": "<!-- Custom Site Chat Widget -->\n<script>\n  window.CHAT_WIDGET_SECRET_KEY = 'sk_a1b2c3d4...';\n  ...\n</script>",
    "createdAt": "2025-01-15T10:05:00",
    "documentCount": null,
    "totalSize": null
  }
}
```

---

### 2. Regenerate Secret Key
**Endpoint:** `POST /api/collection/regenerate-key`

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

**Request:**
```http
POST /api/collection/regenerate-key
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "success": true,
  "message": "מפתח חדש נוצר בהצלחה",
  "data": {
    "collectionName": "user_1_a7b3f2e1",
    "secretKey": "sk_NEW_KEY_x9y8z7w6v5u4t3s2r1q0p",
    "embedCode": "<!-- Custom Site Chat Widget -->\n<script>\n  window.CHAT_WIDGET_SECRET_KEY = 'sk_NEW_KEY_...';\n  ...\n</script>",
    "createdAt": "2025-01-15T10:05:00"
  }
}
```

---

### 3. Get Embed Code
**Endpoint:** `GET /api/collection/embed-code`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server

    C->>S: GET /api/collection/embed-code
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Extract current user
    S->>S: Get or create collection
    S-->>C: 200 OK + {embedCode}
```

**Request:**
```http
GET /api/collection/embed-code
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "success": true,
  "embedCode": "<!-- Custom Site Chat Widget -->\n<script>\n  window.CHAT_WIDGET_SECRET_KEY = 'sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6';\n  window.CHAT_WIDGET_API_URL = 'http://localhost:8080';\n  \n  // ⭐ התאמה אישית (אופציונלי)\n  window.CHAT_WIDGET_TITLE = 'ENTER TITEL OF THE CHAT LIKE: COMPENY NAME'; \n  window.CHAT_WIDGET_BOT_NAME = 'ENTER NAME OF THE BOT USER';\n  window.CHAT_WIDGET_BOT_AVATAR = 'ENTER BOT PROFILE IMAGE OR NULL'; \n  window.CHAT_WIDGET_USER_AVATAR = 'ENTER LINK OF USER PROFILE IMAGE OR NULL'; \n</script>\n<script src=\"http://localhost:3000/chat-widget.js\"></script>\n<!-- End Chat Widget -->"
}
```

---

## Document APIs

### 1. Upload Document
**Endpoint:** `POST /api/documents/upload`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant Async as Async Processor
    participant S3 as S3 Storage
    participant Q as Qdrant
    participant AI as OpenAI

    C->>S: POST /api/documents/upload (multipart/form-data)
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Read file to memory (byte[])
    S->>S: Calculate SHA-256 hash
    S->>S: Create Document entity (status=PENDING)
    S->>DB: Save document
    DB-->>S: Document saved (ID assigned)
    S-->>C: 200 OK + Document response (immediately)
    
    Note over Async: 🔄 Background Processing Starts
    
    Async->>DB: Load document by ID
    DB-->>Async: Document loaded
    
    Async->>Async: Update stage: UPLOADING (10%)
    Async->>DB: Save progress
    
    Async->>S3: Upload file to S3
    S3-->>Async: File uploaded
    Async->>Async: Update progress (20%)
    Async->>DB: Save progress
    
    Async->>Async: Update stage: EXTRACTING_TEXT (30%)
    Async->>DB: Save progress
    Async->>Async: Extract text with ApachePdfBox
    Async->>Async: Update progress (45%)
    Async->>DB: Save progress
    
    Async->>Async: Update stage: CREATING_CHUNKS (50%)
    Async->>DB: Save progress
    Async->>Async: Split text into chunks (500 chars, 50 overlap)
    Async->>Async: Update progress (60%)
    Async->>DB: Save progress
    
    Async->>Async: Update stage: CREATING_EMBEDDINGS (65%)
    Async->>DB: Save progress
    
    loop For each chunk
        Async->>AI: Generate embedding (text-embedding-3-large)
        AI-->>Async: Embedding vector (3072 dimensions)
        Async->>Async: Add metadata (document_id, user_id, chunk_index)
        Async->>Q: Store embedding in Qdrant
        Q-->>Async: Stored
        Async->>Async: Update progress (65-95%)
        Async->>DB: Save progress
    end
    
    Async->>Async: Mark as COMPLETED (100%)
    Async->>DB: Save final status
    DB-->>Async: Saved
```

**Request:**
```http
POST /api/documents/upload
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: multipart/form-data

file: [binary PDF data]
```

**Response (Immediate):**
```json
{
  "success": true,
  "message": "המסמך הועלה ומעובד ברקע",
  "document": {
    "id": 1,
    "userId": 1,
    "originalFileName": "contract.pdf",
    "fileType": "pdf",
    "fileSize": 2457600,
    "fileSizeFormatted": "2.34 MB",
    "filePath": "users/1/documents/1705315200000_contract.pdf",
    "processingStatus": "PENDING",
    "processingProgress": 5,
    "processingStage": "UPLOADING",
    "processingStageDescription": "מעלה לשרת...",
    "characterCount": null,
    "chunkCount": null,
    "active": true,
    "errorMessage": null,
    "createdAt": "2025-01-15T10:00:00",
    "processedAt": null,
    "statistics": null
  }
}
```

---

### 2. Get My Documents
**Endpoint:** `GET /api/documents/my-documents`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database

    C->>S: GET /api/documents/my-documents
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Extract current user
    S->>DB: Find documents WHERE user_id={id} AND active=true ORDER BY displayOrder
    DB-->>S: List of documents
    S->>S: Map to DocumentResponse DTOs
    S-->>C: 200 OK + List of documents
```

**Request:**
```http
GET /api/documents/my-documents
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 1,
      "originalFileName": "contract.pdf",
      "fileType": "pdf",
      "fileSize": 2457600,
      "fileSizeFormatted": "2.34 MB",
      "filePath": "users/1/documents/1705315200000_contract.pdf",
      "processingStatus": "COMPLETED",
      "processingProgress": 100,
      "processingStage": "COMPLETED",
      "processingStageDescription": "הושלם בהצלחה",
      "characterCount": 12500,
      "chunkCount": 25,
      "active": true,
      "errorMessage": null,
      "createdAt": "2025-01-15T10:00:00",
      "processedAt": "2025-01-15T10:02:30",
      "statistics": {
        "processingTimeMs": 150000,
        "processingTimeFormatted": "2 דקות",
        "embeddingsCount": 25,
        "estimatedCost": 0.0004
      }
    },
    {
      "id": 2,
      "userId": 1,
      "originalFileName": "report.pdf",
      "fileType": "pdf",
      "fileSize": 1048576,
      "fileSizeFormatted": "1.00 MB",
      "processingStatus": "PROCESSING",
      "processingProgress": 45,
      "processingStage": "EXTRACTING_TEXT",
      "processingStageDescription": "מחלץ טקסט מהמסמך... (45%)",
      "active": true,
      "createdAt": "2025-01-15T11:00:00"
    }
  ],
  "count": 2
}
```

---

### 3. Get Single Document
**Endpoint:** `GET /api/documents/{id}`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database

    C->>S: GET /api/documents/1
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Extract current user
    S->>DB: Find document WHERE id=1 AND active=true
    alt Document not found
        DB-->>S: Not found
        S-->>C: 404 Not Found
    else Document found
        DB-->>S: Document
        alt Not owner
            S-->>C: 403 Forbidden (Unauthorized)
        else Owner
            S->>S: Map to DocumentResponse
            S-->>C: 200 OK + Document details
        end
    end
```

**Request:**
```http
GET /api/documents/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "originalFileName": "contract.pdf",
    "fileType": "pdf",
    "fileSize": 2457600,
    "fileSizeFormatted": "2.34 MB",
    "filePath": "users/1/documents/1705315200000_contract.pdf",
    "processingStatus": "COMPLETED",
    "processingProgress": 100,
    "processingStage": "COMPLETED",
    "processingStageDescription": "הושלם בהצלחה",
    "characterCount": 12500,
    "chunkCount": 25,
    "active": true,
    "errorMessage": null,
    "createdAt": "2025-01-15T10:00:00",
    "processedAt": "2025-01-15T10:02:30",
    "statistics": {
      "processingTimeMs": 150000,
      "processingTimeFormatted": "2 דקות",
      "embeddingsCount": 25,
      "estimatedCost": 0.0004
    }
  }
}
```

---

### 4. Download Document
**Endpoint:** `GET /api/documents/{id}/download`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant S3 as S3 Storage

    C->>S: GET /api/documents/1/download
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Extract current user
    S->>DB: Find document WHERE id=1 AND active=true
    DB-->>S: Document
    alt Not owner
        S-->>C: 403 Forbidden
    else Owner
        S->>S3: Download file from S3
        S3-->>S: File stream
        S-->>C: 200 OK + Binary file (Content-Disposition: attachment)
    end
```

**Request:**
```http
GET /api/documents/1/download
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="contract.pdf"

[Binary PDF data]
```

---

### 5. View Document (Inline)
**Endpoint:** `GET /api/documents/{id}/view`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant S3 as S3 Storage

    C->>S: GET /api/documents/1/view
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Extract current user
    S->>DB: Find document
    DB-->>S: Document
    alt Not owner
        S-->>C: 403 Forbidden
    else Owner
        S->>S3: Download file from S3
        S3-->>S: File stream
        S-->>C: 200 OK + Binary file (Content-Disposition: inline)
    end
```

**Request:**
```http
GET /api/documents/1/view
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: inline; filename="contract.pdf"

[Binary PDF data - opens in browser]
```

---

### 6. Get Presigned Download URL
**Endpoint:** `GET /api/documents/{id}/download-url`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant S3 as AWS S3

    C->>S: GET /api/documents/1/download-url
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Extract current user
    S->>DB: Find document
    DB-->>S: Document
    alt Not owner
        S-->>C: 403 Forbidden
    else Owner
        S->>S3: Generate presigned URL (expires in 3600s)
        S3-->>S: Presigned URL
        S-->>C: 200 OK + {url, expiresIn}
    end
```

**Request:**
```http
GET /api/documents/1/download-url
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "success": true,
  "url": "https://s3.eu-west-1.amazonaws.com/bucket/users/1/documents/1705315200000_contract.pdf?X-Amz-Algorithm=...",
  "expiresIn": 3600
}
```

---

### 7. Delete Document
**Endpoint:** `DELETE /api/documents/{id}`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant Q as Qdrant
    participant S3 as S3 Storage

    C->>S: DELETE /api/documents/1
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Extract current user
    S->>DB: Find document WHERE id=1 AND active=true
    alt Not found or not owner
        S-->>C: 404 Not Found or 403 Forbidden
    else Owner
        DB-->>S: Document
        S->>Q: Delete embeddings WHERE document_id=1
        Q-->>S: Embeddings deleted
        S->>DB: Set document.active = false (soft delete)
        DB-->>S: Updated
        S->>S3: Delete file from S3
        S3-->>S: File deleted
        S-->>C: 200 OK {message}
    end
```

**Request:**
```http
DELETE /api/documents/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "success": true,
  "message": "מסמך נמחק בהצלחה"
}
```

---

### 8. Delete All Documents
**Endpoint:** `DELETE /api/documents/delete-all`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant Q as Qdrant
    participant S3 as S3 Storage

    C->>S: DELETE /api/documents/delete-all
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Extract current user
    S->>DB: Find all documents WHERE user_id={id} AND active=true
    DB-->>S: List of documents
    
    loop For each document
        S->>DB: Set document.active = false
        S->>S3: Delete file from S3
    end
    
    S->>Q: Delete entire collection
    Q-->>S: Collection deleted
    S->>Q: Create new empty collection
    Q-->>S: Collection created
    
    S-->>C: 200 OK {deletedCount}
```

**Request:**
```http
DELETE /api/documents/delete-all
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "success": true,
  "message": "כל המסמכים נמחקו בהצלחה",
  "deletedCount": 5
}
```

---

### 9. Reorder Documents
**Endpoint:** `PUT /api/documents/reorder`

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database

    C->>S: PUT /api/documents/reorder {documentIds: [3,1,2]}
    Note over C,S: Header: Authorization: Bearer {JWT}
    
    S->>S: Extract current user
    S->>S: Validate documentIds list
    
    loop For each documentId (index i)
        S->>DB: Find document WHERE id=documentId AND active=true
        alt Not found or not owner
            S-->>C: 404 Not Found or 403 Forbidden
        else Owner
            DB-->>S: Document
            S->>DB: Set document.displayOrder = i
            DB-->>S: Updated
        end
    end
    
    S-->>C: 200 OK {message}
```

**Request:**
```json
PUT /api/documents/reorder
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "documentIds": [3, 1, 2]
}
```

**Response:**
```json
{
  "success": true,
  "message": "סדר המסמכים עודכן בהצלחה"
}
```

---

## Query APIs

### 1. Ask Question (Public API)
**Endpoint:** `POST /api/query/ask`

```mermaid
sequenceDiagram
    participant Widget as Chat Widget
    participant S as Server
    participant DB as Database
    participant Q as Qdrant
    participant AI as OpenAI

    Widget->>S: POST /api/query/ask {secretKey, question, history}
    Note over Widget,S: No JWT required - public API
    
    S->>DB: Find user by collectionSecretKey
    alt Invalid secret key
        DB-->>S: User not found
        S-->>Widget: 401 Unauthorized
    else Valid secret key
        DB-->>S: User found
        
        S->>S: Validate & limit history (max 10 messages)
        S->>S: Build enhanced query from question + history context
        S->>S: Detect language (Hebrew/English)
        
        S->>AI: Generate embedding for enhanced query
        AI-->>S: Query embedding vector
        
        S->>Q: Search embeddings in user's collection (max 5 results, minScore 0.5)
        Q-->>S: Relevant chunks with scores
        
        alt No relevant documents found
            S-->>Widget: 200 OK {answer: "No info found", confidence: 0.0}
        else Documents found
            S->>S: Build chat messages (System + History + Context + Question)
            S->>AI: Generate answer with GPT-4
            AI-->>S: Answer text
            
            S->>S: Calculate confidence (avg relevance score)
            S->>S: Count tokens used
            S->>S: Build sources list
            
            S-->>Widget: 200 OK {answer, sources, confidence, tokensUsed, responseTimeMs}
        end
    end
```

**Request:**
```json
POST /api/query/ask

{
  "secretKey": "sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "question": "What are the payment terms?",
  "history": [
    {
      "role": "user",
      "content": "Tell me about this contract"
    },
    {
      "role": "assistant",
      "content": "This is a service agreement between Company A and Company B..."
    }
  ]
}
```

**Response (With Results):**
```json
{
  "success": true,
  "data": {
    "answer": "According to the contract, payment terms are Net 30 days from invoice date. Payments must be made via bank transfer to the account specified in Section 5.2.",
    "sources": [
      {
        "documentName": "contract.pdf",
        "excerpt": "Payment terms: Net 30 days from date of invoice. All payments shall be made by wire transfer to the account details provided in Section 5.2...",
        "relevanceScore": 0.89,
        "isPrimary": true
      },
      {
        "documentName": "contract.pdf",
        "excerpt": "Late payments will incur a penalty of 1.5% per month...",
        "relevanceScore": 0.72,
        "isPrimary": false
      }
    ],
    "responseTimeMs": 2340,
    "confidence": 0.805,
    "tokensUsed": 487
  }
}
```

**Response (No Results):**
```json
{
  "success": true,
  "data": {
    "answer": "Sorry, I couldn't find relevant information in the documents.",
    "sources": [],
    "responseTimeMs": 450,
    "confidence": 0.0,
    "tokensUsed": 0
  }
}
```

**Error Response (Invalid Secret Key):**
```json
{
  "success": false,
  "error": "Invalid secret key"
}
```

---

## Processing Stages Details

### Document Processing Progress

| Stage | Progress % | Description (Hebrew) | Description (English) |
|-------|-----------|---------------------|---------------------|
| UPLOADING | 10-20% | מעלה לשרת... | Uploading to server... |
| EXTRACTING_TEXT | 30-45% | מחלץ טקסט מהמסמך... | Extracting text from PDF... |
| CREATING_CHUNKS | 50-60% | מחלק לחלקים... | Splitting into chunks... |
| CREATING_EMBEDDINGS | 65-95% | יוצר embeddings... | Creating embeddings... |
| STORING | 65-95% | שומר במאגר... | Storing in database... |
| COMPLETED | 100% | הושלם בהצלחה | Completed successfully |
| FAILED | - | נכשל | Failed |

---

## Error Responses

All error responses follow this format:

```json
{
  "success": false,
  "errorCode": "ERROR_CODE_HERE",
  "message": "User-friendly error message",
  "details": "Technical details (optional)",
  "timestamp": "2025-01-15T10:30:00",
  "path": "/api/endpoint",
  "fieldErrors": {
    "fieldName": "Field-specific error message"
  }
}
```

### Common Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| AUTHENTICATION_FAILED | 401 | Invalid credentials or token |
| INVALID_TOKEN | 401 | JWT token expired or invalid |
| ACCESS_DENIED | 403 | User doesn't have permission |
| RESOURCE_NOT_FOUND | 404 | Document, user, or collection not found |
| DUPLICATE_RESOURCE | 409 | Email or username already exists |
| VALIDATION_ERROR | 400 | Input validation failed |
| FILE_TOO_LARGE | 400 | Uploaded file exceeds size limit |
| FILE_PROCESSING_ERROR | 500 | Error processing document |
| EXTERNAL_SERVICE_ERROR | 503 | S3, Qdrant, or OpenAI service error |
| INTERNAL_SERVER_ERROR | 500 | Unexpected server error |

---

## Rate Limits & Constraints

| Resource | Limit | Description |
|----------|-------|-------------|
| File Upload | 50 MB | Maximum PDF file size |
| Request Size | 100 MB | Maximum total request size |
| History Messages | 10 | Maximum chat history messages in query |
| Relevant Chunks | 5 | Maximum chunks returned from Qdrant |
| JWT Expiration | 1 hour | Token validity period |
| Verification Code | 15 minutes | Email verification code expiry |
| Reset Code | 15 minutes | Password reset code expiry |
| Presigned URL | 1 hour | S3 download link expiry |

---

## Technology Stack

- **Backend Framework:** Spring Boot 3.3.4
- **Database:** PostgreSQL
- **Vector Database:** Qdrant
- **Storage:** AWS S3 (or compatible)
- **AI:** OpenAI (text-embedding-3-large, GPT-4)
- **Authentication:** JWT with BCrypt
- **PDF Processing:** Apache PDFBox
- **Async Processing:** Spring @Async with ThreadPoolExecutor

---

## Environment Variables

Required environment variables (from `.env` file):

```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=customsitechat
POSTGRES_USER=your_user
POSTGRES_PASSWORD=your_password

# JWT
JWT_SECRET_KEY=your-super-secret-jwt-key-min-256-bits
JWT_EXPIRATION_MS=3600000

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password

# OpenAI
OPENAI_API_KEY=sk-your-openai-api-key-here

# AWS S3
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key
AWS_REGION=eu-west-1
AWS_S3_BUCKET=custom-site-chat

# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Frontend
FRONTEND_URL=http://localhost:3000

# Qdrant
QDRANT_HOST=localhost
QDRANT_GRPC_PORT=6334
```

---

## Notes

1. **Authentication:** All endpoints except `/auth/*` and `/api/query/ask` require JWT Bearer token
2. **Soft Deletes:** Documents are soft-deleted (active=false) rather than physically removed
3. **Async Processing:** Document upload returns immediately; processing happens in background
4. **Thread Safety:** Concurrent document uploads are handled by ThreadPoolExecutor
5. **Vector Search:** Uses cosine similarity with 3072-dimensional embeddings
6. **Language Detection:** Automatic Hebrew/English detection for contextual responses
7. **History Context:** Query API supports conversation history for contextual understanding
8. **CORS:** Configured to accept requests from any origin (`*`)

---

**Generated:** 2025-01-15  
**Version:** 1.0  
**Backend Port:** 8080  
**Frontend Port:** 3000