package kr.co.awesomelead.groupware_backend.domain.annualleave;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.annualleave.repository.AnnualLeaveRepository;
import kr.co.awesomelead.groupware_backend.domain.annualleave.service.AnnualLeaveService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class) // Mockito 확장 기능을 JUnit 5와 통합
@ActiveProfiles("test")
public class AnnualLeaveServiceTest {

    @Mock
    private AnnualLeaveRepository annualLeaveRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnnualLeaveService annualLeaveService;

    private CustomUserDetails customUserDetails;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setNameKor("testName");

        customUserDetails = new CustomUserDetails(user); // CustomUserDetails 생성자 가정
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void getAnnualLeave_Success() {
        // given
        AnnualLeave annualLeave = new AnnualLeave();
        annualLeave.setUser(user);
        annualLeave.setTotal(15);
        user.setAnnualLeave(annualLeave);

        when(userRepository.findById(customUserDetails.getId())).thenReturn(Optional.of(user));
        when(annualLeaveRepository.findByUser(user)).thenReturn(annualLeave);

        // when
        AnnualLeave result = annualLeaveService.getAnnualLeave(customUserDetails);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(15);
        assertThat(result.getUser().getNameKor()).isEqualTo("testName");
        verify(userRepository, times(1)).findById(1L);
        verify(annualLeaveRepository, times(1)).findByUser(user);
    }

    @Test
    @DisplayName("연차 정보 조회 실패 - 사용자를 찾을 수 없음")
    void getAnnualLeave_Fail_UserNotFound() {
        // given
        when(userRepository.findById(customUserDetails.getId())).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
            assertThrows(
                CustomException.class,
                () -> {
                    annualLeaveService.getAnnualLeave(customUserDetails);
                });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(annualLeaveRepository, never()).findByUser(any(User.class));
    }
}
