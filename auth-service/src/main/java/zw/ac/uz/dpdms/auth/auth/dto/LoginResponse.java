package zw.ac.uz.dpdms.auth.auth.dto;

import zw.ac.uz.dpdms.auth.user.dto.UserResponse;

/** accessToken goes in the header of every later request: "Authorization: Bearer &lt;accessToken&gt;". */
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
}
