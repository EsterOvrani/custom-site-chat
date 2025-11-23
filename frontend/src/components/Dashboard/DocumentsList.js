// frontend/src/components/Dashboard/DocumentsList.js
import React from 'react';

const DocumentsList = ({ documents, onUploadNew, onDelete, onReorder, loading }) => {
  
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
          <p style={{ color: '#666', marginBottom: '30px' }}>
            העלה את המסמך הראשון שלך כדי להתחיל
          </p>
          <button
            onClick={onUploadNew}
            style={{
              padding: '12px 30px',
              background: '#667eea',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              fontWeight: 600,
              cursor: 'pointer',
              fontSize: '16px'
            }}
          >
            העלה מסמך
          </button>
        </div>
      )}

      {/* Documents Grid */}
      {documents.length > 0 && (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
          gap: '20px'
        }}>
          {documents.map((doc) => (
            <div
              key={doc.id}
              style={{
                background: 'white',
                border: '1px solid #e1e8ed',
                borderRadius: '12px',
                padding: '20px',
                transition: 'all 0.2s',
                cursor: 'pointer'
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
              </div>

              {/* Actions */}
              <div style={{ display: 'flex', gap: '10px', paddingTop: '15px', borderTop: '1px solid #e1e8ed' }}>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onDelete(doc.id);
                  }}
                  style={{
                    flex: 1,
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
      )}
    </div>
  );
};

export default DocumentsList;