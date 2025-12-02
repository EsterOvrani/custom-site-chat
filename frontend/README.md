# Frontend Documentation

> Custom Site Chat - React-based document management and AI chat platform

## 📋 Overview

This is the frontend application for Custom Site Chat, a platform that allows users to upload documents, manage collections, and embed AI-powered chat widgets on their websites.

**Live Site:** [custom-site-chat.com](https://custom-site-chat.com)

---

## 🏗️ Tech Stack

- **Framework:** React 18.2.0
- **Routing:** React Router DOM
- **HTTP Client:** Axios with interceptors
- **State Management:** React Hooks (useState, useEffect, useRef)
- **Authentication:** JWT + Google OAuth
- **Styling:** Custom CSS with RTL support
- **Language:** Hebrew (RTL interface)

---

## 🎯 Key Features

### 1. **User Authentication**
- Email/Password registration and login
- Google OAuth integration
- Email verification with 6-digit codes
- Password reset flow
- JWT-based session management

### 2. **Document Management**
- Multi-file upload (up to 50 MB per file)
- Real-time processing status with polling
- Document preview and download
- Soft delete with recovery option
- Processing pipeline: Upload → Extract → Chunk → Embeddings → Complete

### 3. **Collection Settings**
- Auto-generated collection names (`user_{userId}_{uuid8}`)
- Secret key management (`sk_{uuid32}`)
- Regenerate API keys with warnings
- Copy embed code with one click

### 4. **Embeddable Chat Widget**
- Standalone JavaScript widget (`/public/chat-widget.js`)
- Customizable appearance (title, bot name, avatars)
- RTL/LTR auto-detection based on language
- Chat history with 10-message limit
- Reset conversation functionality

---

## 📁 Project Structure

```
frontend/
├── public/
│   ├── chat-widget.js          # Embeddable chat widget
│   └── index.html
├── src/
│   ├── components/
│   │   ├── Auth/              # Login, Register, Verify, ForgotPassword
│   │   ├── Documents/         # Upload, List, Progress tracking
│   │   ├── Settings/          # Collection settings, API keys
│   │   └── Common/            # Shared UI components
│   ├── services/
│   │   └── api.js             # Axios instance with interceptors
│   ├── App.js                 # Main app with routing
│   └── index.js               # Entry point
├── docs/                       # Detailed Hebrew documentation
│   ├── AUTH_FLOW.md
│   ├── DOCUMENTS_FLOW.md
│   ├── SETTINGS_FLOW.md
│   └── WIDGET_FLOW.md
├── package.json
└── README.md                   # This file
```

---

## 🚀 Getting Started

### Prerequisites
- Node.js 16+ and npm
- Backend API running (see backend documentation)

### Installation

```bash
# Clone the repository
git clone <repository-url>
cd frontend

# Install dependencies
npm install

# Create .env file
cp .env.example .env
```

### Environment Variables

```env
REACT_APP_API_URL=http://localhost:5000
REACT_APP_GOOGLE_CLIENT_ID=your-google-client-id
```

### Development

```bash
# Start development server
npm start

# Open browser at http://localhost:3000
```

### Production Build

```bash
# Build for production
npm run build

# Serve static files from build/ directory
```

---

## 🔐 Authentication Flow

1. **Registration:**
   - User registers with email/password or Google OAuth
   - Email verification code sent (15-minute expiry)
   - User verifies with 6-digit code
   - Account activated

2. **Login:**
   - User logs in with email/password or Google OAuth
   - JWT token stored in LocalStorage
   - Token included in all API requests via Axios interceptors
   - Auto-logout on 401 responses

3. **Password Reset:**
   - User requests reset code via email
   - 6-digit code sent (15-minute expiry)
   - User enters code and new password
   - Password updated

---

## 📄 Document Processing Pipeline

```
Upload → Pending → Uploading (10-20%) → Extracting (30-45%) → 
Chunking (50-60%) → Embeddings (65-95%) → Storing (65-95%) → Completed (100%)
```

**Features:**
- Real-time progress tracking with 2-second polling
- Visual progress bar with stage-specific icons
- Placeholder system for async uploads
- Download via presigned URLs (1-hour expiry)
- Soft delete (sets `active=false`)

---

## ⚙️ Collection Settings

Each user has a collection with:
- **Collection Name:** `user_{userId}_{uuid8}`
- **Secret Key:** `sk_{uuid32}` (for widget authentication)
- **Embed Code:** Pre-configured HTML snippet

**Actions:**
- Regenerate secret key (invalidates old key with warning)
- Copy secret key to clipboard
- Copy embed code to clipboard

---

## 💬 Chat Widget Integration

### Embed Code Example

```html
<script>
(function() {
  var script = document.createElement('script');
  script.src = 'https://custom-site-chat.com/chat-widget.js';
  script.async = true;
  script.onload = function() {
    window.ChatWidget.init({
      secretKey: 'sk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
      apiUrl: 'https://api.custom-site-chat.com',
      title: 'Chat with us',
      botName: 'Assistant',
      userAvatar: '👤',
      botAvatar: '🤖'
    });
  };
  document.body.appendChild(script);
})();
</script>
```

### Widget Features
- Toggle button (bottom-right corner)
- Chat interface with message history
- RTL/LTR support based on content language
- 10-message history limit with warning
- Reset conversation option

---

## 🎨 Design System

### Colors
- **Primary:** `#667eea` (blue-purple)
- **Secondary:** `#764ba2` (dark purple)
- **Success:** `#28a745` (green)
- **Warning:** `#ffc107` (yellow)
- **Error:** `#dc3545` (red)

### Gradients
- **Primary Gradient:** `linear-gradient(135deg, #667eea 0%, #764ba2 100%)`

### Typography
- **Font Family:** 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif
- **Direction:** RTL (Hebrew)

### Responsive Design
- **Mobile:** < 768px
- **Tablet:** 768px - 1024px
- **Desktop:** > 1024px

---

## 📚 Detailed Documentation

For in-depth implementation details and flow diagrams, see the `docs/` directory:

- **[Authentication Flow](docs/AUTH_FLOW.md)** - Login, registration, OAuth, password reset
- **[Document Management](docs/DOCUMENTS_FLOW.md)** - Upload, processing, download, delete
- **[Settings](docs/SETTINGS_FLOW.md)** - Collection settings, API keys, embed code
- **[Chat Widget](docs/WIDGET_FLOW.md)** - Widget initialization, chat flow, customization

Each document contains:
- Detailed flow diagrams (Mermaid)
- UI state visualizations
- Component relationships
- Error handling
- Troubleshooting guides

---

## 🐛 Common Issues

### Token Expiration
**Problem:** User logged out unexpectedly  
**Solution:** JWT tokens expire after X hours. Implement refresh token logic or extend expiry time.

### CORS Errors
**Problem:** API requests blocked by browser  
**Solution:** Ensure backend CORS settings allow frontend origin.

### Google OAuth Not Working
**Problem:** Google login button not appearing  
**Solution:** Verify `REACT_APP_GOOGLE_CLIENT_ID` in `.env` and Google Console settings.

### Document Upload Fails
**Problem:** Large files not uploading  
**Solution:** Check file size (max 50 MB) and backend upload limits.

---

## 🔧 Development Tools

- **React DevTools:** Browser extension for debugging
- **Network Tab:** Monitor API requests and responses
- **LocalStorage Inspector:** View stored JWT tokens and settings

---

## 📦 Deployment

### Docker Deployment

```dockerfile
FROM node:16-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npx", "serve", "-s", "build", "-l", "3000"]
```

### Build Command
```bash
docker build -t custom-site-chat-frontend .
docker run -p 3000:3000 custom-site-chat-frontend
```

---

## 📄 License

[Add your license here]

---

## 👥 Contributing

[Add contribution guidelines here]

---

## 📞 Support

For questions or issues:
- Email: support@custom-site-chat.com
- Website: https://custom-site-chat.com

---

**Built with ❤️ using React**
