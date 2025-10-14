package com.poker.game.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * 扑克牌类
 * 
 * 表示一张标准的扑克牌，包含花色和点数
 * 实现了Comparable接口用于排序和比较
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class Card implements Comparable<Card> {
    
    @NonNull
    private final Suit suit;
    
    @NonNull
    private final Rank rank;
    
    /**
     * 获取牌的显示名称，如"A♠"、"K♥"
     */
    public String getDisplayName() {
        return rank.getSymbol() + suit.getSymbol();
    }
    
    /**
     * 获取牌的中文显示名称，如"黑桃A"、"红桃K"
     */
    public String getChineseDisplayName() {
        return suit.getChineseName() + rank.getChineseName();
    }
    
    /**
     * 获取牌的简短表示，用于存储，如"AS"、"KH"
     */
    public String getShortName() {
        return rank.getSymbol() + suit.name().charAt(0);
    }
    
    /**
     * 从简短表示创建Card对象
     * @param shortName 如"AS"、"KH"、"10D"
     */
    public static Card fromShortName(String shortName) {
        if (shortName == null || shortName.length() < 2) {
            throw new IllegalArgumentException("Invalid card format: " + shortName);
        }
        
        String rankStr;
        String suitStr;
        
        // 处理10的特殊情况
        if (shortName.startsWith("10")) {
            rankStr = "10";
            suitStr = shortName.substring(2);
        } else {
            rankStr = shortName.substring(0, shortName.length() - 1);
            suitStr = shortName.substring(shortName.length() - 1);
        }
        
        Rank rank = Rank.fromSymbol(rankStr);
        Suit suit = Suit.valueOf(getSuitFromChar(suitStr.charAt(0)));
        
        return new Card(suit, rank);
    }
    
    private static String getSuitFromChar(char c) {
        switch (c) {
            case 'S': return "SPADE";
            case 'H': return "HEART";
            case 'C': return "CLUB";
            case 'D': return "DIAMOND";
            default: throw new IllegalArgumentException("Invalid suit char: " + c);
        }
    }
    
    /**
     * 比较两张牌的大小（仅比较点数，不比较花色）
     * 返回值：正数表示当前牌更大，负数表示更小，0表示相等
     */
    @Override
    public int compareTo(Card other) {
        return Integer.compare(this.rank.getValue(), other.rank.getValue());
    }
    
    /**
     * 判断是否为红色牌
     */
    public boolean isRed() {
        return suit.isRed();
    }
    
    /**
     * 判断是否为黑色牌
     */
    public boolean isBlack() {
        return suit.isBlack();
    }
    
    
    @Override
    public String toString() {
        return getDisplayName();
    }
    
    /**
     * 获取牌的唯一ID（0-51），用于某些算法优化
     */
    public int getCardId() {
        return suit.ordinal() * 13 + (rank.getValue() - 2);
    }
    
    /**
     * 从ID创建Card对象
     */
    public static Card fromId(int id) {
        if (id < 0 || id > 51) {
            throw new IllegalArgumentException("Card ID must be between 0 and 51");
        }
        
        Suit suit = Suit.values()[id / 13];
        Rank rank = Rank.values()[id % 13];
        return new Card(suit, rank);
    }
}
