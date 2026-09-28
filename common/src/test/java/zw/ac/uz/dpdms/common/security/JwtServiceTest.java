package zw.ac.uz.dpdms.common.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-definitely-longer-than-32-bytes";
    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Test
    void recorderRoundTrip() {
        AuthenticatedUser user = new AuthenticatedUser("rec.fire.w3", Role.WARD_RECORDER, HazardType.FIRE, "Ward 3");
        AuthenticatedUser parsed = jwtService.parse(jwtService.generateToken(user));
        assertEquals(user, parsed);
    }

    @Test
    void nationalHasNoHazardOrWard() {
        AuthenticatedUser user = new AuthenticatedUser("nat", Role.NATIONAL_VIEWER, null, null);
        AuthenticatedUser parsed = jwtService.parse(jwtService.generateToken(user));
        assertEquals(Role.NATIONAL_VIEWER, parsed.role());
        assertNull(parsed.hazard());
        assertNull(parsed.ward());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService attacker = new JwtService("a-completely-different-secret-of-32-bytes+", 60);
        String forged = attacker.generateToken(
                new AuthenticatedUser("evil", Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null));
        assertThrows(JwtException.class, () -> jwtService.parse(forged));
    }

    @Test
    void garbageTokenIsRejected() {
        assertThrows(JwtException.class, () -> jwtService.parse("not.a.jwt"));
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60));
    }
}
