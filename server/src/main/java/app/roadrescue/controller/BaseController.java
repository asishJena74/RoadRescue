package app.roadrescue.controller;

import app.roadrescue.config.AuthInterceptor;
import app.roadrescue.exception.ApiException;
import app.roadrescue.model.AuthUser;
import app.roadrescue.model.Role;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.http.HttpStatus;

abstract class BaseController {
  protected AuthUser auth(HttpServletRequest request) {
    return (AuthUser) request.getAttribute(AuthInterceptor.AUTH_ATTR);
  }

  protected void requireRole(AuthUser auth, Role... roles) {
    if (auth == null || Arrays.stream(roles).noneMatch(role -> role == auth.role())) {
      throw new ApiException(HttpStatus.FORBIDDEN, "Access denied.");
    }
  }
}
