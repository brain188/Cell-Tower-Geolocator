import React, { useState, useEffect } from 'react';
import './SearchForm.css';

const SearchForm = ({ onSearch, onCitySearch, onRangeChange }) => {
  const [mcc, setMcc] = useState('');
  const [mnc, setMnc] = useState('');
  const [lac, setLac] = useState('');
  const [cellId, setCellId] = useState('');
  const [range, setRange] = useState('');
  const [city, setCity] = useState(''); 
  const [suggestions, setSuggestions] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);

  const locationiqKey = import.meta.env.VITE_LOCATIONIQ_KEY;

  const handleRangeChange = (e) => {
    setRange(e.target.value);
    onRangeChange(e.target.value); 
  };

  // Autocomplete effect
  useEffect(() => {
    if (city.trim().length < 3) {
      setSuggestions([]);
      setShowDropdown(false);
      return;
    }

    const fetchSuggestions = async () => {
      try {
        const url = locationiqKey
          ? `https://api.locationiq.com/v1/autocomplete?key=${locationiqKey}&q=${encodeURIComponent(city)}&limit=5&format=json`
          : `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(city)}&limit=5`;

        const response = await fetch(url);
        const data = await response.json();
        setSuggestions(data);
        setShowDropdown(true);
      } catch (err) {
        console.error('Autocomplete error:', err);
      }
    };

    fetchSuggestions();
  }, [city, locationiqKey]);

  // Handle selecting a suggestion
  const handleSelectSuggestion = (suggestion) => {
    const placeName = suggestion.display_name || suggestion.display_place || suggestion.name || city;
    setCity(placeName);
    setShowDropdown(false);
    onCitySearch({ name: placeName, radius: Number(range) }); 
  };

  const handleCityKeyPress = async (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (!city.trim()) return;

      onCitySearch({ name: city, radius: Number(range) });
      setShowDropdown(false);
    }
  };

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

  return (
    <div className="search-container">
      {/* Area/City Search with Icon + Dropdown */}
      <div className="search-header">
        <div className="city-search-wrapper">
          <input
            type="text"
            placeholder=" Find a city (e.g. London)"
            className="city-search-input"
            value={city}
            onChange={(e) => setCity(e.target.value)}
            onKeyPress={handleCityKeyPress}
            onFocus={() => suggestions.length > 0 && setShowDropdown(true)}
          />
          <span className="search-icon"></span>

          {showDropdown && suggestions.length > 0 && (
            <ul className="suggestions-dropdown">
              {suggestions.map((sug, i) => (
                <li key={i} onClick={() => handleSelectSuggestion(sug)}>
                  {sug.display_name || sug.display_place || sug.name}
                </li>
              ))}
            </ul>
          )}
        </div>
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
              onChange={handleRangeChange}
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