package app.roadrescue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class UserDtos {
  private UserDtos() {
  }

  public record VehicleRequest(
      @Size(min = 2) String brand,
      @Size(min = 1) String model,
      @Size(min = 3) String plateNumber,
      @Min(1990) Integer year,
      String color,
      String fuelType
  ) {
  }

  public record ProviderProfileRequest(
      String bio,
      @NotNull @Size(min = 1) List<String> services,
      @Size(min = 3) String workingHours,
      @NotNull @Min(1) Double serviceRadiusKm,
      @NotNull Double currentLat,
      @NotNull Double currentLng,
      String phoneNumber,
      String businessName,
      String address,
      @NotNull @Min(0) Double baseRate,
      @Min(0) Integer yearsExperience
  ) {
  }

  public record AvailabilityRequest(boolean isOnline) {
  }

  public record ModerateUserRequest(Boolean isBlocked, Boolean isVerified) {
  }
}
