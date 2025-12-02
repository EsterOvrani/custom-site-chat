# 🤖 Smart Document Chat

### AI-Powered Document Chat Platform with RAG Technology

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![License](https://img.shields.io/badge/license-Portfolio-green)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)
![React](https://img.shields.io/badge/React-18.2-blue)

---

## 🌐 Live Website

**[https://smart-document-chat.com](https://smart-document-chat.com)**

---

## 📖 Project Description

**Smart Document Chat** is an advanced platform that enables users to conduct intelligent conversations with documents using AI and RAG (Retrieval-Augmented Generation) technology. The system uploads documents, analyzes their content, and allows users to ask questions and receive accurate answers based on document content.

### 🎯 Project Goals

Creating an advanced and accessible solution for document information management through:
- Smart document processing with AI
- Advanced semantic search
- Intuitive user interface
- Secure and scalable cloud infrastructure

---

## ✨ Key Features

### 🔐 Security and Authentication
- **Secure login** with JWT Tokens
- **Google OAuth 2.0 authentication** for quick login
- **Email verification** for enhanced security
- **Role-based permissions**

### 📄 Document Management
- **Document upload** - Support for PDF files (up to 50MB)
- **Automatic processing** - Chunking and embedding generation
- **Cloud storage** - Secure storage in AWS S3
- **Status tracking** - Real-time processing status monitoring with 7 stages

### 💬 Smart Conversations
- **RAG (Retrieval-Augmented Generation)** - Accurate answers from documents
- **Context preservation** - System remembers conversation history (up to 10 messages)
- **Confidence score** - Confidence rating for each answer
- **Sources** - Display exact sources from documents
- **Multilingual support** - Answers in question language (Hebrew/English)

### ⚡ Performance
- **Async Processing** - Background document processing
- **Real-time Updates** - Live progress tracking
- **Optimized Search** - HNSW vector search in Qdrant
- **Smart Caching** - Efficient data loading

### 🎨 User Experience
- **Intuitive interface** - Clean and simple design
- **Responsive Design** - Adapts to all screen sizes
- **Real-time notifications** - Updates on system operations
- **Multiple chat management** - Ability to manage multiple chats simultaneously

---

## 🚀 Technologies

### Backend
- **Spring Boot 3.3.4** - Main framework
- **Java 21** - Programming language
- **Spring Security + JWT** - Authentication and authorization
- **Spring Data JPA** - Database management
- **PostgreSQL** - Relational database
- **LangChain4j** - Integration with OpenAI and NLP
- **Qdrant** - Vector Database for semantic search
- **AWS S3** - Cloud document storage
- **Apache PDFBox** - PDF file processing
- **MapStruct** - Automatic object mapping
- **Lombok** - Reduce boilerplate code

### Frontend
- **React 18.2** - JavaScript library for building UI
- **React Router** - Navigation between pages
- **Axios** - HTTP communication with Backend
- **CSS3** - Modern and responsive design

### DevOps & Infrastructure
- **Docker & Docker Compose** - Containerization
- **Jenkins** - Automated CI/CD
- **Nginx** - Reverse proxy and Load balancing
- **Let's Encrypt** - SSL certificates
- **AWS EC2** - Cloud servers
- **Newman** - Automated API testing

### Authentication & Integration
- **Google OAuth 2.0** - Google account login
- **JavaMail** - Email sending (user verification)

---

## 🏗️ Architecture

### CI/CD Architecture

![CI/CD Architecture](resources/architecture/cicd-architecture.png)

**CI/CD Process:**
1. **Push to GitHub** - Developer uploads new code
2. **Jenkins Webhook** - Automatically triggers Pipeline
3. **Build & Test** - Build test environment and run Newman tests
4. **Quality Gate** - If tests pass, continue to production
5. **Production Build** - Build clean images without TEST_MODE
6. **Tagging** - Tag with commit message and version
7. **Push to Registry** - Upload to Docker Hub
8. **Manual Deploy** - Pull new images to AWS

---

### AWS Architecture

![AWS Architecture](resources/architecture/aws-architecture.png)

**AWS Components:**

1. **EC2 Instance** - Virtual server for running the application
2. **Route 53** - DNS management for smart-document-chat.com domain
3. **S3 Bucket** - Storage for user documents
4. **Security Groups** - Firewall for server protection
5. **Elastic IP** - Fixed IP address for server
6. **Let's Encrypt** - Free SSL certificates with automatic renewal

**Traffic Flow:**
1. User accesses https://smart-document-chat.com
2. Nginx receives request and performs SSL termination
3. API requests are forwarded to Backend (Spring Boot)
4. Frontend requests are forwarded to React Container
5. Backend communicates with PostgreSQL, Qdrant, and S3

---

### System Flow Diagram

![System Flow Chart](resources/architecture/system-flow.png)

**Flow Description:**

1. **Registration and Verification:**
   - User registers and receives verification code via email
   - Code verification enables login

2. **Login:**
   - Username/Password or Google OAuth
   - Receive JWT Token for future authentication

3. **Upload Documents:**
   - Upload PDF files (up to 50MB each)
   - Automatic background processing with 7 stages
   - Real-time progress tracking

4. **Questions and Answers:**
   - Ask questions from documents
   - Semantic search in Qdrant
   - Receive AI-generated answers with sources

5. **Document Management:**
   - View documents
   - Download documents
   - Delete documents

---

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- AWS Account (for S3)
- OpenAI API Key
- Google OAuth Client ID (optional)

### Installation

1. **Clone the repository:**
```bash
   git clone https://github.com/yourusername/smart-document-chat.git
   cd smart-document-chat
```

2. **Configure environment variables:**
```bash
   cp .env.example .env
   # Edit .env with your credentials
```

3. **Start services:**
```bash
   docker-compose up -d
```

4. **Access the application:**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080
   - API Documentation: See [Backend README](./backend/README.md)

---

## 👥 User Guide

### Getting Started

#### 1. Registration

1. Navigate to the registration page
2. Fill in your details:
   - First Name & Last Name
   - Username (unique)
   - Email
   - Password
3. Click "Register" or use "Sign up with Google"
4. Check your email for verification code (6 digits)
5. Enter the code to activate your account

#### 2. Upload Documents

1. Login to your dashboard
2. Click "📄 My Documents" tab
3. Click "➕ Upload New Document"
4. Select one or more PDF files (max 50MB each)
5. Wait for processing (7 stages):
   - ⬆️ Uploading (10-20%)
   - 📄 Extracting text (30-45%)
   - ✂️ Creating chunks (50-60%)
   - 🧠 Creating embeddings (65-95%)
   - ✅ Completed (100%)

#### 3. Embed Chat Widget

1. Go to "⚙️ Embed Code & Settings" tab
2. Copy your Secret Key (keep it safe!)
3. Copy the Embed Code
4. Paste it in your website before `</body>` tag
5. Customize (optional):
```javascript
   window.CHAT_WIDGET_TITLE = 'My Company Support';
   window.CHAT_WIDGET_BOT_NAME = 'Assistant';
```

#### 4. Using the Chat Widget

1. A chat bubble appears on your website
2. Click to open the chat window
3. Ask questions about your documents
4. The AI will:
   - Search relevant content
   - Generate accurate answers
   - Show confidence score
   - Display sources from documents
5. Chat history is saved (up to 10 messages)
6. Click "🔄 Start New Chat" to reset

### Tips for Best Results

✅ **Do:**
- Upload clear, text-based PDFs
- Ask specific questions
- Use natural language
- Reference context from previous messages

❌ **Don't:**
- Upload scanned images without OCR
- Ask multiple questions at once
- Expect answers outside document content

---

## 📊 API Documentation

Complete API documentation with flow diagrams is available:

- **[Backend API Documentation](./backend/README.md)** - Full REST API reference
- **[Authentication API](./backend/docs/AUTH_API.md)** - Login, register, OAuth
- **[Collection API](./backend/docs/COLLECTION_API.md)** - Secret keys, embed code
- **[Document API](./backend/docs/DOCUMENT_API.md)** - Upload, manage documents
- **[Query API](./backend/docs/QUERY_API.md)** - Ask questions, get answers

---

## 🛠️ Development

### Project Structure
```
smart-document-chat/
├── backend/                # Spring Boot API
│   ├── src/
│   ├── docs/              # API Documentation
│   └── README.md          # Backend documentation
├── frontend/              # React UI
│   ├── src/
│   └── public/
├── nginx/                 # Reverse proxy config
├── docker-compose.yml     # Production services
├── docker-compose.test.yml # Test environment
├── Jenkinsfile           # CI/CD pipeline
└── README.md             # This file
```

### Running Tests
```bash
# Backend tests
cd backend
./mvnw test

# API tests with Newman
docker-compose -f docker-compose.test.yml up newman
```

### Building for Production
```bash
# Build all services
docker-compose build

# Or build individually
cd backend && docker build -t backend-prod .
cd frontend && docker build -t frontend-prod .
```

---

## 📈 Performance Metrics

| Metric | Value |
|--------|-------|
| PDF Upload | Up to 50MB per file |
| Processing Time | 2-3 minutes per 10MB |
| Query Response | < 2 seconds |
| Embedding Dimensions | 3,072 (text-embedding-3-large) |
| Max Relevant Chunks | 5 per query |
| Max Chat History | 10 messages |
| Vector Search | HNSW (m=16, ef=128) |

---

## 🔒 Security Features

- ✅ JWT-based authentication
- ✅ Email verification required
- ✅ Password hashing (BCrypt)
- ✅ Google OAuth 2.0 support
- ✅ User-isolated collections
- ✅ Secret keys for public API
- ✅ Soft-delete architecture
- ✅ CORS protection
- ✅ SQL injection prevention (JPA)

---

## 📧 Contact

**Ester Ovrani**
- 📧 Email: ester.ovrani@gmail.com
- 💼 Portfolio: [smart-document-chat.com](https://smart-document-chat.com)
- 🔗 LinkedIn: [Your LinkedIn Profile]
- 🐙 GitHub: [Your GitHub Profile]

---

## 📄 License

This project is created for portfolio purposes.

---

<div align="center">
  <p><strong>Built with ❤️ by Ester Ovrani</strong></p>
  <p>
    <a href="https://smart-document-chat.com">🌐 Visit Live Site</a>
  </p>
</div>