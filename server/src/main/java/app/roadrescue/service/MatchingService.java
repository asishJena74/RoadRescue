package app.roadrescue.service;

import app.roadrescue.exception.ApiException;
import app.roadrescue.model.IssueType;
import app.roadrescue.model.ProviderCandidate;
import app.roadrescue.model.ProviderKind;
import app.roadrescue.model.ServiceType;
import app.roadrescue.repository.ProfileRepository;
import app.roadrescue.repository.RequestRepository;
import app.roadrescue.util.Geo;
import app.roadrescue.util.Pricing;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {
  private final ProfileRepository profiles;
  private final RequestRepository requests;

  public MatchingService(ProfileRepository profiles, RequestRepository requests) {
    this.profiles = profiles;
    this.requests = requests;
  }

  public List<ProviderCandidate> findBestProviders(IssueType issueType, double lat, double lng) {
    return java.util.stream.Stream.concat(
            profiles.mechanics().stream().map(profile -> mechanicCandidate(profile, issueType, lat, lng)),
            profiles.garages().stream().map(profile -> garageCandidate(profile, issueType, lat, lng))
        )
        .filter(Objects::nonNull)
        .sorted(Comparator.comparingDouble(ProviderCandidate::distanceKm).thenComparing(Comparator.comparingDouble(ProviderCandidate::rating).reversed()))
        .toList();
  }

  public Map<String, Object> assignProviderToRequest(String requestId) {
    var request = requests.findRaw(requestId).orElseThrow(() -> new ApiException("Request not found."));
    var providers = findBestProviders(IssueType.valueOf(String.valueOf(request.get("issueType"))), number(request, "pickupLat"), number(request, "pickupLng"));
    var rejected = app.roadrescue.repository.Sql.array(request, "rejectedProviderIds");
    for (var provider : providers) {
      if (rejected.contains(provider.providerId())) {
        continue;
      }
      if (provider.providerKind() == ProviderKind.MECHANIC) {
        var mechanic = profiles.findMechanicByUserId(provider.providerId());
        if (mechanic == null || mechanic.get("activeRequestId") != null) {
          continue;
        }
        profiles.setMechanicActiveRequest(provider.providerId(), requestId);
      }
      return requests.assign(requestId, provider);
    }
    return requests.expire(requestId);
  }

  public void releaseProviderIfNeeded(Map<String, Object> request) {
    if ("MECHANIC".equals(String.valueOf(request.get("providerKind"))) && request.get("providerId") != null) {
      profiles.releaseMechanic(String.valueOf(request.get("providerId")), String.valueOf(request.get("id")));
    }
  }

  private ProviderCandidate mechanicCandidate(Map<String, Object> profile, IssueType issueType, double lat, double lng) {
    var supported = Pricing.supportedServices(issueType).stream().map(ServiceType::name).toList();
    var services = (List<String>) profile.get("services");
    if (!Boolean.TRUE.equals(profile.get("isOnline")) || profile.get("activeRequestId") != null || services.stream().noneMatch(supported::contains)) {
      return null;
    }
    var distance = Geo.haversineDistanceKm(lat, lng, number(profile, "currentLat"), number(profile, "currentLng"));
    if (distance > number(profile, "serviceRadiusKm")) {
      return null;
    }
    return new ProviderCandidate(
        String.valueOf(profile.get("userId")),
        ProviderKind.MECHANIC,
        distance,
        Geo.estimateEtaMinutes(distance),
        Pricing.estimateBasePrice(issueType, distance) + number(profile, "baseRate"),
        number(profile, "avgRating"),
        "Mobile Mechanic"
    );
  }

  private ProviderCandidate garageCandidate(Map<String, Object> profile, IssueType issueType, double lat, double lng) {
    var supported = Pricing.supportedServices(issueType).stream().map(ServiceType::name).toList();
    var services = (List<String>) profile.get("services");
    if (!Boolean.TRUE.equals(profile.get("isOnline")) || services.stream().noneMatch(supported::contains)) {
      return null;
    }
    var distance = Geo.haversineDistanceKm(lat, lng, number(profile, "locationLat"), number(profile, "locationLng"));
    if (distance > number(profile, "serviceRadiusKm")) {
      return null;
    }
    return new ProviderCandidate(
        String.valueOf(profile.get("userId")),
        ProviderKind.GARAGE,
        distance,
        Geo.estimateEtaMinutes(distance),
        Pricing.estimateBasePrice(issueType, distance) + number(profile, "baseRate"),
        number(profile, "avgRating"),
        "Garage Dispatch"
    );
  }

  private double number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).doubleValue();
  }
}
