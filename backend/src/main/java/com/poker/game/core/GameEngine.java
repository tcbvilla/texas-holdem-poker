package com.poker.game.core;

import com.poker.game.model.Card;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 德州扑克游戏引擎
 * 管理游戏状态、流程控制和规则执行
 */
@Component
@Scope("prototype")
@Slf4j
@Getter
public class GameEngine {

    private final PotManager potManager;
    private final HandEvaluator handEvaluator;

    // 结算相关
    private PotManager.PotStructure potStructure;
    private PotManager.SettlementResult settlementResult;

    public GameEngine(PotManager potManager, HandEvaluator handEvaluator) {
        this.potManager = potManager;
        this.handEvaluator = handEvaluator;
    }

    /**
     * 游戏状态枚举
     */
    public enum GameState {
        WAITING_FOR_PLAYERS,    // 等待玩家
        PRE_FLOP,              // 翻牌前
        FLOP,                  // 翻牌
        TURN,                  // 转牌  
        RIVER,                 // 河牌
        SHOWDOWN,              // 摊牌
        FINISHED               // 游戏结束
    }

    /**
     * 玩家行动枚举
     */
    public enum PlayerAction {
        FOLD,       // 弃牌
        CHECK,      // 过牌
        CALL,       // 跟注
        RAISE,      // 加注
        ALL_IN      // 全下
    }

    /**
     * 玩家信息
     */
    @Getter
    public static class Player {
        private final int id;
        private final String name;
        private BigDecimal chips;           // 当前筹码
        private BigDecimal betAmount;       // 本轮下注金额
        private BigDecimal totalBet;        // 总下注金额
        private List<Card> holeCards;       // 底牌
        private boolean isActive;           // 是否还在游戏中
        private boolean isAllIn;            // 是否全下
        private boolean hasFolded;          // 是否已弃牌
        private boolean hasActed;           // 本轮是否已行动

        public Player(int id, String name, BigDecimal initialChips) {
            this.id = id;
            this.name = name;
            this.chips = initialChips;
            this.betAmount = BigDecimal.ZERO;
            this.totalBet = BigDecimal.ZERO;
            this.holeCards = new ArrayList<>();
            this.isActive = true;
            this.isAllIn = false;
            this.hasFolded = false;
            this.hasActed = false;
        }

        public void resetForNewRound() {
            this.betAmount = BigDecimal.ZERO;
            
            // 全下玩家在新下注轮中不需要行动，保持hasActed=true
            if (!this.isAllIn) {
                this.hasActed = false;
            }
            
            // 重置全下状态 - 新的下注轮开始，如果玩家还有筹码就可以继续行动
            if (this.chips.compareTo(BigDecimal.ZERO) > 0) {
                this.isAllIn = false;
            }
        }

        public void resetForNewHand() {
            this.betAmount = BigDecimal.ZERO;
            this.totalBet = BigDecimal.ZERO;
            this.hasActed = false;
        }

        public void bet(BigDecimal amount) {
            if (amount.compareTo(chips) >= 0) {
                // 全下
                this.betAmount = this.betAmount.add(chips);
                this.totalBet = this.totalBet.add(chips);
                this.chips = BigDecimal.ZERO;
                this.isAllIn = true;
            } else {
                this.betAmount = this.betAmount.add(amount);
                this.totalBet = this.totalBet.add(amount);
                this.chips = this.chips.subtract(amount);
            }
            this.hasActed = true;
        }

        public void fold() {
            this.hasFolded = true;
            this.isActive = false;
            this.hasActed = true;
        }

        public void setHasActed(boolean hasActed) {
            this.hasActed = hasActed;
        }

        public void addChips(BigDecimal amount) {
            this.chips = this.chips.add(amount);
        }
    }

    /**
     * 游戏状态信息
     */
    @Getter
    public static class GameInfo {
        private final String gameId;
        private final GameState state;
        private final List<Player> players;
        private final List<Card> communityCards;
        private final BigDecimal currentBet;
        private final BigDecimal totalPot;
        private final int buttonPosition;
        private final int currentPlayerIndex;
        private final int smallBlind;
        private final int bigBlind;
        private final PotManager.PotStructure potStructure;
        private final PotManager.SettlementResult settlementResult;

        public GameInfo(String gameId, GameState state, List<Player> players, 
                       List<Card> communityCards, BigDecimal currentBet, BigDecimal totalPot,
                       int buttonPosition, int currentPlayerIndex, int smallBlind, int bigBlind,
                       PotManager.PotStructure potStructure, PotManager.SettlementResult settlementResult) {
            this.gameId = gameId;
            this.state = state;
            this.players = new ArrayList<>(players);
            this.communityCards = new ArrayList<>(communityCards);
            this.currentBet = currentBet;
            this.totalPot = totalPot;
            this.buttonPosition = buttonPosition;
            this.currentPlayerIndex = currentPlayerIndex;
            this.smallBlind = smallBlind;
            this.bigBlind = bigBlind;
            this.potStructure = potStructure;
            this.settlementResult = settlementResult;
        }
    }

    // 游戏状态
    private String gameId;
    private GameState currentState;
    private List<Player> players;
    private List<Card> communityCards;
    private Deck deck;
    private BigDecimal currentBet;
    private BigDecimal totalPot;
    private int buttonPosition;
    private int currentPlayerIndex;
    private int smallBlindAmount;
    private int bigBlindAmount;

    /**
     * 初始化新游戏
     */
    public void initializeGame(String gameId, List<Player> players, int smallBlind, int bigBlind) {
        this.gameId = gameId;
        this.players = new ArrayList<>(players);
        this.communityCards = new ArrayList<>();
        this.deck = new Deck();
        this.currentBet = BigDecimal.ZERO;
        this.totalPot = BigDecimal.ZERO;
        this.buttonPosition = 0;
        this.currentPlayerIndex = 0;
        this.smallBlindAmount = smallBlind;
        this.bigBlindAmount = bigBlind;
        this.currentState = GameState.WAITING_FOR_PLAYERS;

        log.info("游戏初始化完成，游戏ID：{}，玩家数：{}，盲注：{}/{}", 
                gameId, players.size(), smallBlind, bigBlind);
    }

    /**
     * 开始新一手牌
     */
    public void startNewHand() {
        // 过滤掉筹码为0的玩家
        List<Player> activePlayers = players.stream()
                .filter(p -> p.getChips().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        
        if (activePlayers.size() < 2) {
            throw new IllegalStateException("至少需要2个有筹码的玩家才能开始游戏，当前有筹码玩家数：" + activePlayers.size());
        }

        // 重置游戏状态
        currentState = GameState.PRE_FLOP;
        communityCards.clear();
        currentBet = BigDecimal.ZERO;
        totalPot = BigDecimal.ZERO;
        
        // 清理结算数据
        potStructure = null;
        settlementResult = null;

        // 重置玩家状态
        for (Player player : players) {
            if (player.getChips().compareTo(BigDecimal.ZERO) > 0) {
                // 只有有筹码的玩家才参与游戏
                player.resetForNewHand(); // 重置总下注和本轮下注
                player.holeCards.clear();
                player.hasFolded = false;
                player.isAllIn = false;
                player.isActive = true; // 有筹码的玩家重新激活
            } else {
                // 筹码为0的玩家不参与游戏
                player.isActive = false;
                player.hasFolded = true;
                player.holeCards.clear();
                log.info("玩家{}筹码为0，不参与本局游戏", player.getName());
            }
        }

        // 洗牌
        deck.shuffle(null);

        // 发底牌
        dealHoleCards();

        // 收取盲注
        collectBlinds();

        // 设置第一个行动玩家（大盲注后的第一个玩家）
        currentPlayerIndex = getNextActivePlayer((buttonPosition + 3) % players.size());

        log.info("新一手牌开始，按钮位置：{}，当前行动玩家：{}", buttonPosition, currentPlayerIndex);
    }

    /**
     * 发底牌
     */
    private void dealHoleCards() {
        // 每个玩家发2张底牌
        for (int round = 0; round < 2; round++) {
            for (Player player : players) {
                if (player.isActive) {
                    Card card = deck.dealCard();
                    player.holeCards.add(card);
                }
            }
        }
        log.info("底牌发放完成");
    }

    /**
     * 收取盲注
     */
    private void collectBlinds() {
        int smallBlindPos = (buttonPosition + 1) % players.size();
        int bigBlindPos = (buttonPosition + 2) % players.size();

        Player smallBlindPlayer = players.get(smallBlindPos);
        Player bigBlindPlayer = players.get(bigBlindPos);

        // 收取小盲注
        BigDecimal smallBlindBet = BigDecimal.valueOf(smallBlindAmount);
        smallBlindPlayer.bet(smallBlindBet);
        totalPot = totalPot.add(smallBlindBet);
        // 小盲注在翻牌前也有行动机会，不标记为已行动
        smallBlindPlayer.hasActed = false;

        // 收取大盲注
        BigDecimal bigBlindBet = BigDecimal.valueOf(bigBlindAmount);
        bigBlindPlayer.bet(bigBlindBet);
        totalPot = totalPot.add(bigBlindBet);
        currentBet = bigBlindBet;
        // 大盲注在翻牌前也有行动机会，不标记为已行动
        bigBlindPlayer.hasActed = false;

        log.info("盲注收取完成，小盲注：{}（玩家{}），大盲注：{}（玩家{}）", 
                smallBlindAmount, smallBlindPlayer.getName(), 
                bigBlindAmount, bigBlindPlayer.getName());
    }

    /**
     * 处理玩家行动
     */
    public boolean processPlayerAction(int playerId, PlayerAction action, BigDecimal amount) {
        Player currentPlayer = players.get(currentPlayerIndex);
        
        if (currentPlayer.getId() != playerId) {
            log.warn("不是当前玩家的回合，当前玩家：{}，尝试行动玩家：{}", 
                    currentPlayer.getId(), playerId);
            return false;
        }

        switch (action) {
            case FOLD:
                currentPlayer.fold();
                log.info("玩家{}弃牌", currentPlayer.getName());
                break;
                
            case CHECK:
                if (currentBet.compareTo(currentPlayer.betAmount) > 0) {
                    log.warn("玩家{}无法过牌，当前需要跟注{}", 
                            currentPlayer.getName(), currentBet.subtract(currentPlayer.betAmount));
                    return false;
                }
                currentPlayer.hasActed = true;
                log.info("玩家{}过牌", currentPlayer.getName());
                break;
                
            case CALL:
                BigDecimal callAmount = currentBet.subtract(currentPlayer.betAmount);
                if (callAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("玩家{}无需跟注", currentPlayer.getName());
                    return false;
                }
                currentPlayer.bet(callAmount);
                totalPot = totalPot.add(callAmount);
                log.info("玩家{}跟注{}", currentPlayer.getName(), callAmount);
                break;
                
            case RAISE:
                if (amount == null) {
                    log.warn("玩家{}加注金额为空", currentPlayer.getName());
                    return false;
                }
                
                // 计算最小加注额（当前下注额 + 大盲注）
                BigDecimal minRaise = currentBet.add(BigDecimal.valueOf(bigBlindAmount));
                if (amount.compareTo(minRaise) < 0) {
                    log.warn("玩家{}加注金额太小：{}，最小加注额为{}", 
                            currentPlayer.getName(), amount, minRaise);
                    return false;
                }
                
                // 检查玩家是否有足够筹码
                BigDecimal raiseAmount = amount.subtract(currentPlayer.betAmount);
                if (raiseAmount.compareTo(currentPlayer.chips) > 0) {
                    log.warn("玩家{}筹码不足：需要{}，拥有{}", 
                            currentPlayer.getName(), raiseAmount, currentPlayer.chips);
                    return false;
                }
                
                currentPlayer.bet(raiseAmount);
                totalPot = totalPot.add(raiseAmount);
                currentBet = amount;
                
                // 重置其他玩家的行动状态
                for (Player player : players) {
                    if (player != currentPlayer && player.isActive && !player.isAllIn) {
                        player.hasActed = false;
                    }
                }
                
                log.info("玩家{}加注到{}", currentPlayer.getName(), amount);
                break;
                
            case ALL_IN:
                if (currentPlayer.chips.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("玩家{}筹码为0，无法全下", currentPlayer.getName());
                    return false;
                }
                
                BigDecimal allInAmount = currentPlayer.chips.add(currentPlayer.betAmount);
                currentPlayer.bet(currentPlayer.chips);
                totalPot = totalPot.add(currentPlayer.chips);
                
                // 全下玩家标记为已行动（相当于执行了一次过牌）
                currentPlayer.hasActed = true;
                
                if (allInAmount.compareTo(currentBet) > 0) {
                    currentBet = allInAmount;
                    // 重置其他玩家的行动状态
                    for (Player player : players) {
                        if (player != currentPlayer && player.isActive && !player.isAllIn) {
                            player.hasActed = false;
                        }
                    }
                }
                
                log.info("玩家{}全下{}", currentPlayer.getName(), allInAmount);
                break;
        }

        // 检查下注轮是否结束
        if (isBettingRoundComplete()) {
            // 检查是否只剩一个玩家，如果是则直接结束游戏
            List<Player> activePlayers = getActivePlayers();
            if (activePlayers.size() == 1) {
                // 只剩一个玩家，游戏结束，该玩家获胜
                Player winner = activePlayers.get(0);
                winner.addChips(totalPot);
                currentState = GameState.FINISHED;
                currentPlayerIndex = -1;
                
                // 创建结算信息，让前端能显示获胜结果
                createSingleWinnerSettlement(winner, totalPot);
                
                log.info("游戏结束：玩家{}获胜，获得底池{}", winner.getName(), totalPot);
            } else {
                // 检查是否只剩全下玩家，如果是则自动执行到摊牌
                List<Player> nonAllInPlayers = activePlayers.stream()
                        .filter(p -> !p.isAllIn)
                        .collect(Collectors.toList());
                
                if (nonAllInPlayers.isEmpty()) {
                    // 所有活跃玩家都全下了，自动执行到摊牌
                    log.info("所有玩家都全下，自动执行到摊牌");
                    advanceToShowdown();
                } else {
                    advanceToNextState();
                }
            }
        } else {
            // 移动到下一个玩家
            currentPlayerIndex = getNextActivePlayer(currentPlayerIndex + 1);
            
            // 翻牌前特殊处理：如果所有人都行动完了，但大盲注还没有额外行动，轮到大盲注
            if (currentState == GameState.PRE_FLOP && currentPlayerIndex == -1) {
                int bigBlindPos = (buttonPosition + 2) % players.size();
                Player bigBlindPlayer = players.get(bigBlindPos);
                
                if (bigBlindPlayer.isActive && !bigBlindPlayer.isAllIn && 
                    bigBlindPlayer.betAmount.compareTo(BigDecimal.valueOf(bigBlindAmount)) == 0 &&
                    !bigBlindPlayer.hasActed) {
                    currentPlayerIndex = bigBlindPos;
                    log.debug("翻牌前：轮到大盲注玩家{}行动", bigBlindPlayer.getName());
                }
            }
        }

        return true;
    }

    /**
     * 检查下注轮是否完成
     */
    private boolean isBettingRoundComplete() {
        List<Player> activePlayers = getActivePlayers();
        
        if (activePlayers.size() <= 1) {
            return true; // 只剩一个玩家
        }

        // 检查所有活跃玩家是否都已行动
        for (Player player : activePlayers) {
            if (!player.hasActed) {
                return false;
            }
        }

        // 检查所有非全下玩家的下注金额是否相等
        List<Player> nonAllInPlayers = activePlayers.stream()
                .filter(p -> !p.isAllIn)
                .collect(Collectors.toList());

        if (nonAllInPlayers.isEmpty()) {
            return true; // 所有玩家都全下了
        }

        // 检查所有非全下玩家的下注金额是否相等
        BigDecimal expectedBet = currentBet;
        for (Player player : nonAllInPlayers) {
            if (player.betAmount.compareTo(expectedBet) != 0) {
                return false;
            }
        }

        // 翻牌前特殊处理：如果大盲注玩家还没有在当前下注轮中行动过（除了强制盲注），
        // 且所有人都跟注到大盲注，大盲注玩家应该有行动机会
        if (currentState == GameState.PRE_FLOP) {
            int bigBlindPos = (buttonPosition + 2) % players.size();
            Player bigBlindPlayer = players.get(bigBlindPos);
            
            // 如果大盲注玩家还活跃且没有在翻牌前额外行动过
            if (bigBlindPlayer.isActive && !bigBlindPlayer.isAllIn && 
                bigBlindPlayer.betAmount.compareTo(BigDecimal.valueOf(bigBlindAmount)) == 0 &&
                !bigBlindPlayer.hasActed) {
                
                // 检查是否所有其他玩家都跟注到大盲注
                boolean allCallToBigBlind = activePlayers.stream()
                        .filter(p -> p != bigBlindPlayer)
                        .allMatch(p -> p.isAllIn || p.betAmount.compareTo(BigDecimal.valueOf(bigBlindAmount)) >= 0);
                
                if (allCallToBigBlind) {
                    log.debug("翻牌前：大盲注玩家{}还有行动机会", bigBlindPlayer.getName());
                    return false; // 大盲注还需要行动
                }
            }
        }

        return true;
    }

    /**
     * 推进到下一个游戏状态
     */
    private void advanceToNextState() {
        // 重置玩家下注轮状态
        for (Player player : players) {
            player.resetForNewRound();
        }
        
        // 重置当前下注额（新的下注轮从0开始）
        currentBet = BigDecimal.ZERO;

        switch (currentState) {
            case PRE_FLOP:
                dealFlop();
                currentState = GameState.FLOP;
                break;
            case FLOP:
                dealTurn();
                currentState = GameState.TURN;
                break;
            case TURN:
                dealRiver();
                currentState = GameState.RIVER;
                break;
            case RIVER:
                currentState = GameState.SHOWDOWN;
                performShowdown();
                break;
            case SHOWDOWN:
                currentState = GameState.FINISHED;
                break;
            case WAITING_FOR_PLAYERS:
            case FINISHED:
                // 这些状态不需要推进
                break;
        }

        if (currentState != GameState.SHOWDOWN && currentState != GameState.FINISHED) {
            // 设置下一轮的第一个行动玩家（小盲注位置开始）
            currentPlayerIndex = getNextActivePlayer((buttonPosition + 1) % players.size());
        }

        log.info("游戏状态推进到：{}", currentState);
    }

    /**
     * 自动执行到摊牌（当所有玩家都全下时）
     */
    private void advanceToShowdown() {
        log.info("开始自动执行到摊牌，当前状态：{}", currentState);
        
        // 根据当前状态，自动执行后续的发牌
        while (currentState != GameState.SHOWDOWN && currentState != GameState.FINISHED) {
            switch (currentState) {
                case PRE_FLOP:
                    dealFlop();
                    currentState = GameState.FLOP;
                    log.info("自动发翻牌：{}", communityCards.subList(0, 3));
                    break;
                case FLOP:
                    dealTurn();
                    currentState = GameState.TURN;
                    log.info("自动发转牌：{}", communityCards.get(3));
                    break;
                case TURN:
                    dealRiver();
                    currentState = GameState.RIVER;
                    log.info("自动发河牌：{}", communityCards.get(4));
                    break;
                case RIVER:
                    currentState = GameState.SHOWDOWN;
                    performShowdown();
                    break;
                default:
                    log.warn("未知的游戏状态：{}", currentState);
                    break;
            }
        }
        
        log.info("自动执行到摊牌完成，最终状态：{}", currentState);
    }

    /**
     * 执行摊牌和结算
     */
    private void performShowdown() {
        log.info("开始执行摊牌和结算");

        // 创建底池结构
        List<PotManager.PlayerBetInfo> playerBets = players.stream()
                .map(player -> new PotManager.PlayerBetInfo(
                        player.getId(),
                        player.getName(),
                        player.getTotalBet(),
                        player.isHasFolded(),
                        players.indexOf(player) // 使用索引作为座位位置
                ))
                .collect(Collectors.toList());

        potStructure = potManager.createPots(playerBets);
        log.info("底池结构创建完成：{}个底池，总金额={}", 
                potStructure.getPots().size(), potStructure.getTotalAmount());

        // 评估所有活跃玩家的牌型
        Map<Integer, HandRank> playerHands = new HashMap<>();
        for (Player player : players) {
            if (!player.isHasFolded()) {
                List<Card> allCards = new ArrayList<>(player.getHoleCards());
                allCards.addAll(communityCards);
                HandRank handRank = handEvaluator.evaluateHand(allCards);
                playerHands.put(player.getId(), handRank);
                
                log.debug("玩家{}牌型：{}", player.getName(), handRank.getHandType().getChineseName());
            }
        }

        // 结算底池
        settlementResult = potManager.settlePots(potStructure, playerHands);
        
        // 更新总底池金额
        totalPot = potStructure.getTotalAmount();
        
        // 分配筹码给获胜者
        for (Map.Entry<Integer, BigDecimal> entry : settlementResult.getPlayerWinnings().entrySet()) {
            int playerId = entry.getKey();
            BigDecimal winnings = entry.getValue();
            
            Player winner = players.stream()
                    .filter(p -> p.getId() == playerId)
                    .findFirst()
                    .orElse(null);
                    
            if (winner != null) {
                // 在德州扑克中，获胜者获得底池，筹码直接累加
                winner.addChips(winnings);
                log.info("玩家{}获得筹码：{}，最终筹码：{}", winner.getName(), winnings, winner.getChips());
            }
        }

        log.info("摊牌结算完成：{}", settlementResult.getSummary());
        
        // 设置游戏状态为结束
        currentState = GameState.FINISHED;
        currentPlayerIndex = -1;
        
        log.info("游戏结束，状态设置为：{}", currentState);
    }

    /**
     * 创建单玩家获胜的结算信息
     */
    private void createSingleWinnerSettlement(Player winner, BigDecimal potAmount) {
        // 创建简单的底池结构
        List<PotManager.Pot> pots = new ArrayList<>();
        List<Integer> allPlayerIds = players.stream().map(Player::getId).collect(Collectors.toList());
        PotManager.Pot mainPot = new PotManager.Pot(potAmount, allPlayerIds, 0, "主池");
        mainPot.setWinners(List.of(winner.getId()));
        mainPot.setWinnings(Map.of(winner.getId(), potAmount));
        pots.add(mainPot);

        Map<Integer, BigDecimal> playerContributions = new HashMap<>();
        for (Player player : players) {
            playerContributions.put(player.getId(), player.getTotalBet());
        }

        potStructure = new PotManager.PotStructure(pots, potAmount, playerContributions);

        // 创建结算结果
        Map<Integer, BigDecimal> playerWinnings = new HashMap<>();
        playerWinnings.put(winner.getId(), potAmount);

        List<String> summaryParts = new ArrayList<>();
        summaryParts.add(String.format("玩家%s获胜，获得底池%s", winner.getName(), potAmount));

        settlementResult = new PotManager.SettlementResult(
                pots,
                playerWinnings,
                potAmount,
                String.join("；", summaryParts)
        );

        log.info("单玩家获胜结算信息创建完成：{}", settlementResult.getSummary());
    }

    /**
     * 开始下一局游戏
     */
    public void startNextHand() {
        if (currentState != GameState.FINISHED) {
            throw new IllegalStateException("当前游戏尚未结束，无法开始下一局");
        }

        // 移动按钮位置
        buttonPosition = (buttonPosition + 1) % players.size();

        // 清理上一局的结算信息
        potStructure = null;
        settlementResult = null;

        // 重置游戏状态并开始新一局
        startNewHand();
        
        log.info("开始下一局游戏，新的按钮位置：{}", buttonPosition);
    }

    /**
     * 检查是否可以开始下一局
     */
    public boolean canStartNextHand() {
        // 只要游戏状态是FINISHED就可以开始下一局
        // 即使所有玩家筹码为0，也可以开始新局（玩家可以重新买入）
        return currentState == GameState.FINISHED;
    }

    /**
     * 发翻牌
     */
    private void dealFlop() {
        deck.burnCard(); // 烧牌
        for (int i = 0; i < 3; i++) {
            communityCards.add(deck.dealCard());
        }
        log.info("翻牌发放完成：{}", communityCards);
    }

    /**
     * 发转牌
     */
    private void dealTurn() {
        deck.burnCard(); // 烧牌
        communityCards.add(deck.dealCard());
        log.info("转牌发放完成：{}", communityCards.get(3));
    }

    /**
     * 发河牌
     */
    private void dealRiver() {
        deck.burnCard(); // 烧牌
        communityCards.add(deck.dealCard());
        log.info("河牌发放完成：{}", communityCards.get(4));
    }

    /**
     * 获取下一个需要行动的玩家索引
     */
    private int getNextActivePlayer(int startIndex) {
        for (int i = 0; i < players.size(); i++) {
            int index = (startIndex + i) % players.size();
            Player player = players.get(index);
            if (player.isActive && !player.isAllIn && !player.hasActed) {
                return index;
            }
        }
        return -1; // 没有找到需要行动的玩家
    }

    /**
     * 获取所有活跃玩家
     */
    private List<Player> getActivePlayers() {
        return players.stream()
                .filter(p -> p.isActive)
                .toList();
    }

    /**
     * 获取当前游戏信息
     */
    public GameInfo getGameInfo() {
        // 动态计算总底池，确保准确性
        BigDecimal calculatedTotalPot = players.stream()
                .map(Player::getTotalBet)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new GameInfo(
                gameId, currentState, players, communityCards,
                currentBet, calculatedTotalPot, buttonPosition, currentPlayerIndex,
                smallBlindAmount, bigBlindAmount, potStructure, settlementResult
        );
    }

    /**
     * 移动按钮位置到下一个玩家
     */
    public void moveButton() {
        buttonPosition = (buttonPosition + 1) % players.size();
        log.info("按钮位置移动到玩家：{}", players.get(buttonPosition).getName());
    }
}
