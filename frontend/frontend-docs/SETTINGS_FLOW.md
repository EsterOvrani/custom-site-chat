# ⚙️ Settings Flow - זרימת הגדרות

[← חזרה ל-README הראשי](./README.md)

---

## סקירה

טאב ההגדרות מאפשר:
- 📊 צפייה במידע על הקולקשן
- 🔑 ניהול Secret Key
- 📋 קוד הטמעה (Embed Code)

---

## 1️⃣ טעינת הגדרות - Load Settings

### תרשים זרימה

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Dashboard as Dashboard
    participant API as API Service
    participant Backend as Backend
    participant Qdrant as Qdrant
    
    U->>Dashboard: לוחץ על טאב "הגדרות"
    Dashboard->>Dashboard: setActiveTab('settings')
    
    Note over Dashboard: useEffect מופעל
    
    Dashboard->>API: collectionAPI.getCollectionInfo()
    API->>Backend: GET /api/collection/info
    
    Backend->>Backend: מצא משתמש
    
    alt קולקשן קיים
        Backend-->>API: 200 OK + collection data
    else קולקשן לא קיים
        Backend->>Backend: יצירת collection_name
        Backend->>Backend: יצירת secret_key
        Backend->>Qdrant: Create collection
        Backend->>Backend: יצירת embed_code
        Backend->>Backend: שמירה ב-DB
        Backend-->>API: 200 OK + collection data
    end
    
    API-->>Dashboard: response.data.data
    Dashboard->>Dashboard: setCollection(data)
    Dashboard-->>U: הצגת הגדרות
```

### Collection Name Format

```
user_{userId}_{uuid8}

דוגמה: user_1_a7b3f2e1
```

### Secret Key Format

```
sk_{uuid32}

דוגמה: sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
```

---

## 2️⃣ חידוש Secret Key - Regenerate Key

### תרשים זרימה

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Settings as CollectionSettings
    participant Confirm as window.confirm
    participant API as API Service
    participant Backend as Backend
    
    U->>Settings: לוחץ "🔄 צור מפתח חדש"
    Settings->>Confirm: "האם אתה בטוח?"
    
    Note over Confirm: אזהרה: המפתח הישן<br/>יהפוך ללא תקף!
    
    alt משתמש מאשר
        Confirm-->>Settings: true
        Settings->>Settings: setLoading(true)
        Settings->>API: collectionAPI.regenerateSecretKey()
        
        API->>Backend: POST /api/collection/regenerate-key
        Backend->>Backend: UUID.randomUUID()
        Backend->>Backend: format: sk_{uuid}
        Backend->>Backend: עדכון: user.collectionSecretKey
        Backend->>Backend: עדכון: user.embedCode
        Backend-->>API: 200 OK + new collection
        
        API-->>Settings: response.data.data
        Settings->>Settings: setCollection(newData)
        Settings->>Settings: showToast("מפתח חדש נוצר")
        Settings-->>U: הצג מפתח חדש
    else משתמש מבטל
        Confirm-->>Settings: false
        Settings-->>U: אין פעולה
    end
    
    Settings->>Settings: setLoading(false)
```

### ⚠️ השפעת חידוש מפתח

```mermaid
graph TB
    Regenerate[חידוש מפתח] --> Old[מפתח ישן]
    Regenerate --> New[מפתח חדש]
    
    Old --> Invalid[❌ לא תקף יותר]
    Invalid --> WidgetFail[Widget מפסיק לעבוד]
    WidgetFail --> Update[צריך לעדכן קוד באתר!]
    
    New --> Valid[✅ תקף]
    Valid --> Works[Widget חדש יעבוד]
    
    style Invalid fill:#dc3545,color:#fff
    style Valid fill:#28a745,color:#fff
```

---

## 3️⃣ העתקת Secret Key

### תרשים זרימה

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Settings as CollectionSettings
    participant Clipboard as navigator.clipboard
    participant State as copiedKey state
    
    U->>Settings: לוחץ "📋 העתק"
    Settings->>Clipboard: writeText(secretKey)
    Clipboard-->>Settings: הועתק
    Settings->>State: setCopiedKey(true)
    Settings-->>U: כפתור: "✓ הועתק"
    
    Note over Settings: המתן 2 שניות
    
    Settings->>State: setCopiedKey(false)
    Settings-->>U: כפתור: "📋 העתק"
```

### UI Feedback

```
לפני: [📋 העתק]
אחרי: [✓ הועתק]  (ירוק)
```

---

## 4️⃣ העתקת קוד הטמעה - Copy Embed Code

### תרשים זרימה

```mermaid
sequenceDiagram
    participant U as משתמש
    participant Settings as CollectionSettings
    participant Clipboard as navigator.clipboard
    participant State as copiedEmbed state
    
    U->>Settings: לוחץ "📋 העתק קוד"
    Settings->>Clipboard: writeText(embedCode)
    Clipboard-->>Settings: הועתק
    Settings->>State: setCopiedEmbed(true)
    Settings-->>U: כפתור: "✓ הועתק"
    
    Note over Settings: המתן 2 שניות
    
    Settings->>State: setCopiedEmbed(false)
    Settings-->>U: כפתור: "📋 העתק קוד"
```

### Embed Code Structure

```html
<!-- Custom Site Chat Widget -->
<script>
  window.CHAT_WIDGET_SECRET_KEY = 'sk_...';
  window.CHAT_WIDGET_API_URL = 'http://localhost:8080';
  
  // התאמה אישית (אופציונלי)
  window.CHAT_WIDGET_TITLE = 'שם החברה';
  window.CHAT_WIDGET_BOT_NAME = 'שם הבוט';
  window.CHAT_WIDGET_BOT_AVATAR = 'URL או null';
  window.CHAT_WIDGET_USER_AVATAR = 'URL או null';
</script>
<script src="http://localhost:3000/chat-widget.js"></script>
<!-- End Chat Widget -->
```

---

## 5️⃣ תצוגת UI - CollectionSettings Component

### Layout Structure

```
┌─────────────────────────────────────────────────────┐
│  ⚙️ הגדרות קולקשן וקוד הטמעה                      │
│                                                      │
│  ┌──────────────────────────────────────────────┐  │
│  │  📊 מידע על הקולקשן                         │  │
│  │                                               │  │
│  │  שם קולקשן:    user_1_a7b3f2e1              │  │
│  │  נוצר בתאריך:  15/01/2025                   │  │
│  └──────────────────────────────────────────────┘  │
│                                                      │
│  ┌──────────────────────────────────────────────┐  │
│  │  🔑 Secret Key                                │  │
│  │                                               │  │
│  │  [sk_a1b2c3d4...]  [📋 העתק]  [🔄 חדש]     │  │
│  │                                               │  │
│  │  ⚠️ אזהרה: יצירת מפתח חדש תבטל הישן        │  │
│  └──────────────────────────────────────────────┘  │
│                                                      │
│  ┌──────────────────────────────────────────────┐  │
│  │  🎨 קוד הטמעה                                │  │
│  │                                               │  │
│  │  ┌────────────────────────────────────────┐  │  │
│  │  │  <!-- Custom Site Chat Widget -->     │  │  │
│  │  │  <script>                              │  │  │
│  │  │    window.CHAT_WIDGET_SECRET_KEY...   │  │  │
│  │  │                       [📋 העתק קוד]   │  │  │
│  │  └────────────────────────────────────────┘  │  │
│  │                                               │  │
│  │  💡 טיפ: הדבק לפני תג </body>               │  │
│  └──────────────────────────────────────────────┘  │
│                                                      │
│  ┌──────────────────────────────────────────────┐  │
│  │  📖 דוגמת שימוש                              │  │
│  │                                               │  │
│  │  <!DOCTYPE html>                              │  │
│  │  <html>...                                    │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## 6️⃣ State Management

### Component State

```javascript
const [copiedEmbed, setCopiedEmbed] = useState(false);
const [copiedKey, setCopiedKey] = useState(false);
```

### Props from Dashboard

```javascript
<CollectionSettings
  collection={collection}           // object
  onRegenerateKey={handleRegenerate} // function
  loading={loading}                 // boolean
/>
```

---

## 7️⃣ Error Handling

### תרשים שגיאות

```mermaid
graph TB
    Load[טעינת הגדרות] --> Check{הצליח?}
    
    Check -->|כן| Display[הצג הגדרות]
    Check -->|לא| Error[הצג שגיאה]
    
    Error --> E1[401: לא מחובר]
    Error --> E2[503: Qdrant לא זמין]
    Error --> E3[500: שגיאת שרת]
    
    E1 --> Redirect[הפניה ל-Login]
    E2 --> Toast1[הצג: "שגיאה בשירות"]
    E3 --> Toast2[הצג: "שגיאה כללית"]
    
    style E1 fill:#dc3545,color:#fff
    style E2 fill:#ffc107
    style E3 fill:#dc3545,color:#fff
```

---

## 📊 Copy Mechanism Flow

```mermaid
sequenceDiagram
    participant Button
    participant Handler
    participant API as Clipboard API
    participant State
    participant UI
    
    Button->>Handler: onClick
    Handler->>API: navigator.clipboard.writeText(text)
    
    alt הצליח
        API-->>Handler: Promise resolved
        Handler->>State: setCopied(true)
        State->>UI: עדכון כפתור → "✓ הועתק"
        
        Note over Handler: setTimeout 2000ms
        
        Handler->>State: setCopied(false)
        State->>UI: חזרה ל-"📋 העתק"
    else נכשל
        API-->>Handler: Promise rejected
        Handler->>UI: הצג שגיאה
    end
```

---

## 🎨 Visual States

### Loading State

```
┌──────────────────────────────┐
│  ⚙️ טוען הגדרות...          │
│                               │
│       [spinner]               │
└──────────────────────────────┘
```

### Success State (After Regenerate)

```
┌──────────────────────────────────────┐
│  ✅ מפתח חדש נוצר בהצלחה!          │
│                                       │
│  🔑 sk_NEW_KEY_x9y8z7w6v5u4         │
└──────────────────────────────────────┘
```

### Copy Feedback

```javascript
// Before
[📋 העתק]

// During (2 seconds)
[✓ הועתק]  // Green background

// After
[📋 העתק]  // Back to default
```

---

## 🔐 Security Considerations

### Secret Key Visibility

```mermaid
graph TB
    Secret[Secret Key] --> Visible[גלוי בממשק]
    Visible --> Warning[⚠️ אזהרה]
    Warning --> Tip1[אל תשתף בפומבי]
    Warning --> Tip2[שמור במקום בטוח]
    Warning --> Tip3[חדש במקרה של דליפה]
    
    style Warning fill:#ffc107
```

### Embed Code Security

- **Embedded ב-HTML:** הקוד נכלל ב-HTML הציבורי
- **Secret Key חשוף:** כל מי שרואה את הקוד יכול לראות את המפתח
- **מגבלות:** Secret Key מאפשר רק שאילת שאלות (Query API)
- **לא מאפשר:** מחיקה, עריכה, או גישה לנתוני משתמש

---

## 📋 Quick Reference

| פעולה | Endpoint | תוצאה |
|-------|----------|-------|
| טעינה | GET /collection/info | קבלת/יצירת collection |
| חידוש | POST /collection/regenerate-key | Secret Key חדש |
| קוד | GET /collection/embed-code | קוד להטמעה |

---

[← חזרה ל-README הראשי](./README.md)
