package app.roadrescue.dto;

import app.roadrescue.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
  private AuthDtos() {
  }

  public record RegisterRequest(
      @Size(min = 2) String name,
      @Email String email,
      @Size(min = 8) String password,
      @Size(min = 7) String phone,
      @NotNull Role role
  ) {
  }

  public record LoginRequest(
      @Email String email,
      @Size(min = 8) String password
  ) {
  }
}
