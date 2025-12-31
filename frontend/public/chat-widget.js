// ==============================================================================
// CHAT WIDGET - Custom Site Chat
// ==============================================================================

(function () {
  'use strict';

  // ============================================================================
  // CONFIGURATION
  // ============================================================================
  
  const config = {
    apiUrl: window.chatWidgetConfig?.apiUrl || 'http://localhost:8080',
    secretKey: window.chatWidgetConfig?.secretKey || '',
    businessType: window.chatWidgetConfig?.businessType || 'אתר כללי',
    position: window.chatWidgetConfig?.position || 'bottom-right',
    primaryColor: window.chatWidgetConfig?.primaryColor || '#667eea',
    welcomeMessage: window.chatWidgetConfig?.welcomeMessage || 'שלום! איך אפשר לעזור?'
  };

  // ============================================================================
  // STATE MANAGEMENT
  // ============================================================================
  
  const state = {
    isOpen: false,
    isMinimized: false,
    isLoading: false,
    history: [],
    messageCount: 0
  };

  // ============================================================================
  // ANALYTICS PROMPTS
  // ============================================================================
  
  const ANALYTICS_PROMPTS = {
    unansweredQuestions: `נתח את השיחה למעלה וזהה שאלות שלא ידעת לענות עליהן.

כללים:
- החזר לי רק שאלות שהמשתמש שאל (role: "user")
- החזר לי רק שאלות שקשורות לנושאים שבאתר, ולא שאלות חולין כגון: בדיחות, ברכות, או שאלות שלא קשורות לאתר
- אם שאלה היא המשך - נרמל אותה להיות מובנת לבד

פורמט תשובה - שורה אחת לכל שאלה:
בצורה כזאת:
כמה עולה החולצה?
האם יש משלוח חינם?
מתי אתם פתוחים?

אם אין שאלות - החזר שורה ריקה.`,

    topics: `נתח את השיחה למעלה וזהה את הנושאים העיקריים (עד 3 נושאים).

זהה כוונות:
לדוגמא:
- "כמה עולה" = מחירים
- "משלוח" = משלוחים  
- "החזרה" = החזרות

פורמט תשובה - שורה אחת לכל נושא:
בצורה כזאת:
מחירים
משלוחים
החזרות

אם אין נושאים - החזר שורה ריקה.`
  };

  // ============================================================================
  // DOM ELEMENTS CACHE
  // ============================================================================
  
  let elements = {};

  // ============================================================================
  // INITIALIZATION
  // ============================================================================
  
  function init() {
    if (!config.secretKey) {
      console.error('❌ Chat Widget Error: secretKey is required!');
      return;
    }

    injectStyles();
    createWidget();
    cacheElements();
    setupEventListeners();
    
    console.log('✅ Chat Widget initialized successfully');
  }

  // ============================================================================
  // STYLES INJECTION
  // ============================================================================
  
  function injectStyles() {
    const style = document.createElement('style');
    style.textContent = `
      /* Widget Container */
      .chat-widget-container {
        position: fixed;
        ${config.position.includes('right') ? 'right: 20px;' : 'left: 20px;'}
        ${config.position.includes('top') ? 'top: 20px;' : 'bottom: 20px;'}
        z-index: 999999;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      }

      /* Toggle Button */
      .chat-widget-toggle {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: ${config.primaryColor};
        border: none;
        cursor: pointer;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        display: flex;
        align-items: center;
        justify-content: center;
        transition: transform 0.2s, box-shadow 0.2s;
      }

      .chat-widget-toggle:hover {
        transform: scale(1.05);
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
      }

      .chat-widget-toggle svg {
        width: 28px;
        height: 28px;
        fill: white;
      }

      /* Chat Window */
      .chat-widget-window {
        display: none;
        position: fixed;
        ${config.position.includes('right') ? 'right: 20px;' : 'left: 20px;'}
        ${config.position.includes('top') ? 'top: 20px;' : 'bottom: 90px;'}
        width: 380px;
        height: 600px;
        max-height: calc(100vh - 120px);
        background: white;
        border-radius: 16px;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
        display: flex;
        flex-direction: column;
        overflow: hidden;
        animation: slideUp 0.3s ease-out;
      }

      .chat-widget-window.open {
        display: flex;
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
        background: ${config.primaryColor};
        color: white;
        padding: 16px;
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .chat-widget-header h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
      }

      .chat-widget-header-buttons {
        display: flex;
        gap: 8px;
      }

      .chat-widget-header button {
        background: rgba(255, 255, 255, 0.2);
        border: none;
        color: white;
        width: 32px;
        height: 32px;
        border-radius: 50%;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.2s;
      }

      .chat-widget-header button:hover {
        background: rgba(255, 255, 255, 0.3);
      }

      /* Messages Area */
      .chat-widget-messages {
        flex: 1;
        overflow-y: auto;
        padding: 16px;
        background: #f7f9fc;
      }

      .chat-message {
        margin-bottom: 12px;
        display: flex;
        gap: 8px;
      }

      .chat-message.user {
        flex-direction: row-reverse;
      }

      .chat-message-content {
        max-width: 75%;
        padding: 10px 14px;
        border-radius: 12px;
        word-wrap: break-word;
      }

      .chat-message.bot .chat-message-content {
        background: white;
        color: #333;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
      }

      .chat-message.user .chat-message-content {
        background: ${config.primaryColor};
        color: white;
      }

      /* Token Counter */
      .chat-widget-token-counter {
        padding: 8px 16px;
        background: #f0f4f8;
        border-top: 1px solid #e2e8f0;
        font-size: 12px;
        color: #64748b;
        text-align: center;
      }

      /* Input Area */
      .chat-widget-input {
        padding: 16px;
        background: white;
        border-top: 1px solid #e2e8f0;
        display: flex;
        gap: 8px;
      }

      .chat-widget-input input {
        flex: 1;
        padding: 10px 14px;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        font-size: 14px;
        outline: none;
        transition: border-color 0.2s;
      }

      .chat-widget-input input:focus {
        border-color: ${config.primaryColor};
      }

      .chat-widget-input button {
        background: ${config.primaryColor};
        color: white;
        border: none;
        padding: 10px 20px;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 500;
        transition: opacity 0.2s;
      }

      .chat-widget-input button:hover:not(:disabled) {
        opacity: 0.9;
      }

      .chat-widget-input button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      /* Loading Indicator */
      .chat-loading {
        display: flex;
        gap: 4px;
        padding: 10px 14px;
      }

      .chat-loading span {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: ${config.primaryColor};
        animation: bounce 1.4s infinite ease-in-out;
      }

      .chat-loading span:nth-child(1) { animation-delay: -0.32s; }
      .chat-loading span:nth-child(2) { animation-delay: -0.16s; }

      @keyframes bounce {
        0%, 80%, 100% { transform: scale(0); }
        40% { transform: scale(1); }
      }

      /* Mobile Responsive */
      @media (max-width: 480px) {
        .chat-widget-window {
          width: calc(100vw - 32px);
          height: calc(100vh - 100px);
          ${config.position.includes('right') ? 'right: 16px;' : 'left: 16px;'}
        }
      }
    `;
    document.head.appendChild(style);
  }

  // ============================================================================
  // CREATE WIDGET HTML
  // ============================================================================
  
  function createWidget() {
    const container = document.createElement('div');
    container.className = 'chat-widget-container';
    container.innerHTML = `
      <button class="chat-widget-toggle" id="chatToggle" aria-label="פתח צ'אט">
        <svg viewBox="0 0 24 24">
          <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/>
        </svg>
      </button>

      <div class="chat-widget-window" id="chatWindow">
        <div class="chat-widget-header">
          <h3>💬 צ'אט</h3>
          <div class="chat-widget-header-buttons">
            <button id="resetChat" title="שיחה חדשה">🔄</button>
            <button id="closeChat" title="סגור">✕</button>
          </div>
        </div>

        <div class="chat-widget-messages" id="chatMessages">
          <div class="chat-message bot">
            <div class="chat-message-content">${config.welcomeMessage}</div>
          </div>
        </div>

        <div class="chat-widget-token-counter" id="tokenCounter">
          0 שאלות נשאלו
        </div>

        <div class="chat-widget-input">
          <input 
            type="text" 
            id="userInput" 
            placeholder="הקלד/י הודעה..."
            autocomplete="off"
          />
          <button id="sendButton">שלח</button>
        </div>
      </div>
    `;
    document.body.appendChild(container);
  }

  // ============================================================================
  // CACHE DOM ELEMENTS
  // ============================================================================
  
  function cacheElements() {
    elements = {
      toggle: document.getElementById('chatToggle'),
      window: document.getElementById('chatWindow'),
      messages: document.getElementById('chatMessages'),
      input: document.getElementById('userInput'),
      sendButton: document.getElementById('sendButton'),
      closeButton: document.getElementById('closeChat'),
      resetButton: document.getElementById('resetChat'),
      tokenCounter: document.getElementById('tokenCounter')
    };
  }

  // ============================================================================
  // EVENT LISTENERS
  // ============================================================================
  
  function setupEventListeners() {
    // Analytics: Send on page unload
    window.addEventListener('beforeunload', async () => {
      await sendAnalytics();
    });

    // Toggle chat window
    elements.toggle.addEventListener('click', () => {
      state.isOpen = !state.isOpen;
      elements.window.classList.toggle('open', state.isOpen);
      
      if (state.isOpen) {
        elements.input.focus();
      }
    });

    // Close button (only hides, doesn't delete history)
    elements.closeButton.addEventListener('click', () => {
      state.isOpen = false;
      elements.window.classList.remove('open');
    });

    // Reset button (new chat + send analytics)
    elements.resetButton.addEventListener('click', async () => {
      await sendAnalytics();
      resetChat();
    });

    // Send message on button click
    elements.sendButton.addEventListener('click', () => {
      const message = elements.input.value.trim();
      if (message) {
        sendMessage(message);
      }
    });

    // Send message on Enter key
    elements.input.addEventListener('keypress', (e) => {
      if (e.key === 'Enter' && !state.isLoading) {
        const message = elements.input.value.trim();
        if (message) {
          sendMessage(message);
        }
      }
    });
  }

  // ============================================================================
  // SEND MESSAGE
  // ============================================================================
  
  async function sendMessage(userMessage) {
    if (state.isLoading) return;

    // Add user message to UI
    addMessage('user', userMessage);
    
    // Clear input
    elements.input.value = '';
    
    // Add to history
    state.history.push({
      role: 'user',
      content: userMessage
    });

    // Update counter (count only user messages)
    updateTokenCounter();

    // Show loading
    state.isLoading = true;
    elements.sendButton.disabled = true;
    const loadingId = addLoadingMessage();

    try {
      // Send to backend
      const response = await fetch(`${config.apiUrl}/api/collections/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          secretKey: config.secretKey,
          userMessage: userMessage,
          conversationHistory: state.history
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();

      // Remove loading
      removeLoadingMessage(loadingId);

      // Add bot response
      addMessage('bot', data.response);

      // Add to history
      state.history.push({
        role: 'assistant',
        content: data.response
      });

    } catch (error) {
      console.error('❌ Chat error:', error);
      removeLoadingMessage(loadingId);
      addMessage('bot', 'מצטער, אירעה שגיאה. נסה שוב.');
    } finally {
      state.isLoading = false;
      elements.sendButton.disabled = false;
      elements.input.focus();
    }
  }

  // ============================================================================
  // SEND ANALYTICS (AT END OF SESSION)
  // ============================================================================
  
  async function sendAnalytics() {
    if (state.history.length === 0) return;

    console.log('📊 Analyzing session...');

    try {
      // Step 1: Ask bot to analyze unanswered questions
      const questionsAnalysis = await askBotForAnalysis(ANALYTICS_PROMPTS.unansweredQuestions);
      
      // Step 2: Ask bot to analyze topics
      const topicsAnalysis = await askBotForAnalysis(ANALYTICS_PROMPTS.topics);

      // Step 3: Send to analytics endpoint
      await fetch(`${config.apiUrl}/api/analytics/session-ended`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          secretKey: config.secretKey,
          unansweredQuestions: questionsAnalysis,
          topics: topicsAnalysis
        }),
        keepalive: true
      });

      console.log('✅ Analytics sent successfully');
      console.log('📊 Questions:', questionsAnalysis);
      console.log('📊 Topics:', topicsAnalysis);

    } catch (err) {
      console.error('❌ Analytics error:', err);
    }
  }

  // ============================================================================
  // ASK BOT FOR ANALYSIS (REUSE SAME SESSION)
  // ============================================================================
  
  async function askBotForAnalysis(prompt) {
    try {
      const response = await fetch(`${config.apiUrl}/api/collections/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          secretKey: config.secretKey,
          userMessage: prompt,
          conversationHistory: state.history
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      
      // Extract JSON from response
      return extractJsonFromResponse(data.response);

    } catch (error) {
      console.error('❌ Analytics query failed:', error);
      return [];
    }
  }

  // ============================================================================
  // EXTRACT LINES FROM RESPONSE
  // ============================================================================
  
  function extractJsonFromResponse(text) {
    try {
      // Split text into lines and filter
      const lines = text
        .split('\n')
        .map(line => line.trim())
        .filter(line => {
          // Filter empty lines and unwanted text
          if (!line) return false;
          if (line.startsWith('```')) return false;
          if (line.startsWith('[')) return false;
          if (line.startsWith(']')) return false;
          if (line.startsWith('{')) return false;
          if (line.startsWith('}')) return false;
          if (line.toLowerCase().includes('json')) return false;
          if (line.includes('נתח')) return false;
          if (line.includes('החזר')) return false;
          if (line.includes('פורמט')) return false;
          if (line.includes('כללים')) return false;
          if (line.includes('דוגמ')) return false;
          return true;
        });
      
      return lines;
    } catch (e) {
      console.warn('Failed to extract lines from response:', text);
      return [];
    }
  }

  // ============================================================================
  // UI HELPER FUNCTIONS
  // ============================================================================
  
  function addMessage(role, content) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `chat-message ${role}`;
    messageDiv.innerHTML = `
      <div class="chat-message-content">${escapeHtml(content)}</div>
    `;
    elements.messages.appendChild(messageDiv);
    elements.messages.scrollTop = elements.messages.scrollHeight;
  }

  function addLoadingMessage() {
    const loadingDiv = document.createElement('div');
    loadingDiv.className = 'chat-message bot';
    loadingDiv.id = 'loading-' + Date.now();
    loadingDiv.innerHTML = `
      <div class="chat-message-content">
        <div class="chat-loading">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    `;
    elements.messages.appendChild(loadingDiv);
    elements.messages.scrollTop = elements.messages.scrollHeight;
    return loadingDiv.id;
  }

  function removeLoadingMessage(loadingId) {
    const loadingDiv = document.getElementById(loadingId);
    if (loadingDiv) {
      loadingDiv.remove();
    }
  }

  function updateTokenCounter() {
    // Count only user messages (not assistant responses!)
    const userMessageCount = state.history.filter(msg => msg.role === 'user').length;
    elements.tokenCounter.textContent = `${userMessageCount} ${userMessageCount === 1 ? 'שאלה נשאלה' : 'שאלות נשאלו'}`;
  }

  function resetChat() {
    state.history = [];
    state.messageCount = 0;
    elements.messages.innerHTML = `
      <div class="chat-message bot">
        <div class="chat-message-content">${config.welcomeMessage}</div>
      </div>
    `;
    updateTokenCounter();
    console.log('🔄 Chat reset');
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  // ============================================================================
  // START
  // ============================================================================
  
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();