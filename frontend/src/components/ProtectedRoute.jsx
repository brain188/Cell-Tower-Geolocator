import React from 'react';
import { Navigate } from 'react-router-dom';
import { isAuthenticated } from '../utils/auth';

const ProtectedRoute = ({ children }) => {
  if (!isAuthenticated()) {
    alert("You must log in to access this page.");
    return <Navigate to="/login" replace />;
  }
  return children;
};

export default ProtectedRoute;