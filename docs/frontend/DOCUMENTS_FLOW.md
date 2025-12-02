# 📄 Documents Flow - זרימת מסמכים

[← חזרה ל-README הראשי](./README.md)

---

## סקירה

מודול המסמכים כולל:
- 📤 העלאת מסמכים (יחיד/מרובה)
- 📊 מעקב אחר עיבוד (Polling)
- 📥 הורדה וצפייה
- 🗑️ מחיקה

---

## 1️⃣ העלאת מסמכים - Upload Flow

### תרשים זרימה ראשי

```mermaid
sequenceDiagram
    participant U as משתמש
    participant List as DocumentsList
    participant Modal as UploadModal
    participant Dashboard as Dashboard
    participant API as API Service
    participant Backend as Backend
    
    U->>List: לוחץ "העלה מסמך חדש"
    List->>Modal: פתיחת Modal
    
    U->>Modal: בוחר 1+ קבצי PDF
    Modal->>Modal: וולידציה (PDF, <50MB)
    Modal-->>U: הצג רשימת קבצים
    
    U->>Modal: לוחץ "העלה"
    
    loop עבור כל קובץ
        Modal->>Modal: יצירת Placeholder
        Modal->>Dashboard: onComplete(placeholder)
        Dashboard->>List: הוסף Placeholder לרשימה
        
        Note over Modal,Backend: העלאה אסינכרונית
        
        Modal->>API: uploadDocument(file)
        API->>Backend: POST /documents/upload
        Backend->>Backend: שמירת קובץ ב-S3
        Backend->>Backend: יצירת Document (PENDING)
        Backend-->>API: {success, document}
        
        API-->>Modal: document (status=PENDING)
        Modal->>Dashboard: onComplete(document, placeholderId)
        Dashboard->>List: החלף Placeholder במסמך אמיתי
    end
    
    Modal->>Modal: סגור Modal
    
    Note over Backend: 🔄 עיבוד אסינכרוני מתחיל
```

### Placeholder System

```mermaid
graph LR
    A[קובץ נבחר] --> B[יצירת Placeholder]
    B --> C[הצגה ברשימה]
    C --> D[העלאה לשרת]
    D --> E{הצליח?}
    E -->|כן| F[החלף במסמך אמיתי]
    E -->|לא| G[הסר Placeholder]
    
    style B fill:#ffc107
    style F fill:#28a745,color:#fff
    style G fill:#dc3545,color:#fff
```

---

## 2️⃣ מעקב אחר עיבוד - Polling System

### תרשים Polling

```mermaid
sequenceDiagram
    participant Dashboard
    participant State
    participant API
    participant Backend
    
    Dashboard->>State: useEffect: בדיקה
    
    loop כל פעם ש-documents משתנה
        State->>State: filter: PROCESSING/PENDING?
        
        alt יש מסמכים בעיבוד
            State-->>Dashboard: כן, {count} מסמכים
            Dashboard->>Dashboard: setInterval(2000ms)
            
            loop כל 2 שניות
                Dashboard->>API: GET /documents/my-documents
                API->>Backend: בקשה
                Backend-->>API: רשימה מעודכנת
                API-->>Dashboard: documents[]
                Dashboard->>State: setDocuments(newDocs)
                
                Note over Dashboard,State: ⚡ עדכון Progress Bars
            end
        else אין מסמכים בעיבוד
            State-->>Dashboard: לא
            Dashboard->>Dashboard: clearInterval()
            Note over Dashboard: ⏹️ עצור Polling
        end
    end
```

### מצבי עיבוד - Processing States

```mermaid
stateDiagram-v2
    [*] --> PENDING: קובץ הועלה
    PENDING --> PROCESSING: התחל עיבוד
    
    PROCESSING --> UPLOADING: 10-20%
    UPLOADING --> EXTRACTING: 30-45%
    EXTRACTING --> CHUNKING: 50-60%
    CHUNKING --> EMBEDDINGS: 65-95%
    EMBEDDINGS --> COMPLETED: 100%
    
    PROCESSING --> FAILED: שגיאה
    FAILED --> [*]
    COMPLETED --> [*]
    
    note right of UPLOADING: מעלה לשרת
    note right of EXTRACTING: מחלץ טקסט
    note right of CHUNKING: מחלק לחלקים
    note right of EMBEDDINGS: יוצר embeddings
```

---

## 3️⃣ Progress Bar Component

### תצוגה ויזואלית

```
┌─────────────────────────────────────────────────────────┐
│  📄  contract.pdf                         2.34 MB       │
│                                                          │
│  ██████████████░░░░░░░░░░░░░░░░░ 45%                   │
│                                                          │
│  מחלץ טקסט מהמסמך...                            45%    │
└─────────────────────────────────────────────────────────┘
```

### Logic Flow

```mermaid
graph TB
    Props[Props: progress, stage, fileName] --> Icon[קביעת אייקון]
    Props --> Color[קביעת צבע]
    Props --> Bar[Progress Bar]
    
    Icon --> Display1[⬆️ <20%]
    Icon --> Display2[📄 20-40%]
    Icon --> Display3[✂️ 40-60%]
    Icon --> Display4[🧠 60-95%]
    Icon --> Display5[✅ 100%]
    
    Color --> C1[#667eea <20%]
    Color --> C2[#ffc107 20-40%]
    Color --> C3[#17a2b8 40-60%]
    Color --> C4[#28a745 >95%]
    
    Bar --> Animation[Shine Animation]
    
    style Display1 fill:#667eea,color:#fff
    style Display2 fill:#ffc107
    style Display3 fill:#17a2b8,color:#fff
    style Display4 fill:#28a745,color:#fff
    style Display5 fill:#28a745,color:#fff
```

---

## 4️⃣ הורדה וצפייה - Download & View

### תרשים זרימה

```mermaid
sequenceDiagram
    participant U as משתמש
    participant List as DocumentsList
    participant API as API
    participant Backend as Backend
    participant S3 as AWS S3
    
    alt הורדה (Download)
        U->>List: לוחץ "⬇️ הורד"
        List->>API: downloadDocument(docId)
        API->>Backend: GET /documents/{id}/download
        Backend->>S3: קבל קובץ
        S3-->>Backend: Binary data
        Backend-->>API: Blob (PDF)
        API-->>List: response.data
        List->>List: יצירת Blob URL
        List->>List: <a href={url} download>
        List-->>U: הורדת קובץ
    else צפייה (View)
        U->>List: לוחץ "👁️ צפה"
        List->>API: getDownloadUrl(docId)
        API->>Backend: GET /documents/{id}/download-url
        Backend->>S3: Generate Presigned URL (1h)
        S3-->>Backend: URL חתום
        Backend-->>API: {url, expiresIn: 3600}
        API-->>List: response.data.url
        List->>List: window.open(url, '_blank')
        List-->>U: פתיחה בטאב חדש
    end
```

### Presigned URL Flow

```mermaid
graph LR
    Request[בקשת צפייה] --> Backend[Backend]
    Backend --> S3[S3 Generate URL]
    S3 --> URL[URL + Signature]
    URL --> Expiry[תוקף: 1 שעה]
    Expiry --> Browser[פתיחה בדפדפן]
    
    style URL fill:#28a745,color:#fff
    style Expiry fill:#ffc107
```

---

## 5️⃣ מחיקת מסמך - Delete Flow

### תרשים זרימה

```mermaid
sequenceDiagram
    participant U as משתמש
    participant List as DocumentsList
    participant Confirm as window.confirm
    participant API as API
    participant Backend as Backend
    participant Qdrant as Qdrant
    participant S3 as S3
    
    U->>List: לוחץ "🗑️ מחק"
    List->>Confirm: "האם אתה בטוח?"
    
    alt משתמש מאשר
        Confirm-->>List: true
        List->>API: deleteDocument(docId)
        API->>Backend: DELETE /documents/{id}
        
        Backend->>Qdrant: Delete embeddings
        Backend->>Backend: Set active=false (Soft delete)
        Backend->>S3: Delete file
        
        Backend-->>API: 200 OK
        API-->>List: success
        List->>List: loadDocuments() - רענון
        List-->>U: הצג: "✅ המסמך נמחק"
    else משתמש מבטל
        Confirm-->>List: false
        List-->>U: אין פעולה
    end
```

### Soft Delete vs Hard Delete

```mermaid
graph TB
    Delete[Delete Request] --> Check{Soft או Hard?}
    
    Check -->|Soft Delete| S1[Set active=false]
    S1 --> S2[שמור ב-DB]
    S2 --> S3[המסמך נשאר קיים]
    
    Check -->|Hard Delete| H1[Delete from Qdrant]
    H1 --> H2[Delete from S3]
    H2 --> H3[Delete from DB]
    
    style S1 fill:#ffc107
    style H1 fill:#dc3545,color:#fff
    style H2 fill:#dc3545,color:#fff
    style H3 fill:#dc3545,color:#fff
```

---

## 6️⃣ Component Hierarchy

```mermaid
graph TB
    Dashboard[Dashboard.js]
    
    subgraph "Documents Tab"
        DocsList[DocumentsList.js]
        Upload[UploadModal.js]
        Progress[ProgressBar.js]
    end
    
    subgraph "State Management"
        DocsState[documents[]]
        LoadingState[loading]
        PollingRef[pollingIntervalRef]
    end
    
    Dashboard --> DocsList
    DocsList --> Upload
    Upload --> Progress
    
    Dashboard --> DocsState
    Dashboard --> LoadingState
    Dashboard --> PollingRef
    
    DocsState --> DocsList
    LoadingState --> DocsList
    
    style Dashboard fill:#667eea,color:#fff
    style DocsState fill:#28a745,color:#fff
```

---

## 7️⃣ UI States

### Empty State

```
┌─────────────────────────────────────────┐
│                                          │
│              📄                          │
│                                          │
│       אין מסמכים עדיין                  │
│                                          │
│  העלה מסמכים כדי לבנות את מאגר הידע    │
│        של הצ'אט שלך                     │
│                                          │
│    [➕ העלה מסמך חדש]                   │
│                                          │
└─────────────────────────────────────────┘
```

### Processing State

```
┌─────────────────────────────────────────┐
│  מסמכים בעיבוד (2)                      │
│                                          │
│  📄 report.pdf                           │
│  ████████████░░░░░░░ 65%                │
│  יוצר embeddings... 65%                 │
│                                          │
│  📄 contract.pdf                         │
│  █████░░░░░░░░░░░░░ 30%                 │
│  מחלץ טקסט מהמסמך... 30%               │
└─────────────────────────────────────────┘
```

### Completed State

```
┌───────────────────────────────────────────────┐
│  מסמכים מעובדים (5)                          │
│                                                │
│  ┌────────────────────┐  ┌────────────────┐  │
│  │ 📄 report.pdf      │  │ 📄 invoice.pdf │  │
│  │ 2.5 MB             │  │ 1.2 MB         │  │
│  │                    │  │                │  │
│  │ ✓ מעובד            │  │ ✓ מעובד        │  │
│  │                    │  │                │  │
│  │ [👁️] [⬇️] [🗑️]   │  │ [👁️] [⬇️] [🗑️]│  │
│  └────────────────────┘  └────────────────┘  │
└───────────────────────────────────────────────┘
```

---

## 🔄 useEffect Dependencies

```javascript
// Polling Effect
useEffect(() => {
  const hasProcessing = documents.some(
    doc => doc.processingStatus === 'PROCESSING' 
        || doc.processingStatus === 'PENDING'
  );
  
  if (hasProcessing) {
    // Start polling
    intervalRef.current = setInterval(() => {
      loadDocuments(true); // silent
    }, 2000);
  } else {
    // Stop polling
    clearInterval(intervalRef.current);
  }
  
  return () => clearInterval(intervalRef.current);
}, [documents]);
```

---

## 📊 Performance Metrics

| מדד | ערך |
|-----|-----|
| **Polling Interval** | 2 שניות |
| **Max File Size** | 50 MB |
| **Concurrent Uploads** | ללא הגבלה (async) |
| **Processing Time** | 1-3 דקות (ממוצע) |
| **Presigned URL TTL** | 1 שעה |

---

[← חזרה ל-README הראשי](./README.md)
