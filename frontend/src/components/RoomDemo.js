import React, { useState, useEffect } from 'react';
import './RoomDemo.css';

const RoomDemo = () => {
    const [roomInfo, setRoomInfo] = useState(null);
    const [gameInfo, setGameInfo] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [playerActions, setPlayerActions] = useState({});

    // 创建测试房间
    const createTestRoom = async () => {
        setLoading(true);
        setError(null);
        
        try {
            const response = await fetch('/school/api/demo/room/create-test', {
                method: 'POST'
            });
            
            const data = await response.json();
            
            if (data.success) {
                setRoomInfo(data.roomInfo);
                setGameInfo(data.gameInfo);
            } else {
                setError(data.error || '创建房间失败');
            }
        } catch (err) {
            setError('网络错误: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    // 获取房间信息
    const fetchRoomInfo = async (roomId) => {
        try {
            const response = await fetch(`/school/api/demo/room/${roomId}`);
            const data = await response.json();
            
            if (data.success) {
                setRoomInfo(data.roomInfo);
            }
        } catch (err) {
            console.error('获取房间信息失败:', err);
        }
    };

    // 获取游戏状态
    const fetchGameStatus = async (roomId) => {
        try {
            const response = await fetch(`/school/api/demo/room/${roomId}/game/status`);
            const data = await response.json();
            
            if (data.success) {
                setGameInfo(data.gameInfo);
            }
        } catch (err) {
            console.error('获取游戏状态失败:', err);
        }
    };

    // 处理玩家行动
    const handlePlayerAction = async (playerId, action, amount = null) => {
        if (!roomInfo) return;
        
        setLoading(true);
        setError(null);
        
        try {
            const body = new URLSearchParams({
                playerId: playerId.toString(),
                action: action
            });
            
            if (amount !== null) {
                body.append('amount', amount.toString());
            }
            
            const response = await fetch(`/school/api/demo/room/${roomInfo.roomId}/game/action`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: body
            });
            
            const data = await response.json();
            
            if (data.success) {
                setGameInfo(data.gameInfo);
                // 立即刷新游戏状态以确保底池信息是最新的
                setTimeout(() => {
                    fetchGameStatus(roomInfo.roomId);
                }, 100);
                // 清除该玩家的行动状态
                setPlayerActions(prev => {
                    const newActions = { ...prev };
                    delete newActions[playerId];
                    return newActions;
                });
            } else {
                setError(data.error || '行动失败');
            }
        } catch (err) {
            setError('网络错误: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    // 开始下一局
    const startNextHand = async () => {
        if (!roomInfo) return;
        
        setLoading(true);
        setError(null);
        
        try {
            const response = await fetch(`/school/api/demo/room/${roomInfo.roomId}/game/next-hand`, {
                method: 'POST'
            });
            
            const data = await response.json();
            
            if (data.success) {
                setGameInfo(data.gameInfo);
                setRoomInfo(data.roomInfo);
            } else {
                setError(data.error || '开始下一局失败');
            }
        } catch (err) {
            setError('网络错误: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    // 补码
    const rebuy = async (playerId, amount) => {
        if (!roomInfo) return;
        
        setLoading(true);
        setError(null);
        
        try {
            const response = await fetch(`/school/api/demo/room/${roomInfo.roomId}/rebuy`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `playerId=${playerId}&amount=${amount}`
            });
            
            const data = await response.json();
            
            if (data.success) {
                setRoomInfo(data.roomInfo);
            } else {
                setError(data.error || '补码失败');
            }
        } catch (err) {
            setError('网络错误: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    // 关闭房间
    const closeRoom = async () => {
        if (!roomInfo) return;
        
        setLoading(true);
        setError(null);
        
        try {
            const response = await fetch(`/school/api/demo/room/${roomInfo.roomId}/close`, {
                method: 'POST'
            });
            
            const data = await response.json();
            
            if (data.success) {
                setRoomInfo(null);
                setGameInfo(null);
            } else {
                setError(data.error || '关闭房间失败');
            }
        } catch (err) {
            setError('网络错误: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    // 定期刷新游戏状态
    useEffect(() => {
        if (roomInfo && roomInfo.gameInProgress) {
            const interval = setInterval(() => {
                fetchGameStatus(roomInfo.roomId);
            }, 5000); // 增加刷新间隔到5秒，减少与玩家行动的冲突
            
            return () => clearInterval(interval);
        }
    }, [roomInfo]);

    // 渲染玩家座位 - 与游戏演示完全一致
    const renderPlayer = (player, isCurrentPlayer, position) => {
        const isSeated = roomInfo && roomInfo.members.find(m => m.id === player.id)?.status === 'SEATED';
        
        return (
            <div 
                className={`player-seat ${isCurrentPlayer ? 'current-player' : ''} ${player.hasFolded ? 'folded' : ''} ${!isSeated ? 'not-seated' : ''}`}
                style={position}
            >
                <div className="player-info">
                    <div className="player-header">
                        <span className="player-name">{player.name}</span>
                        {(player.isAllIn || player.allIn) && <span className="all-in-badge">全下</span>}
                        {player.hasFolded && <span className="folded-badge">弃牌</span>}
                    </div>
                    
                    <div className="player-stats">
                        <div className="chips">筹码: {player.chips}</div>
                        <div className="bet-amount">下注: {player.betAmount}</div>
                        <div className="total-bet">总下注: {player.totalBet}</div>
                        {gameInfo.settlementResult && gameInfo.settlementResult.playerWinnings && gameInfo.settlementResult.playerWinnings[player.id] > 0 && (
                            <div className="winnings">获胜: {gameInfo.settlementResult.playerWinnings[player.id]}</div>
                        )}
                    </div>

                    <div className="hole-cards">
                        {player.holeCards && player.holeCards.map((card, cardIndex) => (
                            <div key={cardIndex} className="hole-card">
                                {renderCard(card)}
                            </div>
                        ))}
                    </div>

                    {isCurrentPlayer && !player.hasFolded && (player.isActive || player.active) && (
                        <div className="action-panel">
                            <div className="action-buttons">
                                {renderActionButtons(player)}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        );
    };

    // 处理行动点击
    const handleActionClick = (player, action) => {
        if (action === 'RAISE') {
            const amount = prompt('请输入加注金额:');
            if (amount && !isNaN(amount)) {
                handlePlayerAction(player.id, action, parseFloat(amount));
            }
        } else {
            handlePlayerAction(player.id, action);
        }
    };

    // 获取可用行动
    const getAvailableActions = (player) => {
        if (!gameInfo) return [];
        
        const actions = [];
        
        if (player.hasFolded) return actions;
        
        if (player.chips <= 0) return actions;
        
        if (player.betAmount < gameInfo.currentBet) {
            actions.push('CALL');
            actions.push('FOLD');
            if (player.chips > gameInfo.currentBet - player.betAmount) {
                actions.push('RAISE');
            }
            actions.push('ALL_IN');
        } else {
            actions.push('CHECK');
            actions.push('FOLD');
            actions.push('RAISE');
            actions.push('ALL_IN');
        }
        
        return actions;
    };

    // 获取行动文本
    const getActionText = (action) => {
        const actionTexts = {
            'FOLD': '弃牌',
            'CHECK': '过牌',
            'CALL': '跟注',
            'RAISE': '加注',
            'ALL_IN': '全下'
        };
        return actionTexts[action] || action;
    };

    // 计算玩家位置 - 与游戏演示保持一致
    const calculatePlayerPosition = (index, totalPlayers) => {
        const tableWidth = 1000;
        const tableHeight = 500;
        const centerX = tableWidth / 2;
        const centerY = tableHeight / 2;
        
        const a = tableWidth / 2 + 100;
        const b = tableHeight / 2 + 100;
        
        const angle = (index * 2 * Math.PI) / totalPlayers - Math.PI / 2;
        
        const x = centerX + a * Math.cos(angle);
        const y = centerY + b * Math.sin(angle);
        
        return {
            left: `${x - 120}px`,
            top: `${y - 80}px`
        };
    };

    // 渲染卡片 - 与游戏演示保持一致
    const renderCard = (card) => {
        if (!card) return null;
        
        // 处理房间API返回的卡片数据结构
        const isRed = card.red === true || card.color === '红色';
        const display = card.displayName || card.display;
        const chinese = card.chineseDisplayName || card.chinese;
        
        return (
            <div className={`card ${isRed ? 'red' : 'black'}`}>
                <div className="card-display">{display}</div>
                <div className="card-chinese">{chinese}</div>
            </div>
        );
    };

    // 渲染房间成员列表
    const renderRoomMembers = () => {
        if (!roomInfo) return null;
        
        return (
            <div className="room-members">
                <h3>房间成员</h3>
                <div className="members-list">
                    {roomInfo.members.map(member => (
                        <div key={member.id} className={`member-item ${member.status.toLowerCase()}`}>
                            <div className="member-name">{member.name}</div>
                            <div className="member-chips">筹码: {member.chips}</div>
                            {member.pendingChips > 0 && (
                                <div className="member-pending-chips">待生效: {member.pendingChips}</div>
                            )}
                            <div className="member-status">
                                {member.status === 'IN_ROOM' && '在房间'}
                                {member.status === 'SEATED' && '已入座'}
                                {member.status === 'LEFT_ROOM' && '已离开'}
                            </div>
                            <div className="member-actions">
                                <button 
                                    onClick={() => {
                                        const amount = prompt(`为${member.name}补码，请输入金额:`);
                                        if (amount && !isNaN(amount) && parseFloat(amount) > 0) {
                                            rebuy(member.id, parseFloat(amount));
                                        }
                                    }}
                                    className="rebuy-btn"
                                    disabled={loading}
                                >
                                    补码
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        );
    };

    // 渲染游戏历史
    const renderGameHistory = () => {
        return (
            <div className="game-history">
                <h3>游戏历史</h3>
                <div className="history-list">
                    {!roomInfo || !roomInfo.gameHistory || roomInfo.gameHistory.length === 0 ? (
                        <div className="no-history">
                            <p>暂无游戏历史</p>
                        </div>
                    ) : (
                        roomInfo.gameHistory.map((game, index) => (
                            <div key={index} className="history-item">
                                <div className="game-id">游戏: {game.gameId}</div>
                                <div className="game-winner">获胜者: {game.winner}</div>
                                <div className="game-pot">底池: {game.potAmount}</div>
                                <div className="game-time">
                                    {new Date(game.startTime).toLocaleTimeString()} - 
                                    {new Date(game.endTime).toLocaleTimeString()}
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
        );
    };

    // 渲染行动按钮 - 与游戏演示保持一致
    const renderActionButtons = (player) => {
        const actions = getAvailableActions(player);
        
        return actions.map((action, index) => {
            const actionInfo = {
                action: action,
                name: getActionText(action),
                amount: action === 'CALL' ? gameInfo.currentBet - player.betAmount : null
            };
            
            return (
                <button
                    key={index}
                    className={`action-btn ${action.toLowerCase()}`}
                    onClick={() => handleActionClick(player, action)}
                    disabled={loading}
                >
                    {actionInfo.name}
                    {actionInfo.amount && ` (${actionInfo.amount})`}
                </button>
            );
        });
    };

    return (
        <div className="room-demo">
            <div className="demo-header">
                <h2>德州扑克房间演示</h2>
                <div className="demo-controls">
                    {!roomInfo ? (
                        <button 
                            onClick={createTestRoom} 
                            disabled={loading}
                            className="create-game-btn"
                        >
                            {loading ? '创建中...' : '创建测试房间'}
                        </button>
                    ) : (
                        <div className="room-controls">
                            <span className="room-id">房间ID: {roomInfo.roomId}</span>
                            {gameInfo && gameInfo.state === 'FINISHED' && (
                                <button 
                                    onClick={startNextHand}
                                    disabled={loading}
                                    className="next-hand-btn"
                                >
                                    {loading ? '开始中...' : '继续下一局'}
                                </button>
                            )}
                            <button 
                                onClick={() => fetchRoomInfo(roomInfo.roomId)}
                                className="refresh-btn"
                            >
                                刷新房间信息
                            </button>
                            <button 
                                onClick={closeRoom}
                                className="btn btn-danger"
                            >
                                关闭房间
                            </button>
                        </div>
                    )}
                </div>
            </div>

            {error && (
                <div className="error-message">
                    {error}
                </div>
            )}

            {/* 游戏信息栏 - 独立定位在顶部 */}
            {gameInfo && (
                <div className="game-info-bar">
                    <h3>🎮 游戏状态: {gameInfo.state}</h3>
                    <div className="game-stats">
                        <div className="stat">
                            <span className="label">底池:</span>
                            <span className="value">{gameInfo.totalPot}</span>
                        </div>
                        <div className="stat">
                            <span className="label">当前下注:</span>
                            <span className="value">{gameInfo.currentBet}</span>
                        </div>
                        <div className="stat">
                            <span className="label">盲注:</span>
                            <span className="value">{gameInfo.smallBlind}/{gameInfo.bigBlind}</span>
                        </div>
                        <div className="stat">
                            <span className="label">按钮位:</span>
                            <span className="value">玩家{gameInfo.buttonPosition + 1}</span>
                        </div>
                        {gameInfo.currentPlayerIndex >= 0 && gameInfo.players[gameInfo.currentPlayerIndex] && (
                            <div className="stat current-turn">
                                <span className="label">当前玩家:</span>
                                <span className="value">{gameInfo.players[gameInfo.currentPlayerIndex].name}</span>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {gameInfo ? (
                <div className="game-state">

                    <div className="poker-table">
                        <div className="table-surface">
                            {/* 公共牌 */}
                            {gameInfo.communityCards && gameInfo.communityCards.length > 0 && (
                                <div className="community-cards">
                                    <h4>公共牌</h4>
                                    <div className="cards-row">
                                        {gameInfo.communityCards.map((card, index) => (
                                            <div key={index} className="community-card">
                                                {renderCard(card)}
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* 底池显示 */}
                            <div className="pot-display">
                                <div className="pot-amount">
                                    总底池: {gameInfo.totalPot}
                                </div>
                                {gameInfo.currentBet > 0 && (
                                    <div className="current-bet">
                                        当前下注: {gameInfo.currentBet}
                                    </div>
                                )}
                                
                                {/* 底池结构显示 */}
                                {gameInfo.potStructure && gameInfo.potStructure.pots && gameInfo.potStructure.pots.length > 0 && (
                                    <div className="pot-structure">
                                        <h4>底池结构</h4>
                                        {gameInfo.potStructure.pots.map((pot, index) => (
                                            <div key={index} className="pot-item">
                                                <span className="pot-name">{pot.description}:</span>
                                                <span className="pot-amount">{pot.amount}</span>
                                                <span className="pot-players">
                                                    ({pot.eligiblePlayerIds.length}人)
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                )}
                                
                                {/* 结算信息 */}
                                {gameInfo.settlementResult && (
                                    <div className="settlement-info">
                                        <div className="settlement-summary">
                                            🏆 {gameInfo.settlementResult.summary}
                                        </div>
                                        <div className="total-distributed">
                                            总分配: {gameInfo.settlementResult.totalDistributed}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* 玩家座位 */}
                        {gameInfo.players.map((player, index) => {
                            const isCurrentPlayer = gameInfo.currentPlayerIndex === index;
                            const position = calculatePlayerPosition(index, gameInfo.players.length);
                            
                            return (
                                <div key={player.id}>
                                    {renderPlayer(player, isCurrentPlayer, position)}
                                </div>
                            );
                        })}
                    </div>

                </div>
            ) : (
                <div className="no-game">
                    <p>没有进行中的游戏</p>
                    {roomInfo && (
                        <button 
                            onClick={() => {
                                // 这里可以添加开始新游戏的逻辑
                                console.log('开始新游戏');
                            }}
                            className="create-game-btn"
                        >
                            开始新游戏
                        </button>
                    )}
                </div>
            )}

            {/* 房间成员和游戏历史 - 放在下方 */}
            <div className="room-details">
                <div className="room-members-section">
                    {renderRoomMembers()}
                </div>
                <div className="game-history-section">
                    {renderGameHistory()}
                </div>
            </div>
        </div>
    );
};

export default RoomDemo;
