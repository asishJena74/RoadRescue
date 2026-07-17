package app.roadrescue.security;

import app.roadrescue.model.AuthUser;
import app.roadrescue.model.Role;
import app.roadrescue.config.EnvFiles;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;

  public JwtService(@Value("${roadrescue.jwt-secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(EnvFiles.value("JWT_SECRET", secret).getBytes(StandardCharsets.UTF_8));
  }

  public String sign(AuthUser user) {
    var now = Instant.now();
    return Jwts.builder()
        .claim("userId", user.userId())
        .claim("role", user.role().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(7 * 24 * 60 * 60)))
        .signWith(key)
        .compact();
  }

  public AuthUser verify(String token) {
    var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    return new AuthUser(String.valueOf(claims.get("userId")), Role.valueOf(String.valueOf(claims.get("role"))));
  }
}
