// frontend/src/components/Dashboard/TokenUsage.js
import React from 'react';
import tokenSSEService from '../../services/tokenSSE';
import './TokenUsage.css';

const TokenUsage = ({ tokenInfo, loading }) => {
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
      
      // הפעלת אנימציה
      setIsAnimating(true);
      setTimeout(() => setIsAnimating(false), 600);
    }
  }, [tokenInfo]);

  if (loading || !tokenInfo) {
    return (
      <div className="token-usage-card loading">
        <div className="token-header">
          <h3>💰 מכסת טוקנים</h3>
        </div>
        <div className="loading-spinner"></div>
      </div>
    );
  }

  const { quota, used, remaining, usagePercentage } = tokenInfo;
  
  // חישוב צבע לפי אחוז השימוש
  const getProgressColor = (percentage) => {
    if (percentage < 50) return '#10b981'; // ירוק
    if (percentage < 75) return '#f59e0b'; // כתום
    return '#ef4444'; // אדום
  };

  // פורמט מספרים עם פסיקים
  const formatNumber = (num) => {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  };

  return (
    <div className="token-usage-card">
      <div className="token-header">
        <div className="token-header-left">
          <h3>💰 מכסת טוקנים</h3>
          {isConnected && (
            <span className="live-indicator" title="מחובר לעדכונים בזמן אמת">
              🟢 Live
            </span>
          )}
        </div>
        <div className="token-header-right">
          <span className="token-percentage" style={{ color: getProgressColor(usagePercentage) }}>
            {usagePercentage.toFixed(1)}% בשימוש
          </span>
          <span className="last-updated">
            עודכן: {lastUpdated.toLocaleTimeString('he-IL', { 
              hour: '2-digit', 
              minute: '2-digit',
              second: '2-digit'
            })}
          </span>
        </div>
      </div>

      <div className="token-stats">
        <div className="token-stat">
          <div className="stat-label">נותרו</div>
          <div 
            className={`stat-value ${isAnimating ? 'animating' : ''}`}
            style={{ color: getProgressColor(usagePercentage) }}
          >
            {formatNumber(remaining)}
          </div>
        </div>
        
        <div className="token-stat">
          <div className="stat-label">נוצלו</div>
          <div className={`stat-value secondary ${isAnimating ? 'animating' : ''}`}>
            {formatNumber(used)}
          </div>
        </div>
        
        <div className="token-stat">
          <div className="stat-label">סך הכל</div>
          <div className="stat-value secondary">
            {formatNumber(quota)}
          </div>
        </div>
      </div>

      <div className="token-progress-bar">
        <div 
          className="token-progress-fill"
          style={{ 
            width: `${Math.min(usagePercentage, 100)}%`,
            backgroundColor: getProgressColor(usagePercentage)
          }}
        />
      </div>

      {usagePercentage > 90 && usagePercentage < 100 && (
        <div className="token-warning">
          ⚠️ מכסת הטוקנים קרובה להסתיים
        </div>
      )}
      
      {remaining === 0 && (
        <div className="token-error">
          ❌ מכסת הטוקנים הסתיימה - צור קשר לחידוש
        </div>
      )}
    </div>
  );
};

export default TokenUsage;