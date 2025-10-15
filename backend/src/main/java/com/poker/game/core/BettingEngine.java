package com.poker.game.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 德州扑克下注引擎
 * 专门处理下注逻辑、验证和计算
 */
@Component
@Slf4j
public class BettingEngine {

    /**
     * 下注结果
     */
    @Getter
    @RequiredArgsConstructor
    public static class BettingResult {
        private final boolean success;
        private final String message;
        private final BigDecimal actualAmount;  // 实际下注金额
        private final boolean isAllIn;          // 是否全下
        private final BigDecimal newPotSize;    // 新的底池大小
        private final BigDecimal newCurrentBet; // 新的当前下注额

        public static BettingResult success(String message, BigDecimal actualAmount, 
                                          boolean isAllIn, BigDecimal newPotSize, BigDecimal newCurrentBet) {
            return new BettingResult(true, message, actualAmount, isAllIn, newPotSize, newCurrentBet);
        }

        public static BettingResult failure(String message) {
            return new BettingResult(false, message, BigDecimal.ZERO, false, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    /**
     * 下注验证结果
     */
    @Getter
    @RequiredArgsConstructor
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final BigDecimal minBet;    // 最小下注额
        private final BigDecimal maxBet;    // 最大下注额（玩家筹码）

        public static ValidationResult valid(String message, BigDecimal minBet, BigDecimal maxBet) {
            return new ValidationResult(true, message, minBet, maxBet);
        }

        public static ValidationResult invalid(String message, BigDecimal minBet, BigDecimal maxBet) {
            return new ValidationResult(false, message, minBet, maxBet);
        }
    }

    /**
     * 底池信息
     */
    @Getter
    @RequiredArgsConstructor
    public static class PotInfo {
        private final BigDecimal mainPot;           // 主池
        private final List<SidePot> sidePots;       // 边池列表
        private final BigDecimal totalPot;         // 总底池

        @Getter
        @RequiredArgsConstructor
        public static class SidePot {
            private final BigDecimal amount;        // 边池金额
            private final List<Integer> playerIds;  // 有资格争夺此边池的玩家ID
        }
    }

    /**
     * 验证弃牌操作
     */
    public ValidationResult validateFold(GameEngine.Player player) {
        if (player.isHasFolded()) {
            return ValidationResult.invalid("玩家已经弃牌", BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (!player.isActive()) {
            return ValidationResult.invalid("玩家不在游戏中", BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return ValidationResult.valid("可以弃牌", BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * 验证过牌操作
     */
    public ValidationResult validateCheck(GameEngine.Player player, BigDecimal currentBet) {
        if (player.isHasFolded()) {
            return ValidationResult.invalid("玩家已经弃牌", BigDecimal.ZERO, player.getChips());
        }
        if (!player.isActive()) {
            return ValidationResult.invalid("玩家不在游戏中", BigDecimal.ZERO, player.getChips());
        }
        if (player.isAllIn()) {
            return ValidationResult.invalid("玩家已全下", BigDecimal.ZERO, player.getChips());
        }

        BigDecimal callAmount = currentBet.subtract(player.getBetAmount());
        if (callAmount.compareTo(BigDecimal.ZERO) > 0) {
            return ValidationResult.invalid(
                String.format("无法过牌，需要跟注 %s", callAmount), 
                callAmount, player.getChips()
            );
        }

        return ValidationResult.valid("可以过牌", BigDecimal.ZERO, player.getChips());
    }

    /**
     * 验证跟注操作
     */
    public ValidationResult validateCall(GameEngine.Player player, BigDecimal currentBet) {
        if (player.isHasFolded()) {
            return ValidationResult.invalid("玩家已经弃牌", BigDecimal.ZERO, player.getChips());
        }
        if (!player.isActive()) {
            return ValidationResult.invalid("玩家不在游戏中", BigDecimal.ZERO, player.getChips());
        }
        if (player.isAllIn()) {
            return ValidationResult.invalid("玩家已全下", BigDecimal.ZERO, player.getChips());
        }

        BigDecimal callAmount = currentBet.subtract(player.getBetAmount());
        if (callAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("无需跟注", BigDecimal.ZERO, player.getChips());
        }

        if (callAmount.compareTo(player.getChips()) > 0) {
            // 筹码不足，只能全下
            return ValidationResult.valid(
                String.format("筹码不足跟注，将全下 %s", player.getChips()),
                player.getChips(), player.getChips()
            );
        }

        return ValidationResult.valid(
            String.format("可以跟注 %s", callAmount),
            callAmount, player.getChips()
        );
    }

    /**
     * 验证加注操作
     */
    public ValidationResult validateRaise(GameEngine.Player player, BigDecimal raiseAmount, 
                                        BigDecimal currentBet, BigDecimal bigBlind) {
        if (player.isHasFolded()) {
            return ValidationResult.invalid("玩家已经弃牌", currentBet, player.getChips());
        }
        if (!player.isActive()) {
            return ValidationResult.invalid("玩家不在游戏中", currentBet, player.getChips());
        }
        if (player.isAllIn()) {
            return ValidationResult.invalid("玩家已全下", currentBet, player.getChips());
        }

        // 计算最小加注额（当前下注额 + 大盲注）
        BigDecimal minRaise = currentBet.add(BigDecimal.valueOf(bigBlind.longValue()));
        BigDecimal maxRaise = player.getChips().add(player.getBetAmount());

        if (raiseAmount.compareTo(minRaise) < 0) {
            return ValidationResult.invalid(
                String.format("加注金额太小，最小加注额为 %s", minRaise),
                minRaise, maxRaise
            );
        }

        if (raiseAmount.compareTo(maxRaise) > 0) {
            return ValidationResult.invalid(
                String.format("加注金额超过筹码，最大加注额为 %s", maxRaise),
                minRaise, maxRaise
            );
        }

        return ValidationResult.valid(
            String.format("可以加注到 %s", raiseAmount),
            minRaise, maxRaise
        );
    }

    /**
     * 验证全下操作
     */
    public ValidationResult validateAllIn(GameEngine.Player player) {
        if (player.isHasFolded()) {
            return ValidationResult.invalid("玩家已经弃牌", BigDecimal.ZERO, player.getChips());
        }
        if (!player.isActive()) {
            return ValidationResult.invalid("玩家不在游戏中", BigDecimal.ZERO, player.getChips());
        }
        if (player.isAllIn()) {
            return ValidationResult.invalid("玩家已全下", BigDecimal.ZERO, player.getChips());
        }
        if (player.getChips().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("玩家没有筹码", BigDecimal.ZERO, player.getChips());
        }

        return ValidationResult.valid(
            String.format("可以全下 %s", player.getChips()),
            player.getChips(), player.getChips()
        );
    }

    /**
     * 执行弃牌
     */
    public BettingResult executeFold(GameEngine.Player player) {
        ValidationResult validation = validateFold(player);
        if (!validation.isValid()) {
            return BettingResult.failure(validation.getMessage());
        }

        player.fold();
        log.info("玩家 {} 弃牌", player.getName());

        return BettingResult.success(
            String.format("玩家 %s 弃牌", player.getName()),
            BigDecimal.ZERO, false, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }

    /**
     * 执行过牌
     */
    public BettingResult executeCheck(GameEngine.Player player, BigDecimal currentBet) {
        ValidationResult validation = validateCheck(player, currentBet);
        if (!validation.isValid()) {
            return BettingResult.failure(validation.getMessage());
        }

        player.setHasActed(true);
        log.info("玩家 {} 过牌", player.getName());

        return BettingResult.success(
            String.format("玩家 %s 过牌", player.getName()),
            BigDecimal.ZERO, false, BigDecimal.ZERO, currentBet
        );
    }

    /**
     * 执行跟注
     */
    public BettingResult executeCall(GameEngine.Player player, BigDecimal currentBet, BigDecimal totalPot) {
        ValidationResult validation = validateCall(player, currentBet);
        if (!validation.isValid()) {
            return BettingResult.failure(validation.getMessage());
        }

        BigDecimal callAmount = currentBet.subtract(player.getBetAmount());
        BigDecimal actualAmount = callAmount.min(player.getChips());
        boolean isAllIn = actualAmount.compareTo(player.getChips()) == 0;

        player.bet(actualAmount);
        BigDecimal newPotSize = totalPot.add(actualAmount);

        log.info("玩家 {} 跟注 {}{}",
                player.getName(), actualAmount, isAllIn ? "（全下）" : "");

        return BettingResult.success(
            String.format("玩家 %s 跟注 %s%s",
                    player.getName(), actualAmount, isAllIn ? "（全下）" : ""),
            actualAmount, isAllIn, newPotSize, currentBet
        );
    }

    /**
     * 执行加注
     */
    public BettingResult executeRaise(GameEngine.Player player, BigDecimal raiseAmount,
                                    BigDecimal currentBet, BigDecimal totalPot, BigDecimal bigBlind) {
        ValidationResult validation = validateRaise(player, raiseAmount, currentBet, bigBlind);
        if (!validation.isValid()) {
            return BettingResult.failure(validation.getMessage());
        }

        BigDecimal betNeeded = raiseAmount.subtract(player.getBetAmount());
        BigDecimal actualAmount = betNeeded.min(player.getChips());
        boolean isAllIn = actualAmount.compareTo(player.getChips()) == 0;

        player.bet(actualAmount);
        BigDecimal newCurrentBet = player.getBetAmount();
        BigDecimal newPotSize = totalPot.add(actualAmount);

        log.info("玩家 {} 加注到 {}{}",
                player.getName(), newCurrentBet, isAllIn ? "（全下）" : "");

        return BettingResult.success(
            String.format("玩家 %s 加注到 %s%s",
                    player.getName(), newCurrentBet, isAllIn ? "（全下）" : ""),
            actualAmount, isAllIn, newPotSize, newCurrentBet
        );
    }

    /**
     * 执行全下
     */
    public BettingResult executeAllIn(GameEngine.Player player, BigDecimal currentBet, BigDecimal totalPot) {
        ValidationResult validation = validateAllIn(player);
        if (!validation.isValid()) {
            return BettingResult.failure(validation.getMessage());
        }

        BigDecimal allInAmount = player.getChips();
        player.bet(allInAmount);
        BigDecimal newPotSize = totalPot.add(allInAmount);
        BigDecimal newCurrentBet = player.getBetAmount().max(currentBet);

        log.info("玩家 {} 全下 {}", player.getName(), allInAmount);

        return BettingResult.success(
            String.format("玩家 %s 全下 %s", player.getName(), allInAmount),
            allInAmount, true, newPotSize, newCurrentBet
        );
    }

    /**
     * 计算底池信息（包括边池）
     */
    public PotInfo calculatePotInfo(List<GameEngine.Player> players) {
        // 收集所有玩家的总下注金额
        Map<Integer, BigDecimal> playerBets = players.stream()
                .collect(Collectors.toMap(
                        GameEngine.Player::getId,
                        GameEngine.Player::getTotalBet
                ));

        // 获取所有不同的下注金额并排序
        List<BigDecimal> betLevels = playerBets.values().stream()
                .filter(bet -> bet.compareTo(BigDecimal.ZERO) > 0)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (betLevels.isEmpty()) {
            return new PotInfo(BigDecimal.ZERO, new ArrayList<>(), BigDecimal.ZERO);
        }

        List<PotInfo.SidePot> sidePots = new ArrayList<>();
        BigDecimal previousLevel = BigDecimal.ZERO;
        BigDecimal totalPot = BigDecimal.ZERO;

        for (BigDecimal currentLevel : betLevels) {
            BigDecimal levelContribution = currentLevel.subtract(previousLevel);

            // 找出在此级别有贡献的玩家
            List<Integer> eligiblePlayers = players.stream()
                    .filter(p -> p.getTotalBet().compareTo(currentLevel) >= 0)
                    .map(GameEngine.Player::getId)
                    .collect(Collectors.toList());

            if (!eligiblePlayers.isEmpty()) {
                BigDecimal potAmount = levelContribution.multiply(BigDecimal.valueOf(eligiblePlayers.size()));
                sidePots.add(new PotInfo.SidePot(potAmount, eligiblePlayers));
                totalPot = totalPot.add(potAmount);
            }

            previousLevel = currentLevel;
        }

        // 主池是第一个边池（如果存在）
        BigDecimal mainPot = sidePots.isEmpty() ? BigDecimal.ZERO : sidePots.get(0).getAmount();

        log.debug("底池计算完成：主池 {}，边池数量 {}，总底池 {}",
                mainPot, sidePots.size() - 1, totalPot);

        return new PotInfo(mainPot, sidePots, totalPot);
    }

    /**
     * 检查下注轮是否完成
     */
    public boolean isBettingRoundComplete(List<GameEngine.Player> players, BigDecimal currentBet) {
        List<GameEngine.Player> activePlayers = players.stream()
                .filter(GameEngine.Player::isActive)
                .collect(Collectors.toList());

        if (activePlayers.size() <= 1) {
            return true; // 只剩一个或没有活跃玩家
        }

        // 检查所有活跃玩家是否都已行动
        for (GameEngine.Player player : activePlayers) {
            if (!player.isHasActed()) {
                return false;
            }
        }

        // 检查所有非全下玩家的下注金额是否相等
        List<GameEngine.Player> nonAllInPlayers = activePlayers.stream()
                .filter(p -> !p.isAllIn())
                .collect(Collectors.toList());

        if (nonAllInPlayers.isEmpty()) {
            return true; // 所有玩家都全下了
        }

        BigDecimal expectedBet = currentBet;
        for (GameEngine.Player player : nonAllInPlayers) {
            if (player.getBetAmount().compareTo(expectedBet) != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取下一个需要行动的玩家索引
     */
    public int getNextPlayerToAct(List<GameEngine.Player> players, int currentIndex) {
        for (int i = 1; i <= players.size(); i++) {
            int nextIndex = (currentIndex + i) % players.size();
            GameEngine.Player player = players.get(nextIndex);

            if (player.isActive() && !player.isAllIn() && !player.isHasActed()) {
                return nextIndex;
            }
        }
        return -1; // 没有找到需要行动的玩家
    }

    /**
     * 重置玩家的下注轮状态
     */
    public void resetBettingRound(List<GameEngine.Player> players) {
        for (GameEngine.Player player : players) {
            player.resetForNewRound();
        }
        log.debug("下注轮状态已重置");
    }

    /**
     * 计算最小加注额
     */
    public BigDecimal calculateMinRaise(BigDecimal currentBet, BigDecimal bigBlind) {
        return currentBet.add(bigBlind);
    }

    /**
     * 获取玩家可用的行动选项
     */
    public List<GameEngine.PlayerAction> getAvailableActions(GameEngine.Player player, BigDecimal currentBet) {
        List<GameEngine.PlayerAction> actions = new ArrayList<>();

        if (!player.isActive() || player.isHasFolded()) {
            return actions; // 已弃牌或不活跃的玩家没有行动选项
        }

        if (player.isAllIn()) {
            return actions; // 已全下的玩家没有行动选项
        }

        // 弃牌总是可用的
        actions.add(GameEngine.PlayerAction.FOLD);

        BigDecimal callAmount = currentBet.subtract(player.getBetAmount());

        if (callAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // 可以过牌
            actions.add(GameEngine.PlayerAction.CHECK);
        } else {
            // 需要跟注
            actions.add(GameEngine.PlayerAction.CALL);
        }

        // 如果有筹码，可以加注或全下
        if (player.getChips().compareTo(BigDecimal.ZERO) > 0) {
            actions.add(GameEngine.PlayerAction.RAISE);
            actions.add(GameEngine.PlayerAction.ALL_IN);
        }

        return actions;
    }
}
