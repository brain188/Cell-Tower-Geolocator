import React from 'react';
import { MapContainer, TileLayer, Marker, Popup, ZoomControl } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import './MapView.css';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

const defaultIcon = new L.Icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

const MapView = ({ latitude, longitude, accuracy, cellTowers, cityMarker }) => {
  console.log('MapView props:', { latitude, longitude, accuracy, cellTowers, cityMarker });
  const position = latitude && longitude ? [latitude, longitude] : [37.77, -122.42];
  const mapKey = latitude && longitude ? `${latitude}-${longitude}` : `default-${Date.now()}`;
  const zoom = 14;

  return (
    <MapContainer key={mapKey} center={position} zoom={zoom} scrollWheelZoom={false} className="map-container-view" zoomControl={false}>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <ZoomControl position="topright" />
      {Array.isArray(cellTowers) && cellTowers.map((tower) => (
        <Marker key={tower.id} position={tower.position} icon={defaultIcon}>
          <Popup>{tower.popupText}</Popup>
        </Marker>
      ))}
      {cityMarker && (
        <Marker key={cityMarker.id} position={cityMarker.position} icon={defaultIcon}>
          <Popup>{cityMarker.popupText}</Popup>
        </Marker>
      )}
      {latitude && longitude && (
        <Marker position={[latitude, longitude]} icon={defaultIcon}>
          <Popup>
            Latitude: {latitude}<br />
            Longitude: {longitude}<br />
            Provider: OpenCellID
          </Popup>
        </Marker>
      )}
    </MapContainer>
  );
};

export default MapView;
