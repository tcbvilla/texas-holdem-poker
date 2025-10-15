package com.poker.game.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 德州扑克底池管理器
 * 负责底池的创建、分割和结算
 */
@Component
@Slf4j
public class PotManager {

    /**
     * 底池信息
     */
    @Getter
    @RequiredArgsConstructor
    public static class Pot {
        private final BigDecimal amount;                    // 底池金额
        private final List<Integer> eligiblePlayerIds;      // 有资格争夺的玩家ID
        private final int potIndex;                         // 底池索引（0=主池，1+=边池）
        private final String description;                   // 底池描述
        
        // 结算后的信息
        private List<Integer> winnerIds = new ArrayList<>();
        private Map<Integer, BigDecimal> winnings = new HashMap<>();
        private boolean settled = false;

        public void setWinners(List<Integer> winners) {
            this.winnerIds = new ArrayList<>(winners);
        }

        public void setWinnings(Map<Integer, BigDecimal> winningsMap) {
            this.winnings = new HashMap<>(winningsMap);
            this.settled = true;
        }

        public boolean hasWinner(int playerId) {
            return winnerIds.contains(playerId);
        }

        public BigDecimal getWinning(int playerId) {
            return winnings.getOrDefault(playerId, BigDecimal.ZERO);
        }
    }

    /**
     * 底池创建结果
     */
    @Getter
    public static class PotStructure {
        private final List<Pot> pots;                       // 所有底池
        private final BigDecimal totalAmount;               // 总金额
        private final Map<Integer, BigDecimal> playerContributions; // 玩家贡献统计

        public PotStructure(List<Pot> pots, BigDecimal totalAmount, Map<Integer, BigDecimal> playerContributions) {
            this.pots = pots;
            this.totalAmount = totalAmount;
            this.playerContributions = playerContributions;
        }

        public Pot getMainPot() {
            return pots.isEmpty() ? null : pots.get(0);
        }

        public List<Pot> getSidePots() {
            return pots.size() <= 1 ? new ArrayList<>() : pots.subList(1, pots.size());
        }
    }

    /**
     * 结算结果
     */
    @Getter
    public static class SettlementResult {
        private final List<Pot> settledPots;               // 已结算的底池
        private final Map<Integer, BigDecimal> playerWinnings; // 玩家获胜金额
        private final BigDecimal totalDistributed;         // 总分配金额
        private final String summary;                       // 结算摘要

        public SettlementResult(List<Pot> settledPots, Map<Integer, BigDecimal> playerWinnings, 
                               BigDecimal totalDistributed, String summary) {
            this.settledPots = settledPots;
            this.playerWinnings = playerWinnings;
            this.totalDistributed = totalDistributed;
            this.summary = summary;
        }

        public BigDecimal getPlayerWinning(int playerId) {
            return playerWinnings.getOrDefault(playerId, BigDecimal.ZERO);
        }
    }

    /**
     * 玩家下注信息
     */
    @Getter
    @RequiredArgsConstructor
    public static class PlayerBetInfo {
        private final int playerId;
        private final String playerName;
        private final BigDecimal totalBet;                  // 总下注金额
        private final boolean hasFolded;                    // 是否弃牌
        private final int seatPosition;                     // 座位位置（用于平分时的优先级）
    }

    /**
     * 创建底池结构
     * 
     * @param playerBets 玩家下注信息列表
     * @return 底池结构
     */
    public PotStructure createPots(List<PlayerBetInfo> playerBets) {
        log.info("开始创建底池结构，参与玩家数：{}", playerBets.size());

        // 过滤掉没有下注的玩家
        List<PlayerBetInfo> validBets = playerBets.stream()
                .filter(bet -> bet.getTotalBet().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        if (validBets.isEmpty()) {
            log.warn("没有有效的下注，创建空底池结构");
            return new PotStructure(new ArrayList<>(), BigDecimal.ZERO, new HashMap<>());
        }

        // 获取所有不同的下注层级并排序
        List<BigDecimal> betLevels = validBets.stream()
                .map(PlayerBetInfo::getTotalBet)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        log.debug("下注层级：{}", betLevels);

        List<Pot> pots = new ArrayList<>();
        BigDecimal previousLevel = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Integer, BigDecimal> playerContributions = new HashMap<>();

        // 为每个层级创建底池
        for (int i = 0; i < betLevels.size(); i++) {
            BigDecimal currentLevel = betLevels.get(i);
            BigDecimal levelContribution = currentLevel.subtract(previousLevel);

            // 找出在此层级有贡献的玩家（包括弃牌玩家的贡献）
            List<Integer> contributingPlayerIds = validBets.stream()
                    .filter(bet -> bet.getTotalBet().compareTo(currentLevel) >= 0)
                    .map(PlayerBetInfo::getPlayerId)
                    .collect(Collectors.toList());

            // 找出有资格争夺此底池的玩家（排除弃牌玩家）
            List<Integer> eligiblePlayerIds = validBets.stream()
                    .filter(bet -> bet.getTotalBet().compareTo(currentLevel) >= 0)
                    .filter(bet -> !bet.isHasFolded())
                    .map(PlayerBetInfo::getPlayerId)
                    .collect(Collectors.toList());

            if (!contributingPlayerIds.isEmpty()) {
                BigDecimal potAmount = levelContribution.multiply(BigDecimal.valueOf(contributingPlayerIds.size()));
                String description = i == 0 ? "主池" : "边池" + i;

                Pot pot = new Pot(potAmount, eligiblePlayerIds, i, description);
                pots.add(pot);
                totalAmount = totalAmount.add(potAmount);

                // 记录玩家贡献
                for (int playerId : contributingPlayerIds) {
                    playerContributions.merge(playerId, levelContribution, BigDecimal::add);
                }

                log.debug("创建{}：金额={}，贡献玩家={}，有资格玩家={}", 
                         description, potAmount, contributingPlayerIds, eligiblePlayerIds);
            }

            previousLevel = currentLevel;
        }

        log.info("底池结构创建完成：{}个底池，总金额={}", pots.size(), totalAmount);
        return new PotStructure(pots, totalAmount, playerContributions);
    }

    /**
     * 结算底池
     * 
     * @param potStructure 底池结构
     * @param playerHands 玩家牌型信息
     * @return 结算结果
     */
    public SettlementResult settlePots(PotStructure potStructure, Map<Integer, HandRank> playerHands) {
        log.info("开始结算底池，底池数量：{}", potStructure.getPots().size());

        List<Pot> settledPots = new ArrayList<>();
        Map<Integer, BigDecimal> totalPlayerWinnings = new HashMap<>();
        BigDecimal totalDistributed = BigDecimal.ZERO;
        List<String> summaryParts = new ArrayList<>();

        // 按顺序结算每个底池
        for (Pot pot : potStructure.getPots()) {
            if (pot.getEligiblePlayerIds().isEmpty()) {
                log.warn("{}没有有资格的玩家，跳过", pot.getDescription());
                continue;
            }

            // 找出此底池的获胜者
            List<Integer> winners = findPotWinners(pot.getEligiblePlayerIds(), playerHands);
            
            if (winners.isEmpty()) {
                log.warn("{}没有找到获胜者，跳过", pot.getDescription());
                continue;
            }

            // 分配底池金额
            Map<Integer, BigDecimal> potWinnings = distributePotAmount(pot.getAmount(), winners, potStructure.getPlayerContributions());
            
            // 更新底池信息
            pot.setWinners(winners);
            pot.setWinnings(potWinnings);
            settledPots.add(pot);

            // 累计玩家总获胜金额
            for (Map.Entry<Integer, BigDecimal> entry : potWinnings.entrySet()) {
                totalPlayerWinnings.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
                totalDistributed = totalDistributed.add(entry.getValue());
            }

            // 生成摘要
            String winnerNames = winners.stream()
                    .map(id -> "玩家" + id)
                    .collect(Collectors.joining(", "));
            summaryParts.add(String.format("%s(%s)由%s获得", pot.getDescription(), pot.getAmount(), winnerNames));

            log.info("{}结算完成：获胜者={}，分配金额={}", pot.getDescription(), winners, potWinnings);
        }

        String summary = summaryParts.isEmpty() ? "无底池需要结算" : String.join("；", summaryParts);
        
        log.info("底池结算完成：总分配金额={}，获胜玩家数={}", totalDistributed, totalPlayerWinnings.size());
        return new SettlementResult(settledPots, totalPlayerWinnings, totalDistributed, summary);
    }

    /**
     * 找出底池的获胜者
     */
    private List<Integer> findPotWinners(List<Integer> eligiblePlayerIds, Map<Integer, HandRank> playerHands) {
        if (eligiblePlayerIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有有资格玩家的牌型
        List<Map.Entry<Integer, HandRank>> eligibleHands = eligiblePlayerIds.stream()
                .filter(playerHands::containsKey)
                .map(playerId -> Map.entry(playerId, playerHands.get(playerId)))
                .collect(Collectors.toList());

        if (eligibleHands.isEmpty()) {
            log.warn("没有找到有资格玩家的牌型信息");
            return new ArrayList<>();
        }

        // 找出最强牌型
        HandRank bestHand = eligibleHands.stream()
                .map(Map.Entry::getValue)
                .max(HandRank::compareTo)
                .orElse(null);

        if (bestHand == null) {
            return new ArrayList<>();
        }

        // 找出所有拥有最强牌型的玩家
        List<Integer> winners = eligibleHands.stream()
                .filter(entry -> entry.getValue().compareTo(bestHand) == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.debug("底池获胜者：{}，牌型：{}", winners, bestHand.getHandType());
        return winners;
    }

    /**
     * 分配底池金额
     */
    private Map<Integer, BigDecimal> distributePotAmount(BigDecimal potAmount, List<Integer> winners, 
                                                        Map<Integer, BigDecimal> playerContributions) {
        Map<Integer, BigDecimal> winnings = new HashMap<>();
        
        if (winners.isEmpty()) {
            return winnings;
        }

        // 计算每人应得的基础金额
        BigDecimal baseAmount = potAmount.divide(BigDecimal.valueOf(winners.size()), 0, RoundingMode.DOWN);
        BigDecimal remainder = potAmount.remainder(BigDecimal.valueOf(winners.size()));

        // 按座位位置排序（用于分配多余筹码）
        List<Integer> sortedWinners = new ArrayList<>(winners);
        sortedWinners.sort(Integer::compareTo); // 简化：按玩家ID排序，实际应该按座位位置

        // 分配基础金额
        for (Integer winnerId : sortedWinners) {
            winnings.put(winnerId, baseAmount);
        }

        // 分配多余筹码给位置靠前的玩家
        for (int i = 0; i < remainder.intValue(); i++) {
            Integer winnerId = sortedWinners.get(i);
            winnings.put(winnerId, winnings.get(winnerId).add(BigDecimal.ONE));
        }

        log.debug("底池分配：总金额={}，基础金额={}，多余筹码={}，分配结果={}", 
                 potAmount, baseAmount, remainder, winnings);

        return winnings;
    }

    /**
     * 验证底池结构的正确性
     */
    public boolean validatePotStructure(PotStructure potStructure, List<PlayerBetInfo> originalBets) {
        // 验证总金额是否匹配
        BigDecimal expectedTotal = originalBets.stream()
                .map(PlayerBetInfo::getTotalBet)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean amountMatches = potStructure.getTotalAmount().compareTo(expectedTotal) == 0;
        
        if (!amountMatches) {
            log.error("底池总金额不匹配：期望={}，实际={}", expectedTotal, potStructure.getTotalAmount());
        }

        // 验证每个玩家的贡献是否正确
        for (PlayerBetInfo bet : originalBets) {
            BigDecimal expectedContribution = bet.getTotalBet();
            BigDecimal actualContribution = potStructure.getPlayerContributions()
                    .getOrDefault(bet.getPlayerId(), BigDecimal.ZERO);
            
            if (expectedContribution.compareTo(actualContribution) != 0) {
                log.error("玩家{}贡献不匹配：期望={}，实际={}", 
                         bet.getPlayerId(), expectedContribution, actualContribution);
                return false;
            }
        }

        log.debug("底池结构验证通过");
        return amountMatches;
    }

    /**
     * 获取底池统计信息
     */
    public Map<String, Object> getPotStatistics(PotStructure potStructure) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalPots", potStructure.getPots().size());
        stats.put("totalAmount", potStructure.getTotalAmount());
        stats.put("mainPotAmount", potStructure.getMainPot() != null ? potStructure.getMainPot().getAmount() : BigDecimal.ZERO);
        stats.put("sidePotCount", potStructure.getSidePots().size());
        stats.put("sidePotTotal", potStructure.getSidePots().stream()
                .map(Pot::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        return stats;
    }
}
