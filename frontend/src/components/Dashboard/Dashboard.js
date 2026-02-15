import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI, collectionAPI, documentAPI, tokenAPI } from '../../services/api';
import axios from 'axios';
import DocumentsList from './DocumentsList';
import MyAccount from './MyAccount';
import Customization from './Customization';
import EmbedCode from './EmbedCode';
import Analytics from './Analytics';
import DuplicateDialog from './DuplicateDialog';
import tokenSSEService from '../../services/tokenSSE';

import './Dashboard.css';

const Dashboard = () => {
  const [currentUser, setCurrentUser] = useState(null);
  const [collection, setCollection] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [activeTab, setActiveTab] = useState('account');
  const [toast, setToast] = useState({ show: false, message: '', type: 'success' });
  const [loading, setLoading] = useState(false);
  const [duplicateDialog, setDuplicateDialog] = useState(null);
  const [tokenInfo, setTokenInfo] = useState(null);
  const [tokenLoading, setTokenLoading] = useState(false);
  const [customizationSettings, setCustomizationSettings] = useState(null);

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
      loadTokenInfo();
    }
  }, [currentUser]);

  // ⭐ Polling - בדיקה אוטומטית של מסמכים בעיבוד
  useEffect(() => {
    const hasProcessingDocs = documents.some(doc => 
      doc.processingStatus === 'PROCESSING' || 
      doc.processingStatus === 'PENDING' ||
      doc.isTemporary
    );

    if (hasProcessingDocs) {
      console.log('🔄 Starting polling - documents in progress detected');
      
      pollingIntervalRef.current = setInterval(() => {
        console.log('🔄 Polling for updates...');
        loadDocuments(true);
      }, 2000);
    } else {
      if (pollingIntervalRef.current) {
        console.log('⏹️ Stopping polling - no documents in progress');
        clearInterval(pollingIntervalRef.current);
        pollingIntervalRef.current = null;
      }
    }

    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
      }
    };
  }, [documents]);

  // ⭐ SSE - חיבור לעדכוני טוקנים בזמן אמת ✅ חדש
  useEffect(() => {
    if (!currentUser) return;

    console.log('🚀 Setting up SSE for token updates');

    const handleTokenUpdate = (data) => {
      console.log('💰 Received real-time token update:', data);
      
      setTokenInfo(prevInfo => {
        if (!prevInfo) return prevInfo;
        
        return {
          ...prevInfo,
          used: data.used,
          remaining: data.remaining,
          usagePercentage: data.usagePercentage
        };
      });

      if (data.usagePercentage >= 95 && data.remaining > 0) {
        showToast(`⚠️ נותרו רק ${data.remaining.toLocaleString()} טוקנים!`, 'warning');
      } else if (data.remaining === 0) {
        showToast('❌ מכסת הטוקנים הסתיימה!', 'error');
      }
    };

    tokenSSEService.connect();
    tokenSSEService.addListener(handleTokenUpdate);

    return () => {
      console.log('🔌 Cleaning up SSE connection');
      tokenSSEService.removeListener(handleTokenUpdate);
    };
  }, [currentUser]);

  // ⭐ בדיקה אם הדפדפן תומך ב-SSE ✅ חדש
  useEffect(() => {
    if (typeof EventSource === 'undefined') {
      console.error('❌ Browser does not support SSE');
      showToast('הדפדפן לא תומך בעדכונים בזמן אמת', 'warning');
    }
  }, []);

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
      tokenSSEService.disconnect(); // ✅ הוספנו את זה
      
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

  // ==================== Token Functions ====================
  const loadTokenInfo = async () => {
    try {
      setTokenLoading(true);
      const response = await tokenAPI.getTokenUsage();
      if (response.data) {
        setTokenInfo(response.data);
      }
    } catch (error) {
      console.error('Error loading token info:', error);
    } finally {
      setTokenLoading(false);
    }
  };

  // ==================== Document Functions ====================
  
  /**
   * ✅ FIX: טעינת מסמכים עם שמירה של placeholders
   * @param {boolean} silent - אם true, לא להציג spinner
   */
  const loadDocuments = async (silent = false) => {
    try {
      if (!silent) {
        setLoading(true);
      }
      
      const response = await documentAPI.getMyDocuments();
      
      if (response.data.success) {
        const serverDocs = response.data.data || [];
        
        // ✅ FIX: שמור placeholders זמניים
        setDocuments(prev => {
          // קבל את כל ה-placeholders הזמניים
          const tempDocs = prev.filter(doc => doc.isTemporary);
          
          // מסמכים אמיתיים מהשרת (לא זמניים)
          const realDocs = serverDocs.map(doc => ({ ...doc, isTemporary: false }));
          
          // ✅ FIX: מחק רק placeholders שהמסמך שלהם כבר הגיע
          const validTempDocs = tempDocs.filter(tempDoc => {
            // בדוק אם המסמך הזה כבר הגיע מהשרת
            const matchingDoc = realDocs.find(realDoc => 
              realDoc.originalFileName === tempDoc.originalFileName &&
              Math.abs(realDoc.fileSize - tempDoc.fileSize) < 100 // tolerance
            );
            
            if (matchingDoc) {
              console.log(`🔄 Removing placeholder for: ${tempDoc.originalFileName} (found on server with ID: ${matchingDoc.id})`);
              return false; // הסר את ה-placeholder
            }
            
            return true; // שמור את ה-placeholder
          });
          
          // שלב: מסמכים אמיתיים + placeholders תקפים
          const combined = [...realDocs, ...validTempDocs];
          
          // עדכן רק אם יש שינוי
          if (JSON.stringify(combined) !== JSON.stringify(prev)) {
            console.log('📄 Documents updated:', {
              real: realDocs.length,
              temp: validTempDocs.length,
              total: combined.length
            });
            return combined;
          }
          
          return prev;
        });
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

  // ⭐ העלאה ישירה של קבצים עם בדיקת כפילויות
  const handleUploadNew = async (files) => {
    if (!files || files.length === 0) return;
    
    console.log(`🚀 Starting upload of ${files.length} files`);
    
    for (const file of files) {
      await checkAndUploadFile(file);
    }
  };

  /**
   * בדיקה אם הקובץ כפול ואז העלאה
   */
  const checkAndUploadFile = async (file) => {
    try {
      // Check if file with same name exists
      const checkResponse = await documentAPI.checkDuplicate(file.name);
      
      if (checkResponse.data.success && checkResponse.data.data.exists) {
        // File exists - show duplicate dialog
        const duplicateData = checkResponse.data.data;
        
        setDuplicateDialog({
          file: file,
          existingDocId: duplicateData.existingDocumentId,
          suggestedName: duplicateData.suggestedName,
          fileName: duplicateData.fileName
        });
        
        return; // Wait for user decision
      }
      
      // File doesn't exist - normal upload
      await uploadSingleFile(file, null);
      
    } catch (error) {
      console.error(`❌ Error checking duplicate for ${file.name}:`, error);
      showToast(`שגיאה בבדיקת הקובץ ${file.name}`, 'error');
    }
  };

  /**
   * Handle "Replace" button from duplicate dialog
   */
  const handleReplace = async () => {
    if (!duplicateDialog) return;
    
    const { file, existingDocId } = duplicateDialog;
    setDuplicateDialog(null);
    
    console.log(`🔄 [${file.name}] REPLACEMENT MODE - will replace document ID: ${existingDocId}`);
    
    // ✅ FIX: מחק את המסמך הישן מה-UI לפני ההעלאה
    setDocuments(prev => prev.filter(doc => doc.id !== existingDocId));
    
    // Upload with replacement
    await uploadSingleFile(file, existingDocId);
  };

  /**
   * Handle "Save as new" button from duplicate dialog
   */
  const handleRename = async () => {
    if (!duplicateDialog) return;
    
    const { file, suggestedName } = duplicateDialog;
    setDuplicateDialog(null);
    
    // Create new file with suggested name
    const renamedFile = new File([file], suggestedName, { type: file.type });
    
    // Upload as new
    await uploadSingleFile(renamedFile, null);
  };

  /**
   * Handle "Cancel" button from duplicate dialog
   */
  const handleCancelUpload = () => {
    setDuplicateDialog(null);
    showToast('העלאה בוטלה', 'info');
  };

  /**
   * ✅ FIX: העלאת קובץ בודד עם שמירה נכונה של placeholders
   * @param {File} file - הקובץ להעלאה
   * @param {number|null} replaceDocumentId - ID של מסמך להחלפה (null להעלאה רגילה)
   */
  const uploadSingleFile = async (file, replaceDocumentId = null) => {
    console.log(`📤 [${file.name}] Starting upload - Replace ID: ${replaceDocumentId || 'NONE'}`);

    // ✅ FIX: יצירת placeholder עם דגל isTemporary
    const placeholderId = `temp-${Date.now()}-${Math.random()}`;
    const placeholder = {
      id: placeholderId,
      originalFileName: file.name,
      fileSize: file.size,
      fileSizeFormatted: formatFileSize(file.size),
      processingStatus: 'PENDING',
      processingProgress: 5,
      processingStage: 'UPLOADING',
      processingStageDescription: replaceDocumentId ? 'מחליף קובץ...' : 'מעלה לשרת...',
      createdAt: new Date().toISOString(),
      active: true,
      isTemporary: true, // ✅ FIX: סמן כ-temporary
      replacingDocumentId: replaceDocumentId // ✅ FIX: שמור את ה-ID שמחליפים
    };

    console.log(`📤 [${file.name}] Adding placeholder (ID: ${placeholderId})`);
    setDocuments(prev => [placeholder, ...prev]);
    
    // העלאה בפועל
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      // Add replaceDocumentId if this is a replacement
      if (replaceDocumentId) {
        formData.append('replaceDocumentId', replaceDocumentId);
      }
      
      const token = localStorage.getItem('token');
      const response = await axios.post('/api/documents/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          'Authorization': `Bearer ${token}`
        }
      });
      
      console.log(`✅ [${file.name}] Upload successful`);
      
      if (response.data.success && response.data.document) {
        console.log(`✅ [${file.name}] Upload response received:`, response.data.document);
        
        // ✅ FIX: החלף את ה-placeholder במסמך האמיתי
        setDocuments(prev => prev.map(doc => {
          if (doc.id === placeholderId) {
            console.log(`✅ [${file.name}] ${replaceDocumentId ? 'Replacement' : 'Upload'} successful - new ID: ${response.data.document.id}`);
            return {
              ...response.data.document,
              isTemporary: false // ✅ FIX: זה כבר לא temporary
            };
          }
          return doc;
        }));

        // Show appropriate message
        if (replaceDocumentId) {
          showToast(`${file.name} הוחלף בהצלחה`, 'success');
        } else {
          showToast(`${file.name} הועלה בהצלחה`, 'success');
        }

        // ✅ עדכן מכסת טוקנים לאחר העלאה
        loadTokenInfo();

      } else {
        throw new Error('No document in response');
      }
      
    } catch (error) {
      console.error(`❌ [${file.name}] Upload error:`, error);
      
      // ✅ FIX: הסרת placeholder במקרה של שגיאה
      setDocuments(prev => prev.filter(doc => doc.id !== placeholderId));
      
      // ✅ FIX: אם זו הייתה החלפה שנכשלה, החזר את המסמך הישן
      if (replaceDocumentId) {
        console.log(`🔄 [${file.name}] Replacement failed - reloading documents`);
        loadDocuments(true);
      }
      
      const errorMsg = error.response?.data?.message || error.message;
      showToast(`שגיאה בהעלאת ${file.name}: ${errorMsg}`, 'error');
    }
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
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

  const handleDeleteAllDocuments = async () => {
    const completedDocs = documents.filter(doc => doc.processingStatus === 'COMPLETED' || doc.processingStatus === 'FAILED');
    
    if (completedDocs.length === 0) {
      showToast('אין מסמכים למחיקה', 'info');
      return;
    }

    if (!window.confirm(`האם אתה בטוח שברצונך למחוק את כל ${completedDocs.length} המסמכים? פעולה זו אינה ניתנת לביטול!`)) return;

    try {
      setLoading(true);
      const response = await documentAPI.deleteAllDocuments();
      if (response.data.success) {
        showToast(`${response.data.deletedCount} מסמכים נמחקו בהצלחה`, 'success');
        loadDocuments();
      }
    } catch (error) {
      console.error('Error deleting all documents:', error);
      showToast('שגיאה במחיקת המסמכים', 'error');
    } finally {
      setLoading(false);
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

  // ספירת מסמכים בעיבוד (כולל זמניים)
  const processingCount = documents.filter(doc => 
    doc.processingStatus === 'PROCESSING' || 
    doc.processingStatus === 'PENDING' ||
    doc.isTemporary
  ).length;

  // ==================== Render ====================
  
  // Helper function for progress bar color
  const getProgressColor = (percentage) => {
    if (percentage < 50) return '#10b981'; // ירוק
    if (percentage < 75) return '#f59e0b'; // כתום
    return '#ef4444'; // אדום
  };

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
          
          {/* Token Progress Bar in Header */}
          {tokenInfo && (
            <div className="header-token-usage" title={`${tokenInfo.usagePercentage?.toFixed(1)}% בשימוש`}>
              <div className="header-token-progress">
                <div 
                  className="header-token-fill"
                  style={{ 
                    width: `${Math.min(tokenInfo.usagePercentage || 0, 100)}%`,
                    backgroundColor: getProgressColor(tokenInfo.usagePercentage || 0)
                  }}
                />
              </div>
              <span className="header-token-text" style={{ color: getProgressColor(tokenInfo.usagePercentage || 0) }}>
                {tokenInfo.usagePercentage?.toFixed(0)}%
              </span>
            </div>
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
            className={`tab ${activeTab === 'account' ? 'active' : ''}`}
            onClick={() => setActiveTab('account')}
          >
            👤 החשבון שלי
          </button>
          <button
            className={`tab ${activeTab === 'documents' ? 'active' : ''}`}
            onClick={() => setActiveTab('documents')}
          >
            📄 המסמכים שלי ({documents.filter(d => !d.isTemporary).length})
          </button>
          <button
            className={`tab ${activeTab === 'customization' ? 'active' : ''}`}
            onClick={() => setActiveTab('customization')}
          >
            🎨 התאמה אישית
          </button>
          <button
            className={`tab ${activeTab === 'embed' ? 'active' : ''}`}
            onClick={() => setActiveTab('embed')}
          >
            📦 קוד הטמעה
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
          {activeTab === 'account' && (
            <MyAccount 
              tokenInfo={tokenInfo} 
              loading={tokenLoading} 
              currentUser={currentUser}
            />
          )}
          
          {activeTab === 'documents' && (
            <DocumentsList
              documents={documents}
              onUploadNew={handleUploadNew}
              onDelete={handleDeleteDocument}
              onDeleteAll={handleDeleteAllDocuments}
              onReorder={handleReorderDocuments}
              loading={loading}
            />
          )}

          {activeTab === 'customization' && collection && (
            <Customization
              collection={collection}
              onRegenerateKey={handleRegenerateKey}
              loading={loading}
              onSettingsChange={setCustomizationSettings}
            />
          )}

          {activeTab === 'embed' && collection && (
            <EmbedCode
              collection={collection}
              customizationSettings={customizationSettings}
            />
          )}

          {activeTab === 'analytics' && (
            <Analytics />
          )}
        </div>
      </div>

      {/* Toast Notifications */}
      {toast.show && (
        <div className={`toast ${toast.type} show`}>
          <span>{toast.message}</span>
        </div>
      )}

      {/* Duplicate Dialog */}
      {duplicateDialog && (
        <DuplicateDialog
          fileName={duplicateDialog.fileName}
          suggestedName={duplicateDialog.suggestedName}
          onReplace={handleReplace}
          onRename={handleRename}
          onCancel={handleCancelUpload}
        />
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