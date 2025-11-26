// frontend/src/components/Dashboard/ProgressBar.js
import React from 'react';

const ProgressBar = ({ progress, stage, fileName, fileSize }) => {
  const getStageColor = () => {
    if (progress === 100) return '#28a745';
    if (progress >= 65) return '#17a2b8';
    if (progress >= 40) return '#ffc107';
    return '#667eea';
  };

  const getStageIcon = () => {
    if (progress < 20) return '⬆️';
    if (progress < 40) return '📄';
    if (progress < 60) return '✂️';
    if (progress < 95) return '🧠';
    return '✅';
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '';
    if (bytes < 1024 * 1024) {
      return (bytes / 1024).toFixed(1) + ' KB';
    }
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  return (
    <div style={{
      background: 'white',
      border: '2px solid #e1e8ed',
      borderRadius: '12px',
      padding: '20px',
      marginBottom: '20px',
      boxShadow: '0 2px 8px rgba(0,0,0,0.05)'
    }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        marginBottom: '15px'
      }}>
        <div style={{ fontSize: '2rem' }}>
          {getStageIcon()}
        </div>
        <div style={{ flex: 1 }}>
          <div style={{
            fontWeight: 600,
            color: '#333',
            marginBottom: '4px',
            wordBreak: 'break-word'
          }}>
            {fileName}
          </div>
          <div style={{
            fontSize: '13px',
            color: '#666'
          }}>
            {fileSize && formatFileSize(fileSize)}
          </div>
        </div>
      </div>

      {/* Progress Bar */}
      <div style={{
        background: '#f0f0f0',
        borderRadius: '8px',
        height: '12px',
        overflow: 'hidden',
        marginBottom: '10px',
        position: 'relative'
      }}>
        <div style={{
          background: `linear-gradient(90deg, ${getStageColor()}, ${getStageColor()}dd)`,
          height: '100%',
          width: `${progress}%`,
          transition: 'width 0.3s ease-out',
          borderRadius: '8px',
          position: 'relative'
        }}>
          {/* Animated shine effect */}
          <div style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent)',
            animation: 'shine 1.5s infinite'
          }} />
        </div>
      </div>

      {/* Status Text */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        fontSize: '14px'
      }}>
        <span style={{ color: '#666', fontWeight: 500 }}>
          {stage}
        </span>
        <span style={{
          color: getStageColor(),
          fontWeight: 600
        }}>
          {progress}%
        </span>
      </div>

      <style>
        {`
          @keyframes shine {
            0% {
              transform: translateX(-100%);
            }
            100% {
              transform: translateX(100%);
            }
          }
        `}
      </style>
    </div>
  );
};

export default ProgressBar;