# Authentication Flow

> Complete authentication system with email/password and Google OAuth integration

---

## 📑 Table of Contents

1. [Overview](#overview)
2. [Email/Password Login](#emailpassword-login)
3. [Google OAuth Login](#google-oauth-login)
4. [Email/Password Registration](#emailpassword-registration)
5. [Google OAuth Registration](#google-oauth-registration)
6. [Email Verification](#email-verification)
7. [Forgot Password](#forgot-password)
8. [Reset Password](#reset-password)
9. [Component Relationships](#component-relationships)
10. [JWT Token Management](#jwt-token-management)
11. [Error Handling](#error-handling)

---

## Overview

The authentication system supports two methods:
- **Email/Password**: Traditional registration with email verification
- **Google OAuth**: One-click sign-up with auto-verification

**Key Features:**
- JWT-based authentication
- 6-digit verification codes (15-minute expiry)
- Auto-logout on token expiration
- Axios interceptors for token injection
- LocalStorage for session persistence

---

## Email/Password Login

### Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant LoginPage
    participant API
    participant LocalStorage
    participant Dashboard

    User->>LoginPage: Enter email & password
    LoginPage->>API: POST /auth/login
    
    alt Success
        API-->>LoginPage: 200 OK + JWT token
        LoginPage->>LocalStorage: Store token
        LoginPage->>Dashboard: Redirect to /dashboard
    else Error (Invalid credentials)
        API-->>LoginPage: 401 Unauthorized
        LoginPage-->>User: Show error message
    else Error (Email not verified)
        API-->>LoginPage: 403 Forbidden
        LoginPage-->>User: "Please verify your email"
    end
```

### Request/Response

**Request:**
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response (Success):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "123",
    "email": "user@example.com",
    "name": "John Doe"
  }
}
```

**Response (Error):**
```json
{
  "error": "Invalid email or password"
}
```

### UI States

```
┌─────────────────────────┐
│   Login Form (Empty)    │
│  ┌───────────────────┐  │
│  │ Email             │  │
│  └───────────────────┘  │
│  ┌───────────────────┐  │
│  │ Password          │  │
│  └───────────────────┘  │
│  [Login] [Register]     │
│  ─── OR ───             │
│  [🔵 Login with Google] │
└─────────────────────────┘

┌─────────────────────────┐
│   Login Form (Loading)  │
│  ⏳ Logging in...        │
│  [Disabled inputs]      │
└─────────────────────────┘

┌─────────────────────────┐
│   Login Form (Error)    │
│  ❌ Invalid credentials  │
│  [Active inputs]        │
└─────────────────────────┘
```

---

## Google OAuth Login

### Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant LoginPage
    participant GoogleOAuth
    participant API
    participant LocalStorage
    participant Dashboard

    User->>LoginPage: Click "Login with Google"
    LoginPage->>GoogleOAuth: Open Google popup
    GoogleOAuth-->>User: Select Google account
    User->>GoogleOAuth: Authorize app
    GoogleOAuth-->>LoginPage: Return credential token
    LoginPage->>API: POST /auth/google-login {credential}
    
    alt User exists
        API-->>LoginPage: 200 OK + JWT token
        LoginPage->>LocalStorage: Store token
        LoginPage->>Dashboard: Redirect to /dashboard
    else User not found
        API-->>LoginPage: 404 Not Found
        LoginPage-->>User: "Account not found, please register"
    end
```

### Google OAuth Configuration

**Google Identity Services API:**
```javascript
window.google.accounts.id.initialize({
  client_id: process.env.REACT_APP_GOOGLE_CLIENT_ID,
  callback: handleGoogleLogin
});
```

**Request:**
```http
POST /auth/google-login
Content-Type: application/json

{
  "credential": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjM..."
}
```

---

## Email/Password Registration

### Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant RegisterPage
    participant API
    participant EmailService
    participant VerifyPage

    User->>RegisterPage: Fill form (name, email, password)
    RegisterPage->>API: POST /auth/register
    
    alt Success
        API->>EmailService: Send verification code
        API-->>RegisterPage: 201 Created
        RegisterPage->>VerifyPage: Redirect with email param
        EmailService-->>User: Email with 6-digit code
    else Error (Email exists)
        API-->>RegisterPage: 409 Conflict
        RegisterPage-->>User: "Email already registered"
    else Error (Validation failed)
        API-->>RegisterPage: 400 Bad Request
        RegisterPage-->>User: Show validation errors
    end
```

### Password Validation

**Requirements:**
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character

**Validation Logic:**
```mermaid
flowchart TD
    A[Password Input] --> B{Length >= 8?}
    B -->|No| E[❌ Error]
    B -->|Yes| C{Has uppercase?}
    C -->|No| E
    C -->|Yes| D{Has lowercase?}
    D -->|No| E
    D -->|Yes| F{Has number?}
    F -->|No| E
    F -->|Yes| G{Has special char?}
    G -->|No| E
    G -->|Yes| H[✅ Valid]
```

### Request/Response

**Request:**
```http
POST /auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response (Success):**
```json
{
  "message": "User registered successfully. Please check your email for verification code.",
  "email": "john@example.com"
}
```

---

## Google OAuth Registration

### Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant RegisterPage
    participant GoogleOAuth
    participant API
    participant LocalStorage
    participant Dashboard

    User->>RegisterPage: Click "Register with Google"
    RegisterPage->>GoogleOAuth: Open Google popup
    GoogleOAuth-->>User: Select Google account
    User->>GoogleOAuth: Authorize app
    GoogleOAuth-->>RegisterPage: Return credential token
    RegisterPage->>API: POST /auth/google-register {credential}
    
    alt Success (New user)
        API-->>RegisterPage: 201 Created + JWT token
        RegisterPage->>LocalStorage: Store token
        RegisterPage->>Dashboard: Redirect to /dashboard
        Note over RegisterPage: Email auto-verified ✅
    else Error (Email exists)
        API-->>RegisterPage: 409 Conflict
        RegisterPage-->>User: "Email already registered"
    end
```

### Key Difference: Email vs Google OAuth

| Feature | Email/Password | Google OAuth |
|---------|---------------|--------------|
| Email Verification | ✉️ Required (6-digit code) | ✅ Auto-verified |
| Password | 🔒 User creates | ❌ Not needed |
| Registration Time | 2-3 minutes | 10 seconds |
| Security | Medium (depends on password) | High (Google auth) |

---

## Email Verification

### Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant VerifyPage
    participant API
    participant LoginPage

    User->>VerifyPage: Land on page (email in URL)
    VerifyPage->>User: Show 6-digit code input
    
    Note over User: User checks email
    User->>VerifyPage: Enter 6-digit code
    VerifyPage->>API: POST /auth/verify-email
    
    alt Success
        API-->>VerifyPage: 200 OK
        VerifyPage-->>User: ✅ "Email verified!"
        VerifyPage->>LoginPage: Redirect after 2s
    else Error (Invalid code)
        API-->>VerifyPage: 400 Bad Request
        VerifyPage-->>User: "Invalid code"
    else Error (Code expired)
        API-->>VerifyPage: 410 Gone
        VerifyPage-->>User: "Code expired. Resend?"
    end
    
    alt User clicks "Resend Code"
        VerifyPage->>API: POST /auth/resend-verification
        API-->>VerifyPage: 200 OK
        VerifyPage-->>User: "New code sent!"
    end
```

### Verification Code Format

**Format:** 6 digits (e.g., `123456`)  
**Expiry:** 15 minutes  
**Storage:** Backend database with timestamp

### UI States

```
┌─────────────────────────────┐
│   Email Verification        │
│  Check your email for code  │
│  ┌─┬─┬─┬─┬─┬─┐              │
│  │1│2│3│4│5│6│              │
│  └─┴─┴─┴─┴─┴─┘              │
│  [Verify]                   │
│  Didn't receive? [Resend]   │
└─────────────────────────────┘
```

---

## Forgot Password

### Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant ForgotPasswordPage
    participant API
    participant EmailService
    participant ResetPasswordPage

    User->>ForgotPasswordPage: Enter email
    ForgotPasswordPage->>API: POST /auth/forgot-password
    
    alt Email exists
        API->>EmailService: Send reset code
        API-->>ForgotPasswordPage: 200 OK
        ForgotPasswordPage->>ResetPasswordPage: Redirect with email
        EmailService-->>User: Email with 6-digit code
    else Email not found
        API-->>ForgotPasswordPage: 404 Not Found
        Note over ForgotPasswordPage: For security, show generic message
        ForgotPasswordPage-->>User: "If email exists, code was sent"
    end
```

### Security Note

Even if the email doesn't exist, the app shows the same success message to prevent email enumeration attacks.

---

## Reset Password

### Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant ResetPasswordPage
    participant API
    participant LoginPage

    User->>ResetPasswordPage: Land on page (email in URL)
    User->>ResetPasswordPage: Enter code & new password
    ResetPasswordPage->>API: POST /auth/reset-password
    
    alt Success
        API-->>ResetPasswordPage: 200 OK
        ResetPasswordPage-->>User: ✅ "Password reset!"
        ResetPasswordPage->>LoginPage: Redirect after 2s
    else Error (Invalid code)
        API-->>ResetPasswordPage: 400 Bad Request
        ResetPasswordPage-->>User: "Invalid code"
    else Error (Code expired)
        API-->>ResetPasswordPage: 410 Gone
        ResetPasswordPage-->>User: "Code expired. Request new one"
    end
```

### Reset Password States

```mermaid
stateDiagram-v2
    [*] --> EnterCode
    EnterCode --> ValidatingCode: Submit
    ValidatingCode --> EnterPassword: Valid
    ValidatingCode --> CodeError: Invalid/Expired
    CodeError --> EnterCode: Retry
    EnterPassword --> Resetting: Submit
    Resetting --> Success: Password updated
    Resetting --> PasswordError: Validation failed
    PasswordError --> EnterPassword: Fix errors
    Success --> [*]
```

---

## Component Relationships

```mermaid
graph TD
    A[App.js] --> B[Login]
    A --> C[Register]
    A --> D[VerifyEmail]
    A --> E[ForgotPassword]
    A --> F[ResetPassword]
    
    B --> G[api.js - Axios]
    C --> G
    D --> G
    E --> G
    F --> G
    
    G --> H[LocalStorage]
    
    B --> I[Dashboard]
    C --> D
    D --> B
    E --> F
    F --> B
    
    style G fill:#667eea,color:#fff
    style H fill:#764ba2,color:#fff
```

---

## JWT Token Management

### Token Flow

```mermaid
sequenceDiagram
    participant Component
    participant AxiosInterceptor
    participant LocalStorage
    participant API
    
    Component->>AxiosInterceptor: Make API request
    AxiosInterceptor->>LocalStorage: Get token
    LocalStorage-->>AxiosInterceptor: Return token
    AxiosInterceptor->>API: Add Authorization header
    API-->>AxiosInterceptor: Response
    
    alt 401 Unauthorized
        AxiosInterceptor->>LocalStorage: Remove token
        AxiosInterceptor->>Component: Redirect to /login
    else 200 OK
        AxiosInterceptor->>Component: Return data
    end
```

### Axios Interceptor Code

```javascript
// Request interceptor - Add token to headers
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - Handle 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

## Error Handling

### Error Message Mapping

| HTTP Status | Error Type | User Message |
|-------------|-----------|--------------|
| 400 | Bad Request | "Invalid input. Please check your data." |
| 401 | Unauthorized | "Invalid email or password." |
| 403 | Forbidden | "Please verify your email first." |
| 404 | Not Found | "Account not found." |
| 409 | Conflict | "Email already registered." |
| 410 | Gone | "Verification code expired." |
| 500 | Server Error | "Something went wrong. Try again later." |

### Error State Diagram

```mermaid
flowchart TD
    A[API Request] --> B{Response}
    B -->|200-299| C[✅ Success]
    B -->|400| D[❌ Validation Error]
    B -->|401| E[❌ Unauthorized]
    B -->|403| F[❌ Forbidden]
    B -->|404| G[❌ Not Found]
    B -->|409| H[❌ Conflict]
    B -->|410| I[❌ Expired]
    B -->|500+| J[❌ Server Error]
    
    D --> K[Show field errors]
    E --> L[Redirect to login]
    F --> M[Show verification message]
    G --> N[Show not found message]
    H --> O[Show conflict message]
    I --> P[Show resend option]
    J --> Q[Show retry button]
```

---

## Summary

✅ **Authentication Methods:** Email/Password + Google OAuth  
✅ **Verification:** 6-digit codes with 15-minute expiry  
✅ **Security:** JWT tokens, password validation, auto-logout  
✅ **User Experience:** Clear error messages, loading states  
✅ **Token Management:** Axios interceptors for seamless API calls

---

[← Back to Main README](../README.md)
