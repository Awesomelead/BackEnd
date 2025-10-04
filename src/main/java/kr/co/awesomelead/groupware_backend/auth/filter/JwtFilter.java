package kr.co.awesomelead.groupware_backend.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import kr.co.awesomelead.groupware_backend.auth.util.JWTUtil;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        // 1. 요청 헤더에서 Authorization 헤더를 찾음
        String authorization = request.getHeader("Authorization");

        // 2. Authorization 헤더가 없거나, 'Bearer '로 시작하지 않으면 다음 필터로 넘김
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 'Bearer ' 부분을 잘라내어 순수 토큰만 획득
        String token = authorization.split(" ")[1];

        // 4. 토큰이 만료되었는지 확인. 만료되었다면 다음 필터로 넘김 (어차피 뒤에서 걸림)
        if (jwtUtil.isExpired(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 5. 토큰에서 username과 role을 획득
        String username = jwtUtil.getUsername(token);
        userRepository
            .findByEmail(username)
            .ifPresent(
                user -> {
                    // 사용자의 상태가 'AVAILABLE'이 아니면 인증하지 않음
                    if (user.getStatus() != Status.AVAILABLE) {
                        return; // 인증 객체를 SecurityContext에 저장하지 않고 메소드 종료
                    }

                    // 조회된 실제 사용자 정보로 UserDetails 객체 생성
                    CustomUserDetails customUserDetails = new CustomUserDetails(user);

                    // Spring Security 인증 토큰 생성
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            customUserDetails,
                            null,
                            customUserDetails.getAuthorities());

                    // SecurityContext에 인증 정보 저장
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                });

        filterChain.doFilter(request, response);
    }
}
