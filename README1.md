# 💬 Custom Site Chat

### AI-powered chatbot platform for websites using RAG technology

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![License](https://img.shields.io/badge/license-Portfolio-green)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)
![React](https://img.shields.io/badge/React-18.2-blue)

---

## 🌐 Live Demo

**[https://custom-site-chat.com](https://custom-site-chat)**

Try it live! Upload your PDFs and create an intelligent chatbot in minutes.

---

## 📖 What is Custom Site Chat?

**Custom Site Chat** is a full-stack platform that transforms your PDF documents into an intelligent, conversational AI chatbot. Upload your documents, get an embed code, and add a smart chat widget to your website - no coding required!

### 🎯 Key Benefits

- 🚀 **5-Minute Setup** - Upload PDFs, copy code, paste into your website
- 🤖 **Powered by RAG** - Retrieval-Augmented Generation for accurate, source-based answers
- 🌍 **Multi-Language** - Supports Hebrew and English with automatic detection
- 🔒 **Secure & Isolated** - Each user gets their own private vector database collection
- 📱 **Embed Anywhere** - Works on any website with simple JavaScript snippet

---

## ✨ Features

### 🔐 User Management
- Multiple authentication methods (Email/Password, Google OAuth)
- Secure email verification
- Complete password recovery flow

### 📄 Document Processing
- PDF upload with real-time progress tracking
- Automatic text extraction and intelligent chunking
- AI-powered embeddings using OpenAI's text-embedding-3-large

### 💬 Smart Chat Widget
- Natural language queries in Hebrew or English
- Context-aware conversations (maintains chat history)
- Source citations with relevance scores
- Easy website integration with copy-paste code

### ⚙️ Collection Management
- Private Qdrant vector collection per user
- Secure API access with secret keys
- Customizable widget appearance

---

## 🏗️ System Architecture

```mermaid
graph TB
    subgraph Frontend[⚛️ Frontend Layer]
        Dashboard[📊 React Dashboard<br/>Document Management]
        Widget[💬 Chat Widget<br/>User Interface]
    end
    
    subgraph Backend[🚀 Backend API]
        Auth[🔐 Authentication<br/>JWT Tokens]
        DocService[📄 Document Service<br/>PDF Processing]
        QueryService[🔍 Query Service<br/>RAG Pipeline]
    end
    
    subgraph Storage[💾 Storage Layer]
        DB[(🗄️ PostgreSQL<br/>Users & Metadata)]
        S3[(☁️ AWS S3<br/>PDF Files)]
        Vector[(🧠 Qdrant<br/>Vector Search)]
    end
    
    subgraph AI[🤖 AI Services]
        EmbedAPI[OpenAI API<br/>text-embedding-3-large]
        GPT[OpenAI GPT-4<br/>Answer Generation]
    end
    
    Dashboard --> Auth
    Dashboard --> DocService
    Widget --> QueryService
    
    Auth --> DB
    DocService --> DB
    DocService --> S3
    DocService --> EmbedAPI
    DocService --> Vector
    
    QueryService --> DB
    QueryService --> EmbedAPI
    QueryService --> Vector
    QueryService --> GPT
    
    style Frontend fill:#dbeafe,stroke:#1e40af,stroke-width:3px
    style Backend fill:#dcfce7,stroke:#10b981,stroke-width:3px
    style Storage fill:#fef3c7,stroke:#f59e0b,stroke-width:3px
    style AI fill:#fce7f3,stroke:#ec4899,stroke-width:3px
    
    style DB fill:#e0e7ff,stroke:#6366f1,stroke-width:3px
    style Vector fill:#fce7f3,stroke:#ec4899,stroke-width:4px
    style S3 fill:#dbeafe,stroke:#0ea5e9,stroke-width:3px
```

---

## 🧠 How RAG Works

**RAG (Retrieval-Augmented Generation)** combines document search with AI generation for accurate, source-based answers.

```mermaid
flowchart LR
    subgraph Phase1[📥 Phase 1: Data Ingestion]
        PDF[📄 Upload PDF]
        S3[☁️ AWS S3<br/>File Storage]
        Text[📝 Extract Text]
        Chunks[✂️ Text Chunks]
        Embed1[🤖 OpenAI<br/>Embeddings]
        Qdrant1[(🧠 Qdrant<br/>Vector DB)]
        DB1[(🗄️ PostgreSQL<br/>Metadata)]
        
        PDF --> S3
        PDF --> Text
        Text --> Chunks
        Chunks --> Embed1
        Embed1 --> Qdrant1
        S3 --> DB1
        Chunks --> DB1
    end
    
    subgraph Phase2[🔍 Phase 2: Semantic Search]
        Question[❓ User Question]
        Embed2[🤖 OpenAI<br/>Query Embedding]
        Qdrant2[(🧠 Qdrant<br/>Search Vectors)]
        Top5[📊 Top 5 Results]
        
        Question --> Embed2
        Embed2 --> Qdrant2
        Qdrant2 --> Top5
    end
    
    subgraph Phase3[🤖 Phase 3: Answer Generation]
        Context[📝 Context Chunks]
        GPT4[🤖 OpenAI GPT-4<br/>Generate Answer]
        Answer[💬 AI Response]
        DB3[(🗄️ PostgreSQL<br/>Query Log)]
        
        Context --> GPT4
        GPT4 --> Answer
        Answer --> DB3
    end
    
    Phase1 --> Phase2
    Phase2 --> Phase3
    Top5 --> Context
    
    style Phase1 fill:#fef3c7,stroke:#f59e0b,stroke-width:4px
    style Phase2 fill:#dcfce7,stroke:#10b981,stroke-width:4px
    style Phase3 fill:#dbeafe,stroke:#1e40af,stroke-width:4px
    
    style Qdrant1 fill:#fce7f3,stroke:#ec4899,stroke-width:3px
    style Qdrant2 fill:#fce7f3,stroke:#ec4899,stroke-width:3px
    style DB1 fill:#e0e7ff,stroke:#6366f1,stroke-width:3px
    style DB3 fill:#e0e7ff,stroke:#6366f1,stroke-width:3px
```

**How it works:**
1. **Ingestion**: PDFs are uploaded, text is extracted, split into chunks, and converted to vector embeddings
2. **Search**: User questions are converted to embeddings and matched against stored vectors using semantic similarity
3. **Generation**: Top matching chunks are sent to GPT-4 as context to generate accurate, source-based answers

---

## 🚀 Tech Stack

### Backend
- **Spring Boot 3.3.4** - REST API framework
- **Java 21** - Programming language
- **PostgreSQL** - User accounts, metadata, logs
- **Qdrant** - Vector database for embeddings
- **AWS S3** - PDF file storage
- **OpenAI API** - Embeddings (text-embedding-3-large) and GPT-4

### Frontend
- **React 18.2** - User interface
- **React Router** - Client-side routing
- **Axios** - HTTP client

### Infrastructure
- **Docker & Docker Compose** - Containerization
- **Jenkins** - CI/CD automation
- **Nginx** - Reverse proxy
- **AWS EC2** - Cloud hosting

---

## 📚 Documentation

### 📖 Main Documentation
- **[User Guide](./USER-GUIDE.md)** - Complete user manual with screenshots
- **[Backend API](./backend/README.md)** - API documentation with examples
- **[Frontend](./frontend/README.md)** - Frontend architecture and setup

### 🏗️ Architecture & Deployment
- **[AWS Architecture](./docs/AWS-ARCHITECTURE.md)** - Cloud infrastructure setup
- **[CI/CD Pipeline](./docs/CICD-PIPELINE.md)** - Automated deployment process
- **[RAG Explanation](./docs/RAG-EXPLANATION.md)** - Deep dive into RAG implementation

---

## 🚀 Quick Start

### Prerequisites
- Java 21
- Node.js 18+
- Docker & Docker Compose
- PostgreSQL
- Qdrant
- OpenAI API key
- AWS S3 bucket

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/custom-site-chat.git
cd custom-site-chat
```

2. **Configure environment**
```bash
# Backend
cd backend
cp .env.example .env
# Edit .env with your credentials

# Frontend
cd ../frontend
cp .env.example .env
# Edit .env with your credentials
```

3. **Start services with Docker**
```bash
docker-compose up -d
```

4. **Access the application**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui.html

---

## 📊 Project Statistics

- **Total Lines of Code**: ~15,000+
- **API Endpoints**: 20+
- **Database Tables**: 6
- **Processing Stages**: 7
- **Supported Languages**: 2 (Hebrew, English)
- **Max File Size**: 50 MB
- **Vector Dimensions**: 3072

---

## 📄 License

This project is created for portfolio purposes.

---

## 📧 Contact

**Ester Ovrani**

- 📧 Email: ester.ovrani@gmail.com
- 💼 Portfolio: [custom-site-chat.com](https://custom-site-chat.com)
- 🐙 GitHub: [Your GitHub Profile](https://github.com/EsterOvrani)

---

## 🌟 Acknowledgments

- **OpenAI** for GPT-4 and text-embedding-3-large models
- **Qdrant** for high-performance vector search
- **Spring Boot** community for excellent documentation
- **React** team for the amazing UI library

---

<div align="center">
    <a href="https://custom-site-chat.com">🌐 Visit Live Site</a> •
    <a href="./USER-GUIDE.md">📖 User Guide</a> •
    <a href="./backend/README.md">🔧 API Docs</a>
  </p>
</div>
