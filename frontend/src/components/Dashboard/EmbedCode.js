// frontend/src/components/Dashboard/EmbedCode.js
import React, { useState, useEffect } from 'react';
import './EmbedCode.css';

const EmbedCode = ({ collection, customizationSettings: propSettings }) => {
  const [copiedEmbed, setCopiedEmbed] = useState(false);
  const [copiedEnv, setCopiedEnv] = useState(false);
  const [activeGuide, setActiveGuide] = useState('react');
  
  // Default settings
  const defaultSettings = {
    chatTitle: 'צ\'אט חכם',
    botName: 'עוזר',
    primaryColor: '#667eea',
    welcomeMessage: 'שלום! איך אוכל לעזור לך היום?',
    placeholder: 'הקלד את שאלתך כאן...',
    position: 'bottom-right',
    botAvatar: '',
    showPoweredBy: true
  };

  // Get settings from props or localStorage
  const [settings, setSettings] = useState(defaultSettings);

  // עדכון הגדרות כאשר propSettings משתנה או מ-localStorage
  useEffect(() => {
    if (propSettings) {
      setSettings(prev => ({ ...prev, ...propSettings }));
    } else {
      const savedSettings = localStorage.getItem('chatWidgetSettings');
      if (savedSettings) {
        try {
          const parsed = JSON.parse(savedSettings);
          setSettings(prev => ({ ...prev, ...parsed }));
        } catch (e) {
          console.error('Error loading saved settings:', e);
        }
      }
    }
  }, [propSettings]);

  const copyToClipboard = (text, setCopied) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  // יצירת תוכן קובץ ENV
  const generateEnvContent = (platform) => {
    const baseContent = `# Chat Widget Configuration
# Generated at: ${new Date().toISOString()}
# DO NOT commit this file to version control!

`;

    switch (platform) {
      case 'react':
        return baseContent + `# React (Vite) Environment Variables
VITE_CHAT_WIDGET_SECRET_KEY=${collection?.secretKey || 'YOUR_SECRET_KEY'}
VITE_CHAT_WIDGET_API_URL=https://custom-site-chat.com
VITE_CHAT_WIDGET_TITLE=${settings.chatTitle}
VITE_CHAT_WIDGET_BOT_NAME=${settings.botName}
VITE_CHAT_WIDGET_PRIMARY_COLOR=${settings.primaryColor}
VITE_CHAT_WIDGET_POSITION=${settings.position}
`;
      case 'nextjs':
        return baseContent + `# Next.js Environment Variables
NEXT_PUBLIC_CHAT_WIDGET_SECRET_KEY=${collection?.secretKey || 'YOUR_SECRET_KEY'}
NEXT_PUBLIC_CHAT_WIDGET_API_URL=https://custom-site-chat.com
NEXT_PUBLIC_CHAT_WIDGET_TITLE=${settings.chatTitle}
NEXT_PUBLIC_CHAT_WIDGET_BOT_NAME=${settings.botName}
NEXT_PUBLIC_CHAT_WIDGET_PRIMARY_COLOR=${settings.primaryColor}
NEXT_PUBLIC_CHAT_WIDGET_POSITION=${settings.position}
`;
      case 'html':
        return baseContent + `# Node.js / Express Environment Variables
CHAT_WIDGET_SECRET_KEY=${collection?.secretKey || 'YOUR_SECRET_KEY'}
CHAT_WIDGET_API_URL=https://custom-site-chat.com
CHAT_WIDGET_TITLE=${settings.chatTitle}
CHAT_WIDGET_BOT_NAME=${settings.botName}
CHAT_WIDGET_PRIMARY_COLOR=${settings.primaryColor}
CHAT_WIDGET_POSITION=${settings.position}
`;
      case 'wordpress':
        return baseContent + `# WordPress Configuration
# Add these to wp-config.php using define()
#
# define('CHAT_WIDGET_SECRET_KEY', '${collection?.secretKey || 'YOUR_SECRET_KEY'}');
# define('CHAT_WIDGET_API_URL', 'https://custom-site-chat.com');
# define('CHAT_WIDGET_TITLE', '${settings.chatTitle}');
# define('CHAT_WIDGET_BOT_NAME', '${settings.botName}');

CHAT_WIDGET_SECRET_KEY=${collection?.secretKey || 'YOUR_SECRET_KEY'}
CHAT_WIDGET_API_URL=https://custom-site-chat.com
`;
      default:
        return baseContent;
    }
  };

  // הורדת קובץ ENV
  const downloadEnvFile = (platform) => {
    const content = generateEnvContent(platform);
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = platform === 'react' ? '.env.local' : '.env';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  // יצירת קוד מוכן להעתקה
  const generateEmbedCode = (platform) => {
    const avatarCode = settings.botAvatar ? `\n    window.CHAT_WIDGET_BOT_AVATAR = '${settings.botAvatar}';` : '';
    
    switch (platform) {
      case 'react':
        return `// ChatWidget.jsx - העתק לתיקיית components
import { useEffect } from 'react';

export default function ChatWidget() {
  useEffect(() => {
    // Configuration from environment variables
    window.CHAT_WIDGET_SECRET_KEY = import.meta.env.VITE_CHAT_WIDGET_SECRET_KEY;
    window.CHAT_WIDGET_API_URL = import.meta.env.VITE_CHAT_WIDGET_API_URL;
    
    // Widget customization
    window.CHAT_WIDGET_TITLE = '${settings.chatTitle}';
    window.CHAT_WIDGET_BOT_NAME = '${settings.botName}';
    window.CHAT_WIDGET_PRIMARY_COLOR = '${settings.primaryColor}';
    window.CHAT_WIDGET_PLACEHOLDER = '${settings.placeholder}';
    window.CHAT_WIDGET_POSITION = '${settings.position}';
    window.CHAT_WIDGET_WELCOME_MESSAGE = '${settings.welcomeMessage}';${avatarCode}
    
    // Load widget script
    const script = document.createElement('script');
    script.src = 'https://custom-site-chat.com/chat-widget.js';
    script.async = true;
    document.body.appendChild(script);
    
    return () => {
      document.body.removeChild(script);
    };
  }, []);
  
  return null;
}

// In App.jsx add:
// import ChatWidget from './components/ChatWidget';
// <ChatWidget />`;

      case 'nextjs':
        return `// components/ChatWidget.tsx
'use client';
import { useEffect } from 'react';

export default function ChatWidget() {
  useEffect(() => {
    // Configuration
    (window as any).CHAT_WIDGET_SECRET_KEY = process.env.NEXT_PUBLIC_CHAT_WIDGET_SECRET_KEY;
    (window as any).CHAT_WIDGET_API_URL = process.env.NEXT_PUBLIC_CHAT_WIDGET_API_URL;
    
    // Customization
    (window as any).CHAT_WIDGET_TITLE = '${settings.chatTitle}';
    (window as any).CHAT_WIDGET_BOT_NAME = '${settings.botName}';
    (window as any).CHAT_WIDGET_PRIMARY_COLOR = '${settings.primaryColor}';
    (window as any).CHAT_WIDGET_PLACEHOLDER = '${settings.placeholder}';
    (window as any).CHAT_WIDGET_POSITION = '${settings.position}';
    (window as any).CHAT_WIDGET_WELCOME_MESSAGE = '${settings.welcomeMessage}';${avatarCode ? avatarCode.replace('window', '(window as any)') : ''}
    
    // Load widget
    const script = document.createElement('script');
    script.src = 'https://custom-site-chat.com/chat-widget.js';
    script.async = true;
    document.body.appendChild(script);
    
    return () => {
      document.body.removeChild(script);
    };
  }, []);
  
  return null;
}

// In layout.tsx or page.tsx:
// import ChatWidget from '@/components/ChatWidget';
// <ChatWidget />`;

      case 'html':
        return `<!-- הוסף לפני תגית </body> -->
<script>
  // Configuration - המפתח צריך להגיע מהשרת!
  window.CHAT_WIDGET_SECRET_KEY = '<%- secretKey %>';  // EJS
  // או: window.CHAT_WIDGET_SECRET_KEY = '{{ secret_key }}'; // Jinja/Django
  // או: window.CHAT_WIDGET_SECRET_KEY = '<?php echo $secretKey; ?>'; // PHP
  
  window.CHAT_WIDGET_API_URL = 'https://custom-site-chat.com';
  
  // Widget Customization
  window.CHAT_WIDGET_TITLE = '${settings.chatTitle}';
  window.CHAT_WIDGET_BOT_NAME = '${settings.botName}';
  window.CHAT_WIDGET_PRIMARY_COLOR = '${settings.primaryColor}';
  window.CHAT_WIDGET_PLACEHOLDER = '${settings.placeholder}';
  window.CHAT_WIDGET_POSITION = '${settings.position}';
  window.CHAT_WIDGET_WELCOME_MESSAGE = '${settings.welcomeMessage}';${avatarCode}
</script>
<script src="https://custom-site-chat.com/chat-widget.js" async></script>`;

      case 'wordpress':
        return `<?php
// הוסף לקובץ functions.php של ה-Theme
function add_chat_widget() {
    $secret_key = defined('CHAT_WIDGET_SECRET_KEY') ? CHAT_WIDGET_SECRET_KEY : '';
    if (empty($secret_key)) return;
    ?>
    <script>
      window.CHAT_WIDGET_SECRET_KEY = '<?php echo esc_js($secret_key); ?>';
      window.CHAT_WIDGET_API_URL = 'https://custom-site-chat.com';
      
      // Widget Customization
      window.CHAT_WIDGET_TITLE = '${settings.chatTitle}';
      window.CHAT_WIDGET_BOT_NAME = '${settings.botName}';
      window.CHAT_WIDGET_PRIMARY_COLOR = '${settings.primaryColor}';
      window.CHAT_WIDGET_PLACEHOLDER = '${settings.placeholder}';
      window.CHAT_WIDGET_POSITION = '${settings.position}';
      window.CHAT_WIDGET_WELCOME_MESSAGE = '${settings.welcomeMessage}';
    </script>
    <script src="https://custom-site-chat.com/chat-widget.js" async></script>
    <?php
}
add_action('wp_footer', 'add_chat_widget');
?>

<!-- wp-config.php - הוסף מעל "That's all, stop editing!" -->
<?php
define('CHAT_WIDGET_SECRET_KEY', '${collection?.secretKey || 'YOUR_SECRET_KEY'}');
?>`;

      default:
        return '';
    }
  };

  const currentCode = generateEmbedCode(activeGuide);

  return (
    <div className="embed-code-container">
      <h2>📦 קוד הטמעה</h2>

      {/* הוראות מהירות */}
      <div className="quick-start-section">
        <h3>🚀 התחלה מהירה</h3>
        <div className="quick-start-steps">
          <div className="step">
            <div className="step-number">1</div>
            <div className="step-content">
              <strong>הורד קובץ ENV</strong>
              <p>קובץ הגדרות מוכן עם המפתח שלך</p>
            </div>
          </div>
          <div className="step">
            <div className="step-number">2</div>
            <div className="step-content">
              <strong>העתק את הקוד</strong>
              <p>קוד מותאם להגדרות שבחרת</p>
            </div>
          </div>
          <div className="step">
            <div className="step-number">3</div>
            <div className="step-content">
              <strong>הדבק בפרויקט</strong>
              <p>שים את הקובץ והקוד במקום הנכון</p>
            </div>
          </div>
        </div>
      </div>

      {/* בחירת פלטפורמה */}
      <div className="platform-tabs">
        {['react', 'nextjs', 'html', 'wordpress'].map(tech => (
          <button
            key={tech}
            onClick={() => setActiveGuide(tech)}
            className={`platform-tab ${activeGuide === tech ? 'active' : ''}`}
          >
            {tech === 'react' && '⚛️ React'}
            {tech === 'nextjs' && '▲ Next.js'}
            {tech === 'html' && '🌐 HTML + Backend'}
            {tech === 'wordpress' && '📝 WordPress'}
          </button>
        ))}
      </div>

      {/* הורדת קובץ ENV */}
      <div className="download-section">
        <h3>📥 שלב 1: הורד קובץ הגדרות</h3>
        <p>הקובץ כולל את המפתח שלך וכל ההגדרות. שים אותו בתיקייה הראשית של הפרויקט.</p>
        
        <div className="download-actions">
          <button 
            className="download-btn primary"
            onClick={() => downloadEnvFile(activeGuide)}
          >
            📥 הורד קובץ {activeGuide === 'react' ? '.env.local' : '.env'}
          </button>
          
          <button 
            className="copy-env-btn"
            onClick={() => {
              copyToClipboard(generateEnvContent(activeGuide), setCopiedEnv);
            }}
          >
            {copiedEnv ? '✓ הועתק!' : '📋 העתק תוכן'}
          </button>
        </div>

        <div className="env-preview">
          <pre>{generateEnvContent(activeGuide)}</pre>
        </div>

        <div className="security-note">
          ⚠️ <strong>חשוב:</strong> הוסף את הקובץ ל-<code>.gitignore</code> - לא להעלות ל-Git!
        </div>
      </div>

      {/* קוד הטמעה */}
      <div className="code-section">
        <div className="code-header">
          <h3>📝 שלב 2: העתק את הקוד</h3>
          <button 
            className={`copy-code-btn ${copiedEmbed ? 'copied' : ''}`}
            onClick={() => copyToClipboard(currentCode, setCopiedEmbed)}
          >
            {copiedEmbed ? '✓ הועתק!' : '📋 העתק קוד'}
          </button>
        </div>
        
        <div className="code-preview">
          <pre>{currentCode}</pre>
        </div>
      </div>

      {/* הגדרות שיושמו */}
      <div className="applied-settings-section">
        <h3>⚙️ הגדרות שיושמו בקוד</h3>
        <div className="settings-summary">
          <div className="setting-preview">
            <span className="setting-name">כותרת:</span>
            <span className="setting-value">{settings.chatTitle}</span>
          </div>
          <div className="setting-preview">
            <span className="setting-name">שם בוט:</span>
            <span className="setting-value">{settings.botName}</span>
          </div>
          <div className="setting-preview">
            <span className="setting-name">צבע:</span>
            <span className="setting-value">
              <span className="color-preview" style={{ background: settings.primaryColor }}></span>
              {settings.primaryColor}
            </span>
          </div>
          <div className="setting-preview">
            <span className="setting-name">מיקום:</span>
            <span className="setting-value">{settings.position === 'bottom-right' ? 'ימין למטה' : 'שמאל למטה'}</span>
          </div>
        </div>
        <p className="settings-note">
          💡 ניתן לשנות הגדרות אלו בטאב "התאמה אישית"
        </p>
      </div>

      {/* טיפים לאבטחה */}
      <div className="security-tips">
        <h3>🔐 טיפים לאבטחה</h3>
        <ul>
          <li>הוסף את <code>.env</code> ו-<code>.env.local</code> ל-<code>.gitignore</code></li>
          <li>לעולם אל תשים את המפתח ישירות בקוד שעולה ל-Git</li>
          <li>השתמש ב-environment variables בכל הסביבות</li>
          <li>אם המפתח נחשף - צור מפתח חדש מיד בטאב "התאמה אישית"</li>
          <li>השתמש ב-HTTPS בלבד באתר שלך</li>
        </ul>
      </div>
    </div>
  );
};

export default EmbedCode;
