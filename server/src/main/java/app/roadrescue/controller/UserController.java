package app.roadrescue.controller;

import app.roadrescue.dto.UserDtos.AvailabilityRequest;
import app.roadrescue.dto.UserDtos.ProviderProfileRequest;
import app.roadrescue.dto.UserDtos.VehicleRequest;
import app.roadrescue.exception.ApiException;
import app.roadrescue.model.Role;
import app.roadrescue.repository.ProfileRepository;
import app.roadrescue.repository.UserRepository;
import app.roadrescue.repository.VehicleRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController {
  private final VehicleRepository vehicles;
  private final ProfileRepository profiles;
  private final UserRepository users;

  public UserController(VehicleRepository vehicles, ProfileRepository profiles, UserRepository users) {
    this.vehicles = vehicles;
    this.profiles = profiles;
    this.users = users;
  }

  @PostMapping("/vehicles")
  public ResponseEntity<Map<String, Object>> addVehicle(HttpServletRequest servletRequest, @Valid @RequestBody VehicleRequest request) {
    var auth = auth(servletRequest);
    requireRole(auth, Role.CUSTOMER);
    return ResponseEntity.status(HttpStatus.CREATED).body(vehicles.create(auth.userId(), request));
  }

  @PutMapping("/provider-profile")
  public Map<String, Object> providerProfile(HttpServletRequest servletRequest, @Valid @RequestBody ProviderProfileRequest request) {
    var auth = auth(servletRequest);
    requireRole(auth, Role.MECHANIC, Role.GARAGE_OWNER);
    var user = users.findRawById(auth.userId()).orElseThrow(() -> new ApiException("User not found."));
    return profiles.upsert(auth.userId(), auth.role(), String.valueOf(user.get("name")), String.valueOf(user.get("phone")), request);
  }

  @PatchMapping("/availability")
  public Map<String, Object> availability(HttpServletRequest servletRequest, @RequestBody AvailabilityRequest request) {
    var auth = auth(servletRequest);
    requireRole(auth, Role.MECHANIC, Role.GARAGE_OWNER);
    if (users.findRawById(auth.userId()).isEmpty()) {
      throw new ApiException("User not found.");
    }
    return profiles.updateAvailability(auth.userId(), auth.role(), request.isOnline());
  }
}
