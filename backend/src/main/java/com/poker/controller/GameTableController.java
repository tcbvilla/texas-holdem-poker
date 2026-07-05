package com.poker.controller;

import com.poker.auth.AuthService;
import com.poker.entity.User;
import com.poker.game.service.GameTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Live game table endpoints. All operations are scoped to the authenticated user.
 */
@RestController
@RequestMapping("/api/game/rooms/{roomId}")
@RequiredArgsConstructor
@Slf4j
public class GameTableController {

    private final GameTableService gameTableService;
    private final AuthService authService;

    /**
     * Enter the table and buy in. Uses the room default chips when buyin is omitted.
     */
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> join(@PathVariable Long roomId,
                                                    @RequestBody(required = false) JoinRequest request) {
        return execute(() -> {
            User user = authService.getCurrentUser();
            Long buyin = request != null ? request.getBuyin() : null;
            return gameTableService.join(roomId, user, buyin);
        });
    }

    /**
     * Leave the table (stand up and remove from the room).
     */
    @PostMapping("/leave")
    public ResponseEntity<Map<String, Object>> leave(@PathVariable Long roomId) {
        return execute(() -> {
            User user = authService.getCurrentUser();
            return gameTableService.leave(roomId, user);
        });
    }

    /**
     * Add chips between hands.
     */
    @PostMapping("/rebuy")
    public ResponseEntity<Map<String, Object>> rebuy(@PathVariable Long roomId,
                                                     @RequestBody RebuyRequest request) {
        return execute(() -> {
            User user = authService.getCurrentUser();
            return gameTableService.rebuy(roomId, user, request.getAmount());
        });
    }

    /**
     * Start a new hand.
     */
    @PostMapping("/start-hand")
    public ResponseEntity<Map<String, Object>> startHand(@PathVariable Long roomId) {
        return execute(() -> {
            User user = authService.getCurrentUser();
            return gameTableService.startHand(roomId, user);
        });
    }

    /**
     * Start the next hand after a finished one.
     */
    @PostMapping("/next-hand")
    public ResponseEntity<Map<String, Object>> nextHand(@PathVariable Long roomId) {
        return execute(() -> {
            User user = authService.getCurrentUser();
            return gameTableService.startNextHand(roomId, user);
        });
    }

    /**
     * Submit a player action: FOLD / CHECK / CALL / RAISE / ALL_IN.
     */
    @PostMapping("/action")
    public ResponseEntity<Map<String, Object>> action(@PathVariable Long roomId,
                                                      @RequestBody ActionRequest request) {
        return execute(() -> {
            User user = authService.getCurrentUser();
            BigDecimal amount = request.getAmount() != null ? BigDecimal.valueOf(request.getAmount()) : null;
            return gameTableService.processAction(roomId, user, request.getAction(), amount);
        });
    }

    /**
     * Poll the current table state (per-viewer, hides opponents' hole cards).
     */
    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> state(@PathVariable Long roomId) {
        return execute(() -> {
            User user = authService.getCurrentUser();
            return gameTableService.getState(roomId, user);
        });
    }

    private ResponseEntity<Map<String, Object>> execute(java.util.function.Supplier<Map<String, Object>> supplier) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = supplier.get();
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Game table operation failed: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    public static class JoinRequest {
        private Long buyin;

        public Long getBuyin() { return buyin; }
        public void setBuyin(Long buyin) { this.buyin = buyin; }
    }

    public static class RebuyRequest {
        private long amount;

        public long getAmount() { return amount; }
        public void setAmount(long amount) { this.amount = amount; }
    }

    public static class ActionRequest {
        private String action;
        private Long amount;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
    }
}
