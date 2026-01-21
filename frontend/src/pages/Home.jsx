import React, { useState } from "react";
import Header from "../components/Header.jsx";
import SearchForm from "../components/SearchForm.jsx";
import MapView from "../components/MapView.jsx";
import About from "../components/About.jsx";
import Footer from "../components/Footer.jsx";
import ResponseForm from "../components/ResponseForm.jsx";
import { showToast } from "../utils/toast.jsx";
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
  const [polygons, setPolygons] = useState([]);
  const [penetrationResult, setPenetrationResult] = useState(null); 

  const [range, setRange] = useState(null);

  const handleSearch = async (request) => {
    if (loading) return;
    setLoading(true);
    console.log("Sending request:", request);

    const token = localStorage.getItem('accessToken');

    setRange(request.range ?? null);

    try {
      const apiBase = import.meta.env.VITE_API_BASE;
      const response = await fetch(`${apiBase}/api/v1/geolocate/priority`, {
      // const apiBase = "http://127.0.0.1:8081";
      // const response = await fetch(`${apiBase}/api/v1/geolocate/priority`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "User-Agent": "cell-geolocation-project/1.0 (tendongbrain@gmail.com)",
          "Authorization": `Bearer ${token}`,
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

      if (lat === null || lon === null) {
        throw new Error("Invalid geolocation data from server");
      }
    

      setLatitude(lat);
      setLongitude(lon);
      setAccuracy(acc);
      setProviderUsed(result?.providerUsed ?? null);
      setAddress(result?.address ?? null);
      setAddressDetail(result?.addressDetail ?? null);

      const fullChosen = {
        ...result,
        originalRequestedCellId: result.originalRequestedCellId || request.cellId,
        cellId: result.cellId || request.cellId,
        fallbackUsed: result.fallbackUsed || false,
        relatedCells: result.relatedCells || [],
        technoCell: result.technoCell || 'U',
        frequenceCell: result.frequenceCell || 'N/A'
      };

      const searchMarker = {
        id: `search-${Date.now()}`,
        position: [lat, lon],
        mcc: request.mcc,
        mnc: request.mnc,
        lac: request.lac,
        cellId: fullChosen.cellId || request.cellId,
        originalRequestedCellId: fullChosen.originalRequestedCellId || request.cellId,
        fallbackUsed: fullChosen.fallbackUsed || false,
        technoCell: fullChosen.technoCell,
        frequenceCell: fullChosen.frequenceCell,
        popupText: `Searched Cell Tower\nMCC: ${request.mcc}\nMNC: ${request.mnc}\nLAC: ${request.lac}\nOriginal Requested: ${fullChosen.originalRequestedCellId}\nUsed Cell ID: ${fullChosen.cellId}${fullChosen.fallbackUsed ? ' (Fallback)' : ''}\nProvider: ${result.providerUsed ?? ""}`,
        range: request.range ?? null, 
      };

      const formattedRelated = relatedCells
        .filter(c => c.latitude != null && c.longitude != null)
        .map((c, i) => ({
          id: `related-${i}`,
          position: [parseFloat(c.latitude), parseFloat(c.longitude)],
          lac: c.lac,
          cellId: c.ci,
          btsId: c["Id BTS New"] || "N/A",
          technoCell: c.techno_cell || 'U',
          frequenceCell: c.frequence_cell || 'N/A',
          provider: result.providerUsed ?? "Unknown",
          address: c.address || "Unknown",
          range: request.range ?? null, 
          popupText: `Related Cell\nLAC: ${c.lac}\nCell ID: ${c.ci}\nBTS: ${c["Id BTS New"] || "N/A"}\nProvider: ${result.providerUsed ?? "Unknown"}\nAddress: ${c.address || "Unknown"}`
        }));

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
    if (!name.trim()) return;

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
        const backendArea = name; 

        // Set map center
        setLatitude(lat);
        setLongitude(lon);
        setAccuracy(null);
        setProviderUsed(null);
        setAddress(displayName);
        setAddressDetail(result.address || null);

        const apiBase = import.meta.env.VITE_API_BASE;
        const cellsResponse = await fetch(`${apiBase}/api/v1/cells/by-area?query=${encodeURIComponent(backendArea)}`,
        // const cellsResponse = await fetch(
        //   `http://localhost:8081/api/v1/cells/by-area?query=${encodeURIComponent(backendArea)}`,
          {
            method: 'GET',
            headers: {
              'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
            }
          }
        );

        if (cellsResponse.ok) {
          const cells = await cellsResponse.json();

          if (cells.length === 0) {
            showToast(`No cells found in ${backendArea}`, "info");
          } else {
            const areaMarkers = cells.map((c, i) => ({
              id: `area-cell-${i}-${Date.now()}`,
              position: [parseFloat(c.latitude), parseFloat(c.longitude)],
              technoCell: c.techno_cell || 'U',
              frequenceCell: c.frequence_cell || 'N/A',
              popupText: `Cell in ${backendArea}\nLAC: ${c.lac || 'N/A'}\nCell ID: ${c.ci || 'N/A'}\nSite: ${c.site_name || c.nomdusite || 'N/A'}\nBTS ID: ${c.bts_id || c["Id BTS New"] || 'N/A'}\nLocalite: ${c.localite || 'N/A'}\nQuartier: ${c.quartier || 'N/A'}\nRegion: ${c.region || 'N/A'}\nDept: ${c.departement || 'N/A'}\nFrequency: ${c.frequence_cell || 'N/A'}`
            }));

            setCellTowers(prev => [...prev, ...areaMarkers]);

            showToast(`Showing ${cells.length} cells in ${backendArea}`, "success");
          }
        } else {
          showToast("Failed to fetch cells for this area", "error");
        }

        // Only call penetration if range exists and is > 0 
        const numericRange = Number(range);
        if (!isNaN(numericRange) && numericRange > 0) {

          const coverageRes = await fetch(`${apiBase}/api/v1/coverage/penetration`, {
          // const coverageRes = await fetch('http://localhost:8081/api/v1/coverage/penetration', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
            },
            body: JSON.stringify({
              area: backendArea,
              radiusMeters: numericRange
            })
          });

          if (coverageRes.ok) {
            const coverage = await coverageRes.json();

            showToast(
              coverage.message,
              coverage.classification === "High" ? "success" :
              coverage.classification === "Medium" ? "info" : "warning"
            );

            // Draw polygons
            if (coverage.coveragePolygonsGeoJson?.length > 0) {
              const polygonMarkers = coverage.coveragePolygonsGeoJson.map((geoJson, i) => ({
                id: `coverage-poly-${i}-${Date.now()}`,
                geoJson: JSON.parse(geoJson),
                style: { color: 'blue', fillColor: 'blue', fillOpacity: 0.2 }
              }));
              setPolygons(prev => [...prev, ...polygonMarkers]);
            }

            // Show penetration result in modal
            setPenetrationResult(coverage);
          } else {
            showToast("Failed to calculate coverage penetration", "error");
          }
        }

      } else {
        showToast("Location not found. Try again.", "warning");
      }
    } catch (err) {
      console.error("Error fetching location or cells:", err);
      showToast("Error fetching data. Check connection.", "error");
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
          range={range} 
          polygons={polygons}
        />

        <SearchForm 
          onSearch={handleSearch} 
          onCitySearch={handleCitySearch}
          onRangeChange={setRange} 
        />

        <ResponseForm
          latitude={latitude}
          longitude={longitude}
          accuracy={accuracy}
          providerUsed={providerUsed}
          address={address}
          addressDetail={addressDetail}
        />
      </div>

      {penetrationResult && (
        <div className="penetration-modal" onClick={() => setPenetrationResult(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <button 
              className="close-btn" 
              onClick={() => setPenetrationResult(null)}
            >
              ×
            </button>
            <h3>Penetration Rate Result</h3>
            <div className="result-grid">
              <div><strong>Area:</strong> {penetrationResult.area || 'N/A'}</div>
              <div><strong>Radius:</strong> {penetrationResult.radiusMeters} m</div>
              <div><strong>Penetration Rate:</strong> {penetrationResult.penetrationRate?.toFixed(2)}%</div>
              <div><strong>Classification:</strong> {penetrationResult.classification}</div>
              <div><strong>Covered Area:</strong> {penetrationResult.coveredAreaKm2?.toFixed(2)} km²</div>
              <div><strong>Total Area:</strong> {penetrationResult.totalAreaKm2?.toFixed(2)} km²</div>
              <div className="full-width"><strong>Message:</strong> {penetrationResult.message}</div>
            </div>
          </div>
        </div>
      )}

      <About />
      <Footer />
    </div>
  );
};

export default Home;