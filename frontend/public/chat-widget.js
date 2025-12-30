// frontend/public/chat-widget.js - גרסה מעודכנת עם תיקון זיהוי קול

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
      voiceEnabled: true,
      defaultLanguage: 'he-IL',
      supportedLanguages: [
        { code: 'he-IL', name: 'עברית', flag: '🇮🇱' },
        { code: 'en-US', name: 'English', flag: '🇺🇸' }
      ]
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

      /* Voice Indicator */
      .voice-indicator {
        font-size: 12px;
        opacity: 0.8;
        margin-top: 4px;
        direction: rtl;
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

      /* Language Selector */
      .language-selector {
        display: flex;
        gap: 8px;
        padding: 8px 16px;
        background: #f8f9ff;
        border-bottom: 1px solid #e1e8ed;
        justify-content: center;
      }

      .lang-btn {
        padding: 6px 12px;
        border: 1px solid #e1e8ed;
        border-radius: 20px;
        background: white;
        cursor: pointer;
        font-size: 12px;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .lang-btn:hover {
        background: #f0f0f0;
      }

      .lang-btn.active {
        background: ${config.primaryColor};
        color: white;
        border-color: ${config.primaryColor};
      }

      .lang-btn:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      /* Recording Bar */
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

      /* Voice Button */
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

      /* Send Button */
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

      /* Voice Preview */
      .voice-preview {
        display: none;
        align-items: center;
        gap: 10px;
        padding: 12px;
        background: #f0f0f0;
        border-radius: 8px;
        margin-bottom: 10px;
      }

      .voice-preview.show {
        display: flex;
      }

      .voice-preview-icon {
        font-size: 20px;
      }

      .voice-preview-info {
        flex: 1;
      }

      .voice-preview-text {
        font-size: 14px;
        color: #333;
        direction: rtl;
      }

      .voice-preview-duration {
        font-size: 11px;
        color: #666;
        margin-top: 2px;
      }

      .voice-preview-delete {
        background: #dc3545;
        color: white;
        border: none;
        padding: 6px 12px;
        border-radius: 6px;
        cursor: pointer;
        font-size: 12px;
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

      /* Browser Warning */
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

      /* Auto Language Detection Info */
      .auto-lang-info {
        background: #e3f2fd;
        color: #1976d2;
        padding: 6px 12px;
        border-radius: 6px;
        font-size: 11px;
        text-align: center;
        margin-bottom: 8px;
        display: none;
      }

      .auto-lang-info.show {
        display: block;
      }
    `;

    const styleSheet = document.createElement('style');
    styleSheet.textContent = styles;
    document.head.appendChild(styleSheet);
  }

  // ==================== HTML Creation ====================
  function createWidgetHTML(config) {
    const languageButtons = config.supportedLanguages.map((lang, index) => `
      <button 
        class="lang-btn ${index === 0 ? 'active' : ''}" 
        id="lang-btn-${lang.code}"
        data-lang="${lang.code}"
      >
        <span>${lang.flag}</span>
        <span>${lang.name}</span>
      </button>
    `).join('');

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
          ${config.voiceEnabled ? `
          <div class="language-selector" id="language-selector">
            ${languageButtons}
          </div>
          ` : ''}
          <div class="limit-warning" id="limit-warning">
            ⚠️ הגעת למגבלת 10 הודעות. לחץ על "התחל שיחה חדשה".
          </div>
          <div class="browser-warning" id="browser-warning">
            ⚠️ הדפדפן שלך לא תומך בהקלטת קול
          </div>
          <div class="auto-lang-info" id="auto-lang-info">
            🌍 זיהוי שפה אוטומטי מופעל
          </div>
          <div class="recording-bar" id="recording-bar">
            <div class="recording-timer">
              <div class="recording-dot"></div>
              <span id="recording-lang-display">מקליט</span>
              <span id="recording-timer">0:00</span>
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
            <div class="voice-preview" id="voice-preview">
              <div class="voice-preview-icon">🎤</div>
              <div class="voice-preview-info">
                <div class="voice-preview-text" id="voice-preview-text"></div>
                <div class="voice-preview-duration" id="voice-preview-duration"></div>
              </div>
              <button class="voice-preview-delete" id="voice-preview-delete">🗑️</button>
            </div>
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

  // ==================== Event Listeners Setup ====================
  function setupEventListeners(config) {
    const state = {
      messages: [],
      history: [],
      isOpen: false,
      isLoading: false,
      isRecording: false,
      recordingStartTime: null,
      recordingTimer: null,
      voiceTranscript: null,
      voiceDuration: null,
      recognition: null,
      currentLanguage: config.defaultLanguage,
      sessionId: generateSessionId(),
      maxHistoryMessages: config.maxHistoryMessages,
      autoLanguageDetection: true,
      currentTranscript: '' // ✅ משתנה לשמירת טקסט נוכחי
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
      recordingLangDisplay: document.getElementById('recording-lang-display'),
      cancelRecordingBtn: document.getElementById('cancel-recording-btn'),
      inputContainer: document.getElementById('input-container'),
      voicePreview: document.getElementById('voice-preview'),
      voicePreviewText: document.getElementById('voice-preview-text'),
      voicePreviewDuration: document.getElementById('voice-preview-duration'),
      voicePreviewDelete: document.getElementById('voice-preview-delete'),
      languageSelector: document.getElementById('language-selector'),
      autoLangInfo: document.getElementById('auto-lang-info')
    };

    if (config.voiceEnabled) {
      setupVoiceRecognition(state, elements, config);
      setupLanguageButtons(state, elements, config);
    }

    loadHistoryFromSession(state, elements, config);

    elements.toggleButton.addEventListener('click', () => toggleWidget(state, elements));
    elements.resetButton.addEventListener('click', () => resetChat(state, elements, config));
    elements.sendButton.addEventListener('click', () => sendMessage(state, elements, config));
    
    if (elements.voiceButton) {
      elements.voiceButton.addEventListener('click', () => toggleVoiceRecording(state, elements, config));
    }

    if (elements.cancelRecordingBtn) {
      elements.cancelRecordingBtn.addEventListener('click', () => cancelRecording(state, elements));
    }

    if (elements.voicePreviewDelete) {
      elements.voicePreviewDelete.addEventListener('click', () => deleteVoiceRecording(state, elements));
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

  // ==================== 🌍 Language Buttons Setup ====================
  function setupLanguageButtons(state, elements, config) {
    config.supportedLanguages.forEach(lang => {
      const btn = document.getElementById(`lang-btn-${lang.code}`);
      if (btn) {
        btn.addEventListener('click', () => {
          state.currentLanguage = lang.code;
          state.autoLanguageDetection = false;
          
          if (state.recognition) {
            state.recognition.lang = lang.code;
            console.log('🌍 Language manually set to:', lang.code);
          }
          
          config.supportedLanguages.forEach(l => {
            const b = document.getElementById(`lang-btn-${l.code}`);
            if (b) {
              b.classList.toggle('active', l.code === lang.code);
            }
          });
          
          if (elements.autoLangInfo) {
            elements.autoLangInfo.classList.remove('show');
          }
        });
      }
    });
  }

  // ==================== 🎤 Voice Recognition Setup ====================
  
  function setupVoiceRecognition(state, elements, config) {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    
    if (!SpeechRecognition) {
      console.warn('Speech Recognition not supported in this browser');
      if (elements.voiceButton) {
        elements.voiceButton.style.display = 'none';
      }
      if (elements.languageSelector) {
        elements.languageSelector.style.display = 'none';
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
      console.log('🎤 Voice recording started with language:', recognition.lang);
      state.isRecording = true;
      state.recordingStartTime = Date.now();
      finalTranscript = '';
      interimTranscript = '';
      detectedLanguageSwitch = false;
      
      elements.voiceButton.classList.add('recording');
      elements.recordingBar.classList.add('active');
      elements.inputContainer.classList.add('recording');
      
      const currentLang = config.supportedLanguages.find(l => l.code === recognition.lang);
      const langName = currentLang ? currentLang.name : 'Unknown';
      elements.inputField.placeholder = `מקשיב (${langName})...`;
      elements.recordingLangDisplay.textContent = `מקליט (${langName})`;
      
      elements.inputField.disabled = true;
      elements.sendButton.disabled = true;
      
      config.supportedLanguages.forEach(lang => {
        const btn = document.getElementById(`lang-btn-${lang.code}`);
        if (btn) btn.disabled = true;
      });
      
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
      
      // ✅ שמור את הטקסט הסופי ב-state
      state.currentTranscript = finalTranscript.trim();
      
      console.log('🎤 Final:', finalTranscript);
      console.log('🎤 Interim:', interimTranscript);
      console.log('🎤 Saved in state:', state.currentTranscript);
      
      if (state.autoLanguageDetection && !detectedLanguageSwitch && finalTranscript.trim().length > 0) {
        const detectedLang = detectLanguageFromText(finalTranscript);
        const currentRecognitionLang = recognition.lang;
        
        if (detectedLang && detectedLang !== currentRecognitionLang) {
          console.log(`🔄 Auto-switching language from ${currentRecognitionLang} to ${detectedLang}`);
          detectedLanguageSwitch = true;
          
          recognition.stop();
          
          state.currentLanguage = detectedLang;
          recognition.lang = detectedLang;
          
          config.supportedLanguages.forEach(l => {
            const b = document.getElementById(`lang-btn-${l.code}`);
            if (b) {
              b.classList.toggle('active', l.code === detectedLang);
            }
          });
          
          if (elements.autoLangInfo) {
            elements.autoLangInfo.textContent = `🌍 עברתי אוטומטית ל-${config.supportedLanguages.find(l => l.code === detectedLang)?.name}`;
            elements.autoLangInfo.classList.add('show');
            setTimeout(() => {
              elements.autoLangInfo.classList.remove('show');
            }, 3000);
          }
          
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
      console.error('🎤 Speech recognition error:', event.error);
      
      let errorMessage = 'שגיאה בהקלטת קול';
      switch(event.error) {
        case 'no-speech':
          errorMessage = 'לא זוהה דיבור - נסה לדבר קרוב יותר למיקרופון';
          break;
        case 'audio-capture':
          errorMessage = 'לא ניתן לגשת למיקרופון - בדוק שהוא מחובר ושיש לך הרשאות';
          break;
        case 'not-allowed':
          errorMessage = 'נדרשת הרשאה למיקרופון - אפשר גישה בהגדרות הדפדפן';
          break;
        case 'network':
          errorMessage = 'נדרש חיבור אינטרנט לזיהוי קול';
          break;
        case 'aborted':
          if (!detectedLanguageSwitch) {
            errorMessage = 'ההקלטה בוטלה';
          } else {
            return;
          }
          break;
      }
      
      if (event.error !== 'aborted' || !detectedLanguageSwitch) {
        alert(errorMessage);
        stopRecording(state, elements, recognition, false);
      }
    };

    recognition.onend = () => {
      console.log('🎤 Voice recording ended');
      console.log('🎤 Final transcript at end:', finalTranscript);
      
      // ✅ שמור את הטקסט הסופי ב-state לפני שנעצור
      if (finalTranscript.trim().length > 0) {
        state.currentTranscript = finalTranscript.trim();
      }
      
      if (state.isRecording && !detectedLanguageSwitch) {
        // ✅ תן זמן קטן ל-onresult לסיים
        setTimeout(() => {
          stopRecording(state, elements, recognition, true);
        }, 300);
      }
    };

    state.recognition = recognition;
  }

  // ==================== 🌍 Language Detection from Text ====================
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

  function toggleVoiceRecording(state, elements, config) {
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
        console.error('Failed to start recording:', error);
        alert('שגיאה בהפעלת ההקלטה');
      }
    }
  }

  function stopRecording(state, elements, recognition, saveTranscript) {
    console.log('🎤 Stopping recording, saveTranscript:', saveTranscript);
    
    state.isRecording = false;
    
    if (state.recordingTimer) {
      clearInterval(state.recordingTimer);
      state.recordingTimer = null;
    }
    
    const duration = Math.floor((Date.now() - state.recordingStartTime) / 1000);
    state.voiceDuration = duration;
    
    elements.voiceButton.classList.remove('recording');
    elements.recordingBar.classList.remove('active');
    elements.inputContainer.classList.remove('recording');
    elements.recordingTimer.textContent = '0:00';
    elements.inputField.placeholder = 'הקלד הודעה...';
    
    if (elements.languageSelector) {
      const langButtons = elements.languageSelector.querySelectorAll('.lang-btn');
      langButtons.forEach(btn => btn.disabled = false);
    }
    
    // ✅ נסה קודם את הטקסט מ-state, אחר כך מה-input
    let fullTranscript = state.currentTranscript || elements.inputField.value.trim();
    
    console.log('📝 Transcript from state:', state.currentTranscript);
    console.log('📝 Transcript from input:', elements.inputField.value.trim());
    console.log('📝 Final transcript:', fullTranscript);
    
    if (saveTranscript && fullTranscript.length > 0) {
      state.voiceTranscript = fullTranscript;
      
      elements.voicePreviewText.textContent = fullTranscript;
      const minutes = Math.floor(duration / 60);
      const seconds = duration % 60;
      elements.voicePreviewDuration.textContent = `🎤 ${minutes}:${seconds.toString().padStart(2, '0')}`;
      elements.voicePreview.classList.add('show');
      
      elements.inputField.value = '';
      elements.inputField.style.height = 'auto';
      elements.inputField.disabled = true;
      
      elements.sendButton.disabled = false;
      
      // ✅ נקה את currentTranscript
      state.currentTranscript = '';
      
      console.log('✅ Voice preview shown with text:', fullTranscript);
    } else {
      elements.inputField.value = '';
      elements.inputField.style.height = 'auto';
      elements.inputField.disabled = false;
      elements.sendButton.disabled = false;
      
      // ✅ נקה את currentTranscript
      state.currentTranscript = '';
      
      if (saveTranscript && fullTranscript.length === 0) {
        alert('לא זוהה טקסט בהקלטה');
      }
    }
  }

  function cancelRecording(state, elements) {
    if (state.recognition && state.isRecording) {
      elements.inputField.value = '';
      state.currentTranscript = '';
      state.recognition.stop();
    }
  }

  function deleteVoiceRecording(state, elements) {
    state.voiceTranscript = null;
    state.voiceDuration = null;
    elements.voicePreview.classList.remove('show');
    elements.inputField.disabled = false;
    elements.sendButton.disabled = false;
    elements.inputField.focus();
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
      if (!state.voiceTranscript) {
        elements.inputField.disabled = false;
      }
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
      state.voiceTranscript = null;
      state.voiceDuration = null;
      state.currentTranscript = '';
      
      const storageKey = 'chatHistory_' + config.secretKey;
      sessionStorage.removeItem(storageKey);
      
      elements.voicePreview.classList.remove('show');
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

  function formatDuration(seconds) {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
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
      
      const voiceIcon = msg.isVoice ? '<div class="voice-indicator">🎤 הודעת קול</div>' : '';
      
      return `
        <div class="chat-message ${msg.role}">
          <div class="chat-message-avatar">${createAvatar(msg.role, config)}</div>
          <div class="chat-message-content">
            ${voiceIcon}
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
    let question = '';
    let isVoice = false;
    let duration = 0;
    
    if (state.voiceTranscript) {
      question = state.voiceTranscript;
      isVoice = true;
      duration = state.voiceDuration || 0;
      
      state.voiceTranscript = null;
      state.voiceDuration = null;
      elements.voicePreview.classList.remove('show');
      elements.inputField.disabled = false;
    } else {
      question = elements.inputField.value.trim();
    }
    
    if (!question || state.isLoading || isAtLimit(state)) return;

    state.messages.push({
      role: 'user',
      content: question,
      timestamp: new Date().toISOString(),
      isVoice: isVoice,
      duration: duration
    });

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
        state.messages.push({
          role: 'assistant',
          content: data.data.answer,
          timestamp: new Date().toISOString(),
          isVoice: false
        });

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

  console.log('✅ Chat Widget with Multi-Language Voice Recognition initialized successfully');
})();