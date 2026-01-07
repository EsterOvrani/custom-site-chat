import React, { useState } from 'react';
import { analyticsAPI } from '../../services/api';
import { Pie } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend
} from 'chart.js';

// רישום רכיבי Chart.js
ChartJS.register(ArcElement, Tooltip, Legend);

const Analytics = () => {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [analysis, setAnalysis] = useState(null);
  const [showAnalysis, setShowAnalysis] = useState(false);

  // הורדת קובץ
  const handleDownload = async () => {
    setLoading(true);
    setMessage('');

    try {
      const response = await analyticsAPI.downloadQuestions();
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.download = 'unanswered_questions.txt';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      setMessage('✅ הקובץ הורד בהצלחה');
    } catch (error) {
      console.error('Download error:', error);
      setMessage('❌ שגיאה בהורדת הקובץ');
    } finally {
      setLoading(false);
    }
  };

  // מחיקת שאלות
  const handleClear = async () => {
    if (!window.confirm('האם אתה בטוח שברצונך למחוק את כל השאלות?')) {
      return;
    }

    setLoading(true);
    setMessage('');

    try {
      await analyticsAPI.clearQuestions();
      setMessage('✅ השאלות נמחקו בהצלחה');
      setAnalysis(null);
      setShowAnalysis(false);
    } catch (error) {
      console.error('Clear error:', error);
      setMessage('❌ שגיאה במחיקת השאלות');
    } finally {
      setLoading(false);
    }
  };

  // 🆕 ניתוח חכם
  const handleAnalyze = async () => {
    setLoading(true);
    setMessage('');
    setShowAnalysis(false);

    try {
      const response = await analyticsAPI.analyzeQuestions();
      
      if (response.data.success) {
        setAnalysis(response.data.data);
        setShowAnalysis(true);
        setMessage('✅ ניתוח הושלם בהצלחה');
      } else {
        setMessage('❌ ' + (response.data.error || 'שגיאה בניתוח'));
      }
    } catch (error) {
      console.error('Analysis error:', error);
      setMessage('❌ שגיאה בניתוח השאלות');
    } finally {
      setLoading(false);
    }
  };

  // 🆕 הורדת קובץ מסודר
  const handleDownloadAnalysis = () => {
    if (!analysis) return;

    let content = '📊 ניתוח שאלות - Custom Site Chat\n';
    content += '=====================================\n\n';
    content += `סיכום: ${analysis.summary}\n`;
    content += `סה"כ שאלות: ${analysis.totalQuestions}\n\n`;

    analysis.categories.forEach(cat => {
      content += `\n${cat.icon} ${cat.categoryName} (${cat.totalCount} שאלות)\n`;
      content += '─────────────────────────────────────\n';
      
      cat.questions.forEach((q, idx) => {
        content += `${idx + 1}. ${q.question} × ${q.count}\n`;
      });
    });

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'analyzed_questions.txt';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  // 🆕 הכנת נתונים לגרף
  const getChartData = () => {
    if (!analysis) return null;

    return {
      labels: analysis.categories.map(cat => `${cat.icon} ${cat.categoryName}`),
      datasets: [{
        data: analysis.categories.map(cat => cat.totalCount),
        backgroundColor: [
          '#667eea',
          '#764ba2',
          '#f093fb',
          '#4facfe',
          '#43e97b',
          '#fa709a',
          '#fee140',
          '#30cfd0'
        ],
        borderWidth: 2,
        borderColor: '#fff'
      }]
    };
  };

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

      {/* הסבר */}
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

      {/* הודעות */}
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

      {/* כפתורים */}
      <div style={{ 
        display: 'flex', 
        gap: '15px', 
        flexWrap: 'wrap',
        marginBottom: '30px'
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

        {/* 🆕 כפתור ניתוח */}
        <button
          onClick={handleAnalyze}
          disabled={loading}
          style={{
            padding: '14px 28px',
            background: loading ? '#ccc' : 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
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
          {loading ? '⏳ מנתח...' : '🤖 ניתוח חכם עם AI'}
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
          {loading ? '⏳ מוחק...' : '🗑️ מחק שאלות'}
        </button>
      </div>

      {/* 🆕 תצוגת ניתוח */}
      {showAnalysis && analysis && (
        <div style={{
          background: '#f8f9ff',
          border: '2px solid #667eea',
          borderRadius: '12px',
          padding: '30px',
          marginTop: '30px'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '20px'
          }}>
            <h3 style={{ margin: 0, color: '#333' }}>📈 תוצאות הניתוח</h3>
            <button
              onClick={handleDownloadAnalysis}
              style={{
                padding: '10px 20px',
                background: '#28a745',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                fontWeight: 600,
                cursor: 'pointer',
                fontSize: '14px'
              }}
            >
              💾 הורד ניתוח
            </button>
          </div>

          {/* סיכום */}
          <div style={{
            background: 'white',
            padding: '20px',
            borderRadius: '8px',
            marginBottom: '25px',
            border: '1px solid #e1e8ed'
          }}>
            <strong style={{ color: '#667eea' }}>סיכום:</strong>
            <p style={{ margin: '10px 0 0 0', color: '#666' }}>{analysis.summary}</p>
            <p style={{ margin: '10px 0 0 0', color: '#999', fontSize: '14px' }}>
              סה"כ {analysis.totalQuestions} שאלות נותחו
            </p>
          </div>

          {/* גרף */}
          <div style={{
            background: 'white',
            padding: '30px',
            borderRadius: '8px',
            marginBottom: '25px',
            display: 'flex',
            justifyContent: 'center',
            border: '1px solid #e1e8ed'
          }}>
            <div style={{ width: '400px', height: '400px' }}>
              <Pie
                data={getChartData()}
                options={{
                  responsive: true,
                  maintainAspectRatio: true,
                  plugins: {
                    legend: {
                      position: 'bottom',
                      labels: {
                        font: { size: 14 },
                        padding: 15
                      }
                    },
                    tooltip: {
                      callbacks: {
                        label: (context) => {
                          const label = context.label || '';
                          const value = context.parsed || 0;
                          const total = context.dataset.data.reduce((a, b) => a + b, 0);
                          const percentage = ((value / total) * 100).toFixed(1);
                          return `${label}: ${value} שאלות (${percentage}%)`;
                        }
                      }
                    }
                  }
                }}
              />
            </div>
          </div>

          {/* רשימת קטגוריות */}
          <div>
            {analysis.categories.map((category, idx) => (
              <div
                key={idx}
                style={{
                  background: 'white',
                  border: '1px solid #e1e8ed',
                  borderRadius: '8px',
                  padding: '20px',
                  marginBottom: '15px'
                }}
              >
                <h4 style={{
                  color: '#333',
                  marginBottom: '15px',
                  fontSize: '18px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px'
                }}>
                  <span style={{ fontSize: '24px' }}>{category.icon}</span>
                  {category.categoryName}
                  <span style={{
                    background: '#667eea',
                    color: 'white',
                    padding: '4px 12px',
                    borderRadius: '12px',
                    fontSize: '14px',
                    fontWeight: 600
                  }}>
                    {category.totalCount}
                  </span>
                </h4>

                <ul style={{
                  listStyle: 'none',
                  padding: 0,
                  margin: 0
                }}>
                  {category.questions.map((q, qIdx) => (
                    <li
                      key={qIdx}
                      style={{
                        padding: '12px',
                        background: '#f8f9ff',
                        borderRadius: '6px',
                        marginBottom: '8px',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center'
                      }}
                    >
                      <span style={{ color: '#333' }}>{q.question}</span>
                      <span style={{
                        background: '#764ba2',
                        color: 'white',
                        padding: '4px 10px',
                        borderRadius: '10px',
                        fontSize: '13px',
                        fontWeight: 600,
                        minWidth: '40px',
                        textAlign: 'center'
                      }}>
                        × {q.count}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* הוראות */}
      <div style={{
        background: '#fff3cd',
        border: '1px solid #ffc107',
        borderRadius: '10px',
        padding: '15px',
        marginTop: '25px'
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
    </div>
  );
};

export default Analytics;