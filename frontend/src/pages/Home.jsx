import React, { useState } from 'react';
import Header from '../components/Header.jsx';
import SearchForm from '../components/SearchForm.jsx';
import MapView from '../components/MapView.jsx';
import About from '../components/About.jsx';
import Footer from '../components/Footer.jsx';
import ResponseForm from '../components/ResponseForm.jsx';
import './Home.css';

const Home = () => {
  const [latitude, setLatitude] = useState(null);
  const [longitude, setLongitude] = useState(null);
  const [accuracy, setAccuracy] = useState(null);
  const [cellTowers, setCellTowers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [cityMarker, setCityMarker] = useState(null);
  const [providerUsed, setProviderUsed] = useState(null);
  const [address, setAddress] = useState(null);
  const [addressDetail, setAddressDetail] = useState(null);

  const handleSearch = async (request) => {
  if (loading) return;
  setLoading(true);
  console.log('Sending request:', request);

  try {
    const response = await fetch('http://localhost:8081/api/v1/geolocate/priority', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'User-Agent': 'cell-geolocation-project/1.0 (tendongbrain@gmail.com)'
      },
      body: JSON.stringify(request),
    });

    console.log('Response status:', response.status);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    console.log('Full response data:', JSON.stringify(data, null, 2));

    //  extract chosen if it exists
    const result = data.chosen ? data.chosen : data;

    const lat = result.latitude !== undefined ? result.latitude : null;
    const lon = result.longitude !== undefined ? result.longitude : null;

    if (!lat || !lon) {
      throw new Error('Invalid geolocation data from server');
    }

    setLatitude(lat);
    setLongitude(lon);
    setAccuracy(null);
    setProviderUsed(result.providerUsed || null);
    setAddress(result.address || null);
    setAddressDetail(result.addressDetail || null);

    setCellTowers([{
      id: Date.now(),
      position: [lat, lon],
      type: 'green',
      popupText: `Cell Tower\nMCC: ${request.mcc}\nMNC: ${request.mnc}\nLAC: ${request.lac}\nCell ID: ${request.cellId}`
    }]);

  } catch (error) {
    console.error('Error fetching geolocation:', error);
    alert(error.message || 'An unexpected error occurred. Please try again later.');
    setLatitude(null);
    setLongitude(null);
    setProviderUsed(null);
    setAddress(null);
    setAddressDetail(null);
  } finally {
    setLoading(false);
  }
};


  const handleCitySearch = async ({ lat, lon, name }) => {
    try {
      const response = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(name)}&addressdetails=1`
      );
      const data = await response.json();
      if (data && data.length > 0) {
        const result = data[0];
        let popupText;
        if (result.type === 'country') {
          popupText = `Country: ${result.address.country}`;
        } else {
          const cityOrTown = result.address.city || result.address.town || name;
          const capital = (result.address.country_code === 'gb' && cityOrTown.toLowerCase() === 'london') ? 'London' :
                         (result.address.country_code === 'fr' && cityOrTown.toLowerCase() === 'paris') ? 'Paris' : null;
          popupText = `City: ${cityOrTown}\n${capital ? `Capital: ${capital}\n` : ''}Country: ${result.address.country}`;
        }
        setCityMarker({
          id: Date.now(),
          position: [parseFloat(lat), parseFloat(lon)],
          popupText: popupText
        });
        setLatitude(parseFloat(lat));
        setLongitude(parseFloat(lon));
        setProviderUsed(null);
        setAddress(null);
        setAddressDetail(null);
      } else {
        alert('Location not found');
      }
    } catch (err) {
      console.error('Error fetching location details:', err);
      alert('Error fetching location details');
    }
  };

  return (
    <div className="home-page">
      <Header />
      <div className="main-content-home">
        <MapView 
          latitude={latitude} 
          longitude={longitude} 
          accuracy={accuracy} 
          cellTowers={cellTowers} 
          cityMarker={cityMarker}
        />
        <SearchForm onSearch={handleSearch} onCitySearch={handleCitySearch} />
        <ResponseForm 
          latitude={latitude} 
          longitude={longitude} 
          providerUsed={providerUsed} 
          address={address} 
          addressDetail={addressDetail} 
        />
      </div>
      <About />
      <Footer />
    </div>
  );
};

export default Home;