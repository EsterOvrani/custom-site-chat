// frontend/public/chat-widget.js - גרסה נקייה בלי voice indicator

(function() {
  'use strict';

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initWidget);
  } else {
    initWidget();
  }

  function initWidget() {
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
      voiceEnabled: true
    };

    if (!WIDGET_CONFIG.secretKey) {
      console.error('❌ Chat Widget: Missing SECRET_KEY');
      return;
    }

    injectStyles(WIDGET_CONFIG);
    createWidgetHTML(WIDGET_CONFIG);
    setupEventListeners(WIDGET_CONFIG);
  }

  function injectStyles(config) {
    const styles = `
      .chat-widget-container {
        position: fixed;
        ${config.position.includes('right') ? 'right: 20px;' : 'left: 20px;'}
        bottom: 20px;
        z-index: 9999;
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      }

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

      .message-counter {
        font-size: 11px;
        opacity: 0.9;
        margin-top: 3px;
      }

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

      .chat-widget-messages {
        flex: 1;
        overflow-y: auto;
        padding: 20px;
        background: #f8f9ff;
      }

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

      .chat-message-content {
        display: flex;
        flex-direction: column;
        max-width: 70%;
      }

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

      .recording-bar {
        background: #fce8e6;
        color: #d93025;
        padding: 8px 16px;
        display: none;
        align-items: center;
        justify-content: space-between;
        font-size: 13px;
        border-bottom: 1px solid #f4b4af;
      }

      .recording-bar.active {
        display: flex;
      }

      .recording-timer {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 500;
      }

      .recording-dot {
        width: 8px;
        height: 8px;
        background: #d93025;
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

      .cancel-btn {
        background: transparent;
        border: none;
        color: #d93025;
        padding: 4px 8px;
        cursor: pointer;
        font-size: 12px;
        border-radius: 4px;
      }

      .cancel-btn:hover {
        background: rgba(217,48,37,0.1);
      }

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

      .input-container {
        flex: 1;
        background: #f1f3f4;
        border-radius: 24px;
        padding: 4px 16px;
        display: flex;
        align-items: center;
        gap: 8px;
        transition: background 0.3s;
      }

      .input-container.recording {
        background: #fce8e6;
      }

      .chat-widget-input {
        flex: 1;
        padding: 10px 0;
        border: none;
        background: transparent;
        font-size: 14px;
        font-family: inherit;
        resize: none;
        outline: none;
        direction: rtl;
        text-align: right;
        max-height: 100px;
      }

      .chat-widget-input::placeholder {
        color: #5f6368;
      }

      .chat-widget-input:disabled {
        cursor: not-allowed;
        opacity: 0.6;
      }

      .voice-btn {
        background: transparent;
        border: none;
        cursor: pointer;
        padding: 6px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        transition: background 0.2s;
        flex-shrink: 0;
      }

      .voice-btn:hover:not(:disabled) {
        background: rgba(0,0,0,0.08);
      }

      .voice-btn:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .voice-btn.recording {
        animation: pulse 1.5s infinite;
      }

      @keyframes pulse {
        0%, 100% {
          opacity: 1;
        }
        50% {
          opacity: 0.6;
        }
      }

      .mic-icon {
        width: 20px;
        height: 20px;
        fill: #5f6368;
      }

      .voice-btn.recording .mic-icon {
        fill: #d93025;
      }

      .send-btn {
        background: ${config.primaryColor};
        color: white;
        border: none;
        border-radius: 50%;
        width: 40px;
        height: 40px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
        flex-shrink: 0;
      }

      .send-btn:hover:not(:disabled) {
        background: #5568d3;
        box-shadow: 0 2px 4px rgba(0,0,0,0.2);
      }

      .send-btn:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .send-icon {
        width: 20px;
        height: 20px;
        fill: white;
        transform: rotate(180deg);
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
            ⚠️ הגעת למגבלת 10 הודעות. לחץ על "התחל שיחה חדשה".
          </div>
          <div class="browser-warning" id="browser-warning">
            ⚠️ הדפדפן שלך לא תומך בהקלטת קול
          </div>
          <div class="recording-bar" id="recording-bar">
            <div class="recording-timer">
              <div class="recording-dot"></div>
              <span>מקליט <span id="recording-timer">0:00</span></span>
            </div>
            <button class="cancel-btn" id="cancel-recording-btn">ביטול</button>
          </div>
          <div class="chat-widget-messages" id="chat-widget-messages">
            <div class="chat-widget-empty">
              <div class="chat-widget-empty-icon">💬</div>
              <h3>שלום!</h3>
              <p>שאל שאלה על המסמכים שלך</p>
              ${config.voiceEnabled ? '<p style="font-size: 12px; margin-top: 10px;">💡 לחץ על 🎤 להקלטת הודעה קולית!</p>' : ''}
            </div>
          </div>
          <div class="chat-widget-input-area">
            <div class="chat-widget-input-wrapper">
              <div class="input-container" id="input-container">
                <textarea 
                  class="chat-widget-input" 
                  id="chat-widget-input"
                  placeholder="הקלד הודעה..."
                  rows="1"
                ></textarea>
                ${config.voiceEnabled ? `
                <button class="voice-btn" id="chat-widget-voice" title="הקלט הודעה קולית">
                  <svg class="mic-icon" viewBox="0 0 24 24">
                    <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
                    <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
                  </svg>
                </button>
                ` : ''}
              </div>
              <button class="send-btn" id="chat-widget-send" title="שלח">
                <svg class="send-icon" viewBox="0 0 24 24">
                  <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    `;

    const widgetContainer = document.createElement('div');
    widgetContainer.innerHTML = widgetHTML;
    document.body.appendChild(widgetContainer);
  }

  function setupEventListeners(config) {
    const state = {
      messages: [],
      history: [],
      isOpen: false,
      isLoading: false,
      isRecording: false,
      recordingStartTime: null,
      recordingTimer: null,
      recognition: null,
      currentLanguage: 'he-IL',
      sessionId: generateSessionId(),
      maxHistoryMessages: config.maxHistoryMessages,
      currentTranscript: ''
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
      recordingBar: document.getElementById('recording-bar'),
      recordingTimer: document.getElementById('recording-timer'),
      cancelRecordingBtn: document.getElementById('cancel-recording-btn'),
      inputContainer: document.getElementById('input-container')
    };

    if (config.voiceEnabled) {
      setupVoiceRecognition(state, elements);
    }

    loadHistoryFromSession(state, elements, config);

    elements.toggleButton.addEventListener('click', () => toggleWidget(state, elements));
    elements.resetButton.addEventListener('click', () => resetChat(state, elements, config));
    elements.sendButton.addEventListener('click', () => sendMessage(state, elements, config));
    
    if (elements.voiceButton) {
      elements.voiceButton.addEventListener('click', () => toggleVoiceRecording(state, elements));
    }

    if (elements.cancelRecordingBtn) {
      elements.cancelRecordingBtn.addEventListener('click', () => cancelRecording(state, elements));
    }
    
    elements.inputField.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage(state, elements, config);
      }
    });

    elements.inputField.addEventListener('input', () => {
      elements.inputField.style.height = 'auto';
      elements.inputField.style.height = elements.inputField.scrollHeight + 'px';
    });
  }

  function setupVoiceRecognition(state, elements) {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    
    if (!SpeechRecognition) {
      if (elements.voiceButton) {
        elements.voiceButton.style.display = 'none';
      }
      elements.browserWarning.classList.add('show');
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = state.currentLanguage;
    recognition.maxAlternatives = 1;

    let finalTranscript = '';
    let interimTranscript = '';
    let detectedLanguageSwitch = false;

    recognition.onstart = () => {
      state.isRecording = true;
      state.recordingStartTime = Date.now();
      finalTranscript = '';
      interimTranscript = '';
      detectedLanguageSwitch = false;
      
      elements.voiceButton.classList.add('recording');
      elements.recordingBar.classList.add('active');
      elements.inputContainer.classList.add('recording');
      elements.inputField.placeholder = 'מקשיב...';
      
      elements.inputField.disabled = false;
      elements.sendButton.disabled = false;
      
      state.recordingTimer = setInterval(() => {
        const elapsed = Math.floor((Date.now() - state.recordingStartTime) / 1000);
        const minutes = Math.floor(elapsed / 60);
        const seconds = elapsed % 60;
        elements.recordingTimer.textContent = `${minutes}:${seconds.toString().padStart(2, '0')}`;
      }, 100);
    };

    recognition.onresult = (event) => {
      interimTranscript = '';
      
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          finalTranscript += transcript + ' ';
        } else {
          interimTranscript += transcript;
        }
      }
      
      const fullText = finalTranscript + interimTranscript;
      elements.inputField.value = fullText;
      elements.inputField.style.height = 'auto';
      elements.inputField.style.height = elements.inputField.scrollHeight + 'px';
      
      state.currentTranscript = finalTranscript.trim();
      
      if (!detectedLanguageSwitch && finalTranscript.trim().length > 0) {
        const detectedLang = detectLanguageFromText(finalTranscript);
        const currentRecognitionLang = recognition.lang;
        
        if (detectedLang && detectedLang !== currentRecognitionLang) {
          detectedLanguageSwitch = true;
          recognition.stop();
          state.currentLanguage = detectedLang;
          recognition.lang = detectedLang;
          
          setTimeout(() => {
            try {
              recognition.start();
            } catch (e) {
              console.error('Failed to restart recognition:', e);
            }
          }, 100);
          
          return;
        }
      }
    };

    recognition.onerror = (event) => {
      if (event.error !== 'aborted' && event.error !== 'no-speech') {
        let errorMessage = 'שגיאה בהקלטת קול';
        switch(event.error) {
          case 'audio-capture':
            errorMessage = 'לא ניתן לגשת למיקרופון';
            break;
          case 'not-allowed':
            errorMessage = 'נדרשת הרשאה למיקרופון';
            break;
          case 'network':
            errorMessage = 'נדרש חיבור אינטרנט';
            break;
        }
        
        if (event.error !== 'aborted') {
          alert(errorMessage);
        }
      }
      
      stopRecording(state, elements);
    };

    recognition.onend = () => {
      if (finalTranscript.trim().length > 0) {
        state.currentTranscript = finalTranscript.trim();
      }
      
      if (state.isRecording && !detectedLanguageSwitch) {
        setTimeout(() => {
          stopRecording(state, elements);
        }, 300);
      }
    };

    state.recognition = recognition;
  }

  function detectLanguageFromText(text) {
    if (!text || text.trim().length === 0) return null;
    
    let hebrewChars = 0;
    let englishChars = 0;
    let totalChars = 0;
    
    for (let char of text) {
      if (/\p{L}/u.test(char)) {
        totalChars++;
        if (char >= '\u0590' && char <= '\u05FF') {
          hebrewChars++;
        } else if (/[a-zA-Z]/.test(char)) {
          englishChars++;
        }
      }
    }
    
    if (totalChars === 0) return null;
    
    const hebrewRatio = hebrewChars / totalChars;
    const englishRatio = englishChars / totalChars;
    
    if (hebrewRatio > 0.3) {
      return 'he-IL';
    } else if (englishRatio > 0.3) {
      return 'en-US';
    }
    
    return null;
  }

  function toggleVoiceRecording(state, elements) {
    if (!state.recognition) {
      alert('הדפדפן שלך לא תומך בהקלטת קול');
      return;
    }

    if (isAtLimit(state)) {
      return;
    }

    if (state.isRecording) {
      state.recognition.stop();
    } else {
      try {
        state.recognition.start();
      } catch (error) {
        alert('שגיאה בהפעלת ההקלטה');
      }
    }
  }

  function stopRecording(state, elements) {
    state.isRecording = false;
    
    if (state.recordingTimer) {
      clearInterval(state.recordingTimer);
      state.recordingTimer = null;
    }
    
    elements.voiceButton.classList.remove('recording');
    elements.recordingBar.classList.remove('active');
    elements.inputContainer.classList.remove('recording');
    elements.recordingTimer.textContent = '0:00';
    elements.inputField.placeholder = 'הקלד הודעה...';
    
    elements.inputField.disabled = false;
    elements.sendButton.disabled = false;
    elements.inputField.focus();
    
    state.currentTranscript = '';
  }

  function cancelRecording(state, elements) {
    if (state.recognition && state.isRecording) {
      elements.inputField.value = '';
      state.currentTranscript = '';
      state.recognition.stop();
    }
  }

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
    
    elements.messageCounter.textContent = `${messageCount}/${state.maxHistoryMessages} הודעות`;
    
    if (messageCount > 0) {
      elements.resetButton.classList.add('show');
    } else {
      elements.resetButton.classList.remove('show');
    }
    
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
      state.currentTranscript = '';
      
      const storageKey = 'chatHistory_' + config.secretKey;
      sessionStorage.removeItem(storageKey);
      
      renderMessages(state, elements, config);
      updateUI(state, elements);
    }
  }

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

  function toggleWidget(state, elements) {
    state.isOpen = !state.isOpen;
    elements.widgetWindow.classList.toggle('open', state.isOpen);
    
    if (state.isOpen) {
      elements.inputField.focus();
      elements.toggleButton.textContent = '✕';
    } else {
      elements.toggleButton.textContent = '💬';
      
      if (state.isRecording && state.recognition) {
        cancelRecording(state, elements);
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
          ${config.voiceEnabled ? '<p style="font-size: 12px; margin-top: 10px;">💡 לחץ על 🎤 להקלטת הודעה קולית!</p>' : ''}
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
      
      return `
        <div class="chat-message ${msg.role}">
          <div class="chat-message-avatar">${createAvatar(msg.role, config)}</div>
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

  async function sendMessage(state, elements, config) {
    const question = elements.inputField.value.trim();
    
    if (!question || state.isLoading || isAtLimit(state)) return;

    state.messages.push({
      role: 'user',
      content: question,
      timestamp: new Date().toISOString()
    });

    state.history.push({
      role: 'user',
      content: question
    });

    elements.inputField.value = '';
    elements.inputField.style.height = 'auto';
    state.currentTranscript = '';
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
        state.messages.push({
          role: 'assistant',
          content: data.data.answer,
          timestamp: new Date().toISOString()
        });

        state.history.push({
          role: 'assistant',
          content: data.data.answer
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
      if (elements.voiceButton && !isAtLimit(state)) {
        elements.voiceButton.disabled = false;
      }
      
      renderMessages(state, elements, config);
      saveHistoryToSession(state, config);
      updateUI(state, elements);
      elements.inputField.focus();
    }
  }

  console.log('✅ Chat Widget initialized');
})();