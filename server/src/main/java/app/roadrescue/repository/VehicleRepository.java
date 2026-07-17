package app.roadrescue.repository;

import app.roadrescue.dto.UserDtos.VehicleRequest;
import app.roadrescue.util.Ids;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VehicleRepository {
  private final JdbcTemplate jdbc;
  private final UserRepository users;

  public VehicleRepository(JdbcTemplate jdbc, UserRepository users) {
    this.jdbc = jdbc;
    this.users = users;
  }

  public Map<String, Object> create(String userId, VehicleRequest request) {
    var id = Ids.cuid();
    jdbc.update("""
        INSERT INTO "Vehicle" ("id","userId","brand","model","plateNumber","year","color","fuelType","updatedAt")
        VALUES (?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
        """, id, userId, request.brand(), request.model(), request.plateNumber(), request.year(), request.color(), request.fuelType());
    return jdbc.queryForList("SELECT * FROM \"Vehicle\" WHERE \"id\" = ?", id).stream().findFirst().map(users::vehicle).orElseThrow();
  }
}
