package app.roadrescue.config;

import app.roadrescue.exception.ApiException;
import app.roadrescue.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
  public static final String AUTH_ATTR = "auth";
  private final JwtService jwtService;

  public AuthInterceptor(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
      return true;
    }

    var path = request.getRequestURI();
    var needsAuth = path.equals("/api/auth/me")
        || path.startsWith("/api/users")
        || path.startsWith("/api/requests")
        || path.startsWith("/api/admin");
    if (!needsAuth) {
      return true;
    }

    var header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required.");
    }

    try {
      request.setAttribute(AUTH_ATTR, jwtService.verify(header.substring(7)));
      return true;
    } catch (RuntimeException error) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token.");
    }
  }
}
