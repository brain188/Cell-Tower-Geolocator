import React, { useState } from "react";
import Header from "../components/Header.jsx";
import SearchForm from "../components/SearchForm.jsx";
import MapView from "../components/MapView.jsx";
import About from "../components/About.jsx";
import Footer from "../components/Footer.jsx";
import ResponseForm from "../components/ResponseForm.jsx";
import "./Home.css";

const Home = () => {
  const [latitude, setLatitude] = useState(null);
  const [longitude, setLongitude] = useState(null);
  const [accuracy, setAccuracy] = useState(null);
  const [cellTowers, setCellTowers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [providerUsed, setProviderUsed] = useState(null);
  const [address, setAddress] = useState(null);
  const [addressDetail, setAddressDetail] = useState(null);
  const [cityMarker, setCityMarker] = useState(null);

  
  const [range, setRange] = useState(null);

  const handleSearch = async (request) => {
    if (loading) return;
    setLoading(true);
    console.log("Sending request:", request);

    setRange(request.range ?? null);

    try {
      // const apiBase = import.meta.env.VITE_API_BASE;
      // const response = await fetch(`${apiBase}/api/v1/geolocate/priority`, {
      const apiBase = "http://127.0.0.1:8081";

      const response = await fetch(`${apiBase}/api/v1/geolocate/priority`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "User-Agent": "cell-geolocation-project/1.0 (tendongbrain@gmail.com)",
        },
        body: JSON.stringify(request),
      });

      console.log("Response status:", response.status);
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

      const data = await response.json();
      console.log("Full response data:", JSON.stringify(data, null, 2));

      // Adapt to priority structure
      const result = data?.priorityResults?.chosen ?? data?.priorityResult ?? data;
      const relatedCells = data?.relatedCells || [];

      const lat = result?.latitude ?? null;
      const lon = result?.longitude ?? null;
      const acc = result?.accuracy ?? null;

      // Validate coordinates
      if (lat === null || lon === null) {
        throw new Error("Invalid geolocation data from server");
      }

      setLatitude(lat);
      setLongitude(lon);
      setAccuracy(acc);
      setProviderUsed(result?.providerUsed ?? null);
      setAddress(result?.address ?? null);
      setAddressDetail(result?.addressDetail ?? null);

      // REQUESTED CELL MARKER
      const searchMarker = {
        id: `search-${Date.now()}`,
        position: [lat, lon],
        mcc: request.mcc,
        mnc: request.mnc,
        lac: request.lac,
        cellId: request.cellId,
        popupText: `Searched Cell Tower\nMCC: ${request.mcc}\nMNC: ${request.mnc}\nLAC: ${request.lac}\nCell ID: ${request.cellId}\nProvider: ${result.providerUsed ?? ""}`,
        range: request.range ?? null, 
      };

      // RELATED CELLS MARKERS
      const formattedRelated = relatedCells
        .filter(c => c.latitude != null && c.longitude != null)
        .map((c, i) => ({
          id: `related-${i}`,
          position: [parseFloat(c.latitude), parseFloat(c.longitude)],
          lac: c.lac,
          cellId: c.ci,
          btsId: c["Id BTS New"] || "N/A",
          provider: result.providerUsed ?? "Unknown",
          address: c.address || "Unknown",
          range: request.range ?? null, 
          popupText: `Related Cell\nLAC: ${c.lac}\nCell ID: ${c.ci}\nBTS: ${c["Id BTS New"] || "N/A"}\nProvider: ${result.providerUsed ?? "Unknown"}\nAddress: ${c.address || "Unknown"}`
        }));

      // SET ALL MARKERS
      setCellTowers([searchMarker, ...formattedRelated]);

    } catch (error) {
      console.error("Error fetching geolocation:", error);
      alert(error.message || "Unexpected error. Try again.");
      setLatitude(null);
      setLongitude(null);
      setAccuracy(null);
      setProviderUsed(null);
      setAddress(null);
      setAddressDetail(null);
      setCellTowers([]);
      setRange(null); 
    } finally {
      setLoading(false);
    }
  };

  const handleCitySearch = async ({ name }) => {
    try {
      const locationiqKey = import.meta.env.VITE_LOCATIONIQ_KEY;
      let response;

      if (locationiqKey) {
        response = await fetch(
          `https://us1.locationiq.com/v1/search?key=${locationiqKey}&q=${encodeURIComponent(name)}&format=json&addressdetails=1`
        );
      } else {
        response = await fetch(
          `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(name)}&addressdetails=1`
        );
      }

      const data = await response.json();
      if (data && data.length > 0) {
        const result = data[0];
        const lat = parseFloat(result.lat);
        const lon = parseFloat(result.lon);
        const displayName = result.display_name || name;

        setCityMarker({
          id: Date.now(),
          position: [lat, lon],
          popupText: displayName,
        });

        setLatitude(lat);
        setLongitude(lon);
        setAccuracy(null);
        setProviderUsed(null);
        setAddress(displayName);
        setAddressDetail(result.address || null);
      } else {
        alert("City not found. Try again.");
      }
    } catch (err) {
      console.error("Error fetching location:", err);
      alert("Error fetching location data.");
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
          providerUsed={providerUsed}
          cellTowers={cellTowers}
          cityMarker={cityMarker}
          range={range} 
        />
        <SearchForm onSearch={handleSearch} onCitySearch={handleCitySearch} />
        <ResponseForm
          latitude={latitude}
          longitude={longitude}
          accuracy={accuracy}
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
