package app.roadrescue.service;

import app.roadrescue.dto.RequestDtos.CreateRequest;
import app.roadrescue.dto.RequestDtos.ReviewRequest;
import app.roadrescue.dto.RequestDtos.StatusRequest;
import app.roadrescue.exception.ApiException;
import app.roadrescue.model.AuthUser;
import app.roadrescue.model.RequestStatus;
import app.roadrescue.model.Role;
import app.roadrescue.repository.ProfileRepository;
import app.roadrescue.repository.RequestRepository;
import app.roadrescue.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestService {
  private final RequestRepository requests;
  private final UserRepository users;
  private final ProfileRepository profiles;
  private final MatchingService matching;

  public RequestService(RequestRepository requests, UserRepository users, ProfileRepository profiles, MatchingService matching) {
    this.requests = requests;
    this.users = users;
    this.profiles = profiles;
    this.matching = matching;
  }

  public List<Map<String, Object>> nearbyProviders(app.roadrescue.model.IssueType issueType, double lat, double lng) {
    return matching.findBestProviders(issueType, lat, lng).stream().limit(10).map(provider -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("providerId", provider.providerId());
      row.put("providerKind", provider.providerKind());
      row.put("distanceKm", provider.distanceKm());
      row.put("etaMinutes", provider.etaMinutes());
      row.put("priceEstimate", provider.priceEstimate());
      row.put("rating", provider.rating());
      row.put("serviceLabel", provider.serviceLabel());
      row.put("user", users.findRawById(provider.providerId()).map(user -> users.hydrate(user, false, false)).orElse(null));
      return row;
    }).toList();
  }

  @Transactional
  public Map<String, Object> create(String customerId, CreateRequest request) {
    var created = requests.create(customerId, request);
    var assigned = matching.assignProviderToRequest(String.valueOf(created.get("id")));
    requests.updatePaymentAmount(String.valueOf(created.get("id")), ((Number) assigned.get("priceEstimate")).doubleValue());
    return requests.findHydrated(String.valueOf(created.get("id"))).orElse(assigned);
  }

  public List<Map<String, Object>> list(AuthUser auth) {
    var user = users.findRawById(auth.userId()).orElseThrow(() -> new ApiException("User not found."));
    var role = Role.valueOf(String.valueOf(user.get("role")));
    if (role == Role.CUSTOMER) {
      return requests.listForCustomer(auth.userId());
    }
    if (role == Role.ADMIN) {
      return requests.listAll();
    }
    return requests.listForProvider(auth.userId());
  }

  public Map<String, Object> get(String requestId) {
    return requests.findHydrated(requestId).orElseThrow(() -> new ApiException("Request not found."));
  }

  @Transactional
  public Map<String, Object> updateStatus(AuthUser auth, String requestId, StatusRequest payload) {
    var request = requests.findRaw(requestId).orElseThrow(() -> new ApiException("Request not found."));
    var isAdmin = auth.role() == Role.ADMIN;
    var isCustomer = auth.userId().equals(request.get("customerId"));
    var isProvider = auth.userId().equals(request.get("providerId"));

    if (!isAdmin && !isCustomer && !isProvider) {
      throw new ApiException("You are not allowed to update this request.");
    }
    if (isCustomer && payload.status() != RequestStatus.CANCELLED) {
      throw new ApiException("Customers can only cancel requests.");
    }
    if (isProvider && (payload.status() == RequestStatus.PENDING || payload.status() == RequestStatus.CANCELLED)) {
      throw new ApiException("Providers cannot set that status.");
    }
    if (payload.status() == RequestStatus.CANCELLED && Boolean.FALSE.equals(request.get("customerCanCancel"))) {
      throw new ApiException("Request can no longer be cancelled.");
    }

    var updated = requests.updateStatus(requestId, payload.status(), payload.note(), payload.latitude(), payload.longitude());
    if (payload.status() == RequestStatus.COMPLETED
        || payload.status() == RequestStatus.CANCELLED
        || payload.status() == RequestStatus.REJECTED
        || payload.status() == RequestStatus.EXPIRED) {
      matching.releaseProviderIfNeeded(updated);
    }
    if (payload.status() == RequestStatus.REJECTED) {
      var providerId = updated.get("providerId") == null ? null : String.valueOf(updated.get("providerId"));
      if (providerId != null) {
        requests.clearRejectedProvider(requestId, providerId);
      }
      var reassigned = matching.assignProviderToRequest(requestId);
      requests.updatePaymentAmount(requestId, ((Number) reassigned.get("priceEstimate")).doubleValue());
      return requests.findHydrated(requestId).orElse(reassigned);
    }
    return updated;
  }

  @Transactional
  public Map<String, Object> review(AuthUser auth, String requestId, ReviewRequest payload) {
    var request = requests.findHydrated(requestId).orElseThrow(() -> new ApiException("Request not found."));
    if (!auth.userId().equals(request.get("customerId"))) {
      throw new ApiException("Only the customer can review this request.");
    }
    if (!RequestStatus.COMPLETED.name().equals(String.valueOf(request.get("status"))) || request.get("providerId") == null) {
      throw new ApiException("Reviews are only allowed after a completed service.");
    }
    if (request.get("review") != null) {
      throw new ApiException("Review already submitted.");
    }
    var review = requests.createReview(requestId, auth.userId(), String.valueOf(request.get("providerId")), payload.rating(), payload.comment());
    var avg = requests.averageRating(String.valueOf(request.get("providerId")));
    profiles.updateAvgRating(String.valueOf(request.get("providerId")), avg == null ? payload.rating() : avg);
    return review;
  }
}
