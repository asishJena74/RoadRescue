package app.roadrescue.dto;

import app.roadrescue.model.IssueType;
import app.roadrescue.model.PaymentMethod;
import app.roadrescue.model.RequestStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class RequestDtos {
  private RequestDtos() {
  }

  public record CreateRequest(
      String vehicleId,
      @NotNull IssueType issueType,
      @Size(min = 5) String description,
      @NotNull Double pickupLat,
      @NotNull Double pickupLng,
      String manualLocationText,
      List<String> mediaUrls,
      @NotNull PaymentMethod paymentMethod
  ) {
  }

  public record StatusRequest(
      @NotNull RequestStatus status,
      @Size(min = 3) String note,
      Double latitude,
      Double longitude
  ) {
  }

  public record ReviewRequest(
      @Min(1) @Max(5) Integer rating,
      @Size(min = 3) String comment
  ) {
  }
}
