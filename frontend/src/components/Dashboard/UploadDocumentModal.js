import React, { useState } from 'react';
import axios from 'axios';

const UploadDocumentModal = ({ onClose, onComplete }) => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [error, setError] = useState('');

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    
    if (!file) return;

    // בדיקת סוג קובץ
    if (file.type !== 'application/pdf') {
      setError('ניתן להעלות רק קבצי PDF');
      return;
    }

    // בדיקת גודל (50MB)
    if (file.size > 50 * 1024 * 1024) {
      setError('גודל הקובץ חורג מ-50MB');
      return;
    }

    setSelectedFile(file);
    setError('');
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!selectedFile) {
      setError('נא לבחור קובץ');
      return;
    }

    // ⭐ צור placeholder מיד
    const placeholderId = `temp-${Date.now()}`;
    const placeholder = {
      id: placeholderId,
      originalFileName: selectedFile.name,
      fileSize: selectedFile.size,
      fileSizeFormatted: formatFileSize(selectedFile.size),
      processingStatus: 'PENDING',
      processingProgress: 5,
      processingStage: 'UPLOADING',
      processingStageDescription: 'מעלה לשרת...',
      createdAt: new Date().toISOString(),
      active: true,
      isPlaceholder: true
    };

    // ⭐ סגור את המודל מיד
    onClose();

    // ⭐ הוסף placeholder לרשימה
    if (onComplete) {
      onComplete(placeholder);
    }

    // ⭐ שלח את הקובץ ברקע
    try {
      const formData = new FormData();
      formData.append('file', selectedFile);

      const token = localStorage.getItem('token');
      
      const response = await axios.post(
        '/api/documents/upload', 
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
            'Authorization': `Bearer ${token}`
          }
        }
      );

      if (response.data.success && response.data.document) {
        console.log('✅ Upload successful, replacing placeholder');
        
        // ⭐ החלף את ה-placeholder עם המסמך האמיתי
        if (onComplete) {
          onComplete(response.data.document, placeholderId);
        }
      } else {
        console.error('❌ Upload failed, removing placeholder');
        
        // ⭐ הסר את ה-placeholder
        if (onComplete) {
          onComplete(null, placeholderId);
        }
      }
    } catch (err) {
      console.error('❌ Upload error:', err);
      
      // ⭐ הסר את ה-placeholder
      if (onComplete) {
        onComplete(null, placeholderId);
      }
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>📄 העלאת מסמך חדש</h2>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <div className="file-upload-container">
              <input
                type="file"
                id="file-input"
                accept=".pdf"
                onChange={handleFileSelect}
                style={{ display: 'none' }}
              />
              
              <label
                htmlFor="file-input"
                className="file-upload-label"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  padding: '40px',
                  border: '2px dashed #007bff',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  backgroundColor: selectedFile ? '#f0f8ff' : '#fafafa',
                  transition: 'all 0.3s ease'
                }}
              >
                {selectedFile ? (
                  <>
                    <div style={{ fontSize: '48px', marginBottom: '10px' }}>
                      📄
                    </div>
                    <div style={{ 
                      fontSize: '16px', 
                      fontWeight: 600,
                      marginBottom: '5px',
                      color: '#333'
                    }}>
                      {selectedFile.name}
                    </div>
                    <div style={{ fontSize: '14px', color: '#666' }}>
                      {formatFileSize(selectedFile.size)}
                    </div>
                    <div style={{ 
                      marginTop: '15px',
                      fontSize: '13px',
                      color: '#007bff'
                    }}>
                      לחץ לבחירת קובץ אחר
                    </div>
                  </>
                ) : (
                  <>
                    <div style={{ fontSize: '48px', marginBottom: '10px' }}>
                      📁
                    </div>
                    <div style={{ fontSize: '16px', fontWeight: 600, marginBottom: '5px' }}>
                      לחץ לבחירת קובץ
                    </div>
                    <div style={{ fontSize: '14px', color: '#666' }}>
                      או גרור קובץ לכאן
                    </div>
                  </>
                )}
              </label>

              {error && (
                <div className="error-message" style={{
                  marginTop: '15px',
                  padding: '10px',
                  backgroundColor: '#fee',
                  color: '#c33',
                  borderRadius: '5px',
                  fontSize: '14px'
                }}>
                  ⚠️ {error}
                </div>
              )}
            </div>

            <div style={{ 
              marginTop: '20px', 
              padding: '12px', 
              background: '#f8f9fa', 
              borderRadius: '6px',
              fontSize: '13px',
              color: '#666'
            }}>
              * ניתן להעלות קבצי PDF בלבד (מקסימום 50MB)
            </div>
          </div>

          <div className="modal-actions" style={{ marginTop: '25px' }}>
            <button
              type="button"
              className="btn-cancel"
              onClick={onClose}
            >
              ביטול
            </button>
            <button
              type="submit"
              className="btn-submit"
              disabled={!selectedFile}
            >
              ✓ העלה מסמך
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UploadDocumentModal;