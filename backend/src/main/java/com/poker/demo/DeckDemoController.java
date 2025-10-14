package com.poker.demo;

import com.poker.game.core.Deck;
import com.poker.game.core.GameRanking;
import com.poker.game.core.HandEvaluator;
import com.poker.game.model.Card;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 发牌系统演示控制器
 * 
 * 用于演示和测试核心发牌功能
 * 展示公平发牌算法的实际效果
 */
@Slf4j
@RestController
@RequestMapping("/api/demo/deck")
@RequiredArgsConstructor
public class DeckDemoController {
    
    private final ObjectProvider<Deck> deckProvider;
    private final HandEvaluator handEvaluator;
    private final GameRanking gameRanking;
    
    /**
     * 演示完整的德州扑克发牌流程
     */
    @PostMapping("/texas-holdem-demo")
    public Map<String, Object> texasHoldemDemo(
            @RequestParam(defaultValue = "6") int playerCount,
            @RequestParam(required = false) String customSeed) {
        
        log.info("开始德州扑克发牌演示，玩家数：{}，自定义种子：{}", playerCount, customSeed);
        
        // 获取新的Deck实例（prototype scope）
        Deck deck = deckProvider.getObject();
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 洗牌
            deck.shuffle(customSeed);
            result.put("shuffleSeed", deck.getShuffleSeed());
            result.put("shuffleTimestamp", deck.getShuffleTimestamp());
            
            // 2. 发底牌
            Map<Integer, List<Card>> holeCards = deck.dealHoleCards(playerCount);
            result.put("holeCards", convertHoleCardsToDisplay(holeCards));
            
            // 3. 发翻牌
            List<Card> flop = deck.dealFlop();
            result.put("flop", convertCardsToDisplay(flop));
            
            // 4. 发转牌
            Card turn = deck.dealTurn();
            result.put("turn", convertCardToDisplay(turn));
            
            // 5. 发河牌
            Card river = deck.dealRiver();
            result.put("river", convertCardToDisplay(river));
            
            // 6. 牌库状态
            Deck.DeckStatus status = deck.getStatus();
            result.put("deckStatus", Map.of(
                "remainingCards", status.remainingCards(),
                "dealtCards", status.dealtCards(),
                "burnedCards", status.burnedCards()
            ));
            
            // 7. 计算排名
            List<Card> communityCards = new ArrayList<>(flop);
            communityCards.add(turn);
            communityCards.add(river);
            
            Map<Integer, List<Card>> playerHoleCards = new HashMap<>();
            for (Map.Entry<Integer, List<Card>> entry : holeCards.entrySet()) {
                playerHoleCards.put(entry.getKey(), entry.getValue());
            }
            
            GameRanking.GameRankingResult rankingResult = gameRanking.calculateRanking(
                playerHoleCards, communityCards);
            
            // 转换排名结果为显示格式
            List<Map<String, Object>> playerRankings = new ArrayList<>();
            for (GameRanking.PlayerHandInfo playerInfo : rankingResult.getPlayerRankings()) {
                Map<String, Object> playerRank = new HashMap<>();
                playerRank.put("playerId", playerInfo.getPlayerId());
                playerRank.put("playerName", playerInfo.getPlayerName());
                playerRank.put("rank", playerInfo.getRank());
                playerRank.put("isWinner", playerInfo.isWinner());
                playerRank.put("handType", playerInfo.getHandRank().getHandType().name());
                playerRank.put("handTypeChinese", playerInfo.getHandRank().getHandType().getChineseName());
                playerRank.put("handDescription", playerInfo.getHandRank().getDescription());
                playerRank.put("bestFiveCards", playerInfo.getHandRank().getBestFiveCardsString());
                playerRank.put("holeCards", convertCardsToDisplay(playerInfo.getHoleCards()));
                playerRankings.add(playerRank);
            }
            
            result.put("ranking", Map.of(
                "playerRankings", playerRankings,
                "winners", rankingResult.getWinners().stream()
                    .map(winner -> Map.of(
                        "playerId", winner.getPlayerId(),
                        "playerName", winner.getPlayerName(),
                        "handDescription", winner.getHandRank().getDescription()
                    ))
                    .collect(Collectors.toList()),
                "summary", rankingResult.getSummary(),
                "detailedReport", gameRanking.generateDetailedReport(rankingResult)
            ));
            
            // 8. 统计信息
            result.put("statistics", Map.of(
                "totalCardsUsed", 52 - status.remainingCards(),
                "holeCardsDealt", playerCount * 2,
                "communityCards", 5,
                "burnedCards", status.burnedCards(),
                "expectedBurnedCards", 4,  // 发底牌1张 + 翻牌1张 + 转牌1张 + 河牌1张
                "dealingPhases", List.of("底牌前烧牌", "翻牌前烧牌", "转牌前烧牌", "河牌前烧牌")
            ));
            
            result.put("success", true);
            result.put("message", "德州扑克发牌演示完成，已计算排名");
            
        } catch (Exception e) {
            log.error("发牌演示出错", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 测试洗牌随机性
     */
    @PostMapping("/shuffle-randomness-test")
    public Map<String, Object> shuffleRandomnessTest(
            @RequestParam(defaultValue = "100") int iterations) {
        
        log.info("开始洗牌随机性测试，迭代次数：{}", iterations);
        
        Map<String, Integer> firstCardCounts = new HashMap<>();
        
        for (int i = 0; i < iterations; i++) {
            Deck deck = deckProvider.getObject();
            deck.shuffle();
            Card firstCard = deck.dealCard();
            
            String cardKey = firstCard.getDisplayName();
            firstCardCounts.merge(cardKey, 1, Integer::sum);
        }
        
        // 计算统计信息
        double expectedCount = (double) iterations / 52;
        double maxDeviation = 0;
        String mostFrequentCard = "";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : firstCardCounts.entrySet()) {
            int count = entry.getValue();
            double deviation = Math.abs(count - expectedCount) / expectedCount;
            
            if (deviation > maxDeviation) {
                maxDeviation = deviation;
            }
            
            if (count > maxCount) {
                maxCount = count;
                mostFrequentCard = entry.getKey();
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("iterations", iterations);
        result.put("expectedCountPerCard", expectedCount);
        result.put("maxDeviation", String.format("%.2f%%", maxDeviation * 100));
        result.put("mostFrequentCard", mostFrequentCard);
        result.put("maxCount", maxCount);
        result.put("cardDistribution", firstCardCounts);
        // 调整随机性判断标准：50%以内为良好，50%-80%为需要关注，80%以上为异常
        String quality;
        if (maxDeviation < 0.5) {
            quality = "良好";
        } else if (maxDeviation < 0.8) {
            quality = "需要关注";
        } else {
            quality = "异常";
        }
        result.put("randomnessQuality", quality);
        
        return result;
    }
    
    
    /**
     * 获取牌库基本信息
     */
    @GetMapping("/info")
    public Map<String, Object> getDeckInfo() {
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalCards", 52);
        result.put("suits", List.of("♠黑桃", "♥红桃", "♣梅花", "♦方片"));
        result.put("ranks", List.of("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"));
        
        // 算法信息
        result.put("algorithms", Map.of(
            "shuffle", "Fisher-Yates洗牌算法",
            "cut", "完全随机切牌",
            "randomGenerator", "SecureRandom加密级随机数",
            "burnCard", "标准德州扑克烧牌规则"
        ));
        
        // 发牌流程
        result.put("dealingProcess", List.of(
            "1. Fisher-Yates洗牌",
            "2. 随机位置切牌",
            "3. 烧1张牌 → 发底牌",
            "4. 烧1张牌 → 发翻牌(3张)",
            "5. 烧1张牌 → 发转牌(1张)",
            "6. 烧1张牌 → 发河牌(1张)"
        ));
        
        // 技术特性
        result.put("features", Map.of(
            "scope", "prototype - 每个游戏独立实例",
            "reproducible", "种子可重现验证",
            "secure", "防作弊机制完备",
            "standard", "符合国际德州扑克规则"
        ));
        
        return result;
    }
    
    // 辅助方法：转换底牌显示格式
    private Map<String, Object> convertHoleCardsToDisplay(Map<Integer, List<Card>> holeCards) {
        Map<String, Object> result = new HashMap<>();
        
        // 德州扑克2-9人桌座位信息
        Map<Integer, Map<String, String>> seatPositions = getSeatPositions(holeCards.size());
        
        holeCards.forEach((player, cards) -> {
            String playerKey = "玩家" + player;
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("cards", convertCardsToDisplay(cards));
            playerData.put("seatInfo", seatPositions.getOrDefault(player, 
                Map.of("name", "未知位置", "position", "Unknown", "emoji", "👤")));
            result.put(playerKey, playerData);
        });
        
        return result;
    }
    
    // 获取不同人数的座位位置信息
    private Map<Integer, Map<String, String>> getSeatPositions(int playerCount) {
        Map<Integer, Map<String, String>> seatPositions = new HashMap<>();
        
        // 基础位置定义
        String[] positionNames = {
            "小盲位", "大盲位", "枪口位", "中位", "关煞位", "按钮位", "后位", "前位", "边位"
        };
        String[] positionNamesEn = {
            "Small Blind", "Big Blind", "Under the Gun", "Middle Position", 
            "Cutoff", "Button", "Late Position", "Early Position", "Side Position"
        };
        String[] emojis = {
            "🟡", "🔴", "🔫", "🎯", "✂️", "🎲", "🕐", "🌅", "📍"
        };
        
        // 根据人数分配座位
        for (int i = 1; i <= playerCount; i++) {
            String name, position, emoji;
            
            if (playerCount == 2) {
                // 2人桌：小盲位和大盲位
                if (i == 1) {
                    name = positionNames[0]; position = positionNamesEn[0]; emoji = emojis[0];
                } else {
                    name = positionNames[1]; position = positionNamesEn[1]; emoji = emojis[1];
                }
            } else if (playerCount == 3) {
                // 3人桌：小盲位、大盲位、按钮位
                if (i == 1) {
                    name = positionNames[0]; position = positionNamesEn[0]; emoji = emojis[0];
                } else if (i == 2) {
                    name = positionNames[1]; position = positionNamesEn[1]; emoji = emojis[1];
                } else {
                    name = positionNames[5]; position = positionNamesEn[5]; emoji = emojis[5];
                }
            } else if (playerCount == 4) {
                // 4人桌：小盲位、大盲位、枪口位、按钮位
                if (i == 1) {
                    name = positionNames[0]; position = positionNamesEn[0]; emoji = emojis[0];
                } else if (i == 2) {
                    name = positionNames[1]; position = positionNamesEn[1]; emoji = emojis[1];
                } else if (i == 3) {
                    name = positionNames[2]; position = positionNamesEn[2]; emoji = emojis[2];
                } else {
                    name = positionNames[5]; position = positionNamesEn[5]; emoji = emojis[5];
                }
            } else if (playerCount == 5) {
                // 5人桌：小盲位、大盲位、枪口位、关煞位、按钮位
                if (i == 1) {
                    name = positionNames[0]; position = positionNamesEn[0]; emoji = emojis[0];
                } else if (i == 2) {
                    name = positionNames[1]; position = positionNamesEn[1]; emoji = emojis[1];
                } else if (i == 3) {
                    name = positionNames[2]; position = positionNamesEn[2]; emoji = emojis[2];
                } else if (i == 4) {
                    name = positionNames[4]; position = positionNamesEn[4]; emoji = emojis[4];
                } else {
                    name = positionNames[5]; position = positionNamesEn[5]; emoji = emojis[5];
                }
            } else if (playerCount == 6) {
                // 6人桌：小盲位、大盲位、枪口位、中位、关煞位、按钮位
                if (i == 1) {
                    name = positionNames[0]; position = positionNamesEn[0]; emoji = emojis[0];
                } else if (i == 2) {
                    name = positionNames[1]; position = positionNamesEn[1]; emoji = emojis[1];
                } else if (i == 3) {
                    name = positionNames[2]; position = positionNamesEn[2]; emoji = emojis[2];
                } else if (i == 4) {
                    name = positionNames[3]; position = positionNamesEn[3]; emoji = emojis[3];
                } else if (i == 5) {
                    name = positionNames[4]; position = positionNamesEn[4]; emoji = emojis[4];
                } else {
                    name = positionNames[5]; position = positionNamesEn[5]; emoji = emojis[5];
                }
            } else if (playerCount == 7) {
                // 7人桌：小盲位、大盲位、枪口位、中位、中位、关煞位、按钮位
                if (i == 1) {
                    name = positionNames[0]; position = positionNamesEn[0]; emoji = emojis[0];
                } else if (i == 2) {
                    name = positionNames[1]; position = positionNamesEn[1]; emoji = emojis[1];
                } else if (i == 3) {
                    name = positionNames[2]; position = positionNamesEn[2]; emoji = emojis[2];
                } else if (i == 4 || i == 5) {
                    name = positionNames[3]; position = positionNamesEn[3]; emoji = emojis[3];
                } else if (i == 6) {
                    name = positionNames[4]; position = positionNamesEn[4]; emoji = emojis[4];
                } else {
                    name = positionNames[5]; position = positionNamesEn[5]; emoji = emojis[5];
                }
            } else if (playerCount == 8) {
                // 8人桌：小盲位、大盲位、枪口位、中位、中位、中位、关煞位、按钮位
                if (i == 1) {
                    name = positionNames[0]; position = positionNamesEn[0]; emoji = emojis[0];
                } else if (i == 2) {
                    name = positionNames[1]; position = positionNamesEn[1]; emoji = emojis[1];
                } else if (i == 3) {
                    name = positionNames[2]; position = positionNamesEn[2]; emoji = emojis[2];
                } else if (i >= 4 && i <= 6) {
                    name = positionNames[3]; position = positionNamesEn[3]; emoji = emojis[3];
                } else if (i == 7) {
                    name = positionNames[4]; position = positionNamesEn[4]; emoji = emojis[4];
                } else {
                    name = positionNames[5]; position = positionNamesEn[5]; emoji = emojis[5];
                }
            } else if (playerCount == 9) {
                // 9人桌：小盲位、大盲位、枪口位、中位、中位、中位、中位、关煞位、按钮位
                if (i == 1) {
                    name = positionNames[0]; position = positionNamesEn[0]; emoji = emojis[0];
                } else if (i == 2) {
                    name = positionNames[1]; position = positionNamesEn[1]; emoji = emojis[1];
                } else if (i == 3) {
                    name = positionNames[2]; position = positionNamesEn[2]; emoji = emojis[2];
                } else if (i >= 4 && i <= 7) {
                    name = positionNames[3]; position = positionNamesEn[3]; emoji = emojis[3];
                } else if (i == 8) {
                    name = positionNames[4]; position = positionNamesEn[4]; emoji = emojis[4];
                } else {
                    name = positionNames[5]; position = positionNamesEn[5]; emoji = emojis[5];
                }
            } else {
                // 默认情况
                name = "未知位置"; position = "Unknown"; emoji = "👤";
            }
            
            seatPositions.put(i, Map.of("name", name, "position", position, "emoji", emoji));
        }
        
        return seatPositions;
    }
    
    // 辅助方法：转换牌列表显示格式
    private List<Map<String, String>> convertCardsToDisplay(List<Card> cards) {
        return cards.stream()
                .map(this::convertCardToDisplay)
                .toList();
    }
    
    // 辅助方法：转换单张牌显示格式
    private Map<String, String> convertCardToDisplay(Card card) {
        Map<String, String> cardInfo = new HashMap<>();
        cardInfo.put("display", card.getDisplayName());
        cardInfo.put("chinese", card.getChineseDisplayName());
        cardInfo.put("suit", card.getSuit().getChineseName());
        cardInfo.put("rank", card.getRank().getChineseName());
        cardInfo.put("color", card.isRed() ? "红色" : "黑色");
        return cardInfo;
    }
}
