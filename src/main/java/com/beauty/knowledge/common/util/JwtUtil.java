package com.beauty.knowledge.common.util;

import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String CLAIM_ROLE = "role";

    @Value("${beauty.jwt.secret}")
    private String secret;

    @Value("${beauty.jwt.expire-in-seconds:86400}")
    private Long expireInSeconds;

    public String generateToken(Long userId, String role) {
        SecretKey secretKey = getSecretKey();
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expireInSeconds);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            SecretKey secretKey = getSecretKey();
            Jws<Claims> claimsJws = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return claimsJws.getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Invalid token");
        }
    }

    public boolean validate(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (BusinessException ex) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        try {
            return Long.parseLong(claims.getSubject());
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Invalid user id in token");
        }
    }

    public String getRole(String token) {
        Claims claims = parseToken(token);
        Object role = claims.get(CLAIM_ROLE);
        if (role == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Role not found in token");
        }
        return String.valueOf(role);
    }

    public long getRemainingExpire(String token) {
        Claims claims = parseToken(token);
        long seconds = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(seconds, 0L);
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "beauty.jwt.secret must be at least 32 characters");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
