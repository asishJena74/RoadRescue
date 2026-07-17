package app.roadrescue.util;

import app.roadrescue.model.IssueType;
import app.roadrescue.model.ServiceType;
import java.util.List;
import java.util.Map;

public final class Pricing {
  private static final Map<IssueType, List<ServiceType>> CAPABILITIES = Map.of(
      IssueType.FLAT_TIRE, List.of(ServiceType.MOBILE_MECHANIC, ServiceType.GARAGE),
      IssueType.BATTERY_DEAD, List.of(ServiceType.BATTERY, ServiceType.MOBILE_MECHANIC, ServiceType.GARAGE),
      IssueType.ENGINE_PROBLEM, List.of(ServiceType.MOBILE_MECHANIC, ServiceType.GARAGE, ServiceType.EMERGENCY),
      IssueType.ACCIDENT, List.of(ServiceType.TOWING, ServiceType.EMERGENCY, ServiceType.GARAGE),
      IssueType.FUEL_EMPTY, List.of(ServiceType.FUEL_DELIVERY, ServiceType.EMERGENCY),
      IssueType.TOWING_NEEDED, List.of(ServiceType.TOWING, ServiceType.GARAGE),
      IssueType.KEY_LOCKED, List.of(ServiceType.MOBILE_MECHANIC, ServiceType.EMERGENCY),
      IssueType.OVERHEATING, List.of(ServiceType.MOBILE_MECHANIC, ServiceType.GARAGE),
      IssueType.OTHER, List.of(ServiceType.EMERGENCY, ServiceType.MOBILE_MECHANIC, ServiceType.GARAGE)
  );

  private static final Map<IssueType, Double> BASE = Map.of(
      IssueType.FLAT_TIRE, 25.0,
      IssueType.BATTERY_DEAD, 35.0,
      IssueType.ENGINE_PROBLEM, 65.0,
      IssueType.ACCIDENT, 90.0,
      IssueType.FUEL_EMPTY, 30.0,
      IssueType.TOWING_NEEDED, 80.0,
      IssueType.KEY_LOCKED, 40.0,
      IssueType.OVERHEATING, 55.0,
      IssueType.OTHER, 45.0
  );

  private Pricing() {
  }

  public static List<ServiceType> supportedServices(IssueType issueType) {
    return CAPABILITIES.get(issueType);
  }

  public static double estimateBasePrice(IssueType issueType, double distanceKm) {
    return Math.round((BASE.get(issueType) + distanceKm * 2.75) * 100.0) / 100.0;
  }
}
