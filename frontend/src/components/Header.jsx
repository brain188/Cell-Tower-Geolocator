// src/components/Header.jsx
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getUser, logout } from '../utils/auth';
import './Header.css';

const Header = () => {
  const navigate = useNavigate();
  const user = getUser();

  const handleLogout = () => {
    logout(navigate);
  };

  const getInitials = (username) => {
    return username ? username.slice(0, 2).toUpperCase() : '?';
  };

  return (
    <header className="header">
      <div className="header-left">
        <Link to="/home" className="cellgeolocator">CellGeolocator</Link>
      </div>
      <nav className="header-nav">
        {user ? (
          <>
            {/* <Link to="/home">HOME</Link> */}
            <div className="user-section">
              <div className="user-avatar">
                <span className="initials">{getInitials(user.username)}</span>
              </div>
              <button onClick={handleLogout} className="logout-btn">Logout</button>
            </div>
          </>
        ) : (
          <>
            <Link to="/login">LOGIN</Link>
            <Link to="/signup">SIGN UP</Link>
          </>
        )}
      </nav>
    </header>
  );
};

export default Header;