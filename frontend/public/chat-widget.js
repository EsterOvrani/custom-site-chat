// frontend/public/chat-widget.js - עם תמיכה בהקלטת קול

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
      userAvatar: window.CHAT_WIDGET_USER_AVATAR || null,
      maxHistoryMessages: 10,
      voiceEnabled: true // ✅ תמיכה בקול
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
    setupEventListeners(WIDGET_CONFIG);
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
        position: relative;
      }

      .chat-widget-header h3 {
        margin: 0;
        font-size: 18px;
      }

      /* Message Counter */
      .message-counter {
        font-size: 11px;
        opacity: 0.9;
        margin-top: 3px;
      }

      /* Reset Button */
      .reset-button {
        position: absolute;
        left: 20px;
        top: 50%;
        transform: translateY(-50%);
        background: rgba(255,255,255,0.2);
        border: 1px solid rgba(255,255,255,0.3);
        color: white;
        padding: 6px 12px;
        border-radius: 6px;
        cursor: pointer;
        font-size: 11px;
        transition: all 0.3s;
        display: none;
      }

      .reset-button:hover {
        background: rgba(255,255,255,0.3);
      }

      .reset-button.show {
        display: block;
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

      .chat-message.user {
        flex-direction: row-reverse;
        justify-content: flex-start;
      }

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

      .chat-message-bubble.rtl {
        direction: rtl;
        text-align: right;
      }

      .chat-message-bubble.ltr {
        direction: ltr;
        text-align: left;
      }

      .chat-message.user .chat-message-bubble {
        background: ${config.primaryColor};
        color: white;
      }

      .chat-message.assistant .chat-message-bubble {
        background: white;
        color: #333;
        border: 1px solid #e1e8ed;
      }

      /* 🎤 Voice Message Indicator */
      .voice-message-indicator {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        opacity: 0.9;
        margin-top: 4px;
      }

      /* Limit Warning */
      .limit-warning {
        background: #fff3cd;
        color: #856404;
        padding: 10px;
        border-radius: 8px;
        margin: 10px 20px;
        font-size: 13px;
        text-align: center;
        display: none;
      }

      .limit-warning.show {
        display: block;
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
        align-items: flex-end;
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
        max-height: 120px;
      }

      .chat-widget-input:focus {
        border-color: ${config.primaryColor};
      }

      .chat-widget-input:disabled {
        background-color: #f5f5f5;
        cursor: not-allowed;
      }

      /* 🎤 Voice Button */
      .chat-widget-voice {
        padding: 12px;
        background: white;
        color: ${config.primaryColor};
        border: 2px solid ${config.primaryColor};
        border-radius: 8px;
        cursor: pointer;
        font-size: 20px;
        transition: all 0.3s;
        min-width: 48px;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .chat-widget-voice:hover:not(:disabled) {
        background: ${config.primaryColor};
        color: white;
      }

      .chat-widget-voice:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      .chat-widget-voice.recording {
        background: #dc3545;
        color: white;
        border-color: #dc3545;
        animation: pulse 1.5s infinite;
      }

      @keyframes pulse {
        0%, 100% {
          opacity: 1;
        }
        50% {
          opacity: 0.7;
        }
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

      /* 🎤 Recording Indicator */
      .recording-indicator {
        position: absolute;
        bottom: 100%;
        left: 50%;
        transform: translateX(-50%);
        background: #dc3545;
        color: white;
        padding: 8px 16px;
        border-radius: 8px;
        margin-bottom: 10px;
        font-size: 13px;
        display: none;
        align-items: center;
        gap: 8px;
        box-shadow: 0 2px 8px rgba(0,0,0,0.2);
      }

      .recording-indicator.active {
        display: flex;
      }

      .recording-dot {
        width: 8px;
        height: 8px;
        background: white;
        border-radius: 50%;
        animation: blink 1s infinite;
      }

      @keyframes blink {
        0%, 100% {
          opacity: 1;
        }
        50% {
          opacity: 0.3;
        }
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

      /* 🎤 Browser Not Supported Warning */
      .browser-warning {
        background: #f8d7da;
        color: #721c24;
        padding: 10px;
        border-radius: 8px;
        margin: 10px 20px;
        font-size: 13px;
        text-align: center;
        display: none;
      }

      .browser-warning.show {
        display: block;
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
            <button class="reset-button" id="reset-button">
              🔄 התחל שיחה חדשה
            </button>
            <div>
              <h3>${escapeHtml(config.title)}</h3>
              <div class="message-counter" id="message-counter">0/10 הודעות</div>
            </div>
          </div>
          <div class="limit-warning" id="limit-warning">
            ⚠️ הגעת למגבלת 10 הודעות. לחץ על "התחל שיחה חדשה" למעלה.
          </div>
          <div class="browser-warning" id="browser-warning">
            ⚠️ הדפדפן שלך לא תומך בהקלטת קול
          </div>
          <div class="chat-widget-messages" id="chat-widget-messages">
            <div class="chat-widget-empty">
              <div class="chat-widget-empty-icon">💬</div>
              <h3>שלום!</h3>
              <p>שאל שאלה על המסמכים שלך</p>
              ${config.voiceEnabled ? '<p style="font-size: 12px; margin-top: 10px;">💡 אפשר גם להקליט קול!</p>' : ''}
            </div>
          </div>
          <div class="chat-widget-input-area">
            <div class="recording-indicator" id="recording-indicator">
              <div class="recording-dot"></div>
              <span>מקליט...</span>
            </div>
            <div class="chat-widget-input-wrapper">
              <textarea 
                class="chat-widget-input" 
                id="chat-widget-input"
                placeholder="שאל שאלה או הקלט קול..."
                rows="1"
              ></textarea>
              ${config.voiceEnabled ? '<button class="chat-widget-voice" id="chat-widget-voice" title="הקלט קול">🎤</button>' : ''}
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
  function setupEventListeners(config) {
    const state = {
      messages: [],
      history: [],
      isOpen: false,
      isLoading: false,
      isRecording: false,
      recognition: null,
      sessionId: generateSessionId(),
      maxHistoryMessages: config.maxHistoryMessages
    };

    const elements = {
      toggleButton: document.getElementById('chat-widget-toggle'),
      resetButton: document.getElementById('reset-button'),
      widgetWindow: document.getElementById('chat-widget-window'),
      messagesContainer: document.getElementById('chat-widget-messages'),
      inputField: document.getElementById('chat-widget-input'),
      sendButton: document.getElementById('chat-widget-send'),
      voiceButton: document.getElementById('chat-widget-voice'),
      messageCounter: document.getElementById('message-counter'),
      limitWarning: document.getElementById('limit-warning'),
      browserWarning: document.getElementById('browser-warning'),
      recordingIndicator: document.getElementById('recording-indicator')
    };

    // בדיקה אם הדפדפן תומך ב-Web Speech API
    if (config.voiceEnabled) {
      setupVoiceRecognition(state, elements, config);
    }

    // טען היסטוריה מ-sessionStorage
    loadHistoryFromSession(state, elements, config);

    elements.toggleButton.addEventListener('click', () => toggleWidget(state, elements));
    elements.resetButton.addEventListener('click', () => resetChat(state, elements, config));
    elements.sendButton.addEventListener('click', () => sendMessage(state, elements, config));
    
    if (elements.voiceButton) {
      elements.voiceButton.addEventListener('click', () => toggleVoiceRecording(state, elements, config));
    }
    
    elements.inputField.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage(state, elements, config);
      }
    });

    // Auto-resize textarea
    elements.inputField.addEventListener('input', () => {
      elements.inputField.style.height = 'auto';
      elements.inputField.style.height = elements.inputField.scrollHeight + 'px';
    });
  }

  // ==================== 🎤 Voice Recognition Setup ====================
  
  function setupVoiceRecognition(state, elements, config) {
    // בדיקה אם הדפדפן תומך
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    
    if (!SpeechRecognition) {
      console.warn('Speech Recognition not supported in this browser');
      if (elements.voiceButton) {
        elements.voiceButton.style.display = 'none';
      }
      elements.browserWarning.classList.add('show');
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = false; // הקלטה של משפט אחד
    recognition.interimResults = false; // רק תוצאות סופיות
    recognition.lang = 'he-IL'; // עברית כברירת מחדל
    recognition.maxAlternatives = 1;

    recognition.onstart = () => {
      console.log('🎤 Voice recording started');
      state.isRecording = true;
      elements.voiceButton.classList.add('recording');
      elements.recordingIndicator.classList.add('active');
      elements.inputField.placeholder = 'מקשיב...';
    };

    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      console.log('🎤 Transcript:', transcript);
      
      // הוסף את הטקסט לשדה הקלט
      elements.inputField.value = transcript;
      elements.inputField.style.height = 'auto';
      elements.inputField.style.height = elements.inputField.scrollHeight + 'px';
      
      // שלח אוטומטית
      setTimeout(() => {
        sendMessage(state, elements, config, true); // true = from voice
      }, 300);
    };

    recognition.onerror = (event) => {
      console.error('🎤 Speech recognition error:', event.error);
      
      let errorMessage = 'שגיאה בהקלטת קול';
      switch(event.error) {
        case 'no-speech':
          errorMessage = 'לא זוהה דיבור';
          break;
        case 'audio-capture':
          errorMessage = 'לא ניתן לגשת למיקרופון';
          break;
        case 'not-allowed':
          errorMessage = 'נדרשת הרשאה למיקרופון';
          break;
      }
      
      elements.inputField.placeholder = errorMessage;
      setTimeout(() => {
        elements.inputField.placeholder = 'שאל שאלה או הקלט קול...';
      }, 3000);
    };

    recognition.onend = () => {
      console.log('🎤 Voice recording ended');
      state.isRecording = false;
      elements.voiceButton.classList.remove('recording');
      elements.recordingIndicator.classList.remove('active');
      elements.inputField.placeholder = 'שאל שאלה או הקלט קול...';
    };

    state.recognition = recognition;
  }

  function toggleVoiceRecording(state, elements, config) {
    if (!state.recognition) {
      alert('הדפדפן שלך לא תומך בהקלטת קול');
      return;
    }

    if (isAtLimit(state)) {
      return;
    }

    if (state.isRecording) {
      // עצור הקלטה
      state.recognition.stop();
    } else {
      // התחל הקלטה
      try {
        // זיהוי שפה אוטומטי לפי השדה קיים
        const currentText = elements.inputField.value;
        const lang = detectLanguage(currentText);
        state.recognition.lang = lang === 'he' ? 'he-IL' : 'en-US';
        
        state.recognition.start();
      } catch (error) {
        console.error('Failed to start recording:', error);
        alert('שגיאה בהפעלת ההקלטה');
      }
    }
  }

  // ==================== History Management ====================
  
  function loadHistoryFromSession(state, elements, config) {
    try {
      const storageKey = 'chatHistory_' + config.secretKey;
      const saved = sessionStorage.getItem(storageKey);
      if (saved) {
        const data = JSON.parse(saved);
        state.history = data.history || [];
        state.messages = data.messages || [];
        
        renderMessages(state, elements, config);
        updateUI(state, elements);
        
        console.log('✅ Loaded history:', state.history.length, 'messages');
      }
    } catch (e) {
      console.error('Failed to load history:', e);
    }
  }

  function saveHistoryToSession(state, config) {
    try {
      const storageKey = 'chatHistory_' + config.secretKey;
      sessionStorage.setItem(
        storageKey,
        JSON.stringify({
          history: state.history,
          messages: state.messages
        })
      );
    } catch (e) {
      console.error('Failed to save history:', e);
    }
  }

  function isAtLimit(state) {
    return state.history.length >= state.maxHistoryMessages;
  }

  function updateUI(state, elements) {
    const messageCount = state.history.length;
    
    // עדכן מונה
    elements.messageCounter.textContent = `${messageCount}/${state.maxHistoryMessages} הודעות`;
    
    // הצג/הסתר כפתור איפוס
    if (messageCount > 0) {
      elements.resetButton.classList.add('show');
    } else {
      elements.resetButton.classList.remove('show');
    }
    
    // הצג אזהרה אם הגענו למגבלה
    if (isAtLimit(state)) {
      elements.limitWarning.classList.add('show');
      elements.inputField.disabled = true;
      elements.sendButton.disabled = true;
      if (elements.voiceButton) {
        elements.voiceButton.disabled = true;
      }
    } else {
      elements.limitWarning.classList.remove('show');
      elements.inputField.disabled = false;
      elements.sendButton.disabled = false;
      if (elements.voiceButton) {
        elements.voiceButton.disabled = false;
      }
    }
  }

  function resetChat(state, elements, config) {
    if (confirm('האם אתה בטוח שברצונך להתחיל שיחה חדשה? ההיסטוריה תימחק.')) {
      state.history = [];
      state.messages = [];
      const storageKey = 'chatHistory_' + config.secretKey;
      sessionStorage.removeItem(storageKey);
      
      renderMessages(state, elements, config);
      updateUI(state, elements);
      
      console.log('✅ Chat reset');
    }
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

  function createAvatar(role, config) {
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
      
      // עצור הקלטה אם פתוחה
      if (state.isRecording && state.recognition) {
        state.recognition.stop();
      }
    }
  }

  function renderMessages(state, elements, config) {
    if (state.messages.length === 0) {
      elements.messagesContainer.innerHTML = `
        <div class="chat-widget-empty">
          <div class="chat-widget-empty-icon">💬</div>
          <h3>שלום!</h3>
          <p>שאל שאלה על המסמכים שלך</p>
          ${config.voiceEnabled ? '<p style="font-size: 12px; margin-top: 10px;">💡 אפשר גם להקליט קול!</p>' : ''}
        </div>
      `;
      return;
    }

    const messagesHTML = state.messages.map(msg => {
      const language = detectLanguage(msg.content);
      const textDirection = language === 'he' ? 'rtl' : 'ltr';
      
      const cleanedContent = msg.content
        .split('\n')
        .map(line => line.trim())
        .filter(line => line.length > 0)
        .join('\n')
        .trim();
      
      const voiceIndicator = msg.isVoice ? '<div class="voice-message-indicator">🎤 הודעת קול</div>' : '';
      
      return `
        <div class="chat-message ${msg.role}">
          <div class="chat-message-avatar">${createAvatar(msg.role, config)}</div>
          <div class="chat-message-content">
            <div class="chat-message-bubble ${textDirection}">${escapeHtml(cleanedContent)}</div>
            ${voiceIndicator}
          </div>
        </div>
      `;
    }).join('');

    elements.messagesContainer.innerHTML = messagesHTML;

    if (state.isLoading) {
      elements.messagesContainer.innerHTML += `
        <div class="chat-message assistant">
          <div class="chat-message-avatar">${createAvatar('assistant', config)}</div>
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

  async function sendMessage(state, elements, config, isVoice = false) {
    const question = elements.inputField.value.trim();
    
    if (!question || state.isLoading || isAtLimit(state)) return;

    // הוסף הודעת משתמש להצגה
    state.messages.push({
      role: 'user',
      content: question,
      timestamp: new Date().toISOString(),
      isVoice: isVoice
    });

    // הוסף להיסטוריה
    state.history.push({
      role: 'user',
      content: question
    });

    elements.inputField.value = '';
    elements.inputField.style.height = 'auto';
    state.isLoading = true;
    elements.sendButton.disabled = true;
    if (elements.voiceButton) {
      elements.voiceButton.disabled = true;
    }
    
    renderMessages(state, elements, config);
    saveHistoryToSession(state, config);
    updateUI(state, elements);

    try {
      const response = await fetch(`${config.apiUrl}/api/query/ask`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          secretKey: config.secretKey,
          question: question,
          history: state.history
        })
      });

      const data = await response.json();

      if (data.success && data.data.answer) {
        // הוסף תשובה להצגה
        state.messages.push({
          role: 'assistant',
          content: data.data.answer,
          timestamp: new Date().toISOString(),
          isVoice: false
        });

        // הוסף תשובה להיסטוריה
        state.history.push({
          role: 'assistant',
          content: data.data.answer
        });
      } else {
        state.messages.push({
          role: 'assistant',
          content: 'מצטער, לא הצלחתי למצוא תשובה. אנא נסה שוב.',
          timestamp: new Date().toISOString(),
          isVoice: false
        });
      }
    } catch (error) {
      console.error('Chat Widget Error:', error);
      state.messages.push({
        role: 'assistant',
        content: 'אירעה שגיאה. אנא נסה שוב מאוחר יותר.',
        timestamp: new Date().toISOString(),
        isVoice: false
      });
    } finally {
      state.isLoading = false;
      elements.sendButton.disabled = false;
      if (elements.voiceButton && !isAtLimit(state)) {
        elements.voiceButton.disabled = false;
      }
      
      renderMessages(state, elements, config);
      saveHistoryToSession(state, config);
      updateUI(state, elements);
      elements.inputField.focus();
    }
  }

  console.log('✅ Chat Widget with Voice initialized successfully');
})();