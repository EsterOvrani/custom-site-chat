// frontend/src/components/Dashboard/Customization.js
import React, { useState, useEffect } from 'react';
import './Customization.css';

const Customization = ({ collection, onRegenerateKey, loading, onSettingsChange }) => {
  const [copiedKey, setCopiedKey] = useState(false);
  const [settings, setSettings] = useState({
    chatTitle: 'צ\'אט חכם',
    botName: 'עוזר',
    primaryColor: '#667eea',
    welcomeMessage: 'שלום! איך אוכל לעזור לך היום?',
    placeholder: 'הקלד את שאלתך כאן...',
    position: 'bottom-right',
    botAvatar: '',
    showPoweredBy: true
  });

  // טעינת הגדרות משמורות (אם יש)
  useEffect(() => {
    const savedSettings = localStorage.getItem('chatWidgetSettings');
    if (savedSettings) {
      try {
        const parsed = JSON.parse(savedSettings);
        const newSettings = { ...settings, ...parsed };
        setSettings(newSettings);
        // שליחת ההגדרות ל-parent
        if (onSettingsChange) {
          onSettingsChange(newSettings);
        }
      } catch (e) {
        console.error('Error loading saved settings:', e);
      }
    }
  }, []);

  const handleChange = (field, value) => {
    setSettings(prev => {
      const newSettings = { ...prev, [field]: value };
      // שמירה אוטומטית
      localStorage.setItem('chatWidgetSettings', JSON.stringify(newSettings));
      if (onSettingsChange) {
        onSettingsChange(newSettings);
      }
      return newSettings;
    });
  };

  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        handleChange('botAvatar', reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const copyToClipboard = (text, setCopied) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  const resetToDefaults = () => {
    if (window.confirm('האם אתה בטוח שברצונך לאפס את כל ההגדרות?')) {
      const defaults = {
        chatTitle: 'צ\'אט חכם',
        botName: 'עוזר',
        primaryColor: '#667eea',
        welcomeMessage: 'שלום! איך אוכל לעזור לך היום?',
        placeholder: 'הקלד את שאלתך כאן...',
        position: 'bottom-right',
        botAvatar: '',
        showPoweredBy: true
      };
      setSettings(defaults);
      localStorage.setItem('chatWidgetSettings', JSON.stringify(defaults));
      if (onSettingsChange) {
        onSettingsChange(defaults);
      }
    }
  };

  return (
    <div className="customization-container">
      <h2>🎨 התאמה אישית</h2>
      <p className="customization-description">
        התאם את הצ'אט לעיצוב האתר שלך. ההגדרות יישמרו אוטומטית ויופיעו בקוד ההטמעה.
      </p>

      {/* הגדרות עיצוב צ'אט */}
      <div className="customization-section">
        <h3>💬 הגדרות הצ'אט</h3>
        
        <div className="settings-grid">
          <div className="setting-item">
            <label>כותרת הצ'אט:</label>
            <input
              type="text"
              value={settings.chatTitle}
              onChange={(e) => handleChange('chatTitle', e.target.value)}
              placeholder="למשל: שירות לקוחות"
            />
          </div>

          <div className="setting-item">
            <label>שם הבוט:</label>
            <input
              type="text"
              value={settings.botName}
              onChange={(e) => handleChange('botName', e.target.value)}
              placeholder="למשל: עוזר"
            />
          </div>

          <div className="setting-item">
            <label>צבע ראשי:</label>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={settings.primaryColor}
                onChange={(e) => handleChange('primaryColor', e.target.value)}
              />
              <input
                type="text"
                value={settings.primaryColor}
                onChange={(e) => handleChange('primaryColor', e.target.value)}
                placeholder="#667eea"
              />
            </div>
          </div>

          <div className="setting-item">
            <label>הודעת פתיחה:</label>
            <textarea
              value={settings.welcomeMessage}
              onChange={(e) => handleChange('welcomeMessage', e.target.value)}
              placeholder="הודעה שתוצג כשהצ'אט נפתח"
              rows={2}
            />
          </div>

          <div className="setting-item">
            <label>טקסט בתיבת ההקלדה:</label>
            <input
              type="text"
              value={settings.placeholder}
              onChange={(e) => handleChange('placeholder', e.target.value)}
              placeholder="הקלד את שאלתך..."
            />
          </div>

          <div className="setting-item">
            <label>מיקום הכפתור:</label>
            <select
              value={settings.position}
              onChange={(e) => handleChange('position', e.target.value)}
            >
              <option value="bottom-right">ימין למטה</option>
              <option value="bottom-left">שמאל למטה</option>
            </select>
          </div>

          <div className="setting-item full-width">
            <label>תמונת אווטאר לבוט:</label>
            <div className="avatar-upload">
              {settings.botAvatar ? (
                <div className="avatar-preview">
                  <img src={settings.botAvatar} alt="Bot Avatar" />
                  <button 
                    className="remove-avatar"
                    onClick={() => handleChange('botAvatar', '')}
                  >
                    ✕
                  </button>
                </div>
              ) : (
                <div className="avatar-placeholder">
                  <span>🤖</span>
                </div>
              )}
              <label className="upload-avatar-btn">
                📷 העלה תמונה
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleImageUpload}
                  hidden
                />
              </label>
            </div>
          </div>

          <div className="setting-item checkbox-item">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={settings.showPoweredBy}
                onChange={(e) => handleChange('showPoweredBy', e.target.checked)}
              />
              <span>הצג "Powered by"</span>
            </label>
          </div>
        </div>

        <button className="reset-btn" onClick={resetToDefaults}>
          🔄 אפס להגדרות ברירת מחדל
        </button>
      </div>

      {/* הגדרות קולקשן */}
      <div className="customization-section">
        <h3>📊 מידע על הקולקשן</h3>
        
        <div className="collection-info-grid">
          <div className="info-item">
            <span className="info-label">שם קולקשן:</span>
            <span className="info-value mono">{collection?.collectionName}</span>
          </div>
          <div className="info-item">
            <span className="info-label">נוצר בתאריך:</span>
            <span className="info-value">
              {collection?.createdAt ? new Date(collection.createdAt).toLocaleDateString('he-IL') : '-'}
            </span>
          </div>
        </div>
      </div>

      {/* Secret Key */}
      <div className="customization-section key-section">
        <h3>🔑 מפתח סודי (Secret Key)</h3>
        <p className="section-description">
          המפתח הזה נדרש כדי שה-Widget יוכל לתקשר עם המסמכים שלך. 
          <strong> שמור אותו במקום בטוח!</strong>
        </p>

        <div className="key-input-wrapper">
          <input
            type="text"
            value={collection?.secretKey || ''}
            readOnly
            className="key-input"
          />
          <button
            onClick={() => copyToClipboard(collection?.secretKey, setCopiedKey)}
            className={`copy-btn ${copiedKey ? 'copied' : ''}`}
          >
            {copiedKey ? '✓ הועתק' : '📋 העתק'}
          </button>
          <button
            onClick={onRegenerateKey}
            disabled={loading}
            className="regenerate-btn"
          >
            🔄 צור מפתח חדש
          </button>
        </div>

        <div className="warning-box">
          ⚠️ <strong>אזהרה:</strong> יצירת מפתח חדש תבטל את המפתח הישן. 
          כל widget שמשתמש במפתח הישן יפסיק לעבוד.
        </div>
      </div>

      {/* תצוגה מקדימה */}
      <div className="customization-section preview-section">
        <h3>👁️ תצוגה מקדימה</h3>
        <div className="preview-container">
          <div 
            className="preview-chat-widget"
            style={{ '--primary-color': settings.primaryColor }}
          >
            <div className="preview-header" style={{ background: settings.primaryColor }}>
              <div className="preview-avatar">
                {settings.botAvatar ? (
                  <img src={settings.botAvatar} alt="Bot" />
                ) : (
                  <span>🤖</span>
                )}
              </div>
              <div className="preview-title">{settings.chatTitle}</div>
              <button className="preview-close">✕</button>
            </div>
            <div className="preview-messages">
              <div className="preview-message bot">
                <div className="preview-message-avatar">
                  {settings.botAvatar ? (
                    <img src={settings.botAvatar} alt="Bot" />
                  ) : (
                    <span>🤖</span>
                  )}
                </div>
                <div className="preview-message-content">
                  <div className="preview-bot-name">{settings.botName}</div>
                  <div className="preview-message-text">{settings.welcomeMessage}</div>
                </div>
              </div>
            </div>
            <div className="preview-input">
              <input type="text" placeholder={settings.placeholder} disabled />
              <button style={{ background: settings.primaryColor }}>➤</button>
            </div>
            {settings.showPoweredBy && (
              <div className="preview-powered-by">Powered by Custom Site Chat</div>
            )}
          </div>
        </div>
      </div>

      {/* הערה */}
      <div className="customization-note">
        💡 <strong>טיפ:</strong> לאחר שמירת ההגדרות, עבור לטאב "קוד הטמעה" לקבלת הקוד המותאם אישית.
      </div>
    </div>
  );
};

export default Customization;
