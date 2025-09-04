import React from 'react';
import './About.css';

const About = () => {
  return (
    <div className="about-container">
      <div className="about-content">
        <h2>What is CellGeolocator?</h2>
        <p>
          CellGeolocator resolves a mobile cell (MCC, MNC, LAC/TAC, Cell ID) to geographic
          coordinates by querying location providers (e.g. OpenCellID, Unwired Labs,
          Combain) and showing the best result on the map.
        </p>
      </div>
    </div>
  );
};

export default About;