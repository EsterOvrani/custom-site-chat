import React, { useState } from 'react';
import axios from 'axios';


const UploadDocumentModal = ({ onClose, onComplete }) => {
  const [selectedFiles, setSelectedFiles] = useState([]); // ⭐ שינוי: array של קבצים
  const [error, setError] = useState('');
  const [uploading, setUploading] = useState(false);

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B';
    else if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    else return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const handleFileSelect = (e) => {
    const files = Array.from(e.target.files); // ⭐ המרה ל-array
    
    // ⭐ ולידציה לכל קובץ
    const validFiles = [];
    let hasError = false;

    for (const file of files) {
      // בדיקת סוג קובץ
      if (file.type !== 'application/pdf') {
        setError(`הקובץ "${file.name}" אינו PDF. ניתן להעלות רק קבצי PDF.`);
        hasError = true;
        break;
      }

      // בדיקת גודל (50MB לכל קובץ)
      const maxSize = 50 * 1024 * 1024;
      if (file.size > maxSize) {
        setError(`הקובץ "${file.name}" גדול מדי. גודל מקסימלי: 50MB`);
        hasError = true;
        break;
      }

      validFiles.push(file);
    }

    if (!hasError) {
      setSelectedFiles(validFiles);
      setError('');
    } else {
      setSelectedFiles([]);
      e.target.value = ''; // איפוס input
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (selectedFiles.length === 0) {
      setError('נא לבחור לפחות קובץ אחד');
      return;
    }

    setUploading(true);
    setError('');

    // ⭐ לולאה על כל הקבצים
    const uploadPromises = selectedFiles.map(async (file) => {
      // יצירת placeholder לכל קובץ
      const placeholderId = `temp-${Date.now()}-${Math.random()}`;
      const placeholder = {
        id: placeholderId,
        originalFileName: file.name,
        fileSize: file.size,
        fileSizeFormatted: formatFileSize(file.size),
        processingStatus: 'PENDING',
        processingProgress: 5,
        processingStage: 'UPLOADING',
        processingStageDescription: 'מעלה לשרת...',
        createdAt: new Date().toISOString(),
        active: true,
        isPlaceholder: true
      };

      // ⭐ הוסף placeholder מיד
      if (onComplete) {
        onComplete(placeholder);
      }

      // העלאה בפועל
      try {
        const formData = new FormData();
        formData.append('file', file);

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

        console.log(`📥 [${file.name}] Server response:`, response.data);

        if (response.data.success && response.data.document) {
          const serverDoc = response.data.document;
          
          console.log(`✅ [${file.name}] Upload successful, replacing placeholder`);
          
          // ⭐ החלף placeholder במסמך אמיתי
          if (onComplete) {
            onComplete(serverDoc, placeholderId);
          }

          return { success: true, fileName: file.name };
        } else {
          console.error(`❌ [${file.name}] Upload failed - no document in response`);
          
          // ⭐ הסר placeholder
          if (onComplete) {
            onComplete(null, placeholderId);
          }

          return { success: false, fileName: file.name, error: 'שגיאה לא ידועה' };
        }

      } catch (err) {
        console.error(`❌ [${file.name}] Upload error:`, err);

        // ⭐ הסר placeholder
        if (onComplete) {
          onComplete(null, placeholderId);
        }

        return { 
          success: false, 
          fileName: file.name, 
          error: err.response?.data?.message || err.message 
        };
      }
    });

    // ⭐ המתן לסיום כל ההעלאות
    try {
      const results = await Promise.all(uploadPromises);
      
      const successCount = results.filter(r => r.success).length;
      const failCount = results.filter(r => !r.success).length;

      console.log(`📊 Upload summary: ${successCount} succeeded, ${failCount} failed`);

      if (failCount > 0) {
        const failedFiles = results
          .filter(r => !r.success)
          .map(r => r.fileName)
          .join(', ');
        
        setError(`נכשל העלאת הקבצים: ${failedFiles}`);
      }

      // ⭐ סגור את המודל רק אחרי שהכל נשלח
      if (successCount > 0) {
        setTimeout(() => {
          onClose();
        }, 500);
      }

    } catch (err) {
      console.error('❌ Upload process error:', err);
      setError('שגיאה כללית בתהליך ההעלאה');
    } finally {
      setUploading(false);
    }
  };

  const handleRemoveFile = (index) => {
    const newFiles = selectedFiles.filter((_, i) => i !== index);
    setSelectedFiles(newFiles);
    
    // אם לא נשארו קבצים, אפס את ה-input
    if (newFiles.length === 0) {
      const fileInput = document.getElementById('file-input');
      if (fileInput) fileInput.value = '';
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>📄 העלאת מסמכים</h2>
          <button className="close-button" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="file-input-wrapper">
            <label htmlFor="file-input" className="file-input-label">
              {selectedFiles.length === 0 ? (
                <>
                  <span className="upload-icon">📁</span>
                  <span>בחר קובץ אחד או יותר (PDF)</span>
                  <span className="file-input-hint">גודל מקסימלי: 50MB לכל קובץ</span>
                </>
              ) : (
                <>
                  <span className="upload-icon">✅</span>
                  <span>{selectedFiles.length} קבצים נבחרו</span>
                  <span className="file-input-hint">לחץ לבחירת קבצים נוספים</span>
                </>
              )}
            </label>
            <input
              id="file-input"
              type="file"
              accept="application/pdf"
              onChange={handleFileSelect}
              multiple  // ⭐ זה מאפשר בחירה מרובה!
              disabled={uploading}
            />
          </div>

          {/* ⭐ רשימת הקבצים שנבחרו */}
          {selectedFiles.length > 0 && (
            <div className="selected-files-list">
              <h3>קבצים נבחרים:</h3>
              {selectedFiles.map((file, index) => (
                <div key={index} className="selected-file-item">
                  <div className="file-info">
                    <span className="file-name">📄 {file.name}</span>
                    <span className="file-size">{formatFileSize(file.size)}</span>
                  </div>
                  {!uploading && (
                    <button
                      type="button"
                      className="remove-file-button"
                      onClick={() => handleRemoveFile(index)}
                      title="הסר קובץ"
                    >
                      ✕
                    </button>
                  )}
                </div>
              ))}
              <div className="total-size">
                סה"כ: {formatFileSize(selectedFiles.reduce((sum, f) => sum + f.size, 0))}
              </div>
            </div>
          )}

          {error && (
            <div className="error-message">
              ⚠️ {error}
            </div>
          )}

          <div className="modal-actions">
            <button
              type="button"
              onClick={onClose}
              className="cancel-button"
              disabled={uploading}
            >
              ביטול
            </button>
            <button
              type="submit"
              className="upload-button"
              disabled={uploading || selectedFiles.length === 0}
            >
              {uploading ? (
                <>
                  <span className="spinner"></span>
                  מעלה {selectedFiles.length} קבצים...
                </>
              ) : (
                `העלה ${selectedFiles.length} קבצים`
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UploadDocumentModal;