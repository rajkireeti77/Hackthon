import React from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

function initials(email) {
  if (!email) return "U";
  return email.slice(0, 1).toUpperCase();
}

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function onLogout() {
    await logout();
    navigate("/login");
  }

  return (
    <header className="navbar">
      <div className="navbar-inner container">
        <Link to="/dashboard" className="brand">
          <span className="brand-mark" aria-hidden="true">
            PG
          </span>
          <span className="brand-text">
            <span className="brand-name">PhishGuard AI</span>
            <span className="brand-sub">Phishing & social engineering detection</span>
          </span>
        </Link>

        <nav className="nav-links">
          {user ? (
            <>
              <NavLink to="/dashboard" className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}>
                Dashboard
              </NavLink>
              <NavLink to="/alerts" className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}>
                Alerts
              </NavLink>
              <NavLink to="/tips" className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}>
                Security Tips
              </NavLink>
              <div className="user-chip" title={user.email || user.uid}>
                <span className="avatar">{initials(user.email)}</span>
                <span className="user-email">{user.email || "Signed in"}</span>
              </div>
              <button className="btn btn-ghost" onClick={onLogout}>
                Logout
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}>
                Login
              </NavLink>
              <NavLink to="/signup" className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}>
                Signup
              </NavLink>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}

