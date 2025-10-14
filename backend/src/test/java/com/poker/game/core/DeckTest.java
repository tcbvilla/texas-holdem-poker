package com.poker.game.core;

import com.poker.game.model.Card;
import com.poker.game.model.Rank;
import com.poker.game.model.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deck类的单元测试
 * 
 * 测试重点：
 * 1. 基本功能正确性
 * 2. 洗牌随机性验证
 * 3. 发牌规则符合德州扑克标准
 * 4. 边界条件处理
 */
@SpringBootTest
public class DeckTest {
    
    private Deck deck;
    
    @BeforeEach
    void setUp() {
        deck = new Deck();
    }
    
    @Test
    void testDeckInitialization() {
        // 测试牌库初始化
        assertEquals(52, deck.getRemainingCards(), "新牌库应该有52张牌");
        assertEquals(0, deck.getDealtCards().size(), "初始状态不应该有已发的牌");
        assertEquals(0, deck.getBurnedCards().size(), "初始状态不应该有烧掉的牌");
    }
    
    @Test
    void testShuffle() {
        // 测试洗牌功能
        String customSeed = "test_seed_123";
        deck.shuffle(customSeed);
        
        assertNotNull(deck.getShuffleSeed(), "洗牌后应该有种子");
        assertTrue(deck.getShuffleSeed().contains(customSeed), "种子应该包含自定义部分");
        assertTrue(deck.getShuffleTimestamp() > 0, "应该记录洗牌时间戳");
    }
    
    @Test
    void testCutAfterShuffle() {
        // 测试洗牌后的切牌功能
        // 记录洗牌前的牌序（用于验证切牌确实改变了顺序）
        
        // 使用固定种子确保可重现
        String seed = "cut_test_seed";
        
        // 第一次洗牌，不记录牌序（因为切牌是洗牌的一部分）
        deck.shuffle(seed);
        List<Card> cardsAfterShuffleAndCut = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            cardsAfterShuffleAndCut.add(deck.dealCard());
        }
        
        // 重新洗牌，验证相同种子产生相同结果（包括切牌）
        Deck deck2 = new Deck();
        deck2.shuffle(seed);
        List<Card> cardsAfterShuffleAndCut2 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            cardsAfterShuffleAndCut2.add(deck2.dealCard());
        }
        
        // 相同种子应该产生相同的结果（包括切牌效果）
        assertEquals(cardsAfterShuffleAndCut, cardsAfterShuffleAndCut2, 
                    "相同种子的洗牌+切牌应该产生相同结果");
    }
    
    @Test
    void testShuffleRandomness() {
        // 测试洗牌随机性 - 多次洗牌结果应该不同
        deck.shuffle();
        Card firstCard1 = deck.dealCard();
        
        deck.shuffle();
        Card firstCard2 = deck.dealCard();
        
        deck.shuffle();
        Card firstCard3 = deck.dealCard();
        
        // 三次洗牌的第一张牌不太可能都相同（概率 = 1/52 * 1/52 ≈ 0.037%）
        assertFalse(firstCard1.equals(firstCard2) && firstCard2.equals(firstCard3),
                   "连续三次洗牌的第一张牌都相同，可能存在随机性问题");
    }
    
    @Test
    void testShuffleReproducibility() {
        // 测试洗牌可重现性 - 相同种子应该产生相同结果
        String seed = "reproducible_test";
        
        // 第一次洗牌
        Deck deck1 = new Deck();
        deck1.shuffle(seed);
        List<Card> cards1 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            cards1.add(deck1.dealCard());
        }
        
        // 第二次洗牌（相同种子）
        Deck deck2 = new Deck();
        deck2.shuffle(seed);
        List<Card> cards2 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            cards2.add(deck2.dealCard());
        }
        
        assertEquals(cards1, cards2, "相同种子应该产生相同的洗牌结果");
    }
    
    @Test
    void testDealCard() {
        // 测试发牌功能
        deck.shuffle();
        
        Card card = deck.dealCard();
        assertNotNull(card, "应该能发出牌");
        assertEquals(51, deck.getRemainingCards(), "发牌后剩余牌数应该减少");
        assertEquals(1, deck.getDealtCards().size(), "已发牌数应该增加");
        assertTrue(deck.getDealtCards().contains(card), "已发牌列表应该包含刚发的牌");
    }
    
    @Test
    void testDealAllCards() {
        // 测试发完所有牌
        deck.shuffle();
        
        List<Card> allCards = new ArrayList<>();
        while (deck.getRemainingCards() > 0) {
            Card card = deck.dealCard();
            assertNotNull(card, "在牌库不为空时应该能发出牌");
            allCards.add(card);
        }
        
        assertEquals(52, allCards.size(), "应该能发出52张牌");
        assertEquals(0, deck.getRemainingCards(), "发完后剩余牌数应该为0");
        
        // 验证没有重复的牌
        Set<Card> uniqueCards = new HashSet<>(allCards);
        assertEquals(52, uniqueCards.size(), "52张牌应该都不相同");
        
        // 尝试再发牌应该返回null
        Card extraCard = deck.dealCard();
        assertNull(extraCard, "牌库为空时发牌应该返回null");
    }
    
    @Test
    void testBurnCard() {
        // 测试烧牌功能
        deck.shuffle();
        int initialCards = deck.getRemainingCards();
        
        Card burnedCard = deck.burnCard();
        
        assertNotNull(burnedCard, "应该能烧掉一张牌");
        assertEquals(initialCards - 1, deck.getRemainingCards(), "烧牌后剩余牌数应该减少");
        assertEquals(1, deck.getBurnedCards().size(), "烧牌列表应该增加");
        assertTrue(deck.getBurnedCards().contains(burnedCard), "烧牌列表应该包含被烧的牌");
    }
    
    @Test
    void testDealHoleCards() {
        // 测试发底牌功能
        deck.shuffle();
        int playerCount = 6;
        
        Map<Integer, List<Card>> holeCards = deck.dealHoleCards(playerCount);
        
        assertEquals(playerCount, holeCards.size(), "应该为每个玩家发底牌");
        
        // 验证每个玩家都有2张底牌
        for (int i = 1; i <= playerCount; i++) {
            assertTrue(holeCards.containsKey(i), "应该包含玩家" + i + "的底牌");
            assertEquals(2, holeCards.get(i).size(), "每个玩家应该有2张底牌");
        }
        
        // 验证总共发了13张牌（1张烧牌 + 12张底牌）
        assertEquals(52 - 13, deck.getRemainingCards(), "发底牌后应该剩余39张牌");
        assertEquals(12, deck.getDealtCards().size(), "应该发出12张牌");
        assertEquals(1, deck.getBurnedCards().size(), "应该烧掉1张牌");
        
        // 验证没有重复的牌
        Set<Card> allHoleCards = new HashSet<>();
        holeCards.values().forEach(allHoleCards::addAll);
        assertEquals(12, allHoleCards.size(), "所有底牌应该都不相同");
    }
    
    @Test
    void testDealFlop() {
        // 测试发翻牌
        deck.shuffle();
        int initialCards = deck.getRemainingCards();
        
        List<Card> flop = deck.dealFlop();
        
        assertEquals(3, flop.size(), "翻牌应该有3张");
        assertEquals(initialCards - 4, deck.getRemainingCards(), "应该消耗4张牌（1张烧牌+3张翻牌）");
        assertEquals(1, deck.getBurnedCards().size(), "应该烧掉1张牌");
        assertEquals(3, deck.getDealtCards().size(), "应该发出3张牌");
    }
    
    @Test
    void testDealTurn() {
        // 测试发转牌
        deck.shuffle();
        deck.dealFlop();  // 先发翻牌
        int cardsBeforeTurn = deck.getRemainingCards();
        
        Card turn = deck.dealTurn();
        
        assertNotNull(turn, "应该能发出转牌");
        assertEquals(cardsBeforeTurn - 2, deck.getRemainingCards(), "应该消耗2张牌（1张烧牌+1张转牌）");
        assertEquals(2, deck.getBurnedCards().size(), "总共应该烧掉2张牌");
    }
    
    @Test
    void testDealRiver() {
        // 测试发河牌
        deck.shuffle();
        deck.dealFlop();  // 先发翻牌
        deck.dealTurn();  // 再发转牌
        int cardsBeforeRiver = deck.getRemainingCards();
        
        Card river = deck.dealRiver();
        
        assertNotNull(river, "应该能发出河牌");
        assertEquals(cardsBeforeRiver - 2, deck.getRemainingCards(), "应该消耗2张牌（1张烧牌+1张河牌）");
        assertEquals(3, deck.getBurnedCards().size(), "总共应该烧掉3张牌");
    }
    
    @Test
    void testCompleteTexasHoldemDeal() {
        // 测试完整的德州扑克发牌流程
        deck.shuffle();
        
        // 1. 发底牌给6个玩家（1张烧牌 + 12张底牌）
        Map<Integer, List<Card>> holeCards = deck.dealHoleCards(6);
        assertEquals(39, deck.getRemainingCards(), "发底牌后应该剩余39张");
        assertEquals(1, deck.getBurnedCards().size(), "发底牌应该烧1张牌");
        
        // 2. 发翻牌（1张烧牌 + 3张翻牌）
        List<Card> flop = deck.dealFlop();
        assertEquals(35, deck.getRemainingCards(), "发翻牌后应该剩余35张");
        assertEquals(2, deck.getBurnedCards().size(), "总共应该烧2张牌");
        
        // 3. 发转牌（1张烧牌 + 1张转牌）
        Card turn = deck.dealTurn();
        assertEquals(33, deck.getRemainingCards(), "发转牌后应该剩余33张");
        assertEquals(3, deck.getBurnedCards().size(), "总共应该烧3张牌");
        
        // 4. 发河牌（1张烧牌 + 1张河牌）
        Card river = deck.dealRiver();
        assertEquals(31, deck.getRemainingCards(), "发河牌后应该剩余31张");
        assertEquals(4, deck.getBurnedCards().size(), "总共应该烧4张牌");
        
        // 验证总计算
        int totalUsed = 12 + 4 + 5;  // 底牌12 + 烧牌4 + 公共牌5
        assertEquals(52 - totalUsed, deck.getRemainingCards(), "总牌数计算应该正确");
        
        // 验证公共牌
        List<Card> communityCards = new ArrayList<>(flop);
        communityCards.add(turn);
        communityCards.add(river);
        assertEquals(5, communityCards.size(), "公共牌应该有5张");
        
        // 验证没有重复
        Set<Card> allUsedCards = new HashSet<>();
        holeCards.values().forEach(allUsedCards::addAll);
        allUsedCards.addAll(communityCards);
        allUsedCards.addAll(deck.getBurnedCards());
        assertEquals(21, allUsedCards.size(), "所有使用的牌应该都不相同");
    }
    
    @Test
    void testInvalidPlayerCount() {
        // 测试无效玩家数量
        deck.shuffle();
        
        assertThrows(IllegalArgumentException.class, () -> {
            deck.dealHoleCards(0);
        }, "玩家数量为0应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> {
            deck.dealHoleCards(11);
        }, "玩家数量超过10应该抛出异常");
    }
    
    @Test
    void testInsufficientCards() {
        // 测试牌数不足的情况
        deck.shuffle();
        
        // 先发掉大部分牌
        for (int i = 0; i < 50; i++) {
            deck.dealCard();
        }
        
        // 尝试发底牌给6个玩家（需要12张牌，但只剩2张）
        assertThrows(IllegalStateException.class, () -> {
            deck.dealHoleCards(6);
        }, "牌数不足时应该抛出异常");
    }
    
    @Test
    void testReset() {
        // 测试重置功能
        deck.shuffle();
        deck.dealCard();
        deck.burnCard();
        
        // 重置前的状态
        assertTrue(deck.getRemainingCards() < 52, "重置前应该有牌被使用");
        assertTrue(deck.getDealtCards().size() > 0, "重置前应该有已发的牌");
        assertTrue(deck.getBurnedCards().size() > 0, "重置前应该有烧掉的牌");
        
        // 执行重置
        deck.reset();
        
        // 重置后的状态
        assertEquals(52, deck.getRemainingCards(), "重置后应该有52张牌");
        assertEquals(0, deck.getDealtCards().size(), "重置后不应该有已发的牌");
        assertEquals(0, deck.getBurnedCards().size(), "重置后不应该有烧掉的牌");
        assertNull(deck.getShuffleSeed(), "重置后种子应该为空");
        assertEquals(0, deck.getShuffleTimestamp(), "重置后时间戳应该为0");
    }
    
    @Test
    void testDeckStatus() {
        // 测试牌库状态信息
        deck.shuffle();
        deck.dealCard();
        deck.burnCard();
        
        Deck.DeckStatus status = deck.getStatus();
        
        assertEquals(50, status.remainingCards(), "状态应该反映正确的剩余牌数");
        assertEquals(1, status.dealtCards(), "状态应该反映正确的已发牌数");
        assertEquals(1, status.burnedCards(), "状态应该反映正确的烧牌数");
        assertNotNull(status.shuffleSeed(), "状态应该包含洗牌种子");
        assertTrue(status.shuffleTimestamp() > 0, "状态应该包含洗牌时间戳");
    }
    
    /**
     * 统计测试 - 验证洗牌的统计分布
     * 注意：这个测试可能需要较长时间，可以在需要时单独运行
     */
    @Test
    void testShuffleDistribution() {
        // 统计每张牌出现在第一位的次数
        Map<Card, Integer> firstCardCounts = new HashMap<>();
        int iterations = 1000;  // 可以增加到10000以获得更准确的结果
        
        for (int i = 0; i < iterations; i++) {
            Deck testDeck = new Deck();
            testDeck.shuffle();
            Card firstCard = testDeck.dealCard();
            
            firstCardCounts.merge(firstCard, 1, Integer::sum);
        }
        
        // 验证分布相对均匀（每张牌期望出现 iterations/52 次）
        double expectedCount = (double) iterations / 52;
        double tolerance = expectedCount * 0.5;  // 允许50%的偏差
        
        for (Map.Entry<Card, Integer> entry : firstCardCounts.entrySet()) {
            int actualCount = entry.getValue();
            assertTrue(Math.abs(actualCount - expectedCount) < tolerance,
                      String.format("牌 %s 出现次数 %d 偏离期望值 %.2f 过多", 
                                  entry.getKey().getDisplayName(), actualCount, expectedCount));
        }
    }
}
