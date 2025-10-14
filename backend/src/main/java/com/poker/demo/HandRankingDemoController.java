package com.poker.demo;

import com.poker.game.core.Deck;
import com.poker.game.core.GameRanking;
import com.poker.game.core.HandEvaluator;
import com.poker.game.core.HandRank;
import com.poker.game.model.Card;
import com.poker.game.model.Rank;
import com.poker.game.model.Suit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 牌型识别和排名演示控制器
 */
@RestController
@RequestMapping("/api/demo/ranking")
@RequiredArgsConstructor
@Slf4j
public class HandRankingDemoController {
    
    private final ObjectProvider<Deck> deckProvider;
    private final HandEvaluator handEvaluator;
    private final GameRanking gameRanking;
    
    /**
     * 牌型识别演示响应
     */
    public record HandRankingDemoResponse(
        boolean success,
        String message,
        List<Card> communityCards,
        Map<String, PlayerResult> players,
        GameRanking.GameRankingResult ranking,
        String detailedReport,
        long timestamp
    ) {}
    
    /**
     * 玩家结果
     */
    public record PlayerResult(
        int playerId,
        String playerName,
        List<CardInfo> holeCards,
        HandRankInfo handRank,
        int rank,
        boolean isWinner
    ) {}
    
    /**
     * 牌型信息
     */
    public record HandRankInfo(
        String type,
        String typeChinese,
        String description,
        String bestFiveCards,
        int strength
    ) {}
    
    /**
     * 卡牌信息
     */
    public record CardInfo(
        String suit,
        String rank,
        String display,
        String chinese,
        String color
    ) {}
    
    /**
     * 德州扑克牌型识别和排名演示
     */
    @PostMapping("/texas-holdem-ranking")
    public HandRankingDemoResponse texasHoldemRankingDemo(
            @RequestParam(defaultValue = "6") int playerCount,
            @RequestParam(required = false) String customSeed) {
        
        try {
            log.info("开始德州扑克排名演示，玩家数：{}，自定义种子：{}", playerCount, customSeed);
            
            // 验证玩家数量
            if (playerCount < 2 || playerCount > 9) {
                return new HandRankingDemoResponse(
                    false, 
                    "玩家数量必须在2-9之间", 
                    null, null, null, null,
                    System.currentTimeMillis()
                );
            }
            
            // 创建新牌库并洗牌
            Deck deck = deckProvider.getObject();
            deck.shuffle(customSeed);
            
            // 发牌
            Map<Integer, List<Card>> playerHoleCards = new HashMap<>();
            
            // 给每个玩家发2张底牌
            for (int round = 0; round < 2; round++) {
                deck.burnCard(); // 发牌前烧牌
                for (int player = 1; player <= playerCount; player++) {
                    if (!playerHoleCards.containsKey(player)) {
                        playerHoleCards.put(player, new ArrayList<>());
                    }
                    playerHoleCards.get(player).add(deck.dealCard());
                }
            }
            
            // 发公共牌
            List<Card> communityCards = new ArrayList<>();
            
            // 翻牌（3张）
            deck.burnCard();
            for (int i = 0; i < 3; i++) {
                communityCards.add(deck.dealCard());
            }
            
            // 转牌（1张）
            deck.burnCard();
            communityCards.add(deck.dealCard());
            
            // 河牌（1张）
            deck.burnCard();
            communityCards.add(deck.dealCard());
            
            // 计算排名
            GameRanking.GameRankingResult rankingResult = gameRanking.calculateRanking(
                playerHoleCards, communityCards);
            
            // 转换为响应格式
            Map<String, PlayerResult> playersResult = new HashMap<>();
            
            for (GameRanking.PlayerHandInfo playerInfo : rankingResult.getPlayerRankings()) {
                List<CardInfo> holeCardInfos = playerInfo.getHoleCards().stream()
                    .map(this::convertCardToInfo)
                    .collect(Collectors.toList());
                
                HandRankInfo handRankInfo = convertHandRankToInfo(playerInfo.getHandRank());
                
                PlayerResult playerResult = new PlayerResult(
                    playerInfo.getPlayerId(),
                    playerInfo.getPlayerName(),
                    holeCardInfos,
                    handRankInfo,
                    playerInfo.getRank(),
                    playerInfo.isWinner()
                );
                
                playersResult.put(playerInfo.getPlayerName(), playerResult);
            }
            
            // 生成详细报告
            String detailedReport = gameRanking.generateDetailedReport(rankingResult);
            
            log.info("德州扑克排名演示完成：{}", rankingResult.getSummary());
            
            return new HandRankingDemoResponse(
                true,
                "德州扑克排名演示成功",
                communityCards,
                playersResult,
                rankingResult,
                detailedReport,
                System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("德州扑克排名演示失败", e);
            return new HandRankingDemoResponse(
                false,
                "演示失败：" + e.getMessage(),
                null, null, null, null,
                System.currentTimeMillis()
            );
        }
    }
    
    /**
     * 牌型识别测试（指定牌型）
     */
    @PostMapping("/hand-evaluation-test")
    public Map<String, Object> handEvaluationTest(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> cardStrings = (List<String>) request.get("cards");
            
            if (cardStrings == null || cardStrings.size() != 7) {
                return Map.of(
                    "success", false,
                    "error", "必须提供7张牌进行测试"
                );
            }
            
            // 解析卡牌
            List<Card> cards = cardStrings.stream()
                .map(this::parseCard)
                .collect(Collectors.toList());
            
            // 评估牌型
            HandRank handRank = handEvaluator.evaluateHand(cards);
            
            return Map.of(
                "success", true,
                "cards", cards.stream().map(this::convertCardToInfo).collect(Collectors.toList()),
                "handRank", convertHandRankToInfo(handRank),
                "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("牌型识别测试失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 获取所有牌型示例
     */
    @GetMapping("/hand-types-examples")
    public Map<String, Object> getHandTypesExamples() {
        try {
            List<Map<String, Object>> examples = new ArrayList<>();
            
            // 皇家同花顺
            examples.add(createHandTypeExample(
                "ROYAL_FLUSH", "皇家同花顺",
                Arrays.asList("AS", "KS", "QS", "JS", "10S", "9H", "8H"),
                "最强牌型：同花色的A-K-Q-J-10"
            ));
            
            // 同花顺
            examples.add(createHandTypeExample(
                "STRAIGHT_FLUSH", "同花顺",
                Arrays.asList("9H", "8H", "7H", "6H", "5H", "AC", "KD"),
                "同花色的连续五张牌"
            ));
            
            // 四条
            examples.add(createHandTypeExample(
                "FOUR_OF_A_KIND", "四条",
                Arrays.asList("AS", "AH", "AD", "AC", "KS", "QH", "JD"),
                "四张相同点数的牌"
            ));
            
            // 葫芦
            examples.add(createHandTypeExample(
                "FULL_HOUSE", "葫芦",
                Arrays.asList("KS", "KH", "KD", "QS", "QH", "JC", "10D"),
                "三张相同点数 + 一对"
            ));
            
            // 同花
            examples.add(createHandTypeExample(
                "FLUSH", "同花",
                Arrays.asList("AS", "JS", "9S", "7S", "5S", "KH", "QD"),
                "五张同花色的牌"
            ));
            
            // 顺子
            examples.add(createHandTypeExample(
                "STRAIGHT", "顺子",
                Arrays.asList("AS", "KH", "QD", "JC", "10S", "9H", "8D"),
                "连续五张牌（不同花色）"
            ));
            
            // 三条
            examples.add(createHandTypeExample(
                "THREE_OF_A_KIND", "三条",
                Arrays.asList("QS", "QH", "QD", "AS", "KH", "JC", "10D"),
                "三张相同点数的牌"
            ));
            
            // 两对
            examples.add(createHandTypeExample(
                "TWO_PAIR", "两对",
                Arrays.asList("AS", "AH", "KS", "KH", "QD", "JC", "10S"),
                "两个不同的对子"
            ));
            
            // 一对
            examples.add(createHandTypeExample(
                "ONE_PAIR", "一对",
                Arrays.asList("AS", "AH", "KS", "QH", "JD", "10C", "9S"),
                "一个对子"
            ));
            
            // 高牌
            examples.add(createHandTypeExample(
                "HIGH_CARD", "高牌",
                Arrays.asList("AS", "KH", "QD", "JC", "9S", "8H", "7D"),
                "没有任何组合，比较最大的牌"
            ));
            
            return Map.of(
                "success", true,
                "examples", examples,
                "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("获取牌型示例失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    // ========== 辅助方法 ==========
    
    private Map<String, Object> createHandTypeExample(String type, String typeChinese, 
                                                     List<String> cardStrings, String description) {
        List<Card> cards = cardStrings.stream()
            .map(this::parseCard)
            .collect(Collectors.toList());
        
        HandRank handRank = handEvaluator.evaluateHand(cards);
        
        return Map.of(
            "type", type,
            "typeChinese", typeChinese,
            "description", description,
            "cards", cards.stream().map(this::convertCardToInfo).collect(Collectors.toList()),
            "handRank", convertHandRankToInfo(handRank)
        );
    }
    
    private CardInfo convertCardToInfo(Card card) {
        return new CardInfo(
            card.getSuit().name(),
            card.getRank().name(),
            card.getDisplayName(),
            card.getChineseDisplayName(),
            card.getSuit().isRed() ? "红色" : "黑色"
        );
    }
    
    private HandRankInfo convertHandRankToInfo(HandRank handRank) {
        return new HandRankInfo(
            handRank.getHandType().name(),
            handRank.getHandType().getChineseName(),
            handRank.getDescription(),
            handRank.getBestFiveCardsString(),
            handRank.getHandType().getValue()
        );
    }
    
    private Card parseCard(String cardString) {
        // 解析如 "AS", "KH", "10D" 等格式
        if (cardString.length() < 2) {
            throw new IllegalArgumentException("Invalid card format: " + cardString);
        }
        
        String suitChar = cardString.substring(cardString.length() - 1);
        String rankStr = cardString.substring(0, cardString.length() - 1);
        
        // 解析花色
        Suit suit = switch (suitChar) {
            case "S" -> Suit.SPADE;
            case "H" -> Suit.HEART;
            case "C" -> Suit.CLUB;
            case "D" -> Suit.DIAMOND;
            default -> throw new IllegalArgumentException("Invalid suit: " + suitChar);
        };
        
        // 解析点数
        Rank rank = switch (rankStr) {
            case "2" -> Rank.TWO;
            case "3" -> Rank.THREE;
            case "4" -> Rank.FOUR;
            case "5" -> Rank.FIVE;
            case "6" -> Rank.SIX;
            case "7" -> Rank.SEVEN;
            case "8" -> Rank.EIGHT;
            case "9" -> Rank.NINE;
            case "10" -> Rank.TEN;
            case "J" -> Rank.JACK;
            case "Q" -> Rank.QUEEN;
            case "K" -> Rank.KING;
            case "A" -> Rank.ACE;
            default -> throw new IllegalArgumentException("Invalid rank: " + rankStr);
        };
        
        return new Card(suit, rank);
    }
}
