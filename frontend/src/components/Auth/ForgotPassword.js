// frontend/src/components/Auth/ForgotPassword.js
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI } from '../../services/api';
import './ForgotPassword.css';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await authAPI.forgotPassword(email);
      
      if (response.data.success) {
        setSuccess(true);
        setTimeout(() => {
          navigate('/reset-password?email=' + encodeURIComponent(email));
        }, 2000);
      } else {
        setError(response.data.error || 'שגיאה בשליחת קוד איפוס');
      }
    } catch (err) {
      console.error('Forgot password error:', err);
      if (err.response?.data?.message) {
        setError(err.response.data.message);
      } else {
        setError('שגיאה בחיבור לשרת');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="forgot-password-page">
      <div className="forgot-password-container">
        <div className="logo">🔐 Custom Site Chat</div>
        <div className="subtitle">איפוס סיסמה</div>

        {error && (
          <div className="alert alert-error">{error}</div>
        )}

        {success ? (
          <div className="success-state">
            <div className="success-icon">✅</div>
            <h3>קוד איפוס נשלח!</h3>
            <p>בדוק את תיבת המייל שלך ב:</p>
            <p style={{ fontWeight: 600, color: '#667eea', marginTop: '10px' }}>{email}</p>
            <p style={{ fontSize: '14px', color: '#666', marginTop: '15px' }}>
              מעביר אותך לעמוד איפוס הסיסמה...
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="email">כתובת אימייל:</label>
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="הזן את כתובת המייל שלך"
                required
                disabled={loading}
              />
            </div>

            <button 
              type="submit" 
              className="submit-btn"
              disabled={loading}
            >
              {loading ? 'שולח...' : 'שלח קוד איפוס'}
            </button>
          </form>
        )}

        <div className="back-to-login">
          <p>
            נזכרת בסיסמה?{' '}
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

export default ForgotPassword;