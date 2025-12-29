// frontend/public/chat-widget.js - גרסה עובדת ונקייה

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
      console.error('❌ Missing SECRET_KEY');
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
        from { opacity: 0; transform: translateY(20px); }
        to { opacity: 1; transform: translateY(0); }
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
      }

      .chat-message.assistant {
        flex-direction: row;
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

      .voice-indicator {
        font-size: 12px;
        opacity: 0.8;
        margin-top: 4px;
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
        0%, 100% { opacity: 1; }
        50% { opacity: 0.6; }
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

      .recording-bar {
        background: #fce8e6;
        color: #d93025;
        padding: 8px 16px;
        display: none;
        align-items: center;
        justify-content: space-between;
        font-size: 13px;
        border-top: 1px solid #f4b4af;
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
        0%, 100% { opacity: 1; }
        50% { opacity: 0.3; }
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
    const html = `
      <div class="chat-widget-container">
        <button class="chat-widget-button" id="toggle-btn">💬</button>
        <div class="chat-widget-window" id="widget-window">
          <div class="chat-widget-header">
            <button class="reset-button" id="reset-btn">🔄 התחל שיחה חדשה</button>
            <div>
              <h3>${escapeHtml(config.title)}</h3>
              <div class="message-counter" id="msg-counter">0/10 הודעות</div>
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
              <span id="rec-time">0:00</span>
            </div>
            <button class="cancel-btn" id="cancel-btn">ביטול</button>
          </div>
          <div class="chat-widget-messages" id="messages">
            <div class="chat-widget-empty">
              <div class="chat-widget-empty-icon">💬</div>
              <h3>שלום!</h3>
              <p>שאל שאלה על המסמכים שלך</p>
              <p style="font-size: 12px; margin-top: 10px;">💡 לחץ על 🎤 להקלטה</p>
            </div>
          </div>
          <div class="chat-widget-input-area">
            <div class="chat-widget-input-wrapper">
              <div class="input-container" id="input-container">
                <textarea 
                  class="chat-widget-input" 
                  id="input"
                  placeholder="הקלד הודעה..."
                  rows="1"
                ></textarea>
                <button class="voice-btn" id="voice-btn" title="הקלט קול">
                  <svg class="mic-icon" viewBox="0 0 24 24">
                    <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
                    <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
                  </svg>
                </button>
              </div>
              <button class="send-btn" id="send-btn" title="שלח">
                <svg class="send-icon" viewBox="0 0 24 24">
                  <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    `;

    document.body.insertAdjacentHTML('beforeend', html);
  }

  function setupEventListeners(config) {
    const $ = id => document.getElementById(id);
    
    const state = {
      messages: [],
      history: [],
      isOpen: false,
      isLoading: false,
      isRecording: false,
      recordingStartTime: null,
      recordingTimer: null,
      recordedText: '',
      recognition: null,
      maxHistoryMessages: config.maxHistoryMessages
    };

    setupVoice(state, config);
    loadHistory(state, config);

    $('toggle-btn').onclick = () => toggleWidget(state);
    $('reset-btn').onclick = () => resetChat(state, config);
    $('send-btn').onclick = () => sendMessage(state, config);
    $('voice-btn').onclick = () => toggleVoice(state);
    $('cancel-btn').onclick = () => cancelRecording(state);
    
    $('input').onkeydown = (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage(state, config);
      }
    };

    $('input').oninput = () => {
      $('input').style.height = 'auto';
      $('input').style.height = $('input').scrollHeight + 'px';
    };
  }

  function setupVoice(state, config) {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    
    if (!SpeechRecognition) {
      document.getElementById('voice-btn').style.display = 'none';
      document.getElementById('browser-warning').classList.add('show');
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = 'he-IL';

    recognition.onstart = () => {
      state.isRecording = true;
      state.recordingStartTime = Date.now();
      state.recordedText = '';
      
      document.getElementById('voice-btn').classList.add('recording');
      document.getElementById('recording-bar').classList.add('active');
      document.getElementById('input-container').classList.add('recording');
      document.getElementById('input').placeholder = 'מקשיב...';
      document.getElementById('input').disabled = true;
      document.getElementById('send-btn').disabled = true;
      
      state.recordingTimer = setInterval(() => {
        const elapsed = Math.floor((Date.now() - state.recordingStartTime) / 1000);
        document.getElementById('rec-time').textContent = 
          `${Math.floor(elapsed / 60)}:${(elapsed % 60).toString().padStart(2, '0')}`;
      }, 1000);
    };

    recognition.onresult = (event) => {
      let interim = '';
      let final = '';

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          final += transcript + ' ';
        } else {
          interim += transcript;
        }
      }

      if (final) state.recordedText += final;
      
      const input = document.getElementById('input');
      input.value = state.recordedText + interim;
      input.style.height = 'auto';
      input.style.height = input.scrollHeight + 'px';
    };

    recognition.onerror = (event) => {
      let msg = 'שגיאה בהקלטה';
      if (event.error === 'no-speech') msg = 'לא זוהה דיבור';
      else if (event.error === 'audio-capture') msg = 'לא ניתן לגשת למיקרופון';
      else if (event.error === 'not-allowed') msg = 'נדרשת הרשאה למיקרופון';
      
      alert(msg);
      cancelRecording(state);
    };

    recognition.onend = () => {
      if (state.recordingTimer) {
        clearInterval(state.recordingTimer);
        state.recordingTimer = null;
      }
      
      state.isRecording = false;
      document.getElementById('voice-btn').classList.remove('recording');
      document.getElementById('recording-bar').classList.remove('active');
      document.getElementById('input-container').classList.remove('recording');
      document.getElementById('input').placeholder = 'הקלד הודעה...';
      document.getElementById('input').disabled = false;
      document.getElementById('send-btn').disabled = false;
      
      if (state.recordedText.trim()) {
        const input = document.getElementById('input');
        input.value = state.recordedText.trim();
        input.style.height = 'auto';
        input.style.height = input.scrollHeight + 'px';
        input.focus();
      }
    };

    state.recognition = recognition;
  }

  function toggleVoice(state) {
    if (!state.recognition) {
      alert('הדפדפן שלך לא תומך בהקלטת קול');
      return;
    }

    if (isAtLimit(state)) return;

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

  function cancelRecording(state) {
    if (state.recognition && state.isRecording) {
      state.recordedText = '';
      document.getElementById('input').value = '';
      state.recognition.stop();
    }
  }

  function loadHistory(state, config) {
    try {
      const saved = sessionStorage.getItem('chatHistory_' + config.secretKey);
      if (saved) {
        const data = JSON.parse(saved);
        state.history = data.history || [];
        state.messages = data.messages || [];
        renderMessages(state, config);
        updateUI(state);
      }
    } catch (e) {}
  }

  function saveHistory(state, config) {
    try {
      sessionStorage.setItem('chatHistory_' + config.secretKey, JSON.stringify({
        history: state.history,
        messages: state.messages
      }));
    } catch (e) {}
  }

  function isAtLimit(state) {
    return state.history.length >= state.maxHistoryMessages;
  }

  function updateUI(state) {
    const count = state.history.length;
    document.getElementById('msg-counter').textContent = `${count}/${state.maxHistoryMessages} הודעות`;
    
    document.getElementById('reset-btn').classList.toggle('show', count > 0);
    
    const atLimit = isAtLimit(state);
    document.getElementById('limit-warning').classList.toggle('show', atLimit);
    document.getElementById('input').disabled = atLimit || state.isRecording;
    document.getElementById('send-btn').disabled = atLimit;
    document.getElementById('voice-btn').disabled = atLimit;
  }

  function resetChat(state, config) {
    if (confirm('האם להתחיל שיחה חדשה? ההיסטוריה תימחק.')) {
      state.history = [];
      state.messages = [];
      sessionStorage.removeItem('chatHistory_' + config.secretKey);
      renderMessages(state, config);
      updateUI(state);
    }
  }

  function toggleWidget(state) {
    state.isOpen = !state.isOpen;
    document.getElementById('widget-window').classList.toggle('open', state.isOpen);
    document.getElementById('toggle-btn').textContent = state.isOpen ? '✕' : '💬';
    
    if (state.isOpen) {
      document.getElementById('input').focus();
    } else if (state.isRecording && state.recognition) {
      cancelRecording(state);
    }
  }

  function renderMessages(state, config) {
    const container = document.getElementById('messages');
    
    if (state.messages.length === 0) {
      container.innerHTML = `
        <div class="chat-widget-empty">
          <div class="chat-widget-empty-icon">💬</div>
          <h3>שלום!</h3>
          <p>שאל שאלה על המסמכים שלך</p>
          <p style="font-size: 12px; margin-top: 10px;">💡 לחץ על 🎤 להקלטה</p>
        </div>
      `;
      return;
    }

    const html = state.messages.map(msg => {
      const lang = detectLanguage(msg.content);
      const dir = lang === 'he' ? 'rtl' : 'ltr';
      const content = msg.content.trim();
      const voiceIcon = msg.isVoice ? '<div class="voice-indicator">🎤 הודעת קול</div>' : '';
      
      return `
        <div class="chat-message ${msg.role}">
          <div class="chat-message-avatar">${createAvatar(msg.role, config)}</div>
          <div class="chat-message-content">
            ${voiceIcon}
            <div class="chat-message-bubble ${dir}">${escapeHtml(content)}</div>
          </div>
        </div>
      `;
    }).join('');

    container.innerHTML = html;

    if (state.isLoading) {
      container.innerHTML += `
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

    container.scrollTop = container.scrollHeight;
  }

  async function sendMessage(state, config) {
    const input = document.getElementById('input');
    const question = input.value.trim();
    const isVoice = state.recordedText.length > 0;
    
    if (!question || state.isLoading || isAtLimit(state)) return;

    state.recordedText = '';

    state.messages.push({
      role: 'user',
      content: question,
      timestamp: new Date().toISOString(),
      isVoice: isVoice
    });

    state.history.push({
      role: 'user',
      content: question
    });

    input.value = '';
    input.style.height = 'auto';
    state.isLoading = true;
    document.getElementById('send-btn').disabled = true;
    document.getElementById('voice-btn').disabled = true;
    
    renderMessages(state, config);
    saveHistory(state, config);
    updateUI(state);

    try {
      const res = await fetch(`${config.apiUrl}/api/query/ask`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          secretKey: config.secretKey,
          question: question,
          history: state.history
        })
      });

      const data = await res.json();

      const answer = (data.success && data.data.answer) 
        ? data.data.answer 
        : 'מצטער, לא הצלחתי למצוא תשובה.';

      state.messages.push({
        role: 'assistant',
        content: answer,
        timestamp: new Date().toISOString(),
        isVoice: false
      });

      state.history.push({
        role: 'assistant',
        content: answer
      });
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
      if (!isAtLimit(state)) {
        document.getElementById('send-btn').disabled = false;
        document.getElementById('voice-btn').disabled = false;
      }
      
      renderMessages(state, config);
      saveHistory(state, config);
      updateUI(state);
      input.focus();
    }
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  function detectLanguage(text) {
    if (!text) return 'he';
    let heb = 0, total = 0;
    for (let char of text) {
      if (/\p{L}/u.test(char)) {
        total++;
        if (char >= '\u0590' && char <= '\u05FF') heb++;
      }
    }
    return (total > 0 && (heb / total) > 0.3) ? 'he' : 'en';
  }

  function createAvatar(role, config) {
    if (role === 'user') {
      return config.userAvatar 
        ? `<img src="${escapeHtml(config.userAvatar)}" alt="User" />`
        : 'אני';
    } else {
      return config.botAvatar 
        ? `<img src="${escapeHtml(config.botAvatar)}" alt="${escapeHtml(config.botName)}" />`
        : escapeHtml(config.botName.charAt(0));
    }
  }

  console.log('✅ Chat Widget ready');
})();