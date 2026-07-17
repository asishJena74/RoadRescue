package app.roadrescue.repository;

import app.roadrescue.dto.RequestDtos.CreateRequest;
import app.roadrescue.model.PaymentMethod;
import app.roadrescue.model.PaymentStatus;
import app.roadrescue.model.ProviderCandidate;
import app.roadrescue.model.RequestStatus;
import app.roadrescue.util.Ids;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RequestRepository {
  private final JdbcTemplate jdbc;
  private final UserRepository users;

  public RequestRepository(JdbcTemplate jdbc, UserRepository users) {
    this.jdbc = jdbc;
    this.users = users;
  }

  public Map<String, Object> create(String customerId, CreateRequest request) {
    var id = Ids.cuid();
    var media = request.mediaUrls() == null ? List.<String>of() : request.mediaUrls();
    jdbc.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO "AssistanceRequest"
            ("id","customerId","vehicleId","issueType","description","pickupLat","pickupLng","manualLocationText",
             "mediaUrls","rejectedProviderIds","priceEstimate","paymentMethod","updatedAt")
          VALUES (?,?,?,CAST(? AS "IssueType"),?,?,?,?,?,?,0,CAST(? AS "PaymentMethod"),CURRENT_TIMESTAMP)
          """, Statement.NO_GENERATED_KEYS);
      ps.setString(1, id);
      ps.setString(2, customerId);
      ps.setString(3, request.vehicleId());
      ps.setString(4, request.issueType().name());
      ps.setString(5, request.description());
      ps.setDouble(6, request.pickupLat());
      ps.setDouble(7, request.pickupLng());
      ps.setString(8, request.manualLocationText());
      ps.setArray(9, DbArrays.text(connection, media));
      ps.setArray(10, DbArrays.text(connection, List.of()));
      ps.setString(11, request.paymentMethod().name());
      return ps;
    });
    createUpdate(id, RequestStatus.PENDING, "Emergency request created.", null, null);
    createPayment(id, 0, request.paymentMethod(), PaymentStatus.PENDING, null);
    return findRaw(id).orElseThrow();
  }

  public Optional<Map<String, Object>> findRaw(String id) {
    return jdbc.queryForList("SELECT * FROM \"AssistanceRequest\" WHERE \"id\" = ?", id).stream().findFirst();
  }

  public Map<String, Object> assign(String requestId, ProviderCandidate candidate) {
    jdbc.update("""
        UPDATE "AssistanceRequest"
        SET "status" = CAST('MATCHING' AS "RequestStatus"), "providerId" = ?, "providerKind" = CAST(? AS "ProviderKind"),
            "distanceKm" = ?, "etaMinutes" = ?, "priceEstimate" = ?, "assignedAt" = CURRENT_TIMESTAMP,
            "updatedAt" = CURRENT_TIMESTAMP
        WHERE "id" = ?
        """, candidate.providerId(), candidate.providerKind().name(), candidate.distanceKm(), candidate.etaMinutes(), candidate.priceEstimate(), requestId);
    createUpdate(requestId, RequestStatus.MATCHING, "Request routed to " + candidate.serviceLabel() + ".", null, null);
    return findHydrated(requestId).orElseThrow();
  }

  public Map<String, Object> expire(String requestId) {
    jdbc.update("""
        UPDATE "AssistanceRequest"
        SET "status" = CAST('EXPIRED' AS "RequestStatus"), "updatedAt" = CURRENT_TIMESTAMP
        WHERE "id" = ?
        """, requestId);
    createUpdate(requestId, RequestStatus.EXPIRED, "No provider available in radius.", null, null);
    return findHydrated(requestId).orElseThrow();
  }

  public Map<String, Object> updateStatus(String requestId, RequestStatus status, String note, Double latitude, Double longitude) {
    var assignments = new ArrayList<Object>();
    var sql = new StringBuilder("""
        UPDATE "AssistanceRequest"
        SET "status" = CAST(? AS "RequestStatus"), "customerCanCancel" = ?, "updatedAt" = CURRENT_TIMESTAMP
        """);
    assignments.add(status.name());
    assignments.add(!(status == RequestStatus.SERVICE_STARTED || status == RequestStatus.COMPLETED));
    if (status == RequestStatus.SERVICE_STARTED) {
      sql.append(", \"startedAt\" = CURRENT_TIMESTAMP");
    }
    if (status == RequestStatus.COMPLETED) {
      sql.append(", \"completedAt\" = CURRENT_TIMESTAMP");
    }
    if (status == RequestStatus.CANCELLED) {
      sql.append(", \"cancelledAt\" = CURRENT_TIMESTAMP");
    }
    sql.append(" WHERE \"id\" = ?");
    assignments.add(requestId);
    jdbc.update(sql.toString(), assignments.toArray());
    createUpdate(requestId, status, note, latitude, longitude);
    return findHydrated(requestId).orElseThrow();
  }

  public Map<String, Object> clearRejectedProvider(String requestId, String providerId) {
    jdbc.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          UPDATE "AssistanceRequest"
          SET "rejectedProviderIds" = array_append(COALESCE("rejectedProviderIds", ARRAY[]::TEXT[]), ?),
              "providerId" = NULL, "providerKind" = NULL, "distanceKm" = NULL, "etaMinutes" = NULL, "updatedAt" = CURRENT_TIMESTAMP
          WHERE "id" = ?
          """);
      ps.setString(1, providerId);
      ps.setString(2, requestId);
      return ps;
    });
    return findRaw(requestId).orElseThrow();
  }

  public void updatePaymentAmount(String requestId, double amount) {
    jdbc.update("UPDATE \"Payment\" SET \"amount\" = ?, \"updatedAt\" = CURRENT_TIMESTAMP WHERE \"requestId\" = ?", amount, requestId);
  }

  public List<Map<String, Object>> listForCustomer(String userId) {
    return jdbc.queryForList("SELECT \"id\" FROM \"AssistanceRequest\" WHERE \"customerId\" = ? ORDER BY \"createdAt\" DESC", userId)
        .stream().map(row -> findHydrated(Sql.text(row, "id")).orElseThrow()).toList();
  }

  public List<Map<String, Object>> listForProvider(String userId) {
    return jdbc.queryForList("SELECT \"id\" FROM \"AssistanceRequest\" WHERE \"providerId\" = ? ORDER BY \"createdAt\" DESC", userId)
        .stream().map(row -> findHydrated(Sql.text(row, "id")).orElseThrow()).toList();
  }

  public List<Map<String, Object>> listAll() {
    return jdbc.queryForList("SELECT \"id\" FROM \"AssistanceRequest\" ORDER BY \"createdAt\" DESC")
        .stream().map(row -> findHydrated(Sql.text(row, "id")).orElseThrow()).toList();
  }

  public Optional<Map<String, Object>> findHydrated(String id) {
    return findRaw(id).map(this::hydrate);
  }

  public long countRequests() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM \"AssistanceRequest\"", Long.class);
  }

  public long countCompleted() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM \"AssistanceRequest\" WHERE \"status\" = CAST('COMPLETED' AS \"RequestStatus\")", Long.class);
  }

  public long countActive() {
    return jdbc.queryForObject("""
        SELECT COUNT(*) FROM "AssistanceRequest"
        WHERE "status" IN (
          CAST('PENDING' AS "RequestStatus"), CAST('MATCHING' AS "RequestStatus"), CAST('ACCEPTED' AS "RequestStatus"),
          CAST('ON_THE_WAY' AS "RequestStatus"), CAST('SERVICE_STARTED' AS "RequestStatus")
        )
        """, Long.class);
  }

  public double paidRevenue() {
    var value = jdbc.queryForObject("SELECT COALESCE(SUM(\"amount\"),0) FROM \"Payment\" WHERE \"status\" = CAST('PAID' AS \"PaymentStatus\")", Double.class);
    return value == null ? 0 : value;
  }

  public Map<String, Object> createReview(String requestId, String reviewerId, String targetUserId, int rating, String comment) {
    var id = Ids.cuid();
    jdbc.update("""
        INSERT INTO "Review" ("id","requestId","reviewerId","targetUserId","rating","comment")
        VALUES (?,?,?,?,?,?)
        """, id, requestId, reviewerId, targetUserId, rating, comment);
    return jdbc.queryForList("SELECT * FROM \"Review\" WHERE \"id\" = ?", id).stream().findFirst().map(this::normalized).orElseThrow();
  }

  public Double averageRating(String targetUserId) {
    return jdbc.queryForObject("SELECT AVG(\"rating\") FROM \"Review\" WHERE \"targetUserId\" = ?", Double.class, targetUserId);
  }

  private void createPayment(String requestId, double amount, PaymentMethod method, PaymentStatus status, String reference) {
    jdbc.update("""
        INSERT INTO "Payment" ("id","requestId","amount","method","status","reference","updatedAt")
        VALUES (?,?,?,CAST(? AS "PaymentMethod"),CAST(? AS "PaymentStatus"),?,CURRENT_TIMESTAMP)
        """, Ids.cuid(), requestId, amount, method.name(), status.name(), reference);
  }

  private void createUpdate(String requestId, RequestStatus status, String note, Double latitude, Double longitude) {
    jdbc.update("""
        INSERT INTO "ServiceUpdate" ("id","requestId","status","note","latitude","longitude")
        VALUES (?,?,CAST(? AS "RequestStatus"),?,?,?)
        """, Ids.cuid(), requestId, status.name(), note, latitude, longitude);
  }

  private Map<String, Object> hydrate(Map<String, Object> raw) {
    var out = normalized(raw);
    out.put("mediaUrls", Sql.array(raw, "mediaUrls"));
    out.put("rejectedProviderIds", Sql.array(raw, "rejectedProviderIds"));
    out.put("customer", users.findRawById(Sql.text(raw, "customerId")).map(row -> users.hydrate(row, false, false)).orElse(null));
    out.put("provider", Sql.text(raw, "providerId") == null ? null : users.findRawById(Sql.text(raw, "providerId")).map(row -> users.hydrate(row, false, false)).orElse(null));
    out.put("vehicle", Sql.text(raw, "vehicleId") == null ? null : jdbc.queryForList("SELECT * FROM \"Vehicle\" WHERE \"id\" = ?", Sql.text(raw, "vehicleId")).stream().findFirst().map(users::vehicle).orElse(null));
    out.put("serviceUpdates", jdbc.queryForList("SELECT * FROM \"ServiceUpdate\" WHERE \"requestId\" = ? ORDER BY \"createdAt\" ASC", Sql.text(raw, "id")).stream().map(this::normalized).toList());
    out.put("payment", jdbc.queryForList("SELECT * FROM \"Payment\" WHERE \"requestId\" = ?", Sql.text(raw, "id")).stream().findFirst().map(this::normalized).orElse(null));
    out.put("review", jdbc.queryForList("SELECT * FROM \"Review\" WHERE \"requestId\" = ?", Sql.text(raw, "id")).stream().findFirst().map(this::normalized).orElse(null));
    return out;
  }

  private Map<String, Object> normalized(Map<String, Object> row) {
    var out = new LinkedHashMap<String, Object>();
    for (var entry : row.entrySet()) {
      var value = entry.getValue();
      if (value instanceof java.sql.Timestamp timestamp) {
        out.put(entry.getKey(), timestamp.toInstant().toString());
      } else if (value instanceof java.sql.Array) {
        out.put(entry.getKey(), Sql.array(row, entry.getKey()));
      } else {
        out.put(entry.getKey(), value);
      }
    }
    return out;
  }
}
