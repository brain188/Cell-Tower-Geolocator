import React, { useState } from 'react';
import Header from '../components/Header.jsx';
import SearchForm from '../components/SearchForm.jsx';
import MapView from '../components/MapView.jsx';
import About from '../components/About.jsx';
import Footer from '../components/Footer.jsx';
import './Home.css';

const Home = () => {
  const [latitude, setLatitude] = useState(null);
  const [longitude, setLongitude] = useState(null);
  const [accuracy, setAccuracy] = useState(null);
  const [cellTowers, setCellTowers] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleSearch = async (request) => {
    if (loading) return;
    setLoading(true);
    console.log('Sending request:', request);
    try {
      const response = await fetch('http://localhost:8081/api/v1/resolve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      });
      console.log('Response status:', response.status);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      console.log('Full response data:', JSON.stringify(data, null, 2)); 
      const lat = data.latitude !== undefined ? data.latitude : null;
      const lon = data.longitude !== undefined ? data.longitude : null;

      if (!lat || !lon || (lat === null && lon === null)) {
        throw new Error('Invalid geolocation data from server');
      }
      setLatitude(lat);
      setLongitude(lon);
      setAccuracy(null);
      setCellTowers([{
        id: Date.now(),
        position: [lat, lon],
        type: 'green',
        popupText: `Cell Tower\nMCC: ${request.mcc}\nMNC: ${request.mnc}\nLAC: ${request.lac}\nCell ID: ${request.cellId}`
      }]);
    } catch (error) {
      console.error('Error fetching geolocation:', error);
      alert(error.message || 'An unexpected error occurred. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="home-page">
      <Header />
      <div className="main-content-home">
        <MapView latitude={latitude} longitude={longitude} accuracy={accuracy} cellTowers={cellTowers} />
        <SearchForm onSearch={handleSearch} />
      </div>
      <About />
      <Footer />
    </div>
  );
};

export default Home;