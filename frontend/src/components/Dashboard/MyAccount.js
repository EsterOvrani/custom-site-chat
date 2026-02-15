// frontend/src/components/Dashboard/MyAccount.js
import React from 'react';
import tokenSSEService from '../../services/tokenSSE';
import './MyAccount.css';

const MyAccount = ({ tokenInfo, loading, currentUser }) => {
  const [isConnected, setIsConnected] = React.useState(false);
  const [lastUpdated, setLastUpdated] = React.useState(new Date());
  const [isAnimating, setIsAnimating] = React.useState(false);
  
  // בדיקת מצב חיבור כל שנייה
  React.useEffect(() => {
    const checkConnection = setInterval(() => {
      setIsConnected(tokenSSEService.isConnected());
    }, 1000);
    
    return () => clearInterval(checkConnection);
  }, []);

  // עדכון זמן כשהטוקנים משתנים
  React.useEffect(() => {
    if (tokenInfo) {
      setLastUpdated(new Date());
      setIsAnimating(true);
      setTimeout(() => setIsAnimating(false), 600);
    }
  }, [tokenInfo]);

  // חישוב צבע לפי אחוז השימוש
  const getProgressColor = (percentage) => {
    if (percentage < 50) return '#10b981'; // ירוק
    if (percentage < 75) return '#f59e0b'; // כתום
    return '#ef4444'; // אדום
  };

  // פורמט מספרים עם פסיקים
  const formatNumber = (num) => {
    return num?.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',') || '0';
  };

  // חישוב עלות משוערת (לדוגמה - ניתן להתאים)
  const calculateCost = (used) => {
    // עלות משוערת: $0.003 לכל 1000 טוקנים (דוגמה)
    const costPer1000 = 0.003;
    return ((used / 1000) * costPer1000).toFixed(4);
  };

  if (loading || !tokenInfo) {
    return (
      <div className="my-account-container">
        <h2>👤 החשבון שלי</h2>
        <div className="loading-container">
          <div className="spinner"></div>
          <p>טוען נתונים...</p>
        </div>
      </div>
    );
  }

  const { quota, used, remaining, usagePercentage } = tokenInfo;

  return (
    <div className="my-account-container">
      <h2>👤 החשבון שלי</h2>

      {/* פרטי משתמש */}
      <div className="account-section user-info-section">
        <h3>📋 פרטי משתמש</h3>
        <div className="info-grid">
          <div className="info-item">
            <span className="info-label">שם מלא:</span>
            <span className="info-value">{currentUser?.fullName || '-'}</span>
          </div>
          <div className="info-item">
            <span className="info-label">אימייל:</span>
            <span className="info-value">{currentUser?.email || '-'}</span>
          </div>
          <div className="info-item">
            <span className="info-label">שם משתמש:</span>
            <span className="info-value">{currentUser?.username || '-'}</span>
          </div>
        </div>
      </div>

      {/* מכסת טוקנים */}
      <div className="account-section token-section">
        <div className="section-header">
          <h3>💰 מכסת טוקנים</h3>
          <div className="live-status">
            {isConnected && (
              <span className="live-indicator" title="מחובר לעדכונים בזמן אמת">
                🟢 Live
              </span>
            )}
            <span className="last-updated">
              עודכן: {lastUpdated.toLocaleTimeString('he-IL', { 
                hour: '2-digit', 
                minute: '2-digit',
                second: '2-digit'
              })}
            </span>
          </div>
        </div>

        {/* Progress Bar גדול */}
        <div className="token-progress-container">
          <div className="token-progress-bar large">
            <div 
              className="token-progress-fill"
              style={{ 
                width: `${Math.min(usagePercentage, 100)}%`,
                backgroundColor: getProgressColor(usagePercentage)
              }}
            />
          </div>
          <div className="progress-labels">
            <span>0</span>
            <span 
              className="progress-percentage"
              style={{ color: getProgressColor(usagePercentage) }}
            >
              {usagePercentage.toFixed(1)}% בשימוש
            </span>
            <span>{formatNumber(quota)}</span>
          </div>
        </div>

        {/* סטטיסטיקות טוקנים */}
        <div className="token-stats-grid">
          <div className="token-stat-card">
            <div className="stat-icon">📊</div>
            <div className="stat-content">
              <div className="stat-label">סה"כ מכסה</div>
              <div className="stat-value">{formatNumber(quota)}</div>
              <div className="stat-sublabel">טוקנים</div>
            </div>
          </div>

          <div className="token-stat-card">
            <div className="stat-icon">📈</div>
            <div className="stat-content">
              <div className="stat-label">נוצלו</div>
              <div className={`stat-value ${isAnimating ? 'animating' : ''}`}>
                {formatNumber(used)}
              </div>
              <div className="stat-sublabel">טוקנים</div>
            </div>
          </div>

          <div className="token-stat-card highlight">
            <div className="stat-icon">✨</div>
            <div className="stat-content">
              <div className="stat-label">נותרו</div>
              <div 
                className={`stat-value ${isAnimating ? 'animating' : ''}`}
                style={{ color: getProgressColor(usagePercentage) }}
              >
                {formatNumber(remaining)}
              </div>
              <div className="stat-sublabel">טוקנים</div>
            </div>
          </div>

          <div className="token-stat-card cost-card">
            <div className="stat-icon">💵</div>
            <div className="stat-content">
              <div className="stat-label">עלות משוערת</div>
              <div className="stat-value">${calculateCost(used)}</div>
              <div className="stat-sublabel">עד כה</div>
            </div>
          </div>
        </div>

        {/* אזהרות */}
        {usagePercentage > 90 && usagePercentage < 100 && (
          <div className="token-warning">
            ⚠️ מכסת הטוקנים קרובה להסתיים - נותרו רק {formatNumber(remaining)} טוקנים
          </div>
        )}
        
        {remaining === 0 && (
          <div className="token-error">
            ❌ מכסת הטוקנים הסתיימה - צור קשר לחידוש המנוי
          </div>
        )}

        {/* טיפ לייעול */}
        <div className="optimization-tip">
          <h4>💡 טיפ לייעול עלויות</h4>
          <p>
            כדי לחסוך בטוקנים, מומלץ להשתמש במסמכים ממוקדים וקצרים יותר. 
            מסמכים ארוכים צורכים יותר טוקנים בכל שאילתה.
          </p>
        </div>
      </div>
    </div>
  );
};

export default MyAccount;
