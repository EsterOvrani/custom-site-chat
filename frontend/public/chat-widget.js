// frontend/public/chat-widget.js - גרסה מתוקנת מלאה

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
      primaryColor: '#1a73e8',
      secondaryColor: '#5f6368',
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

  // ==================== CSS ====================
  function injectStyles(config) {
    const styles = `
      .chat-widget-container {
        position: fixed;
        ${config.position.includes('right') ? 'right: 20px;' : 'left: 20px;'}
        bottom: 20px;
        z-index: 9999;
        font-family: 'Google Sans', 'Roboto', Arial, sans-serif;
      }

      .chat-widget-button {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: ${config.primaryColor};
        border: none;
        box-shadow: 0 2px 8px rgba(0,0,0,0.2);
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
        font-size: 28px;
      }

      .chat-widget-button:hover {
        box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        transform: scale(1.05);
      }

      .chat-widget-window {
        position: absolute;
        bottom: 80px;
        ${config.position.includes('right') ? 'right: 0;' : 'left: 0;'}
        width: 380px;
        height: 600px;
        background: white;
        border-radius: 12px;
        box-shadow: 0 4px 16px rgba(0,0,0,0.15);
        display: none;
        flex-direction: column;
        overflow: hidden;
      }

      .chat-widget-window.open {
        display: flex;
        animation: slideUp 0.2s ease-out;
      }

      @keyframes slideUp {
        from { opacity: 0; transform: translateY(10px); }
        to { opacity: 1; transform: translateY(0); }
      }

      .chat-widget-header {
        background: white;
        color: #202124;
        padding: 16px 20px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #e8eaed;
      }

      .chat-widget-header h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
      }

      .message-counter {
        font-size: 12px;
        color: #5f6368;
        margin-top: 2px;
      }

      .header-actions {
        display: flex;
        gap: 8px;
      }

      .header-button {
        background: transparent;
        border: none;
        color: #5f6368;
        cursor: pointer;
        padding: 8px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.2s;
        font-size: 18px;
      }

      .header-button:hover {
        background: #f1f3f4;
      }

      .reset-button {
        background: transparent;
        border: 1px solid #dadce0;
        color: #5f6368;
        padding: 6px 12px;
        border-radius: 4px;
        cursor: pointer;
        font-size: 12px;
        transition: all 0.2s;
        display: none;
      }

      .reset-button:hover {
        background: #f8f9fa;
        border-color: #5f6368;
      }

      .reset-button.show {
        display: block;
      }

      .chat-widget-messages {
        flex: 1;
        overflow-y: auto;
        padding: 20px;
        background: #ffffff;
      }

      .chat-message {
        margin-bottom: 16px;
        display: flex;
        align-items: flex-start;
        gap: 12px;
      }

      .chat-message.user {
        flex-direction: row-reverse;
      }

      .chat-message.assistant {
        flex-direction: row;
      }

      .chat-message-avatar {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: #e8eaed;
        color: #5f6368;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 500;
        font-size: 13px;
        flex-shrink: 0;
        overflow: hidden;
      }

      .chat-message.user .chat-message-avatar {
        background: ${config.primaryColor};
        color: white;
      }

      .chat-message-avatar img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .chat-message-content {
        display: flex;
        flex-direction: column;
        max-width: 75%;
      }

      .chat-message-bubble {
        padding: 10px 14px;
        border-radius: 18px;
        line-height: 1.4;
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
        border-bottom-right-radius: 4px;
      }

      .chat-message.assistant .chat-message-bubble {
        background: #f1f3f4;
        color: #202124;
        border-bottom-left-radius: 4px;
      }

      /* 🎤 פלייר אודיו */
      .voice-audio-player {
        background: ${config.primaryColor};
        border-radius: 20px;
        padding: 8px 16px;
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 200px;
        color: white;
        cursor: pointer;
      }

      .voice-play-button {
        background: rgba(255,255,255,0.2);
        border: none;
        width: 32px;
        height: 32px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: background 0.2s;
        font-size: 14px;
        color: white;
      }

      .voice-play-button:hover {
        background: rgba(255,255,255,0.3);
      }

      .voice-waveform {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 3px;
        height: 32px;
      }

      .voice-waveform-bar {
        width: 3px;
        background: rgba(255,255,255,0.8);
        border-radius: 2px;
        transition: height 0.1s;
      }

      .voice-waveform-bar:nth-child(1) { height: 12px; }
      .voice-waveform-bar:nth-child(2) { height: 20px; }
      .voice-waveform-bar:nth-child(3) { height: 16px; }
      .voice-waveform-bar:nth-child(4) { height: 24px; }
      .voice-waveform-bar:nth-child(5) { height: 14px; }
      .voice-waveform-bar:nth-child(6) { height: 18px; }
      .voice-waveform-bar:nth-child(7) { height: 22px; }
      .voice-waveform-bar:nth-child(8) { height: 16px; }
      .voice-waveform-bar:nth-child(9) { height: 20px; }
      .voice-waveform-bar:nth-child(10) { height: 12px; }

      .voice-waveform.playing .voice-waveform-bar {
        animation: waveAnimation 1s ease-in-out infinite;
      }

      @keyframes waveAnimation {
        0%, 100% { height: 12px; }
        50% { height: 24px; }
      }

      .voice-waveform-bar:nth-child(2) { animation-delay: 0.1s; }
      .voice-waveform-bar:nth-child(3) { animation-delay: 0.2s; }
      .voice-waveform-bar:nth-child(4) { animation-delay: 0.3s; }
      .voice-waveform-bar:nth-child(5) { animation-delay: 0.4s; }
      .voice-waveform-bar:nth-child(6) { animation-delay: 0.5s; }
      .voice-waveform-bar:nth-child(7) { animation-delay: 0.6s; }
      .voice-waveform-bar:nth-child(8) { animation-delay: 0.7s; }
      .voice-waveform-bar:nth-child(9) { animation-delay: 0.8s; }
      .voice-waveform-bar:nth-child(10) { animation-delay: 0.9s; }

      .voice-duration {
        font-size: 12px;
        color: rgba(255,255,255,0.9);
        min-width: 35px;
        text-align: right;
      }

      /* 🎤 תצוגת הקלטה בזמן אמת */
      .recording-preview {
        background: #fce8e6;
        border: 1px solid #f4b4af;
        border-radius: 12px;
        padding: 12px 16px;
        margin: 8px 0;
        display: none;
        align-items: center;
        gap: 12px;
      }

      .recording-preview.show {
        display: flex;
      }

      .recording-preview-wave {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 3px;
        height: 24px;
      }

      .recording-preview-bar {
        width: 3px;
        background: #d93025;
        border-radius: 2px;
        animation: recordingWave 1s ease-in-out infinite;
      }

      @keyframes recordingWave {
        0%, 100% { height: 8px; }
        50% { height: 20px; }
      }

      .recording-preview-bar:nth-child(1) { height: 10px; }
      .recording-preview-bar:nth-child(2) { height: 16px; }
      .recording-preview-bar:nth-child(3) { height: 12px; }
      .recording-preview-bar:nth-child(4) { height: 18px; }
      .recording-preview-bar:nth-child(5) { height: 14px; }
      .recording-preview-bar:nth-child(6) { height: 16px; }
      .recording-preview-bar:nth-child(7) { height: 12px; }
      .recording-preview-bar:nth-child(8) { height: 14px; }

      .recording-preview-bar:nth-child(2) { animation-delay: 0.1s; }
      .recording-preview-bar:nth-child(3) { animation-delay: 0.2s; }
      .recording-preview-bar:nth-child(4) { animation-delay: 0.3s; }
      .recording-preview-bar:nth-child(5) { animation-delay: 0.4s; }
      .recording-preview-bar:nth-child(6) { animation-delay: 0.5s; }
      .recording-preview-bar:nth-child(7) { animation-delay: 0.6s; }
      .recording-preview-bar:nth-child(8) { animation-delay: 0.7s; }

      .recording-preview-time {
        color: #d93025;
        font-weight: 500;
        font-size: 13px;
        min-width: 40px;
      }

      .recording-preview-text {
        flex: 1;
        color: #5f6368;
        font-size: 13px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .limit-warning {
        background: #fef7e0;
        color: #b06000;
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

      .chat-widget-input-area {
        padding: 12px 16px;
        border-top: 1px solid #e8eaed;
        background: white;
      }

      /* 🔧 שדה קלט + כפתורים */
      .chat-widget-input-wrapper {
        display: flex;
        align-items: flex-end;
        gap: 8px;
      }

      .input-field-container {
        flex: 1;
        position: relative;
        background: #f1f3f4;
        border-radius: 24px;
        padding: 4px 16px;
        display: flex;
        align-items: center;
        gap: 8px;
        transition: background 0.2s;
      }

      .input-field-container:focus-within {
        background: #e8eaed;
      }

      .input-field-container.recording {
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
        color: #202124;
      }

      .chat-widget-input::placeholder {
        color: #5f6368;
      }

      .chat-widget-input:disabled {
        cursor: not-allowed;
        opacity: 0.6;
      }

      /* 🎤 כפתור מיקרופון - בתוך השדה */
      .input-icon-button {
        background: transparent;
        border: none;
        cursor: pointer;
        padding: 6px;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
        border-radius: 50%;
        min-width: 32px;
        height: 32px;
      }

      .input-icon-button:hover:not(:disabled) {
        background: rgba(0,0,0,0.08);
      }

      .input-icon-button:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .input-icon-button.recording {
        animation: pulse 1.5s infinite;
      }

      @keyframes pulse {
        0%, 100% { opacity: 1; }
        50% { opacity: 0.6; }
      }

      /* SVG Icons */
      .mic-icon {
        width: 20px;
        height: 20px;
        fill: #5f6368;
      }

      .input-icon-button.recording .mic-icon {
        fill: #d93025;
      }

      /* ➤ כפתור שליחה - בנפרד */
      .chat-widget-send {
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

      .chat-widget-send:hover:not(:disabled) {
        background: #1557b0;
        box-shadow: 0 2px 4px rgba(0,0,0,0.2);
      }

      .chat-widget-send:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .send-icon {
        width: 20px;
        height: 20px;
        fill: white;
        transform: rotate(180deg); /* ✅ הפיכת החץ לשמאל */
      }

      .recording-status {
        background: #fce8e6;
        color: #d93025;
        padding: 8px 16px;
        display: none;
        align-items: center;
        justify-content: space-between;
        font-size: 13px;
        border-top: 1px solid #f4b4af;
      }

      .recording-status.active {
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
        0%, 100% { opacity: 1; }
        50% { opacity: 0.3; }
      }

      .cancel-recording {
        background: transparent;
        border: none;
        color: #d93025;
        padding: 4px 8px;
        cursor: pointer;
        font-size: 12px;
        font-weight: 500;
        border-radius: 4px;
        transition: background 0.2s;
      }

      .cancel-recording:hover {
        background: rgba(217,48,37,0.1);
      }

      .typing-indicator {
        display: flex;
        gap: 4px;
        padding: 8px 12px;
      }

      .typing-dot {
        width: 8px;
        height: 8px;
        background: #5f6368;
        border-radius: 50%;
        animation: typing 1.4s infinite;
      }

      .typing-dot:nth-child(2) { animation-delay: 0.2s; }
      .typing-dot:nth-child(3) { animation-delay: 0.4s; }

      @keyframes typing {
        0%, 60%, 100% { transform: translateY(0); }
        30% { transform: translateY(-8px); }
      }

      .chat-widget-empty {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        color: #5f6368;
        text-align: center;
        padding: 20px;
      }

      .chat-widget-empty-icon {
        font-size: 48px;
        margin-bottom: 16px;
        opacity: 0.5;
      }

      .chat-widget-empty h3 {
        color: #202124;
        font-size: 16px;
        font-weight: 500;
        margin: 0 0 8px 0;
      }

      .chat-widget-empty p {
        margin: 4px 0;
        font-size: 14px;
      }

      .browser-warning {
        background: #fef7e0;
        color: #b06000;
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

      .options-menu {
        position: absolute;
        top: 100%;
        right: 0;
        background: white;
        border-radius: 8px;
        box-shadow: 0 2px 12px rgba(0,0,0,0.15);
        padding: 8px 0;
        margin-top: 4px;
        display: none;
        min-width: 180px;
        z-index: 10000;
      }

      .options-menu.show {
        display: block;
      }

      .options-menu-item {
        padding: 10px 16px;
        cursor: pointer;
        font-size: 14px;
        color: #202124;
        transition: background 0.2s;
      }

      .options-menu-item:hover {
        background: #f1f3f4;
      }
    `;

    const styleSheet = document.createElement('style');
    styleSheet.textContent = styles;
    document.head.appendChild(styleSheet);
  }

  // ==================== HTML ====================
  function createWidgetHTML(config) {
    const widgetHTML = `
      <div class="chat-widget-container">
        <button class="chat-widget-button" id="chat-widget-toggle">💬</button>
        <div class="chat-widget-window" id="chat-widget-window">
          <div class="chat-widget-header">
            <div>
              <h3>${escapeHtml(config.title)}</h3>
              <div class="message-counter" id="message-counter">0/10 הודעות</div>
            </div>
            <div class="header-actions">
              <button class="reset-button" id="reset-button">🔄 איפוס</button>
              <button class="header-button" id="options-button" title="אפשרויות">⋮</button>
            </div>
          </div>
          <div class="options-menu" id="options-menu">
            <div class="options-menu-item" id="menu-reset">🔄 התחל שיחה חדשה</div>
            <div class="options-menu-item" id="menu-about">ℹ️ אודות</div>
          </div>
          <div class="limit-warning" id="limit-warning">
            ⚠️ הגעת למגבלת 10 הודעות. לחץ על "איפוס" להתחלה חדשה.
          </div>
          <div class="browser-warning" id="browser-warning">
            ⚠️ הדפדפן שלך לא תומך בהקלטת קול
          </div>
          <div class="recording-status" id="recording-status">
            <div class="recording-timer">
              <div class="recording-dot"></div>
              <span id="recording-time">0:00</span>
            </div>
            <button class="cancel-recording" id="cancel-recording">ביטול</button>
          </div>
          <div class="chat-widget-messages" id="chat-widget-messages">
            <div class="chat-widget-empty">
              <div class="chat-widget-empty-icon">💬</div>
              <h3>שלום!</h3>
              <p>שאל שאלה על המסמכים שלך</p>
              ${config.voiceEnabled ? '<p style="font-size: 12px; margin-top: 8px;">💡 לחץ על המיקרופון להקלטה</p>' : ''}
            </div>
          </div>
          <div class="chat-widget-input-area">
            <!-- 🎤 תצוגת הקלטה -->
            <div class="recording-preview" id="recording-preview">
              <div class="recording-preview-wave">
                <div class="recording-preview-bar"></div>
                <div class="recording-preview-bar"></div>
                <div class="recording-preview-bar"></div>
                <div class="recording-preview-bar"></div>
                <div class="recording-preview-bar"></div>
                <div class="recording-preview-bar"></div>
                <div class="recording-preview-bar"></div>
                <div class="recording-preview-bar"></div>
              </div>
              <div class="recording-preview-time" id="preview-time">0:00</div>
              <div class="recording-preview-text" id="preview-text">מקליט...</div>
            </div>

            <div class="chat-widget-input-wrapper">
              <div class="input-field-container" id="input-field-container">
                <textarea 
                  class="chat-widget-input" 
                  id="chat-widget-input"
                  placeholder="הקלד הודעה..."
                  rows="1"
                ></textarea>
                ${config.voiceEnabled ? `
                <button class="input-icon-button" id="chat-widget-voice" title="הקלט קול">
                  <svg class="mic-icon" viewBox="0 0 24 24">
                    <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
                    <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
                  </svg>
                </button>
                ` : ''}
              </div>
              <button class="chat-widget-send" id="chat-widget-send" title="שלח">
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

  // ==================== Event Listeners ====================
  function setupEventListeners(config) {
    const state = {
      messages: [],
      history: [],
      isOpen: false,
      isLoading: false,
      isRecording: false,
      recordingStartTime: null,
      recordingTimer: null,
      recordedText: '',
      recordedAudioBlob: null,
      mediaRecorder: null,
      audioChunks: [],
      recognition: null,
      sessionId: generateSessionId(),
      maxHistoryMessages: config.maxHistoryMessages
    };

    const elements = {
      toggleButton: document.getElementById('chat-widget-toggle'),
      resetButton: document.getElementById('reset-button'),
      optionsButton: document.getElementById('options-button'),
      optionsMenu: document.getElementById('options-menu'),
      menuReset: document.getElementById('menu-reset'),
      menuAbout: document.getElementById('menu-about'),
      widgetWindow: document.getElementById('chat-widget-window'),
      messagesContainer: document.getElementById('chat-widget-messages'),
      inputField: document.getElementById('chat-widget-input'),
      inputFieldContainer: document.getElementById('input-field-container'),
      sendButton: document.getElementById('chat-widget-send'),
      voiceButton: document.getElementById('chat-widget-voice'),
      messageCounter: document.getElementById('message-counter'),
      limitWarning: document.getElementById('limit-warning'),
      browserWarning: document.getElementById('browser-warning'),
      recordingStatus: document.getElementById('recording-status'),
      recordingTime: document.getElementById('recording-time'),
      cancelRecording: document.getElementById('cancel-recording'),
      recordingPreview: document.getElementById('recording-preview'),
      previewTime: document.getElementById('preview-time'),
      previewText: document.getElementById('preview-text')
    };

    if (config.voiceEnabled) {
      setupVoiceRecognition(state, elements, config);
    }

    loadHistoryFromSession(state, elements, config);

    elements.toggleButton.addEventListener('click', () => toggleWidget(state, elements));
    elements.resetButton.addEventListener('click', () => resetChat(state, elements, config));
    elements.sendButton.addEventListener('click', () => sendMessage(state, elements, config));
    
    if (elements.voiceButton) {
      elements.voiceButton.addEventListener('click', () => toggleVoiceRecording(state, elements, config));
    }

    if (elements.cancelRecording) {
      elements.cancelRecording.addEventListener('click', () => cancelRecording(state, elements));
    }

    elements.optionsButton.addEventListener('click', (e) => {
      e.stopPropagation();
      elements.optionsMenu.classList.toggle('show');
    });

    elements.menuReset.addEventListener('click', () => {
      elements.optionsMenu.classList.remove('show');
      resetChat(state, elements, config);
    });

    elements.menuAbout.addEventListener('click', () => {
      elements.optionsMenu.classList.remove('show');
      alert('Chat Widget v2.0\nעם תמיכה בהקלטת קול');
    });

    document.addEventListener('click', (e) => {
      if (!elements.optionsButton.contains(e.target) && !elements.optionsMenu.contains(e.target)) {
        elements.optionsMenu.classList.remove('show');
      }
    });
    
    elements.inputField.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage(state, elements, config);
      }
    });

    elements.inputField.addEventListener('input', () => {
      elements.inputField.style.height = 'auto';
      elements.inputField.style.height = Math.min(elements.inputField.scrollHeight, 100) + 'px';
    });
  }

  // ==================== Voice Recognition ====================
  
  function setupVoiceRecognition(state, elements, config) {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    
    if (!SpeechRecognition) {
      console.warn('Speech Recognition not supported');
      if (elements.voiceButton) {
        elements.voiceButton.style.display = 'none';
      }
      elements.browserWarning.classList.add('show');
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = 'he-IL'; // ✅ עברית
    recognition.maxAlternatives = 3; // ✅ יותר אלטרנטיבות לזיהוי טוב יותר

    // Setup audio recording
    setupMediaRecorder(state, elements);

    recognition.onstart = () => {
      console.log('🎤 Recording started');
      state.isRecording = true;
      state.recordingStartTime = Date.now();
      state.recordedText = '';
      
      elements.voiceButton.classList.add('recording');
      elements.recordingStatus.classList.add('active');
      elements.recordingPreview.classList.add('show'); // ✅ הצג תצוגת הקלטה
      elements.inputFieldContainer.classList.add('recording');
      elements.inputField.placeholder = 'מקשיב...';
      elements.inputField.disabled = true;
      elements.sendButton.disabled = true;
      
      state.recordingTimer = setInterval(() => {
        const elapsed = Math.floor((Date.now() - state.recordingStartTime) / 1000);
        const minutes = Math.floor(elapsed / 60);
        const seconds = elapsed % 60;
        const timeStr = `${minutes}:${seconds.toString().padStart(2, '0')}`;
        elements.recordingTime.textContent = timeStr;
        elements.previewTime.textContent = timeStr; // ✅ עדכן גם בתצוגה
      }, 1000);

      if (state.mediaRecorder && state.mediaRecorder.state === 'inactive') {
        state.audioChunks = [];
        state.mediaRecorder.start();
      }
    };

    recognition.onresult = (event) => {
      let interimTranscript = '';
      let finalTranscript = '';

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        console.log('🎤 Transcript:', transcript, 'Confidence:', event.results[i][0].confidence);
        
        if (event.results[i].isFinal) {
          finalTranscript += transcript + ' ';
        } else {
          interimTranscript += transcript;
        }
      }

      if (finalTranscript) {
        state.recordedText += finalTranscript;
      }

      const displayText = state.recordedText + interimTranscript;
      elements.previewText.textContent = displayText || 'מקליט...'; // ✅ הצג טקסט בתצוגה
    };

    recognition.onerror = (event) => {
      console.error('🎤 Error:', event.error);
      let errorMessage = 'שגיאה בהקלטה';
      switch(event.error) {
        case 'no-speech':
          errorMessage = 'לא זוהה דיבור - נסה שוב';
          break;
        case 'audio-capture':
          errorMessage = 'לא ניתן לגשת למיקרופון';
          break;
        case 'not-allowed':
          errorMessage = 'נדרשת הרשאה למיקרופון';
          break;
        case 'network':
          errorMessage = 'שגיאת חיבור - בדוק אינטרנט';
          break;
        case 'aborted':
          // אל תציג שגיאה אם המשתמש עצר
          return;
      }
      alert(errorMessage);
      cancelRecording(state, elements);
    };

    recognition.onend = () => {
      console.log('🎤 Recording ended');
      
      if (state.recordingTimer) {
        clearInterval(state.recordingTimer);
        state.recordingTimer = null;
      }

      if (state.mediaRecorder && state.mediaRecorder.state === 'recording') {
        state.mediaRecorder.stop();
      }
      
      state.isRecording = false;
      elements.voiceButton.classList.remove('recording');
      elements.recordingStatus.classList.remove('active');
      elements.inputFieldContainer.classList.remove('recording');
      elements.inputField.placeholder = 'הקלד הודעה...';
      elements.inputField.disabled = false;
      elements.sendButton.disabled = false;
      
      // ✅ השאר את התצוגה פתוחה עם הטקסט
      if (state.recordedText.trim()) {
        elements.previewText.textContent = state.recordedText.trim();
        console.log('✅ Recorded text:', state.recordedText);
      } else {
        // אם לא הוקלט כלום, הסתר את התצוגה
        elements.recordingPreview.classList.remove('show');
        elements.previewText.textContent = 'מקליט...';
      }
    };

    state.recognition = recognition;
  }

  function setupMediaRecorder(state, elements) {
    navigator.mediaDevices.getUserMedia({ 
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    })
      .then(stream => {
        const mediaRecorder = new MediaRecorder(stream);
        
        mediaRecorder.ondataavailable = (event) => {
          if (event.data.size > 0) {
            state.audioChunks.push(event.data);
          }
        };

        mediaRecorder.onstop = () => {
          const audioBlob = new Blob(state.audioChunks, { type: 'audio/webm' });
          state.recordedAudioBlob = audioBlob;
          console.log('🎤 Audio saved:', audioBlob.size, 'bytes');
        };

        state.mediaRecorder = mediaRecorder;
      })
      .catch(err => {
        console.error('Microphone error:', err);
        alert('לא ניתן לגשת למיקרופון. אנא אפשר גישה בהגדרות הדפדפן.');
      });
  }

  function toggleVoiceRecording(state, elements, config) {
    if (!state.recognition) {
      alert('הדפדפן שלך לא תומך בהקלטת קול');
      return;
    }

    if (isAtLimit(state)) return;

    if (state.isRecording) {
      // ✅ עצור הקלטה
      console.log('🎤 Stopping recording...');
      state.recognition.stop();
    } else {
      // ✅ התחל הקלטה
      try {
        state.recognition.lang = 'he-IL'; // וודא עברית
        state.recognition.start();
        console.log('🎤 Starting recording in Hebrew...');
      } catch (error) {
        console.error('Failed to start:', error);
        alert('שגיאה בהפעלת ההקלטה');
      }
    }
  }

  function cancelRecording(state, elements) {
    if (state.recognition && state.isRecording) {
      state.recordedText = '';
      state.recordedAudioBlob = null;
      state.audioChunks = [];
      elements.recordingPreview.classList.remove('show');
      elements.previewText.textContent = 'מקליט...';
      state.recognition.abort(); // ✅ עצור מיידי
    }
  }

  // ==================== History ====================
  
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
      console.error('Failed to save:', e);
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
      if (!state.isRecording) {
        elements.inputField.disabled = false;
        elements.sendButton.disabled = false;
        if (elements.voiceButton) {
          elements.voiceButton.disabled = false;
        }
      }
    }
  }

  function resetChat(state, elements, config) {
    if (confirm('האם להתחיל שיחה חדשה? ההיסטוריה תימחק.')) {
      state.history = [];
      state.messages = [];
      const storageKey = 'chatHistory_' + config.secretKey;
      sessionStorage.removeItem(storageKey);
      
      renderMessages(state, elements, config);
      updateUI(state, elements);
    }
  }

  // ==================== Utilities ====================
  
  function generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  function detectLanguage(text) {
    if (!text || text.trim().length === 0) return 'he';
    
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
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
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
          ${config.voiceEnabled ? '<p style="font-size: 12px; margin-top: 8px;">💡 לחץ על המיקרופון להקלטה</p>' : ''}
        </div>
      `;
      return;
    }

    const messagesHTML = state.messages.map((msg, index) => {
      const language = detectLanguage(msg.content);
      const textDirection = language === 'he' ? 'rtl' : 'ltr';
      
      const cleanedContent = msg.content
        .split('\n')
        .map(line => line.trim())
        .filter(line => line.length > 0)
        .join('\n')
        .trim();
      
      // ✅ הצג פלייר אודיו רק אם זו הודעת קול
      if (msg.isVoice && msg.audioUrl) {
        const duration = msg.audioDuration || 10;
        const audioId = `audio-${index}`;
        
        return `
          <div class="chat-message ${msg.role}">
            <div class="chat-message-avatar">${createAvatar(msg.role, config)}</div>
            <div class="chat-message-content">
              <div class="voice-audio-player" onclick="window.toggleAudioPlayback('${audioId}', this)">
                <button class="voice-play-button" id="play-btn-${audioId}">▶</button>
                <div class="voice-waveform" id="waveform-${audioId}">
                  <div class="voice-waveform-bar"></div>
                  <div class="voice-waveform-bar"></div>
                  <div class="voice-waveform-bar"></div>
                  <div class="voice-waveform-bar"></div>
                  <div class="voice-waveform-bar"></div>
                  <div class="voice-waveform-bar"></div>
                  <div class="voice-waveform-bar"></div>
                  <div class="voice-waveform-bar"></div>
                  <div class="voice-waveform-bar"></div>
                  <div class="voice-waveform-bar"></div>
                </div>
                <div class="voice-duration">${formatDuration(duration)}</div>
                <audio id="${audioId}" src="${msg.audioUrl}" style="display:none;"></audio>
              </div>
            </div>
          </div>
        `;
      } else {
        // ✅ הודעת טקסט רגילה
        return `
          <div class="chat-message ${msg.role}">
            <div class="chat-message-avatar">${createAvatar(msg.role, config)}</div>
            <div class="chat-message-content">
              <div class="chat-message-bubble ${textDirection}">${escapeHtml(cleanedContent)}</div>
            </div>
          </div>
        `;
      }
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

  window.toggleAudioPlayback = function(audioId, playerDiv) {
    const audio = document.getElementById(audioId);
    const playBtn = document.getElementById('play-btn-' + audioId);
    const waveform = document.getElementById('waveform-' + audioId);
    
    if (audio.paused) {
      audio.play();
      playBtn.textContent = '⏸';
      waveform.classList.add('playing');
      
      audio.onended = () => {
        playBtn.textContent = '▶';
        waveform.classList.remove('playing');
      };
    } else {
      audio.pause();
      playBtn.textContent = '▶';
      waveform.classList.remove('playing');
    }
  };

  async function sendMessage(state, elements, config) {
    // ✅ בדוק אם יש טקסט מוקלט או טקסט שהוקלד
    const typedText = elements.inputField.value.trim();
    const recordedText = state.recordedText.trim();
    const question = recordedText || typedText; // עדיפות להקלטה
    
    if (!question || state.isLoading || isAtLimit(state)) return;

    // ✅ בדוק אם זו הודעת קול או טקסט
    const isVoice = recordedText.length > 0 && state.recordedAudioBlob !== null;
    
    let audioUrl = null;
    let audioDuration = 0;

    if (isVoice) {
      audioUrl = URL.createObjectURL(state.recordedAudioBlob);
      audioDuration = Math.floor((Date.now() - state.recordingStartTime) / 1000);
    }

    state.messages.push({
      role: 'user',
      content: question,
      timestamp: new Date().toISOString(),
      isVoice: isVoice, // ✅ סמן כהודעת קול רק אם באמת הוקלטה
      audioUrl: audioUrl,
      audioDuration: audioDuration
    });

    state.history.push({
      role: 'user',
      content: question
    });

    // ✅ נקה הכל
    state.recordedText = '';
    state.recordedAudioBlob = null;
    state.audioChunks = [];
    elements.inputField.value = '';
    elements.inputField.style.height = 'auto';
    elements.recordingPreview.classList.remove('show');
    elements.previewText.textContent = 'מקליט...';
    
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
          content: 'מצטער, לא הצלחתי למצוא תשובה.',
          timestamp: new Date().toISOString(),
          isVoice: false
        });
      }
    } catch (error) {
      console.error('Error:', error);
      state.messages.push({
        role: 'assistant',
        content: 'אירעה שגיאה. נסה שוב.',
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

  console.log('✅ Google Chat Widget initialized');
})();