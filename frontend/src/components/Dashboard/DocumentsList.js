// frontend/src/components/Dashboard/DocumentsList.js
import React, { useState, useEffect } from 'react';
import { documentAPI } from '../../services/api';
import ProgressBar from './ProgressBar';

const DocumentsList = ({ documents, onUploadNew, onDelete, onReorder, loading }) => {
  const [downloading, setDownloading] = useState({});
  const [processingDocs, setProcessingDocs] = useState([]);
  
  // ⭐ Polling - בדיקה אוטומטית של מסמכים בעיבוד
  useEffect(() => {
    const docsInProgress = documents.filter(doc => 
      doc.processingStatus === 'PROCESSING' || doc.processingStatus === 'PENDING'
    );
    
    setProcessingDocs(docsInProgress);
    
    if (docsInProgress.length > 0) {
      const interval = setInterval(() => {
        console.log('🔄 Polling for document updates...');
        // הפונקציה הזו תופעל על ידי הקומפוננטה האב (Dashboard)
        // שתקרא ל-loadDocuments() שוב
      }, 2000); // בדיקה כל 2 שניות
      
      return () => clearInterval(interval);
    }
  }, [documents]);
  
  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    return new Date(dateString).toLocaleDateString('he-IL');
  };

  const getStatusBadge = (status) => {
    const statuses = {
      'COMPLETED': { label: '✓ מעובד', color: '#28a745' },
      'PROCESSING': { label: '⏳ מעבד', color: '#ffc107' },
      'PENDING': { label: '○ ממתין', color: '#6c757d' },
      'FAILED': { label: '✗ נכשל', color: '#dc3545' }
    };

    const badge = statuses[status] || statuses['PENDING'];

    return (
      <span style={{
        padding: '4px 12px',
        background: badge.color,
        color: 'white',
        borderRadius: '12px',
        fontSize: '12px',
        fontWeight: 600
      }}>
        {badge.label}
      </span>
    );
  };

  const handleDownload = async (documentId, fileName) => {
    try {
      setDownloading(prev => ({ ...prev, [documentId]: true }));
      
      const response = await documentAPI.downloadDocument(documentId);
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
    } catch (error) {
      console.error('Error downloading document:', error);
      alert('שגיאה בהורדת המסמך');
    } finally {
      setDownloading(prev => ({ ...prev, [documentId]: false }));
    }
  };

  const handleView = async (documentId) => {
    try {
      const response = await documentAPI.getDownloadUrl(documentId);
      
      if (response.data.success && response.data.url) {
        window.open(response.data.url, '_blank');
      }
    } catch (error) {
      console.error('Error viewing document:', error);
      alert('שגיאה בפתיחת המסמך');
    }
  };

  if (loading && documents.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '60px 20px' }}>
        <div className="spinner"></div>
        <p style={{ marginTop: '20px', color: '#666' }}>טוען מסמכים...</p>
      </div>
    );
  }

  return (
    <div className="documents-list-container">
      {/* Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '30px'
      }}>
        <h2 style={{ margin: 0, color: '#333' }}>המסמכים שלי</h2>
        <button
          className="btn-primary"
          onClick={onUploadNew}
          style={{
            padding: '12px 24px',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            fontWeight: 600,
            cursor: 'pointer',
            fontSize: '16px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}
        >
          <span>➕</span>
          <span>העלה מסמך חדש</span>
        </button>
      </div>

      {/* ⭐ Progress Bars Section */}
      {processingDocs.length > 0 && (
        <div style={{ marginBottom: '30px' }}>
          <h3 style={{
            fontSize: '16px',
            color: '#666',
            marginBottom: '15px',
            fontWeight: 600
          }}>
            מסמכים בעיבוד ({processingDocs.length})
          </h3>
          {processingDocs.map(doc => (
            <ProgressBar
              key={doc.id}
              progress={doc.processingProgress || 0}
              stage={doc.processingStageDescription || 'מעבד...'}
              fileName={doc.originalFileName}
              fileSize={doc.fileSize}
            />
          ))}
        </div>
      )}

      {/* Empty State */}
      {documents.length === 0 && !loading && (
        <div style={{
          textAlign: 'center',
          padding: '80px 20px',
          background: '#f8f9ff',
          borderRadius: '12px',
          border: '2px dashed #cbd5e1'
        }}>
          <div style={{ fontSize: '4rem', marginBottom: '20px' }}>📄</div>
          <h3 style={{ color: '#333', marginBottom: '10px' }}>אין מסמכים עדיין</h3>
          <p style={{ color: '#666' }}>
            העלה מסמכים כדי לבנות את מאגר הידע של הצ'אט שלך
          </p>
        </div>
      )}

      {/* Completed Documents Grid */}
      {documents.filter(doc => doc.processingStatus === 'COMPLETED' || doc.processingStatus === 'FAILED').length > 0 && (
        <>
          <h3 style={{
            fontSize: '16px',
            color: '#666',
            marginBottom: '15px',
            fontWeight: 600,
            marginTop: processingDocs.length > 0 ? '30px' : '0'
          }}>
            מסמכים מעובדים ({documents.filter(doc => doc.processingStatus === 'COMPLETED').length})
          </h3>
          
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
            gap: '20px'
          }}>
            {documents
              .filter(doc => doc.processingStatus === 'COMPLETED' || doc.processingStatus === 'FAILED')
              .map((doc) => (
                <div
                  key={doc.id}
                  style={{
                    background: 'white',
                    border: '1px solid #e1e8ed',
                    borderRadius: '12px',
                    padding: '20px',
                    transition: 'all 0.2s',
                    cursor: 'default'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.1)';
                    e.currentTarget.style.transform = 'translateY(-2px)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.boxShadow = 'none';
                    e.currentTarget.style.transform = 'translateY(0)';
                  }}
                >
                  {/* Document Icon & Title */}
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', marginBottom: '15px' }}>
                    <div style={{ fontSize: '2.5rem' }}>📄</div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 600, color: '#333', marginBottom: '5px', wordBreak: 'break-word' }}>
                        {doc.originalFileName}
                      </div>
                      <div style={{ fontSize: '13px', color: '#666' }}>
                        {formatFileSize(doc.fileSize)}
                      </div>
                    </div>
                  </div>

                  {/* Status */}
                  <div style={{ marginBottom: '15px' }}>
                    {getStatusBadge(doc.processingStatus)}
                  </div>

                  {/* Metadata */}
                  <div style={{ fontSize: '13px', color: '#666', marginBottom: '15px' }}>
                    <div>📅 הועלה: {formatDate(doc.createdAt)}</div>
                    {doc.processingStatus === 'COMPLETED' && (
                      <div style={{ color: '#28a745', marginTop: '5px' }}>
                        ✓ מוכן לשאילת שאלות
                      </div>
                    )}
                    {doc.processingStatus === 'FAILED' && doc.errorMessage && (
                      <div style={{ color: '#dc3545', marginTop: '5px', fontSize: '12px' }}>
                        {doc.errorMessage}
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div style={{ 
                    display: 'flex', 
                    gap: '8px', 
                    paddingTop: '15px', 
                    borderTop: '1px solid #e1e8ed',
                    flexWrap: 'wrap'
                  }}>
                    <button
                      onClick={() => handleView(doc.id)}
                      disabled={doc.processingStatus !== 'COMPLETED'}
                      style={{
                        flex: 1,
                        minWidth: '80px',
                        padding: '8px',
                        background: doc.processingStatus === 'COMPLETED' ? '#e8f0fe' : '#f5f5f5',
                        color: doc.processingStatus === 'COMPLETED' ? '#1976d2' : '#999',
                        border: '1px solid',
                        borderColor: doc.processingStatus === 'COMPLETED' ? '#bbdefb' : '#ddd',
                        borderRadius: '6px',
                        cursor: doc.processingStatus === 'COMPLETED' ? 'pointer' : 'not-allowed',
                        fontSize: '14px',
                        fontWeight: 500
                      }}
                    >
                      👁️ צפה
                    </button>

                    <button
                      onClick={() => handleDownload(doc.id, doc.originalFileName)}
                      disabled={downloading[doc.id] || doc.processingStatus !== 'COMPLETED'}
                      style={{
                        flex: 1,
                        minWidth: '80px',
                        padding: '8px',
                        background: doc.processingStatus === 'COMPLETED' ? '#e8f5e9' : '#f5f5f5',
                        color: doc.processingStatus === 'COMPLETED' ? '#2e7d32' : '#999',
                        border: '1px solid',
                        borderColor: doc.processingStatus === 'COMPLETED' ? '#c8e6c9' : '#ddd',
                        borderRadius: '6px',
                        cursor: (doc.processingStatus === 'COMPLETED' && !downloading[doc.id]) ? 'pointer' : 'not-allowed',
                        fontSize: '14px',
                        fontWeight: 500,
                        opacity: downloading[doc.id] ? 0.6 : 1
                      }}
                    >
                      {downloading[doc.id] ? '⏬ מוריד...' : '⬇️ הורד'}
                    </button>

                    <button
                      onClick={() => onDelete(doc.id)}
                      style={{
                        flex: 1,
                        minWidth: '80px',
                        padding: '8px',
                        background: '#fee',
                        color: '#c33',
                        border: '1px solid #fcc',
                        borderRadius: '6px',
                        cursor: 'pointer',
                        fontSize: '14px',
                        fontWeight: 500
                      }}
                    >
                      🗑️ מחק
                    </button>
                  </div>
                </div>
              ))}
          </div>
        </>
      )}
    </div>
  );
};

export default DocumentsList;