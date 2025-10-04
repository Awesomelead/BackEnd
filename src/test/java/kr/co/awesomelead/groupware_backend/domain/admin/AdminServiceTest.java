package kr.co.awesomelead.groupware_backend.domain.admin;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.service.AdminService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("사용자 등록 승인 성공")
    void approveUserRegistration_Success() {
        // given
        Long userId = 1L;
        User pendingUser = new User(); // 테스트용 User 객체 생성
        pendingUser.setId(userId);
        pendingUser.setStatus(Status.PENDING);

        UserApprovalRequestDto requestDto = new UserApprovalRequestDto();
        requestDto.setHireDate(LocalDate.of(2025, 9, 22));
        requestDto.setJobType("정규직");
        requestDto.setPosition("사원");
        requestDto.setWorkLocation("본사");
        requestDto.setRole(Role.ROLE_USER);

        // userRepository.findById가 호출되면 PENDING 상태의 유저를 반환하도록 설정
        when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));

        // when
        adminService.approveUserRegistration(userId, requestDto);

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // 캡처된 User 객체의 필드가 예상대로 업데이트되었는지 검증
        assertThat(savedUser.getStatus()).isEqualTo(Status.AVAILABLE);
        assertThat(savedUser.getHireDate()).isEqualTo(requestDto.getHireDate());
        assertThat(savedUser.getPosition()).isEqualTo(requestDto.getPosition());
        assertThat(savedUser.getRole()).isEqualTo(requestDto.getRole());
    }

    @Test
    @DisplayName("사용자 등록 승인 실패 - 사용자를 찾을 수 없음")
    void approveUserRegistration_Fail_UserNotFound() {
        // given
        Long userId = 99L;
        UserApprovalRequestDto requestDto = new UserApprovalRequestDto();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
            assertThrows(
                CustomException.class,
                () -> {
                    adminService.approveUserRegistration(userId, requestDto);
                });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(userRepository, never()).save(ArgumentMatchers.any(User.class));
    }

    @Test
    @DisplayName("사용자 등록 승인 실패 - 이미 처리된 요청")
    void approveUserRegistration_Fail_AlreadyProcessed() {
        // given
        Long userId = 1L;
        User availableUser = new User(); // 이미 AVAILABLE 상태인 유저 객체 생성
        availableUser.setId(userId);
        availableUser.setStatus(Status.AVAILABLE);

        UserApprovalRequestDto requestDto = new UserApprovalRequestDto();
        when(userRepository.findById(userId)).thenReturn(Optional.of(availableUser));

        // when & then
        CustomException exception =
            assertThrows(
                CustomException.class,
                () -> {
                    adminService.approveUserRegistration(userId, requestDto);
                });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATED_SIGNUP_REQUEST);

        // 상태가 PENDING이 아니므로 save 메서드가 호출되지 않았는지 검증
        verify(userRepository, never()).save(ArgumentMatchers.any(User.class));
    }
}
