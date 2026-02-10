// frontend/src/services/tokenSSE.js

class TokenSSEService {
  constructor() {
    this.eventSource = null;
    this.listeners = new Set();
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 2000; // 2 seconds
  }

  /**
   * התחברות ל-SSE stream
   */
  connect() {
    if (this.eventSource) {
      console.log('⚠️ SSE already connected');
      return;
    }

    const token = localStorage.getItem('token');
    if (!token) {
      console.error('❌ No token found, cannot connect to SSE');
      return;
    }

    console.log('📡 Connecting to token SSE stream...');

    // יצירת EventSource עם header של Authorization
    const url = `/api/tokens/stream`;
    
    this.eventSource = new EventSource(url, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    // אירוע חיבור
    this.eventSource.addEventListener('connected', (event) => {
      console.log('✅ Connected to token SSE:', event.data);
      this.reconnectAttempts = 0; // איפוס מונה ניסיונות
    });

    // אירוע עדכון טוקנים
    this.eventSource.addEventListener('token-update', (event) => {
      console.log('💰 Token update received:', event.data);
      
      try {
        const data = JSON.parse(event.data);
        this.notifyListeners(data);
      } catch (error) {
        console.error('Error parsing token update:', error);
      }
    });

    // טיפול בשגיאות
    this.eventSource.onerror = (error) => {
      console.error('❌ SSE error:', error);
      
      if (this.eventSource.readyState === EventSource.CLOSED) {
        console.log('🔄 SSE connection closed, attempting to reconnect...');
        this.handleReconnect();
      }
    };
  }

  /**
   * ניסיון חיבור מחדש
   */
  handleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('❌ Max reconnect attempts reached');
      return;
    }

    this.reconnectAttempts++;
    console.log(`🔄 Reconnect attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);

    setTimeout(() => {
      this.disconnect();
      this.connect();
    }, this.reconnectDelay * this.reconnectAttempts); // Exponential backoff
  }

  /**
   * ניתוק מה-SSE
   */
  disconnect() {
    if (this.eventSource) {
      console.log('📴 Disconnecting from token SSE');
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  /**
   * הרשמה לעדכוני טוקנים
   */
  addListener(callback) {
    this.listeners.add(callback);
    console.log(`📢 Added listener (total: ${this.listeners.size})`);
  }

  /**
   * הסרת listener
   */
  removeListener(callback) {
    this.listeners.delete(callback);
    console.log(`📢 Removed listener (total: ${this.listeners.size})`);
  }

  /**
   * שליחת עדכון לכל ה-listeners
   */
  notifyListeners(data) {
    this.listeners.forEach(callback => {
      try {
        callback(data);
      } catch (error) {
        console.error('Error in listener callback:', error);
      }
    });
  }

  /**
   * בדיקת מצב החיבור
   */
  isConnected() {
    return this.eventSource && this.eventSource.readyState === EventSource.OPEN;
  }
}

// יצירת instance יחיד
const tokenSSEService = new TokenSSEService();

export default tokenSSEService;