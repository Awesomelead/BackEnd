package kr.co.awesomelead.groupware_backend.domain.auth;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.auth.entity.RefreshToken;
import kr.co.awesomelead.groupware_backend.auth.repository.RefreshTokenRepository;
import kr.co.awesomelead.groupware_backend.auth.service.RefreshTokenService;
import kr.co.awesomelead.groupware_backend.auth.util.JWTUtil;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private JWTUtil jwtUtil;

    @InjectMocks private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("새로운 Refresh Token 생성 및 저장 성공 (기존 토큰 없음)")
    void createAndSaveRefreshToken_Success_NoExistingToken() {
        // given
        String email = "test@example.com";
        String role = "ROLE_USER";
        String generatedTokenValue = "new-refresh-token";

        when(jwtUtil.createJwt(anyString(), anyString(), anyLong()))
                .thenReturn(generatedTokenValue);
        when(refreshTokenRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when
        String resultToken = refreshTokenService.createAndSaveRefreshToken(email, role);

        // then
        assertThat(resultToken).isEqualTo(generatedTokenValue);

        // findByEmail은 호출되었지만, delete는 호출되지 않았는지 검증
        verify(refreshTokenRepository, times(1)).findByEmail(email);
        verify(refreshTokenRepository, never()).delete(any());

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();

        assertThat(savedToken.getEmail()).isEqualTo(email);
        assertThat(savedToken.getTokenValue()).isEqualTo(generatedTokenValue);
        assertThat(savedToken.getExpirationDate()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("새로운 Refresh Token 생성 및 저장 성공 (기존 토큰 존재 시 삭제)")
    void createAndSaveRefreshToken_Success_WithExistingToken() {
        // given
        String email = "test@example.com";
        String role = "ROLE_USER";
        String generatedTokenValue = "new-refresh-token";
        RefreshToken existingToken = RefreshToken.builder().email(email).build();

        when(jwtUtil.createJwt(anyString(), anyString(), anyLong()))
                .thenReturn(generatedTokenValue);
        when(refreshTokenRepository.findByEmail(email)).thenReturn(Optional.of(existingToken));

        // when
        String resultToken = refreshTokenService.createAndSaveRefreshToken(email, role);

        // then
        assertThat(resultToken).isEqualTo(generatedTokenValue);

        // findByEmail과 delete가 모두 호출되었는지 검증
        verify(refreshTokenRepository, times(1)).findByEmail(email);
        verify(refreshTokenRepository, times(1)).delete(existingToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh Token 검증 성공")
    void validateRefreshToken_Success() {
        // given
        String tokenValue = "valid-token";
        RefreshToken validToken =
                RefreshToken.builder()
                        .tokenValue(tokenValue)
                        .expirationDate(LocalDateTime.now().plusDays(1)) // 만료되지 않음
                        .build();

        when(refreshTokenRepository.findByTokenValue(tokenValue))
                .thenReturn(Optional.of(validToken));
        // RefreshToken 클래스의 isExpired()가 false를 반환한다고 가정

        // when
        RefreshToken result = refreshTokenService.validateRefreshToken(tokenValue);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTokenValue()).isEqualTo(tokenValue);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Refresh Token 검증 실패 - 존재하지 않는 토큰")
    void validateRefreshToken_Fail_NotFound() {
        // given
        String tokenValue = "non-existent-token";
        when(refreshTokenRepository.findByTokenValue(tokenValue)).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
                assertThrows(
                        CustomException.class,
                        () -> refreshTokenService.validateRefreshToken(tokenValue));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("Refresh Token 검증 실패 - 만료된 토큰")
    void validateRefreshToken_Fail_Expired() {
        // given
        String tokenValue = "expired-token";
        // isExpired() 메서드가 true를 반환하도록 expirationDate를 과거로 설정
        RefreshToken expiredToken =
                RefreshToken.builder()
                        .tokenValue(tokenValue)
                        .expirationDate(LocalDateTime.now().minusDays(1))
                        .build();

        when(refreshTokenRepository.findByTokenValue(tokenValue))
                .thenReturn(Optional.of(expiredToken));

        // when & then
        CustomException exception =
                assertThrows(
                        CustomException.class,
                        () -> refreshTokenService.validateRefreshToken(tokenValue));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXPIRED_TOKEN);

        // 만료된 토큰이므로 delete 메서드가 호출되었는지 검증
        verify(refreshTokenRepository, times(1)).delete(expiredToken);
    }
}
