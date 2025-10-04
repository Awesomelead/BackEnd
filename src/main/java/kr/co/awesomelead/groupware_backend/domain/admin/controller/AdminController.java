package kr.co.awesomelead.groupware_backend.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;

import kr.co.awesomelead.groupware_backend.domain.admin.dto.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.service.AdminService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "회원가입 승인", description = "관리자가 회원가입 요청에 대해 승인합니다.")
    @PatchMapping("/users/{userId}/approve")
    public ResponseEntity<String> approveUser(
        @PathVariable("userId") Long userId, @RequestBody UserApprovalRequestDto requestDto) {

        adminService.approveUserRegistration(userId, requestDto);

        return ResponseEntity.ok(userId + "번 사용자의 회원가입이 성공적으로 승인되었습니다.");
    }
}
