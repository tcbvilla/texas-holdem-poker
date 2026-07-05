package com.poker.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persisted buy-in / cash-out summary when a table session closes.
 */
@Data
public class RoomSettlementSnapshot {
    private LocalDateTime closedAt;
    private List<PlayerSettlement> players = new ArrayList<>();

    @Data
    public static class PlayerSettlement {
        private Integer userId;
        private String username;
        private long totalBuyIn;
        private long remainingChips;
        private long profitLoss;
    }
}
