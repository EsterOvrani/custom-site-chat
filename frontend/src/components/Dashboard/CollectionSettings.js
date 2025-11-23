// frontend/src/components/Dashboard/CollectionSettings.js
import React, { useState } from 'react';

const CollectionSettings = ({ collection, onRegenerateKey, loading }) => {
  const [copiedEmbed, setCopiedEmbed] = useState(false);
  const [copiedKey, setCopiedKey] = useState(false);

  const copyToClipboard = (text, setCopied) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  return (
    <div className="collection-settings-container">
      <h2 style={{ marginBottom: '30px', color: '#333' }}>⚙️ הגדרות קולקשן וקוד הטמעה</h2>

      {/* Collection Info */}
      <div style={{
        background: 'white',
        border: '1px solid #e1e8ed',
        borderRadius: '12px',
        padding: '25px',
        marginBottom: '25px'
      }}>
        <h3 style={{ marginBottom: '20px', color: '#667eea' }}>📊 מידע על הקולקשן</h3>
        
        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            padding: '12px',
            background: '#f8f9ff',
            borderRadius: '8px'
          }}>
            <span style={{ fontWeight: 600, color: '#555' }}>שם קולקשן:</span>
            <span style={{ fontFamily: 'monospace', color: '#333' }}>
              {collection.collectionName}
            </span>
          </div>

          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            padding: '12px',
            background: '#f8f9ff',
            borderRadius: '8px'
          }}>
            <span style={{ fontWeight: 600, color: '#555' }}>נוצר בתאריך:</span>
            <span style={{ color: '#333' }}>
              {new Date(collection.createdAt).toLocaleDateString('he-IL')}
            </span>
          </div>
        </div>
      </div>

      {/* Secret Key */}
      <div style={{
        background: 'white',
        border: '2px solid #ffc107',
        borderRadius: '12px',
        padding: '25px',
        marginBottom: '25px'
      }}>
        <h3 style={{ marginBottom: '15px', color: '#ffc107' }}>🔑 Secret Key</h3>
        <p style={{ color: '#666', marginBottom: '20px', fontSize: '14px' }}>
          המפתח הזה נדרש כדי שה-Widget יוכל לתקשר עם המסמכים שלך. <strong>שמור אותו במקום בטוח!</strong>
        </p>

        <div style={{
          display: 'flex',
          gap: '10px',
          alignItems: 'center'
        }}>
          <input
            type="text"
            value={collection.secretKey}
            readOnly
            style={{
              flex: 1,
              padding: '12px',
              border: '1px solid #e1e8ed',
              borderRadius: '8px',
              fontFamily: 'monospace',
              background: '#f8f9ff',
              fontSize: '14px'
            }}
          />
          <button
            onClick={() => copyToClipboard(collection.secretKey, setCopiedKey)}
            style={{
              padding: '12px 20px',
              background: copiedKey ? '#28a745' : '#667eea',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: 600,
              transition: 'background 0.2s'
            }}
          >
            {copiedKey ? '✓ הועתק' : '📋 העתק'}
          </button>
          <button
            onClick={onRegenerateKey}
            disabled={loading}
            style={{
              padding: '12px 20px',
              background: '#dc3545',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: 600,
              opacity: loading ? 0.5 : 1
            }}
          >
            🔄 צור מפתח חדש
          </button>
        </div>

        <div style={{
          marginTop: '15px',
          padding: '12px',
          background: '#fff3cd',
          border: '1px solid #ffc107',
          borderRadius: '8px',
          fontSize: '13px',
          color: '#856404'
        }}>
          ⚠️ <strong>אזהרה:</strong> יצירת מפתח חדש תבטל את המפתח הישן. כל widget שמשתמש במפתח הישן יפסיק לעבוד.
        </div>
      </div>

      {/* Embed Code */}
      <div style={{
        background: 'white',
        border: '2px solid #667eea',
        borderRadius: '12px',
        padding: '25px'
      }}>
        <h3 style={{ marginBottom: '15px', color: '#667eea' }}>🎨 קוד הטמעה (Embed Code)</h3>
        <p style={{ color: '#666', marginBottom: '20px', fontSize: '14px' }}>
          העתק את הקוד הזה והדבק אותו באתר שלך, ממש לפני תג <code>&lt;/body&gt;</code>
        </p>

        <div style={{ position: 'relative' }}>
          <pre style={{
            background: '#1e1e1e',
            color: '#d4d4d4',
            padding: '20px',
            borderRadius: '8px',
            overflow: 'auto',
            fontSize: '13px',
            fontFamily: 'monospace',
            maxHeight: '300px',
            lineHeight: '1.6'
          }}>
            {collection.embedCode}
          </pre>
          
          <button
            onClick={() => copyToClipboard(collection.embedCode, setCopiedEmbed)}
            style={{
              position: 'absolute',
              top: '15px',
              left: '15px',
              padding: '8px 16px',
              background: copiedEmbed ? '#28a745' : '#667eea',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              fontWeight: 600,
              fontSize: '13px',
              transition: 'background 0.2s'
            }}
          >
            {copiedEmbed ? '✓ הועתק' : '📋 העתק קוד'}
          </button>
        </div>

        <div style={{
          marginTop: '20px',
          padding: '15px',
          background: '#e8f0fe',
          border: '1px solid #667eea',
          borderRadius: '8px',
          fontSize: '14px',
          color: '#1565c0'
        }}>
          💡 <strong>טיפ:</strong> אחרי הדבקת הקוד, תראה בועה כחולה בפינת המסך. לחיצה עליה תפתח את חלון הצ'אט.
        </div>
      </div>

      {/* Usage Example */}
      <div style={{
        marginTop: '25px',
        background: '#f8f9ff',
        border: '1px solid #e1e8ed',
        borderRadius: '12px',
        padding: '20px'
      }}>
        <h4 style={{ marginBottom: '15px', color: '#333' }}>📖 דוגמת שימוש</h4>
        <pre style={{
          background: 'white',
          padding: '15px',
          borderRadius: '6px',
          fontSize: '13px',
          fontFamily: 'monospace',
          overflow: 'auto',
          border: '1px solid #e1e8ed'
        }}>
{`<!DOCTYPE html>
<html>
<head>
  <title>האתר שלי</title>
</head>
<body>
  
  <!-- התוכן של האתר שלך כאן -->
  <h1>ברוכים הבאים לאתר שלי</h1>
  
  <!-- 👇 הדבק את קוד ה-Widget כאן -->
  ${collection.embedCode}
  
</body>
</html>`}
        </pre>
      </div>
    </div>
  );
};

export default CollectionSettings;