// frontend/src/components/Auth/Verify.js
import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authAPI } from '../../services/api';
import './Verify.css';

const Verify = () => {
  const [searchParams] = useSearchParams();
  const [verificationCode, setVerificationCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [resendSuccess, setResendSuccess] = useState(false);
  const [countdown, setCountdown] = useState(3);
  
  const navigate = useNavigate();
  
  // פרמטרים מה-URL
  const email = searchParams.get('email');
  const mode = searchParams.get('mode'); // 'register', 'reset', or 'wait'

  // קביעת מצב
  const isPasswordReset = mode === 'reset';

  // בדיקת פרמטרים
  useEffect(() => {
    if (!email) {
      navigate(isPasswordReset ? '/forgot-password' : '/register');
    }
  }, [email, isPasswordReset, navigate]);

  // ספירה לאחור אחרי הצלחה
  useEffect(() => {
    if (success && countdown > 0) {
      const timer = setTimeout(() => {
        setCountdown(countdown - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else if (success && countdown === 0) {
      if (isPasswordReset) {
        // מעבר לדף שינוי סיסמה
        navigate(`/reset-password?email=${encodeURIComponent(email)}&verified=true`);
      } else {
        // מעבר לדשבורד (עמוד הבית)
        navigate('/');
      }
    }
  }, [success, countdown, navigate, email, isPasswordReset]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!verificationCode || verificationCode.length !== 6) {
      setError('נא להזין קוד בן 6 ספרות');
      return;
    }

    setLoading(true);

    try {
      let response;
      
      if (isPasswordReset) {
        // אימות קוד איפוס סיסמה
        response = await authAPI.verifyResetCode(email, verificationCode);
      } else {
        // אימות רישום (יוצר את המשתמש ב-DB!)
        response = await authAPI.verify({ 
          email, 
          verificationCode 
        });
        
        // 🆕 אחרי הרשמה מוצלחת - שומר טוקן ומשתמש
        if (response.data.success && response.data.token) {
          localStorage.setItem('token', response.data.token);
          localStorage.setItem('user', JSON.stringify(response.data.user));
        }
      }

      if (response.data.success) {
        setSuccess(true);
      } else {
        setError(response.data.error || 'קוד אימות שגוי');
      }
    } catch (err) {
      console.error('Verification error:', err);
      if (err.response?.data?.message) {
        setError(err.response.data.message);
      } else if (err.response?.data?.error) {
        setError(err.response.data.error);
      } else {
        setError('שגיאה בחיבור לשרת');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setResendLoading(true);
    setResendSuccess(false);
    setError('');

    try {
      if (isPasswordReset) {
        // שליחת קוד איפוס מחדש
        await authAPI.forgotPassword(email);
      } else {
        // שליחת קוד אימות מחדש
        await authAPI.resendVerificationCode(email);
      }
      setResendSuccess(true);
    } catch (err) {
      console.error('Resend error:', err);
      setError('שגיאה בשליחת קוד חדש');
    } finally {
      setResendLoading(false);
    }
  };

  const handleCodeChange = (e) => {
    // מאפשר רק מספרים, מקסימום 6 תווים
    const value = e.target.value.replace(/\D/g, '').slice(0, 6);
    setVerificationCode(value);
  };

  // הודעת הצלחה - רק ההפניה שונה
  const getSuccessMessage = () => {
    if (isPasswordReset) {
      return 'מעביר אותך ליצירת סיסמה חדשה...';
    }
    return 'מעביר אותך לדשבורד...';
  };

  return (
    <div className="verify-page">
      <div className="verify-container">
        <div className="logo">🔐 Custom Site Chat</div>
        <div className="subtitle">אימות חשבון</div>

        {error && (
          <div className="alert alert-error">{error}</div>
        )}

        {resendSuccess && (
          <div className="alert alert-success">קוד חדש נשלח בהצלחה!</div>
        )}

        {success ? (
          <div className="success-state">
            <div className="success-icon">✅</div>
            <h3>הקוד אומת בהצלחה!</h3>
            <p>{getSuccessMessage()}</p>
            <p style={{ fontSize: '14px', color: '#666', marginTop: '10px' }}>
              מעביר בעוד {countdown} שניות...
            </p>
          </div>
        ) : (
          <>
            <div className="email-info">
              <p>הזן את קוד האימות שנשלח למייל שלך</p>
              <p className="email-address">{email}</p>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="verificationCode">קוד אימות (6 ספרות):</label>
                <input
                  type="text"
                  id="verificationCode"
                  className="code-input"
                  value={verificationCode}
                  onChange={handleCodeChange}
                  placeholder="000000"
                  maxLength={6}
                  required
                  disabled={loading}
                  autoFocus
                />
              </div>

              <button 
                type="submit" 
                className="submit-btn"
                disabled={loading || verificationCode.length !== 6}
              >
                {loading ? 'מאמת...' : 'אמת קוד'}
              </button>
            </form>

            <div className="resend-section">
              <p>לא קיבלת קוד?</p>
              <button 
                className="resend-btn"
                onClick={handleResend}
                disabled={resendLoading}
              >
                {resendLoading ? 'שולח...' : 'שלח קוד חדש'}
              </button>
              <p className="spam-note">בדוק גם בתיקיית הספאם</p>
            </div>
          </>
        )}

        <div className="back-to-login">
          <p>
            <span 
              onClick={() => navigate('/login')}
              style={{ cursor: 'pointer', color: '#667eea', fontWeight: 500 }}
            >
              חזור להתחברות
            </span>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Verify;