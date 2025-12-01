# 📄 Document API Documentation

[← Back to Main README](../README.md)

---

## Overview

The Document module handles PDF document upload, processing, storage, and management. Documents are processed asynchronously with real-time progress tracking.

**Base Path:** `/api/documents`

**Authentication:** Required (JWT Bearer token)

---

## Table of Contents

1. [Upload Document](#1-upload-document)
2. [Get My Documents](#2-get-my-documents)
3. [Get Single Document](#3-get-single-document)
4. [Download Document](#4-download-document)
5. [View Document (Inline)](#5-view-document-inline)
6. [Get Presigned Download URL](#6-get-presigned-download-url)
7. [Delete Document](#7-delete-document)
8. [Delete All Documents](#8-delete-all-documents)
9. [Reorder Documents](#9-reorder-documents)

---

## 1. Upload Document

**Endpoint:** `POST /api/documents/upload`

**Authentication:** Required

**Content-Type:** `multipart/form-data`

### Flow Diagram

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant Async as Async Processor
    participant S3 as S3 Storage
    participant Q as Qdrant
    participant AI as OpenAI

    C->>S: POST /api/documents/upload (PDF file)
    S->>S: Read file to memory (byte[])
    S->>S: Calculate SHA-256 hash
    S->>S: Create Document entity (status=PENDING)
    S->>DB: Save document
    DB-->>S: Document saved (ID assigned)
    S-->>C: 200 OK + Document (immediately)
    
    Note over Async: 🔄 Background Processing
    
    Async->>DB: Update: UPLOADING (10%)
    Async->>S3: Upload file
    Async->>DB: Update: UPLOADING (20%)
    
    Async->>DB: Update: EXTRACTING_TEXT (30%)
    Async->>Async: Extract text with PDFBox
    Async->>DB: Update: EXTRACTING_TEXT (45%)
    
    Async->>DB: Update: CREATING_CHUNKS (50%)
    Async->>Async: Split into chunks
    Async->>DB: Update: CREATING_CHUNKS (60%)
    
    Async->>DB: Update: CREATING_EMBEDDINGS (65%)
    loop For each chunk
        Async->>AI: Generate embedding
        AI-->>Async: Embedding vector
        Async->>Q: Store in Qdrant
        Async->>DB: Update progress (65-95%)
    end
    
    Async->>DB: Update: COMPLETED (100%)
```

### Request

```http
POST /api/documents/upload
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: multipart/form-data

file: [binary PDF data]
```

### Response (Immediate)

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

### Processing Stages

| Stage | Progress % | Description |
|-------|-----------|-------------|
| `UPLOADING` | 10-20% | מעלה לשרת... |
| `EXTRACTING_TEXT` | 30-45% | מחלץ טקסט מהמסמך... |
| `CREATING_CHUNKS` | 50-60% | מחלק לחלקים... |
| `CREATING_EMBEDDINGS` | 65-95% | יוצר embeddings... |
| `STORING` | 65-95% | שומר במאגר... |
| `COMPLETED` | 100% | הושלם בהצלחה |
| `FAILED` | - | נכשל |

---

## 2. Get My Documents

**Endpoint:** `GET /api/documents/my-documents`

**Authentication:** Required

### Request

```http
GET /api/documents/my-documents
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

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
      "originalFileName": "report.pdf",
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

## 3. Get Single Document

**Endpoint:** `GET /api/documents/{id}`

**Authentication:** Required

### Request

```http
GET /api/documents/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

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

## 4. Download Document

**Endpoint:** `GET /api/documents/{id}/download`

**Authentication:** Required

### Request

```http
GET /api/documents/1/download
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="contract.pdf"

[Binary PDF data]
```

---

## 5. View Document (Inline)

**Endpoint:** `GET /api/documents/{id}/view`

**Authentication:** Required

Opens PDF in browser instead of downloading.

### Request

```http
GET /api/documents/1/view
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: inline; filename="contract.pdf"

[Binary PDF data - opens in browser]
```

---

## 6. Get Presigned Download URL

**Endpoint:** `GET /api/documents/{id}/download-url`

**Authentication:** Required

Generates a temporary S3 URL (expires in 1 hour).

### Request

```http
GET /api/documents/1/download-url
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

```json
{
  "success": true,
  "url": "https://s3.eu-west-1.amazonaws.com/bucket/users/1/documents/1705315200000_contract.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...",
  "expiresIn": 3600
}
```

---

## 7. Delete Document

**Endpoint:** `DELETE /api/documents/{id}`

**Authentication:** Required

Soft-deletes the document (sets `active=false`).

### Flow Diagram

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Server
    participant DB as Database
    participant Q as Qdrant
    participant S3 as S3

    C->>S: DELETE /api/documents/1
    S->>DB: Find document
    alt Not found or not owner
        S-->>C: 404 or 403
    else Owner
        S->>Q: Delete embeddings
        S->>DB: Set active=false
        S->>S3: Delete file
        S-->>C: 200 OK
    end
```

### Request

```http
DELETE /api/documents/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

```json
{
  "success": true,
  "message": "מסמך נמחק בהצלחה"
}
```

---

## 8. Delete All Documents

**Endpoint:** `DELETE /api/documents/delete-all`

**Authentication:** Required

Deletes all documents and resets the collection.

### Request

```http
DELETE /api/documents/delete-all
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response

```json
{
  "success": true,
  "message": "כל המסמכים נמחקו בהצלחה",
  "deletedCount": 5
}
```

---

## 9. Reorder Documents

**Endpoint:** `PUT /api/documents/reorder`

**Authentication:** Required

Updates the display order of documents.

### Request

```json
PUT /api/documents/reorder
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "documentIds": [3, 1, 2]
}
```

### Response

```json
{
  "success": true,
  "message": "סדר המסמכים עודכן בהצלחה"
}
```

---

## Common Errors

### 404 Not Found

```json
{
  "success": false,
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "מסמך '1' לא נמצא",
  "timestamp": "2025-01-15T10:30:00"
}
```

### 403 Forbidden

```json
{
  "success": false,
  "errorCode": "UNAUTHORIZED",
  "message": "אין לך הרשאה לגשת למסמך עם ID 1",
  "timestamp": "2025-01-15T10:30:00"
}
```

### 400 File Too Large

```json
{
  "success": false,
  "errorCode": "FILE_TOO_LARGE",
  "message": "הקובץ גדול מדי. גודל מקסימלי: 10MB",
  "timestamp": "2025-01-15T10:30:00"
}
```

---

[← Back to Main README](../README.md)
