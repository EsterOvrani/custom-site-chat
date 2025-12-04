# 💬 Custom Site Chat

### Custom AI chatbot platform for websites powered by RAG technology

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![License](https://img.shields.io/badge/license-Portfolio-green)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)
![React](https://img.shields.io/badge/React-18.2-blue)

---

## 🌐 Live Website

**[https://custom-site-chat.com](https://custom-site-chat.com)**

---

## 📖 Project Description

**Custom Site Chat** Custom Site Chat is a comprehensive full-stack platform that enables users to create AI-powered chatbots for their websites based on their own documents. Users can upload PDF documents, and the system automatically processes them using advanced AI embeddings and vector search technology. The generated chatbot can then be easily embedded into any website using a simple JavaScript snippet.

### 🎯 Project Goals

- **Enable Easy AI Chatbot Creation** - Allow users to build intelligent chatbots without coding knowledge.
- **Leverage Existing Documents** - Transform static PDF files into interactive, queryable knowledge bases.
- **Provide Seamless Website Integration** - Offer simple embed code for instant chatbot deployment.
- **Ensure Scalability & Security** - Support multiple users with isolated data and secure authentication.
- **Deliver Natural Conversations** - Use advanced AI for context-aware, multi-language responses.

---

## ✨ Key Features

### 🔐 User Management
- **Multiple Authentication Methods**: Email/password and Google OAuth 2.0
- **Email Verification**: Secure account activation with 6-digit verification codes
- **Password Recovery**: Complete password reset flow with email verification
- **JWT-Based Sessions**: Secure, stateless authentication

### 📄 Document Processing
- **PDF Upload & Processing**: Asynchronous document handling with real-time progress tracking
- **Multi-Stage Processing Pipeline**:
  - File upload to AWS S3
  - Text extraction using Apache PDFBox
  - Intelligent chunking (500 characters with 50-character overlap)
  - AI embeddings generation using OpenAI's text-embedding-3-large
  - Vector storage in Qdrant database
- **Progress Monitoring**: 7-stage processing with live progress updates
- **Multiple Documents**: Support for unlimited document uploads per user

### 💬 AI-Powered Chat Widget
- **Natural Language Queries**: Ask questions in Hebrew or English
- **Context-Aware Responses**: Maintains conversation history (up to 10 messages)
- **Source Citations**: Provides relevant document excerpts with relevance scores
- **Easy Integration**: Simple copy-paste JavaScript embed code
- **Customizable UI**: Configure widget title, bot name, and avatars

### ⚙️ Collection Management
- **Isolated User Collections**: Each user gets a dedicated Qdrant collection
- **Secure API Access**: Secret key-based authentication for widget API
- **Embeddable Widget**: Pre-generated, customizable embed code
- **Key Regeneration**: Security-focused secret key rotation

## 🚀 Technologies

### Backend
- **Spring Boot 3.3.4** - Main framework for building REST APIs
- **Java 21** - Programming language
- **Spring Security + JWT** - Authentication and authorization
- **Spring Data JPA** - Database management with ORM
- **PostgreSQL** - Relational database for user accounts and metadata
- **Qdrant** - Vector database for storing and searching document embeddings
- **OpenAI API** - AI models for generating embeddings (text-embedding-3-large) and chat responses (GPT-4)
- **AWS S3** - Cloud object storage for uploaded PDF files
- **BCrypt** - Password hashing algorithm
- **Apache PDFBox** - PDF text extraction library
- **Maven** - Dependency management and build automation

### Frontend
- **React 18.2.0** - JavaScript library for building user interfaces
- **React Router DOM 6.20.0** - Client-side routing for SPA navigation
- **Axios 1.12.2** - Promise-based HTTP client for API requests
- **CSS3** - Modern and responsive design

### DevOps & Infrastructure
- **Docker** - Containerization platform for consistent deployment
- **Docker Compose** - Multi-container orchestration for development and production
- **Jenkins** - CI/CD automation server for building, testing, and deploying
- **Nginx** - Reverse proxy server and static file serving
- **AWS EC2** - Cloud compute service for hosting the application
- **Newman** - Automated API testing using Postman collections

### Authentication & Integration
- **Google OAuth 2.0** - Third-party authentication via Google accounts
- **SMTP (Gmail)** - Email service for verification codes and password reset

---

## 🏗️ Architecture

### CI/CD Architecture

![CI/CD Architecture](resorces/arcitecture/cicd-architecture.png)

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

### System Flow

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant N as Nginx
    participant B as Backend
    participant DB as PostgreSQL
    participant S3 as AWS S3
    participant Q as Qdrant
    participant AI as OpenAI

    U->>F: Upload PDF Document
    F->>N: POST /api/documents/upload
    N->>B: Forward Request
    B->>DB: Save Document Metadata (PENDING)
    B-->>F: Return Document ID
    
    Note over B,S3: Async Processing Begins
    B->>S3: Upload PDF File
    B->>B: Extract Text with PDFBox
    B->>B: Create Text Chunks (500 chars)
    
    loop For Each Chunk
        B->>AI: Generate Embedding (3072 dims)
        AI-->>B: Return Embedding Vector
        B->>Q: Store Embedding + Metadata
    end
    
    B->>DB: Update Status to COMPLETED
    
    Note over U,AI: Query Flow
    U->>F: Ask Question via Widget
    F->>N: POST /api/query/ask
    N->>B: Forward with Secret Key
    B->>DB: Validate Secret Key
    B->>AI: Generate Query Embedding
    B->>Q: Semantic Search (top 5)
    Q-->>B: Return Relevant Chunks
    B->>AI: Generate Answer with GPT-4
    AI-->>B: Return Answer
    B-->>F: Return Answer + Sources
    F-->>U: Display Answer

```

## 🚢 Deployment

### Production Deployment (Docker)

```bash
# Build production images
docker-compose -f docker-compose.yml build --no-cache

# Push to registry
docker tag backend-prod:latest your-registry.com/custom-site-chat-backend:latest
docker push your-registry.com/custom-site-chat-backend:latest

docker tag frontend-prod:latest your-registry.com/custom-site-chat-frontend:latest
docker push your-registry.com/custom-site-chat-frontend:latest

# Deploy on production server
docker-compose -f docker-compose.yml up -d
```

### CI/CD with Jenkins

The project includes a `Jenkinsfile` that:
1. Runs Newman API tests in isolated environment
2. Builds production Docker images
3. Tags with Git commit message
4. Pushes to Docker registry

**Manual trigger:**
```bash
# On Jenkins server
curl -X POST http://jenkins:8080/job/custom-site-chat/build \
  --user admin:token
```

### Environment Variables for Production

```bash
# Use strong secrets
JWT_SECRET_KEY=$(openssl rand -base64 64)
POSTGRES_PASSWORD=$(openssl rand -base64 32)

# Disable test mode
TEST_MODE_ENABLED=false
BYPASS_EMAIL_VERIFICATION=false

# Use production URLs
FRONTEND_URL=https://your-domain.com
AWS_REGION=eu-west-1

# Enable HTTPS
NGINX_PORT=443
```

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

**Coding Standards:**
- Backend: Follow Spring Boot best practices, use Lombok
- Frontend: ESLint with Airbnb style guide
- Commits: Use conventional commits (feat, fix, docs, etc.)

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 📧 Contact

**Project Maintainer:** Your Name

- 📧 Email: your-email@example.com
- 🐙 GitHub: [@yourusername](https://github.com/yourusername)
- 💼 LinkedIn: [Your LinkedIn](https://linkedin.com/in/yourprofile)

**Support:**
- 🐛 Report bugs: [GitHub Issues](https://github.com/yourusername/custom-site-chat/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/yourusername/custom-site-chat/discussions)
- 📖 Documentation: [Wiki](https://github.com/yourusername/custom-site-chat/wiki)

---

## 🌟 Acknowledgments

- **OpenAI** for GPT-4 and text-embedding-3-large models
- **Qdrant** for high-performance vector search
- **Spring Boot** community for excellent documentation
- **React** team for the amazing UI framework

---

**Version:** 1.0.0  
**Last Updated:** 2025-01-15  
**Status:** Production Ready ✅

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
