package app.roadrescue.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class EnvFiles {
  private static Map<String, String> cached;

  private EnvFiles() {
  }

  public static String value(String key, String fallback) {
    var env = System.getenv(key);
    if (env != null && !env.isBlank()) {
      return env;
    }
    return fileValues().getOrDefault(key, fallback);
  }

  private static Map<String, String> fileValues() {
    if (cached != null) {
      return cached;
    }
    cached = new HashMap<>();
    for (var path : new Path[] { Path.of(".env"), Path.of("..", ".env"), Path.of("server", ".env") }) {
      if (!Files.exists(path)) {
        continue;
      }
      try {
        for (var line : Files.readAllLines(path)) {
          var trimmed = line.trim();
          if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.contains("=")) {
            continue;
          }
          var split = trimmed.split("=", 2);
          cached.putIfAbsent(split[0].trim(), split[1].trim());
        }
      } catch (Exception ignored) {
        // Spring can still use real environment variables if the local file cannot be read.
      }
    }
    return cached;
  }
}
