import React, { createContext, useContext, useEffect, useMemo, useState } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const authDisabled =
    import.meta.env.VITE_DISABLE_AUTH === "true" || import.meta.env.DEV;
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (authDisabled) {
      setUser({ uid: "demo", email: "demo@local" });
      setLoading(false);
      return;
    }

    let unsub = null;
    (async () => {
      const { auth } = await import("../firebase.js");
      const { onAuthStateChanged } = await import("firebase/auth");
      unsub = onAuthStateChanged(auth, (u) => {
        setUser(u);
        setLoading(false);
      });
    })().catch(() => {
      setUser(null);
      setLoading(false);
    });

    return () => {
      if (typeof unsub === "function") unsub();
    };
  }, [authDisabled]);

  const value = useMemo(() => {
    return {
      user,
      loading,
      async signup(email, password) {
        if (authDisabled) return { user: { uid: "demo", email: "demo@local" } };
        const { auth } = await import("../firebase.js");
        const { createUserWithEmailAndPassword } = await import("firebase/auth");
        return await createUserWithEmailAndPassword(auth, email, password);
      },
      async login(email, password) {
        if (authDisabled) return { user: { uid: "demo", email: "demo@local" } };
        const { auth } = await import("../firebase.js");
        const { signInWithEmailAndPassword } = await import("firebase/auth");
        return await signInWithEmailAndPassword(auth, email, password);
      },
      async logout() {
        if (authDisabled) return;
        const { auth } = await import("../firebase.js");
        const { signOut } = await import("firebase/auth");
        return await signOut(auth);
      },
      async getIdToken() {
        if (authDisabled) return null;
        const { auth } = await import("../firebase.js");
        const current = auth.currentUser;
        if (!current) return null;
        return await current.getIdToken();
      }
    };
  }, [user, loading, authDisabled]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

