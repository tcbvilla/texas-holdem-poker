package com.poker.controller;

import com.poker.auth.AdminAuth;
import com.poker.auth.AuthService;
import com.poker.dto.InviteCodeDto;
import com.poker.entity.User;
import com.poker.service.InviteCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/invite-codes")
@RequiredArgsConstructor
@Slf4j
public class AdminInviteCodeController {

    private final InviteCodeService inviteCodeService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listInviteCodes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = authService.getCurrentUser();
            AdminAuth.requireAdmin(currentUser);

            Page<InviteCodeDto> result = inviteCodeService.listCodes(status, page, size);

            Map<String, Object> data = new HashMap<>();
            data.put("content", result.getContent());
            data.put("page", result.getNumber());
            data.put("size", result.getSize());
            data.put("totalElements", result.getTotalElements());
            data.put("totalPages", result.getTotalPages());

            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to list invite codes", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> generateInviteCodes(@RequestBody GenerateRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = authService.getCurrentUser();
            AdminAuth.requireAdmin(currentUser);

            int count = request.getCount() != null ? request.getCount() : 1;
            List<String> codes = inviteCodeService.generateCodes(currentUser, count);

            Map<String, Object> data = new HashMap<>();
            data.put("codes", codes);

            response.put("success", true);
            response.put("message", "邀请码生成成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to generate invite codes", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    public static class GenerateRequest {
        private Integer count;

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }
}
