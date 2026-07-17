package app.roadrescue.repository;

import java.sql.Array;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Sql {
  private Sql() {
  }

  static String text(Map<String, Object> row, String key) {
    var value = row.get(key);
    return value == null ? null : String.valueOf(value);
  }

  static Double decimal(Map<String, Object> row, String key) {
    var value = row.get(key);
    return value == null ? null : ((Number) value).doubleValue();
  }

  static Integer integer(Map<String, Object> row, String key) {
    var value = row.get(key);
    return value == null ? null : ((Number) value).intValue();
  }

  static Boolean bool(Map<String, Object> row, String key) {
    return (Boolean) row.get(key);
  }

  static Object instant(Map<String, Object> row, String key) {
    var value = row.get(key);
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }
    return value;
  }

  public static List<String> array(Map<String, Object> row, String key) {
    var value = row.get(key);
    if (value == null) {
      return List.of();
    }
    if (value instanceof Array sqlArray) {
      try {
        return Arrays.stream((Object[]) sqlArray.getArray()).map(String::valueOf).toList();
      } catch (Exception error) {
        throw new IllegalStateException(error);
      }
    }
    if (value instanceof String[] strings) {
      return List.of(strings);
    }
    return List.of();
  }
}
