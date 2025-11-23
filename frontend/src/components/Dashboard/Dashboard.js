// frontend/src/components/Dashboard/Dashboard.js
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI, collectionAPI, documentAPI } from '../../services/api';
import DocumentsList from './DocumentsList';
import CollectionSettings from './CollectionSettings';
import UploadDocumentModal from './UploadDocumentModal';
import './Dashboard.css';

const Dashboard = () => {
  // ==================== State ====================
  const [currentUser, setCurrentUser] = useState(null);
  const [collection, setCollection] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [activeTab, setActiveTab] = useState('documents'); // 'documents' or 'settings'
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [toast, setToast] = useState({ show: false, message: '', type: 'success' });
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  // ==================== Effects ====================
  useEffect(() => {
    checkAuth();
  }, []);

  useEffect(() => {
    if (currentUser) {
      loadCollection();
      loadDocuments();
    }
  }, [currentUser]);

  // ==================== Auth Functions ====================
  const checkAuth = async () => {
    try {
      const response = await authAPI.checkStatus();
      if (response.data.success && response.data.authenticated && response.data.user) {
        setCurrentUser(response.data.user);
      } else {
        navigate('/login');
      }
    } catch (error) {
      console.error('Error checking auth:', error);
      navigate('/login');
    }
  };

  const logout = async () => {
    if (!window.confirm('האם אתה בטוח שברצונך להתנתק?')) return;

    try {
      await authAPI.logout();
      navigate('/login');
    } catch (error) {
      console.error('Logout error:', error);
      showToast('שגיאה בהתנתקות', 'error');
    }
  };

  // ==================== Collection Functions ====================
  const loadCollection = async () => {
    try {
      const response = await collectionAPI.getCollectionInfo();
      if (response.data.success) {
        setCollection(response.data.data);
      }
    } catch (error) {
      console.error('Error loading collection:', error);
      showToast('שגיאה בטעינת הגדרות', 'error');
    }
  };

  const handleRegenerateKey = async () => {
    if (!window.confirm('האם אתה בטוח? המפתח הישן יהפוך ללא תקף!')) return;

    try {
      setLoading(true);
      const response = await collectionAPI.regenerateSecretKey();
      if (response.data.success) {
        setCollection(response.data.data);
        showToast('מפתח חדש נוצר בהצלחה', 'success');
      }
    } catch (error) {
      console.error('Error regenerating key:', error);
      showToast('שגיאה ביצירת מפתח חדש', 'error');
    } finally {
      setLoading(false);
    }
  };

  // ==================== Document Functions ====================
  const loadDocuments = async () => {
    try {
      setLoading(true);
      const response = await documentAPI.getMyDocuments();
      
      if (response.data.success) {
        setDocuments(response.data.data || []);
      }
    } catch (error) {
      console.error('Error loading documents:', error);
      showToast('שגיאה בטעינת מסמכים', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUploadComplete = () => {
    setShowUploadModal(false);
    loadDocuments();
    showToast('✅ המסמך הועלה בהצלחה ומעובד כעת', 'success');
  };

  const handleDeleteDocument = async (documentId) => {
    if (!window.confirm('האם אתה בטוח שברצונך למחוק מסמך זה?')) return;

    try {
      const response = await documentAPI.deleteDocument(documentId);
      if (response.data.success) {
        showToast('✅ המסמך נמחק בהצלחה', 'success');
        loadDocuments();
      }
    } catch (error) {
      console.error('Error deleting document:', error);
      showToast('שגיאה במחיקת מסמך', 'error');
    }
  };

  const handleReorderDocuments = async (newOrder) => {
    try {
      const documentIds = newOrder.map(doc => doc.id);
      await documentAPI.reorderDocuments(documentIds);
      setDocuments(newOrder);
      showToast('✅ סדר המסמכים עודכן', 'success');
    } catch (error) {
      console.error('Error reordering documents:', error);
      showToast('שגיאה בעדכון סדר המסמכים', 'error');
    }
  };

  // ==================== Helper Functions ====================
  const showToast = (message, type = 'success') => {
    setToast({ show: true, message, type });
    setTimeout(() => {
      setToast({ show: false, message: '', type: 'success' });
    }, 3000);
  };

  // ==================== Render ====================
  return (
    <div className="dashboard">
      {/* ==================== Header ==================== */}
      <header className="header">
        <div className="logo">📚 Custom Site Chat</div>
        <div className="user-info">
          <span className="welcome-text">
            שלום, {currentUser?.fullName || currentUser?.username}
          </span>
          <button className="logout-btn" onClick={logout}>
            התנתק
          </button>
        </div>
      </header>

      {/* ==================== Main Content ==================== */}
      <div className="main-content">
        {/* Tabs */}
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'documents' ? 'active' : ''}`}
            onClick={() => setActiveTab('documents')}
          >
            📄 המסמכים שלי ({documents.length})
          </button>
          <button
            className={`tab ${activeTab === 'settings' ? 'active' : ''}`}
            onClick={() => setActiveTab('settings')}
          >
            ⚙️ קוד הטמעה והגדרות
          </button>
        </div>

        {/* Tab Content */}
        <div className="tab-content">
          {activeTab === 'documents' && (
            <DocumentsList
              documents={documents}
              onUploadNew={() => setShowUploadModal(true)}
              onDelete={handleDeleteDocument}
              onReorder={handleReorderDocuments}
              loading={loading}
            />
          )}

          {activeTab === 'settings' && collection && (
            <CollectionSettings
              collection={collection}
              onRegenerateKey={handleRegenerateKey}
              loading={loading}
            />
          )}
        </div>
      </div>

      {/* ==================== Modals ==================== */}
      {showUploadModal && (
        <UploadDocumentModal
          onClose={() => setShowUploadModal(false)}
          onComplete={handleUploadComplete}
        />
      )}

      {/* Toast Notifications */}
      {toast.show && (
        <div className={`toast ${toast.type} show`}>
          <span>{toast.message}</span>
        </div>
      )}

      {/* Global Loading Spinner */}
      {loading && (
        <div style={{
          position: 'fixed',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          background: 'white',
          padding: '30px',
          borderRadius: '12px',
          boxShadow: '0 4px 20px rgba(0,0,0,0.2)',
          zIndex: 9999
        }}>
          <div className="spinner"></div>
          <p style={{ marginTop: '15px', textAlign: 'center' }}>טוען...</p>
        </div>
      )}
    </div>
  );
};

export default Dashboard;