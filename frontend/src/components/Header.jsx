
import React from 'react';
import { Link } from 'react-router-dom';
import './Header.css';

const Header = () => {
  return (
    <header className="header">
      <div className="header-left">
        <span className="cellgeolocator">CellGeolocator</span>
      </div>
      <nav className="header-nav">
        <Link to="/home">HOME</Link>
        <Link to="/login">LOGIN</Link>
        <Link to="/signup">SIGN UP</Link>
      </nav>
    </header>
  );
};

export default Header;