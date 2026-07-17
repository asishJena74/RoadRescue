package app.roadrescue.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

final class DbArrays {
  private DbArrays() {
  }

  static java.sql.Array text(Connection connection, Collection<String> values) throws SQLException {
    return connection.createArrayOf("text", values.toArray(String[]::new));
  }

  static java.sql.Array serviceType(Connection connection, Collection<String> values) throws SQLException {
    return connection.createArrayOf("ServiceType", values.toArray(String[]::new));
  }
}
