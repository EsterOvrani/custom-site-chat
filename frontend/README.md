# 💬 Custom Site Chat - Frontend

<div align="center">

![Custom Site Chat](screenshots/hero-banner.png)

**AI-Powered Document Chat Platform** | Built with React ⚛️

[![React](https://img.shields.io/badge/React-18.2.0-blue?logo=react)](https://reactjs.org/)
[![Live Demo](https://img.shields.io/badge/Live-Demo-success?logo=vercel)](https://custom-site-chat.com)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

[Live Demo](https://custom-site-chat.com) • [Documentation](docs/) • [Report Bug](https://github.com/yourusername/custom-site-chat/issues) • [Request Feature](https://github.com/yourusername/custom-site-chat/issues)

</div>

---

## 📸 Screenshots

<div align="center">

### 🔐 Authentication
<img src="screenshots/login-page.png" alt="Login Page" width="400"/>
<img src="screenshots/register-page.png" alt="Register Page" width="400"/>

### 📄 Document Management
<img src="screenshots/dashboard.png" alt="Dashboard" width="800"/>
<img src="screenshots/upload-document.png" alt="Upload Document" width="800"/>

### 💬 Chat Widget
<img src="screenshots/chat-widget-closed.png" alt="Chat Widget Closed" width="300"/>
<img src="screenshots/chat-widget-open.png" alt="Chat Widget Open" width="300"/>

</div>

> 📌 **Note:** Add your actual screenshots to the `screenshots/` folder

---

## ✨ Features

### 🎯 **For Users**

| Feature | Description | Screenshot |
|---------|-------------|------------|
| 🔐 **Multiple Auth Methods** | Email/Password + Google OAuth | [View →](screenshots/login-page.png) |
| 📄 **Document Upload** | Drag & drop PDFs (up to 50 MB) | [View →](screenshots/upload-document.png) |
| 📊 **Real-time Progress** | Live processing with 7-stage pipeline | [View →](screenshots/processing-status.png) |
| 💬 **Embeddable Widget** | Add AI chat to any website | [View →](screenshots/chat-widget-demo.png) |
| 🌍 **RTL/LTR Support** | Auto-detect Hebrew/English | [View →](screenshots/rtl-support.png) |
| 🎨 **Customizable UI** | Brand colors, avatars, titles | [View →](screenshots/widget-customization.png) |

### 🛠️ **For Developers**

- ⚡ **React 18** with Hooks
- 🎨 **Custom CSS** with RTL support
- 🔄 **Axios Interceptors** for JWT
- 📱 **Responsive Design** (Mobile, Tablet, Desktop)
- 🧪 **Zero External UI Libraries** (Lightweight!)
- 🔒 **Secure Authentication** (JWT + OAuth)

---

## 🎨 Design System

### **Color Palette**

```css
/* Primary Colors */
--primary: #667eea;         /* Blue-Purple */
--secondary: #764ba2;       /* Dark Purple */

/* Status Colors */
--success: #28a745;         /* Green */
--warning: #ffc107;         /* Yellow */
--error: #dc3545;           /* Red */
--info: #17a2b8;            /* Cyan */

/* Gradients */
--gradient-primary: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

### **Typography**

```css
/* Font Family */
font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;

/* Font Sizes */
--text-xs: 12px;
--text-sm: 14px;
--text-base: 16px;
--text-lg: 18px;
--text-xl: 20px;
--text-2xl: 24px;
--text-3xl: 30px;
```

### **Spacing**

```css
--space-1: 4px;
--space-2: 8px;
--space-3: 12px;
--space-4: 16px;
--space-5: 20px;
--space-6: 24px;
--space-8: 32px;
```

---

## 🚀 Quick Start

### **Prerequisites**

- Node.js 16+ and npm
- Backend API running ([see backend docs](../backend/README.md))

### **Installation**

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/custom-site-chat.git
cd custom-site-chat/frontend

# 2. Install dependencies
npm install

# 3. Setup environment variables
cp .env.example .env
# Edit .env with your API URL and Google Client ID

# 4. Start development server
npm start
```

The app will open at **http://localhost:3000**

### **Environment Variables**

```env
REACT_APP_API_URL=http://localhost:8080
REACT_APP_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

---

## 📱 User Journey

### **1. Registration & Login**

```
┌─────────────┐
│   Landing   │
│    Page     │
└──────┬──────┘
       │
       ├──────────────┬──────────────┐
       ▼              ▼              ▼
  ┌─────────┐   ┌─────────┐   ┌──────────┐
  │  Login  │   │Register │   │  Google  │
  │  Email  │   │  Form   │   │  OAuth   │
  └────┬────┘   └────┬────┘   └────┬─────┘
       │             │              │
       │             ▼              │
       │      ┌─────────────┐       │
       │      │   Verify    │       │
       │      │   Email     │       │
       │      └──────┬──────┘       │
       │             │              │
       └─────────────┴──────────────┘
                     │
                     ▼
              ┌────────────┐
              │ Dashboard  │
              └────────────┘
```

### **2. Document Management**

```
Dashboard
    │
    ├─► Upload Document ─► Processing (2-4 min) ─► Completed ✅
    │                           │
    │                           ├─► Progress: 15% (Uploading)
    │                           ├─► Progress: 40% (Extracting)
    │                           ├─► Progress: 80% (Embeddings)
    │                           └─► Progress: 100% (Ready!)
    │
    ├─► View Document ─► Opens in new tab
    ├─► Download Document ─► Saves to disk
    └─► Delete Document ─► Confirmation ─► Removed
```

### **3. Widget Integration**

```
Settings Page
    │
    ├─► Copy Secret Key ─► Clipboard (sk_xxx...)
    │
    └─► Copy Embed Code ─► Paste in website ─► Widget Live! 🎉
                                                    │
                                                    ├─► User asks question
                                                    ├─► AI generates answer
                                                    └─► Sources shown
```

---

## 🧩 Component Architecture

```
src/
├── components/
│   ├── Auth/
│   │   ├── Login.jsx              # Email/Password + Google OAuth
│   │   ├── Register.jsx           # User registration form
│   │   ├── VerifyEmail.jsx        # 6-digit code verification
│   │   ├── ForgotPassword.jsx     # Request reset code
│   │   └── ResetPassword.jsx      # Set new password
│   │
│   ├── Documents/
│   │   ├── DocumentList.jsx       # List all documents
│   │   ├── UploadDocument.jsx     # Drag & drop upload
│   │   ├── DocumentCard.jsx       # Single document display
│   │   └── ProgressBar.jsx        # Processing progress (0-100%)
│   │
│   ├── Settings/
│   │   ├── CollectionSettings.jsx # Secret key management
│   │   ├── EmbedCode.jsx          # Widget embed code
│   │   └── CopyButton.jsx         # Clipboard copy utility
│   │
│   └── Common/
│       ├── Navbar.jsx             # Main navigation
│       ├── Sidebar.jsx            # Mobile menu
│       ├── Toast.jsx              # Notifications
│       └── Modal.jsx              # Generic modal
│
├── services/
│   └── api.js                     # Axios instance + interceptors
│
├── App.js                         # Main app with routing
└── index.js                       # Entry point
```

---

## 🎭 UI States & Interactions

### **Loading States**

| Component | Loading State | Visual |
|-----------|---------------|--------|
| Login Button | `🔄 Logging in...` | Spinner + Disabled |
| Document Upload | `⬆️ Uploading 15%` | Progress bar |
| Chat Widget | `⏳ Typing...` | Typing indicator |
| Settings Load | `⚙️ Loading...` | Skeleton screen |

### **Error States**

| Error Type | Message | Action |
|------------|---------|--------|
| 401 Unauthorized | `❌ Invalid credentials` | Show error + Clear form |
| 403 Forbidden | `✉️ Please verify your email` | Link to verify |
| 404 Not Found | `🔍 Account not found` | Suggest registration |
| 500 Server Error | `⚠️ Something went wrong` | Retry button |

### **Success States**

| Action | Message | Duration |
|--------|---------|----------|
| Login | `✅ Welcome back!` | 2 seconds |
| Upload | `✅ Document uploaded` | 3 seconds |
| Copy | `📋 Copied to clipboard` | 2 seconds |
| Delete | `🗑️ Document deleted` | 2 seconds |

### **Empty States**

```
┌────────────────────────────┐
│                            │
│          📄                │
│   No documents yet         │
│                            │
│  Upload your first PDF     │
│  to get started!           │
│                            │
│   [Upload Document]        │
│                            │
└────────────────────────────┘
```

---

## 📱 Responsive Design

### **Breakpoints**

```css
/* Mobile First */
@media (max-width: 767px) {
  /* Mobile styles */
}

@media (min-width: 768px) and (max-width: 1023px) {
  /* Tablet styles */
}

@media (min-width: 1024px) {
  /* Desktop styles */
}
```

### **Layout Examples**

**Mobile (< 768px):**
- Single column layout
- Hamburger menu
- Full-width buttons
- Stacked cards

**Tablet (768-1023px):**
- Two-column grid
- Sidebar navigation
- Medium-sized modals
- Responsive tables

**Desktop (1024px+):**
- Multi-column layout
- Fixed sidebar
- Large modals
- Full data tables

---

## 🔐 Authentication Flow

### **Email/Password Login**

1. User enters email + password
2. Click "Login" → API call
3. **Success:** JWT token → LocalStorage → Redirect to Dashboard
4. **Error:** Show error message

### **Google OAuth Login**

1. User clicks "Sign in with Google"
2. Google popup opens → Select account
3. Google returns credential token
4. Send to API → JWT token
5. Redirect to Dashboard (auto-verified ✅)

### **Email Verification**

1. User registers → 6-digit code sent to email
2. Enter code on verify page
3. **Valid:** Account activated → Redirect to login
4. **Invalid:** Show error + Resend option

---

## 📊 Document Processing Pipeline

```
User Upload
    ↓
[PENDING 0%]
    ↓
[UPLOADING 10-20%] ─► Upload to S3
    ↓
[EXTRACTING 30-45%] ─► Extract text from PDF
    ↓
[CHUNKING 50-60%] ─► Split into 500-char chunks
    ↓
[EMBEDDINGS 65-95%] ─► Generate AI embeddings
    ↓
[STORING 65-95%] ─► Save to Qdrant
    ↓
[COMPLETED 100%] ✅ ─► Ready for queries!
```

**Processing Time:**
- Small (< 5 MB): 1-2 minutes
- Medium (5-20 MB): 2-4 minutes
- Large (20-50 MB): 4-6 minutes

**Polling:** Frontend polls every 2 seconds for status updates

---

## 💬 Chat Widget

### **Embed Code**

```html
<!-- Paste before </body> -->
<script>
(function() {
  var script = document.createElement('script');
  script.src = 'https://custom-site-chat.com/chat-widget.js';
  script.async = true;
  script.onload = function() {
    window.ChatWidget.init({
      secretKey: 'sk_your_secret_key_here',
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

### **Widget Features**

- 💬 **Toggle Button** (bottom-right corner)
- 📝 **Message History** (max 10 messages)
- 🌍 **Auto-detect Language** (RTL/LTR)
- ⚠️ **Limit Warning** (at 10 messages)
- 🔄 **Reset Conversation** (clear history)
- 🎨 **Customizable** (colors, avatars, text)

### **Widget States**

| State | Visual |
|-------|--------|
| Closed | Floating button with 💬 icon |
| Open (Empty) | Welcome message from bot |
| Open (Active) | Conversation with scroll |
| Loading | `⏳ Typing...` indicator |
| Limit Reached | Warning banner + Disabled input |

---

## 🔧 Development

### **Project Structure**

```
frontend/
├── public/
│   ├── chat-widget.js          # Standalone widget
│   ├── index.html
│   └── favicon.ico
│
├── src/
│   ├── components/             # React components
│   ├── services/               # API services
│   ├── styles/                 # CSS files
│   ├── utils/                  # Helper functions
│   ├── App.js                  # Main app
│   └── index.js                # Entry point
│
├── screenshots/                # UI screenshots (add yours!)
├── docs/                       # Detailed documentation
│   ├── AUTH_FLOW.md
│   ├── DOCUMENTS_FLOW.md
│   ├── SETTINGS_FLOW.md
│   └── WIDGET_FLOW.md
│
├── .env.example                # Environment template
├── package.json
└── README.md                   # This file
```

### **Available Scripts**

```bash
# Development
npm start                # Start dev server (http://localhost:3000)
npm run build           # Build for production
npm run serve           # Serve production build

# Testing (if configured)
npm test                # Run tests
npm run test:coverage   # Test coverage report

# Linting (if configured)
npm run lint            # Check code quality
npm run lint:fix        # Auto-fix issues
```

### **Code Style**

- **Components:** PascalCase (`DocumentCard.jsx`)
- **Functions:** camelCase (`handleSubmit()`)
- **Files:** kebab-case (`api-service.js`)
- **CSS Classes:** kebab-case (`.upload-button`)
- **Direction:** RTL (Hebrew interface)

---

## 🐛 Common Issues & Solutions

### **1. Token Expiration**

**Problem:** User logged out unexpectedly

**Solution:**
```javascript
// Add token refresh logic in api.js
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      // Refresh token or redirect to login
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### **2. CORS Errors**

**Problem:** API requests blocked

**Solution:** Ensure backend allows frontend origin:
```javascript
// Backend: app.use(cors({ origin: 'http://localhost:3000' }))
```

### **3. Google OAuth Not Working**

**Problem:** Button not appearing

**Solution:**
1. Check `REACT_APP_GOOGLE_CLIENT_ID` in `.env`
2. Verify authorized origins in Google Console
3. Check browser console for errors

### **4. Document Upload Fails**

**Problem:** Large files not uploading

**Solution:**
- Check file size (max 50 MB)
- Verify backend upload limits
- Check network timeout settings

---

## 📚 Documentation

For detailed implementation guides, see the `docs/` folder:

| Document | Description |
|----------|-------------|
| [AUTH_FLOW.md](docs/AUTH_FLOW.md) | Complete authentication flows with diagrams |
| [DOCUMENTS_FLOW.md](docs/DOCUMENTS_FLOW.md) | Upload, processing, download, delete flows |
| [SETTINGS_FLOW.md](docs/SETTINGS_FLOW.md) | Collection settings & API key management |
| [WIDGET_FLOW.md](docs/WIDGET_FLOW.md) | Chat widget integration & customization |

Each guide includes:
- 📊 Flow diagrams (Mermaid)
- 🎨 UI state visualizations
- 💻 Code examples
- ⚠️ Error handling
- 🔧 Troubleshooting

---

## 🚀 Deployment

### **Production Build**

```bash
# Build optimized production bundle
npm run build

# Output: build/ directory (static files)
```

### **Deploy to Vercel** (Recommended)

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
vercel

# Production deployment
vercel --prod
```

### **Deploy with Docker**

```dockerfile
FROM node:16-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

```bash
docker build -t custom-site-chat-frontend .
docker run -p 3000:80 custom-site-chat-frontend
```

---

## 🤝 Contributing

We welcome contributions! Here's how:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### **Contribution Guidelines**

- Follow existing code style
- Add screenshots for UI changes
- Update documentation
- Test on multiple browsers
- Keep commits atomic

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Team

- **Your Name** - *Initial work* - [GitHub](https://github.com/yourusername)

See also the list of [contributors](https://github.com/yourusername/custom-site-chat/contributors) who participated in this project.

---

## 🙏 Acknowledgments

- [React](https://reactjs.org/) - UI framework
- [Axios](https://axios-http.com/) - HTTP client
- [Google OAuth](https://developers.google.com/identity) - Authentication
- [OpenAI](https://openai.com/) - AI embeddings

---

## 📞 Support

- 📧 Email: support@custom-site-chat.com
- 🌐 Website: [custom-site-chat.com](https://custom-site-chat.com)
- 💬 Issues: [GitHub Issues](https://github.com/yourusername/custom-site-chat/issues)

---

<div align="center">

**[⬆ back to top](#-custom-site-chat---frontend)**

Made with ❤️ using React

</div>
