package app.roadrescue.repository;

import app.roadrescue.dto.UserDtos.ProviderProfileRequest;
import app.roadrescue.model.Role;
import app.roadrescue.util.Ids;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {
  private final JdbcTemplate jdbc;
  private final UserRepository users;

  public ProfileRepository(JdbcTemplate jdbc, UserRepository users) {
    this.jdbc = jdbc;
    this.users = users;
  }

  public List<Map<String, Object>> mechanics() {
    return jdbc.queryForList("SELECT * FROM \"MechanicProfile\"").stream().map(users::profile).toList();
  }

  public List<Map<String, Object>> garages() {
    return jdbc.queryForList("SELECT * FROM \"GarageProfile\"").stream().map(users::profile).toList();
  }

  public Map<String, Object> findMechanicByUserId(String userId) {
    return jdbc.queryForList("SELECT * FROM \"MechanicProfile\" WHERE \"userId\" = ?", userId).stream()
        .findFirst().map(users::profile).orElse(null);
  }

  public Map<String, Object> findGarageByUserId(String userId) {
    return jdbc.queryForList("SELECT * FROM \"GarageProfile\" WHERE \"userId\" = ?", userId).stream()
        .findFirst().map(users::profile).orElse(null);
  }

  public Map<String, Object> upsert(String userId, Role role, String userName, String userPhone, ProviderProfileRequest request) {
    if (role == Role.MECHANIC) {
      return upsertMechanic(userId, request);
    }
    return upsertGarage(userId, userName, userPhone, request);
  }

  public Map<String, Object> updateAvailability(String userId, Role role, boolean isOnline) {
    if (role == Role.MECHANIC) {
      jdbc.update("UPDATE \"MechanicProfile\" SET \"isOnline\" = ? WHERE \"userId\" = ?", isOnline, userId);
      return findMechanicByUserId(userId);
    }
    jdbc.update("UPDATE \"GarageProfile\" SET \"isOnline\" = ? WHERE \"userId\" = ?", isOnline, userId);
    return findGarageByUserId(userId);
  }

  public void setMechanicActiveRequest(String userId, String requestId) {
    jdbc.update("UPDATE \"MechanicProfile\" SET \"activeRequestId\" = ? WHERE \"userId\" = ?", requestId, userId);
  }

  public void releaseMechanic(String userId, String requestId) {
    jdbc.update("UPDATE \"MechanicProfile\" SET \"activeRequestId\" = NULL WHERE \"userId\" = ? AND \"activeRequestId\" = ?", userId, requestId);
  }

  public void updateAvgRating(String userId, double rating) {
    jdbc.update("UPDATE \"MechanicProfile\" SET \"avgRating\" = ? WHERE \"userId\" = ?", rating, userId);
    jdbc.update("UPDATE \"GarageProfile\" SET \"avgRating\" = ? WHERE \"userId\" = ?", rating, userId);
  }

  private Map<String, Object> upsertMechanic(String userId, ProviderProfileRequest request) {
    if (findMechanicByUserId(userId) == null) {
      jdbc.update(connection -> {
        PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO "MechanicProfile" ("id","userId","bio","services","serviceRadiusKm","currentLat","currentLng","workingHours","baseRate","yearsExperience")
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """, Statement.NO_GENERATED_KEYS);
        ps.setString(1, Ids.cuid());
        ps.setString(2, userId);
        ps.setString(3, request.bio());
        ps.setArray(4, DbArrays.serviceType(connection, request.services()));
        ps.setDouble(5, request.serviceRadiusKm());
        ps.setDouble(6, request.currentLat());
        ps.setDouble(7, request.currentLng());
        ps.setString(8, request.workingHours());
        ps.setDouble(9, request.baseRate());
        ps.setInt(10, request.yearsExperience() == null ? 0 : request.yearsExperience());
        return ps;
      });
    } else {
      jdbc.update(connection -> {
        PreparedStatement ps = connection.prepareStatement("""
            UPDATE "MechanicProfile"
            SET "bio" = ?, "services" = ?, "workingHours" = ?, "serviceRadiusKm" = ?, "currentLat" = ?,
                "currentLng" = ?, "baseRate" = ?, "yearsExperience" = ?
            WHERE "userId" = ?
            """);
        ps.setString(1, request.bio());
        ps.setArray(2, DbArrays.serviceType(connection, request.services()));
        ps.setString(3, request.workingHours());
        ps.setDouble(4, request.serviceRadiusKm());
        ps.setDouble(5, request.currentLat());
        ps.setDouble(6, request.currentLng());
        ps.setDouble(7, request.baseRate());
        ps.setInt(8, request.yearsExperience() == null ? 0 : request.yearsExperience());
        ps.setString(9, userId);
        return ps;
      });
    }
    return findMechanicByUserId(userId);
  }

  private Map<String, Object> upsertGarage(String userId, String userName, String userPhone, ProviderProfileRequest request) {
    var businessName = request.businessName() == null || request.businessName().isBlank() ? userName + " Garage" : request.businessName();
    var address = request.address() == null || request.address().isBlank() ? "Address pending" : request.address();
    var phone = request.phoneNumber() == null || request.phoneNumber().isBlank() ? userPhone : request.phoneNumber();
    if (findGarageByUserId(userId) == null) {
      jdbc.update(connection -> {
        PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO "GarageProfile" ("id","userId","businessName","services","serviceRadiusKm","locationLat","locationLng","workingHours","address","baseRate","phoneNumber")
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """);
        ps.setString(1, Ids.cuid());
        ps.setString(2, userId);
        ps.setString(3, businessName);
        ps.setArray(4, DbArrays.serviceType(connection, request.services()));
        ps.setDouble(5, request.serviceRadiusKm());
        ps.setDouble(6, request.currentLat());
        ps.setDouble(7, request.currentLng());
        ps.setString(8, request.workingHours());
        ps.setString(9, address);
        ps.setDouble(10, request.baseRate());
        ps.setString(11, phone);
        return ps;
      });
    } else {
      jdbc.update(connection -> {
        PreparedStatement ps = connection.prepareStatement("""
            UPDATE "GarageProfile"
            SET "businessName" = ?, "services" = ?, "serviceRadiusKm" = ?, "locationLat" = ?, "locationLng" = ?,
                "workingHours" = ?, "address" = ?, "baseRate" = ?, "phoneNumber" = ?
            WHERE "userId" = ?
            """);
        ps.setString(1, businessName);
        ps.setArray(2, DbArrays.serviceType(connection, request.services()));
        ps.setDouble(3, request.serviceRadiusKm());
        ps.setDouble(4, request.currentLat());
        ps.setDouble(5, request.currentLng());
        ps.setString(6, request.workingHours());
        ps.setString(7, address);
        ps.setDouble(8, request.baseRate());
        ps.setString(9, phone);
        ps.setString(10, userId);
        return ps;
      });
    }
    return findGarageByUserId(userId);
  }
}
