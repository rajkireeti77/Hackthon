import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import LoadingSpinner from "./LoadingSpinner";

export default function ProtectedRoute({ children }) {
  const authDisabled =
    import.meta.env.VITE_DISABLE_AUTH === "true" || import.meta.env.DEV;
  const { user, loading } = useAuth();
  const location = useLocation();

  if (authDisabled) return children;
  if (loading) return <LoadingSpinner label="Checking session..." />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;

  return children;
}

