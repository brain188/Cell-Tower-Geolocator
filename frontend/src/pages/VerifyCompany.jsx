import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Header.jsx';
import { showToast } from '../utils/toast.jsx';
import './Auth.css';

const VerifyCompany = () => {
  const [formData, setFormData] = useState({ companyName: '', companyDepartment: '' });
  const [errors, setErrors] = useState({});
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');

  const validate = () => {
    const newErrors = {};
    if (!formData.companyName.trim()) newErrors.companyName = 'Company name is required';
    if (!formData.companyDepartment.trim()) newErrors.companyDepartment = 'Department is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      // const response = await fetch('http://localhost:8081/api/v1/auth/verify-company', {
      const apiBase = import.meta.env.VITE_API_BASE;

      const response = await fetch(`${apiBase}/api/v1/auth/verify-company`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        showToast('Verification successful!');
        navigate('/home');
      } else {
        const error = await response.json();
        showToast(error.message || 'Verification failed');
      }
    } catch (err) {
      console.error(err);
      showToast('Network error. Please try again.');
    }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: '' });
  };

  return (
    <div className="auth-page">
      <Header />
      <div className="auth-container">
        <h2>Verify Company Information</h2>
        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Company Name</label>
            <input
              type="text"
              name="companyName"
              value={formData.companyName}
              onChange={handleChange}
              placeholder="Enter company name"
            />
            {errors.companyName && <span className="error">{errors.companyName}</span>}
          </div>

          <div className="form-group">
            <label>Company Department</label>
            <input
              type="text"
              name="companyDepartment"
              value={formData.companyDepartment}
              onChange={handleChange}
              placeholder="Enter department"
            />
            {errors.companyDepartment && <span className="error">{errors.companyDepartment}</span>}
          </div>

          <button type="submit" className="auth-button">Verify</button>
        </form>
      </div>
    </div>
  );
};

export default VerifyCompany;