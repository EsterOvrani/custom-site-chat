# 📚 תיעוד מערכת Custom Site Chat

> **תיעוד מקיף למערכת ניהול צ'אטבוט מבוסס AI עם RAG (Retrieval-Augmented Generation)**

---

## 🌐 **אודות המערכת**

**Custom Site Chat** היא מערכת מתקדמת המאפשרת הטמעת צ'אטבוט חכם באתרים, עם יכולות עיבוד מסמכים והבנת תוכן באמצעות AI.

- **אתר:** [custom-site-chat.com](https://custom-site-chat.com)
- **טכנולוגיות:** React, Node.js, FastAPI, PostgreSQL, Qdrant, OpenAI GPT-4
- **תכונות עיקריות:** אימות משתמשים, העלאת מסמכים, Chat Widget, RAG, Google OAuth

---

## 📖 **מבנה התיעוד**

תיעוד המערכת מחולק לשני חלקים עיקריים:

### 🎨 **[Frontend Documentation](docs/frontend/README.md)**
תיעוד ממשק המשתמש (React Application)

- **[📋 README - סקירה כללית](docs/frontend/README.md)**
  - ארכיטקטורה של האפליקציה
  - מבנה תיקיות
  - ניווט ראשי
  - State Management
  - עיצוב UI ו-Design System
  - הגדרות פיתוח

- **[🔐 AUTH_FLOW - זרימות אימות](docs/frontend/AUTH_FLOW.md)**
  - התחברות עם Email/Password
  - התחברות עם Google OAuth
  - הרשמה ואימות אימייל
  - שחזור סיסמה
  - ניהול JWT Tokens
  - 14 תרשימי זרימה

- **[📄 DOCUMENTS_FLOW - ניהול מסמכים](docs/frontend/DOCUMENTS_FLOW.md)**
  - העלאת קבצים
  - מערכת Polling לסטטוס
  - שלבי עיבוד המסמכים
  - Progress Bar
  - הורדה וצפייה במסמכים
  - מחיקת מסמכים
  - 11 תרשימי זרימה

- **[⚙️ SETTINGS_FLOW - הגדרות אוסף](docs/frontend/SETTINGS_FLOW.md)**
  - טעינת הגדרות
  - יצירת Secret Key
  - חידוש Secret Key
  - העתקת Embed Code
  - ניהול Collection
  - 9 תרשימי זרימה

- **[💬 WIDGET_FLOW - Chat Widget](docs/frontend/WIDGET_FLOW.md)**
  - אתחול Widget
  - שאילת שאלות
  - ניהול היסטוריה
  - ניהול Limits
  - איפוס שיחה
  - תמיכה ב-RTL
  - 10 תרשימי זרימה

### 🔧 **Backend Documentation**
תיעוד API ושרתים (מתוכנן - יתווסף בעתיד)

- **Backend API** - Node.js + Express
  - Auth API
  - User API
  - Collection API
  - Documents API
  
- **AI Service** - FastAPI + Python
  - Query API
  - RAG Engine
  - Vector Database (Qdrant)
  - OpenAI Integration

---

## 🚀 **התחלה מהירה**

### **למפתחים:**

1. **קרא את [Frontend README](docs/frontend/README.md)** - הבנת הארכיטקטורה
2. **עקוב אחרי [AUTH_FLOW](docs/frontend/AUTH_FLOW.md)** - מערכת האימות
3. **למד את [DOCUMENTS_FLOW](docs/frontend/DOCUMENTS_FLOW.md)** - ניהול מסמכים
4. **הבן את [WIDGET_FLOW](docs/frontend/WIDGET_FLOW.md)** - הטמעת Widget

### **למשתמשי קצה:**

1. **הרשמה לאתר** - [custom-site-chat.com](https://custom-site-chat.com)
2. **העלאת מסמכים** - PDF, DOCX, TXT (עד 50 MB)
3. **קבלת Secret Key** - בעמוד Settings
4. **הטמעת Widget** - העתקת קוד ההטמעה לאתר

---

## 🛠️ **סטאק טכנולוגי**

### **Frontend:**
- ⚛️ React 18.2.0
- 🧭 React Router
- 📡 Axios
- 🎨 CSS Modules + Gradients
- 🔄 React Hooks (useState, useEffect, useRef)

### **Backend:**
- 🟢 Node.js + Express
- 🐍 Python + FastAPI
- 🗄️ PostgreSQL
- 🔍 Qdrant Vector DB
- 🤖 OpenAI GPT-4

### **אימות:**
- 🔐 JWT Tokens
- 🔑 Google OAuth 2.0
- 📧 Email Verification
- 🔄 Password Reset

### **DevOps:**
- 🐳 Docker + Docker Compose
- ☁️ Cloud Deployment
- 📊 Monitoring & Logging

---

## 📊 **תרשים ארכיטקטורה כללי**

```
┌─────────────────────────────────────────────────────────────────┐
│                         משתמש קצה                                │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    React Application                             │
│  ┌──────────────┬───────────────┬──────────────┬──────────────┐ │
│  │ Auth Pages   │ Documents     │ Settings     │ Chat Widget  │ │
│  │              │ Management    │ Page         │ (Embedded)   │ │
│  └──────────────┴───────────────┴──────────────┴──────────────┘ │
└────────────────┬────────────────────────────────────────────────┘
                 │ HTTP/HTTPS + JWT
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Backend Services                              │
│  ┌────────────────────────────┬──────────────────────────────┐  │
│  │  Node.js API Server        │  FastAPI AI Service          │  │
│  │  • Auth                    │  • Query Processing          │  │
│  │  • Users                   │  • RAG Engine                │  │
│  │  • Collections             │  • Embeddings                │  │
│  │  • Documents               │  • GPT-4 Integration         │  │
│  └──────────┬─────────────────┴────────────┬─────────────────┘  │
└─────────────┼──────────────────────────────┼────────────────────┘
              │                              │
              ▼                              ▼
   ┌──────────────────┐          ┌──────────────────────┐
   │   PostgreSQL     │          │  Qdrant Vector DB    │
   │   (Metadata)     │          │  (Embeddings)        │
   └──────────────────┘          └──────────────────────┘
                                           │
                                           ▼
                                  ┌──────────────────┐
                                  │   OpenAI API     │
                                  │   (GPT-4)        │
                                  └──────────────────┘
```

---

## 🎯 **תכונות עיקריות**

### ✅ **אימות ואבטחה**
- התחברות עם Email/Password
- התחברות עם Google OAuth
- אימות אימייל עם קוד 6 ספרות
- שחזור סיסמה מאובטח
- JWT Tokens עם Refresh
- Secret Keys לכל Collection

### 📤 **ניהול מסמכים**
- העלאת קבצים: PDF, DOCX, TXT
- גודל מקסימלי: 50 MB
- עיבוד אוטומטי: Extraction → Chunking → Embeddings
- Progress Bar עם שלבים
- הורדה וצפייה במסמכים
- מחיקה רכה (Soft Delete)

### 💬 **Chat Widget**
- הטמעה פשוטה באתר (קוד JavaScript)
- תמיכה ב-RTL (עברית/ערבית)
- ניהול היסטוריה (עד 10 הודעות)
- Limits API (15 שאלות/חודש)
- עיצוב מותאם אישית
- גלילה אוטומטית

### 🤖 **AI & RAG**
- שליפת מידע מהמסמכים
- חיפוש סמנטי עם Embeddings
- תשובות מבוססות GPT-4
- ציטוט מקורות
- תמיכה במספר שפות

---

## 📈 **נתונים טכניים**

| פרמטר | ערך |
|-------|-----|
| **גודל מקסימלי לקובץ** | 50 MB |
| **מספר מסמכים מקסימלי** | ללא הגבלה |
| **זמן עיבוד ממוצע** | 1-3 דקות |
| **תדירות Polling** | 2 שניות |
| **Presigned URL TTL** | 1 שעה |
| **מקסימום היסטוריה בWidget** | 10 הודעות |
| **Limit שאלות** | 15/חודש (בסיסי) |
| **זמן תשובה ממוצע** | 2-4 שניות |
| **גודל Widget Script** | ~15 KB |

---

## 🔗 **קישורים חשובים**

- 🌐 **אתר:** [custom-site-chat.com](https://custom-site-chat.com)
- 📚 **תיעוד Frontend:** [docs/frontend/](docs/frontend/)
- 🔐 **Auth Flow:** [docs/frontend/AUTH_FLOW.md](docs/frontend/AUTH_FLOW.md)
- 📄 **Documents Flow:** [docs/frontend/DOCUMENTS_FLOW.md](docs/frontend/DOCUMENTS_FLOW.md)
- ⚙️ **Settings Flow:** [docs/frontend/SETTINGS_FLOW.md](docs/frontend/SETTINGS_FLOW.md)
- 💬 **Widget Flow:** [docs/frontend/WIDGET_FLOW.md](docs/frontend/WIDGET_FLOW.md)

---

## 🎨 **Design System**

### **צבעים:**
- 🔵 Primary: `#667eea` (כחול-סגול)
- 🟣 Secondary: `#764ba2` (סגול כהה)
- 🟢 Success: `#28a745`
- 🟡 Warning: `#ffc107`
- 🔴 Error: `#dc3545`
- 🔵 Info: `#17a2b8`

### **Gradients:**
- Primary: `linear-gradient(135deg, #667eea 0%, #764ba2 100%)`

### **טיפוגרפיה:**
- Font: `'Segoe UI', Tahoma, Geneva, Verdana, sans-serif`
- כיוון: RTL (עברית)

---

## 🔒 **אבטחה**

- ✅ JWT Tokens ב-LocalStorage
- ✅ HTTPS בכל הסביבות
- ✅ Google OAuth 2.0 מאובטח
- ✅ Secret Keys ייחודיים לכל Collection
- ✅ Rate Limiting על API
- ✅ Validation על צד שרת וצד לקוח
- ✅ Presigned URLs עם TTL
- ✅ Soft Delete למסמכים

---

## 📝 **רישיון**

© 2024 Custom Site Chat. All rights reserved.

---

## 📞 **תמיכה**

- 📧 Email: support@custom-site-chat.com
- 🌐 Website: [custom-site-chat.com](https://custom-site-chat.com)
- 📚 Documentation: [docs/](docs/)

---

## 🗺️ **מפת תיעוד - ניווט מהיר**

```
README.md (אתה כאן)
│
├── 📁 docs/
│   │
│   └── 📁 frontend/
│       ├── 📋 README.md ...................... ארכיטקטורה כללית
│       ├── 🔐 AUTH_FLOW.md ................... זרימות אימות (14 תרשימים)
│       ├── 📄 DOCUMENTS_FLOW.md .............. ניהול מסמכים (11 תרשימים)
│       ├── ⚙️ SETTINGS_FLOW.md ............... הגדרות (9 תרשימים)
│       └── 💬 WIDGET_FLOW.md ................. Chat Widget (10 תרשימים)
│
└── 📁 backend/ (מתוכנן לעתיד)
    ├── 📋 README.md
    ├── 🔐 AUTH_API.md
    ├── 👤 USER_API.md
    ├── 📁 COLLECTION_API.md
    ├── 📄 DOCUMENT_API.md
    └── 🤖 QUERY_API.md
```

---

## 🎓 **מדריך למתחילים**

### **שלב 1: הבנת המערכת**
1. קרא README זה לקבלת סקירה כללית
2. עבור ל-[Frontend README](docs/frontend/README.md) להבנת הארכיטקטורה
3. צפה בתרשימי הזרימה בכל מודול

### **שלב 2: הגדרת סביבת פיתוח**
1. התקן Node.js + npm
2. Clone הפרויקט
3. `npm install` בתיקיית frontend
4. `npm start` להרצת שרת פיתוח

### **שלב 3: פיתוח**
1. עקוב אחרי [AUTH_FLOW](docs/frontend/AUTH_FLOW.md) למערכת אימות
2. למד את [DOCUMENTS_FLOW](docs/frontend/DOCUMENTS_FLOW.md) לעבודה עם מסמכים
3. הבן את [WIDGET_FLOW](docs/frontend/WIDGET_FLOW.md) להטמעה באתרים

### **שלב 4: בדיקות**
1. בדוק כל זרימת אימות
2. העלה מסמכים לדוגמה
3. בדוק את ה-Widget באתר מקומי
4. ווידא תמיכה ב-RTL

### **שלב 5: Deployment**
1. Build production: `npm run build`
2. העלה ל-Cloud (Docker/VPS)
3. הגדר משתני סביבה
4. בדוק HTTPS

---

## 📊 **סטטיסטיקות תיעוד**

- **📁 קבצי תיעוד:** 5 (Frontend)
- **📊 תרשימי Mermaid:** 54
- **📏 שורות קוד תיעוד:** 2,455+
- **💾 גודל כולל:** 72 KB
- **🌍 שפה:** עברית (RTL)
- **📝 סגנון:** Flow Diagrams + UI Visualizations

---

## ✨ **עדכונים אחרונים**

**v1.0.0** - דצמבר 2024
- ✅ תיעוד Frontend מלא עם 54 תרשימי זרימה
- ✅ 5 מודולים מפורטים
- ✅ תמיכה ב-RTL מלאה
- ✅ Design System מקיף
- ✅ מדריכים ודוגמאות

---

**🎉 ברוכים הבאים למערכת Custom Site Chat!**

*תיעוד זה מתעדכן באופן שוטף. לשאלות ובעיות, פנה לתמיכה.*
