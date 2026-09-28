package zw.ac.uz.dpdms.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues (auth-service) and verifies (every service) HMAC-SHA256 signed JWTs.
 * The token carries role, hazard and ward, so each service can enforce scoping without
 * calling the auth-service on every request.
 */
public class JwtService {

    public static final int MIN_SECRET_BYTES = 32;
    public static final String ISSUER = "dpdms-auth-service";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_HAZARD = "hazard";
    public static final String CLAIM_WARD = "ward";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(String secret, long expirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret must be at least " + MIN_SECRET_BYTES + " bytes. Set JWT_SECRET in your .env file.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .subject(user.username())
                .issuer(ISSUER)
                .claim(CLAIM_ROLE, user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)));
        if (user.hazard() != null) {
            builder.claim(CLAIM_HAZARD, user.hazard().name());
        }
        if (user.ward() != null) {
            builder.claim(CLAIM_WARD, user.ward());
        }
        return builder.signWith(key).compact();
    }

    /**
     * Verifies signature, issuer and expiry, then rebuilds the user.
     * Throws io.jsonwebtoken.JwtException or IllegalArgumentException if anything is wrong.
     */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String roleClaim = claims.get(CLAIM_ROLE, String.class);
        if (roleClaim == null) {
            throw new MalformedJwtException("Token has no role claim");
        }
        String hazardClaim = claims.get(CLAIM_HAZARD, String.class);
        return new AuthenticatedUser(
                claims.getSubject(),
                Role.valueOf(roleClaim),
                hazardClaim == null ? null : HazardType.valueOf(hazardClaim),
                claims.get(CLAIM_WARD, String.class));
    }

    public Duration getExpiration() {
        return expiration;
    }
}
