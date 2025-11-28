// frontend/src/components/Auth/ResetPassword.js
import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authAPI } from '../../services/api';
import './ResetPassword.css';

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  const [formData, setFormData] = useState({
    newPassword: '',
    confirmPassword: ''
  });
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState('');
  const [countdown, setCountdown] = useState(3);
  
  const navigate = useNavigate();
  
  // פרמטרים מה-URL
  const email = searchParams.get('email');
  const verified = searchParams.get('verified');

  // בדיקה שהגיעו מדף האימות
  useEffect(() => {
    if (!email || verified !== 'true') {
      // אם לא הגיעו מדף האימות, חזרה לשחזור סיסמה
      navigate('/forgot-password');
    }
  }, [email, verified, navigate]);

  // ספירה לאחור אחרי הצלחה
  useEffect(() => {
    if (success && countdown > 0) {
      const timer = setTimeout(() => {
        setCountdown(countdown - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else if (success && countdown === 0) {
      navigate('/login?reset=success');
    }
  }, [success, countdown, navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    
    if (name === 'newPassword') {
      validatePassword(value);
    }
  };

  const validatePassword = (password) => {
    if (password.length < 6) {
      setPasswordStrength('חלשה');
      return;
    }

    let strength = 'חלשה';
    if (password.length >= 8 && /[A-Z]/.test(password) && /[0-9]/.test(password)) {
      strength = 'חזקה';
    } else if (password.length >= 6 && (/[A-Z]/.test(password) || /[0-9]/.test(password))) {
      strength = 'בינונית';
    }

    setPasswordStrength(strength);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // ולידציות
    if (!formData.newPassword) {
      setError('נא להזין סיסמה חדשה');
      return;
    }

    if (formData.newPassword.length < 6) {
      setError('הסיסמה חייבת להכיל לפחות 6 תווים');
      return;
    }

    if (formData.newPassword !== formData.confirmPassword) {
      setError('הסיסמאות אינן זהות');
      return;
    }

    setLoading(true);

    try {
      const response = await authAPI.setNewPassword(email, formData.newPassword);

      if (response.data.success) {
        setSuccess(true);
      } else {
        setError(response.data.error || 'שגיאה בשינוי הסיסמה');
      }
    } catch (err) {
      console.error('Reset password error:', err);
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

  return (
    <div className="reset-password-page">
      <div className="reset-password-container">
        <div className="logo">🔐 Custom Site Chat</div>
        <div className="subtitle">יצירת סיסמה חדשה</div>

        {error && (
          <div className="alert alert-error">{error}</div>
        )}

        {success ? (
          <div className="success-state">
            <div className="success-icon">✅</div>
            <h3>הסיסמה שונתה בהצלחה!</h3>
            <p>כעת תוכל להתחבר עם הסיסמה החדשה</p>
            <p style={{ fontSize: '14px', color: '#666', marginTop: '15px' }}>
              מעביר אותך להתחברות בעוד {countdown} שניות...
            </p>
            <button 
              className="submit-btn"
              onClick={() => navigate('/login?reset=success')}
              style={{ marginTop: '20px' }}
            >
              עבור להתחברות עכשיו
            </button>
          </div>
        ) : (
          <>
            <div className="email-info">
              <p>יוצר סיסמה חדשה עבור:</p>
              <p className="email-address">{email}</p>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="newPassword">סיסמה חדשה:</label>
                <input
                  type="password"
                  id="newPassword"
                  name="newPassword"
                  value={formData.newPassword}
                  onChange={handleChange}
                  placeholder="הזן סיסמה חדשה"
                  required
                  disabled={loading}
                />
                {passwordStrength && (
                  <div className={`password-strength strength-${
                    passwordStrength === 'חזקה' ? 'strong' : 
                    passwordStrength === 'בינונית' ? 'medium' : 'weak'
                  }`}>
                    חוזק סיסמה: {passwordStrength}
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="confirmPassword">אישור סיסמה:</label>
                <input
                  type="password"
                  id="confirmPassword"
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  placeholder="הזן שוב את הסיסמה החדשה"
                  required
                  disabled={loading}
                />
                {formData.confirmPassword && (
                  <div className={`field-validation ${
                    formData.newPassword === formData.confirmPassword 
                      ? 'validation-success' 
                      : 'validation-error'
                  }`}>
                    {formData.newPassword === formData.confirmPassword 
                      ? 'הסיסמאות זהות ✓' 
                      : 'הסיסמאות אינן זהות'}
                  </div>
                )}
              </div>

              <button 
                type="submit" 
                className="submit-btn"
                disabled={loading}
              >
                {loading ? 'משנה סיסמה...' : 'שנה סיסמה'}
              </button>
            </form>
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

export default ResetPassword;