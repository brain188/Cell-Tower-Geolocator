import React, { useState } from 'react';
import './SearchForm.css';

const SearchForm = ({ onSearch, onCitySearch }) => {
  const [mcc, setMcc] = useState('');
  const [mnc, setMnc] = useState('');
  const [lac, setLac] = useState('');
  const [cellId, setCellId] = useState('');
  const [range, setRange] = useState('');
  const [city, setCity] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    onSearch({ 
      mcc, 
      mnc, 
      lac, 
      cellId,
      range: range ? Number(range) : null,
    });
  };

  const handleCityKeyPress = async (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (!city.trim()) return;

      try {
        const response = await fetch(
          `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(city)}`
        );
        const data = await response.json();
        if (data && data.length > 0) {
          const { lat, lon } = data[0];
          onCitySearch({ lat: parseFloat(lat), lon: parseFloat(lon), name: city });
        } else {
          alert('City not found');
        }
      } catch (err) {
        console.error('Error fetching city location:', err);
        alert('Error fetching city location');
      }
    }
  };

  return (
    <div className="search-container">
      <div className="search-header">
        <input
          type="text"
          placeholder=" Find a city (e.g. London)"
          className="city-search-input"
          value={city}
          onChange={(e) => setCity(e.target.value)}
          onKeyPress={handleCityKeyPress}
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
          <div className="input-group">
            <label htmlFor="range">Range (meters)</label>
            <input
              type="number"
              id="range"
              value={range}
              onChange={(e) => setRange(e.target.value)}
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
