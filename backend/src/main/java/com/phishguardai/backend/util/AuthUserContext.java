package com.phishguardai.backend.util;

public final class AuthUserContext {
  private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<>();

  private AuthUserContext() {}

  public static void set(AuthUser user) {
    CURRENT.set(user);
  }

  public static AuthUser getRequired() {
    var user = CURRENT.get();
    if (user == null) {
      throw new IllegalStateException("No authenticated user in context.");
    }
    return user;
  }

  public static AuthUser getOrNull() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}

