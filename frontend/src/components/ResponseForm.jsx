import React, { useState } from 'react';
import './ResponseForm.css';

const ResponseForm = ({ latitude, longitude, accuracy, providerUsed, address, addressDetail }) => {
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);

  return (
    <div className="response-container">
      <h3 className="response-title">Geolocation Response</h3>
      <div className="response-details">
        <div className="response-item">
          <span className="label">Latitude:</span>
          <span className="value">{latitude !== null ? latitude.toFixed(4) : 'N/A'}</span>
        </div>
        <div className="response-item">
          <span className="label">Longitude:</span>
          <span className="value">{longitude !== null ? longitude.toFixed(4) : 'N/A'}</span>
        </div>
        <div className="response-item">
          <span className="label">Accuracy (meters):</span>
          <span className="value">{accuracy ? `${accuracy.toFixed(0)} m` : 'N/A'}</span>
        </div>
        <div className="response-item">
          <span className="label">Provider Used:</span>
          <span className="value">{providerUsed || 'N/A'}</span>
        </div>
        <div className="response-item">
          <span className="label">Address:</span>
          <span className="value">{address || 'N/A'}</span>
        </div>

        {addressDetail && (
          <div className="response-item">
            <div
              className="details-toggle"
              onClick={() => setIsDetailsOpen(!isDetailsOpen)}
            >
              <span className="label">Address Details</span>
              <span className={`toggle-arrow ${isDetailsOpen ? 'open' : ''}`}>
                ▼
              </span>
            </div>
            {isDetailsOpen && (
              <div className="address-details-content">
                <div className="response-item">
                  <span className="label">Country:</span>
                  <span className="value">{addressDetail.country || 'N/A'}</span>
                </div>
                <div className="response-item">
                  <span className="label">Country Code:</span>
                  <span className="value">{addressDetail.countryCode || 'N/A'}</span>
                </div>
                <div className="response-item">
                  <span className="label">City:</span>
                  <span className="value">{addressDetail.cityOrTown || 'N/A'}</span>
                </div>
                <div className="response-item">
                  <span className="label">State/Region:</span>
                  <span className="value">{addressDetail.stateOrRegion || 'N/A'}</span>
                </div>
                <div className="response-item">
                  <span className="label">Postal Code:</span>
                  <span className="value">{addressDetail.postalCode || 'N/A'}</span>
                </div>
                <div className="response-item">
                  <span className="label">Street:</span>
                  <span className="value">{addressDetail.street || 'N/A'}</span>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default ResponseForm;