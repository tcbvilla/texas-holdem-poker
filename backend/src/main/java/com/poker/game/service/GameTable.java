package com.poker.game.service;

import com.poker.dto.RoomSettlementSnapshot;
import com.poker.entity.Room;
import com.poker.game.core.GameEngine;
import com.poker.game.core.HandEvaluator;
import com.poker.game.core.HandRank;
import com.poker.game.core.PotManager;
import com.poker.game.model.Card;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory poker table bound to a persisted room.
 * Bridges the pure {@link GameEngine} with room membership, buy-in and seat management.
 * All mutating methods are synchronized to keep the table consistent under concurrent polling/actions.
 */
@Slf4j
public class GameTable {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_PLAYING = "PLAYING";
    public static final String STATUS_HAND_FINISHED = "HAND_FINISHED";

    private static final long AUTO_NEXT_HAND_MS = 5000;

    static class Seat {
        final int userId;
        final String name;
        int seatIndex;
        BigDecimal chips;
        BigDecimal pendingChips = BigDecimal.ZERO;

        Seat(int userId, String name, int seatIndex, BigDecimal chips) {
            this.userId = userId;
            this.name = name;
            this.seatIndex = seatIndex;
            this.chips = chips;
        }
    }

    /** Per-player buy-in ledger for the current table session (survives leave until table closes). */
    static class SessionPlayer {
        final int userId;
        String name;
        BigDecimal totalBuyIn = BigDecimal.ZERO;
        BigDecimal remainingChips = BigDecimal.ZERO;

        SessionPlayer(int userId, String name) {
            this.userId = userId;
            this.name = name;
        }
    }

    private final Long roomId;
    private final String roomCode;
    private final String roomName;
    private final int smallBlind;
    private final int bigBlind;
    private final int maxSeats;
    private final long minBuyin;
    private final long maxBuyin;
    private final long defaultChips;
    private final int actionTimeSeconds;
    private final Integer durationMinutes;

    private final PotManager potManager;
    private final HandEvaluator handEvaluator;

    private final Map<Integer, Seat> seats = new LinkedHashMap<>();
    private final Map<Integer, SessionPlayer> sessionLedger = new LinkedHashMap<>();
    private final Set<Integer> pendingLeave = new HashSet<>();

    private GameEngine currentGame;
    private boolean handInProgress = false;
    private int handNumber = 0;

    /** Physical seat index of the button on the previous hand (null = first hand). */
    private Integer lastButtonSeatIndex = null;

    private long turnStartedAtMs = 0;
    private int lastTurnPlayerId = -1;
    private long handFinishedAtMs = 0;
    private boolean pendingRoomClose = false;
    private boolean firstHandStarted = false;

    public GameTable(Room room, PotManager potManager, HandEvaluator handEvaluator) {
        this.roomId = room.getId();
        this.roomCode = room.getRoomCode();
        this.roomName = room.getName();
        this.smallBlind = room.getSmallBlind();
        this.bigBlind = room.getBigBlind();
        this.maxSeats = room.getMaxSeats() != null ? room.getMaxSeats() : 9;
        this.minBuyin = room.getMinBuyin() != null ? room.getMinBuyin() : 0L;
        this.maxBuyin = room.getMaxBuyin() != null ? room.getMaxBuyin() : Long.MAX_VALUE;
        this.defaultChips = room.getDefaultChips() != null ? room.getDefaultChips() : 0L;
        this.actionTimeSeconds = room.getActionTimeSeconds() != null ? room.getActionTimeSeconds() : 30;
        this.durationMinutes = room.getDurationMinutes();
        this.potManager = potManager;
        this.handEvaluator = handEvaluator;
    }

    public long getDefaultChips() {
        return defaultChips;
    }

    public long getMinBuyin() {
        return minBuyin;
    }

    public long getMaxBuyin() {
        return maxBuyin;
    }

    public boolean isHandInProgress() {
        return handInProgress;
    }

    public boolean isEmpty() {
        return seats.isEmpty();
    }

    public boolean isPendingRoomClose() {
        return pendingRoomClose;
    }

    public void requestRoomClose() {
        pendingRoomClose = true;
    }

    public boolean consumePendingRoomClose() {
        if (pendingRoomClose && !handInProgress) {
            pendingRoomClose = false;
            return true;
        }
        return false;
    }

    public boolean needsRoomStart() {
        return !firstHandStarted;
    }

    public void markRoomStarted() {
        firstHandStarted = true;
    }

    public synchronized void join(int userId, String name, long buyin) {
        if (seats.containsKey(userId)) {
            throw new IllegalStateException("You are already seated at this table");
        }
        if (buyin < minBuyin || buyin > maxBuyin) {
            throw new IllegalArgumentException("Buy-in must be between " + minBuyin + " and " + maxBuyin);
        }
        int seatIndex = allocateSeatIndex();
        if (seatIndex < 0) {
            throw new IllegalStateException("Table is full");
        }
        seats.put(userId, new Seat(userId, name, seatIndex, BigDecimal.valueOf(buyin)));
        recordBuyIn(userId, name, buyin);
        updateRemainingChips(userId);
        log.info("User {} joined table {} at seat {} with buyin {}", userId, roomId, seatIndex, buyin);
    }

    /**
     * Leave the table at any time. Mid-hand leavers are marked and auto-folded on their turn.
     */
    public synchronized void leave(int userId) {
        Seat seat = seats.get(userId);
        if (seat == null) {
            return;
        }
        if (handInProgress && currentGame != null && isInCurrentHand(userId) && !hasFolded(userId)) {
            pendingLeave.add(userId);
            if (isPlayerTurn(userId)) {
                currentGame.forceFold(userId);
                settleIfFinished();
                removePendingLeavePlayers();
            }
            log.info("User {} marked pending leave at table {}", userId, roomId);
            return;
        }
        updateRemainingChips(userId);
        seats.remove(userId);
        pendingLeave.remove(userId);
        log.info("User {} left table {}", userId, roomId);
    }

    /**
     * Add chips. During a hand the amount is queued for the next hand.
     */
    public synchronized void rebuy(int userId, long amount) {
        Seat seat = seats.get(userId);
        if (seat == null) {
            throw new IllegalStateException("You are not seated at this table");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Rebuy amount must be positive");
        }
        BigDecimal newTotal = seat.chips.add(seat.pendingChips).add(BigDecimal.valueOf(amount));
        if (newTotal.compareTo(BigDecimal.valueOf(maxBuyin)) > 0) {
            throw new IllegalArgumentException("Total stack cannot exceed max buy-in " + maxBuyin);
        }
        if (handInProgress) {
            seat.pendingChips = seat.pendingChips.add(BigDecimal.valueOf(amount));
            log.info("User {} queued rebuy {} for next hand at table {}", userId, amount, roomId);
        } else {
            seat.chips = seat.chips.add(BigDecimal.valueOf(amount));
            log.info("User {} rebought {} at table {}, stack now {}", userId, amount, roomId, seat.chips);
        }
        recordBuyIn(userId, seat.name, amount);
        updateRemainingChips(userId);
    }

    public synchronized void startHand() {
        if (handInProgress) {
            throw new IllegalStateException("A hand is already in progress");
        }
        startHandInternal();
    }

    public synchronized void processAction(int userId, String action, BigDecimal amount) {
        tickGameLoop();
        if (!handInProgress || currentGame == null) {
            throw new IllegalStateException("No hand in progress");
        }
        if (!isInCurrentHand(userId)) {
            throw new IllegalStateException("You are not part of the current hand");
        }
        GameEngine.PlayerAction playerAction;
        try {
            playerAction = GameEngine.PlayerAction.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown action: " + action);
        }
        boolean ok = currentGame.processPlayerAction(userId, playerAction, amount);
        if (!ok) {
            throw new IllegalArgumentException("Invalid action, please check whether it is your turn and the amount is valid");
        }
        refreshTurnTimer();
        settleIfFinished();
    }

    public synchronized void startNextHand() {
        if (handInProgress) {
            throw new IllegalStateException("Current hand is not finished yet");
        }
        startHandInternal();
    }

    public synchronized boolean isMember(int userId) {
        return seats.containsKey(userId);
    }

    public synchronized Map<String, Object> buildStateView(int viewerId) {
        tickGameLoop();

        Map<String, Object> view = new HashMap<>();
        view.put("roomId", roomId);
        view.put("roomCode", roomCode);
        view.put("roomName", roomName);
        view.put("smallBlind", smallBlind);
        view.put("bigBlind", bigBlind);
        view.put("maxSeats", maxSeats);
        view.put("minBuyin", minBuyin);
        view.put("maxBuyin", maxBuyin);
        view.put("defaultChips", defaultChips);
        view.put("actionTimeSeconds", actionTimeSeconds);
        view.put("durationMinutes", durationMinutes);
        view.put("myUserId", viewerId);
        view.put("iAmSeated", seats.containsKey(viewerId));
        view.put("handNumber", handNumber);
        view.put("pendingLeave", pendingLeave.contains(viewerId));

        String status = handInProgress ? STATUS_PLAYING
                : (currentGame != null && currentGame.getCurrentState() == GameEngine.GameState.FINISHED
                    ? STATUS_HAND_FINISHED : STATUS_WAITING);
        view.put("status", status);

        boolean canStart = !handInProgress && playableSeats().size() >= 2;
        view.put("canStart", canStart);

        if (status.equals(STATUS_HAND_FINISHED) && handFinishedAtMs > 0) {
            long remaining = AUTO_NEXT_HAND_MS - (System.currentTimeMillis() - handFinishedAtMs);
            view.put("autoNextHandInMs", Math.max(0, remaining));
        }

        Map<Integer, GameEngine.Player> enginePlayers = new HashMap<>();
        Map<Integer, Integer> engineIndex = new HashMap<>();
        int buttonPos = -1, sbPos = -1, bbPos = -1, turnPos = -1;
        BigDecimal currentBet = BigDecimal.ZERO;
        BigDecimal totalPot = BigDecimal.ZERO;
        List<Map<String, Object>> community = new ArrayList<>();
        Map<Integer, HandRank> showdown = null;
        PotManager.SettlementResult settlement = null;

        if (currentGame != null) {
            List<GameEngine.Player> ps = currentGame.getPlayers();
            for (int i = 0; i < ps.size(); i++) {
                enginePlayers.put(ps.get(i).getId(), ps.get(i));
                engineIndex.put(ps.get(i).getId(), i);
            }
            int n = ps.size();
            buttonPos = currentGame.getButtonPosition();
            if (n == 2) {
                sbPos = buttonPos;
                bbPos = (buttonPos + 1) % n;
            } else if (n > 0) {
                sbPos = (buttonPos + 1) % n;
                bbPos = (buttonPos + 2) % n;
            }
            turnPos = currentGame.getCurrentPlayerIndex();
            currentBet = currentGame.getCurrentBet();
            totalPot = ps.stream().map(GameEngine.Player::getTotalBet).reduce(BigDecimal.ZERO, BigDecimal::add);
            for (Card c : currentGame.getCommunityCards()) {
                community.add(cardToMap(c));
            }
            showdown = currentGame.getShowdownHands();
            settlement = currentGame.getSettlementResult();
        }

        view.put("currentBet", currentBet);
        view.put("totalPot", totalPot);
        view.put("communityCards", community);
        view.put("gameState", currentGame != null ? currentGame.getCurrentState().name() : null);
        if (settlement != null) {
            view.put("settlementSummary", settlement.getSummary());
        }

        boolean revealAll = currentGame != null
                && (currentGame.getCurrentState() == GameEngine.GameState.SHOWDOWN
                    || currentGame.getCurrentState() == GameEngine.GameState.FINISHED)
                && showdown != null;

        List<Map<String, Object>> playerViews = new ArrayList<>();
        List<Seat> sortedSeats = seats.values().stream()
                .sorted(Comparator.comparingInt(s -> s.seatIndex))
                .collect(Collectors.toList());

        int currentTurnUserId = -1;
        for (Seat seat : sortedSeats) {
            Map<String, Object> pv = new HashMap<>();
            GameEngine.Player ep = enginePlayers.get(seat.userId);
            boolean inHand = ep != null;
            int idx = inHand ? engineIndex.get(seat.userId) : -1;

            pv.put("userId", seat.userId);
            pv.put("name", seat.name);
            pv.put("seatIndex", seat.seatIndex);
            pv.put("chips", inHand ? ep.getChips() : seat.chips);
            pv.put("pendingChips", seat.pendingChips);
            pv.put("inHand", inHand);
            pv.put("isSelf", seat.userId == viewerId);
            pv.put("pendingLeave", pendingLeave.contains(seat.userId));
            pv.put("betAmount", inHand ? ep.getBetAmount() : BigDecimal.ZERO);
            pv.put("folded", inHand && ep.isHasFolded());
            pv.put("allIn", inHand && ep.isAllIn());
            if (inHand) {
                pv.put("hasActed", ep.isHasActed());
                if (ep.getLastAction() != null) {
                    pv.put("lastAction", ep.getLastAction().name());
                    if (ep.getLastActionAmount() != null) {
                        pv.put("lastActionAmount", ep.getLastActionAmount());
                    }
                }
            }
            pv.put("isButton", inHand && idx == buttonPos);
            pv.put("isSmallBlind", inHand && idx == sbPos);
            pv.put("isBigBlind", inHand && idx == bbPos);
            boolean isTurn = inHand && idx == turnPos && handInProgress;
            pv.put("isCurrentTurn", isTurn);
            if (isTurn) {
                currentTurnUserId = seat.userId;
            }

            List<Map<String, Object>> holeCards = new ArrayList<>();
            boolean showCards = false;
            if (inHand) {
                boolean self = seat.userId == viewerId;
                boolean revealThis = revealAll && !ep.isHasFolded()
                        && showdown != null && showdown.containsKey(seat.userId);
                if (self || revealThis) {
                    showCards = true;
                    for (Card c : ep.getHoleCards()) {
                        holeCards.add(cardToMap(c));
                    }
                }
            }
            pv.put("hasHoleCards", inHand && ep.getHoleCards().size() == 2);
            pv.put("holeCardsVisible", showCards);
            pv.put("holeCards", holeCards);

            if (showdown != null && showdown.containsKey(seat.userId)) {
                pv.put("handDescription", showdown.get(seat.userId).getDescription());
            }
            if (settlement != null) {
                BigDecimal win = settlement.getPlayerWinnings().getOrDefault(seat.userId, BigDecimal.ZERO);
                pv.put("winAmount", win);
                pv.put("isWinner", win.compareTo(BigDecimal.ZERO) > 0);
            } else {
                pv.put("winAmount", BigDecimal.ZERO);
                pv.put("isWinner", false);
            }

            playerViews.add(pv);
        }
        view.put("players", playerViews);
        view.put("currentTurnUserId", currentTurnUserId);
        view.put("sessionStats", buildSessionStats(enginePlayers));

        if (handInProgress && currentTurnUserId > 0 && actionTimeSeconds > 0) {
            long elapsed = System.currentTimeMillis() - turnStartedAtMs;
            int remaining = (int) Math.max(0, actionTimeSeconds - elapsed / 1000);
            view.put("actionTimeRemaining", remaining);
        }

        Map<String, Object> actions = new HashMap<>();
        GameEngine.Player me = enginePlayers.get(viewerId);
        boolean myTurn = me != null && handInProgress && engineIndex.getOrDefault(viewerId, -1) == turnPos;
        actions.put("myTurn", myTurn);
        if (myTurn && currentGame != null) {
            BigDecimal toCall = currentBet.subtract(me.getBetAmount());
            if (toCall.compareTo(BigDecimal.ZERO) < 0) {
                toCall = BigDecimal.ZERO;
            }
            BigDecimal callable = toCall.min(me.getChips());
            actions.put("callAmount", callable);
            actions.put("canCheck", toCall.compareTo(BigDecimal.ZERO) == 0);
            actions.put("canCall", toCall.compareTo(BigDecimal.ZERO) > 0 && me.getChips().compareTo(BigDecimal.ZERO) > 0);
            BigDecimal fullMinRaiseTotal = currentGame.getMinRaiseTo();
            BigDecimal maxRaiseTotal = me.getBetAmount().add(me.getChips());
            boolean canRaise = maxRaiseTotal.compareTo(fullMinRaiseTotal) >= 0;
            actions.put("canRaise", canRaise);
            actions.put("minRaiseTo", fullMinRaiseTotal);
            actions.put("maxRaiseTo", maxRaiseTotal);
            actions.put("myChips", me.getChips());
        }
        view.put("actions", actions);

        return view;
    }

    // ----------------------------------------------------------------------
    // Game loop ticks (timeout, auto next hand, pending leave)
    // ----------------------------------------------------------------------

    private void tickGameLoop() {
        tickActionTimeout();
        tickAutoNextHand();
    }

    private void tickActionTimeout() {
        if (!handInProgress || currentGame == null || actionTimeSeconds <= 0) {
            return;
        }
        refreshTurnTimer();
        int turnIdx = currentGame.getCurrentPlayerIndex();
        if (turnIdx < 0) {
            return;
        }
        long elapsed = System.currentTimeMillis() - turnStartedAtMs;
        if (elapsed < actionTimeSeconds * 1000L) {
            return;
        }
        int turnPlayerId = currentGame.getPlayers().get(turnIdx).getId();
        log.info("Action timeout for player {} at table {}", turnPlayerId, roomId);
        if (pendingLeave.contains(turnPlayerId)) {
            currentGame.forceFold(turnPlayerId);
        } else {
            currentGame.forceAutoAction(turnPlayerId);
        }
        turnStartedAtMs = System.currentTimeMillis();
        lastTurnPlayerId = -1;
        refreshTurnTimer();
        settleIfFinished();
        removePendingLeavePlayers();
    }

    private void tickAutoNextHand() {
        if (handInProgress || pendingRoomClose) {
            return;
        }
        if (currentGame == null || currentGame.getCurrentState() != GameEngine.GameState.FINISHED) {
            return;
        }
        if (handFinishedAtMs == 0) {
            handFinishedAtMs = System.currentTimeMillis();
            return;
        }
        if (System.currentTimeMillis() - handFinishedAtMs < AUTO_NEXT_HAND_MS) {
            return;
        }
        if (playableSeats().size() < 2) {
            return;
        }
        try {
            startHandInternal();
            handFinishedAtMs = 0;
        } catch (Exception e) {
            log.warn("Auto start next hand failed at table {}: {}", roomId, e.getMessage());
        }
    }

    private void refreshTurnTimer() {
        if (!handInProgress || currentGame == null) {
            return;
        }
        int turnIdx = currentGame.getCurrentPlayerIndex();
        if (turnIdx < 0) {
            return;
        }
        int turnPlayerId = currentGame.getPlayers().get(turnIdx).getId();
        if (turnPlayerId != lastTurnPlayerId) {
            lastTurnPlayerId = turnPlayerId;
            turnStartedAtMs = System.currentTimeMillis();
            if (pendingLeave.contains(turnPlayerId)) {
                currentGame.forceFold(turnPlayerId);
                settleIfFinished();
                removePendingLeavePlayers();
            }
        }
    }

    // ----------------------------------------------------------------------
    // Hand lifecycle
    // ----------------------------------------------------------------------

    private void startHandInternal() {
        applyPendingChips();
        List<Seat> playing = playableSeats();
        if (playing.size() < 2) {
            throw new IllegalStateException("Need at least 2 players with chips to start");
        }

        int buttonEngineIndex = resolveButtonEngineIndex(playing);
        List<GameEngine.Player> enginePlayers = playing.stream()
                .map(s -> new GameEngine.Player(s.userId, s.name, s.chips))
                .collect(Collectors.toList());

        Map<Integer, Integer> seatMap = playing.stream()
                .collect(Collectors.toMap(s -> s.userId, s -> s.seatIndex));
        int physicalButtonSeat = playing.get(buttonEngineIndex).seatIndex;

        currentGame = new GameEngine(potManager, handEvaluator);
        currentGame.setSeatContext(seatMap, physicalButtonSeat);
        String gameId = "room-" + roomId + "-hand-" + (handNumber + 1);
        currentGame.initializeGame(gameId, enginePlayers, smallBlind, bigBlind, buttonEngineIndex);
        currentGame.startNewHand();

        handInProgress = true;
        handNumber++;
        lastTurnPlayerId = -1;
        turnStartedAtMs = System.currentTimeMillis();
        handFinishedAtMs = 0;
        settleIfFinished();
        refreshTurnTimer();
        log.info("Table {} started hand {} with {} players, button seat {}",
                roomId, handNumber, enginePlayers.size(), physicalButtonSeat);
    }

    private void applyPendingChips() {
        for (Seat seat : seats.values()) {
            if (seat.pendingChips.compareTo(BigDecimal.ZERO) > 0) {
                seat.chips = seat.chips.add(seat.pendingChips);
                seat.pendingChips = BigDecimal.ZERO;
            }
        }
    }

    private int resolveButtonEngineIndex(List<Seat> playing) {
        List<Integer> seatIndices = playing.stream()
                .map(s -> s.seatIndex)
                .sorted()
                .collect(Collectors.toList());

        int buttonSeat;
        if (lastButtonSeatIndex == null) {
            buttonSeat = seatIndices.get(0);
        } else {
            buttonSeat = nextSeatClockwise(seatIndices, lastButtonSeatIndex);
        }
        lastButtonSeatIndex = buttonSeat;

        for (int i = 0; i < playing.size(); i++) {
            if (playing.get(i).seatIndex == buttonSeat) {
                return i;
            }
        }
        return 0;
    }

    private int nextSeatClockwise(List<Integer> seats, int fromSeat) {
        for (int s : seats) {
            if (s > fromSeat) {
                return s;
            }
        }
        return seats.get(0);
    }

    private void settleIfFinished() {
        if (currentGame != null && currentGame.getCurrentState() == GameEngine.GameState.FINISHED) {
            syncChipsFromEngine();
            handInProgress = false;
            handFinishedAtMs = System.currentTimeMillis();
            removePendingLeavePlayers();
            log.info("Table {} hand {} finished", roomId, handNumber);
        }
    }

    private void removePendingLeavePlayers() {
        if (handInProgress) {
            return;
        }
        for (Integer id : new HashSet<>(pendingLeave)) {
            updateRemainingChips(id);
            seats.remove(id);
            log.info("Removed pending-leave player {} from table {}", id, roomId);
        }
        pendingLeave.clear();
    }

    private void syncChipsFromEngine() {
        if (currentGame == null) {
            return;
        }
        for (GameEngine.Player p : currentGame.getPlayers()) {
            Seat seat = seats.get(p.getId());
            if (seat != null) {
                seat.chips = p.getChips();
            }
        }
    }

    private List<Seat> playableSeats() {
        return seats.values().stream()
                .filter(s -> s.chips.compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparingInt(s -> s.seatIndex))
                .collect(Collectors.toList());
    }

    private int allocateSeatIndex() {
        Set<Integer> used = seats.values().stream().map(s -> s.seatIndex).collect(Collectors.toSet());
        for (int i = 0; i < maxSeats; i++) {
            if (!used.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInCurrentHand(int userId) {
        if (currentGame == null) {
            return false;
        }
        return currentGame.getPlayers().stream().anyMatch(p -> p.getId() == userId);
    }

    private boolean hasFolded(int userId) {
        if (currentGame == null) {
            return false;
        }
        return currentGame.getPlayers().stream()
                .filter(p -> p.getId() == userId)
                .findFirst()
                .map(GameEngine.Player::isHasFolded)
                .orElse(false);
    }

    private boolean isPlayerTurn(int userId) {
        if (!handInProgress || currentGame == null) {
            return false;
        }
        int idx = currentGame.getCurrentPlayerIndex();
        if (idx < 0) {
            return false;
        }
        return currentGame.getPlayers().get(idx).getId() == userId;
    }

    private Map<String, Object> cardToMap(Card card) {
        Map<String, Object> m = new HashMap<>();
        m.put("rank", card.getRank().getSymbol());
        m.put("suit", card.getSuit().getSymbol());
        m.put("suitName", card.getSuit().name());
        m.put("display", card.getDisplayName());
        m.put("red", card.isRed());
        return m;
    }

    /**
     * Build settlement snapshot from the session ledger. Refreshes remaining chips for seated players first.
     */
    public synchronized RoomSettlementSnapshot exportSettlement() {
        if (handInProgress && currentGame != null) {
            syncChipsFromEngine();
        }
        for (Integer userId : new ArrayList<>(seats.keySet())) {
            updateRemainingChips(userId);
        }

        RoomSettlementSnapshot snapshot = new RoomSettlementSnapshot();
        snapshot.setClosedAt(LocalDateTime.now());
        List<RoomSettlementSnapshot.PlayerSettlement> players = sessionLedger.values().stream()
                .sorted(Comparator.comparingInt(sp -> sp.userId))
                .map(sp -> {
                    RoomSettlementSnapshot.PlayerSettlement ps = new RoomSettlementSnapshot.PlayerSettlement();
                    ps.setUserId(sp.userId);
                    ps.setUsername(sp.name);
                    long buyIn = sp.totalBuyIn.longValue();
                    long remaining = sp.remainingChips.longValue();
                    ps.setTotalBuyIn(buyIn);
                    ps.setRemainingChips(remaining);
                    ps.setProfitLoss(remaining - buyIn);
                    return ps;
                })
                .collect(Collectors.toList());
        snapshot.setPlayers(players);
        return snapshot;
    }

    public synchronized boolean hasSessionLedger() {
        return !sessionLedger.isEmpty();
    }

    private List<Map<String, Object>> buildSessionStats(Map<Integer, GameEngine.Player> enginePlayers) {
        if (handInProgress && currentGame != null) {
            syncChipsFromEngine();
        }
        for (Integer userId : new ArrayList<>(seats.keySet())) {
            updateRemainingChips(userId);
        }

        return sessionLedger.values().stream()
                .sorted(Comparator.comparingInt(sp -> sp.userId))
                .map(sp -> {
                    Seat seat = seats.get(sp.userId);
                    long remaining;
                    if (seat != null) {
                        GameEngine.Player ep = enginePlayers.get(sp.userId);
                        BigDecimal liveChips = ep != null ? ep.getChips() : seat.chips;
                        remaining = liveChips.add(seat.pendingChips).longValue();
                    } else {
                        remaining = sp.remainingChips.longValue();
                    }
                    long buyIn = sp.totalBuyIn.longValue();
                    Map<String, Object> row = new HashMap<>();
                    row.put("userId", sp.userId);
                    row.put("name", sp.name);
                    row.put("totalBuyIn", buyIn);
                    row.put("remainingChips", remaining);
                    row.put("profitLoss", remaining - buyIn);
                    row.put("seated", seat != null);
                    return row;
                })
                .collect(Collectors.toList());
    }

    private void recordBuyIn(int userId, String name, long amount) {
        SessionPlayer sp = sessionLedger.computeIfAbsent(userId, id -> new SessionPlayer(id, name));
        sp.name = name;
        sp.totalBuyIn = sp.totalBuyIn.add(BigDecimal.valueOf(amount));
    }

    private void updateRemainingChips(int userId) {
        Seat seat = seats.get(userId);
        if (seat == null) {
            return;
        }
        SessionPlayer sp = sessionLedger.computeIfAbsent(userId, id -> new SessionPlayer(id, seat.name));
        sp.name = seat.name;
        sp.remainingChips = seat.chips.add(seat.pendingChips);
    }
}
