import React from 'react';
import { MapContainer, TileLayer, Marker, Popup, ZoomControl, Circle } from 'react-leaflet';
import MarkerClusterGroup from 'react-leaflet-cluster';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet.markercluster/dist/MarkerCluster.css';
import './MapView.css';

// Fix missing default icons
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// Blue icon for requested cell
const blueIcon = L.icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

// Orange icon for related cells
const orangeIcon = L.icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-orange.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

const MapView = ({ latitude, longitude, accuracy, providerUsed, cellTowers, cityMarker }) => {
  const position = latitude && longitude ? [latitude, longitude] : [4.0511, 9.7679]; // Cameroon default
  const mapKey = latitude && longitude ? `${latitude}-${longitude}` : `default-${Date.now()}`;
  const zoom = latitude && longitude ? 14 : 6;

  // Separate requested cell from related ones
  const chosen = cellTowers.find(t => t.id.startsWith('search-'));
  const relatedCells = cellTowers.filter(t => !t.id.startsWith('search-'));

  return (
    <MapContainer
      key={mapKey}
      center={position}
      zoom={zoom}
      scrollWheelZoom={true}
      className="map-container-view-inner"
      zoomControl={false}
    >
      {/* Base map (OpenStreetMap) */}
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {/* Overlay map (LocationIQ streets, faint for detail) */}
      {import.meta.env.VITE_LOCATIONIQ_KEY && (
        <TileLayer
          attribution='&copy; <a href="https://locationiq.com/">LocationIQ</a>'
          url={`https://tiles.locationiq.com/v3/streets/{z}/{x}/{y}.png?key=${import.meta.env.VITE_LOCATIONIQ_KEY}`}
          opacity={0.6}
        />
      )}

      <ZoomControl position="topright" />

      {/* REQUESTED CELL – BLUE MARKER + COVERAGE CIRCLE */}
      {chosen && (
        <>
          {accuracy && (
            <Circle
              center={chosen.position}
              radius={accuracy}
              pathOptions={{
                color: '#3388ff',
                fillColor: '#3388ff',
                fillOpacity: 0.2,
                weight: 2,
              }}
            />
          )}
          <Marker position={chosen.position} icon={blueIcon}>
            <Popup>
              <strong>Requested Cell</strong><br />
              MCC: {chosen.mcc}<br />
              MNC: {chosen.mnc}<br />
              LAC: {chosen.lac}<br />
              Cell ID: {chosen.cellId}<br />
              Provider: {providerUsed || 'Unknown'}
            </Popup>
          </Marker>
        </>
      )}

      {/* RELATED CELLS – ORANGE MARKERS + CLUSTERING */}
      {relatedCells.length > 0 && (
        <MarkerClusterGroup
          chunkedLoading
          showCoverageOnHover={false}
          spiderfyOnMaxZoom={true}
          zoomToBoundsOnClick={true}
        >
          {relatedCells.map((tower) => (
            <Marker key={tower.id} position={tower.position} icon={orangeIcon}>
              <Popup>{tower.popupText}</Popup>
            </Marker>
          ))}
        </MarkerClusterGroup>
      )}

      {/* City marker */}
      {cityMarker && (
        <Marker key={cityMarker.id} position={cityMarker.position} icon={L.Icon.Default.prototype.options}>
          <Popup>{cityMarker.popupText}</Popup>
        </Marker>
      )}
    </MapContainer>
  );
};

export default MapView;