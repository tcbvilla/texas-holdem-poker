import React, { useState, useEffect, useRef, useCallback } from 'react';
import './PokerTable.css';
import { apiGet, apiPost } from '../api';
import { setInternalTitle } from '../branding';

const POLL_INTERVAL = 1500;

const POT_PRESETS = [
    { label: '1/3', ratio: 1 / 3 },
    { label: '1/2', ratio: 1 / 2 },
    { label: '2/3', ratio: 2 / 3 },
    { label: '1x', ratio: 1 },
    { label: '2x', ratio: 2 },
];

const clampRaiseTo = (value, min, max) => {
    const v = Math.round(Number(value) || 0);
    if (v < min) return min;
    if (v > max) return max;
    return v;
};

/** Effective pot = table pot + amount needed to call; raise-to = currentBet + call + increment. */
const calcPotRaiseTo = (state, actions, ratio) => {
    const totalPot = Number(state.totalPot || 0);
    const callAmount = Number(actions.callAmount || 0);
    const currentBet = Number(state.currentBet || 0);
    const minRaiseTo = Number(actions.minRaiseTo || 0);
    const maxRaiseTo = Number(actions.maxRaiseTo || 0);
    const effectivePot = totalPot + callAmount;
    const increment = Math.round(effectivePot * ratio);
    const raiseTo = currentBet + callAmount + increment;
    return clampRaiseTo(raiseTo, minRaiseTo, maxRaiseTo);
};

const PokerTable = ({ roomId, user, onBack }) => {
    const [state, setState] = useState(null);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const [busy, setBusy] = useState(false);
    const [buyin, setBuyin] = useState('');
    const [rebuyAmount, setRebuyAmount] = useState('');
    const [raiseTo, setRaiseTo] = useState(0);
    const [raiseInput, setRaiseInput] = useState('');
    const [raiseMenuOpen, setRaiseMenuOpen] = useState(false);
    const [raiseCustomOpen, setRaiseCustomOpen] = useState(false);
    const [showStats, setShowStats] = useState(false);
    const pollRef = useRef(null);
    const turnKeyRef = useRef(null);

    const loadState = useCallback(async () => {
        const data = await apiGet(`/api/game/rooms/${roomId}/state`);
        if (data.success) {
            setState(data.data);
        } else {
            setError(data.message || '加载牌桌失败');
        }
    }, [roomId]);

    useEffect(() => {
        setInternalTitle('牌桌');
        loadState();
        pollRef.current = setInterval(loadState, POLL_INTERVAL);
        return () => clearInterval(pollRef.current);
    }, [loadState]);

    const setRaiseAmount = (value) => {
        const min = Number(state?.actions?.minRaiseTo || 0);
        const max = Number(state?.actions?.maxRaiseTo || 0);
        const clamped = clampRaiseTo(value, min, max);
        setRaiseTo(clamped);
        setRaiseInput(String(clamped));
    };

    // Default raise when a new betting turn starts (1/2 pot, else minimum).
    useEffect(() => {
        if (!state?.actions?.myTurn || !state.actions.canRaise) return;
        const turnKey = `${state.handNumber}-${state.currentTurnUserId}-${state.gameState}`;
        if (turnKey === turnKeyRef.current) return;
        turnKeyRef.current = turnKey;

        const min = Number(state.actions.minRaiseTo || 0);
        const halfPot = calcPotRaiseTo(state, state.actions, 0.5);
        const defaultTo = halfPot > min ? halfPot : min;
        setRaiseTo(defaultTo);
        setRaiseInput(String(defaultTo));
        setRaiseMenuOpen(false);
        setRaiseCustomOpen(false);
    }, [state]);

    const handleRaiseInputChange = (e) => {
        const raw = e.target.value;
        if (raw === '' || /^\d+$/.test(raw)) {
            setRaiseInput(raw);
        }
    };

    const commitRaiseInput = () => {
        if (!state?.actions?.canRaise) return raiseTo;
        const min = Number(state.actions.minRaiseTo || 0);
        const max = Number(state.actions.maxRaiseTo || 0);
        if (raiseInput === '') {
            setRaiseAmount(min);
            return min;
        }
        const clamped = clampRaiseTo(raiseInput, min, max);
        setRaiseTo(clamped);
        setRaiseInput(String(clamped));
        return clamped;
    };

    const closeRaiseUI = () => {
        setRaiseMenuOpen(false);
        setRaiseCustomOpen(false);
    };

    const handlePresetRaise = (ratio) => {
        if (!state?.actions?.canRaise) return;
        const amount = calcPotRaiseTo(state, state.actions, ratio);
        closeRaiseUI();
        act('RAISE', amount);
    };

    const handleRaiseSubmit = () => {
        const amount = commitRaiseInput();
        closeRaiseUI();
        act('RAISE', amount);
    };

    const toggleRaiseMenu = () => {
        setRaiseMenuOpen((open) => {
            if (open) {
                setRaiseCustomOpen(false);
                return false;
            }
            setRaiseCustomOpen(false);
            return true;
        });
    };

    const flash = (msg) => {
        setNotice(msg);
        setTimeout(() => setNotice(''), 2000);
    };

    const doPost = async (path, body, okMsg) => {
        setBusy(true);
        setError('');
        const data = await apiPost(path, body);
        setBusy(false);
        if (data.success) {
            setState(data.data);
            if (okMsg) flash(okMsg);
            return true;
        }
        setError(data.message || '操作失败');
        return false;
    };

    const handleJoin = async () => {
        const amount = buyin ? parseInt(buyin, 10) : (state ? state.defaultChips : undefined);
        await doPost(`/api/game/rooms/${roomId}/join`, { buyin: amount }, '已入座');
    };

    const handleLeave = async () => {
        if (!window.confirm('确定要离开牌桌吗？局中离开将自动弃牌，局末移除座位。')) return;
        setBusy(true);
        setError('');
        const data = await apiPost(`/api/game/rooms/${roomId}/leave`, {});
        setBusy(false);
        if (data.success) {
            setState(data.data);
            if (data.data.roomClosed) {
                flash('房间已关闭');
                if (onBack) onBack();
            } else if (data.data.pendingLeave) {
                flash('已标记离桌，轮到你会自动弃牌');
            } else if (onBack) {
                onBack();
            }
        } else {
            setError(data.message || '操作失败');
        }
    };

    const handleStartHand = () => doPost(`/api/game/rooms/${roomId}/start-hand`, {}, '本局开始');
    const handleRebuy = async () => {
        const amount = parseInt(rebuyAmount, 10);
        if (!amount || amount <= 0) { setError('请输入有效的补码金额'); return; }
        const ok = await doPost(`/api/game/rooms/${roomId}/rebuy`, { amount });
        if (ok) setRebuyAmount('');
    };

    const act = (action, amount) =>
        doPost(`/api/game/rooms/${roomId}/action`, { action, amount });

    if (!state) {
        return (
            <div className="poker-table-page">
                <div className="table-topbar">
                    <button className="back-btn" onClick={onBack}>返回</button>
                </div>
                <div className="table-loading">{error || '正在加载牌桌...'}</div>
            </div>
        );
    }

    const a = state.actions || {};
    const seated = state.iAmSeated;
    const isPlaying = state.status === 'PLAYING';
    const isFinished = state.status === 'HAND_FINISHED';

    return (
        <div className="poker-table-page">
            <div className="table-topbar">
                <button className="back-btn" onClick={onBack}>返回房间列表</button>
                <div className="table-title">
                    {state.roomName} <span className="room-code">#{state.roomCode}</span>
                </div>
                <div className="table-blinds">盲注 {state.smallBlind}/{state.bigBlind}</div>
                {isPlaying && state.actionTimeRemaining != null && (
                    <div className="action-timer">行动倒计时 {state.actionTimeRemaining}s</div>
                )}
                {seated && <button className="leave-btn" onClick={handleLeave} disabled={busy}>离开牌桌</button>}
                <button className="stats-btn" onClick={() => setShowStats(true)}>筹码盈亏</button>
            </div>

            {error && <div className="table-error">{error}</div>}
            {notice && <div className="table-notice">{notice}</div>}

            <div className="felt">
                <div className="table-ring">
                    <div className="board-area">
                        <div className="pot-label">底池: {fmt(state.totalPot)}</div>
                        <div className="community-cards">
                            {state.communityCards.length === 0
                                ? <div className="board-placeholder">等待发牌</div>
                                : state.communityCards.map((c, i) => <Card key={i} card={c} small />)}
                        </div>
                        {state.gameState && <div className="game-state-tag">{stateLabel(state.gameState)}</div>}
                    </div>

                    <div className="seats-ring">
                        {state.players.length === 0
                            ? <div className="no-players">还没有玩家入座</div>
                            : rotatePlayersForView(state.players, state.myUserId).map((p, i, arr) => (
                                <PlayerSeat
                                    key={p.userId}
                                    player={p}
                                    style={getSeatPosition(i, arr.length)}
                                />
                            ))}
                    </div>
                </div>
            </div>

            {showStats && (
                <div className="stats-modal-overlay" onClick={() => setShowStats(false)}>
                    <div className="stats-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="stats-modal-header">
                            <h3>筹码盈亏</h3>
                            <button className="modal-close-btn" onClick={() => setShowStats(false)}>关闭</button>
                        </div>
                        {(!state.sessionStats || state.sessionStats.length === 0) ? (
                            <p className="stats-modal-empty">入座后开始统计</p>
                        ) : (
                            <table className="stats-modal-table">
                                <thead>
                                    <tr>
                                        <th>玩家</th>
                                        <th>筹码</th>
                                        <th>总买入</th>
                                        <th>盈亏</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {state.sessionStats.map((s) => (
                                        <tr
                                            key={s.userId}
                                            className={`${s.userId === state.myUserId ? 'is-self' : ''}${!s.seated ? ' is-left' : ''}`}
                                        >
                                            <td>
                                                {s.name}{s.userId === state.myUserId ? '（你）' : ''}
                                                {!s.seated && <span className="session-left-tag">离桌</span>}
                                            </td>
                                            <td>{fmt(s.remainingChips)}</td>
                                            <td>{fmt(s.totalBuyIn)}</td>
                                            <td className={Number(s.profitLoss) >= 0 ? 'pnl-up' : 'pnl-down'}>
                                                {Number(s.profitLoss) > 0 ? '+' : ''}{fmt(s.profitLoss)}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>
            )}

            {/* Settlement */}
            {isFinished && state.settlementSummary && (
                <div className="settlement-box">
                    <h3>本局结算</h3>
                    <p>{state.settlementSummary}</p>
                    {state.autoNextHandInMs != null && state.canStart && (
                        <p className="auto-next-hint">
                            {Math.ceil(state.autoNextHandInMs / 1000)} 秒后自动开始下一局
                        </p>
                    )}
                </div>
            )}

            {/* Controls */}
            <div className="controls">
                {!seated && (
                    <div className="join-panel">
                        <span>买入筹码：</span>
                        <input
                            type="number"
                            value={buyin}
                            placeholder={`${state.defaultChips}`}
                            onChange={(e) => setBuyin(e.target.value)}
                        />
                        <span className="buyin-hint">范围 {state.minBuyin} - {state.maxBuyin}</span>
                        <button className="primary-btn" onClick={handleJoin} disabled={busy}>入座</button>
                    </div>
                )}

                {seated && !isPlaying && !isFinished && state.canStart && (
                    <button className="primary-btn" onClick={handleStartHand} disabled={busy}>
                        开始本局
                    </button>
                )}

                {seated && isFinished && !state.canStart && (
                    <div className="waiting-hint">等待更多玩家入座（至少 2 名有筹码的玩家）</div>
                )}

                {seated && (
                    <div className="rebuy-panel">
                        <input
                            type="number"
                            value={rebuyAmount}
                            placeholder="补码金额"
                            onChange={(e) => setRebuyAmount(e.target.value)}
                        />
                        <button className="secondary-btn" onClick={handleRebuy} disabled={busy}>补码</button>
                        {isPlaying && <span className="buyin-hint">局中补码将在下一局生效</span>}
                    </div>
                )}

                {isPlaying && a.myTurn && (
                    <div className="action-bar">
                        <button className="act-fold" onClick={() => act('FOLD')} disabled={busy}>弃牌</button>
                        {a.canCheck
                            ? <button className="act-check" onClick={() => act('CHECK')} disabled={busy}>过牌</button>
                            : <button className="act-call" onClick={() => act('CALL')} disabled={busy || !a.canCall}>
                                跟注 {fmt(a.callAmount)}
                              </button>}
                        {a.canRaise && (
                            <div className="raise-wrap">
                                <button
                                    type="button"
                                    className={`act-raise${raiseMenuOpen ? ' is-open' : ''}`}
                                    onClick={toggleRaiseMenu}
                                    disabled={busy}
                                >
                                    加注
                                </button>
                                {raiseMenuOpen && (
                                    <div className="raise-popup">
                                        {POT_PRESETS.map((preset) => {
                                            const presetTo = calcPotRaiseTo(state, a, preset.ratio);
                                            return (
                                                <button
                                                    key={preset.label}
                                                    type="button"
                                                    className="raise-menu-item"
                                                    onClick={() => handlePresetRaise(preset.ratio)}
                                                    disabled={busy}
                                                >
                                                    <span>{preset.label}</span>
                                                    <span>{presetTo}</span>
                                                </button>
                                            );
                                        })}
                                        <button
                                            type="button"
                                            className={`raise-menu-item raise-menu-custom${raiseCustomOpen ? ' is-active' : ''}`}
                                            onClick={() => setRaiseCustomOpen(true)}
                                            disabled={busy}
                                        >
                                            <span>自定义</span>
                                        </button>
                                        {raiseCustomOpen && (
                                            <div className="raise-custom-panel">
                                                <input
                                                    type="range"
                                                    min={Number(a.minRaiseTo)}
                                                    max={Number(a.maxRaiseTo)}
                                                    step={state.bigBlind}
                                                    value={raiseTo}
                                                    onChange={(e) => setRaiseAmount(parseInt(e.target.value, 10))}
                                                />
                                                <div className="raise-custom-row">
                                                    <input
                                                        type="text"
                                                        inputMode="numeric"
                                                        className="raise-input"
                                                        value={raiseInput}
                                                        onChange={handleRaiseInputChange}
                                                        onBlur={commitRaiseInput}
                                                        onKeyDown={(e) => {
                                                            if (e.key === 'Enter') handleRaiseSubmit();
                                                        }}
                                                    />
                                                    <button
                                                        type="button"
                                                        className="raise-confirm-btn"
                                                        onClick={handleRaiseSubmit}
                                                        disabled={busy}
                                                    >
                                                        确认 {raiseInput !== '' ? raiseInput : raiseTo}
                                                    </button>
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        )}
                        <button className="act-allin" onClick={() => act('ALL_IN')} disabled={busy}>全下</button>
                    </div>
                )}

                {isPlaying && !a.myTurn && (
                    <div className="waiting-hint">
                        {state.currentTurnUserId > 0
                            ? `等待 ${turnName(state)} 行动...`
                            : '等待中...'}
                    </div>
                )}
            </div>
        </div>
    );
};

const fmt = (v) => {
    const n = Number(v || 0);
    return Number.isInteger(n) ? n : n.toFixed(0);
};

const formatPlayerAction = (player) => {
    if (!player.lastAction) return null;
    const hasAmt = player.lastActionAmount != null && player.lastActionAmount !== '';
    const amt = hasAmt ? fmt(player.lastActionAmount) : '';
    switch (player.lastAction) {
        case 'CHECK': return '过牌';
        case 'FOLD': return '弃牌';
        case 'CALL': return hasAmt ? `跟注 ${amt}` : '跟注';
        case 'RAISE': return hasAmt ? `加注 ${amt}` : '加注';
        case 'ALL_IN': return hasAmt ? `全下 ${amt}` : '全下';
        default: return null;
    }
};

const stateLabel = (s) => ({
    PRE_FLOP: '翻牌前', FLOP: '翻牌', TURN: '转牌', RIVER: '河牌',
    SHOWDOWN: '摊牌', FINISHED: '本局结束', WAITING_FOR_PLAYERS: '等待玩家'
}[s] || s);

const turnName = (state) => {
    const p = state.players.find((x) => x.userId === state.currentTurnUserId);
    return p ? p.name : '对手';
};

/** Put self at bottom, others clockwise around the table. */
const rotatePlayersForView = (players, myUserId) => {
    const sorted = [...players].sort((a, b) => a.seatIndex - b.seatIndex);
    if (sorted.length === 0) return [];
    const selfIdx = sorted.findIndex((p) => p.userId === myUserId);
    if (selfIdx <= 0) return sorted;
    return [...sorted.slice(selfIdx), ...sorted.slice(0, selfIdx)];
};

/** Distribute seats evenly on an ellipse; index 0 = bottom center. */
const getSeatPosition = (index, total) => {
    const angle = (2 * Math.PI * index) / total + Math.PI / 2;
    const rx = 44;
    const ry = 40;
    const x = 50 + rx * Math.cos(angle);
    const y = 50 + ry * Math.sin(angle);
    return { left: `${x}%`, top: `${y}%` };
};

const Card = ({ card, hidden, small }) => {
    if (hidden || !card) {
        return <div className={`card card-back${small ? ' card-sm' : ''}`} />;
    }
    return (
        <div className={`card ${card.red ? 'card-red' : 'card-black'}${small ? ' card-sm' : ''}`}>
            <span className="card-rank">{card.rank}</span>
            <span className="card-suit">{card.suit}</span>
        </div>
    );
};

const PlayerSeat = ({ player, style }) => {
    const classes = ['player-seat'];
    if (player.isSelf) classes.push('is-self');
    if (player.isCurrentTurn) classes.push('is-turn');
    if (player.folded) classes.push('is-folded');
    if (player.isWinner) classes.push('is-winner');

    // Show last action for everyone except the player who must act now (old label hidden until they act again).
    const actionText = !player.isCurrentTurn ? formatPlayerAction(player) : null;
    const actionClass = player.lastAction
        ? `action-${player.lastAction.toLowerCase().replace(/_/g, '-')}`
        : '';

    return (
        <div className={classes.join(' ')} style={style}>
            <div className="player-badges">
                {player.isButton && <span className="badge badge-btn">D</span>}
                {player.isSmallBlind && <span className="badge badge-sb">SB</span>}
                {player.isBigBlind && <span className="badge badge-bb">BB</span>}
                {player.allIn && <span className="badge badge-allin">AI</span>}
            </div>
            <div className="player-name" title={player.name}>
                {player.name}{player.isSelf ? '（你）' : ''}
            </div>
            <div className="player-chips">{fmt(player.chips)}</div>

            <div className="player-cards">
                {player.hasHoleCards ? (
                    player.holeCardsVisible
                        ? player.holeCards.map((c, i) => <Card key={i} card={c} small />)
                        : [<Card key="a" hidden small />, <Card key="b" hidden small />]
                ) : null}
            </div>

            {Number(player.betAmount) > 0 && (
                <div className="player-bet">前注 {fmt(player.betAmount)}</div>
            )}
            {actionText && (
                <div className={`player-action-tag ${actionClass}`}>
                    {actionText}
                </div>
            )}
            {player.handDescription && (
                <div className="hand-desc" title={player.handDescription}>{player.handDescription}</div>
            )}
            {player.isWinner && <div className="win-tag">+{fmt(player.winAmount)}</div>}
        </div>
    );
};

export default PokerTable;
