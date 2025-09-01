import React, { useState } from 'react';
import './SearchForm.css';

const SearchForm = ({ onSearch }) => {
  const [mcc, setMcc] = useState('');
  const [mnc, setMnc] = useState('');
  const [lac, setLac] = useState('');
  const [cellId, setCellId] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    onSearch({ mcc, mnc, lac, cellId });
  };

  return (
    <div className="search-container">
      <div className="search-header">
        <input
          type="text"
          placeholder="Q Find a city (e.g. London)"
          className="city-search-input"
        />
      </div>
      <div className="search-form-panel">
        <h3 className="search-panel-title">Search Cell Towers</h3>
        <form onSubmit={handleSubmit} className="input-group-container">
          <div className="input-group">
            <label htmlFor="mcc">MCC</label>
            <input
              type="text"
              id="mcc"
              value={mcc}
              onChange={(e) => setMcc(e.target.value)}
              required
            />
          </div>
          <div className="input-group">
            <label htmlFor="mnc">MNC</label>
            <input
              type="text"
              id="mnc"
              value={mnc}
              onChange={(e) => setMnc(e.target.value)}
              required
            />
          </div>
          <div className="input-group">
            <label htmlFor="lac">LAC</label>
            <input
              type="text"
              id="lac"
              value={lac}
              onChange={(e) => setLac(e.target.value)}
              required
            />
          </div>
          <div className="input-group">
            <label htmlFor="cellid">Cell ID</label>
            <input
              type="text"
              id="cellid"
              value={cellId}
              onChange={(e) => setCellId(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="search-button">
            Search
          </button>
        </form>
      </div>
    </div>
  );
};

export default SearchForm;