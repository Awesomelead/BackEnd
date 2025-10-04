package kr.co.awesomelead.groupware_backend.domain.admin.dto;

import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserApprovalRequestDto {

    private LocalDate hireDate;
    private String jobType;
    private String position;
    private String workLocation;
    private Role role;
}
