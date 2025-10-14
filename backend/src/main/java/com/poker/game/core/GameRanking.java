package com.poker.game.core;

import com.poker.game.model.Card;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 游戏排名服务
 * 负责计算每一局的玩家排名和胜负关系
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GameRanking {
    
    private final HandEvaluator handEvaluator;
    
    /**
     * 玩家牌型信息
     */
    @Getter
    public static class PlayerHandInfo {
        private final int playerId;
        private final String playerName;
        private final List<Card> holeCards;
        private final HandRank handRank;
        private final int rank; // 排名（1=最好）
        private final boolean isWinner;
        
        public PlayerHandInfo(int playerId, String playerName, List<Card> holeCards, 
                             HandRank handRank, int rank, boolean isWinner) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.holeCards = new ArrayList<>(holeCards);
            this.handRank = handRank;
            this.rank = rank;
            this.isWinner = isWinner;
        }
        
        public String getHoleCardsString() {
            return holeCards.stream()
                .map(Card::getDisplayName)
                .collect(Collectors.joining(", "));
        }
    }
    
    /**
     * 游戏排名结果
     */
    @Getter
    public static class GameRankingResult {
        private final List<Card> communityCards;
        private final List<PlayerHandInfo> playerRankings;
        private final List<PlayerHandInfo> winners;
        private final String summary;
        
        public GameRankingResult(List<Card> communityCards, List<PlayerHandInfo> playerRankings) {
            this.communityCards = new ArrayList<>(communityCards);
            this.playerRankings = new ArrayList<>(playerRankings);
            this.winners = playerRankings.stream()
                .filter(PlayerHandInfo::isWinner)
                .collect(Collectors.toList());
            this.summary = generateSummary();
        }
        
        private String generateSummary() {
            if (winners.size() == 1) {
                PlayerHandInfo winner = winners.get(0);
                return String.format("%s 获胜！牌型：%s", 
                    winner.getPlayerName(), winner.getHandRank().getDescription());
            } else {
                String winnerNames = winners.stream()
                    .map(PlayerHandInfo::getPlayerName)
                    .collect(Collectors.joining("、"));
                return String.format("%s 平分底池！牌型：%s", 
                    winnerNames, winners.get(0).getHandRank().getDescription());
            }
        }
        
        public String getCommunityCardsString() {
            return communityCards.stream()
                .map(Card::getDisplayName)
                .collect(Collectors.joining(", "));
        }
    }
    
    /**
     * 计算游戏排名
     * @param playerHoleCards 玩家底牌映射 (玩家ID -> 底牌)
     * @param communityCards 公共牌
     * @return 排名结果
     */
    public GameRankingResult calculateRanking(Map<Integer, List<Card>> playerHoleCards, 
                                            List<Card> communityCards) {
        if (communityCards.size() != 5) {
            throw new IllegalArgumentException("公共牌必须是5张");
        }
        
        log.info("开始计算游戏排名，参与玩家数：{}，公共牌：{}", 
            playerHoleCards.size(),
            communityCards.stream().map(Card::getDisplayName).collect(Collectors.joining(", ")));
        
        // 计算每个玩家的最佳牌型
        List<PlayerHandInfo> playerHands = new ArrayList<>();
        
        for (Map.Entry<Integer, List<Card>> entry : playerHoleCards.entrySet()) {
            int playerId = entry.getKey();
            List<Card> holeCards = entry.getValue();
            
            if (holeCards.size() != 2) {
                throw new IllegalArgumentException("每个玩家必须有2张底牌");
            }
            
            // 组合7张牌（2张底牌 + 5张公共牌）
            List<Card> sevenCards = new ArrayList<>(holeCards);
            sevenCards.addAll(communityCards);
            
            // 评估牌型
            HandRank handRank = handEvaluator.evaluateHand(sevenCards);
            
            PlayerHandInfo playerInfo = new PlayerHandInfo(
                playerId, 
                "玩家" + playerId, 
                holeCards, 
                handRank, 
                0, // 排名稍后计算
                false // 是否获胜稍后计算
            );
            
            playerHands.add(playerInfo);
            
            log.debug("玩家{} - 底牌：{}，牌型：{}", 
                playerId, 
                holeCards.stream().map(Card::getDisplayName).collect(Collectors.joining(", ")),
                handRank.toString());
        }
        
        // 按牌型排序（降序，最好的牌型在前）
        playerHands.sort((a, b) -> b.getHandRank().compareTo(a.getHandRank()));
        
        // 计算排名和获胜者
        List<PlayerHandInfo> rankedPlayers = calculateRanksAndWinners(playerHands);
        
        GameRankingResult result = new GameRankingResult(communityCards, rankedPlayers);
        
        log.info("排名计算完成：{}", result.getSummary());
        
        return result;
    }
    
    /**
     * 计算排名和获胜者
     */
    private List<PlayerHandInfo> calculateRanksAndWinners(List<PlayerHandInfo> sortedPlayers) {
        List<PlayerHandInfo> result = new ArrayList<>();
        
        int currentRank = 1;
        HandRank previousHandRank = null;
        boolean hasWinner = false;
        
        for (int i = 0; i < sortedPlayers.size(); i++) {
            PlayerHandInfo player = sortedPlayers.get(i);
            HandRank currentHandRank = player.getHandRank();
            
            // 如果牌型与前一个不同，更新排名
            if (previousHandRank != null && currentHandRank.compareTo(previousHandRank) != 0) {
                currentRank = i + 1;
                hasWinner = true; // 前面的玩家已经确定为获胜者
            }
            
            // 第一名且没有其他获胜者时为获胜者
            boolean isWinner = (currentRank == 1) && !hasWinner;
            
            // 如果当前玩家与第一名牌型相同，也是获胜者
            if (currentRank == 1 || (i > 0 && currentHandRank.compareTo(sortedPlayers.get(0).getHandRank()) == 0)) {
                isWinner = true;
            } else {
                isWinner = false;
            }
            
            PlayerHandInfo rankedPlayer = new PlayerHandInfo(
                player.getPlayerId(),
                player.getPlayerName(),
                player.getHoleCards(),
                player.getHandRank(),
                currentRank,
                isWinner
            );
            
            result.add(rankedPlayer);
            previousHandRank = currentHandRank;
        }
        
        return result;
    }
    
    /**
     * 快速比较两个玩家的牌型（用于简单场景）
     */
    public int compareHands(List<Card> player1HoleCards, List<Card> player2HoleCards, 
                          List<Card> communityCards) {
        List<Card> player1SevenCards = new ArrayList<>(player1HoleCards);
        player1SevenCards.addAll(communityCards);
        
        List<Card> player2SevenCards = new ArrayList<>(player2HoleCards);
        player2SevenCards.addAll(communityCards);
        
        HandRank hand1 = handEvaluator.evaluateHand(player1SevenCards);
        HandRank hand2 = handEvaluator.evaluateHand(player2SevenCards);
        
        return hand1.compareTo(hand2);
    }
    
    /**
     * 生成详细的排名报告
     */
    public String generateDetailedReport(GameRankingResult result) {
        StringBuilder report = new StringBuilder();
        
        report.append("=== 德州扑克游戏结果 ===\n");
        report.append("公共牌：").append(result.getCommunityCardsString()).append("\n\n");
        
        report.append("玩家排名：\n");
        for (PlayerHandInfo player : result.getPlayerRankings()) {
            report.append(String.format("第%d名：%s%s\n", 
                player.getRank(),
                player.getPlayerName(),
                player.isWinner() ? " 🏆" : ""));
            report.append(String.format("  底牌：%s\n", player.getHoleCardsString()));
            report.append(String.format("  牌型：%s\n", player.getHandRank().getDescription()));
            report.append(String.format("  最佳五张：%s\n", player.getHandRank().getBestFiveCardsString()));
            report.append("\n");
        }
        
        report.append("结果：").append(result.getSummary());
        
        return report.toString();
    }
}
