package com.poker.game.core;

import com.poker.game.model.Card;
import com.poker.game.model.Rank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * 德州扑克牌型等级
 * 用于表示和比较不同的牌型
 */
@Getter
@RequiredArgsConstructor
public class HandRank implements Comparable<HandRank> {
    
    /**
     * 牌型类型枚举
     */
    public enum HandType {
        HIGH_CARD(1, "高牌"),
        ONE_PAIR(2, "一对"),
        TWO_PAIR(3, "两对"),
        THREE_OF_A_KIND(4, "三条"),
        STRAIGHT(5, "顺子"),
        FLUSH(6, "同花"),
        FULL_HOUSE(7, "葫芦"),
        FOUR_OF_A_KIND(8, "四条"),
        STRAIGHT_FLUSH(9, "同花顺"),
        ROYAL_FLUSH(10, "皇家同花顺");
        
        private final int value;
        private final String chineseName;
        
        HandType(int value, String chineseName) {
            this.value = value;
            this.chineseName = chineseName;
        }
        
        public int getValue() { return value; }
        public String getChineseName() { return chineseName; }
    }
    
    private final HandType handType;
    private final List<Card> bestFiveCards;
    private final List<Rank> kickerRanks; // 用于比较相同牌型的大小
    
    /**
     * 比较两个牌型的大小
     * @param other 另一个牌型
     * @return 正数表示当前牌型更大，0表示相等，负数表示当前牌型更小
     */
    @Override
    public int compareTo(HandRank other) {
        // 首先比较牌型类型
        int typeComparison = Integer.compare(this.handType.getValue(), other.handType.getValue());
        if (typeComparison != 0) {
            return typeComparison;
        }
        
        // 牌型相同时，比较kicker
        for (int i = 0; i < Math.min(kickerRanks.size(), other.kickerRanks.size()); i++) {
            int kickerComparison = Integer.compare(
                this.kickerRanks.get(i).getValue(), 
                other.kickerRanks.get(i).getValue()
            );
            if (kickerComparison != 0) {
                return kickerComparison;
            }
        }
        
        return 0; // 完全相等
    }
    
    /**
     * 获取牌型描述
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(handType.getChineseName());
        
        switch (handType) {
            case ONE_PAIR:
            case THREE_OF_A_KIND:
            case FOUR_OF_A_KIND:
                sb.append("(").append(kickerRanks.get(0).getChineseName()).append(")");
                break;
            case TWO_PAIR:
                sb.append("(").append(kickerRanks.get(0).getChineseName())
                  .append("和").append(kickerRanks.get(1).getChineseName()).append(")");
                break;
            case FULL_HOUSE:
                sb.append("(").append(kickerRanks.get(0).getChineseName())
                  .append("带").append(kickerRanks.get(1).getChineseName()).append(")");
                break;
            case STRAIGHT:
            case STRAIGHT_FLUSH:
                sb.append("(").append(kickerRanks.get(0).getChineseName()).append("高)");
                break;
            case FLUSH:
            case HIGH_CARD:
                sb.append("(").append(kickerRanks.get(0).getChineseName()).append("高)");
                break;
            case ROYAL_FLUSH:
                sb.append("(").append(bestFiveCards.get(0).getSuit().getChineseName()).append(")");
                break;
        }
        
        return sb.toString();
    }
    
    /**
     * 获取最佳五张牌的字符串表示
     */
    public String getBestFiveCardsString() {
        return bestFiveCards.stream()
            .map(Card::getDisplayName)
            .reduce((a, b) -> a + " " + b)
            .orElse("");
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HandRank handRank = (HandRank) obj;
        return handType == handRank.handType && 
               Objects.equals(kickerRanks, handRank.kickerRanks);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(handType, kickerRanks);
    }
    
    @Override
    public String toString() {
        return getDescription() + " - " + getBestFiveCardsString();
    }
}
