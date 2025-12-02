# Frontend - Smart Document Chat

React-based user interface for document chat platform.

## 🚀 Quick Start
```bash
# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build
```

## 📁 Structure
```
src/
├── components/
│   ├── Auth/              # Login, Register, Verify
│   ├── Dashboard/         # Main app interface
│   └── Error/            # Error pages
├── services/
│   └── api.js            # API client (Axios)
├── App.js                # Main router
└── index.js              # Entry point
```

## 🔧 Environment Variables

Create `.env` file:
```env
REACT_APP_GOOGLE_CLIENT_ID=your-client-id
REACT_APP_API_URL=http://localhost:8080
```

## 🎨 Key Components

### Authentication Flow

- **Login.js** - Email/password + Google OAuth
- **Register.js** - User registration with validation
- **Verify.js** - Email verification (register + password reset)
- **ForgotPassword.js** - Request password reset
- **ResetPassword.js** - Set new password

### Dashboard

- **Dashboard.js** - Main container with tabs
- **DocumentsList.js** - Display and manage documents
- **CollectionSettings.js** - Secret key & embed code
- **UploadDocumentModal.js** - Multi-file upload with preview
- **ProgressBar.js** - Real-time processing progress

### Features

- **Auto-polling** - Updates document status every 2 seconds
- **Multi-file upload** - Upload multiple PDFs simultaneously
- **Real-time progress** - 7-stage processing visualization
- **Toast notifications** - User feedback system
- **Responsive design** - Mobile-friendly interface

## 📡 API Integration

All API calls go through `services/api.js`:
```javascript
import { authAPI, documentAPI, collectionAPI } from './services/api';

// Example usage
const response = await authAPI.login(email, password);
const docs = await documentAPI.getMyDocuments();
```

### API Endpoints Used

- `POST /auth/login` - User login
- `POST /auth/signup` - User registration
- `GET /api/documents/my-documents` - Get user documents
- `POST /api/documents/upload` - Upload PDF
- `GET /api/collection/info` - Get collection details

## 🎯 State Management

No external state management library - uses React hooks:

- `useState` - Component state
- `useEffect` - Side effects & polling
- `useRef` - Polling intervals
- `useNavigate` - Navigation

## 🌐 Routing
```
/ - Dashboard (protected)
/login - Login page
/register - Registration page
/verify - Email verification
/forgot-password - Request password reset
/reset-password - Set new password
/error - Error page
```

## 📦 Dependencies
```json
{
  "react": "^18.2.0",
  "react-dom": "^18.2.0",
  "react-router-dom": "^6.20.0",
  "axios": "^1.12.2"
}
```

## 🔨 Build & Deploy

### Development
```bash
npm start
# Runs on http://localhost:3000
```

### Production Build
```bash
npm run build
# Creates optimized build in /build
```

### Docker
```dockerfile
# Build stage
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 3000
```

## 🎨 Styling

- Pure CSS3 (no framework)
- CSS Variables for theming
- Flexbox & Grid layouts
- Media queries for responsiveness
- Gradient backgrounds (#667eea → #764ba2)

### Key CSS Files

- `Dashboard.css` - Main dashboard styles (1000+ lines)
- `Login.css` - Auth pages styling
- `Verify.css` - Verification flow
- Individual component styles

## 🐛 Debugging

### Common Issues

**1. API calls fail:**
```bash
# Check proxy in package.json
"proxy": "http://localhost:8080"
```

**2. Google OAuth not working:**
```bash
# Verify REACT_APP_GOOGLE_CLIENT_ID is set
echo $REACT_APP_GOOGLE_CLIENT_ID
```

**3. Document upload fails:**
```bash
# Check file size < 50MB
# Verify content type is application/pdf
```

### Browser Console Logs

Enable detailed logging:
```javascript
// In api.js
console.log('🔵 API Request:', config);
console.log('✅ API Response:', response);
```

## 📱 Mobile Support

- Responsive breakpoints: 768px, 480px
- Touch-friendly buttons (min 44px)
- Collapsible sidebar on mobile
- Optimized modals for small screens

## ♿ Accessibility

- Semantic HTML elements
- ARIA labels where needed
- Keyboard navigation support
- Focus management in modals
- Error announcements

## 🔮 Future Improvements

- [ ] Dark mode support
- [ ] Drag & drop file upload
- [ ] Chat widget preview
- [ ] Document preview in modal
- [ ] Export conversation history
- [ ] Internationalization (i18n)

---

**For backend documentation, see [../backend/README.md](../backend/README.md)**