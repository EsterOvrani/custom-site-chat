# 🔐 Authentication Flow - זרימות אימות

[← חזרה ל-README הראשי](./README.md)

---

## סקירה כללית

מודול ה-Authentication כולל 5 מסלולים עיקריים:
1. **התחברות** (Login) - Email/Password + Google OAuth
2. **רישום** (Register) - Email/Password + Google OAuth  
3. **אימות מייל** (Verify)
4. **שכחתי סיסמה** (Forgot Password)
5. **איפוס סיסמה** (Reset Password)

---

## 1️⃣ זרימת התחברות (Login)

### 📊 תרשים זרימה - Email/Password Login

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Login as Login Component
    participant API as API Service
    participant LS as LocalStorage
    participant Backend as Backend
    participant Nav as Navigation
    
    U->>Login: מזין email + password
    U->>Login: לוחץ "התחבר"
    
    Login->>Login: setLoading(true)
    Login->>API: authAPI.login(email, password)
    
    API->>Backend: POST /auth/login
    
    alt התחברות מוצלחת
        Backend-->>API: 200 OK + {token, user}
        API-->>Login: response.data.success = true
        
        Login->>LS: localStorage.setItem('token', token)
        Login->>LS: localStorage.setItem('user', JSON.stringify(user))
        
        Login->>Nav: navigate('/')
        Nav-->>U: מעבר לדשבורד
    else משתמש לא מאומת
        Backend-->>API: 401 + "User not verified"
        API-->>Login: error
        Login-->>U: הצג: "חשבון לא מאומת. אנא אמת את האימייל"
    else סיסמה שגויה
        Backend-->>API: 401 + "Invalid credentials"
        API-->>Login: error
        Login-->>U: הצג: "אימייל או סיסמה שגויים"
    else משתמש לא קיים
        Backend-->>API: 404 + "User not found"
        API-->>Login: error
        Login-->>U: הצג: "משתמש לא נמצא"
    end
    
    Login->>Login: setLoading(false)
```

### 📊 תרשים זרימה - Google OAuth Login

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Login as Login Component
    participant Google as Google OAuth
    participant API as API Service
    participant LS as LocalStorage
    participant Backend as Backend
    participant Nav as Navigation
    
    U->>Login: לוחץ "היכנס עם Google"
    
    Login->>Google: window.google.accounts.id.prompt()
    Google-->>U: חלון התחברות Google
    
    U->>Google: מאשר התחברות
    Google-->>Login: credential (ID Token)
    
    Login->>Login: setLoading(true)
    Login->>API: authAPI.googleLogin(credential)
    
    API->>Backend: POST /auth/google {credential}
    Backend->>Backend: אימות Token מול Google
    
    alt משתמש קיים
        Backend->>Backend: מצא משתמש לפי email
        Backend-->>API: 200 OK + {token, user}
    else משתמש חדש
        Backend->>Backend: יצירת משתמש חדש
        Backend->>Backend: יצירת username ייחודי
        Backend->>Backend: שמירת תמונת פרופיל
        Backend-->>API: 200 OK + {token, user}
    end
    
    API-->>Login: response.data.success = true
    
    Login->>LS: localStorage.setItem('token', token)
    Login->>LS: localStorage.setItem('user', JSON.stringify(user))
    
    Login->>Nav: navigate('/')
    Nav-->>U: מעבר לדשבורד
    
    Login->>Login: setLoading(false)
```

### 🎯 קבצים מעורבים

| קובץ | תפקיד |
|------|-------|
| `Login.js` | קומפוננטה ראשית |
| `Login.css` | עיצוב |
| `GoogleLoginButton.js` | כפתור Google OAuth |
| `api.js` | שירותי API |

---

## 2️⃣ זרימת רישום (Register)

### 📊 תרשים זרימה - Email/Password Registration

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Register as Register Component
    participant API as API Service
    participant Backend as Backend
    participant Email as Email Service
    participant Nav as Navigation
    
    U->>Register: מזין פרטים (שם, email, password)
    
    Note over Register: Validations בזמן אמת
    
    U->>Register: הקלדת username
    Register->>API: authAPI.checkUsername(username)
    API->>Backend: GET /auth/check-username/{username}
    Backend-->>Register: {available: true/false}
    Register-->>U: הצג: "זמין ✓" או "תפוס"
    
    U->>Register: הקלדת email
    Register->>API: authAPI.checkEmail(email)
    API->>Backend: GET /auth/check-email/{email}
    Backend-->>Register: {available: true/false}
    Register-->>U: הצג: "זמין ✓" או "בשימוש"
    
    U->>Register: לוחץ "הירשם"
    
    Register->>Register: setLoading(true)
    Register->>API: authAPI.register({...userData})
    
    API->>Backend: POST /auth/signup
    Backend->>Backend: hash password (BCrypt)
    Backend->>Backend: יצירת קוד אימות (6 ספרות)
    Backend->>Backend: שמירת משתמש (enabled=false)
    Backend->>Email: שליחת קוד אימות למייל
    Backend-->>API: 200 OK {success: true}
    
    API-->>Register: response.data.success = true
    
    Register->>Register: הצג: "רישום בוצע בהצלחה!"
    Register->>Register: setLoading(false)
    
    Register->>Nav: navigate('/verify?email=...&mode=wait')
    Nav-->>U: מעבר לדף אימות
```

### 📊 תרשים זרימה - Google OAuth Registration

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Register as Register Component
    participant Google as Google OAuth
    participant API as API Service
    participant Backend as Backend
    participant Email as Email Service
    participant Nav as Navigation
    
    U->>Register: לוחץ "הירשם עם Google"
    
    Register->>Google: window.google.accounts.id.prompt()
    Google-->>U: חלון התחברות Google
    
    U->>Google: מאשר
    Google-->>Register: credential (ID Token)
    
    Register->>Register: setLoading(true)
    Register->>API: authAPI.googleLogin(credential)
    
    API->>Backend: POST /auth/google {credential}
    Backend->>Backend: אימות Token מול Google
    Backend->>Backend: בדיקה אם email קיים
    
    alt משתמש כבר קיים
        Backend-->>API: 200 OK + {token, user}
        Note over Backend: משתמש מחובר (לא נוצר חדש)
    else משתמש חדש
        Backend->>Backend: יצירת משתמש חדש
        Backend->>Backend: username = google.name + random
        Backend->>Backend: password = temp (8 chars)
        Backend->>Backend: enabled = true (מאומת אוטומטית!)
        Backend->>Email: שליחת פרטי כניסה למייל
        Backend-->>API: 200 OK + {token, user}
    end
    
    API-->>Register: response.data.success = true
    
    Register->>Register: localStorage.setItem('token', token)
    Register->>Register: localStorage.setItem('user', user)
    Register->>Register: הצג: "נרשמת בהצלחה!"
    
    Register->>Nav: navigate('/')
    Nav-->>U: מעבר לדשבורד (ישירות!)
    
    Register->>Register: setLoading(false)
```

### 🎯 הבדלים מרכזיים

| תכונה | Email/Password | Google OAuth |
|-------|---------------|--------------|
| אימות מייל | נדרש (דף Verify) | לא נדרש |
| enabled | false בהתחלה | true מיד |
| password | בחירת משתמש | random (8 chars) |
| profilePictureUrl | null | מתמונת Google |

---

## 3️⃣ זרימת אימות מייל (Verify)

### 📊 תרשים זרימה - Email Verification

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Verify as Verify Component
    participant API as API Service
    participant Backend as Backend
    participant LS as LocalStorage
    participant Nav as Navigation
    
    Note over U: הגיע דרך URL: /verify?email=x@x.com&mode=wait
    
    U->>Verify: פותח דף אימות
    Verify->>Verify: useEffect: טעינת email מ-URL
    Verify-->>U: הצג: "הזן קוד אימות שנשלח למייל x@x.com"
    
    U->>Verify: מזין 6 ספרות
    U->>Verify: לוחץ "אמת קוד"
    
    Verify->>Verify: setLoading(true)
    Verify->>API: authAPI.verify({email, verificationCode})
    
    API->>Backend: POST /auth/verify
    Backend->>Backend: מצא משתמש לפי email
    
    alt קוד תקין
        Backend->>Backend: בדיקה: code === verificationCode
        Backend->>Backend: בדיקה: now < expiresAt (15 דק')
        Backend->>Backend: עדכון: enabled = true
        Backend->>Backend: מחיקת קוד
        Backend->>Backend: יצירת JWT token
        Backend-->>API: 200 OK + {token, user}
        
        API-->>Verify: response.data.success = true
        
        Verify->>LS: localStorage.setItem('token', token)
        Verify->>LS: localStorage.setItem('user', user)
        Verify->>Verify: setSuccess(true)
        Verify-->>U: הצג: "✅ אומת בהצלחה!"
        Verify->>Verify: ספירה לאחור 3 שניות
        Verify->>Nav: navigate('/')
        Nav-->>U: מעבר לדשבורד
    else קוד שגוי
        Backend-->>API: 400 + "Invalid code"
        API-->>Verify: error
        Verify-->>U: הצג: "❌ קוד אימות שגוי"
    else קוד פג תוקף
        Backend-->>API: 400 + "Code expired"
        API-->>Verify: error
        Verify-->>U: הצג: "❌ קוד פג תוקף"
    end
    
    Verify->>Verify: setLoading(false)
```

### 📊 תרשים זרימה - Resend Code

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Verify as Verify Component
    participant API as API Service
    participant Backend as Backend
    participant Email as Email Service
    
    U->>Verify: לוחץ "שלח קוד חדש"
    
    Verify->>Verify: setResendLoading(true)
    Verify->>API: authAPI.resendVerificationCode(email)
    
    API->>Backend: POST /auth/resend?email=...
    Backend->>Backend: מצא משתמש לפי email
    Backend->>Backend: יצירת קוד חדש (6 ספרות)
    Backend->>Backend: עדכון: verificationCodeExpiresAt = now + 15min
    Backend->>Email: שליחת קוד חדש למייל
    Backend-->>API: 200 OK
    
    API-->>Verify: success
    Verify->>Verify: setResendSuccess(true)
    Verify-->>U: הצג: "✅ קוד חדש נשלח בהצלחה!"
    Verify->>Verify: setResendLoading(false)
```

---

## 4️⃣ זרימת שכחתי סיסמה (Forgot Password)

### 📊 תרשים זרימה - Request Reset Code

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Forgot as ForgotPassword Component
    participant API as API Service
    participant Backend as Backend
    participant Email as Email Service
    participant Nav as Navigation
    
    U->>Forgot: מזין email
    U->>Forgot: לוחץ "שלח קוד איפוס"
    
    Forgot->>Forgot: setLoading(true)
    Forgot->>API: authAPI.forgotPassword(email)
    
    API->>Backend: POST /auth/forgot-password {email}
    Backend->>Backend: מצא משתמש לפי email
    
    alt משתמש קיים
        Backend->>Backend: יצירת resetCode (6 ספרות)
        Backend->>Backend: שמירה: resetCode, resetCodeExpiresAt
        Backend->>Email: שליחת קוד למייל
        Backend-->>API: 200 OK
        
        API-->>Forgot: response.data.success = true
        Forgot->>Forgot: setSuccess(true)
        Forgot-->>U: הצג: "📧 קוד איפוס נשלח למייל!"
        
        Forgot->>Nav: navigate('/verify?email=...&mode=reset')
        Nav-->>U: מעבר לדף אימות קוד
    else משתמש לא קיים
        Backend-->>API: 404 + "User not found"
        API-->>Forgot: error
        Forgot-->>U: הצג: "❌ משתמש לא נמצא"
    end
    
    Forgot->>Forgot: setLoading(false)
```

### 🎯 הבדל בין Verification לבין Reset

| תכונה | Email Verification | Password Reset |
|-------|-------------------|----------------|
| **שדה ב-DB** | `verificationCode` | `resetCode` |
| **תוקף** | 15 דקות | 15 דקות |
| **mode ב-URL** | `mode=wait` | `mode=reset` |
| **פעולה** | enabled=true | פתיחת דף איפוס |

---

## 5️⃣ זרימת איפוס סיסמה (Reset Password)

### 📊 תרשים זרימה - Reset Password (3 Steps)

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Forgot as ForgotPassword
    participant Verify as Verify (mode=reset)
    participant Reset as ResetPassword
    participant API as API Service
    participant Backend as Backend
    participant Nav as Navigation
    
    rect rgb(255, 240, 240)
        Note over U,Forgot: שלב 1: בקשת קוד
        U->>Forgot: מזין email
        Forgot->>Backend: POST /auth/forgot-password
        Backend-->>Forgot: קוד נשלח למייל
        Forgot->>Nav: navigate('/verify?email=...&mode=reset')
    end
    
    rect rgb(240, 255, 240)
        Note over U,Verify: שלב 2: אימות קוד
        U->>Verify: מזין 6 ספרות
        Verify->>Backend: POST /auth/verify-reset-code
        Backend->>Backend: בדיקה: resetCode תקין?
        Backend-->>Verify: {verified: true}
        Verify->>Verify: setSuccess(true)
        Verify->>Nav: navigate('/reset-password?email=...&verified=true')
    end
    
    rect rgb(240, 240, 255)
        Note over U,Reset: שלב 3: שינוי סיסמה
        U->>Reset: מזין סיסמה חדשה
        U->>Reset: אישור סיסמה
        Reset->>Backend: POST /auth/set-new-password
        Backend->>Backend: hash סיסמה חדשה (BCrypt)
        Backend->>Backend: עדכון user.password
        Backend->>Backend: מחיקת resetCode
        Backend-->>Reset: 200 OK "סיסמה שונתה בהצלחה"
        Reset->>Reset: setSuccess(true)
        Reset->>Reset: ספירה לאחור 3 שניות
        Reset->>Nav: navigate('/login?reset=success')
    end
    
    Nav-->>U: מעבר ל-Login עם הודעת הצלחה
```

### 📊 תרשים מצבים - Password Reset States

```mermaid
stateDiagram-v2
    [*] --> ForgotPassword: משתמש שכח סיסמה
    
    ForgotPassword --> EmailSent: קוד נשלח
    EmailSent --> VerifyCode: מזין קוד
    
    VerifyCode --> CodeInvalid: קוד שגוי
    CodeInvalid --> VerifyCode: נסה שוב
    
    VerifyCode --> CodeExpired: פג תוקף
    CodeExpired --> ForgotPassword: בקש קוד חדש
    
    VerifyCode --> CodeValid: קוד תקין
    CodeValid --> ResetPassword: מזין סיסמה חדשה
    
    ResetPassword --> PasswordWeak: סיסמה חלשה
    PasswordWeak --> ResetPassword: תקן סיסמה
    
    ResetPassword --> PasswordMismatch: לא זהה
    PasswordMismatch --> ResetPassword: תקן
    
    ResetPassword --> Success: סיסמה שונתה
    Success --> Login: מעבר להתחברות
    Login --> [*]
```

---

## 🎯 Components Relationships

```mermaid
graph TB
    App[App.js]
    
    subgraph "Authentication Components"
        Login[Login.js]
        Register[Register.js]
        Verify[Verify.js]
        Forgot[ForgotPassword.js]
        Reset[ResetPassword.js]
        GoogleBtn[GoogleLoginButton.js]
    end
    
    API[api.js]
    LS[LocalStorage]
    
    App --> Login
    App --> Register
    App --> Verify
    App --> Forgot
    App --> Reset
    
    Login --> GoogleBtn
    Register --> GoogleBtn
    
    Login --> API
    Register --> API
    Verify --> API
    Forgot --> API
    Reset --> API
    
    API --> LS
    
    style App fill:#667eea,color:#fff
    style API fill:#764ba2,color:#fff
    style LS fill:#ffc107
```

---

## 🔒 Security Features

### JWT Token Management

```mermaid
graph LR
    Login[Login Success] --> Store[localStorage.setItem]
    Store --> Token[token: JWT string]
    
    API[API Request] --> Check{Token exists?}
    Check -->|Yes| Add[Add to Authorization header]
    Check -->|No| NoAuth[No authentication]
    
    Add --> Backend[Backend verifies token]
    Backend --> Valid{Valid?}
    
    Valid -->|Yes| Allow[Allow request]
    Valid -->|No| Error401[401 Unauthorized]
    
    Error401 --> Clear[Clear localStorage]
    Clear --> Redirect[Redirect to /login]
    
    style Token fill:#28a745,color:#fff
    style Error401 fill:#dc3545,color:#fff
    style Allow fill:#28a745,color:#fff
```

### Password Validation

| תנאי | דרישה | משוב UI |
|------|-------|---------|
| **אורך** | מינימום 6 תווים | "חלשה" / "בינונית" / "חזקה" |
| **חזקה** | 8+ תווים + אות גדולה + מספר | צבע: ירוק |
| **בינונית** | 6+ תווים + (אות גדולה או מספר) | צבע: צהוב |
| **חלשה** | 6 תווים | צבע: אדום |

---

## 📱 User Experience

### Success States

```mermaid
stateDiagram-v2
    Login --> ShowSuccess: הצלחה
    ShowSuccess --> Wait3Sec: המתן 3 שניות
    Wait3Sec --> Redirect: navigate('/')
    Redirect --> Dashboard
    
    note right of ShowSuccess
        הצג: ✅ "מתחבר..."
        אנימציה: scaleIn
    end note
    
    note right of Wait3Sec
        ספירה לאחור: 3...2...1
    end note
```

### Loading States

```mermaid
stateDiagram-v2
    Idle --> Loading: User clicks button
    Loading --> ShowSpinner: setLoading(true)
    ShowSpinner --> APICall: Axios request
    APICall --> Response: Backend responds
    Response --> HideSpinner: setLoading(false)
    HideSpinner --> Idle: Show result
    
    note right of ShowSpinner
        - Disable button
        - Show spinner
        - Change text: "מעבד..."
    end note
```

---

## 🆘 Error Handling

### Error Messages Map

| Backend Error | Frontend Message |
|--------------|------------------|
| `AUTHENTICATION_FAILED` | "אימייל או סיסמה שגויים" |
| `USER_NOT_VERIFIED` | "חשבון לא מאומת. אנא אמת את האימייל" |
| `INVALID_CODE` | "קוד אימות שגוי" |
| `CODE_EXPIRED` | "קוד פג תוקף" |
| `USER_NOT_FOUND` | "משתמש לא נמצא" |
| `DUPLICATE_EMAIL` | "האימייל כבר בשימוש" |
| `DUPLICATE_USERNAME` | "שם המשתמש כבר תפוס" |

---

[← חזרה ל-README הראשי](./README.md)
