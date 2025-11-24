(function() {
  'use strict';

  // ==================== Initialization ====================
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initWidget);
  } else {
    initWidget();
  }

  function initWidget() {
    // ==================== Configuration ====================
    const WIDGET_CONFIG = {
      apiUrl: window.CHAT_WIDGET_API_URL || 'http://localhost:8080',
      secretKey: window.CHAT_WIDGET_SECRET_KEY,
      position: 'bottom-right',
      primaryColor: '#667eea',
      secondaryColor: '#764ba2',
      title: window.CHAT_WIDGET_TITLE || 'צ\'אט עם המסמכים שלי',
      botName: window.CHAT_WIDGET_BOT_NAME || 'AI',
      botAvatar: window.CHAT_WIDGET_BOT_AVATAR || null,
      userAvatar: window.CHAT_WIDGET_USER_AVATAR || null
    };

    console.log('🔧 Widget Config:', WIDGET_CONFIG);

    if (!WIDGET_CONFIG.secretKey) {
      console.error('❌ Chat Widget: Missing SECRET_KEY');
      return;
    }

    // ==================== Inject CSS ====================
    injectStyles(WIDGET_CONFIG);

    // ==================== Create Widget HTML ====================
    createWidgetHTML(WIDGET_CONFIG);

    // ==================== Initialize Widget ====================
    setupEventListeners();
  }

  // ==================== CSS Injection ====================
  function injectStyles(config) {
    const styles = `
      /* Container */
      .chat-widget-container {
        position: fixed;
        ${config.position.includes('right') ? 'right: 20px;' : 'left: 20px;'}
        bottom: 20px;
        z-index: 9999;
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      }

      /* Toggle Button */
      .chat-widget-button {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: linear-gradient(135deg, ${config.primaryColor} 0%, ${config.secondaryColor} 100%);
        border: none;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: transform 0.2s;
        font-size: 28px;
      }

      .chat-widget-button:hover {
        transform: scale(1.1);
      }

      /* Widget Window */
      .chat-widget-window {
        position: absolute;
        bottom: 80px;
        ${config.position.includes('right') ? 'right: 0;' : 'left: 0;'}
        width: 380px;
        height: 600px;
        background: white;
        border-radius: 16px;
        box-shadow: 0 8px 24px rgba(0,0,0,0.2);
        display: none;
        flex-direction: column;
        overflow: hidden;
      }

      .chat-widget-window.open {
        display: flex;
        animation: slideUp 0.3s ease-out;
      }

      @keyframes slideUp {
        from {
          opacity: 0;
          transform: translateY(20px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      /* Header */
      .chat-widget-header {
        background: linear-gradient(135deg, ${config.primaryColor} 0%, ${config.secondaryColor} 100%);
        color: white;
        padding: 20px;
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .chat-widget-header h3 {
        margin: 0;
        font-size: 18px;
      }

      .chat-widget-close {
        background: none;
        border: none;
        color: white;
        font-size: 24px;
        cursor: pointer;
        padding: 0;
        width: 30px;
        height: 30px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        transition: background 0.2s;
      }

      .chat-widget-close:hover {
        background: rgba(255,255,255,0.2);
      }

      /* Messages Container */
      .chat-widget-messages {
        flex: 1;
        overflow-y: auto;
        padding: 20px;
        background: #f8f9ff;
      }

      /* Message Wrapper */
      .chat-message {
        margin-bottom: 16px;
        display: flex;
        align-items: flex-start;
        gap: 10px;
      }

      /* הודעת משתמש */
      .chat-message.user {
        flex-direction: row-reverse;
        justify-content: flex-start;
      }

      /* הודעת AI */
      .chat-message.assistant {
        flex-direction: row;
        justify-content: flex-start;
      }

      /* Avatar */
      .chat-message-avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: ${config.primaryColor};
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
        font-size: 14px;
        flex-shrink: 0;
        overflow: hidden;
      }

      .chat-message.assistant .chat-message-avatar {
        background: ${config.secondaryColor};
      }

      .chat-message-avatar img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      /* Message Content Wrapper */
      .chat-message-content {
        display: flex;
        flex-direction: column;
        max-width: 70%;
      }

      /* Message Bubble */
      .chat-message-bubble {
        padding: 12px 16px;
        border-radius: 12px;
        line-height: 1.5;
        font-size: 14px;
        word-wrap: break-word;
        white-space: pre-wrap;
      }

      /* כיוון טקסט לפי שפה */
      .chat-message-bubble.rtl {
        direction: rtl;
        text-align: right;
      }

      .chat-message-bubble.ltr {
        direction: ltr;
        text-align: left;
      }

      /* צבעי Bubble */
      .chat-message.user .chat-message-bubble {
        background: ${config.primaryColor};
        color: white;
      }

      .chat-message.assistant .chat-message-bubble {
        background: white;
        color: #333;
        border: 1px solid #e1e8ed;
      }

      /* Input Area */
      .chat-widget-input-area {
        padding: 16px;
        border-top: 1px solid #e1e8ed;
        background: white;
      }

      .chat-widget-input-wrapper {
        display: flex;
        gap: 10px;
      }

      .chat-widget-input {
        flex: 1;
        padding: 12px;
        border: 1px solid #e1e8ed;
        border-radius: 8px;
        font-size: 14px;
        font-family: inherit;
        resize: none;
        outline: none;
        direction: rtl;
        text-align: right;
      }

      .chat-widget-input:focus {
        border-color: ${config.primaryColor};
      }

      .chat-widget-send {
        padding: 12px 20px;
        background: ${config.primaryColor};
        color: white;
        border: none;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 600;
        transition: opacity 0.2s;
      }

      .chat-widget-send:hover:not(:disabled) {
        opacity: 0.9;
      }

      .chat-widget-send:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      /* Typing Indicator */
      .typing-indicator {
        display: flex;
        gap: 4px;
        padding: 12px 16px;
      }

      .typing-dot {
        width: 8px;
        height: 8px;
        background: #999;
        border-radius: 50%;
        animation: typing 1.4s infinite;
      }

      .typing-dot:nth-child(2) {
        animation-delay: 0.2s;
      }

      .typing-dot:nth-child(3) {
        animation-delay: 0.4s;
      }

      @keyframes typing {
        0%, 60%, 100% {
          transform: translateY(0);
        }
        30% {
          transform: translateY(-8px);
        }
      }

      /* Empty State */
      .chat-widget-empty {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        color: #666;
        text-align: center;
        padding: 20px;
      }

      .chat-widget-empty-icon {
        font-size: 48px;
        margin-bottom: 16px;
      }
    `;

    const styleSheet = document.createElement('style');
    styleSheet.textContent = styles;
    document.head.appendChild(styleSheet);
  }

  // ==================== HTML Creation ====================
  function createWidgetHTML(config) {
    const widgetHTML = `
      <div class="chat-widget-container">
        <button class="chat-widget-button" id="chat-widget-toggle">💬</button>
        <div class="chat-widget-window" id="chat-widget-window">
          <div class="chat-widget-header">
            <h3>${escapeHtml(config.title)}</h3>
            <button class="chat-widget-close" id="chat-widget-close">✕</button>
          </div>
          <div class="chat-widget-messages" id="chat-widget-messages">
            <div class="chat-widget-empty">
              <div class="chat-widget-empty-icon">💬</div>
              <h3>שלום!</h3>
              <p>שאל שאלה על המסמכים שלך</p>
            </div>
          </div>
          <div class="chat-widget-input-area">
            <div class="chat-widget-input-wrapper">
              <textarea 
                class="chat-widget-input" 
                id="chat-widget-input"
                placeholder="שאל שאלה..."
                rows="1"
              ></textarea>
              <button class="chat-widget-send" id="chat-widget-send">שלח</button>
            </div>
          </div>
        </div>
      </div>
    `;

    const widgetContainer = document.createElement('div');
    widgetContainer.innerHTML = widgetHTML;
    document.body.appendChild(widgetContainer);
  }

  // ==================== Event Listeners Setup ====================
  function setupEventListeners() {
    const state = {
      messages: [],
      isOpen: false,
      isLoading: false,
      sessionId: generateSessionId()
    };

    const elements = {
      toggleButton: document.getElementById('chat-widget-toggle'),
      closeButton: document.getElementById('chat-widget-close'),
      widgetWindow: document.getElementById('chat-widget-window'),
      messagesContainer: document.getElementById('chat-widget-messages'),
      inputField: document.getElementById('chat-widget-input'),
      sendButton: document.getElementById('chat-widget-send')
    };

    elements.toggleButton.addEventListener('click', () => toggleWidget(state, elements));
    elements.closeButton.addEventListener('click', () => toggleWidget(state, elements));
    elements.sendButton.addEventListener('click', () => sendMessage(state, elements));
    
    elements.inputField.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage(state, elements);
      }
    });
  }

  // ==================== Utility Functions ====================
  
  function generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  // זיהוי שפה
  function detectLanguage(text) {
    if (!text || text.trim().length === 0) return 'en';
    
    let hebrewChars = 0;
    let totalChars = 0;
    
    for (let char of text) {
      if (/\p{L}/u.test(char)) {
        totalChars++;
        if (char >= '\u0590' && char <= '\u05FF') {
          hebrewChars++;
        }
      }
    }
    
    return (totalChars > 0 && (hebrewChars / totalChars) > 0.3) ? 'he' : 'en';
  }

  function createAvatar(role) {
    const config = {
      botName: window.CHAT_WIDGET_BOT_NAME || 'AI',
      botAvatar: window.CHAT_WIDGET_BOT_AVATAR || null,
      userAvatar: window.CHAT_WIDGET_USER_AVATAR || null
    };

    if (role === 'user') {
      if (config.userAvatar) {
        return `<img src="${escapeHtml(config.userAvatar)}" alt="User" />`;
      }
      return 'אני';
    } else {
      if (config.botAvatar) {
        return `<img src="${escapeHtml(config.botAvatar)}" alt="${escapeHtml(config.botName)}" />`;
      }
      return escapeHtml(config.botName.charAt(0));
    }
  }

  // ==================== Widget Functions ====================
  
  function toggleWidget(state, elements) {
    state.isOpen = !state.isOpen;
    elements.widgetWindow.classList.toggle('open', state.isOpen);
    
    if (state.isOpen) {
      elements.inputField.focus();
      elements.toggleButton.textContent = '✕';
    } else {
      elements.toggleButton.textContent = '💬';
    }
  }

  // רינדור הודעות
  function renderMessages(state, elements) {
    if (state.messages.length === 0) {
      elements.messagesContainer.innerHTML = `
        <div class="chat-widget-empty">
          <div class="chat-widget-empty-icon">💬</div>
          <h3>שלום!</h3>
          <p>שאל שאלה על המסמכים שלך</p>
        </div>
      `;
      return;
    }

    const messagesHTML = state.messages.map(msg => {
      // זיהוי שפה
      const language = detectLanguage(msg.content);
      const textDirection = language === 'he' ? 'rtl' : 'ltr';
      
      // ניקוי טקסט - הסרת שורות ריקות ורווחים מיותרים
      const cleanedContent = msg.content
        .split('\n')
        .map(line => line.trim())
        .filter(line => line.length > 0)
        .join('\n')
        .trim();
      
      return `
        <div class="chat-message ${msg.role}">
          <div class="chat-message-avatar">${createAvatar(msg.role)}</div>
          <div class="chat-message-content">
            <div class="chat-message-bubble ${textDirection}">${escapeHtml(cleanedContent)}</div>
          </div>
        </div>
      `;
    }).join('');

    elements.messagesContainer.innerHTML = messagesHTML;

    if (state.isLoading) {
      elements.messagesContainer.innerHTML += `
        <div class="chat-message assistant">
          <div class="chat-message-avatar">${createAvatar('assistant')}</div>
          <div class="chat-message-content">
            <div class="chat-message-bubble rtl">
              <div class="typing-indicator">
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
              </div>
            </div>
          </div>
        </div>
      `;
    }

    elements.messagesContainer.scrollTop = elements.messagesContainer.scrollHeight;
  }

  async function sendMessage(state, elements) {
    const question = elements.inputField.value.trim();
    
    if (!question || state.isLoading) return;

    const config = {
      apiUrl: window.CHAT_WIDGET_API_URL || 'http://localhost:8080',
      secretKey: window.CHAT_WIDGET_SECRET_KEY
    };

    state.messages.push({
      role: 'user',
      content: question,
      timestamp: new Date().toISOString()
    });

    elements.inputField.value = '';
    state.isLoading = true;
    elements.sendButton.disabled = true;
    renderMessages(state, elements);

    try {
      const response = await fetch(`${config.apiUrl}/api/query/ask`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          secretKey: config.secretKey,
          question: question,
          sessionId: state.sessionId
        })
      });

      const data = await response.json();

      if (data.success && data.data.answer) {
        state.messages.push({
          role: 'assistant',
          content: data.data.answer,
          timestamp: new Date().toISOString()
        });
      } else {
        state.messages.push({
          role: 'assistant',
          content: 'מצטער, לא הצלחתי למצוא תשובה. אנא נסה שוב.',
          timestamp: new Date().toISOString()
        });
      }
    } catch (error) {
      console.error('Chat Widget Error:', error);
      state.messages.push({
        role: 'assistant',
        content: 'אירעה שגיאה. אנא נסה שוב מאוחר יותר.',
        timestamp: new Date().toISOString()
      });
    } finally {
      state.isLoading = false;
      elements.sendButton.disabled = false;
      renderMessages(state, elements);
      elements.inputField.focus();
    }
  }

  console.log('✅ Chat Widget initialized successfully');
})();