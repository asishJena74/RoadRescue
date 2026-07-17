package app.roadrescue.controller;

import app.roadrescue.dto.UserDtos.ModerateUserRequest;
import app.roadrescue.model.Role;
import app.roadrescue.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseController {
  private final AdminService adminService;

  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping("/analytics")
  public Map<String, Object> analytics(HttpServletRequest request) {
    requireRole(auth(request), Role.ADMIN);
    return adminService.analytics();
  }

  @GetMapping("/users")
  public List<Map<String, Object>> users(HttpServletRequest request) {
    requireRole(auth(request), Role.ADMIN);
    return adminService.users();
  }

  @PatchMapping("/users/{userId}")
  public Map<String, Object> moderate(HttpServletRequest servletRequest, @PathVariable String userId, @RequestBody ModerateUserRequest request) {
    requireRole(auth(servletRequest), Role.ADMIN);
    return adminService.moderate(userId, request);
  }
}
