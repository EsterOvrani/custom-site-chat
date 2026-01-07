// frontend/src/components/Dashboard/Dashboard.js
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI, collectionAPI, documentAPI } from '../../services/api';
import DocumentsList from './DocumentsList';
import CollectionSettings from './CollectionSettings';
import UploadDocumentModal from './UploadDocumentModal';
import Analytics from './Analytics';

import './Dashboard.css';

const Dashboard = () => {
  const [currentUser, setCurrentUser] = useState(null);
  const [collection, setCollection] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [activeTab, setActiveTab] = useState('documents');
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [toast, setToast] = useState({ show: false, message: '', type: 'success' });
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();
  const pollingIntervalRef = useRef(null);

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

  // ⭐ Polling - בדיקה אוטומטית של מסמכים בעיבוד
  useEffect(() => {
    const hasProcessingDocs = documents.some(doc => 
      doc.processingStatus === 'PROCESSING' || doc.processingStatus === 'PENDING'
    );

    if (hasProcessingDocs) {
      console.log('🔄 Starting polling - documents in progress detected');
      
      // בדיקה כל 2 שניות
      pollingIntervalRef.current = setInterval(() => {
        console.log('🔄 Polling for updates...');
        loadDocuments(true); // true = silent refresh (ללא spinner)
      }, 2000);
    } else {
      // אין מסמכים בעיבוד - עצור polling
      if (pollingIntervalRef.current) {
        console.log('⏹️ Stopping polling - no documents in progress');
        clearInterval(pollingIntervalRef.current);
        pollingIntervalRef.current = null;
      }
    }

    // Cleanup
    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
      }
    };
  }, [documents]);

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
  
  /**
   * טעינת מסמכים
   * @param {boolean} silent - אם true, לא להציג spinner
   */
  const loadDocuments = async (silent = false) => {
    try {
      if (!silent) {
        setLoading(true);
      }
      
      const response = await documentAPI.getMyDocuments();
      
      if (response.data.success) {
        const newDocs = response.data.data || [];
        
        // עדכן רק אם יש שינוי (למנוע re-renders מיותרים)
        if (JSON.stringify(newDocs) !== JSON.stringify(documents)) {
          setDocuments(newDocs);
          console.log('📄 Documents updated:', newDocs.length);
        }
      }
    } catch (error) {
      console.error('Error loading documents:', error);
      if (!silent) {
        showToast('שגיאה בטעינת מסמכים', 'error');
      }
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  };

  const handleUploadComplete = (newDocument, placeholderId) => {
    console.log('📤 Upload event:', { newDocument, placeholderId });
    
    if (placeholderId) {
      // ⭐ זו קריאה שנייה - צריך להחליף או להסיר את ה-placeholder
      if (newDocument) {
        // הצלחה - החלף placeholder עם מסמך אמיתי
        setDocuments(prevDocs => 
          prevDocs.map(doc => 
            doc.id === placeholderId ? newDocument : doc
          )
        );
        console.log('✅ Replaced placeholder with real document');
      } else {
        // שגיאה - הסר את ה-placeholder
        setDocuments(prevDocs => 
          prevDocs.filter(doc => doc.id !== placeholderId)
        );
        showToast('שגיאה בהעלאת המסמך', 'error');
        console.error('❌ Upload failed, removed placeholder');
      }
    } else {
      // ⭐ זו קריאה ראשונה - הוסף placeholder
      setShowUploadModal(false);
      
      if (newDocument) {
        setDocuments(prevDocs => [newDocument, ...prevDocs]);
        // showToast('מעלה מסמך...', 'success');
        console.log('✅ Added placeholder document');
      }
    }
  };

  const handleDeleteDocument = async (documentId) => {
    if (!window.confirm('האם אתה בטוח שברצונך למחוק מסמך זה?')) return;

    try {
      const response = await documentAPI.deleteDocument(documentId);
      if (response.data.success) {
        showToast('המסמך נמחק בהצלחה', 'success');
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
      showToast('סדר המסמכים עודכן', 'success');
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

  // ספירת מסמכים בעיבוד
  const processingCount = documents.filter(doc => 
    doc.processingStatus === 'PROCESSING' || doc.processingStatus === 'PENDING'
  ).length;

  // ==================== Render ====================
  return (
    <div className="dashboard">
      {/* Header */}
      <header className="header">
        <div className="logo">💬 Custom Site Chat</div>
        <div className="user-info">
          {processingCount > 0 && (
            <span style={{
              padding: '6px 12px',
              background: '#ffc107',
              color: 'white',
              borderRadius: '12px',
              fontSize: '13px',
              fontWeight: 600,
              marginLeft: '15px',
              animation: 'pulse 2s infinite'
            }}>
              ⏳ {processingCount} מעבד
            </span>
          )}
          <span className="welcome-text">
            שלום, {currentUser?.fullName || currentUser?.username}
          </span>
          <button className="logout-btn" onClick={logout}>
            התנתק
          </button>
        </div>
      </header>

      {/* Main Content */}
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
          <button
            className={`tab ${activeTab === 'analytics' ? 'active' : ''}`}
            onClick={() => setActiveTab('analytics')}
          >
            📊 Analytics
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

          {activeTab === 'analytics' && (
            <Analytics />
          )}
        </div>
      </div>

      {/* Modals */}
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

      {/* Global Loading Spinner - רק אם loading=true ואין polling */}
      {loading && !processingCount && (
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

      {/* ⭐ Add CSS animation for pulse */}
      <style>
        {`
          @keyframes pulse {
            0%, 100% {
              opacity: 1;
            }
            50% {
              opacity: 0.7;
            }
          }
        `}
      </style>
    </div>
  );
};

export default Dashboard;