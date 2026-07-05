package com.poker.game.service;

import com.poker.dto.RoomSettlementSnapshot;
import com.poker.entity.Room;
import com.poker.entity.User;
import com.poker.game.core.HandEvaluator;
import com.poker.game.core.PotManager;
import com.poker.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all live in-memory poker tables keyed by the persisted room id.
 * This is the bridge between the REST/room layer and the pure game engine.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameTableService {

    private final RoomService roomService;
    private final PotManager potManager;
    private final HandEvaluator handEvaluator;

    private final Map<Long, GameTable> tables = new ConcurrentHashMap<>();

    private GameTable getOrCreateTable(Long roomId) {
        return tables.computeIfAbsent(roomId, id -> {
            Room room = roomService.getRoomById(id);
            if (room.getStatus() == Room.RoomStatus.CANCELLED
                    || room.getStatus() == Room.RoomStatus.FINISHED) {
                throw new IllegalStateException("This room is closed and cannot be played");
            }
            log.info("Creating in-memory table for room {}", id);
            return new GameTable(room, potManager, handEvaluator);
        });
    }

    public Map<String, Object> join(Long roomId, User user, Long buyin) {
        GameTable table = getOrCreateTable(roomId);
        long amount = (buyin != null) ? buyin : table.getDefaultChips();
        table.join(user.getId().intValue(), user.getUsername(), amount);
        return afterTableMutation(roomId, table, user);
    }

    public Map<String, Object> leave(Long roomId, User user) {
        GameTable table = tables.get(roomId);
        if (table != null) {
            table.leave(user.getId().intValue());
            handleEmptyOrClose(roomId, table);
            Map<String, Object> state = table.buildStateView(user.getId().intValue());
            if (!tables.containsKey(roomId)) {
                state.put("roomClosed", true);
            }
            return state;
        }
        return null;
    }

    public Map<String, Object> rebuy(Long roomId, User user, long amount) {
        GameTable table = getTableOrThrow(roomId);
        table.rebuy(user.getId().intValue(), amount);
        return afterTableMutation(roomId, table, user);
    }

    public Map<String, Object> startHand(Long roomId, User user) {
        GameTable table = getTableOrThrow(roomId);
        if (!table.isMember(user.getId().intValue())) {
            throw new IllegalStateException("Only seated players can start a hand");
        }
        onFirstHand(roomId, table);
        table.startHand();
        return afterTableMutation(roomId, table, user);
    }

    public Map<String, Object> processAction(Long roomId, User user, String action, BigDecimal amount) {
        GameTable table = getTableOrThrow(roomId);
        table.processAction(user.getId().intValue(), action, amount);
        return afterTableMutation(roomId, table, user);
    }

    public Map<String, Object> startNextHand(Long roomId, User user) {
        GameTable table = getTableOrThrow(roomId);
        table.startNextHand();
        return afterTableMutation(roomId, table, user);
    }

    public Map<String, Object> getState(Long roomId, User user) {
        GameTable table = getOrCreateTable(roomId);
        checkRoomDuration(roomId, table);
        Map<String, Object> state = table.buildStateView(user.getId().intValue());
        handlePendingRoomClose(roomId, table);
        return state;
    }

    public boolean isHandInProgress(Long roomId) {
        GameTable table = tables.get(roomId);
        return table != null && table.isHandInProgress();
    }

    public void closeTable(Long roomId) {
        GameTable table = tables.get(roomId);
        if (table != null) {
            if (table.hasSessionLedger()) {
                RoomSettlementSnapshot snapshot = table.exportSettlement();
                roomService.saveSettlement(roomId, snapshot);
            }
            tables.remove(roomId);
            log.info("Closed in-memory table for room {}", roomId);
        }
    }

    /**
     * Close table without persisting settlement (used when restarting an already-closed room).
     */
    public void discardTable(Long roomId) {
        GameTable removed = tables.remove(roomId);
        if (removed != null) {
            log.info("Discarded in-memory table for room {} without saving settlement", roomId);
        }
    }

    private Map<String, Object> afterTableMutation(Long roomId, GameTable table, User user) {
        checkRoomDuration(roomId, table);
        Map<String, Object> state = table.buildStateView(user.getId().intValue());
        handleEmptyOrClose(roomId, table);
        handlePendingRoomClose(roomId, table);
        return state;
    }

    private void onFirstHand(Long roomId, GameTable table) {
        if (table.needsRoomStart()) {
            roomService.markRoomRunning(roomId);
            table.markRoomStarted();
        }
    }

    private void handleEmptyOrClose(Long roomId, GameTable table) {
        if (table.isEmpty()) {
            roomService.autoCancelRoom(roomId);
            closeTable(roomId);
            log.info("Table empty, room {} auto-cancelled", roomId);
        }
    }

    private void handlePendingRoomClose(Long roomId, GameTable table) {
        if (table.consumePendingRoomClose()) {
            roomService.autoEndRoom(roomId);
            closeTable(roomId);
            log.info("Room {} closed after pending duration expiry", roomId);
        }
    }

    private void checkRoomDuration(Long roomId, GameTable table) {
        Room room = roomService.getRoomById(roomId);
        if (room.getStatus() != Room.RoomStatus.RUNNING
                || room.getStartedAt() == null
                || room.getDurationMinutes() == null) {
            return;
        }
        long minutes = Duration.between(room.getStartedAt(), LocalDateTime.now()).toMinutes();
        if (minutes >= room.getDurationMinutes()) {
            if (table.isHandInProgress()) {
                table.requestRoomClose();
                log.info("Room {} duration expired, will close after current hand", roomId);
            } else {
                roomService.autoEndRoom(roomId);
                closeTable(roomId);
                log.info("Room {} duration expired and closed", roomId);
            }
        }
    }

    private GameTable getTableOrThrow(Long roomId) {
        GameTable table = tables.get(roomId);
        if (table == null) {
            throw new IllegalStateException("No active table for this room, please enter the room first");
        }
        return table;
    }
}
