# 🔐 Authentication API Documentation

[← Back to Main README](../README.md)

---

## Overview

The Authentication module handles user registration, login, email verification, and password reset flows. It supports both traditional email/password authentication and Google OAuth.

**Base Path:** `/auth`

---

## Table of Contents

1. [Google OAuth Login](#1-google-oauth-login)
2. [Email & Password Login](#2-email--password-login)
3. [Registration](#3-registration-sign-up)
4. [Email Verification](#4-email-verification)
5. [Resend Verification Code](#5-resend-verification-code)
6. [Forgot Password](#6-forgot-password)
7. [Verify Reset Code](#7-verify-reset-code)
8. [Set New Password](#8-set-new-password)
9. [Check Authentication Status](#9-check-authentication-status)
10. [Logout](#10-logout)
11. [Check Username Availability](#11-check-username-availability)
12. [Check Email Availability](#12-check-email-availability)

---

## 1. Google OAuth Login

**Endpoint:** `POST /auth/google`

**Authentication:** None (public)

### Flow Diagram

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant G as Google OAuth
    participant DB as Database
    participant J as JWT Service
    participant E as Email Service

    C->>G: User clicks "Sign in with Google"
    G-->>C: Returns credential (ID token)
    C->>S: POST /auth/google {credential}
    S->>G: Verify ID token
    G-->>S: Token valid + user info
    S->>DB: Check if user exists by email
    alt User exists
        DB-->>S: User found
        S->>DB: Update profile picture if changed
    else New user
        S->>S: Generate unique username
        S->>S: Generate readable temp password (8 chars)
        S->>DB: Create new user
        DB-->>S: User created
        S->>E: Send credentials email
    end
    S->>J: Generate JWT token
    J-->>S: JWT token
    S-->>C: 200 OK + {token, user, expiresIn}
```

### Request

```json
POST /auth/google
Content-Type: application/json

{
  "credential": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}
```

### Response (Success)

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

### Response (Error)

```json
{
  "success": false,
  "error": "Google authentication failed"
}
```

---

## 2. Email & Password Login

**Endpoint:** `POST /auth/login`

**Authentication:** None (public)

### Flow Diagram

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
    alt User not found
        DB-->>S: Not found
        S-->>C: 404 Not Found
    else User found but not verified
        DB-->>S: User (enabled=false)
        S-->>C: 401 Unauthorized (User not verified)
    else User found and verified
        DB-->>S: User (enabled=true)
        S->>Auth: Authenticate(email, password)
        alt Invalid password
            Auth-->>S: Authentication failed
            S-->>C: 401 Unauthorized
        else Valid password
            Auth-->>S: Authentication successful
            S->>J: Generate JWT
            J-->>S: JWT token
            S->>Col: Get or create collection
            Col-->>S: Collection info
            S-->>C: 200 OK + {token, user, collection}
        end
    end
```

### Request

```json
POST /auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "mySecurePassword123"
}
```

### Response (Success)

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

### Response (Not Verified)

```json
{
  "success": false,
  "errorCode": "AUTHENTICATION_FAILED",
  "message": "חשבון לא מאומת. אנא אמת את האימייל שלך"
}
```

---

## 3. Registration (Sign Up)

**Endpoint:** `POST /auth/signup` or `POST /auth/register`

**Authentication:** None (public)

### Flow Diagram

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
        S->>DB: Delete old unverified user
        S->>S: Continue with registration
    else Email doesn't exist
        S->>S: Continue with registration
    end
    
    S->>DB: Check if username exists
    alt Username exists & verified
        DB-->>S: User found (enabled=true)
        S-->>C: 409 Conflict (Username exists)
    else Username exists & not verified
        S->>DB: Delete old unverified user
        S->>S: Continue with registration
    end
    
    S->>S: Generate 6-digit verification code
    S->>S: Hash password with BCrypt
    S->>S: Set verificationCodeExpiresAt = now + 15 minutes
    S->>DB: Create user (enabled=false)
    DB-->>S: User created
    S->>E: Send verification email
    E-->>S: Email sent
    S-->>C: 200 OK {message, email}
```

### Request

```json
POST /auth/signup
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "mySecurePassword123",
  "username": "john_doe",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Response (Success)

```json
{
  "success": true,
  "message": "קוד אימות נשלח למייל. המשתמש ייווצר רק לאחר אימות הקוד.",
  "email": "john@example.com"
}
```

### Response (Duplicate Email)

```json
{
  "success": false,
  "errorCode": "DUPLICATE_RESOURCE",
  "message": "משתמש עם דואר אלקטרוני 'john@example.com' כבר קיים במערכת"
}
```

---

## 4. Email Verification

**Endpoint:** `POST /auth/verify`

**Authentication:** None (public)

### Flow Diagram

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant J as JWT Service

    C->>S: POST /auth/verify {email, verificationCode}
    S->>DB: Find user by email
    alt User not found
        DB-->>S: Not found
        S-->>C: 404 Not Found
    else Already verified
        DB-->>S: User (enabled=true)
        S-->>C: 400 Bad Request (Already verified)
    else Not verified
        DB-->>S: User (enabled=false)
        S->>S: Check if code matches
        S->>S: Check if code not expired
        alt Code invalid
            S-->>C: 400 Bad Request (Invalid code)
        else Code expired
            S-->>C: 400 Bad Request (Code expired)
        else Code valid
            S->>DB: Set user.enabled = true
            S->>DB: Clear verificationCode & expiresAt
            DB-->>S: User updated
            S->>J: Generate JWT
            J-->>S: JWT token
            S-->>C: 200 OK + {token, user}
        end
    end
```

### Request

```json
POST /auth/verify
Content-Type: application/json

{
  "email": "john@example.com",
  "verificationCode": "123456"
}
```

### Response (Success)

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

### Response (Invalid Code)

```json
{
  "success": false,
  "errorCode": "VALIDATION_ERROR",
  "message": "קוד אימות שגוי"
}
```

---

## 5. Resend Verification Code

**Endpoint:** `POST /auth/resend?email={email}`

**Authentication:** None (public)

### Request

```http
POST /auth/resend?email=john@example.com
```

### Response

```json
{
  "success": true,
  "message": "קוד אימות נשלח מחדש"
}
```

---

## 6. Forgot Password

**Endpoint:** `POST /auth/forgot-password`

**Authentication:** None (public)

### Request

```json
POST /auth/forgot-password
Content-Type: application/json

{
  "email": "john@example.com"
}
```

### Response

```json
{
  "success": true,
  "message": "קוד איפוס סיסמה נשלח למייל שלך"
}
```

---

## 7. Verify Reset Code

**Endpoint:** `POST /auth/verify-reset-code`

**Authentication:** None (public)

### Request

```json
POST /auth/verify-reset-code
Content-Type: application/json

{
  "email": "john@example.com",
  "resetCode": "123456"
}
```

### Response (Valid)

```json
{
  "success": true,
  "message": "קוד האיפוס אומת בהצלחה",
  "verified": true
}
```

### Response (Invalid)

```json
{
  "success": false,
  "error": "קוד איפוס לא תקין או שפג תוקפו",
  "verified": false
}
```

---

## 8. Set New Password

**Endpoint:** `POST /auth/set-new-password`

**Authentication:** None (public)

### Request

```json
POST /auth/set-new-password
Content-Type: application/json

{
  "email": "john@example.com",
  "newPassword": "myNewSecurePassword456"
}
```

### Response

```json
{
  "success": true,
  "message": "הסיסמה שונתה בהצלחה! כעת תוכל להתחבר עם הסיסמה החדשה"
}
```

---

## 9. Check Authentication Status

**Endpoint:** `GET /auth/status`

**Authentication:** Optional (JWT token)

### Request

```http
GET /auth/status
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response (Authenticated)

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

### Response (Not Authenticated)

```json
{
  "success": true,
  "authenticated": false
}
```

---

## 10. Logout

**Endpoint:** `POST /auth/logout`

**Authentication:** Required (JWT token)

### Request

```http
POST /auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

**Note:** Client should delete the JWT token from local storage.

---

## 11. Check Username Availability

**Endpoint:** `GET /auth/check-username/{username}`

**Authentication:** None (public)

### Request

```http
GET /auth/check-username/john_doe
```

### Response

```json
{
  "available": false
}
```

---

## 12. Check Email Availability

**Endpoint:** `GET /auth/check-email/{email}`

**Authentication:** None (public)

### Request

```http
GET /auth/check-email/john@example.com
```

### Response

```json
{
  "available": false
}
```

---

## Common Error Responses

### 401 Unauthorized

```json
{
  "success": false,
  "errorCode": "AUTHENTICATION_FAILED",
  "message": "אימות נכשל. אנא בדוק את פרטי ההתחברות שלך",
  "timestamp": "2025-01-15T10:30:00"
}
```

### 409 Conflict

```json
{
  "success": false,
  "errorCode": "DUPLICATE_RESOURCE",
  "message": "משתמש עם דואר אלקטרוני 'john@example.com' כבר קיים במערכת",
  "timestamp": "2025-01-15T10:30:00"
}
```

### 400 Validation Error

```json
{
  "success": false,
  "errorCode": "VALIDATION_ERROR",
  "message": "שגיאות ולידציה בנתונים שהוזנו",
  "fieldErrors": {
    "email": "כתובת אימייל לא תקינה",
    "password": "סיסמה חייבת להכיל לפחות 6 תווים"
  },
  "timestamp": "2025-01-15T10:30:00"
}
```

---

[← Back to Main README](../README.md)
