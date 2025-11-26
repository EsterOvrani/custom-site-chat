// frontend/src/components/Dashboard/UploadDocumentModal.js
import React, { useState } from 'react';
import { documentAPI } from '../../services/api';

const UploadDocumentModal = ({ onClose, onComplete }) => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    
    if (!file) return;

    // Check if PDF
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      setError('ניתן להעלות רק קבצי PDF');
      return;
    }

    // Check file size (50MB max)
    if (file.size > 50 * 1024 * 1024) {
      setError('גודל מקסימלי לקובץ: 50MB');
      return;
    }

    setError('');
    setSelectedFile(file);
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

    setUploading(true);
    setError('');

    try {
      const response = await documentAPI.uploadDocument(selectedFile);

      if (response.data.success) {
        onComplete();
      } else {
        setError(response.data.error || 'שגיאה בהעלאת המסמך');
      }
    } catch (err) {
      console.error('Upload error:', err);
      if (err.response?.data?.error) {
        setError(err.response.data.error);
      } else {
        setError('שגיאה בהעלאת המסמך');
      }
    } finally {
      setUploading(false);
    }
  };


return (
  <div className="modal active">
    <div className="modal-content" style={{ maxWidth: '500px' }}>
      <h2 className="modal-header">📤 העלאת מסמך חדש</h2>

      {error && (
        <div style={{
          padding: '12px',
          background: '#fee',
          color: '#c33',
          border: '1px solid #fcc',
          borderRadius: '8px',
          marginBottom: '20px'
        }}>
          {error}
        </div>
      )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="fileInput" style={{ marginBottom: '15px', display: 'block' }}>
              בחר קובץ PDF:
            </label>

            <div style={{ marginBottom: '15px' }}>
              <label
                htmlFor="fileInput"
                style={{
                  display: 'inline-block',
                  padding: '12px 24px',
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  color: 'white',
                  borderRadius: '8px',
                  cursor: uploading ? 'not-allowed' : 'pointer',
                  fontWeight: 600,
                  opacity: uploading ? 0.5 : 1
                }}
              >
                📎 בחר קובץ
              </label>
              <input
                type="file"
                id="fileInput"
                accept=".pdf"
                onChange={handleFileChange}
                style={{ display: 'none' }}
                disabled={uploading}
              />
            </div>

            {selectedFile && (
              <div style={{
                padding: '15px',
                background: '#f8f9ff',
                border: '1px solid #667eea',
                borderRadius: '8px',
                display: 'flex',
                alignItems: 'center',
                gap: '12px'
              }}>
                <div style={{ fontSize: '2rem' }}>📄</div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 600, color: '#333' }}>
                    {selectedFile.name}
                  </div>
                  <div style={{ fontSize: '13px', color: '#666' }}>
                    {formatFileSize(selectedFile.size)}
                  </div>
                </div>
                {!uploading && (
                  <button
                    type="button"
                    onClick={() => setSelectedFile(null)}
                    style={{
                      background: 'none',
                      border: 'none',
                      fontSize: '20px',
                      cursor: 'pointer',
                      color: '#dc3545'
                    }}
                  >
                    ✕
                  </button>
                )}
              </div>
            )}

            <div style={{
              fontSize: '12px',
              color: '#666',
              marginTop: '10px',
              textAlign: 'right'
            }}>
              * ניתן להעלות קבצי PDF בלבד (מקסימום 50MB)
            </div>
          </div>

          <div className="modal-actions" style={{ marginTop: '25px' }}>
            <button
              type="button"
              className="btn-cancel"
              onClick={onClose}
              disabled={uploading}
            >
              ביטול
            </button>
            <button
              type="submit"
              className="btn-submit"
              disabled={uploading || !selectedFile}
            >
              {uploading ? '⏳ מעלה...' : '✓ העלה מסמך'}
            </button>
          </div>
        </form>

        {uploading && (
          <div style={{
            marginTop: '20px',
            textAlign: 'center',
            padding: '20px',
            background: '#f8f9ff',
            borderRadius: '8px'
          }}>
            <div className="spinner"></div>
            <p style={{ marginTop: '10px', color: '#666' }}>
              מעלה ומעבד מסמך...<br />
              <small>זה עשוי לקחת מספר שניות</small>
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default UploadDocumentModal;