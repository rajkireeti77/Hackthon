package com.phishguardai.backend.config;

import com.phishguardai.backend.util.AuthUser;
import com.phishguardai.backend.util.AuthUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class DevAuthBypassFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      AuthUserContext.set(new AuthUser("demo", "demo@local"));
      filterChain.doFilter(request, response);
    } finally {
      AuthUserContext.clear();
    }
  }
}

