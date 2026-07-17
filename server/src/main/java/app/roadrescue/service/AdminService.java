package app.roadrescue.service;

import app.roadrescue.dto.UserDtos.ModerateUserRequest;
import app.roadrescue.repository.RequestRepository;
import app.roadrescue.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
  private final UserRepository users;
  private final RequestRepository requests;

  public AdminService(UserRepository users, RequestRepository requests) {
    this.users = users;
    this.requests = requests;
  }

  public Map<String, Object> analytics() {
    return Map.of(
        "users", users.countUsers(),
        "requests", requests.countRequests(),
        "activeRequests", requests.countActive(),
        "completedRequests", requests.countCompleted(),
        "revenue", requests.paidRevenue()
    );
  }

  public List<Map<String, Object>> users() {
    return users.listUsers().stream().map(row -> users.hydrate(row, false, false)).toList();
  }

  public Map<String, Object> moderate(String userId, ModerateUserRequest request) {
    return users.sanitize(users.updateModeration(userId, request.isBlocked(), request.isVerified()));
  }
}
