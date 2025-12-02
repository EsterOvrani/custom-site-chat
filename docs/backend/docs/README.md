# API Documentation Index

Welcome to the Custom Site Chat API documentation!

---

## 📚 Available Documentation

### [🔐 Authentication API](./AUTH_API.md)
Complete authentication and user management flows:
- Google OAuth login
- Email/Password authentication
- Registration & verification
- Password reset
- Status checks

**12 endpoints** | **Public & Authenticated**

---

### [👤 User API](./USER_API.md)
User profile and management:
- Get current user
- List all users (admin)

**2 endpoints** | **Authenticated**

---

### [📦 Collection API](./COLLECTION_API.md)
Qdrant collection and embed code management:
- Get collection info
- Regenerate secret key
- Get widget embed code

**3 endpoints** | **Authenticated**

---

### [📄 Document API](./DOCUMENT_API.md)
PDF document upload and management:
- Upload documents (async processing)
- List/Get documents
- Download/View
- Delete operations
- Reorder

**9 endpoints** | **Authenticated**

---

### [💬 Query API](./QUERY_API.md)
Public chat interface for document queries:
- Ask questions with AI
- Conversation history support
- Multi-language support
- Source citations

**1 endpoint** | **Public (Secret Key)**

---

## 🚀 Quick Links

- [← Back to Main README](../README.md)
- [Project Structure](../README.md#-project-structure)
- [Environment Configuration](../README.md#-environment-configuration)
- [Technology Stack](../README.md#-technology-stack)

---

## 📊 API Summary

| Module | Endpoints | Auth Required | Public Access |
|--------|-----------|---------------|---------------|
| Authentication | 12 | Mixed | ✅ Most public |
| User | 2 | ✅ JWT | ❌ |
| Collection | 3 | ✅ JWT | ❌ |
| Document | 9 | ✅ JWT | ❌ |
| Query | 1 | Secret Key | ✅ Public |
| **Total** | **27** | - | - |

---

## 🔑 Authentication Types

### JWT Bearer Token
Used for user-authenticated endpoints:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Secret Key
Used for public Query API:
```json
{
  "secretKey": "sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
}
```

---

## 📖 How to Use This Documentation

1. **Start with Authentication** - Learn how to register/login users
2. **Understand Collections** - Each user gets a Qdrant collection
3. **Upload Documents** - Process PDFs with async handling
4. **Query Documents** - Use the public API to ask questions

---

## 🛠 Common Workflows

### New User Registration Flow
1. `POST /auth/signup` - Create account
2. `POST /auth/verify` - Verify email
3. `GET /api/collection/info` - Auto-create collection
4. `POST /api/documents/upload` - Upload first document

### Document Query Flow
1. Get secret key from `GET /api/collection/info`
2. Embed widget with secret key
3. Use `POST /api/query/ask` to ask questions
4. Receive AI-generated answers with sources

---

**Last Updated:** 2025-01-15
