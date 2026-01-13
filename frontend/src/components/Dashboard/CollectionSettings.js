// frontend/src/components/Dashboard/CollectionSettings.js
import React, { useState } from 'react';

const CollectionSettings = ({ collection, onRegenerateKey, loading }) => {
  const [copiedEmbed, setCopiedEmbed] = useState(false);
  const [copiedKey, setCopiedKey] = useState(false);
  const [activeGuide, setActiveGuide] = useState('react');

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


      {/* Implementation Guides */}
      <div style={{
        marginTop: '25px',
        background: '#f8f9ff',
        border: '1px solid #e1e8ed',
        borderRadius: '12px',
        padding: '25px'
      }}>
        <h3 style={{ marginBottom: '20px', color: '#333', fontSize: '20px' }}>📖 מדריכי הטמעה בטוחה</h3>
        
        {/* Tab Buttons */}
        <div style={{ display: 'flex', gap: '10px', marginBottom: '20px', flexWrap: 'wrap' }}>
          {['react', 'nextjs', 'html', 'wordpress'].map(tech => (
            <button
              key={tech}
              onClick={() => setActiveGuide(tech)}
              style={{
                padding: '10px 20px',
                background: activeGuide === tech ? '#667eea' : 'white',
                color: activeGuide === tech ? 'white' : '#667eea',
                border: '2px solid #667eea',
                borderRadius: '8px',
                cursor: 'pointer',
                fontWeight: 600,
                fontSize: '14px',
                transition: 'all 0.2s'
              }}
            >
              {tech === 'react' && '⚛️ React'}
              {tech === 'nextjs' && '▲ Next.js'}
              {tech === 'html' && '🌐 HTML + Backend'}
              {tech === 'wordpress' && '📝 WordPress'}
            </button>
          ))}
        </div>

        {/* React Guide */}
        {activeGuide === 'react' && (
          <div>
            <h4 style={{ color: '#667eea', marginBottom: '15px' }}>⚛️ הטמעה ב-React (עם Vite / CRA)</h4>
            
            <div style={{ marginBottom: '20px' }}>
              <h5 style={{ color: '#333', marginBottom: '10px' }}>שלב 1: צור קובץ .env.local</h5>
              <pre style={{
                direction: 'ltr',
                textAlign: 'left',
                background: '#1e1e1e',
                color: '#d4d4d4',
                padding: '15px',
                borderRadius: '6px',
                fontSize: '13px',
                fontFamily: 'monospace',
                overflow: 'auto'
              }}>
{`# .env.local
VITE_CHAT_WIDGET_SECRET_KEY=${collection.secretKey}
VITE_CHAT_WIDGET_API_URL=https://custom-site-chat.com`}
              </pre>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <h5 style={{ color: '#333', marginBottom: '10px' }}>שלב 2: הוסף סקריפט ל-index.html או בקומפוננטה</h5>
              <pre style={{
                direction: 'ltr',
                textAlign: 'left',
                background: '#1e1e1e',
                color: '#d4d4d4',
                padding: '15px',
                borderRadius: '6px',
                fontSize: '13px',
                fontFamily: 'monospace',
                overflow: 'auto'
              }}>
{`// src/components/ChatWidget.jsx
import { useEffect } from 'react';

export default function ChatWidget() {
  useEffect(() => {
    // Set configuration
    window.CHAT_WIDGET_SECRET_KEY = import.meta.env.VITE_CHAT_WIDGET_SECRET_KEY;
    window.CHAT_WIDGET_API_URL = import.meta.env.VITE_CHAT_WIDGET_API_URL;
    
    // Optional customization
    window.CHAT_WIDGET_TITLE = 'שם החברה שלך';
    window.CHAT_WIDGET_BOT_NAME = 'עוזר';
    
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

// In App.jsx:
import ChatWidget from './components/ChatWidget';

function App() {
  return (
    <>
      <YourComponents />
      <ChatWidget />
    </>
  );
}`}
              </pre>
            </div>

            <div style={{
              padding: '12px',
              background: '#e8f5e9',
              border: '1px solid #4caf50',
              borderRadius: '6px',
              fontSize: '13px',
              color: '#2e7d32'
            }}>
              ✅ <strong>בטוח!</strong> המפתח לא נחשף בקוד הלקוח
            </div>
          </div>
        )}

        {/* Next.js Guide */}
        {activeGuide === 'nextjs' && (
          <div>
            <h4 style={{ color: '#667eea', marginBottom: '15px' }}>▲ הטמעה ב-Next.js</h4>
            
            <div style={{ marginBottom: '20px' }}>
              <h5 style={{ color: '#333', marginBottom: '10px' }}>שלב 1: צור קובץ .env.local</h5>
              <pre style={{
                direction: 'ltr',
                textAlign: 'left',
                background: '#1e1e1e',
                color: '#d4d4d4',
                padding: '15px',
                borderRadius: '6px',
                fontSize: '13px',
                fontFamily: 'monospace',
                overflow: 'auto'
              }}>
{`# .env.local
NEXT_PUBLIC_CHAT_WIDGET_SECRET_KEY=${collection.secretKey}
NEXT_PUBLIC_CHAT_WIDGET_API_URL=https://custom-site-chat.com`}
              </pre>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <h5 style={{ color: '#333', marginBottom: '10px' }}>שלב 2: הוסף ל-_document.jsx או _app.jsx</h5>
              <pre style={{
                direction: 'ltr',
                textAlign: 'left',
                background: '#1e1e1e',
                color: '#d4d4d4',
                padding: '15px',
                borderRadius: '6px',
                fontSize: '13px',
                fontFamily: 'monospace',
                overflow: 'auto'
              }}>
{`// pages/_document.jsx (Next.js 12+)
import { Html, Head, Main, NextScript } from 'next/document';

export default function Document() {
  return (
    <Html>
      <Head />
      <body>
        <Main />
        <NextScript />
        
        {/* Chat Widget */}
        <script
          dangerouslySetInnerHTML={{
            __html: \`
              window.CHAT_WIDGET_SECRET_KEY = '\${process.env.NEXT_PUBLIC_CHAT_WIDGET_SECRET_KEY}';
              window.CHAT_WIDGET_API_URL = '\${process.env.NEXT_PUBLIC_CHAT_WIDGET_API_URL}';
              window.CHAT_WIDGET_TITLE = 'שם החברה';
              window.CHAT_WIDGET_BOT_NAME = 'עוזר';
            \`
          }}
        />
        <script src="https://custom-site-chat.com/chat-widget.js" async />
      </body>
    </Html>
  );
}`}
              </pre>
            </div>

            <div style={{
              padding: '12px',
              background: '#fff3cd',
              border: '1px solid #ffc107',
              borderRadius: '6px',
              fontSize: '13px',
              color: '#856404'
            }}>
              ⚠️ <strong>שים לב:</strong> משתני NEXT_PUBLIC_ נחשפים בצד הלקוח. לבטחון מירבי, שקול שימוש ב-API Route
            </div>

            <div style={{ marginTop: '15px', marginBottom: '20px' }}>
              <h5 style={{ color: '#333', marginBottom: '10px' }}>אופציה מתקדמת: באמצעות API Route (מומלץ)</h5>
              <pre style={{
                direction: 'ltr',
                textAlign: 'left',
                background: '#1e1e1e',
                color: '#d4d4d4',
                padding: '15px',
                borderRadius: '6px',
                fontSize: '13px',
                fontFamily: 'monospace',
                overflow: 'auto'
              }}>
{`// .env.local (ללא NEXT_PUBLIC)
CHAT_WIDGET_SECRET_KEY=${collection.secretKey}

// pages/api/chat-config.js
export default function handler(req, res) {
  res.status(200).json({
    secretKey: process.env.CHAT_WIDGET_SECRET_KEY,
    apiUrl: 'https://custom-site-chat.com'
  });
}

// components/ChatWidget.jsx
import { useEffect } from 'react';

export default function ChatWidget() {
  useEffect(() => {
    // Fetch config from API route
    fetch('/api/chat-config')
      .then(res => res.json())
      .then(config => {
        window.CHAT_WIDGET_SECRET_KEY = config.secretKey;
        window.CHAT_WIDGET_API_URL = config.apiUrl;
        
        const script = document.createElement('script');
        script.src = 'https://custom-site-chat.com/chat-widget.js';
        document.body.appendChild(script);
      });
  }, []);
  
  return null;
}`}
              </pre>
            </div>

            <div style={{
              padding: '12px',
              background: '#e8f5e9',
              border: '1px solid #4caf50',
              borderRadius: '6px',
              fontSize: '13px',
              color: '#2e7d32'
            }}>
              ✅ <strong>הכי בטוח!</strong> המפתח נשאר רק בצד השרת
            </div>
          </div>
        )}

        {/* HTML + Backend Guide */}
        {activeGuide === 'html' && (
          <div>
            <h4 style={{ color: '#667eea', marginBottom: '15px' }}>🌐 HTML + Backend (Node.js / PHP / Python)</h4>
            
            <div style={{ marginBottom: '20px' }}>
              <h5 style={{ color: '#333', marginBottom: '10px' }}>שלב 1: צור קובץ .env בשרת</h5>
              <pre style={{
                direction: 'ltr',
                textAlign: 'left',
                background: '#1e1e1e',
                color: '#d4d4d4',
                padding: '15px',
                borderRadius: '6px',
                fontSize: '13px',
                fontFamily: 'monospace',
                overflow: 'auto'
              }}>
{`# .env (בצד השרת)
CHAT_WIDGET_SECRET_KEY=${collection.secretKey}
CHAT_WIDGET_API_URL=https://custom-site-chat.com`}
              </pre>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <h5 style={{ color: '#333', marginBottom: '10px' }}>שלב 2: הזרק משתנים בצד השרת</h5>
              
              <details style={{ marginBottom: '10px' }}>
                <summary style={{ cursor: 'pointer', fontWeight: 600, padding: '10px', background: '#f0f0f0', borderRadius: '6px' }}>
                  Node.js + Express (לחץ להרחבה)
                </summary>
                <pre style={{
                  direction: 'ltr',
                  textAlign: 'left',
                  background: '#1e1e1e',
                  color: '#d4d4d4',
                  padding: '15px',
                  borderRadius: '6px',
                  fontSize: '13px',
                  fontFamily: 'monospace',
                  overflow: 'auto',
                  marginTop: '10px'
                }}>
{`// server.js
require('dotenv').config();
const express = require('express');
const app = express();

app.get('/', (req, res) => {
  res.send(\`
    <!DOCTYPE html>
    <html>
    <head>
      <title>האתר שלי</title>
    </head>
    <body>
      <h1>ברוכים הבאים</h1>
      
      <script>
        window.CHAT_WIDGET_SECRET_KEY = '\${process.env.CHAT_WIDGET_SECRET_KEY}';
        window.CHAT_WIDGET_API_URL = '\${process.env.CHAT_WIDGET_API_URL}';
        window.CHAT_WIDGET_TITLE = 'שם החברה';
      </script>
      <script src="https://custom-site-chat.com/chat-widget.js"></script>
    </body>
    </html>
  \`);
});

app.listen(3000);`}
                </pre>
              </details>

              <details style={{ marginBottom: '10px' }}>
                <summary style={{ cursor: 'pointer', fontWeight: 600, padding: '10px', background: '#f0f0f0', borderRadius: '6px' }}>
                  PHP (לחץ להרחבה)
                </summary>
                <pre style={{
                  direction: 'ltr',
                  textAlign: 'left',
                  background: '#1e1e1e',
                  color: '#d4d4d4',
                  padding: '15px',
                  borderRadius: '6px',
                  fontSize: '13px',
                  fontFamily: 'monospace',
                  overflow: 'auto',
                  marginTop: '10px'
                }}>
{`<?php
// index.php
require_once 'vendor/autoload.php'; // If using vlucas/phpdotenv
$dotenv = Dotenv\\Dotenv::createImmutable(__DIR__);
$dotenv->load();

$secretKey = $_ENV['CHAT_WIDGET_SECRET_KEY'];
?>
<!DOCTYPE html>
<html>
<head>
  <title>האתר שלי</title>
</head>
<body>
  <h1>ברוכים הבאים</h1>
  
  <script>
    window.CHAT_WIDGET_SECRET_KEY = '<?php echo htmlspecialchars($secretKey); ?>';
    window.CHAT_WIDGET_API_URL = 'https://custom-site-chat.com';
    window.CHAT_WIDGET_TITLE = 'שם החברה';
  </script>
  <script src="https://custom-site-chat.com/chat-widget.js"></script>
</body>
</html>`}
                </pre>
              </details>

              <details>
                <summary style={{ cursor: 'pointer', fontWeight: 600, padding: '10px', background: '#f0f0f0', borderRadius: '6px' }}>
                  Python + Flask (לחץ להרחבה)
                </summary>
                <pre style={{
                  direction: 'ltr',
                  textAlign: 'left',
                  background: '#1e1e1e',
                  color: '#d4d4d4',
                  padding: '15px',
                  borderRadius: '6px',
                  fontSize: '13px',
                  fontFamily: 'monospace',
                  overflow: 'auto',
                  marginTop: '10px'
                }}>
{`# app.py
from flask import Flask, render_template_string
from dotenv import load_dotenv
import os

load_dotenv()
app = Flask(__name__)

@app.route('/')
def index():
    secret_key = os.getenv('CHAT_WIDGET_SECRET_KEY')
    return render_template_string('''
    <!DOCTYPE html>
    <html>
    <head>
      <title>האתר שלי</title>
    </head>
    <body>
      <h1>ברוכים הבאים</h1>
      
      <script>
        window.CHAT_WIDGET_SECRET_KEY = '{{ secret_key }}';
        window.CHAT_WIDGET_API_URL = 'https://custom-site-chat.com';
        window.CHAT_WIDGET_TITLE = 'שם החברה';
      </script>
      <script src="https://custom-site-chat.com/chat-widget.js"></script>
    </body>
    </html>
    ''', secret_key=secret_key)

if __name__ == '__main__':
    app.run()`}
                </pre>
              </details>
            </div>

            <div style={{
              padding: '12px',
              background: '#e8f5e9',
              border: '1px solid #4caf50',
              borderRadius: '6px',
              fontSize: '13px',
              color: '#2e7d32'
            }}>
              ✅ <strong>בטוח!</strong> המפתח מוזרק מהשרת ולא נחשף בקובץ סטטי
            </div>
          </div>
        )}

        {/* WordPress Guide */}
        {activeGuide === 'wordpress' && (
          <div>
            <h4 style={{ color: '#667eea', marginBottom: '15px' }}>📝 הטמעה ב-WordPress</h4>
            
            <div style={{ marginBottom: '20px' }}>
              <h5 style={{ color: '#333', marginBottom: '10px' }}>שלב 1: הוסף ל-wp-config.php</h5>
              <pre style={{
                direction: 'ltr',
                textAlign: 'left',
                background: '#1e1e1e',
                color: '#d4d4d4',
                padding: '15px',
                borderRadius: '6px',
                fontSize: '13px',
                fontFamily: 'monospace',
                overflow: 'auto'
              }}>
{`// wp-config.php (מעל "That's all, stop editing!")
define('CHAT_WIDGET_SECRET_KEY', '${collection.secretKey}');
define('CHAT_WIDGET_API_URL', 'https://custom-site-chat.com');`}
              </pre>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <h5 style={{ color: '#333', marginBottom: '10px' }}>שלב 2: הוסף לקובץ functions.php של ה-Theme</h5>
              <pre style={{
                direction: 'ltr',
                textAlign: 'left',
                background: '#1e1e1e',
                color: '#d4d4d4',
                padding: '15px',
                borderRadius: '6px',
                fontSize: '13px',
                fontFamily: 'monospace',
                overflow: 'auto'
              }}>
{`// functions.php
function add_chat_widget() {
    $secret_key = CHAT_WIDGET_SECRET_KEY;
    $api_url = CHAT_WIDGET_API_URL;
    ?>
    <script>
      window.CHAT_WIDGET_SECRET_KEY = '<?php echo esc_js($secret_key); ?>';
      window.CHAT_WIDGET_API_URL = '<?php echo esc_js($api_url); ?>';
      window.CHAT_WIDGET_TITLE = '<?php echo esc_js(get_bloginfo('name')); ?>';
      window.CHAT_WIDGET_BOT_NAME = 'עוזר';
    </script>
    <script src="https://custom-site-chat.com/chat-widget.js" async></script>
    <?php
}
add_action('wp_footer', 'add_chat_widget');`}
              </pre>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <h5 style={{ color: '#333', marginBottom: '10px' }}>אופציה 2: באמצעות Plugin</h5>
              <p style={{ fontSize: '13px', color: '#666', marginBottom: '10px' }}>
                צור plugin פשוט שמוסיף את הקוד:
              </p>
              <pre style={{
                direction: 'ltr',
                textAlign: 'left',
                background: '#1e1e1e',
                color: '#d4d4d4',
                padding: '15px',
                borderRadius: '6px',
                fontSize: '13px',
                fontFamily: 'monospace',
                overflow: 'auto'
              }}>
{`<?php
/*
Plugin Name: Custom Site Chat Widget
Description: Adds chat widget securely
Version: 1.0
*/

// Add settings page
add_action('admin_menu', 'chat_widget_menu');
function chat_widget_menu() {
    add_options_page(
        'Chat Widget Settings',
        'Chat Widget',
        'manage_options',
        'chat-widget-settings',
        'chat_widget_settings_page'
    );
}

function chat_widget_settings_page() {
    ?>
    <div class="wrap">
        <h1>Chat Widget Settings</h1>
        <form method="post" action="options.php">
            <?php
            settings_fields('chat_widget_options');
            do_settings_sections('chat-widget-settings');
            ?>
            <table class="form-table">
                <tr>
                    <th>Secret Key</th>
                    <td>
                        <input type="password" 
                               name="chat_widget_secret_key" 
                               value="<?php echo esc_attr(get_option('chat_widget_secret_key')); ?>" 
                               class="regular-text" />
                    </td>
                </tr>
            </table>
            <?php submit_button(); ?>
        </form>
    </div>
    <?php
}

// Register settings
add_action('admin_init', 'chat_widget_register_settings');
function chat_widget_register_settings() {
    register_setting('chat_widget_options', 'chat_widget_secret_key');
}

// Add widget to footer
add_action('wp_footer', 'chat_widget_add_script');
function chat_widget_add_script() {
    $secret_key = get_option('chat_widget_secret_key');
    if (empty($secret_key)) return;
    ?>
    <script>
      window.CHAT_WIDGET_SECRET_KEY = '<?php echo esc_js($secret_key); ?>';
      window.CHAT_WIDGET_API_URL = 'https://custom-site-chat.com';
      window.CHAT_WIDGET_TITLE = '<?php echo esc_js(get_bloginfo('name')); ?>';
    </script>
    <script src="https://custom-site-chat.com/chat-widget.js" async></script>
    <?php
}
?>`}
              </pre>
            </div>

            <div style={{
              padding: '12px',
              background: '#e8f5e9',
              border: '1px solid #4caf50',
              borderRadius: '6px',
              fontSize: '13px',
              color: '#2e7d32'
            }}>
              ✅ <strong>בטוח!</strong> המפתח נשמר במסד הנתונים של WordPress ולא בקוד
            </div>
          </div>
        )}

        {/* General Security Tips */}
        <div style={{
          marginTop: '25px',
          padding: '15px',
          background: '#fff3cd',
          border: '1px solid #ffc107',
          borderRadius: '8px'
        }}>
          <h5 style={{ color: '#856404', marginBottom: '10px' }}>🔐 טיפים לאבטחה</h5>
          <ul style={{ margin: 0, paddingRight: '20px', color: '#856404', fontSize: '13px' }}>
            <li style={{ marginBottom: '5px' }}>הוסף את <code>.env</code> ל-<code>.gitignore</code> (לא להעלות ל-Git!)</li>
            <li style={{ marginBottom: '5px' }}>שמור גיבוי של המפתח במקום בטוח (מנהל סיסמאות)</li>
            <li style={{ marginBottom: '5px' }}>אל תשתף את המפתח באימייל או בצ'אטים</li>
            <li style={{ marginBottom: '5px' }}>אם המפתח נחשף - צור מפתח חדש מיד</li>
            <li>השתמש ב-HTTPS בלבד באתר שלך</li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default CollectionSettings;