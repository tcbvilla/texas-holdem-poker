import React, { useState, useEffect } from 'react';
import './GameDemo.css';

const GameDemo = () => {
  const [gameState, setGameState] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedAction, setSelectedAction] = useState(null);
  const [raiseAmount, setRaiseAmount] = useState('');
  const [playerActions, setPlayerActions] = useState({});
  const [gameHistory, setGameHistory] = useState([]);

  // 获取当前玩家的可用行动
  useEffect(() => {
    if (gameState && gameState.currentPlayerIndex >= 0) {
      const currentPlayer = gameState.players[gameState.currentPlayerIndex];
      if (currentPlayer && !currentPlayer.hasFolded && !currentPlayer.isAllIn) {
        getPlayerActions(currentPlayer.id).then(actions => {
          setPlayerActions(prev => ({
            ...prev,
            [currentPlayer.id]: actions
          }));
        });
      }
    }
  }, [gameState?.currentPlayerIndex, gameState?.gameState]);

  // 创建新游戏
  const createGame = async (playerCount = 4, smallBlind = 5, bigBlind = 10, initialChips = 500) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`/school/api/demo/game/create?playerCount=${playerCount}&smallBlind=${smallBlind}&bigBlind=${bigBlind}&initialChips=${initialChips}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });
      
      const data = await response.json();
      
      if (data.success) {
        setGameState(data);
      } else {
        setError(data.error || '创建游戏失败');
      }
    } catch (err) {
      setError('网络错误：' + err.message);
    } finally {
      setLoading(false);
    }
  };

  // 获取游戏状态
  const refreshGameState = async () => {
    try {
      const response = await fetch('/school/api/demo/game/status');
      const data = await response.json();
      
      if (data.success) {
        setGameState(data);
      }
    } catch (err) {
      console.error('获取游戏状态失败:', err);
    }
  };

  // 执行玩家行动
  const executeAction = async (playerId, action, amount = null) => {
    setLoading(true);
    setError(null);
    
    try {
      let url = `/school/api/demo/game/action?playerId=${playerId}&action=${action}`;
      if (amount !== null) {
        url += `&amount=${amount}`;
      }
      
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });
      
      const data = await response.json();
      
      if (data.success) {
        setGameState(data);
        setSelectedAction(null);
        setRaiseAmount('');
      } else {
        setError(data.error || '行动执行失败');
      }
    } catch (err) {
      setError('网络错误：' + err.message);
    } finally {
      setLoading(false);
    }
  };

  // 开始下一局
  const startNextHand = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch('/school/api/demo/game/next-hand', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });
      
      const data = await response.json();
      
      if (data.success) {
        // 保存上一局的结果到历史记录
        if (gameState && gameState.settlementResult) {
          const handResult = {
            handNumber: gameHistory.length + 1,
            summary: gameState.settlementResult.summary,
            totalPot: gameState.settlementResult.totalDistributed,
            winners: Object.entries(gameState.settlementResult.playerWinnings)
              .filter(([_, winnings]) => winnings > 0)
              .map(([playerId, winnings]) => ({
                playerId: parseInt(playerId),
                playerName: gameState.players.find(p => p.id === parseInt(playerId))?.name || `玩家${playerId}`,
                winnings: winnings
              }))
          };
          setGameHistory(prev => [...prev, handResult]);
        }
        
        setGameState(data);
        setPlayerActions({}); // 清空玩家行动缓存
      } else {
        setError(data.error || '开始下一局失败');
      }
    } catch (err) {
      setError('网络错误：' + err.message);
    } finally {
      setLoading(false);
    }
  };

  // 获取玩家可用行动
  const getPlayerActions = async (playerId) => {
    try {
      const response = await fetch(`/school/api/demo/game/actions/${playerId}`);
      const data = await response.json();
      return data.success ? data.actions : [];
    } catch (err) {
      console.error('获取玩家行动失败:', err);
      return [];
    }
  };

  // 渲染卡牌
  const renderCard = (card) => {
    if (!card) return null;
    
    return (
      <div className={`card ${card.color === '红色' ? 'red' : 'black'}`}>
        <div className="card-display">{card.display}</div>
        <div className="card-chinese">{card.chinese}</div>
      </div>
    );
  };

  // 渲染玩家
  const renderPlayer = (player, isCurrentPlayer, position, playerActions) => {

    const handleActionClick = (action) => {
      if (action.action === 'RAISE') {
        setSelectedAction(action);
      } else {
        executeAction(player.id, action.action, action.amount);
      }
    };

    const handleRaiseSubmit = () => {
      const amount = parseFloat(raiseAmount);
      if (amount && selectedAction) {
        executeAction(player.id, selectedAction.action, amount);
      }
    };

    return (
      <div 
        className={`player-seat ${isCurrentPlayer ? 'current-player' : ''} ${player.hasFolded ? 'folded' : ''}`}
        style={position}
      >
        <div className="player-info">
          <div className="player-header">
            <span className="player-name">{player.name}</span>
            {player.isAllIn && <span className="all-in-badge">全下</span>}
            {player.hasFolded && <span className="folded-badge">弃牌</span>}
          </div>
          
          <div className="player-stats">
            <div className="chips">筹码: {player.chips}</div>
            <div className="bet-amount">下注: {player.betAmount}</div>
            <div className="total-bet">总下注: {player.totalBet}</div>
            {gameState.settlementResult && gameState.settlementResult.playerWinnings[player.id] > 0 && (
              <div className="winnings">获胜: {gameState.settlementResult.playerWinnings[player.id]}</div>
            )}
          </div>

          <div className="hole-cards">
            {player.holeCards.map((card, index) => (
              <div key={index} className="hole-card">
                {renderCard(card)}
              </div>
            ))}
          </div>

          {isCurrentPlayer && !player.hasFolded && !player.isAllIn && (
            <div className="player-actions">
              <h4>可用行动:</h4>
              <div className="action-buttons">
                {playerActions.map((action, index) => (
                  <button
                    key={index}
                    className={`action-btn ${action.action.toLowerCase()}`}
                    onClick={() => handleActionClick(action)}
                    disabled={loading}
                  >
                    {action.name}
                    {action.amount && ` (${action.amount})`}
                  </button>
                ))}
              </div>

              {selectedAction && selectedAction.action === 'RAISE' && (
                <div className="raise-input">
                  <h5>加注金额:</h5>
                  <input
                    type="number"
                    value={raiseAmount}
                    onChange={(e) => setRaiseAmount(e.target.value)}
                    min={selectedAction.minAmount}
                    max={selectedAction.maxAmount}
                    placeholder={`${selectedAction.minAmount} - ${selectedAction.maxAmount}`}
                  />
                  <div className="raise-buttons">
                    <button onClick={handleRaiseSubmit} disabled={!raiseAmount}>
                      确认加注
                    </button>
                    <button onClick={() => setSelectedAction(null)}>
                      取消
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    );
  };

  // 计算玩家位置
  const calculatePlayerPosition = (index, totalPlayers) => {
    const tableWidth = 1000;
    const tableHeight = 600;
    const centerX = tableWidth / 2;
    const centerY = tableHeight / 2;
    
    const a = tableWidth / 2 + 80;
    const b = tableHeight / 2 + 80;
    
    const angle = (index * 2 * Math.PI) / totalPlayers - Math.PI / 2;
    
    const x = centerX + a * Math.cos(angle);
    const y = centerY + b * Math.sin(angle);
    
    return {
      left: `${x - 130}px`,
      top: `${y - 90}px`
    };
  };

  // 渲染游戏状态
  const renderGameState = () => {
    if (!gameState) return null;

    const currentPlayer = gameState.players[gameState.currentPlayerIndex];

    return (
      <div className="game-state">
        <div className="game-info">
          <h3>🎮 游戏状态: {gameState.gameStateName || gameState.gameState}</h3>
          <div className="game-stats">
            <div className="stat">
              <span className="label">底池:</span>
              <span className="value">{gameState.totalPot}</span>
            </div>
            <div className="stat">
              <span className="label">当前下注:</span>
              <span className="value">{gameState.currentBet}</span>
            </div>
            <div className="stat">
              <span className="label">盲注:</span>
              <span className="value">{gameState.smallBlind}/{gameState.bigBlind}</span>
            </div>
            <div className="stat">
              <span className="label">按钮位:</span>
              <span className="value">玩家{gameState.buttonPosition + 1}</span>
            </div>
            {currentPlayer && (
              <div className="stat current-turn">
                <span className="label">当前玩家:</span>
                <span className="value">{currentPlayer.name}</span>
              </div>
            )}
          </div>
        </div>

        <div className="poker-table">
          <div className="table-surface">
            {/* 公共牌 */}
            {gameState.communityCards && gameState.communityCards.length > 0 && (
              <div className="community-cards">
                <h4>公共牌</h4>
                <div className="cards-row">
                  {gameState.communityCards.map((card, index) => (
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
                底池: {gameState.totalPot}
              </div>
              {gameState.currentBet > 0 && (
                <div className="current-bet">
                  当前下注: {gameState.currentBet}
                </div>
              )}
              
              {/* 结算信息 */}
              {gameState.settlementResult && (
                <div className="settlement-info">
                  <div className="settlement-summary">
                    🏆 {gameState.settlementResult.summary}
                  </div>
                  <div className="total-distributed">
                    总分配: {gameState.settlementResult.totalDistributed}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* 玩家座位 */}
          {gameState.players.map((player, index) => {
            const position = calculatePlayerPosition(index, gameState.players.length);
            const isCurrentPlayer = index === gameState.currentPlayerIndex;
            const currentPlayerActions = playerActions[player.id] || [];
            
            return (
              <div key={player.id}>
                {renderPlayer(player, isCurrentPlayer, position, currentPlayerActions)}
              </div>
            );
          })}
        </div>
      </div>
    );
  };

  return (
    <div className="game-demo">
      <div className="demo-header">
        <h2>🃏 德州扑克游戏演示</h2>
        <div className="demo-controls">
          <button 
            onClick={() => createGame(4, 5, 10, 500)} 
            disabled={loading}
            className="create-game-btn"
          >
            {loading ? '创建中...' : '创建新游戏 (4人桌)'}
          </button>
          
          <button 
            onClick={() => createGame(6, 10, 20, 1000)} 
            disabled={loading}
            className="create-game-btn"
          >
            {loading ? '创建中...' : '创建新游戏 (6人桌)'}
          </button>

          {gameState && (
            <>
              <button 
                onClick={refreshGameState} 
                disabled={loading}
                className="refresh-btn"
              >
                刷新状态
              </button>
              
              {gameState.gameState === 'FINISHED' && (
                <button 
                  onClick={startNextHand} 
                  disabled={loading}
                  className="next-hand-btn"
                >
                  {loading ? '开始中...' : '开始下一局'}
                </button>
              )}
            </>
          )}
        </div>
      </div>

      {error && (
        <div className="error-message">
          <h3>❌ 错误</h3>
          <p>{error}</p>
        </div>
      )}

      {loading && (
        <div className="loading-message">
          <h3>⏳ 处理中...</h3>
        </div>
      )}

      {renderGameState()}

      {/* 游戏历史记录 */}
      {gameHistory.length > 0 && (
        <div className="game-history">
          <h3>🏆 游戏历史</h3>
          <div className="history-list">
            {gameHistory.map((hand, index) => (
              <div key={index} className="history-item">
                <div className="hand-number">第{hand.handNumber}局</div>
                <div className="hand-summary">{hand.summary}</div>
                <div className="hand-pot">底池: {hand.totalPot}</div>
                <div className="hand-winners">
                  {hand.winners.map((winner, idx) => (
                    <span key={idx} className="winner-info">
                      {winner.playerName}: +{winner.winnings}
                      {idx < hand.winners.length - 1 && ', '}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default GameDemo;
