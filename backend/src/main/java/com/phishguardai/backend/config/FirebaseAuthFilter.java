package com.phishguardai.backend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.phishguardai.backend.util.AuthUser;
import com.phishguardai.backend.util.AuthUserContext;
import com.phishguardai.backend.util.BearerTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class FirebaseAuthFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String token = BearerTokenUtil.extractBearerToken(request);
      if (token == null) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response
            .getWriter()
            .write("{\"error\":\"Unauthorized\",\"message\":\"Missing Bearer token\"}");
        return;
      }

      FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
      String uid = decoded.getUid();
      String email = decoded.getEmail();
      AuthUserContext.set(new AuthUser(uid, email));

      var authentication =
          new UsernamePasswordAuthenticationToken(uid, token, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(authentication);

      filterChain.doFilter(request, response);
    } catch (Exception ex) {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType("application/json");
      response
          .getWriter()
          .write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or expired token\"}");
    } finally {
      SecurityContextHolder.clearContext();
      AuthUserContext.clear();
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path == null) return false;
    return path.equals("/actuator/health") || path.startsWith("/error");
  }
}

