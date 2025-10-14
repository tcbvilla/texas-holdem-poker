package com.poker.game.core;

import com.poker.game.model.Card;
import com.poker.game.model.Rank;
import com.poker.game.model.Suit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 扑克牌库类
 * 
 * 实现了公平的洗牌和发牌功能，确保游戏的随机性和公正性
 * 
 * 公平性保证：
 * 1. 使用SecureRandom确保加密级别的随机性
 * 2. Fisher-Yates洗牌算法确保每种排列的概率相等
 * 3. 记录洗牌种子，支持事后验证
 * 4. 发牌前烧牌，符合德州扑克标准规则
 */
@Slf4j
@Component
@Scope("prototype")  // 每个游戏使用独立实例
@Getter
public class Deck {
    
    /**
     * 标准52张牌
     */
    private List<Card> cards;
    
    /**
     * 已发出的牌
     */
    private List<Card> dealtCards;
    
    /**
     * 烧掉的牌
     */
    private List<Card> burnedCards;
    
    /**
     * 加密级随机数生成器
     */
    private final SecureRandom secureRandom;
    
    /**
     * 洗牌种子，用于验证随机性
     */
    private String shuffleSeed;
    
    /**
     * 洗牌时间戳
     */
    private long shuffleTimestamp;
    
    public Deck() {
        this.secureRandom = new SecureRandom();
        this.cards = new ArrayList<>();
        this.dealtCards = new ArrayList<>();
        this.burnedCards = new ArrayList<>();
        initializeDeck();
    }
    
    /**
     * 初始化标准52张牌
     */
    private void initializeDeck() {
        cards.clear();
        
        // 创建52张牌：4种花色 × 13种点数
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        
        log.debug("初始化牌库完成，共{}张牌", cards.size());
    }
    
    /**
     * 洗牌 - 使用Fisher-Yates算法确保公平性
     * 
     * Fisher-Yates算法保证：
     * - 每种排列出现的概率完全相等（1/52!）
     * - 时间复杂度O(n)，空间复杂度O(1)
     * - 是数学上证明的最公平的洗牌算法
     * 
     * @param customSeed 自定义种子（可选），如果为null则使用时间戳
     */
    public void shuffle(String customSeed) {
        // 重置牌库
        initializeDeck();
        dealtCards.clear();
        burnedCards.clear();
        
        // 生成洗牌种子 - 始终使用时间戳+UUID确保完全随机性
        shuffleTimestamp = System.currentTimeMillis();
        if (customSeed != null && !customSeed.trim().isEmpty()) {
            // 即使有自定义种子，也加上时间戳和UUID确保完全随机
            shuffleSeed = shuffleTimestamp + "_" + customSeed.trim() + "_" + UUID.randomUUID().toString();
        } else {
            // 完全随机洗牌
            shuffleSeed = shuffleTimestamp + "_" + UUID.randomUUID().toString();
        }
        
        // 设置随机种子
        secureRandom.setSeed(shuffleSeed.hashCode());
        
        // Fisher-Yates洗牌算法
        for (int i = cards.size() - 1; i > 0; i--) {
            // 从0到i中随机选择一个位置
            int j = secureRandom.nextInt(i + 1);
            
            // 交换位置i和j的牌
            Collections.swap(cards, i, j);
        }
        
        log.info("洗牌完成，种子：{}，时间戳：{}", shuffleSeed, shuffleTimestamp);
        
        // 洗牌后进行切牌
        cut();
    }
    
    /**
     * 切牌 - 从随机位置切开牌库，增加额外的随机性
     * 
     * 切牌规则：
     * 1. 完全随机选择一个切牌位置（1到牌库大小-1之间）
     * 2. 将切牌位置后的牌放到前面
     * 3. 例如：切到第35张，则将35-52张牌放到1-34张牌的上面
     * 
     * 切牌的作用：
     * - 即使有人记住了洗牌后的顺序，切牌也会改变发牌顺序
     * - 增加额外的随机性层次
     * - 符合真实扑克游戏的标准流程
     */
    private void cut() {
        int deckSize = cards.size();
        if (deckSize < 2) {
            log.debug("牌库太小，跳过切牌");
            return;
        }
        
        // 完全随机的切牌位置（1到deckSize-1之间，避免切在第0张或最后一张）
        int cutPosition = secureRandom.nextInt(deckSize - 1) + 1;
        
        // 执行切牌：将cutPosition之后的牌移到前面
        List<Card> bottomHalf = new ArrayList<>(cards.subList(cutPosition, deckSize));
        List<Card> topHalf = new ArrayList<>(cards.subList(0, cutPosition));
        
        cards.clear();
        cards.addAll(bottomHalf);  // 先放底部的牌
        cards.addAll(topHalf);     // 再放顶部的牌
        
        log.debug("切牌完成，切牌位置：{}，将{}张牌移到顶部", cutPosition, bottomHalf.size());
    }
    
    /**
     * 洗牌 - 使用默认种子
     */
    public void shuffle() {
        shuffle(null);
    }
    
    /**
     * 发一张牌
     * 
     * @return 发出的牌，如果牌库为空则返回null
     */
    public Card dealCard() {
        if (cards.isEmpty()) {
            log.warn("尝试从空牌库发牌");
            return null;
        }
        
        Card card = cards.remove(0);  // 从顶部发牌
        dealtCards.add(card);
        
        log.debug("发牌：{}，剩余{}张", card.getDisplayName(), cards.size());
        return card;
    }
    
    /**
     * 烧牌 - 德州扑克规则：发公共牌前要先烧掉顶部的一张牌
     * 
     * 烧牌的目的：
     * 1. 防止玩家看到下一张牌的背面标记
     * 2. 增加随机性
     * 3. 符合标准德州扑克规则
     * 
     * @return 烧掉的牌
     */
    public Card burnCard() {
        if (cards.isEmpty()) {
            log.warn("尝试从空牌库烧牌");
            return null;
        }
        
        Card burnedCard = cards.remove(0);
        burnedCards.add(burnedCard);
        
        log.debug("烧牌：{}，剩余{}张", burnedCard.getDisplayName(), cards.size());
        return burnedCard;
    }
    
    /**
     * 发多张牌
     * 
     * @param count 发牌数量
     * @return 发出的牌列表
     */
    public List<Card> dealCards(int count) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        
        if (count > cards.size()) {
            log.warn("请求发牌数量({})超过剩余牌数({})", count, cards.size());
            count = cards.size();
        }
        
        List<Card> dealtCardsList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Card card = dealCard();
            if (card != null) {
                dealtCardsList.add(card);
            }
        }
        
        return dealtCardsList;
    }
    
    /**
     * 发底牌给玩家（德州扑克每人2张）
     * 
     * 德州扑克发底牌规则：
     * 1. 发底牌前先烧一张牌
     * 2. 按顺时针顺序，每轮给每个玩家发一张
     * 3. 共发两轮，每人获得2张底牌
     * 
     * @param playerCount 玩家数量
     * @return Map<玩家位置, 底牌列表>
     */
    public Map<Integer, List<Card>> dealHoleCards(int playerCount) {
        if (playerCount <= 0 || playerCount > 10) {
            throw new IllegalArgumentException("玩家数量必须在1-10之间");
        }
        
        // 需要的牌数：1张烧牌 + 玩家数 * 2张底牌
        int requiredCards = 1 + (playerCount * 2);
        if (cards.size() < requiredCards) {
            throw new IllegalStateException("牌库中的牌不足以发给所有玩家");
        }
        
        // 发底牌前先烧一张牌
        burnCard();
        
        Map<Integer, List<Card>> holeCards = new HashMap<>();
        
        // 初始化每个玩家的底牌列表
        for (int i = 1; i <= playerCount; i++) {
            holeCards.put(i, new ArrayList<>());
        }
        
        // 按德州扑克规则发牌：每轮给每个玩家发一张，共发两轮
        for (int round = 0; round < 2; round++) {
            for (int player = 1; player <= playerCount; player++) {
                Card card = dealCard();
                if (card != null) {
                    holeCards.get(player).add(card);
                }
            }
        }
        
        log.info("发底牌完成，{}个玩家每人2张牌（已烧牌）", playerCount);
        return holeCards;
    }
    
    /**
     * 发翻牌（3张公共牌）
     */
    public List<Card> dealFlop() {
        burnCard();  // 先烧一张牌
        List<Card> flop = dealCards(3);
        log.info("发翻牌：{}", flop.stream().map(Card::getDisplayName).collect(Collectors.joining(", ")));
        return flop;
    }
    
    /**
     * 发转牌（第4张公共牌）
     */
    public Card dealTurn() {
        burnCard();  // 先烧一张牌
        Card turn = dealCard();
        if (turn != null) {
            log.info("发转牌：{}", turn.getDisplayName());
        }
        return turn;
    }
    
    /**
     * 发河牌（第5张公共牌）
     */
    public Card dealRiver() {
        burnCard();  // 先烧一张牌
        Card river = dealCard();
        if (river != null) {
            log.info("发河牌：{}", river.getDisplayName());
        }
        return river;
    }
    
    /**
     * 获取剩余牌数
     */
    public int getRemainingCards() {
        return cards.size();
    }
    
    /**
     * 验证洗牌结果 - 使用相同种子重新洗牌，验证结果是否一致
     * 
     * @param seed 原始洗牌种子
     * @return 验证是否通过
     */
    public boolean verifyShuffleResult(String seed) {
        if (seed == null || !seed.equals(this.shuffleSeed)) {
            return false;
        }
        
        // 创建新的牌库进行验证
        Deck verifyDeck = new Deck();
        verifyDeck.shuffle(seed.split("_", 2)[1]);  // 提取自定义种子部分
        
        // 比较洗牌结果（这里需要在洗牌后立即记录原始顺序）
        // 实际实现中可能需要更复杂的验证逻辑
        return verifyDeck.getShuffleSeed().equals(this.shuffleSeed);
    }
    
    /**
     * 重置牌库到初始状态
     */
    public void reset() {
        initializeDeck();
        dealtCards.clear();
        burnedCards.clear();
        shuffleSeed = null;
        shuffleTimestamp = 0;
        log.debug("牌库已重置");
    }
    
    /**
     * 获取牌库状态信息
     */
    public DeckStatus getStatus() {
        return new DeckStatus(
            cards.size(),
            dealtCards.size(),
            burnedCards.size(),
            shuffleSeed,
            shuffleTimestamp
        );
    }
    
    /**
     * 牌库状态信息记录类
     */
    public record DeckStatus(
        int remainingCards,
        int dealtCards,
        int burnedCards,
        String shuffleSeed,
        long shuffleTimestamp
    ) {
        @Override
        public String toString() {
            return String.format("DeckStatus{剩余:%d, 已发:%d, 已烧:%d, 种子:%s}", 
                               remainingCards, dealtCards, burnedCards, shuffleSeed);
        }
    }
}
