import React, { useState, useEffect } from 'react';
import './Analytics.css';
import { analyticsAPI } from '../../api/api';

/**
 * Analytics Component
 * 
 * Displays analytics data with two tabs:
 * 1. Questions Tab - Shows unanswered questions with counts
 * 2. Categories Tab - Shows topic distribution with chart
 */
function Analytics() {
  const [activeSubTab, setActiveSubTab] = useState('questions'); // 'questions' or 'categories'
  const [stats, setStats] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  // Load data on component mount
  useEffect(() => {
    loadData();
  }, []);

  // Load all analytics data
  const loadData = async () => {
    try {
      setLoading(true);
      
      const [statsData, questionsData, categoriesData] = await Promise.all([
        analyticsAPI.getStats(),
        analyticsAPI.getQuestions(),
        analyticsAPI.getCategories()
      ]);

      setStats(statsData.data);
      setQuestions(questionsData.data || []);
      setCategories(categoriesData.data || []);

    } catch (error) {
      console.error('Failed to load analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  // Download questions as Excel
  const handleDownloadExcel = async () => {
    try {
      const blob = await analyticsAPI.downloadQuestionsExcel();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'analytics-questions.xlsx';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to download Excel:', error);
      alert('שגיאה בהורדת הקובץ');
    }
  };

  // Clear all analytics
  const handleClearAll = async () => {
    if (!window.confirm('האם למחוק את כל נתוני האנליטיקס? פעולה זו בלתי הפיכה.')) {
      return;
    }

    try {
      await analyticsAPI.clearAll();
      await loadData(); // Reload
      alert('נתוני האנליטיקס נמחקו בהצלחה');
    } catch (error) {
      console.error('Failed to clear analytics:', error);
      alert('שגיאה במחיקת הנתונים');
    }
  };

  if (loading) {
    return (
      <div className="analytics-container">
        <div className="analytics-loading">
          <div className="spinner"></div>
          <p>טוען נתונים...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="analytics-container">
      {/* Header with gradient */}
      <div className="analytics-header">
        <div className="analytics-header-content">
          <h2>📊 אנליטיקס</h2>
          <p>ניתוח שאלות ונושאים מהצ'אט בוט</p>
        </div>
      </div>

      {/* Stats Summary Cards */}
      <div className="analytics-stats-cards">
        <div className="stats-card">
          <div className="stats-card-icon">❓</div>
          <div className="stats-card-content">
            <div className="stats-card-value">{stats?.totalQuestions || 0}</div>
            <div className="stats-card-label">שאלות נאספו</div>
          </div>
        </div>

        <div className="stats-card">
          <div className="stats-card-icon">✅</div>
          <div className="stats-card-content">
            <div className="stats-card-value">{stats?.uniqueQuestions || 0}</div>
            <div className="stats-card-label">שאלות ייחודיות</div>
          </div>
        </div>

        <div className="stats-card">
          <div className="stats-card-icon">🏷️</div>
          <div className="stats-card-content">
            <div className="stats-card-value">{stats?.uniqueCategories || 0}</div>
            <div className="stats-card-label">קטגוריות</div>
          </div>
        </div>
      </div>

      {/* Sub-Tabs Navigation */}
      <div className="analytics-sub-tabs">
        <button
          className={`analytics-sub-tab ${activeSubTab === 'questions' ? 'active' : ''}`}
          onClick={() => setActiveSubTab('questions')}
        >
          ❓ שאלות ללא מענה
        </button>
        <button
          className={`analytics-sub-tab ${activeSubTab === 'categories' ? 'active' : ''}`}
          onClick={() => setActiveSubTab('categories')}
        >
          🏷️ נושאים מרכזיים
        </button>
      </div>

      {/* Tab Content */}
      <div className="analytics-content">
        {activeSubTab === 'questions' && (
          <div className="analytics-questions">
            {/* Action Buttons */}
            <div className="analytics-actions">
              <button className="btn-download" onClick={handleDownloadExcel}>
                📥 הורד אקסל
              </button>
              <button className="btn-clear" onClick={handleClearAll}>
                🗑️ נקה הכל
              </button>
            </div>

            {/* Questions List */}
            {questions.length === 0 ? (
              <div className="analytics-empty">
                <div className="empty-icon">📭</div>
                <h3>אין שאלות עדיין</h3>
                <p>שאלות שלא קיבלו מענה יופיעו כאן</p>
                <div className="empty-hint">
                  💡 <strong>טיפ:</strong> שאלות נאספות רק כשהמשתמש סוגר את החלון או מתחיל שיחה חדשה
                </div>
              </div>
            ) : (
              <div className="questions-list">
                {questions.map((q, index) => (
                  <div key={index} className="question-card">
                    <div className="question-header">
                      <h3>{q.question}</h3>
                      <span className="question-count-badge">
                        {q.count} {q.count === 1 ? 'פעם' : 'פעמים'}
                      </span>
                    </div>
                    {q.examples && q.examples.length > 0 && (
                      <div className="question-examples">
                        <strong>דוגמאות לניסוחים:</strong>
                        <ul>
                          {q.examples.map((ex, i) => (
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

        {activeSubTab === 'categories' && (
          <div className="analytics-categories">
            {categories.length === 0 ? (
              <div className="analytics-empty">
                <div className="empty-icon">🏷️</div>
                <h3>אין קטגוריות עדיין</h3>
                <p>נושאי שיחה יופיעו כאן</p>
              </div>
            ) : (
              <div className="categories-chart">
                <h3>התפלגות נושאים</h3>
                {categories.map((cat, index) => (
                  <div key={index} className="category-bar">
                    <div className="category-info">
                      <span className="category-name">{cat.category}</span>
                      <span className="category-count">({cat.count})</span>
                    </div>
                    <div className="category-bar-container">
                      <div
                        className="category-bar-fill"
                        style={{
                          width: `${cat.percentage}%`,
                          backgroundColor: `hsl(${index * 40}, 70%, 60%)`
                        }}
                      >
                        <span className="category-percentage">{cat.percentage.toFixed(1)}%</span>
                      </div>
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
}

export default Analytics;