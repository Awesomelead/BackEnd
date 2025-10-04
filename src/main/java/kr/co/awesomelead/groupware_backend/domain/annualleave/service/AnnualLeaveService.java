package kr.co.awesomelead.groupware_backend.domain.annualleave.service;

import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.annualleave.repository.AnnualLeaveRepository;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnnualLeaveService {

    private final AnnualLeaveRepository annualLeaveRepository;
    private final UserRepository userRepository;

    public AnnualLeave getAnnualLeave(CustomUserDetails userDetails) {
        Long id = userDetails.getId();
        User user =
            userRepository
                .findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return annualLeaveRepository.findByUser(user);
    }
}
