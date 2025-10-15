package com.poker.game.core;

import com.poker.game.model.Card;
import com.poker.game.model.Rank;
import com.poker.game.model.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PotManager 测试类
 */
class PotManagerTest {

    private PotManager potManager;

    @BeforeEach
    void setUp() {
        potManager = new PotManager();
    }

    @Test
    @DisplayName("测试无全下情况 - 所有玩家下注相同")
    void testNormalBetting() {
        // 准备测试数据：3个玩家都下注100
        List<PotManager.PlayerBetInfo> playerBets = Arrays.asList(
                new PotManager.PlayerBetInfo(1, "玩家1", new BigDecimal("100"), false, 1),
                new PotManager.PlayerBetInfo(2, "玩家2", new BigDecimal("100"), false, 2),
                new PotManager.PlayerBetInfo(3, "玩家3", new BigDecimal("100"), false, 3)
        );

        // 创建底池
        PotManager.PotStructure potStructure = potManager.createPots(playerBets);

        // 验证结果
        assertEquals(1, potStructure.getPots().size(), "应该只有一个主池");
        assertEquals(new BigDecimal("300"), potStructure.getTotalAmount(), "总金额应该是300");
        
        PotManager.Pot mainPot = potStructure.getMainPot();
        assertNotNull(mainPot);
        assertEquals(new BigDecimal("300"), mainPot.getAmount(), "主池金额应该是300");
        assertEquals(3, mainPot.getEligiblePlayerIds().size(), "3个玩家都有资格");
        assertTrue(mainPot.getEligiblePlayerIds().containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    @DisplayName("测试单人全下情况")
    void testSingleAllIn() {
        // 准备测试数据：玩家1全下50，玩家2和3各下注200
        List<PotManager.PlayerBetInfo> playerBets = Arrays.asList(
                new PotManager.PlayerBetInfo(1, "玩家1", new BigDecimal("50"), false, 1),
                new PotManager.PlayerBetInfo(2, "玩家2", new BigDecimal("200"), false, 2),
                new PotManager.PlayerBetInfo(3, "玩家3", new BigDecimal("200"), false, 3)
        );

        // 创建底池
        PotManager.PotStructure potStructure = potManager.createPots(playerBets);

        // 验证结果
        assertEquals(2, potStructure.getPots().size(), "应该有主池和边池");
        assertEquals(new BigDecimal("450"), potStructure.getTotalAmount(), "总金额应该是450");

        // 验证主池
        PotManager.Pot mainPot = potStructure.getMainPot();
        assertEquals(new BigDecimal("150"), mainPot.getAmount(), "主池应该是150 (50×3)");
        assertEquals(3, mainPot.getEligiblePlayerIds().size(), "3个玩家都有资格争夺主池");

        // 验证边池
        List<PotManager.Pot> sidePots = potStructure.getSidePots();
        assertEquals(1, sidePots.size(), "应该有1个边池");
        PotManager.Pot sidePot = sidePots.get(0);
        assertEquals(new BigDecimal("300"), sidePot.getAmount(), "边池应该是300 ((200-50)×2)");
        assertEquals(2, sidePot.getEligiblePlayerIds().size(), "只有玩家2和3有资格争夺边池");
        assertTrue(sidePot.getEligiblePlayerIds().containsAll(Arrays.asList(2, 3)));
    }

    @Test
    @DisplayName("测试多人全下情况")
    void testMultipleAllIn() {
        // 准备测试数据：复杂的多层全下
        List<PotManager.PlayerBetInfo> playerBets = Arrays.asList(
                new PotManager.PlayerBetInfo(1, "玩家1", new BigDecimal("50"), false, 1),   // 全下50
                new PotManager.PlayerBetInfo(2, "玩家2", new BigDecimal("150"), false, 2),  // 全下150
                new PotManager.PlayerBetInfo(3, "玩家3", new BigDecimal("300"), false, 3),  // 下注300
                new PotManager.PlayerBetInfo(4, "玩家4", new BigDecimal("300"), false, 4)   // 下注300
        );

        // 创建底池
        PotManager.PotStructure potStructure = potManager.createPots(playerBets);

        // 验证结果
        assertEquals(3, potStructure.getPots().size(), "应该有3个底池");
        assertEquals(new BigDecimal("800"), potStructure.getTotalAmount(), "总金额应该是800");

        List<PotManager.Pot> pots = potStructure.getPots();

        // 验证主池 (50×4 = 200)
        PotManager.Pot mainPot = pots.get(0);
        assertEquals(new BigDecimal("200"), mainPot.getAmount(), "主池应该是200");
        assertEquals(4, mainPot.getEligiblePlayerIds().size(), "4个玩家都有资格");

        // 验证边池1 ((150-50)×3 = 300)
        PotManager.Pot sidePot1 = pots.get(1);
        assertEquals(new BigDecimal("300"), sidePot1.getAmount(), "边池1应该是300");
        assertEquals(3, sidePot1.getEligiblePlayerIds().size(), "玩家2,3,4有资格");
        assertTrue(sidePot1.getEligiblePlayerIds().containsAll(Arrays.asList(2, 3, 4)));

        // 验证边池2 ((300-150)×2 = 300)
        PotManager.Pot sidePot2 = pots.get(2);
        assertEquals(new BigDecimal("300"), sidePot2.getAmount(), "边池2应该是300");
        assertEquals(2, sidePot2.getEligiblePlayerIds().size(), "只有玩家3,4有资格");
        assertTrue(sidePot2.getEligiblePlayerIds().containsAll(Arrays.asList(3, 4)));
    }

    @Test
    @DisplayName("测试弃牌玩家情况")
    void testFoldedPlayers() {
        // 准备测试数据：玩家2弃牌但已投入筹码
        List<PotManager.PlayerBetInfo> playerBets = Arrays.asList(
                new PotManager.PlayerBetInfo(1, "玩家1", new BigDecimal("100"), false, 1),
                new PotManager.PlayerBetInfo(2, "玩家2", new BigDecimal("50"), true, 2),   // 弃牌
                new PotManager.PlayerBetInfo(3, "玩家3", new BigDecimal("100"), false, 3)
        );

        // 创建底池
        PotManager.PotStructure potStructure = potManager.createPots(playerBets);

        // 验证结果
        assertEquals(2, potStructure.getPots().size(), "应该有2个底池");
        assertEquals(new BigDecimal("250"), potStructure.getTotalAmount(), "总金额应该是250");

        // 验证主池 (50×3 = 150)
        PotManager.Pot mainPot = potStructure.getMainPot();
        assertEquals(new BigDecimal("150"), mainPot.getAmount(), "主池应该是150");
        assertEquals(2, mainPot.getEligiblePlayerIds().size(), "只有玩家1,3有资格（玩家2弃牌）");
        assertTrue(mainPot.getEligiblePlayerIds().containsAll(Arrays.asList(1, 3)));

        // 验证边池 ((100-50)×2 = 100)
        List<PotManager.Pot> sidePots = potStructure.getSidePots();
        assertEquals(1, sidePots.size(), "应该有1个边池");
        PotManager.Pot sidePot = sidePots.get(0);
        assertEquals(new BigDecimal("100"), sidePot.getAmount(), "边池应该是100");
        assertEquals(2, sidePot.getEligiblePlayerIds().size(), "只有玩家1,3有资格");
    }

    @Test
    @DisplayName("测试底池结算 - 单一获胜者")
    void testSettlementSingleWinner() {
        // 创建底池结构
        List<PotManager.PlayerBetInfo> playerBets = Arrays.asList(
                new PotManager.PlayerBetInfo(1, "玩家1", new BigDecimal("100"), false, 1),
                new PotManager.PlayerBetInfo(2, "玩家2", new BigDecimal("100"), false, 2),
                new PotManager.PlayerBetInfo(3, "玩家3", new BigDecimal("100"), false, 3)
        );
        PotManager.PotStructure potStructure = potManager.createPots(playerBets);

        // 创建牌型信息（玩家1最强）
        Map<Integer, HandRank> playerHands = new HashMap<>();
        playerHands.put(1, createHandRank(HandRank.HandType.FLUSH));      // 同花
        playerHands.put(2, createHandRank(HandRank.HandType.ONE_PAIR));   // 一对
        playerHands.put(3, createHandRank(HandRank.HandType.HIGH_CARD));  // 高牌

        // 结算底池
        PotManager.SettlementResult result = potManager.settlePots(potStructure, playerHands);

        // 验证结果
        assertEquals(new BigDecimal("300"), result.getTotalDistributed(), "总分配金额应该是300");
        assertEquals(new BigDecimal("300"), result.getPlayerWinning(1), "玩家1应该获得300");
        assertEquals(BigDecimal.ZERO, result.getPlayerWinning(2), "玩家2应该获得0");
        assertEquals(BigDecimal.ZERO, result.getPlayerWinning(3), "玩家3应该获得0");
    }

    @Test
    @DisplayName("测试底池结算 - 平分情况")
    void testSettlementTie() {
        // 创建底池结构
        List<PotManager.PlayerBetInfo> playerBets = Arrays.asList(
                new PotManager.PlayerBetInfo(1, "玩家1", new BigDecimal("100"), false, 1),
                new PotManager.PlayerBetInfo(2, "玩家2", new BigDecimal("100"), false, 2),
                new PotManager.PlayerBetInfo(3, "玩家3", new BigDecimal("100"), false, 3)
        );
        PotManager.PotStructure potStructure = potManager.createPots(playerBets);

        // 创建牌型信息（玩家1和2平分）
        Map<Integer, HandRank> playerHands = new HashMap<>();
        playerHands.put(1, createHandRank(HandRank.HandType.ONE_PAIR));   // 一对
        playerHands.put(2, createHandRank(HandRank.HandType.ONE_PAIR));   // 一对（相同）
        playerHands.put(3, createHandRank(HandRank.HandType.HIGH_CARD));  // 高牌

        // 结算底池
        PotManager.SettlementResult result = potManager.settlePots(potStructure, playerHands);

        // 验证结果
        assertEquals(new BigDecimal("300"), result.getTotalDistributed(), "总分配金额应该是300");
        assertEquals(new BigDecimal("150"), result.getPlayerWinning(1), "玩家1应该获得150");
        assertEquals(new BigDecimal("150"), result.getPlayerWinning(2), "玩家2应该获得150");
        assertEquals(BigDecimal.ZERO, result.getPlayerWinning(3), "玩家3应该获得0");
    }

    @Test
    @DisplayName("测试底池结算 - 奇数筹码分配")
    void testSettlementOddChips() {
        // 创建底池结构（总金额101，无法被2整除）
        List<PotManager.PlayerBetInfo> playerBets = Arrays.asList(
                new PotManager.PlayerBetInfo(1, "玩家1", new BigDecimal("51"), false, 1),
                new PotManager.PlayerBetInfo(2, "玩家2", new BigDecimal("50"), false, 2)
        );
        PotManager.PotStructure potStructure = potManager.createPots(playerBets);

        // 创建牌型信息（两人平分）
        Map<Integer, HandRank> playerHands = new HashMap<>();
        playerHands.put(1, createHandRank(HandRank.HandType.ONE_PAIR));
        playerHands.put(2, createHandRank(HandRank.HandType.ONE_PAIR));

        // 结算底池
        PotManager.SettlementResult result = potManager.settlePots(potStructure, playerHands);

        // 验证结果（位置靠前的玩家多得1筹码）
        assertEquals(new BigDecimal("101"), result.getTotalDistributed(), "总分配金额应该是101");
        assertEquals(new BigDecimal("51"), result.getPlayerWinning(1), "玩家1应该获得51（多1筹码）");
        assertEquals(new BigDecimal("50"), result.getPlayerWinning(2), "玩家2应该获得50");
    }

    @Test
    @DisplayName("测试底池验证")
    void testPotValidation() {
        List<PotManager.PlayerBetInfo> playerBets = Arrays.asList(
                new PotManager.PlayerBetInfo(1, "玩家1", new BigDecimal("100"), false, 1),
                new PotManager.PlayerBetInfo(2, "玩家2", new BigDecimal("200"), false, 2)
        );

        PotManager.PotStructure potStructure = potManager.createPots(playerBets);
        boolean isValid = potManager.validatePotStructure(potStructure, playerBets);

        assertTrue(isValid, "底池结构应该通过验证");
    }

    // 辅助方法：创建HandRank对象
    private HandRank createHandRank(HandRank.HandType handType) {
        // 创建一些示例卡牌
        List<Card> cards = Arrays.asList(
                new Card(Suit.HEART, Rank.ACE),
                new Card(Suit.HEART, Rank.KING),
                new Card(Suit.HEART, Rank.QUEEN),
                new Card(Suit.HEART, Rank.JACK),
                new Card(Suit.HEART, Rank.TEN)
        );
        
        List<Rank> kickers = Arrays.asList(Rank.ACE, Rank.KING);
        
        return new HandRank(handType, cards, kickers);
    }
}
