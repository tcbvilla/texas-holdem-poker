package com.poker.game.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 游戏房间
 * 管理房间成员、游戏状态和筹码
 */
@Component
@Scope("prototype")
@Slf4j
@Getter
public class GameRoom {

    /**
     * 玩家状态
     */
    public enum PlayerStatus {
        IN_ROOM,        // 在房间中，未入座
        SEATED,         // 已入座，可以参与游戏
        LEFT_ROOM       // 已离开房间
    }

    /**
     * 房间成员信息
     */
    public static class RoomMember {
        private final int id;
        private final String name;
        private BigDecimal chips;
        private BigDecimal pendingChips; // 待生效的补码
        private PlayerStatus status;
        private LocalDateTime joinTime;
        private LocalDateTime lastSeatTime;

        public RoomMember(int id, String name, BigDecimal initialChips) {
            this.id = id;
            this.name = name;
            this.chips = initialChips;
            this.pendingChips = BigDecimal.ZERO;
            this.status = PlayerStatus.IN_ROOM;
            this.joinTime = LocalDateTime.now();
        }

        // Getters and Setters
        public int getId() { return id; }
        public String getName() { return name; }
        public BigDecimal getChips() { return chips; }
        public void setChips(BigDecimal chips) { this.chips = chips; }
        public BigDecimal getPendingChips() { return pendingChips; }
        public void setPendingChips(BigDecimal pendingChips) { this.pendingChips = pendingChips; }
        public PlayerStatus getStatus() { return status; }
        public void setStatus(PlayerStatus status) { this.status = status; }
        public LocalDateTime getJoinTime() { return joinTime; }
        public LocalDateTime getLastSeatTime() { return lastSeatTime; }
        public void setLastSeatTime(LocalDateTime lastSeatTime) { this.lastSeatTime = lastSeatTime; }
    }

    /**
     * 游戏历史记录
     */
    public static class GameHistory {
        private final String gameId;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final List<Integer> participants;
        private final Map<Integer, BigDecimal> finalChips;
        private final String winner;
        private final BigDecimal potAmount;

        public GameHistory(String gameId, LocalDateTime startTime, LocalDateTime endTime,
                          List<Integer> participants, Map<Integer, BigDecimal> finalChips,
                          String winner, BigDecimal potAmount) {
            this.gameId = gameId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.participants = participants;
            this.finalChips = finalChips;
            this.winner = winner;
            this.potAmount = potAmount;
        }

        // Getters
        public String getGameId() { return gameId; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public List<Integer> getParticipants() { return participants; }
        public Map<Integer, BigDecimal> getFinalChips() { return finalChips; }
        public String getWinner() { return winner; }
        public BigDecimal getPotAmount() { return potAmount; }
    }

    private final String roomId;
    private final LocalDateTime createTime;
    private final ObjectProvider<GameEngine> gameEngineProvider;

    // 成员管理
    private final Map<Integer, RoomMember> allMembers = new HashMap<>();
    private final List<GameHistory> gameHistory = new ArrayList<>();

    // 游戏管理
    private GameEngine currentGame;
    private boolean gameInProgress = false;
    private LocalDateTime currentGameStartTime;

    public GameRoom(ObjectProvider<GameEngine> gameEngineProvider) {
        this.roomId = "room-" + System.currentTimeMillis();
        this.createTime = LocalDateTime.now();
        this.gameEngineProvider = gameEngineProvider;
    }

    /**
     * 加入房间
     */
    public void joinRoom(int playerId, String playerName, BigDecimal initialChips) {
        if (allMembers.containsKey(playerId)) {
            throw new IllegalArgumentException("玩家已存在于房间中");
        }

        RoomMember member = new RoomMember(playerId, playerName, initialChips);
        allMembers.put(playerId, member);
        log.info("玩家{}加入房间{}，初始筹码：{}", playerName, roomId, initialChips);
    }

    /**
     * 入座
     */
    public void takeSeat(int playerId) {
        RoomMember member = allMembers.get(playerId);
        if (member == null) {
            throw new IllegalArgumentException("玩家不在房间中");
        }

        if (member.getStatus() != PlayerStatus.IN_ROOM) {
            throw new IllegalStateException("只有IN_ROOM状态才能入座");
        }

        // 检查座位数量限制
        long seatedCount = allMembers.values().stream()
                .mapToLong(m -> m.getStatus() == PlayerStatus.SEATED ? 1 : 0)
                .sum();
        if (seatedCount >= 9) {
            throw new IllegalStateException("座位已满，最多9个座位");
        }

        member.setStatus(PlayerStatus.SEATED);
        member.setLastSeatTime(LocalDateTime.now());
        log.info("玩家{}入座", member.getName());
    }

    /**
     * 离开座位
     */
    public void leaveSeat(int playerId) {
        RoomMember member = allMembers.get(playerId);
        if (member == null) {
            throw new IllegalArgumentException("玩家不在房间中");
        }

        if (member.getStatus() != PlayerStatus.SEATED) {
            throw new IllegalStateException("只有SEATED状态才能离开座位");
        }

        member.setStatus(PlayerStatus.IN_ROOM);
        log.info("玩家{}离开座位", member.getName());
    }

    /**
     * 离开房间
     */
    public void leaveRoom(int playerId) {
        RoomMember member = allMembers.get(playerId);
        if (member == null) {
            throw new IllegalArgumentException("玩家不在房间中");
        }

        if (member.getStatus() != PlayerStatus.IN_ROOM) {
            throw new IllegalStateException("只有IN_ROOM状态才能离开房间");
        }

        BigDecimal finalChips = member.getChips();
        member.setStatus(PlayerStatus.LEFT_ROOM);
        allMembers.remove(playerId);
        log.info("玩家{}离开房间，最终筹码：{}", member.getName(), finalChips);
    }

    /**
     * 补码
     */
    public void rebuy(int playerId, BigDecimal amount) {
        RoomMember member = allMembers.get(playerId);
        if (member == null) {
            throw new IllegalArgumentException("玩家不在房间中");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("补码金额必须大于0");
        }

        // 添加到待生效筹码
        member.setPendingChips(member.getPendingChips().add(amount));
        log.info("玩家{}补码{}，待生效筹码：{}", member.getName(), amount, member.getPendingChips());
    }

    /**
     * 开始新游戏
     */
    public void startNewGame(int smallBlind, int bigBlind) {
        if (gameInProgress) {
            throw new IllegalStateException("游戏进行中，无法开始新游戏");
        }

        // 应用待生效的补码
        for (RoomMember member : allMembers.values()) {
            if (member.getPendingChips().compareTo(BigDecimal.ZERO) > 0) {
                member.setChips(member.getChips().add(member.getPendingChips()));
                log.info("玩家{}补码生效，获得筹码：{}，总筹码：{}", 
                        member.getName(), member.getPendingChips(), member.getChips());
                member.setPendingChips(BigDecimal.ZERO);
            }
        }

        // 获取所有SEATED状态的玩家
        List<GameEngine.Player> gamePlayers = allMembers.values().stream()
                .filter(member -> member.getStatus() == PlayerStatus.SEATED)
                .filter(member -> member.getChips().compareTo(BigDecimal.ZERO) > 0)
                .map(member -> new GameEngine.Player(
                        member.getId(),
                        member.getName(),
                        member.getChips()
                ))
                .collect(Collectors.toList());

        if (gamePlayers.size() < 2) {
            throw new IllegalStateException("至少需要2个玩家才能开始游戏");
        }

        // 创建新的游戏实例
        currentGame = gameEngineProvider.getObject();
        String gameId = roomId + "-game-" + System.currentTimeMillis();
        currentGame.initializeGame(gameId, gamePlayers, smallBlind, bigBlind);
        currentGame.startNewHand();

        gameInProgress = true;
        currentGameStartTime = LocalDateTime.now();
        log.info("房间{}开始新游戏，参与玩家：{}", roomId, gamePlayers.stream()
                .map(GameEngine.Player::getName)
                .collect(Collectors.toList()));
    }

    /**
     * 游戏结束处理
     */
    public void onGameFinished() {
        if (!gameInProgress || currentGame == null) {
            return;
        }

        try {
            // 获取游戏信息
            GameEngine.GameInfo gameInfo = currentGame.getGameInfo();
            
            // 更新玩家筹码
            for (GameEngine.Player p : gameInfo.getPlayers()) {
                RoomMember member = allMembers.get(p.getId());
                if (member != null) {
                    member.setChips(p.getChips());
                }
            }

            // 记录游戏历史
            String winner = "未知";
            BigDecimal potAmount = gameInfo.getTotalPot();
            
            if (currentGame.getSettlementResult() != null) {
                Map<Integer, BigDecimal> winnings = currentGame.getSettlementResult().getPlayerWinnings();
                if (!winnings.isEmpty()) {
                    // 找到获得最多筹码的玩家作为获胜者
                    winner = winnings.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(entry -> {
                                RoomMember member = allMembers.get(entry.getKey());
                                return member != null ? member.getName() : "玩家" + entry.getKey();
                            })
                            .orElse("未知");
                }
            }

            List<Integer> participants = gameInfo.getPlayers().stream()
                    .map(GameEngine.Player::getId)
                    .collect(Collectors.toList());

            Map<Integer, BigDecimal> finalChips = gameInfo.getPlayers().stream()
                    .collect(Collectors.toMap(
                            GameEngine.Player::getId,
                            GameEngine.Player::getChips
                    ));

            GameHistory history = new GameHistory(
                    gameInfo.getGameId(),
                    currentGameStartTime,
                    LocalDateTime.now(),
                    participants,
                    finalChips,
                    winner,
                    potAmount
            );
            gameHistory.add(history);

            log.info("房间{}游戏结束，获胜者：{}，底池：{}", roomId, winner, potAmount);

        } finally {
            gameInProgress = false;
            currentGameStartTime = null;
            // 游戏结束后自动准备下一局，不销毁游戏实例
        }
    }

    /**
     * 获取房间信息
     */
    public Map<String, Object> getRoomInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("roomId", roomId);
        info.put("createTime", createTime);
        info.put("gameInProgress", gameInProgress);
        info.put("currentGameStartTime", currentGameStartTime);
        
        // 成员信息
        List<Map<String, Object>> members = allMembers.values().stream()
                .map(member -> {
                    Map<String, Object> memberInfo = new HashMap<>();
                    memberInfo.put("id", member.getId());
                    memberInfo.put("name", member.getName());
                    memberInfo.put("chips", member.getChips());
                    memberInfo.put("pendingChips", member.getPendingChips());
                    memberInfo.put("status", member.getStatus().name());
                    memberInfo.put("joinTime", member.getJoinTime());
                    memberInfo.put("lastSeatTime", member.getLastSeatTime());
                    return memberInfo;
                })
                .collect(Collectors.toList());
        info.put("members", members);

        // 当前游戏信息
        if (currentGame != null) {
            info.put("currentGame", currentGame.getGameInfo());
        }

        // 游戏历史
        info.put("gameHistory", gameHistory);

        return info;
    }

    /**
     * 获取当前游戏信息
     */
    public GameEngine.GameInfo getCurrentGameInfo() {
        if (currentGame == null) {
            return null;
        }
        return currentGame.getGameInfo();
    }

    /**
     * 处理玩家行动
     */
    public void processPlayerAction(int playerId, String action, BigDecimal amount) {
        if (!gameInProgress || currentGame == null) {
            throw new IllegalStateException("没有进行中的游戏");
        }
        GameEngine.PlayerAction playerAction = GameEngine.PlayerAction.valueOf(action.toUpperCase());
        currentGame.processPlayerAction(playerId, playerAction, amount);
    }

    /**
     * 开始下一局
     */
    public boolean canStartNextHand() {
        if (!gameInProgress || currentGame == null) {
            return false;
        }
        return currentGame.canStartNextHand();
    }

    /**
     * 开始下一局
     */
    public void startNextHand() {
        if (!gameInProgress || currentGame == null) {
            throw new IllegalStateException("没有进行中的游戏");
        }
        
        // 先结束当前游戏
        onGameFinished();
        
        // 开始新游戏（这会应用待生效的补码）
        startNewGame(currentGame.getSmallBlindAmount(), currentGame.getBigBlindAmount());
    }

    /**
     * 获取房间成员
     */
    public List<RoomMember> getRoomMembers() {
        return new ArrayList<>(allMembers.values());
    }

    /**
     * 获取已入座的成员
     */
    public List<RoomMember> getSeatedMembers() {
        return allMembers.values().stream()
                .filter(member -> member.getStatus() == PlayerStatus.SEATED)
                .collect(Collectors.toList());
    }

    /**
     * 获取游戏历史
     */
    public List<GameHistory> getGameHistory() {
        return new ArrayList<>(gameHistory);
    }

    /**
     * 关闭房间
     */
    public void closeRoom() {
        // 结束当前游戏
        if (gameInProgress && currentGame != null) {
            onGameFinished();
        }
        
        // 清理所有成员
        allMembers.clear();
        gameHistory.clear();
        currentGame = null;
        gameInProgress = false;
        currentGameStartTime = null;
        
        log.info("房间{}已关闭", roomId);
    }

    /**
     * 检查房间是否活跃
     */
    public boolean isActive() {
        return !allMembers.isEmpty();
    }
}
