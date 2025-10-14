package com.poker.game.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 扑克牌点数枚举
 * 
 * 德州扑克中A可以作为最小（A-2-3-4-5顺子）或最大（10-J-Q-K-A顺子）
 * 但在大多数比较中，A的值为14（最大）
 */
@Getter
@RequiredArgsConstructor
public enum Rank {
    TWO(2, "2", "二"),
    THREE(3, "3", "三"),
    FOUR(4, "4", "四"),
    FIVE(5, "5", "五"),
    SIX(6, "6", "六"),
    SEVEN(7, "7", "七"),
    EIGHT(8, "8", "八"),
    NINE(9, "9", "九"),
    TEN(10, "10", "十"),
    JACK(11, "J", "杰克"),
    QUEEN(12, "Q", "皇后"),
    KING(13, "K", "国王"),
    ACE(14, "A", "A");
    
    private final int value;
    private final String symbol;
    private final String chineseName;
    
    /**
     * 获取A作为1时的值（用于A-2-3-4-5顺子）
     */
    public int getLowAceValue() {
        return this == ACE ? 1 : value;
    }
    
    /**
     * 判断是否为人头牌（J、Q、K、A）
     */
    public boolean isFaceCard() {
        return value >= 11;
    }
    
    /**
     * 根据数值获取Rank
     */
    public static Rank fromValue(int value) {
        for (Rank rank : values()) {
            if (rank.value == value) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Invalid rank value: " + value);
    }
    
    /**
     * 根据符号获取Rank
     */
    public static Rank fromSymbol(String symbol) {
        for (Rank rank : values()) {
            if (rank.symbol.equals(symbol)) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Invalid rank symbol: " + symbol);
    }
}
