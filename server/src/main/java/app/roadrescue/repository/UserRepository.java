package app.roadrescue.repository;

import app.roadrescue.model.Role;
import app.roadrescue.util.Ids;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  private final JdbcTemplate jdbc;

  public UserRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<Map<String, Object>> findRawByEmail(String email) {
    return jdbc.queryForList("SELECT * FROM \"User\" WHERE \"email\" = ?", email).stream().findFirst();
  }

  public Optional<Map<String, Object>> findRawById(String id) {
    return jdbc.queryForList("SELECT * FROM \"User\" WHERE \"id\" = ?", id).stream().findFirst();
  }

  public Map<String, Object> create(String name, String email, String passwordHash, String phone, Role role, boolean verified) {
    var id = Ids.cuid();
    jdbc.update("""
        INSERT INTO "User" ("id","name","email","passwordHash","phone","role","isVerified","updatedAt")
        VALUES (?,?,?,?,?,CAST(? AS "Role"),?,CURRENT_TIMESTAMP)
        """, id, name, email, passwordHash, phone, role.name(), verified);
    return findRawById(id).orElseThrow();
  }

  public List<Map<String, Object>> listUsers() {
    return jdbc.queryForList("SELECT * FROM \"User\" ORDER BY \"createdAt\" DESC");
  }

  public long countUsers() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM \"User\"", Long.class);
  }

  public Map<String, Object> updateModeration(String userId, Boolean isBlocked, Boolean isVerified) {
    if (isBlocked != null) {
      jdbc.update("UPDATE \"User\" SET \"isBlocked\" = ?, \"updatedAt\" = CURRENT_TIMESTAMP WHERE \"id\" = ?", isBlocked, userId);
    }
    if (isVerified != null) {
      jdbc.update("UPDATE \"User\" SET \"isVerified\" = ?, \"updatedAt\" = CURRENT_TIMESTAMP WHERE \"id\" = ?", isVerified, userId);
    }
    return findRawById(userId).orElseThrow();
  }

  public Map<String, Object> hydrate(Map<String, Object> raw, boolean vehicles, boolean notifications) {
    var user = sanitize(raw);
    var mechanic = jdbc.queryForList("SELECT * FROM \"MechanicProfile\" WHERE \"userId\" = ?", Sql.text(raw, "id")).stream()
        .findFirst().map(this::profile).orElse(null);
    var garage = jdbc.queryForList("SELECT * FROM \"GarageProfile\" WHERE \"userId\" = ?", Sql.text(raw, "id")).stream()
        .findFirst().map(this::profile).orElse(null);
    user.put("mechanicProfile", mechanic);
    user.put("garageProfile", garage);
    if (vehicles) {
      user.put("vehicles", jdbc.queryForList("SELECT * FROM \"Vehicle\" WHERE \"userId\" = ? ORDER BY \"createdAt\" DESC", Sql.text(raw, "id"))
          .stream().map(this::vehicle).toList());
    }
    if (notifications) {
      user.put("notifications", jdbc.queryForList("SELECT * FROM \"Notification\" WHERE \"userId\" = ? ORDER BY \"createdAt\" DESC LIMIT 10", Sql.text(raw, "id"))
          .stream().map(this::notification).toList());
    }
    return user;
  }

  public Map<String, Object> sanitize(Map<String, Object> row) {
    var out = new LinkedHashMap<String, Object>();
    for (var entry : row.entrySet()) {
      if (!entry.getKey().equals("passwordHash")) {
        out.put(entry.getKey(), normalize(entry.getValue()));
      }
    }
    return out;
  }

  public Map<String, Object> vehicle(Map<String, Object> row) {
    var out = normalized(row);
    return out;
  }

  public Map<String, Object> profile(Map<String, Object> row) {
    var out = normalized(row);
    if (row.containsKey("services")) {
      out.put("services", Sql.array(row, "services"));
    }
    return out;
  }

  public Map<String, Object> notification(Map<String, Object> row) {
    return normalized(row);
  }

  private Map<String, Object> normalized(Map<String, Object> row) {
    var out = new LinkedHashMap<String, Object>();
    for (var entry : row.entrySet()) {
      out.put(entry.getKey(), normalize(entry.getValue()));
    }
    return out;
  }

  private Object normalize(Object value) {
    if (value instanceof java.sql.Timestamp timestamp) {
      return timestamp.toInstant().toString();
    }
    if (value instanceof java.sql.Array array) {
      try {
        return List.of((Object[]) array.getArray()).stream().map(String::valueOf).toList();
      } catch (Exception error) {
        throw new IllegalStateException(error);
      }
    }
    return value;
  }
}
