package com.poker.game.core;

import com.poker.game.model.Card;
import com.poker.game.model.Rank;
import com.poker.game.model.Suit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 德州扑克牌型识别器
 * 从7张牌中识别最佳的5张牌组合
 */
@Component
@Slf4j
public class HandEvaluator {
    
    /**
     * 评估7张牌的最佳牌型
     * @param sevenCards 7张牌（2张底牌 + 5张公共牌）
     * @return 最佳牌型
     */
    public HandRank evaluateHand(List<Card> sevenCards) {
        if (sevenCards.size() != 7) {
            throw new IllegalArgumentException("必须提供7张牌进行评估");
        }
        
        // 生成所有可能的5张牌组合（C(7,5) = 21种）
        List<List<Card>> allCombinations = generateCombinations(sevenCards, 5);
        
        HandRank bestHand = null;
        for (List<Card> combination : allCombinations) {
            HandRank currentHand = evaluateFiveCards(combination);
            if (bestHand == null || currentHand.compareTo(bestHand) > 0) {
                bestHand = currentHand;
            }
        }
        
        log.debug("评估7张牌: {} -> 最佳牌型: {}", 
            sevenCards.stream().map(Card::getDisplayName).collect(Collectors.joining(", ")),
            bestHand.toString());
        
        return bestHand;
    }
    
    /**
     * 评估5张牌的牌型
     */
    private HandRank evaluateFiveCards(List<Card> fiveCards) {
        // 按点数排序（降序）
        List<Card> sortedCards = fiveCards.stream()
            .sorted((a, b) -> Integer.compare(b.getRank().getValue(), a.getRank().getValue()))
            .collect(Collectors.toList());
        
        // 检查各种牌型
        if (isRoyalFlush(sortedCards)) {
            return createRoyalFlush(sortedCards);
        }
        if (isStraightFlush(sortedCards)) {
            return createStraightFlush(sortedCards);
        }
        if (isFourOfAKind(sortedCards)) {
            return createFourOfAKind(sortedCards);
        }
        if (isFullHouse(sortedCards)) {
            return createFullHouse(sortedCards);
        }
        if (isFlush(sortedCards)) {
            return createFlush(sortedCards);
        }
        if (isStraight(sortedCards)) {
            return createStraight(sortedCards);
        }
        if (isThreeOfAKind(sortedCards)) {
            return createThreeOfAKind(sortedCards);
        }
        if (isTwoPair(sortedCards)) {
            return createTwoPair(sortedCards);
        }
        if (isOnePair(sortedCards)) {
            return createOnePair(sortedCards);
        }
        
        return createHighCard(sortedCards);
    }
    
    // ========== 牌型检查方法 ==========
    
    private boolean isRoyalFlush(List<Card> cards) {
        return isStraightFlush(cards) && cards.get(0).getRank() == Rank.ACE;
    }
    
    private boolean isStraightFlush(List<Card> cards) {
        return isFlush(cards) && isStraight(cards);
    }
    
    private boolean isFourOfAKind(List<Card> cards) {
        Map<Rank, Long> rankCounts = getRankCounts(cards);
        return rankCounts.containsValue(4L);
    }
    
    private boolean isFullHouse(List<Card> cards) {
        Map<Rank, Long> rankCounts = getRankCounts(cards);
        return rankCounts.containsValue(3L) && rankCounts.containsValue(2L);
    }
    
    private boolean isFlush(List<Card> cards) {
        Suit firstSuit = cards.get(0).getSuit();
        return cards.stream().allMatch(card -> card.getSuit() == firstSuit);
    }
    
    private boolean isStraight(List<Card> cards) {
        List<Integer> values = cards.stream()
            .map(card -> card.getRank().getValue())
            .sorted()
            .collect(Collectors.toList());
        
        // 检查A-2-3-4-5（轮子顺）
        if (values.equals(Arrays.asList(2, 3, 4, 5, 14))) {
            return true;
        }
        
        // 检查普通顺子
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) - values.get(i-1) != 1) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isThreeOfAKind(List<Card> cards) {
        Map<Rank, Long> rankCounts = getRankCounts(cards);
        return rankCounts.containsValue(3L) && !rankCounts.containsValue(2L);
    }
    
    private boolean isTwoPair(List<Card> cards) {
        Map<Rank, Long> rankCounts = getRankCounts(cards);
        long pairCount = rankCounts.values().stream().mapToLong(count -> count == 2 ? 1 : 0).sum();
        return pairCount == 2;
    }
    
    private boolean isOnePair(List<Card> cards) {
        Map<Rank, Long> rankCounts = getRankCounts(cards);
        return rankCounts.containsValue(2L) && !rankCounts.containsValue(3L);
    }
    
    // ========== 牌型创建方法 ==========
    
    private HandRank createRoyalFlush(List<Card> cards) {
        return new HandRank(
            HandRank.HandType.ROYAL_FLUSH,
            new ArrayList<>(cards),
            Arrays.asList(Rank.ACE)
        );
    }
    
    private HandRank createStraightFlush(List<Card> cards) {
        Rank highCard = getStraightHighCard(cards);
        return new HandRank(
            HandRank.HandType.STRAIGHT_FLUSH,
            new ArrayList<>(cards),
            Arrays.asList(highCard)
        );
    }
    
    private HandRank createFourOfAKind(List<Card> cards) {
        Map<Rank, Long> rankCounts = getRankCounts(cards);
        Rank fourRank = rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 4)
            .map(Map.Entry::getKey)
            .findFirst().orElseThrow();
        
        Rank kicker = rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 1)
            .map(Map.Entry::getKey)
            .findFirst().orElseThrow();
        
        return new HandRank(
            HandRank.HandType.FOUR_OF_A_KIND,
            new ArrayList<>(cards),
            Arrays.asList(fourRank, kicker)
        );
    }
    
    private HandRank createFullHouse(List<Card> cards) {
        Map<Rank, Long> rankCounts = getRankCounts(cards);
        Rank threeRank = rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 3)
            .map(Map.Entry::getKey)
            .findFirst().orElseThrow();
        
        Rank pairRank = rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 2)
            .map(Map.Entry::getKey)
            .findFirst().orElseThrow();
        
        return new HandRank(
            HandRank.HandType.FULL_HOUSE,
            new ArrayList<>(cards),
            Arrays.asList(threeRank, pairRank)
        );
    }
    
    private HandRank createFlush(List<Card> cards) {
        List<Rank> kickers = cards.stream()
            .map(Card::getRank)
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());
        
        return new HandRank(
            HandRank.HandType.FLUSH,
            new ArrayList<>(cards),
            kickers
        );
    }
    
    private HandRank createStraight(List<Card> cards) {
        Rank highCard = getStraightHighCard(cards);
        return new HandRank(
            HandRank.HandType.STRAIGHT,
            new ArrayList<>(cards),
            Arrays.asList(highCard)
        );
    }
    
    private HandRank createThreeOfAKind(List<Card> cards) {
        Map<Rank, Long> rankCounts = getRankCounts(cards);
        Rank threeRank = rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 3)
            .map(Map.Entry::getKey)
            .findFirst().orElseThrow();
        
        List<Rank> kickers = rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 1)
            .map(Map.Entry::getKey)
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());
        
        List<Rank> allKickers = new ArrayList<>();
        allKickers.add(threeRank);
        allKickers.addAll(kickers);
        
        return new HandRank(
            HandRank.HandType.THREE_OF_A_KIND,
            new ArrayList<>(cards),
            allKickers
        );
    }
    
    private HandRank createTwoPair(List<Card> cards) {
        Map<Rank, Long> rankCounts = getRankCounts(cards);
        List<Rank> pairs = rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 2)
            .map(Map.Entry::getKey)
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());
        
        Rank kicker = rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 1)
            .map(Map.Entry::getKey)
            .findFirst().orElseThrow();
        
        List<Rank> allKickers = new ArrayList<>(pairs);
        allKickers.add(kicker);
        
        return new HandRank(
            HandRank.HandType.TWO_PAIR,
            new ArrayList<>(cards),
            allKickers
        );
    }
    
    private HandRank createOnePair(List<Card> cards) {
        Map<Rank, Long> rankCounts = getRankCounts(cards);
        Rank pairRank = rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 2)
            .map(Map.Entry::getKey)
            .findFirst().orElseThrow();
        
        List<Rank> kickers = rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 1)
            .map(Map.Entry::getKey)
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());
        
        List<Rank> allKickers = new ArrayList<>();
        allKickers.add(pairRank);
        allKickers.addAll(kickers);
        
        return new HandRank(
            HandRank.HandType.ONE_PAIR,
            new ArrayList<>(cards),
            allKickers
        );
    }
    
    private HandRank createHighCard(List<Card> cards) {
        List<Rank> kickers = cards.stream()
            .map(Card::getRank)
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());
        
        return new HandRank(
            HandRank.HandType.HIGH_CARD,
            new ArrayList<>(cards),
            kickers
        );
    }
    
    // ========== 辅助方法 ==========
    
    private Map<Rank, Long> getRankCounts(List<Card> cards) {
        return cards.stream()
            .collect(Collectors.groupingBy(Card::getRank, Collectors.counting()));
    }
    
    private Rank getStraightHighCard(List<Card> cards) {
        List<Integer> values = cards.stream()
            .map(card -> card.getRank().getValue())
            .sorted()
            .collect(Collectors.toList());
        
        // A-2-3-4-5 顺子，5是高牌
        if (values.equals(Arrays.asList(2, 3, 4, 5, 14))) {
            return Rank.FIVE;
        }
        
        // 普通顺子，最大的牌是高牌
        return cards.stream()
            .map(Card::getRank)
            .max(Comparator.comparing(Rank::getValue))
            .orElseThrow();
    }
    
    /**
     * 生成组合
     */
    private List<List<Card>> generateCombinations(List<Card> cards, int r) {
        List<List<Card>> combinations = new ArrayList<>();
        generateCombinationsHelper(cards, r, 0, new ArrayList<>(), combinations);
        return combinations;
    }
    
    private void generateCombinationsHelper(List<Card> cards, int r, int start, 
                                          List<Card> current, List<List<Card>> result) {
        if (current.size() == r) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateCombinationsHelper(cards, r, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
