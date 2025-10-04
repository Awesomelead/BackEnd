package kr.co.awesomelead.groupware_backend.auth.util;

import io.jsonwebtoken.Jwts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Component
public class JWTUtil {

    private SecretKey secretKey;

    // application.yml에서 secret key 값을 가져옴
    public JWTUtil(@Value("${spring.jwt.secret}") String secret) {
        // secret 키를 암호화 알고리즘(HS256)에 맞게 생성
        this.secretKey =
            new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    // JWT 토큰에서 사용자 아이디(loginId)를 추출
    public String getUsername(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("username", String.class);
    }

    // JWT 토큰에서 사용자 역할(role)을 추출
    public String getRole(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("role", String.class);
    }

    // JWT 토큰이 만료되었는지 확인
    public Boolean isExpired(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration()
            .before(new Date());
    }

    // JWT 토큰 생성
    public String createJwt(String username, String role, Long expiredMs) {
        return Jwts.builder()
            .claim("username", username) // 사용자 아이디
            .claim("role", role) // 사용자 역할
            .issuedAt(new Date(System.currentTimeMillis())) // 토큰 발행 시간
            .expiration(new Date(System.currentTimeMillis() + expiredMs)) // 토큰 만료 시간
            .signWith(secretKey) // secret key로 서명
            .compact();
    }
}
