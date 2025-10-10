# Cell Geolocation Project

Welcome to the **Cell Geolocation Project**, a comprehensive web application designed to resolve and visualize geolocation data based on cell tower information. This project integrates a robust **Spring Boot backend** for processing cell tower data (MCC, MNC, LAC, Cell ID) with a modern **React/Vite frontend** that displays results on an interactive map using **Leaflet**.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technologies Used](#technologies-used)
- [Prerequisites](#prerequisites)
- [Backend Setup](#backend-setup)
- [Frontend Setup](#frontend-setup)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)

---

## Overview

The Cell Geolocation Project leverages a backend API to fetch geolocation data from multiple external services such as **OpenCellID**, **Combain**, and **Unwired Labs**. The frontend provides an intuitive user interface for inputting cell tower parameters and visualizing the resolved latitude, longitude, and address on an interactive map.

This application is ideal for **developers, researchers, or enthusiasts** working with **cellular network geolocation**, offering features like user authentication, rate limiting, caching, and priority-based result aggregation.

---

## Features

- **Geolocation Resolution**: Resolve precise location from cell tower data (MCC, MNC, LAC, Cell ID).
- **Interactive Map Visualization**: Display results on a responsive map using **Leaflet** with customizable markers and popups.
- **Multi-Provider Aggregation**: Fetch data from multiple geolocation services and aggregate results for accuracy.
- **User Authentication**: Secure login and signup with JWT-based authentication.
- **Rate Limiting and Caching**: Prevent abuse and improve performance with configurable rate limits and caching.
- **Responsive Design**: Modern, mobile-friendly React frontend.
- **RESTful API**: Well-documented Spring Boot API with OpenAPI/Swagger integration.
- **Error Handling**: Comprehensive error handling for invalid inputs and service failures.
- **Request Logging**: Track and log API requests for monitoring and debugging.

---

## Technologies Used

### Backend

- **Java 17+**: Core programming language.
- **Spring Boot**: Framework for building the RESTful API.
- **Maven**: Build tool and dependency management.
- **Spring Security**: Authentication and authorization.
- **JWT**: Token-based authentication.
- **Spring Data JPA**: Database interactions (if applicable).
- **Caffeine**: Caching (configured in CacheConfig).
- **OpenAPI/Swagger**: API documentation.

### Frontend

- **React 18+**: UI library for building the user interface.
- **Vite**: Fast build tool and development server.
- **Leaflet**: Interactive map library.
- **Axios**: HTTP client for API calls.
- **CSS Modules**: Scoped styling for components.
- **ESLint**: Code linting.

### Other Tools

- **Git**: Version control.

---

## Prerequisites

Ensure you have the following installed on your system:

- **Java 17 or higher** (for the backend).
- **Maven 3.6 or higher** (for building the backend).
- **Node.js 18 or higher** and **npm 9 or higher** (for the frontend).
- **Git** (for cloning the repository).
- A **code editor** such as VS Code or IntelliJ IDEA.

---

## Backend Setup

### Backend Overview

The backend is a Spring Boot application that provides RESTful endpoints for geolocation services, user authentication, and request logging. It integrates with external geolocation providers and includes security features like JWT authentication and rate limiting.

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/your-username/cell-geolocation-project.git
   cd cell-geolocation-project/backend/cell_geolocator
   ```

2. Ensure Java and Maven are installed and configured.

### Running the Backend

1. Navigate to the backend directory:

   ```bash
   cd backend/cell_geolocator
   ```

2. Build the project:

   ```bash
   mvn clean install
   ```

3. Run the application:

   ```bash
   mvn spring-boot:run
   ```

   The backend will start on `http://localhost:8080` by default.

### Configuration

- **Application Properties**: Configure settings in `src/main/resources/application.properties` or `application-dev.properties`.
  - Database connection (if using JPA).
  - External API keys for geolocation providers (e.g., OpenCellID API key).
  - JWT secret and expiration.
- **Environment Variables**: Set sensitive data like API keys as environment variables.

---

## Frontend Setup

### Frontend Overview

The frontend is a React application built with Vite, providing a user-friendly interface for interacting with the geolocation API. It includes components for authentication, map visualization, and form inputs.

### Frontend Installation

1. Navigate to the frontend directory:

   ```bash
   cd frontend
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

### Running the Frontend

1. Start the development server:

   ```bash
   npm run dev
   ```

   The frontend will be available at `http://localhost:5173` (default Vite port).

2. Build for production:

   ```bash
   npm run build
   ```

3. Preview the production build:

   ```bash
   npm run preview
   ```

## API Endpoints

The backend provides the following key endpoints (accessible via Swagger at `http://localhost:8080/swagger-ui.html`):

- **Authentication**:
  - `POST /api/auth/login`: User login.
  - `POST /api/auth/signup`: User registration.

- **Geolocation**:
  - `POST /api/v1/geolocate`: Resolve location from cell tower data.

For detailed API documentation, refer to the OpenAPI/Swagger UI.

---

## Testing

### Integration Testing

- Ensure both backend and frontend are running, then test end-to-end functionality via the UI or API calls.

---

Thank you for using the Cell Geolocation Project! We hope it helps in your geolocation endeavors.
