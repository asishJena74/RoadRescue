package app.roadrescue.model;

public record ProviderCandidate(
    String providerId,
    ProviderKind providerKind,
    double distanceKm,
    int etaMinutes,
    double priceEstimate,
    double rating,
    String serviceLabel
) {
}
