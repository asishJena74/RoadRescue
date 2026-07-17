package app.roadrescue.config;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {
  @Bean
  DataSource dataSource(@Value("${DATABASE_URL:}") String databaseUrl) {
    var configuredUrl = EnvFiles.value("DATABASE_URL", databaseUrl);
    var parsed = parse(configuredUrl);
    var dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(parsed.url());
    dataSource.setUsername(parsed.username());
    dataSource.setPassword(parsed.password());
    return dataSource;
  }

  private ParsedDatabaseUrl parse(String raw) {
    if (raw.startsWith("jdbc:postgresql:")) {
      return new ParsedDatabaseUrl(raw, null, null);
    }
    var uri = URI.create(raw);
    var params = query(uri.getRawQuery());
    if (params.containsKey("schema")) {
      params.put("currentSchema", params.remove("schema"));
    }
    var query = params.isEmpty()
        ? ""
        : "?" + params.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).reduce((a, b) -> a + "&" + b).orElse("");
    var jdbcUrl = "jdbc:postgresql://" + uri.getHost() + (uri.getPort() > -1 ? ":" + uri.getPort() : "") + uri.getPath() + query;
    var userInfo = uri.getRawUserInfo() == null ? ":" : uri.getRawUserInfo();
    var split = userInfo.split(":", 2);
    var username = URLDecoder.decode(split[0], StandardCharsets.UTF_8);
    var password = split.length > 1 ? URLDecoder.decode(split[1], StandardCharsets.UTF_8) : "";
    return new ParsedDatabaseUrl(jdbcUrl, username, password);
  }

  private Map<String, String> query(String rawQuery) {
    var params = new LinkedHashMap<String, String>();
    if (rawQuery == null || rawQuery.isBlank()) {
      return params;
    }
    for (var item : rawQuery.split("&")) {
      var split = item.split("=", 2);
      params.put(split[0], split.length > 1 ? split[1] : "");
    }
    return params;
  }

  private record ParsedDatabaseUrl(String url, String username, String password) {
  }
}
