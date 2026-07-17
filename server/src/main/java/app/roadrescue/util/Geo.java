package app.roadrescue.util;

public final class Geo {
  private Geo() {
  }

  public static double haversineDistanceKm(double startLat, double startLng, double endLat, double endLng) {
    var earthRadiusKm = 6371.0;
    var latDelta = Math.toRadians(endLat - startLat);
    var lngDelta = Math.toRadians(endLng - startLng);
    var a = Math.pow(Math.sin(latDelta / 2), 2)
        + Math.cos(Math.toRadians(startLat))
        * Math.cos(Math.toRadians(endLat))
        * Math.pow(Math.sin(lngDelta / 2), 2);
    return 2 * earthRadiusKm * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  public static int estimateEtaMinutes(double distanceKm) {
    return Math.max(8, (int) Math.round((distanceKm / 35.0) * 60.0));
  }
}
