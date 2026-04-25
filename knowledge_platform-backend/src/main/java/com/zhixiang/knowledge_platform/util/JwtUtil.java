package com.zhixiang.knowledge_platform.util;

import com.zhixiang.knowledge_platform.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${zhixiang.jwt.secret}")
    private String jwtSecret;

    @Value("${zhixiang.jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${zhixiang.jwt.refresh-expiration}")
    private long jwtRefreshExpirationMs;

    @Value("${zhixiang.file.preview.office-online.token-expiration-ms:300000}")
    private long officePreviewTokenExpirationMs;

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("type", "access");

        return createToken(claims, user.getUsername(), jwtExpirationMs);
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("type", "refresh");

        return createToken(claims, user.getUsername(), jwtRefreshExpirationMs);
    }

    public String generateOfficePreviewToken(Long documentId, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("documentId", documentId);
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("purpose", "office_preview");
        claims.put("type", "preview");
        claims.put("nonce", java.util.UUID.randomUUID().toString());
        return createToken(claims, String.valueOf(documentId), officePreviewTokenExpirationMs);
    }

    /**
     * 创建令牌
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.get("userId") != null) {
            return Long.valueOf(claims.get("userId").toString());
        }
        return null;
    }

    /**
     * 从令牌中获取用户角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("role") : null;
    }

    /**
     * 从令牌中获取令牌类型
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("type") : null;
    }

    public String getStringClaim(String token, String claimName) {
        Claims claims = getClaimsFromToken(token);
        Object value = claims != null ? claims.get(claimName) : null;
        return value != null ? value.toString() : null;
    }

    public Long getLongClaim(String token, String claimName) {
        Claims claims = getClaimsFromToken(token);
        Object value = claims != null ? claims.get(claimName) : null;
        return value != null ? Long.valueOf(value.toString()) : null;
    }

    /**
     * 获取令牌过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * 验证令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证令牌是否有效
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return !isTokenExpired(token);
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT token validation error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 验证访问令牌
     */
    public boolean validateAccessToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        String tokenType = getTokenTypeFromToken(token);
        return "access".equals(tokenType);
    }

    /**
     * 验证刷新令牌
     */
    public boolean validateRefreshToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        String tokenType = getTokenTypeFromToken(token);
        return "refresh".equals(tokenType);
    }

    /**
     * 从令牌中获取声明
     */
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("Failed to parse JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 刷新令牌
     */
    public String refreshAccessToken(String refreshToken, User user) {
        if (validateRefreshToken(refreshToken)) {
            return generateAccessToken(user);
        }
        throw new IllegalArgumentException("Invalid refresh token");
    }

    /**
     * 检查令牌是否即将过期（剩余时间少于5分钟）
     */
    public boolean isTokenNearExpiry(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            if (expiration == null) {
                return true;
            }
            long timeLeft = expiration.getTime() - new Date().getTime();
            return timeLeft < 5 * 60 * 1000; // 5分钟
        } catch (Exception e) {
            return true;
        }
    }
}