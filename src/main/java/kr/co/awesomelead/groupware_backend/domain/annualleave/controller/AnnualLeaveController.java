package kr.co.awesomelead.groupware_backend.domain.annualleave.controller;

import io.swagger.v3.oas.annotations.Operation;

import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.annualleave.service.AnnualLeaveService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/annualleave")
@RequiredArgsConstructor
public class AnnualLeaveController {

    private final AnnualLeaveService annualLeaveService;

    @Operation(summary = "연차 조회", description = "연차 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<AnnualLeave> getAnnualLeave(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(annualLeaveService.getAnnualLeave(userDetails));
    }
}
