package app.roadrescue.tools;

import app.roadrescue.config.EnvFiles;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class DatabaseSchemaSetup {
  private static final String LOCAL_SCHEMA = "roadrescue_local";
  private static final String PROD_SCHEMA = "roadrescue_prod";
  private static final BCryptPasswordEncoder PASSWORDS = new BCryptPasswordEncoder(10);

  private DatabaseSchemaSetup() {
  }

  public static void main(String[] args) throws Exception {
    var databaseUrl = EnvFiles.value("DATABASE_URL", "");
    if (databaseUrl.isBlank()) {
      throw new IllegalStateException("DATABASE_URL is required in .env or environment variables.");
    }

    var parsed = parse(databaseUrl);
    try (var connection = DriverManager.getConnection(parsed.jdbcUrl(), parsed.username(), parsed.password())) {
      createSchema(connection, LOCAL_SCHEMA);
      createSchema(connection, PROD_SCHEMA);
      applySchemaIfNeeded(connection, LOCAL_SCHEMA);
      applySchemaIfNeeded(connection, PROD_SCHEMA);
      seedLocalDemoUsers(connection);
    }

    System.out.println("Database setup complete.");
    System.out.println("- Created/verified schemas: " + LOCAL_SCHEMA + ", " + PROD_SCHEMA);
    System.out.println("- Seeded one demo user for each role in " + LOCAL_SCHEMA);
    System.out.println("- Left " + PROD_SCHEMA + " schema-only for production data");
  }

  private static void createSchema(Connection connection, String schema) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"");
    }
  }

  private static void applySchemaIfNeeded(Connection connection, String schema) throws Exception {
    if (userTableExists(connection, schema)) {
      System.out.println("Schema already initialized: " + schema);
      return;
    }

    System.out.println("Applying database structure to schema: " + schema);
    var migrationSql = readMigrationSql();
    try (var statement = connection.createStatement()) {
      statement.execute("SET search_path TO \"" + schema + "\"");
      for (var sql : splitSql(migrationSql)) {
        if (!sql.isBlank()) {
          statement.execute(sql);
        }
      }
    }
  }

  private static boolean userTableExists(Connection connection, String schema) throws Exception {
    try (var statement = connection.prepareStatement("SELECT to_regclass(?)")) {
      statement.setString(1, "\"" + schema + "\".\"User\"");
      try (var result = statement.executeQuery()) {
        return result.next() && result.getString(1) != null;
      }
    }
  }

  private static void seedLocalDemoUsers(Connection connection) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute("SET search_path TO \"" + LOCAL_SCHEMA + "\"");
    }

    var adminId = upsertUser(connection, "demo-admin-user", "Ayesha Admin", "admin@roadrescue.app", "+91 9999000001", "ADMIN", "Admin@123", true);
    var customerId = upsertUser(connection, "demo-customer-user", "Riya Sharma", "riya@roadrescue.app", "+91 9999000002", "CUSTOMER", "Customer@123", true);
    var mechanicId = upsertUser(connection, "demo-mechanic-user", "Arun Mobile Mechanic", "arun@roadrescue.app", "+91 9999000003", "MECHANIC", "Mechanic@123", true);
    var garageId = upsertUser(connection, "demo-garage-user", "Metro Auto Garage", "metro@roadrescue.app", "+91 9999000004", "GARAGE_OWNER", "Garage@123", true);

    upsertVehicle(connection, customerId);
    upsertMechanicProfile(connection, mechanicId);
    upsertGarageProfile(connection, garageId);
    upsertNotification(connection, "demo-notification-admin", adminId, "SYSTEM", "Platform seeded", "Demo local schema ready.");
    upsertNotification(connection, "demo-notification-customer", customerId, "SYSTEM", "Welcome to RoadRescue", "Your local demo account is ready.");
    upsertNotification(connection, "demo-notification-mechanic", mechanicId, "SYSTEM", "Provider profile ready", "Your mechanic demo profile is online.");
    upsertNotification(connection, "demo-notification-garage", garageId, "SYSTEM", "Garage profile ready", "Your garage demo profile is online.");
  }

  private static String upsertUser(
      Connection connection,
      String id,
      String name,
      String email,
      String phone,
      String role,
      String password,
      boolean verified
  ) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO "User" ("id","name","email","passwordHash","phone","role","isVerified","updatedAt")
        VALUES (?,?,?,?,?,CAST(? AS "Role"),?,CURRENT_TIMESTAMP)
        ON CONFLICT ("email") DO UPDATE SET
          "name" = EXCLUDED."name",
          "passwordHash" = EXCLUDED."passwordHash",
          "phone" = EXCLUDED."phone",
          "role" = EXCLUDED."role",
          "isVerified" = EXCLUDED."isVerified",
          "updatedAt" = CURRENT_TIMESTAMP
        RETURNING "id"
        """)) {
      statement.setString(1, id);
      statement.setString(2, name);
      statement.setString(3, email);
      statement.setString(4, PASSWORDS.encode(password));
      statement.setString(5, phone);
      statement.setString(6, role);
      statement.setBoolean(7, verified);
      try (var result = statement.executeQuery()) {
        result.next();
        return result.getString(1);
      }
    }
  }

  private static void upsertVehicle(Connection connection, String customerId) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO "Vehicle" ("id","userId","brand","model","plateNumber","year","color","fuelType","updatedAt")
        VALUES (?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
        ON CONFLICT ("id") DO UPDATE SET
          "userId" = EXCLUDED."userId",
          "brand" = EXCLUDED."brand",
          "model" = EXCLUDED."model",
          "plateNumber" = EXCLUDED."plateNumber",
          "year" = EXCLUDED."year",
          "color" = EXCLUDED."color",
          "fuelType" = EXCLUDED."fuelType",
          "updatedAt" = CURRENT_TIMESTAMP
        """)) {
      statement.setString(1, customerId + "-demo-vehicle");
      statement.setString(2, customerId);
      statement.setString(3, "Hyundai");
      statement.setString(4, "i20");
      statement.setString(5, "DL8CAF1024");
      statement.setInt(6, 2022);
      statement.setString(7, "White");
      statement.setString(8, "Petrol");
      statement.executeUpdate();
    }
  }

  private static void upsertMechanicProfile(Connection connection, String mechanicId) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO "MechanicProfile"
          ("id","userId","bio","services","serviceRadiusKm","currentLat","currentLng","workingHours","isOnline","baseRate","yearsExperience","avgRating","totalEarnings")
        VALUES (?,?,?,ARRAY['MOBILE_MECHANIC','BATTERY','EMERGENCY']::"ServiceType"[],?,?,?,?,?,?,?,?,?)
        ON CONFLICT ("userId") DO UPDATE SET
          "bio" = EXCLUDED."bio",
          "services" = EXCLUDED."services",
          "serviceRadiusKm" = EXCLUDED."serviceRadiusKm",
          "currentLat" = EXCLUDED."currentLat",
          "currentLng" = EXCLUDED."currentLng",
          "workingHours" = EXCLUDED."workingHours",
          "isOnline" = EXCLUDED."isOnline",
          "baseRate" = EXCLUDED."baseRate",
          "yearsExperience" = EXCLUDED."yearsExperience",
          "avgRating" = EXCLUDED."avgRating",
          "totalEarnings" = EXCLUDED."totalEarnings"
        """)) {
      statement.setString(1, "demo-mechanic-profile");
      statement.setString(2, mechanicId);
      statement.setString(3, "Rapid response roadside mechanic for batteries, flats, and overheating.");
      statement.setDouble(4, 18);
      statement.setDouble(5, 28.624);
      statement.setDouble(6, 77.218);
      statement.setString(7, "24/7");
      statement.setBoolean(8, true);
      statement.setDouble(9, 20);
      statement.setInt(10, 7);
      statement.setDouble(11, 4.8);
      statement.setDouble(12, 12450);
      statement.executeUpdate();
    }
  }

  private static void upsertGarageProfile(Connection connection, String garageId) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO "GarageProfile"
          ("id","userId","businessName","services","serviceRadiusKm","locationLat","locationLng","workingHours","isOnline","address","avgRating","totalEarnings","baseRate","phoneNumber")
        VALUES (?,?,?,ARRAY['GARAGE','TOWING','EMERGENCY']::"ServiceType"[],?,?,?,?,?,?,?,?,?,?)
        ON CONFLICT ("userId") DO UPDATE SET
          "businessName" = EXCLUDED."businessName",
          "services" = EXCLUDED."services",
          "serviceRadiusKm" = EXCLUDED."serviceRadiusKm",
          "locationLat" = EXCLUDED."locationLat",
          "locationLng" = EXCLUDED."locationLng",
          "workingHours" = EXCLUDED."workingHours",
          "isOnline" = EXCLUDED."isOnline",
          "address" = EXCLUDED."address",
          "avgRating" = EXCLUDED."avgRating",
          "totalEarnings" = EXCLUDED."totalEarnings",
          "baseRate" = EXCLUDED."baseRate",
          "phoneNumber" = EXCLUDED."phoneNumber"
        """)) {
      statement.setString(1, "demo-garage-profile");
      statement.setString(2, garageId);
      statement.setString(3, "Metro Auto Garage");
      statement.setDouble(4, 25);
      statement.setDouble(5, 28.635);
      statement.setDouble(6, 77.225);
      statement.setString(7, "Mon-Sun, 24/7");
      statement.setBoolean(8, true);
      statement.setString(9, "Connaught Place Service Hub, New Delhi");
      statement.setDouble(10, 4.6);
      statement.setDouble(11, 28300);
      statement.setDouble(12, 30);
      statement.setString(13, "+91 9999000004");
      statement.executeUpdate();
    }
  }

  private static void upsertNotification(Connection connection, String id, String userId, String type, String title, String message) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO "Notification" ("id","userId","type","title","message")
        VALUES (?,?,CAST(? AS "NotificationType"),?,?)
        ON CONFLICT ("id") DO UPDATE SET
          "userId" = EXCLUDED."userId",
          "type" = EXCLUDED."type",
          "title" = EXCLUDED."title",
          "message" = EXCLUDED."message"
        """)) {
      statement.setString(1, id);
      statement.setString(2, userId);
      statement.setString(3, type);
      statement.setString(4, title);
      statement.setString(5, message);
      statement.executeUpdate();
    }
  }

  private static String readMigrationSql() throws Exception {
    try (InputStream input = DatabaseSchemaSetup.class.getResourceAsStream("/db/migration/V1__init.sql")) {
      if (input == null) {
        throw new IllegalStateException("Could not find /db/migration/V1__init.sql");
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static List<String> splitSql(String sql) {
    var statements = new ArrayList<String>();
    var current = new StringBuilder();
    var inSingleQuote = false;
    for (var index = 0; index < sql.length(); index += 1) {
      var value = sql.charAt(index);
      if (value == '\'') {
        inSingleQuote = !inSingleQuote;
      }
      if (value == ';' && !inSingleQuote) {
        statements.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(value);
      }
    }
    if (!current.toString().isBlank()) {
      statements.add(current.toString().trim());
    }
    return statements;
  }

  private static ParsedDatabaseUrl parse(String raw) {
    if (raw.startsWith("jdbc:postgresql:")) {
      return new ParsedDatabaseUrl(stripSchema(raw), null, null);
    }
    var uri = URI.create(raw);
    var params = query(uri.getRawQuery());
    params.remove("schema");
    params.remove("currentSchema");
    var query = params.isEmpty()
        ? ""
        : "?" + String.join("&", params.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
    var jdbcUrl = "jdbc:postgresql://" + uri.getHost() + (uri.getPort() > -1 ? ":" + uri.getPort() : "") + uri.getPath() + query;
    var userInfo = uri.getRawUserInfo() == null ? ":" : uri.getRawUserInfo();
    var split = userInfo.split(":", 2);
    var username = URLDecoder.decode(split[0], StandardCharsets.UTF_8);
    var password = split.length > 1 ? URLDecoder.decode(split[1], StandardCharsets.UTF_8) : "";
    return new ParsedDatabaseUrl(jdbcUrl, username, password);
  }

  private static String stripSchema(String raw) {
    return raw
        .replaceAll("([?&])schema=[^&]*&?", "$1")
        .replaceAll("([?&])currentSchema=[^&]*&?", "$1")
        .replaceAll("[?&]$", "");
  }

  private static Map<String, String> query(String rawQuery) {
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

  private record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
  }
}
