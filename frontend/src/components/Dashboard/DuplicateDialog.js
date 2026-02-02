import React from 'react';

/**
 * Dialog component shown when user tries to upload a duplicate file
 * Offers 3 options: Replace, Save as new, Cancel
 */
const DuplicateDialog = ({ 
  fileName,        // Name of the duplicate file
  suggestedName,   // Suggested unique name (e.g., "file (1).pdf")
  onReplace,       // Callback for "Replace" button
  onRename,        // Callback for "Save as new" button
  onCancel         // Callback for "Cancel" button
}) => {
  return (
    // Overlay background
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0, 0, 0, 0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 9999
    }}>
      {/* Dialog box */}
      <div style={{
        background: 'white',
        borderRadius: '12px',
        padding: '30px',
        maxWidth: '500px',
        width: '90%',
        boxShadow: '0 10px 40px rgba(0, 0, 0, 0.3)'
      }}>
        {/* Title */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          marginBottom: '20px'
        }}>
          <span style={{ fontSize: '2rem' }}>⚠️</span>
          <h3 style={{ 
            margin: 0, 
            fontSize: '20px',
            color: '#333'
          }}>
            File with this name already exists
          </h3>
        </div>

        {/* Message */}
        <p style={{ 
          color: '#666', 
          marginBottom: '25px',
          lineHeight: '1.6'
        }}>
          A file named <strong>{fileName}</strong> already exists.
          <br />
          How would you like to proceed?
        </p>

        {/* Buttons */}
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '10px'
        }}>
          {/* Replace Button */}
          <button
            onClick={onReplace}
            style={{
              padding: '14px 24px',
              background: 'linear-gradient(135deg, #f44336 0%, #d32f2f 100%)',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              fontWeight: 600,
              cursor: 'pointer',
              fontSize: '16px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px',
              transition: 'all 0.2s'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'translateY(-2px)';
              e.currentTarget.style.boxShadow = '0 4px 12px rgba(244, 67, 54, 0.3)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'translateY(0)';
              e.currentTarget.style.boxShadow = 'none';
            }}
          >
            <span>🔄</span>
            <span>Replace existing file</span>
          </button>

          {/* Save as new Button */}
          <button
            onClick={onRename}
            style={{
              padding: '14px 24px',
              background: 'linear-gradient(135deg, #4CAF50 0%, #388E3C 100%)',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              fontWeight: 600,
              cursor: 'pointer',
              fontSize: '16px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px',
              transition: 'all 0.2s'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'translateY(-2px)';
              e.currentTarget.style.boxShadow = '0 4px 12px rgba(76, 175, 80, 0.3)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'translateY(0)';
              e.currentTarget.style.boxShadow = 'none';
            }}
          >
            <div style={{ 
              display: 'flex', 
              alignItems: 'center', 
              gap: '10px' 
            }}>
              <span>➕</span>
              <span>Save as new file</span>
            </div>
            <div style={{ 
              fontSize: '13px', 
              opacity: 0.9,
              fontWeight: 400
            }}>
              ({suggestedName})
            </div>
          </button>

          {/* Cancel Button */}
          <button
            onClick={onCancel}
            style={{
              padding: '14px 24px',
              background: '#f5f5f5',
              color: '#666',
              border: '1px solid #ddd',
              borderRadius: '8px',
              fontWeight: 600,
              cursor: 'pointer',
              fontSize: '16px',
              transition: 'all 0.2s'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#e0e0e0';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = '#f5f5f5';
            }}
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
};

export default DuplicateDialog;