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

import java.time.LocalDateTime;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JWTUtil jwtUtil;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("Refresh Token 생성 성공 - 기존 토큰 없음")
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
    @DisplayName("Refresh Token 생성 성공 - 기존 토큰 존재 시 갱신")
    void createAndSaveRefreshToken_Success_WithExistingToken() {
        // given
        String email = "test@example.com";
        String role = "ROLE_USER";
        String newGeneratedTokenValue = "new-refresh-token";

        RefreshToken existingToken = RefreshToken.builder()
            .email(email)
            .tokenValue("old-refresh-token") // 이전 값
            .expirationDate(LocalDateTime.now().minusDays(1)) // 이전 값
            .build();

        when(jwtUtil.createJwt(anyString(), anyString(), anyLong()))
            .thenReturn(newGeneratedTokenValue);
        when(refreshTokenRepository.findByEmail(email)).thenReturn(Optional.of(existingToken));

        // when
        String resultToken = refreshTokenService.createAndSaveRefreshToken(email, role);

        // then
        assertThat(resultToken).isEqualTo(newGeneratedTokenValue);
        verify(refreshTokenRepository, times(1)).findByEmail(email);

        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository, never()).save(any());
        assertThat(existingToken.getTokenValue()).isEqualTo(newGeneratedTokenValue);
        assertThat(existingToken.getExpirationDate()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("로그아웃 성공 - 토큰 존재 시 삭제")
    void deleteRefreshToken_Success_WhenTokenExists() {
        // given
        String tokenValue = "existing-token";
        RefreshToken existingToken = RefreshToken.builder().tokenValue(tokenValue).build();

        // findByTokenValue가 호출되면, existingToken을 포함한 Optional을 반환하도록 설정
        when(refreshTokenRepository.findByTokenValue(tokenValue)).thenReturn(
            Optional.of(existingToken));

        // when
        refreshTokenService.deleteRefreshToken(tokenValue);

        // then
        // findByTokenValue가 1번 호출되었는지 확인
        verify(refreshTokenRepository, times(1)).findByTokenValue(tokenValue);
        // delete 메소드가 existingToken 객체를 인자로 하여 1번 호출되었는지 확인
        verify(refreshTokenRepository, times(1)).delete(existingToken);
    }

    @Test
    @DisplayName("로그아웃 성공 - 토큰이 존재하지 않아도 에러 없음")
    void deleteRefreshToken_Success_WhenTokenDoesNotExist() {
        // given
        String tokenValue = "non-existent-token";

        // findByTokenValue가 호출되면, 비어있는 Optional을 반환하도록 설정
        when(refreshTokenRepository.findByTokenValue(tokenValue)).thenReturn(Optional.empty());

        // when
        refreshTokenService.deleteRefreshToken(tokenValue);

        // then
        // findByTokenValue는 호출되지만,
        verify(refreshTokenRepository, times(1)).findByTokenValue(tokenValue);
        // ifPresent의 조건이 거짓이므로 delete 메소드는 절대 호출되지 않았는지 확인
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
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
