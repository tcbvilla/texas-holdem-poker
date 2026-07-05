package com.poker.service;

import com.poker.dto.InviteCodeDto;
import com.poker.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InviteCodeService {

    Page<InviteCodeDto> listCodes(String status, int page, int size);

    List<String> generateCodes(User admin, int count);
}
