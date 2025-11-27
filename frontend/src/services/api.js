// frontend/src/services/api.js
import axios from 'axios';

const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Interceptor - add token to every request
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    console.log('🔵 API Request:', {
      method: config.method.toUpperCase(),
      url: config.url,
      hasToken: !!token
    });
    
    return config;
  },
  error => {
    console.error('❌ Request Error:', error);
    return Promise.reject(error);
  }
);

// Interceptor for error handling
api.interceptors.response.use(
  response => {
    console.log('✅ API Response:', {
      url: response.config.url,
      status: response.status
    });
    return response;
  },
  error => {
    console.error('❌ API Error:', {
      url: error.config?.url,
      status: error.response?.status,
      message: error.message
    });
    
    if (error.response?.status === 401) {
      console.warn('⚠️ Unauthorized - Redirecting to login');
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    }
    
    return Promise.reject(error);
  }
);

// ==================== Authentication API ====================
export const authAPI = {
  checkStatus: () => api.get('/auth/status'),
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (userData) => api.post('/auth/signup', userData),
  verify: (data) => api.post('/auth/verify', data),
  checkIfVerified: (email) => api.get(`/auth/check-verified/${encodeURIComponent(email)}`),
  resendVerificationCode: (email) => api.post('/auth/resend', null, { params: { email } }),
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    return api.post('/auth/logout');
  },
  checkUsername: (username) => api.get(`/auth/check-username/${encodeURIComponent(username)}`),
  checkEmail: (email) => api.get(`/auth/check-email/${encodeURIComponent(email)}`),
  googleLogin: (credential) => api.post('/auth/google', { credential }),
  
  // 🆕 Forgot Password & Reset Password
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  resetPassword: (email, resetCode, newPassword) => api.post('/auth/reset-password', { 
    email, 
    resetCode, 
    newPassword 
  })
};

// ==================== Collection API ====================
export const collectionAPI = {
  getCollectionInfo: () => api.get('/collection/info'),
  regenerateSecretKey: () => api.post('/collection/regenerate-key'),
  getEmbedCode: () => api.get('/collection/embed-code')
};

// ==================== Document API ====================
export const documentAPI = {
  getMyDocuments: () => api.get('/documents/my-documents'),
  uploadDocument: (file) => {
    const formData = new FormData();
    formData.append('file', file);

    return api.post('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },
  deleteDocument: (documentId) => api.delete(`/documents/${documentId}`),
  reorderDocuments: (documentIds) => api.put('/documents/reorder', {
    documentIds
  }),
  getDownloadUrl: (documentId) => api.get(`/documents/${documentId}/download-url`),
  downloadDocument: (documentId) => {
    return api.get(`/documents/${documentId}/download`, {
      responseType: 'blob'
    });
  }
};

export default api;