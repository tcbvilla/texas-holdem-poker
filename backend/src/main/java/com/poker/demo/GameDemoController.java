package com.poker.demo;

import com.poker.game.core.BettingEngine;
import com.poker.game.core.GameEngine;
import com.poker.game.core.PotManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 游戏演示控制器
 * 展示完整的德州扑克游戏流程
 */
@Slf4j
@RestController
@RequestMapping("/api/demo/game")
@RequiredArgsConstructor
public class GameDemoController {

    private final ObjectProvider<GameEngine> gameEngineProvider;
    private final BettingEngine bettingEngine;
    
    // 当前演示游戏的实例
    private GameEngine currentGameEngine;

    /**
     * 创建新游戏
     */
    @PostMapping("/create")
    public Map<String, Object> createGame(
            @RequestParam(defaultValue = "6") int playerCount,
            @RequestParam(defaultValue = "10") int smallBlind,
            @RequestParam(defaultValue = "20") int bigBlind,
            @RequestParam(defaultValue = "1000") int initialChips) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 创建玩家
            List<GameEngine.Player> players = new ArrayList<>();
            for (int i = 1; i <= playerCount; i++) {
                players.add(new GameEngine.Player(i, "玩家" + i, BigDecimal.valueOf(initialChips)));
            }

            // 初始化游戏
            String gameId = "demo-" + System.currentTimeMillis();
            currentGameEngine = gameEngineProvider.getObject();
            currentGameEngine.initializeGame(gameId, players, smallBlind, bigBlind);

            // 开始新一手牌
            currentGameEngine.startNewHand();

            // 获取游戏信息
            GameEngine.GameInfo gameInfo = currentGameEngine.getGameInfo();

            result.put("success", true);
            result.put("message", "游戏创建成功");
            result.put("gameId", gameId);
            result.put("gameState", gameInfo.getState().name());
            result.put("players", convertPlayersToDisplay(gameInfo.getPlayers()));
            result.put("currentBet", gameInfo.getCurrentBet());
            result.put("totalPot", gameInfo.getTotalPot());
            result.put("buttonPosition", gameInfo.getButtonPosition());
            result.put("currentPlayerIndex", gameInfo.getCurrentPlayerIndex());
            result.put("smallBlind", gameInfo.getSmallBlind());
            result.put("bigBlind", gameInfo.getBigBlind());

            log.info("游戏创建成功，游戏ID：{}，玩家数：{}", gameId, playerCount);

        } catch (Exception e) {
            log.error("创建游戏失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 创建自定义筹码的游戏（用于测试）
     */
    @PostMapping("/create-custom")
    public Map<String, Object> createCustomGame(
            @RequestParam(defaultValue = "10") int smallBlind,
            @RequestParam(defaultValue = "20") int bigBlind) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 创建玩家 - 玩家4筹码少一些
            List<GameEngine.Player> players = new ArrayList<>();
            players.add(new GameEngine.Player(1, "玩家1", BigDecimal.valueOf(500)));
            players.add(new GameEngine.Player(2, "玩家2", BigDecimal.valueOf(500)));
            players.add(new GameEngine.Player(3, "玩家3", BigDecimal.valueOf(500)));
            players.add(new GameEngine.Player(4, "玩家4", BigDecimal.valueOf(200))); // 筹码少

            // 初始化游戏
            String gameId = "demo-custom-" + System.currentTimeMillis();
            currentGameEngine = gameEngineProvider.getObject();
            currentGameEngine.initializeGame(gameId, players, smallBlind, bigBlind);

            // 开始新一手牌
            currentGameEngine.startNewHand();

            // 获取游戏信息
            GameEngine.GameInfo gameInfo = currentGameEngine.getGameInfo();

            result.put("success", true);
            result.put("message", "自定义游戏创建成功");
            result.put("gameId", gameId);
            result.put("gameState", gameInfo.getState().name());
            result.put("players", convertPlayersToDisplay(gameInfo.getPlayers()));
            result.put("currentBet", gameInfo.getCurrentBet());
            result.put("totalPot", gameInfo.getTotalPot());
            result.put("buttonPosition", gameInfo.getButtonPosition());
            result.put("currentPlayerIndex", gameInfo.getCurrentPlayerIndex());
            result.put("smallBlind", gameInfo.getSmallBlind());
            result.put("bigBlind", gameInfo.getBigBlind());

            log.info("自定义游戏创建成功，游戏ID：{}", gameId);

        } catch (Exception e) {
            log.error("创建自定义游戏失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 玩家行动
     */
    @PostMapping("/action")
    public Map<String, Object> playerAction(
            @RequestParam int playerId,
            @RequestParam String action,
            @RequestParam(required = false) BigDecimal amount) {

        Map<String, Object> result = new HashMap<>();

        try {
            if (currentGameEngine == null) {
                result.put("success", false);
                result.put("error", "没有进行中的游戏");
                return result;
            }
            GameEngine.PlayerAction playerAction = GameEngine.PlayerAction.valueOf(action.toUpperCase());
            boolean success = currentGameEngine.processPlayerAction(playerId, playerAction, amount);

            if (success) {
                // 获取更新后的游戏信息
                GameEngine.GameInfo gameInfo = currentGameEngine.getGameInfo();

                result.put("success", true);
                result.put("message", "行动执行成功");
                result.put("gameState", gameInfo.getState().name());
                result.put("players", convertPlayersToDisplay(gameInfo.getPlayers()));
                result.put("currentBet", gameInfo.getCurrentBet());
                result.put("totalPot", gameInfo.getTotalPot());
                result.put("currentPlayerIndex", gameInfo.getCurrentPlayerIndex());
                result.put("communityCards", convertCardsToDisplay(gameInfo.getCommunityCards()));

                // 如果游戏结束，计算结果
                if (gameInfo.getState() == GameEngine.GameState.FINISHED) {
                    result.put("gameResult", "游戏结束");
                }

            } else {
                result.put("success", false);
                result.put("error", "行动执行失败");
            }

        } catch (Exception e) {
            log.error("玩家行动失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 获取玩家可用行动
     */
    @GetMapping("/actions/{playerId}")
    public Map<String, Object> getAvailableActions(@PathVariable int playerId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (currentGameEngine == null) {
                result.put("success", false);
                result.put("error", "没有进行中的游戏");
                return result;
            }
            GameEngine.GameInfo gameInfo = currentGameEngine.getGameInfo();
            GameEngine.Player player = gameInfo.getPlayers().stream()
                    .filter(p -> p.getId() == playerId)
                    .findFirst()
                    .orElse(null);

            if (player == null) {
                result.put("success", false);
                result.put("error", "玩家不存在");
                return result;
            }

            List<GameEngine.PlayerAction> actions = bettingEngine.getAvailableActions(player, gameInfo.getCurrentBet());
            List<Map<String, Object>> actionDetails = new ArrayList<>();

            for (GameEngine.PlayerAction action : actions) {
                Map<String, Object> actionInfo = new HashMap<>();
                actionInfo.put("action", action.name());
                actionInfo.put("name", getActionName(action));

                switch (action) {
                    case CALL:
                        BigDecimal callAmount = gameInfo.getCurrentBet().subtract(player.getBetAmount());
                        actionInfo.put("amount", callAmount);
                        actionInfo.put("description", "跟注 " + callAmount);
                        break;
                    case RAISE:
                        BigDecimal minRaise = bettingEngine.calculateMinRaise(gameInfo.getCurrentBet(), 
                                BigDecimal.valueOf(gameInfo.getBigBlind()));
                        actionInfo.put("minAmount", minRaise);
                        actionInfo.put("maxAmount", player.getChips().add(player.getBetAmount()));
                        actionInfo.put("description", "加注 (最小: " + minRaise + ")");
                        break;
                    case ALL_IN:
                        actionInfo.put("amount", player.getChips());
                        actionInfo.put("description", "全下 " + player.getChips());
                        break;
                    case CHECK:
                        actionInfo.put("description", "过牌");
                        break;
                    case FOLD:
                        actionInfo.put("description", "弃牌");
                        break;
                }

                actionDetails.add(actionInfo);
            }

            result.put("success", true);
            result.put("playerId", playerId);
            result.put("playerName", player.getName());
            result.put("actions", actionDetails);
            result.put("currentBet", gameInfo.getCurrentBet());
            result.put("playerBet", player.getBetAmount());
            result.put("playerChips", player.getChips());

        } catch (Exception e) {
            log.error("获取玩家行动失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 获取游戏状态
     */
    @GetMapping("/status")
    public Map<String, Object> getGameStatus() {
        Map<String, Object> result = new HashMap<>();

        try {
            if (currentGameEngine == null) {
                result.put("success", false);
                result.put("error", "没有进行中的游戏");
                return result;
            }
            GameEngine.GameInfo gameInfo = currentGameEngine.getGameInfo();

            result.put("success", true);
            result.put("gameId", gameInfo.getGameId());
            result.put("gameState", gameInfo.getState().name());
            result.put("gameStateName", getStateName(gameInfo.getState()));
            result.put("players", convertPlayersToDisplay(gameInfo.getPlayers()));
            result.put("communityCards", convertCardsToDisplay(gameInfo.getCommunityCards()));
            result.put("currentBet", gameInfo.getCurrentBet());
            result.put("totalPot", gameInfo.getTotalPot());
            result.put("buttonPosition", gameInfo.getButtonPosition());
            result.put("currentPlayerIndex", gameInfo.getCurrentPlayerIndex());
            result.put("smallBlind", gameInfo.getSmallBlind());
            result.put("bigBlind", gameInfo.getBigBlind());

            // 添加底池信息
            BettingEngine.PotInfo potInfo = bettingEngine.calculatePotInfo(gameInfo.getPlayers());
            result.put("potInfo", convertPotInfoToDisplay(potInfo));

            // 添加结算信息（如果游戏已结算）
            if (currentGameEngine.getPotStructure() != null) {
                result.put("potStructure", convertPotStructureToDisplay(currentGameEngine.getPotStructure()));
            }
            if (currentGameEngine.getSettlementResult() != null) {
                result.put("settlementResult", convertSettlementResultToDisplay(currentGameEngine.getSettlementResult()));
            }

        } catch (Exception e) {
            log.error("获取游戏状态失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 验证玩家行动
     */
    @PostMapping("/validate")
    public Map<String, Object> validateAction(
            @RequestParam int playerId,
            @RequestParam String action,
            @RequestParam(required = false) BigDecimal amount) {

        Map<String, Object> result = new HashMap<>();

        try {
            if (currentGameEngine == null) {
                result.put("success", false);
                result.put("error", "没有进行中的游戏");
                return result;
            }
            GameEngine.GameInfo gameInfo = currentGameEngine.getGameInfo();
            GameEngine.Player player = gameInfo.getPlayers().stream()
                    .filter(p -> p.getId() == playerId)
                    .findFirst()
                    .orElse(null);

            if (player == null) {
                result.put("success", false);
                result.put("error", "玩家不存在");
                return result;
            }

            GameEngine.PlayerAction playerAction = GameEngine.PlayerAction.valueOf(action.toUpperCase());
            BettingEngine.ValidationResult validation;

            switch (playerAction) {
                case FOLD:
                    validation = bettingEngine.validateFold(player);
                    break;
                case CHECK:
                    validation = bettingEngine.validateCheck(player, gameInfo.getCurrentBet());
                    break;
                case CALL:
                    validation = bettingEngine.validateCall(player, gameInfo.getCurrentBet());
                    break;
                case RAISE:
                    validation = bettingEngine.validateRaise(player, amount, gameInfo.getCurrentBet(),
                            BigDecimal.valueOf(gameInfo.getBigBlind()));
                    break;
                case ALL_IN:
                    validation = bettingEngine.validateAllIn(player);
                    break;
                default:
                    result.put("success", false);
                    result.put("error", "未知行动类型");
                    return result;
            }

            result.put("success", true);
            result.put("valid", validation.isValid());
            result.put("message", validation.getMessage());
            result.put("minBet", validation.getMinBet());
            result.put("maxBet", validation.getMaxBet());

        } catch (Exception e) {
            log.error("验证玩家行动失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 开始下一局游戏
     */
    @PostMapping("/next-hand")
    public Map<String, Object> startNextHand() {
        Map<String, Object> result = new HashMap<>();

        try {
            if (currentGameEngine == null) {
                result.put("success", false);
                result.put("error", "没有进行中的游戏");
                return result;
            }
            if (!currentGameEngine.canStartNextHand()) {
                result.put("success", false);
                result.put("error", "无法开始下一局：游戏尚未结束");
                return result;
            }

            currentGameEngine.startNextHand();

            // 获取新一局的游戏信息
            GameEngine.GameInfo gameInfo = currentGameEngine.getGameInfo();

            result.put("success", true);
            result.put("message", "下一局游戏开始");
            result.put("gameState", gameInfo.getState().name());
            result.put("gameStateName", getStateName(gameInfo.getState()));
            result.put("players", convertPlayersToDisplay(gameInfo.getPlayers()));
            result.put("communityCards", convertCardsToDisplay(gameInfo.getCommunityCards()));
            result.put("currentBet", gameInfo.getCurrentBet());
            result.put("totalPot", gameInfo.getTotalPot());
            result.put("buttonPosition", gameInfo.getButtonPosition());
            result.put("currentPlayerIndex", gameInfo.getCurrentPlayerIndex());
            result.put("smallBlind", gameInfo.getSmallBlind());
            result.put("bigBlind", gameInfo.getBigBlind());

            log.info("下一局游戏开始成功，按钮位置：{}", gameInfo.getButtonPosition());

        } catch (Exception e) {
            log.error("开始下一局游戏失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    // 辅助方法

    private List<Map<String, Object>> convertPlayersToDisplay(List<GameEngine.Player> players) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (GameEngine.Player player : players) {
            Map<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("id", player.getId());
            playerInfo.put("name", player.getName());
            playerInfo.put("chips", player.getChips());
            playerInfo.put("betAmount", player.getBetAmount());
            playerInfo.put("totalBet", player.getTotalBet());
            playerInfo.put("isActive", player.isActive());
            playerInfo.put("isAllIn", player.isAllIn());
            playerInfo.put("hasFolded", player.isHasFolded());
            playerInfo.put("hasActed", player.isHasActed());
            playerInfo.put("holeCards", convertCardsToDisplay(player.getHoleCards()));
            result.add(playerInfo);
        }
        return result;
    }

    private List<Map<String, Object>> convertCardsToDisplay(List<com.poker.game.model.Card> cards) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (com.poker.game.model.Card card : cards) {
            Map<String, Object> cardInfo = new HashMap<>();
            cardInfo.put("suit", card.getSuit().name());
            cardInfo.put("rank", card.getRank().name());
            cardInfo.put("display", card.getDisplayName());
            cardInfo.put("chinese", card.getChineseDisplayName());
            cardInfo.put("color", (card.getSuit().name().equals("HEART") || card.getSuit().name().equals("DIAMOND")) ? "红色" : "黑色");
            result.add(cardInfo);
        }
        return result;
    }

    private Map<String, Object> convertPotInfoToDisplay(BettingEngine.PotInfo potInfo) {
        Map<String, Object> result = new HashMap<>();
        result.put("mainPot", potInfo.getMainPot());
        result.put("totalPot", potInfo.getTotalPot());

        List<Map<String, Object>> sidePots = new ArrayList<>();
        for (BettingEngine.PotInfo.SidePot sidePot : potInfo.getSidePots()) {
            Map<String, Object> sidePotInfo = new HashMap<>();
            sidePotInfo.put("amount", sidePot.getAmount());
            sidePotInfo.put("playerIds", sidePot.getPlayerIds());
            sidePots.add(sidePotInfo);
        }
        result.put("sidePots", sidePots);

        return result;
    }

    private String getActionName(GameEngine.PlayerAction action) {
        switch (action) {
            case FOLD: return "弃牌";
            case CHECK: return "过牌";
            case CALL: return "跟注";
            case RAISE: return "加注";
            case ALL_IN: return "全下";
            default: return action.name();
        }
    }

    private String getStateName(GameEngine.GameState state) {
        switch (state) {
            case WAITING_FOR_PLAYERS: return "等待玩家";
            case PRE_FLOP: return "翻牌前";
            case FLOP: return "翻牌";
            case TURN: return "转牌";
            case RIVER: return "河牌";
            case SHOWDOWN: return "摊牌";
            case FINISHED: return "游戏结束";
            default: return state.name();
        }
    }

    private Map<String, Object> convertPotStructureToDisplay(PotManager.PotStructure potStructure) {
        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", potStructure.getTotalAmount());
        result.put("playerContributions", potStructure.getPlayerContributions());

        List<Map<String, Object>> pots = new ArrayList<>();
        for (PotManager.Pot pot : potStructure.getPots()) {
            Map<String, Object> potInfo = new HashMap<>();
            potInfo.put("amount", pot.getAmount());
            potInfo.put("eligiblePlayerIds", pot.getEligiblePlayerIds());
            potInfo.put("potIndex", pot.getPotIndex());
            potInfo.put("description", pot.getDescription());
            potInfo.put("settled", pot.isSettled());
            if (pot.isSettled()) {
                potInfo.put("winnerIds", pot.getWinnerIds());
                potInfo.put("winnings", pot.getWinnings());
            }
            pots.add(potInfo);
        }
        result.put("pots", pots);

        return result;
    }

    private Map<String, Object> convertSettlementResultToDisplay(PotManager.SettlementResult settlementResult) {
        Map<String, Object> result = new HashMap<>();
        result.put("totalDistributed", settlementResult.getTotalDistributed());
        result.put("summary", settlementResult.getSummary());
        result.put("playerWinnings", settlementResult.getPlayerWinnings());

        List<Map<String, Object>> settledPots = new ArrayList<>();
        for (PotManager.Pot pot : settlementResult.getSettledPots()) {
            Map<String, Object> potInfo = new HashMap<>();
            potInfo.put("description", pot.getDescription());
            potInfo.put("amount", pot.getAmount());
            potInfo.put("winnerIds", pot.getWinnerIds());
            potInfo.put("winnings", pot.getWinnings());
            settledPots.add(potInfo);
        }
        result.put("settledPots", settledPots);

        return result;
    }
}
