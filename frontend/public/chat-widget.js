(function() {
  'use strict';

  // ⭐ המתן שהדף ייטען לגמרי לפני קריאת המשתנים
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

    // ⭐ Debug - בדיקה
    console.log('🔧 Widget Config:', WIDGET_CONFIG);

    if (!WIDGET_CONFIG.secretKey) {
      console.error('❌ Chat Widget: Missing SECRET_KEY');
      return;
    }

    // ==================== CSS Styles ====================
    const styles = `
      .chat-widget-container {
        position: fixed;
        ${WIDGET_CONFIG.position.includes('right') ? 'right: 20px;' : 'left: 20px;'}
        bottom: 20px;
        z-index: 9999;
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        direction: rtl;
      }

      .chat-widget-button {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: linear-gradient(135deg, ${WIDGET_CONFIG.primaryColor} 0%, ${WIDGET_CONFIG.secondaryColor} 100%);
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

      .chat-widget-window {
        position: absolute;
        bottom: 80px;
        ${WIDGET_CONFIG.position.includes('right') ? 'right: 0;' : 'left: 0;'}
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

      .chat-widget-header {
        background: linear-gradient(135deg, ${WIDGET_CONFIG.primaryColor} 0%, ${WIDGET_CONFIG.secondaryColor} 100%);
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

      .chat-widget-messages {
        flex: 1;
        overflow-y: auto;
        padding: 20px;
        background: #f8f9ff;
      }

      .chat-message {
        margin-bottom: 16px;
        display: flex;
        gap: 10px;
      }

      .chat-message.user {
        flex-direction: row-reverse;
      }

      .chat-message-avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: ${WIDGET_CONFIG.primaryColor};
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
        background: ${WIDGET_CONFIG.secondaryColor};
      }

      .chat-message-avatar img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .chat-message-bubble {
        padding: 12px 16px;
        border-radius: 12px;
        max-width: 70%;
        line-height: 1.5;
        font-size: 14px;
      }

      .chat-message.user .chat-message-bubble {
        background: ${WIDGET_CONFIG.primaryColor};
        color: white;
        text-align: right;
      }

      .chat-message.assistant .chat-message-bubble {
        background: white;
        color: #333;
        text-align: right;
        border: 1px solid #e1e8ed;
      }

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
      }

      .chat-widget-input:focus {
        border-color: ${WIDGET_CONFIG.primaryColor};
      }

      .chat-widget-send {
        padding: 12px 20px;
        background: ${WIDGET_CONFIG.primaryColor};
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

    // ==================== HTML Structure ====================
    const widgetHTML = `
      <div class="chat-widget-container">
        <button class="chat-widget-button" id="chat-widget-toggle">
          💬
        </button>
        <div class="chat-widget-window" id="chat-widget-window">
          <div class="chat-widget-header">
            <h3>${escapeHtml(WIDGET_CONFIG.title)}</h3>
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

    // ==================== State ====================
    let messages = [];
    let isOpen = false;
    let isLoading = false;
    let sessionId = generateSessionId();

    // ==================== DOM Elements ====================
    const toggleButton = document.getElementById('chat-widget-toggle');
    const closeButton = document.getElementById('chat-widget-close');
    const widgetWindow = document.getElementById('chat-widget-window');
    const messagesContainer = document.getElementById('chat-widget-messages');
    const inputField = document.getElementById('chat-widget-input');
    const sendButton = document.getElementById('chat-widget-send');

    // ==================== Event Listeners ====================
    toggleButton.addEventListener('click', toggleWidget);
    closeButton.addEventListener('click', toggleWidget);
    sendButton.addEventListener('click', sendMessage);
    inputField.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });

    // ==================== Functions ====================
    function generateSessionId() {
      return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    function toggleWidget() {
      isOpen = !isOpen;
      widgetWindow.classList.toggle('open', isOpen);
      
      if (isOpen) {
        inputField.focus();
        toggleButton.textContent = '✕';
      } else {
        toggleButton.textContent = '💬';
      }
    }

    function createAvatar(role) {
      if (role === 'user') {
        if (WIDGET_CONFIG.userAvatar) {
          return `<img src="${escapeHtml(WIDGET_CONFIG.userAvatar)}" alt="User" />`;
        }
        return 'אני';
      } else {
        if (WIDGET_CONFIG.botAvatar) {
          return `<img src="${escapeHtml(WIDGET_CONFIG.botAvatar)}" alt="${escapeHtml(WIDGET_CONFIG.botName)}" />`;
        }
        const firstLetter = WIDGET_CONFIG.botName.charAt(0);
        return escapeHtml(firstLetter);
      }
    }

    function renderMessages() {
      if (messages.length === 0) {
        messagesContainer.innerHTML = `
          <div class="chat-widget-empty">
            <div class="chat-widget-empty-icon">💬</div>
            <h3>שלום!</h3>
            <p>שאל שאלה על המסמכים שלך</p>
          </div>
        `;
        return;
      }

      messagesContainer.innerHTML = messages.map(msg => `
        <div class="chat-message ${msg.role}">
          <div class="chat-message-avatar">${createAvatar(msg.role)}</div>
          <div class="chat-message-bubble">${escapeHtml(msg.content)}</div>
        </div>
      `).join('');

      if (isLoading) {
        messagesContainer.innerHTML += `
          <div class="chat-message assistant">
            <div class="chat-message-avatar">${createAvatar('assistant')}</div>
            <div class="chat-message-bubble">
              <div class="typing-indicator">
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
              </div>
            </div>
          </div>
        `;
      }

      messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    async function sendMessage() {
      const question = inputField.value.trim();
      
      if (!question || isLoading) return;

      messages.push({
        role: 'user',
        content: question,
        timestamp: new Date().toISOString()
      });

      inputField.value = '';
      isLoading = true;
      sendButton.disabled = true;
      renderMessages();

      try {
        const response = await fetch(`${WIDGET_CONFIG.apiUrl}/api/query/ask`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            secretKey: WIDGET_CONFIG.secretKey,
            question: question,
            sessionId: sessionId
          })
        });

        const data = await response.json();

        if (data.success && data.data.answer) {
          messages.push({
            role: 'assistant',
            content: data.data.answer,
            timestamp: new Date().toISOString()
          });
        } else {
          messages.push({
            role: 'assistant',
            content: 'מצטער, לא הצלחתי למצוא תשובה. אנא נסה שוב.',
            timestamp: new Date().toISOString()
          });
        }
      } catch (error) {
        console.error('Chat Widget Error:', error);
        messages.push({
          role: 'assistant',
          content: 'אירעה שגיאה. אנא נסה שוב מאוחר יותר.',
          timestamp: new Date().toISOString()
        });
      } finally {
        isLoading = false;
        sendButton.disabled = false;
        renderMessages();
        inputField.focus();
      }
    }

    function escapeHtml(text) {
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    }

    console.log('✅ Chat Widget loaded successfully');
  }
})();