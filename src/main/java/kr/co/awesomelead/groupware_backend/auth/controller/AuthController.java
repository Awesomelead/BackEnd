package kr.co.awesomelead.groupware_backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Iterator;
import kr.co.awesomelead.groupware_backend.auth.dto.LoginRequestDto;
import kr.co.awesomelead.groupware_backend.auth.dto.LoginResponseDto;
import kr.co.awesomelead.groupware_backend.auth.service.RefreshTokenService;
import kr.co.awesomelead.groupware_backend.auth.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "로그인, 로그아웃 관련 API")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "로그인", description = "로그인을 합니다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
        @RequestBody LoginRequestDto requestDto, HttpServletResponse response) {

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                requestDto.getEmail(), requestDto.getPassword(), null);

        Authentication authentication = authenticationManager.authenticate(authToken);

        String username = authentication.getName();

        // 사용자의 역할(Role) 정보 추출
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        // JWTUtil을 사용하여 Access Token 생성 (유효기간 1시간으로 설정)
        String accessToken = jwtUtil.createJwt(username, role, 60 * 60 * 1000L);
        String refreshToken = refreshTokenService.createAndSaveRefreshToken(username, role);

        // 리프레쉬 토큰을 HttpOnly 쿠키에 담아 응답
        response.addCookie(createCookie("refresh", refreshToken));

        // JWT를 DTO에 담아 응답
        return ResponseEntity.ok(new LoginResponseDto(accessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh")) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken != null) {
            // DB에서 Refresh Token 삭제
            refreshTokenService.deleteRefreshToken(refreshToken);
        }

        // 클라이언트 측의 쿠키도 만료시켜서 삭제하도록 응답 설정
        Cookie cookie = new Cookie("refresh", null); // value를 null로 설정
        cookie.setMaxAge(0); // 유효기간을 0으로 만들어 즉시 만료
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60); // 쿠키 유효기간 24시간
        // cookie.setSecure(true); // HTTPS 통신에서만 쿠키 전송
        cookie.setPath("/"); // 모든 경로에서 쿠키 접근 가능
        cookie.setHttpOnly(true); // JavaScript가 쿠키에 접근 불가 (XSS 방지)
        return cookie;
    }
}
