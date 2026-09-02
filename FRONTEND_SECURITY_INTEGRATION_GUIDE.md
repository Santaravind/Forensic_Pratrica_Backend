# Frontend Security & Authentication Integration Specification
> **Target Audience**: AI Agents / LLMs and Frontend Developers implementing or refactoring authentication in a React application.
> **Backend Stack**: Spring Boot 4 / Spring Security (Stateless JWT) + PostgreSQL + Resend Email API.

---

## 1. System Overview & Security Architecture

### Core Security Rules
1. **Stateless JWT Authentication**: Once authenticated via `/auth/login`, all protected backend endpoints require a standard HTTP header:
   ```http
   Authorization: Bearer <JWT_TOKEN>
   ```
2. **Mandatory Email OTP Verification**:
   - Accounts are created in an unverified state (`verified: false`).
   - Unverified accounts cannot log in (backend rejects with `400/401` message: *"Account is not verified. Please verify the OTP sent to your email."*).
   - An OTP is a 6-digit numerical code valid for **10 minutes**.
3. **CORS & Credentials**:
   - Backend allows origins: `http://localhost:5173`,`https://www.forensicpatrika.com`.
   - Headers allowed: `Authorization`, `Content-Type`, `*`.
   - Methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`.

---

## 2. API Endpoints Contract

### Base URL
- **Local Development**: `http://localhost:8080`
- **Production**: Configured via frontend `.env` (`VITE_API_BASE_URL` or `REACT_APP_API_BASE_URL`)

---

### Endpoint 1: Register User
*Creates an unverified account and sends a 6-digit OTP email.*

- **URL**: `/auth/register`
- **Method**: `POST`
- **Headers**: `Content-Type: application/json`
- **Request Payload**:
  ```typescript
  interface RegisterRequest {
    fullName: string;      // required
    organization: string;  // required
    domain: string;        // required
    password: string;      // required
    mobileNo: string;      // required, numeric string, unique
    email: string;         // required, valid email format, unique
    role: string;          // required, e.g. "USER" or "ADMIN"
  }
  ```
- **Example Request Body**:
  ```json
  {
    "fullName": "Jane Doe",
    "organization": "Cyber Security Corp",
    "domain": "Digital Forensics",
    "password": "SecurePassword123!",
    "mobileNo": "9876543210",
    "email": "jane.doe@example.com",
    "role": "USER"
  }
  ```
- **Success Response (`201 Created`)**:
  ```json
  {
    "success": true,
    "message": "OTP sent to your email. Please verify your OTP to complete registration."
  }
  ```
- **Error Response (`400 Bad Request`)**:
  ```json
  {
    "message": "Email already registered and verified. Please log in."
    // OR "Mobile number already registered"
    // OR Validation error details
  }
  ```

---

### Endpoint 2: Verify Registration OTP
*Validates the 6-digit code and marks the account as `verified: true`.*

- **URL**: `/auth/verify-otp`
- **Method**: `POST`
- **Request Payload**:
  ```typescript
  interface VerifyOtpRequest {
    email: string;  // required, valid email
    otp: string;    // required, 6-digit string (e.g. "123456")
  }
  ```
- **Example Request Body**:
  ```json
  {
    "email": "jane.doe@example.com",
    "otp": "265832"
  }
  ```
- **Success Response (`200 OK`)**:
  ```json
  {
    "success": true,
    "message": "Email verified successfully. Registration is complete! You can now log in."
  }
  ```
- **Error Response (`400 Bad Request`)**:
  ```json
  {
    "message": "Invalid OTP" 
    // OR "OTP expired" 
    // OR "No account found with email: ..."
  }
  ```

---

### Endpoint 3: Resend OTP
*Generates and sends a fresh 6-digit OTP if previous expired or was not received.*

- **URL**: `/auth/resend-otp`
- **Method**: `POST`
- **Request Payload**:
  ```typescript
  interface ResendOtpRequest {
    email: string; // required, valid email
  }
  ```
- **Example Request Body**:
  ```json
  {
    "email": "jane.doe@example.com"
  }
  ```
- **Success Response (`200 OK`)**:
  ```json
  {
    "success": true,
    "message": "A new OTP has been sent to your email."
  }
  ```
- **Error Response (`400 Bad Request`)**:
  ```json
  {
    "message": "Account is already verified. You can log in directly."
  }
  ```

---

### Endpoint 4: User Login
*Authenticates verified users and issues a JWT token.*

- **URL**: `/auth/login`
- **Method**: `POST`
- **Request Payload**:
  ```typescript
  interface LoginRequest {
    email: string;     // required
    password: string;  // required
    role: string;      // required, must match account role ("USER" or "ADMIN")
  }
  ```
- **Example Request Body**:
  ```json
  {
    "email": "jane.doe@example.com",
    "password": "SecurePassword123!",
    "role": "USER"
  }
  ```
- **Success Response (`200 OK`)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```
- **Error Response (`400 Bad Request` / `401 Unauthorized`)**:
  ```json
  {
    "message": "Invalid credentials"
    // OR "Account is not verified. Please verify the OTP sent to your email."
    // OR "Role mismatch for this account"
  }
  ```

---

### Endpoint 5: Reset Password
*Updates the password for an existing account.*

- **URL**: `/auth/reset-password`
- **Method**: `POST`
- **Request Payload**:
  ```typescript
  interface ResetPasswordRequest {
    email: string;        // required
    newPassword: string;  // required
  }
  ```
- **Example Request Body**:
  ```json
  {
    "email": "jane.doe@example.com",
    "newPassword": "NewSecurePassword789!"
  }
  ```
- **Success Response (`200 OK`)**:
  ```json
  {
    "success": true,
    "message": "Password reset successfully"
  }
  ```

---

## 3. Frontend Architecture & Workflow Blueprints

### Flow 1: Complete Registration & Verification Workflow
```text
[ Registration Page ]
        │
        ├──> (User submits registration form)
        ├──> Calls POST /auth/register
        │
        ├──> On Success (201):
        │       ├── Store 'pending_verification_email' in state/sessionStorage
        │       └── Navigate to [ OTP Verification Page ]
        │
[ OTP Verification Page ]
        │
        ├──> Displays recipient email (e.g. "Sent to jane.doe@example.com")
        ├──> 10-Minute Countdown Timer (600s)
        ├──> 6-Digit input field with auto-focus
        │
        ├──> User clicks "Verify":
        │       └── Calls POST /auth/verify-otp { email, otp }
        │       └── On Success (200):
        │             ├── Clear 'pending_verification_email'
        │             ├── Show success toast
        │             └── Navigate to [ Login Page ]
        │
        └──> User clicks "Resend OTP":
                └── Calls POST /auth/resend-otp { email }
                └── Reset countdown timer to 10 minutes
                └── Disable Resend button for 60s cooldown
```

### Flow 2: Login & Unverified Account Handling
```text
[ Login Page ]
        │
        ├──> (User submits email, password, role)
        ├──> Calls POST /auth/login
        │
        ├──> Success (200):
        │       ├── Store JWT in localStorage / secure cookie / AuthContext
        │       └── Navigate to Dashboard or intended route
        │
        └──> Failure:
                ├── If error message includes "not verified":
                │       ├── Store email in state
                │       └── Prompt user: "Your email is unverified. [Verify Now]"
                │       └── Redirects to OTP verification screen with option to resend.
                └── Otherwise:
                        └── Display "Invalid credentials" or "Role mismatch".
```

---

## 4. Ready-to-Use React Implementation Blueprints

### 1. Centralized API Client (`src/api/apiClient.ts` / `.js`)
```typescript
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Automatic JWT Bearer token injection for all protected routes
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Global response interceptor for 401 handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear token and redirect to login if JWT expired
      localStorage.removeItem('jwt_token');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

---

### 2. Authentication Service (`src/services/authService.ts` / `.js`)
```typescript
import { apiClient } from '../api/apiClient';

export interface RegisterPayload {
  fullName: string;
  organization: string;
  domain: string;
  password: string;
  mobileNo: string;
  email: string;
  role: string;
}

export interface LoginPayload {
  email: string;
  password: string;
  role: string;
}

export const authService = {
  register: async (data: RegisterPayload) => {
    const res = await apiClient.post('/auth/register', data);
    return res.data;
  },

  verifyOtp: async (email: string, otp: string) => {
    const res = await apiClient.post('/auth/verify-otp', { email, otp });
    return res.data;
  },

  resendOtp: async (email: string) => {
    const res = await apiClient.post('/auth/resend-otp', { email });
    return res.data;
  },

  login: async (credentials: LoginPayload) => {
    const res = await apiClient.post<{ token: string }>('/auth/login', credentials);
    if (res.data?.token) {
      localStorage.setItem('jwt_token', res.data.token);
    }
    return res.data;
  },

  resetPassword: async (email: string, newPassword: string) => {
    const res = await apiClient.post('/auth/reset-password', { email, newPassword });
    return res.data;
  },

  logout: () => {
    localStorage.removeItem('jwt_token');
  },

  getToken: () => localStorage.getItem('jwt_token'),

  isAuthenticated: () => Boolean(localStorage.getItem('jwt_token')),
};
```

---

### 3. Protected Route Guard (`src/components/ProtectedRoute.tsx` / `.jsx`)
```tsx
import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { authService } from '../services/authService';

export const ProtectedRoute = () => {
  const isAuth = authService.isAuthenticated();

  if (!isAuth) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
};
```

---

## 5. UI / UX Best Practices for Implementing LLMs

1. **OTP Input Control**:
   - Provide a 6-digit segmented input (or single field with `maxLength={6}` and `letterSpacing: 8px`).
   - Format: Only permit numeric inputs (`0-9`).
2. **Debounce & Resend Cooldown**:
   - Implement a 60-second cooldown timer on the **"Resend OTP"** button to protect against rate limits and spam.
3. **Session Persistence on Refresh**:
   - If user refreshes on the OTP screen, preserve `pendingEmail` in `sessionStorage` (`sessionStorage.setItem('pending_verification_email', email)`).
4. **Token Security**:
   - Never store plain passwords in local state after login/registration.
   - Store the token securely and clear it on explicit logout.
