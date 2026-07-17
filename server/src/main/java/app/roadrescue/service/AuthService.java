package app.roadrescue.service;

import app.roadrescue.dto.AuthDtos.LoginRequest;
import app.roadrescue.dto.AuthDtos.RegisterRequest;
import app.roadrescue.exception.ApiException;
import app.roadrescue.model.AuthUser;
import app.roadrescue.model.Role;
import app.roadrescue.repository.UserRepository;
import app.roadrescue.security.JwtService;
import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final UserRepository users;
  private final JwtService jwt;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

  public AuthService(UserRepository users, JwtService jwt) {
    this.users = users;
    this.jwt = jwt;
  }

  public Map<String, Object> register(RegisterRequest request) {
    if (users.findRawByEmail(request.email()).isPresent()) {
      throw new ApiException("Email already in use.");
    }
    var user = users.create(
        request.name(),
        request.email(),
        passwordEncoder.encode(request.password()),
        request.phone(),
        request.role(),
        request.role() == Role.CUSTOMER
    );
    var safeUser = users.sanitize(user);
    return Map.of("token", jwt.sign(new AuthUser(String.valueOf(user.get("id")), request.role())), "user", safeUser);
  }

  public Map<String, Object> login(LoginRequest request) {
    var user = users.findRawByEmail(request.email()).orElse(null);
    if (user == null || !passwordEncoder.matches(request.password(), String.valueOf(user.get("passwordHash")))) {
      throw new ApiException("Invalid credentials.");
    }
    if (Boolean.TRUE.equals(user.get("isBlocked"))) {
      throw new ApiException("Your account has been blocked.");
    }
    var role = Role.valueOf(String.valueOf(user.get("role")));
    return Map.of(
        "token", jwt.sign(new AuthUser(String.valueOf(user.get("id")), role)),
        "user", users.hydrate(user, false, false)
    );
  }

  public Map<String, Object> me(String userId) {
    return users.findRawById(userId).map(row -> users.hydrate(row, true, true)).orElse(null);
  }
}
