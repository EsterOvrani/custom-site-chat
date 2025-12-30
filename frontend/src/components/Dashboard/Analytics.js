import React, { useState, useEffect } from 'react';
import { analyticsAPI } from '../../services/api';
import './Analytics.css';

const Analytics = () => {
  const [activeSubTab, setActiveSubTab] = useState('questions');
  const [questions, setQuestions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    loadStats();
  }, []);

  useEffect(() => {
    if (activeSubTab === 'questions') {
      loadQuestions();
    } else if (activeSubTab === 'categories') {
      loadCategories();
    }
  }, [activeSubTab]);

  const loadStats = async () => {
    try {
      const response = await analyticsAPI.getStats();
      if (response.data.success) {
        setStats(response.data.data);
      }
    } catch (error) {
      console.error('Error loading stats:', error);
    }
  };

  const loadQuestions = async () => {
    try {
      setLoading(true);
      const response = await analyticsAPI.getQuestions();
      if (response.data.success) {
        setQuestions(response.data.data || []);
      }
    } catch (error) {
      console.error('Error loading questions:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadCategories = async () => {
    try {
      setLoading(true);
      const response = await analyticsAPI.getCategories();
      if (response.data.success) {
        setCategories(response.data.data || []);
      }
    } catch (error) {
      console.error('Error loading categories:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadExcel = async () => {
    try {
      setDownloading(true);
      const response = await analyticsAPI.downloadQuestionsExcel();
      
      const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      });
      
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `questions-report-${new Date().toISOString().split('T')[0]}.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      alert('הדוח הורד בהצלחה!');
    } catch (error) {
      console.error('Error downloading Excel:', error);
      alert('שגיאה בהורדת הדוח');
    } finally {
      setDownloading(false);
    }
  };

  const handleClearAll = async () => {
    if (!window.confirm('האם אתה בטוח שברצונך למחוק את כל נתוני האנליטיקס?')) {
      return;
    }

    try {
      await analyticsAPI.clearAll();
      alert('הנתונים נמחקו בהצלחה');
      loadStats();
      loadQuestions();
      loadCategories();
    } catch (error) {
      console.error('Error clearing analytics:', error);
      alert('שגיאה במחיקת נתונים');
    }
  };

  return (
    <div className="analytics-container">
      {/* Header with Stats */}
      <div className="analytics-header">
        <h2>📊 אנליטיקס</h2>
        {stats && (
          <div className="stats-summary">
            <div className="stat-card">
              <div className="stat-value">{stats.totalSessions}</div>
              <div className="stat-label">שיחות</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.totalQuestions}</div>
              <div className="stat-label">שאלות</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.uniqueQuestions}</div>
              <div className="stat-label">שאלות ייחודיות</div>
            </div>
          </div>
        )}
      </div>

      {/* Sub Tabs */}
      <div className="analytics-sub-tabs">
        <button
          className={`sub-tab ${activeSubTab === 'questions' ? 'active' : ''}`}
          onClick={() => setActiveSubTab('questions')}
        >
          📝 שאלות ללא מענה
        </button>
        <button
          className={`sub-tab ${activeSubTab === 'categories' ? 'active' : ''}`}
          onClick={() => setActiveSubTab('categories')}
        >
          📈 נושאים מרכזיים
        </button>
      </div>

      {/* Content */}
      <div className="analytics-content">
        {/* Questions Tab */}
        {activeSubTab === 'questions' && (
          <div className="questions-section">
            <div className="section-header">
              <h3>שאלות שהבוט לא ידע לענות עליהן</h3>
              <div className="section-actions">
                <button
                  className="btn-download"
                  onClick={handleDownloadExcel}
                  disabled={downloading || questions.length === 0}
                >
                  {downloading ? '⏬ מוריד...' : '📥 הורד דוח Excel'}
                </button>
                <button
                  className="btn-clear"
                  onClick={handleClearAll}
                  disabled={questions.length === 0}
                >
                  🗑️ נקה הכל
                </button>
              </div>
            </div>

            {loading ? (
              <div className="loading">
                <div className="spinner"></div>
                <p>טוען נתונים...</p>
              </div>
            ) : questions.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">📭</div>
                <p>אין שאלות ללא מענה עדיין</p>
                <p className="empty-hint">
                  כשמשתמשים ישאלו שאלות שהבוט לא יודע לענות עליהן, הן יופיעו כאן
                </p>
              </div>
            ) : (
              <div className="questions-list">
                {questions.map((q, index) => (
                  <div key={index} className="question-card">
                    <div className="question-header">
                      <span className="question-count">#{index + 1}</span>
                      <span className="question-frequency">נשאלה {q.count} פעמים</span>
                    </div>
                    <div className="question-text">{q.question}</div>
                    {q.examples && q.examples.length > 0 && (
                      <div className="question-examples">
                        <strong>דוגמאות לניסוחים:</strong>
                        <ul>
                          {q.examples.slice(0, 3).map((ex, i) => (
                            <li key={i}>{ex}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Categories Tab */}
        {activeSubTab === 'categories' && (
          <div className="categories-section">
            <div className="section-header">
              <h3>נושאים שמעניינים את המשתמשים</h3>
              <button
                className="btn-clear"
                onClick={handleClearAll}
                disabled={categories.length === 0}
              >
                🗑️ נקה הכל
              </button>
            </div>

            {loading ? (
              <div className="loading">
                <div className="spinner"></div>
                <p>טוען נתונים...</p>
              </div>
            ) : categories.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">📭</div>
                <p>אין נתונים על נושאים עדיין</p>
                <p className="empty-hint">
                  לאחר שמשתמשים ידברו עם הבוט, הנושאים שהם מתעניינים בהם יופיעו כאן
                </p>
              </div>
            ) : (
              <div className="categories-chart">
                {categories.map((cat, index) => (
                  <div key={index} className="category-bar">
                    <div className="category-info">
                      <span className="category-name">{cat.category}</span>
                      <span className="category-stats">
                        {cat.count} שיחות ({cat.percentage.toFixed(1)}%)
                      </span>
                    </div>
                    <div className="progress-bar">
                      <div
                        className="progress-fill"
                        style={{
                          width: `${cat.percentage}%`,
                          background: `hsl(${220 - index * 20}, 70%, 60%)`
                        }}
                      ></div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Analytics;