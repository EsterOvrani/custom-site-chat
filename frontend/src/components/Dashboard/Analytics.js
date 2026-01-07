// frontend/src/components/Dashboard/Analytics.js
import React, { useState } from 'react';
import { analyticsAPI } from '../../services/api';

const Analytics = () => {
  // 📊 State management
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  /**
   * ⬇️ הורדת קובץ השאלות
   */
  const handleDownload = async () => {
    setLoading(true);
    setMessage('');

    try {
      // 📥 קריאה ל-API דרך api.js
      const response = await analyticsAPI.downloadQuestions();

      // 💾 יצירת קישור להורדה
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.download = 'unanswered_questions.txt';
      document.body.appendChild(link);
      link.click(); // הורדה אוטומטית
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url); // ניקוי

      setMessage('✅ הקובץ הורד בהצלחה');
      
    } catch (error) {
      console.error('Download error:', error);
      setMessage('❌ שגיאה בהורדת הקובץ');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 🗑️ מחיקת כל השאלות
   */
  const handleClear = async () => {
    // ⚠️ אישור מהמשתמש
    if (!window.confirm('האם אתה בטוח שברצונך למחוק את כל השאלות?')) {
      return;
    }

    setLoading(true);
    setMessage('');

    try {
      // 🗑️ קריאה ל-API דרך api.js
      await analyticsAPI.clearQuestions();

      setMessage('✅ השאלות נמחקו בהצלחה');
      
    } catch (error) {
      console.error('Clear error:', error);
      setMessage('❌ שגיאה במחיקת השאלות');
    } finally {
      setLoading(false);
    }
  };

  // 🎨 הממשק
  return (
    <div style={{
      background: 'white',
      borderRadius: '12px',
      padding: '30px',
      minHeight: '400px'
    }}>
      <h2 style={{ marginBottom: '20px', color: '#333' }}>
        📊 Analytics - שאלות ללא תשובה
      </h2>

      {/* 💡 הסבר איך זה עובד */}
      <div style={{
        background: '#e8f0fe',
        border: '1px solid #667eea',
        borderRadius: '10px',
        padding: '20px',
        marginBottom: '30px'
      }}>
        <h3 style={{ 
          color: '#667eea', 
          marginBottom: '15px',
          fontSize: '16px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px'
        }}>
          💡 איך זה עובד?
        </h3>
        <ul style={{ 
          color: '#666',
          lineHeight: '1.8',
          paddingRight: '20px'
        }}>
          <li>המערכת אוספת אוטומטית שאלות שהבוט לא מצא עליהן תשובה</li>
          <li>השאלות מסוננות לפי הקטגוריה שהגדרת בקוד ההטמעה</li>
          <li>רק שאלות רלוונטיות לנושא האתר נשמרות</li>
          <li>כך תדע איזה מידע נוסף כדאי להעלות למאגר!</li>
        </ul>
      </div>

      {/* ⚙️ הוראות הגדרה */}
      <div style={{
        background: '#fff3cd',
        border: '1px solid #ffc107',
        borderRadius: '10px',
        padding: '15px',
        marginBottom: '25px'
      }}>
        <strong style={{ color: '#856404' }}>
          ⚙️ להגדרת קטגוריית האתר:
        </strong>
        <p style={{ 
          color: '#856404', 
          marginTop: '10px',
          fontSize: '14px',
          lineHeight: '1.6'
        }}>
          בקוד ההטמעה, הוסף את השורה:<br/>
          <code style={{ 
            background: 'white',
            padding: '4px 8px',
            borderRadius: '4px',
            display: 'inline-block',
            marginTop: '8px',
            direction: 'ltr'
          }}>
            window.CHAT_WIDGET_SITE_CATEGORY = 'אתר מכירת בגדים';
          </code>
        </p>
      </div>

      {/* ✅ הודעות */}
      {message && (
        <div style={{
          padding: '15px',
          background: message.includes('❌') ? '#fee' : '#efe',
          border: `1px solid ${message.includes('❌') ? '#fcc' : '#cfc'}`,
          borderRadius: '8px',
          marginBottom: '20px'
        }}>
          {message}
        </div>
      )}

      {/* 🔘 כפתורים */}
      <div style={{ 
        display: 'flex', 
        gap: '15px', 
        flexWrap: 'wrap' 
      }}>
        <button
          onClick={handleDownload}
          disabled={loading}
          style={{
            padding: '14px 28px',
            background: loading ? '#ccc' : 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            fontWeight: 600,
            cursor: loading ? 'not-allowed' : 'pointer',
            fontSize: '16px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            transition: 'all 0.3s ease'
          }}
        >
          {loading ? '⏳ טוען...' : '⬇️ הורד קובץ שאלות'}
        </button>

        <button
          onClick={handleClear}
          disabled={loading}
          style={{
            padding: '14px 28px',
            background: loading ? '#ccc' : '#dc3545',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            fontWeight: 600,
            cursor: loading ? 'not-allowed' : 'pointer',
            fontSize: '16px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            transition: 'all 0.3s ease'
          }}
        >
          {loading ? '⏳ מוחק...' : '🗑️ מחק את כל השאלות'}
        </button>
      </div>

      {/* 📝 דוגמה לפורמט */}
      <div style={{
        marginTop: '30px',
        padding: '20px',
        background: '#f8f9ff',
        border: '1px solid #e1e8ed',
        borderRadius: '10px'
      }}>
        <h4 style={{ 
          color: '#333', 
          marginBottom: '15px',
          fontSize: '15px'
        }}>
          📝 פורמט הקובץ
        </h4>
        <pre style={{
          background: 'white',
          padding: '15px',
          borderRadius: '6px',
          fontSize: '13px',
          color: '#666',
          overflow: 'auto',
          border: '1px solid #e1e8ed'
        }}>
{`שאלה 1
האם יש לכם מידות XL?

שאלה 2
מה זמני המשלוח?

שאלה 3
יש אפשרות להחזרה?`}
        </pre>
      </div>
    </div>
  );
};

export default Analytics;