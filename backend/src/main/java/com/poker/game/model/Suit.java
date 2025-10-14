package com.poker.game.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 扑克牌花色枚举
 * 
 * 德州扑克中四种花色在牌型比较时地位相等，
 * 但为了显示和逻辑处理方便，我们定义了顺序
 */
@Getter
@RequiredArgsConstructor
public enum Suit {
    /**
     * 黑桃 ♠ - 传统上认为是最高花色
     */
    SPADE("♠", "黑桃", 4),
    
    /**
     * 红桃 ♥ - 红色花色
     */
    HEART("♥", "红桃", 3),
    
    /**
     * 梅花 ♣ - 黑色花色
     */
    CLUB("♣", "梅花", 2),
    
    /**
     * 方片 ♦ - 红色花色
     */
    DIAMOND("♦", "方片", 1);
    
    private final String symbol;
    private final String chineseName;
    private final int order;
    
    /**
     * 判断是否为红色花色
     */
    public boolean isRed() {
        return this == HEART || this == DIAMOND;
    }
    
    /**
     * 判断是否为黑色花色
     */
    public boolean isBlack() {
        return this == SPADE || this == CLUB;
    }
}
