package com.poker.service.impl;

import com.poker.dto.InviteCodeDto;
import com.poker.entity.InviteCode;
import com.poker.entity.InviteCode.InviteCodeStatus;
import com.poker.entity.User;
import com.poker.repository.InviteCodeRepository;
import com.poker.service.InviteCodeService;
import com.poker.util.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InviteCodeServiceImpl implements InviteCodeService {

    private static final int MAX_BATCH_SIZE = 50;

    private final InviteCodeRepository inviteCodeRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<InviteCodeDto> listCodes(String status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<InviteCode> result;
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            result = inviteCodeRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            InviteCodeStatus parsed = InviteCodeStatus.valueOf(status.toUpperCase());
            result = inviteCodeRepository.findByStatusOrderByCreatedAtDesc(parsed, pageable);
        }
        return result.map(this::toDto);
    }

    @Override
    public List<String> generateCodes(User admin, int count) {
        int safeCount = Math.min(Math.max(count, 1), MAX_BATCH_SIZE);
        List<String> generated = new ArrayList<>(safeCount);

        for (int i = 0; i < safeCount; i++) {
            String code = generateUniqueCode();
            InviteCode inviteCode = new InviteCode();
            inviteCode.setCode(code);
            inviteCode.setStatus(InviteCodeStatus.UNUSED);
            inviteCode.setCreatedBy(admin);
            inviteCodeRepository.save(inviteCode);
            generated.add(code);
        }

        log.info("Generated {} invite codes by admin={}", safeCount, admin.getUsername());
        return generated;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = InviteCodeGenerator.generate();
            if (!inviteCodeRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new RuntimeException("Failed to generate unique invite code");
    }

    private InviteCodeDto toDto(InviteCode entity) {
        InviteCodeDto dto = new InviteCodeDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setStatus(entity.getStatus().name());
        dto.setCreatedAt(entity.getCreatedAt());
        if (entity.getUsedBy() != null) {
            dto.setUsedByUsername(entity.getUsedBy().getUsername());
        }
        dto.setUsedAt(entity.getUsedAt());
        return dto;
    }
}
