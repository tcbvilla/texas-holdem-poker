import React, { useState } from 'react';
import './DeckDemo.css';

const DeckDemo = () => {
  const [demoResult, setDemoResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [playerCount, setPlayerCount] = useState(6);
  const [customSeed, setCustomSeed] = useState('');
  const [activeTab, setActiveTab] = useState('holdem');
  const [showRankingModal, setShowRankingModal] = useState(false);

  const runTexasHoldemDemo = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        playerCount: playerCount.toString()
      });
      
      if (customSeed.trim()) {
        params.append('customSeed', customSeed.trim());
      }

      const response = await fetch(`/school/api/demo/deck/texas-holdem-demo?${params}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        }
      });
      
      const result = await response.json();
      setDemoResult(result);
    } catch (error) {
      console.error('演示请求失败:', error);
      setDemoResult({
        success: false,
        error: '请求失败，请确保后端服务正在运行'
      });
    } finally {
      setLoading(false);
    }
  };

  const runRandomnessTest = async () => {
    setLoading(true);
    try {
      const response = await fetch('/school/api/demo/deck/shuffle-randomness-test?iterations=1000', {
        method: 'POST'
      });
      
      const result = await response.json();
      setDemoResult(result);
    } catch (error) {
      console.error('随机性测试失败:', error);
      setDemoResult({
        success: false,
        error: '测试失败，请确保后端服务正在运行'
      });
    } finally {
      setLoading(false);
    }
  };


  const renderCard = (cardInfo) => {
    if (!cardInfo) return null;
    
    return (
      <div className={`card ${cardInfo.color === '红色' ? 'red' : 'black'}`}>
        <span className="card-display">{cardInfo.display}</span>
        <span className="card-chinese">{cardInfo.chinese}</span>
      </div>
    );
  };

  const renderHoleCards = (holeCards) => {
    // 按照椭圆桌逆时针顺序排列玩家
    const getEllipticalOrder = (playerCount) => {
      // 椭圆桌逆时针顺序：按钮位开始，逆时针排列
      const orders = {
        2: [1, 2], // 小盲位、大盲位
        3: [3, 1, 2], // 按钮位、小盲位、大盲位
        4: [4, 1, 2, 3], // 按钮位、小盲位、大盲位、枪口位
        5: [5, 1, 2, 3, 4], // 按钮位、小盲位、大盲位、枪口位、关煞位
        6: [6, 1, 2, 3, 4, 5], // 按钮位、小盲位、大盲位、枪口位、中位、关煞位
        7: [7, 1, 2, 3, 4, 5, 6], // 按钮位、小盲位、大盲位、枪口位、中位、中位、关煞位
        8: [8, 1, 2, 3, 4, 5, 6, 7], // 按钮位、小盲位、大盲位、枪口位、中位、中位、中位、关煞位
        9: [9, 1, 2, 3, 4, 5, 6, 7, 8] // 按钮位、小盲位、大盲位、枪口位、中位、中位、中位、中位、关煞位
      };
      return orders[playerCount] || Object.keys(holeCards).map(k => parseInt(k.replace('玩家', '')));
    };

    // 计算椭圆桌上的座位位置
    const calculateSeatPosition = (index, totalPlayers) => {
      const tableWidth = 900; // 椭圆桌宽度
      const tableHeight = 400; // 椭圆桌高度
      const centerX = tableWidth / 2;
      const centerY = tableHeight / 2;
      
      // 椭圆参数（桌子外侧）
      const a = tableWidth / 2 + 20; // 椭圆长半轴（桌子外侧）
      const b = tableHeight / 2 + 20; // 椭圆短半轴（桌子外侧）
      
      // 计算角度（从按钮位开始，逆时针）
      const angle = (index * 2 * Math.PI) / totalPlayers - Math.PI / 2; // 从顶部开始
      
      // 计算椭圆上的坐标
      const x = centerX + a * Math.cos(angle);
      const y = centerY + b * Math.sin(angle);
      
      return {
        left: `${x - 70}px`, // 70px是座位宽度的一半
        top: `${y - 50}px`   // 50px是座位高度的一半
      };
    };

    const playerCount = Object.keys(holeCards).length;
    const ellipticalOrder = getEllipticalOrder(playerCount);
    
    return ellipticalOrder.map((playerNum, index) => {
      const player = `玩家${playerNum}`;
      const playerData = holeCards[player];
      if (!playerData) return null;
      
      // 处理新的数据结构：playerData包含cards和seatInfo
      const cards = playerData.cards || playerData; // 兼容旧格式
      const seatInfo = playerData.seatInfo || { name: '未知位置', position: 'Unknown', emoji: '👤' };
      
      // 计算座位位置
      const position = calculateSeatPosition(index, playerCount);
      
      // 获取座位类型
      const getSeatType = (seatInfo) => {
        if (seatInfo.position === 'Button') return 'button';
        if (seatInfo.position === 'Small Blind') return 'small-blind';
        if (seatInfo.position === 'Big Blind') return 'big-blind';
        return 'normal';
      };

      return (
        <div 
          key={player} 
          className="player-hole-cards elliptical-seat"
          data-seat={getSeatType(seatInfo)}
          style={{
            left: position.left,
            top: position.top
          }}
        >
          <div className="player-header">
            <span className="player-emoji">{seatInfo.emoji}</span>
            <div className="player-info">
              <h4>{player}</h4>
              <span className="seat-name">({seatInfo.position})</span>
            </div>
          </div>
          <div className="cards-row">
            {cards.map((card, index) => (
              <div key={index}>
                {renderCard(card)}
              </div>
            ))}
          </div>
        </div>
      );
    });
  };

  const renderTexasHoldemResult = () => {
    if (!demoResult || !demoResult.success) return null;

    return (
      <div className="demo-result">
        <div className="result-section">
          <h3>🃏 德州扑克桌</h3>
          <div className="poker-table-container">
            <div className="hole-cards-grid elliptical-table">
              {renderHoleCards(demoResult.holeCards)}
              
              {/* 公共牌展示在桌子中心 */}
              <div className="community-cards-center">
                <div className="community-cards-horizontal">
                  <div className="flop-section">
                    <h4>翻牌</h4>
                    <div className="cards-row">
                      {demoResult.flop.map((card, index) => (
                        <div key={index}>{renderCard(card)}</div>
                      ))}
                    </div>
                  </div>
                  
                  <div className="turn-section">
                    <h4>转牌</h4>
                    <div className="cards-row">
                      {renderCard(demoResult.turn)}
                    </div>
                  </div>
                  
                  <div className="river-section">
                    <h4>河牌</h4>
                    <div className="cards-row">
                      {renderCard(demoResult.river)}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          {/* 排名按钮 */}
          {demoResult.ranking && (
            <div className="ranking-button-container">
              <button 
                className="ranking-button"
                onClick={() => setShowRankingModal(true)}
              >
                🏆 查看游戏排名
              </button>
            </div>
          )}
        </div>

        <div className="result-section">
          <h3>🎲 洗牌信息</h3>
          <div className="shuffle-info">
            <p><strong>种子:</strong> {demoResult.shuffleSeed}</p>
            <p><strong>时间戳:</strong> {new Date(demoResult.shuffleTimestamp).toLocaleString()}</p>
          </div>
        </div>

        <div className="result-section">
          <h3>📊 统计信息</h3>
          <div className="stats-grid">
            <div className="stat-item">
              <span className="stat-label">剩余牌数:</span>
              <span className="stat-value">{demoResult.deckStatus.remainingCards}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">已发牌数:</span>
              <span className="stat-value">{demoResult.deckStatus.dealtCards}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">烧牌数:</span>
              <span className="stat-value">{demoResult.deckStatus.burnedCards}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">总使用:</span>
              <span className="stat-value">{demoResult.statistics.totalCardsUsed}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">预期烧牌:</span>
              <span className="stat-value">{demoResult.statistics.expectedBurnedCards}</span>
            </div>
          </div>
          
          {demoResult.statistics.dealingPhases && (
            <div className="dealing-phases">
              <h4>🔥 烧牌阶段</h4>
              <div className="phases-list">
                {demoResult.statistics.dealingPhases.map((phase, index) => (
                  <div key={index} className="phase-item">
                    <span className="phase-number">{index + 1}</span>
                    <span className="phase-name">{phase}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    );
  };

  const renderRandomnessResult = () => {
    if (!demoResult || demoResult.iterations === undefined) return null;

    return (
      <div className="demo-result">
        <div className="result-section">
          <h3>📈 随机性测试结果</h3>
          <div className="stats-grid">
            <div className="stat-item">
              <span className="stat-label">测试次数:</span>
              <span className="stat-value">{demoResult.iterations}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">期望频次:</span>
              <span className="stat-value">{demoResult.expectedCountPerCard.toFixed(2)}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">最大偏差:</span>
              <span className="stat-value">{demoResult.maxDeviation}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">随机性质量:</span>
              <span className={`stat-value ${demoResult.randomnessQuality === '良好' ? 'good' : 'warning'}`}>
                {demoResult.randomnessQuality}
              </span>
            </div>
          </div>
          
          <div className="most-frequent">
            <p><strong>出现最多的牌:</strong> {demoResult.mostFrequentCard} ({demoResult.maxCount}次)</p>
          </div>
        </div>
      </div>
    );
  };


  return (
    <div className="deck-demo">
      <h2>🃏 发牌系统演示</h2>
      
      <div className="demo-tabs">
        <button 
          className={activeTab === 'holdem' ? 'active' : ''}
          onClick={() => setActiveTab('holdem')}
        >
          德州扑克演示
        </button>
        <button 
          className={activeTab === 'randomness' ? 'active' : ''}
          onClick={() => setActiveTab('randomness')}
        >
          随机性测试
        </button>
      </div>

      <div className="demo-controls">
        {activeTab === 'holdem' && (
          <>
            <div className="control-group">
              <label>玩家数量 (2-9):</label>
              <select 
                value={playerCount}
                onChange={(e) => setPlayerCount(parseInt(e.target.value))}
                className="player-count-select"
              >
                <option value={2}>2人桌</option>
                <option value={3}>3人桌</option>
                <option value={4}>4人桌</option>
                <option value={5}>5人桌</option>
                <option value={6}>6人桌</option>
                <option value={7}>7人桌</option>
                <option value={8}>8人桌</option>
                <option value={9}>9人桌</option>
              </select>
            </div>
            
            <div className="control-group">
              <label>自定义种子 (可选):</label>
              <input 
                type="text" 
                placeholder="留空使用随机种子"
                value={customSeed}
                onChange={(e) => setCustomSeed(e.target.value)}
              />
            </div>
            
            <button 
              className="demo-button primary"
              onClick={runTexasHoldemDemo}
              disabled={loading}
            >
              {loading ? '发牌中...' : '洗牌'}
            </button>
          </>
        )}

        {activeTab === 'randomness' && (
          <button 
            className="demo-button primary"
            onClick={runRandomnessTest}
            disabled={loading}
          >
            {loading ? '测试中...' : '运行随机性测试 (1000次)'}
          </button>
        )}

      </div>

      {demoResult && !demoResult.success && (
        <div className="error-message">
          <h3>❌ 错误</h3>
          <p>{demoResult.error}</p>
        </div>
      )}

      {demoResult && demoResult.success && activeTab === 'holdem' && renderTexasHoldemResult()}
      {demoResult && activeTab === 'randomness' && renderRandomnessResult()}

      {/* 排名弹窗 */}
      {showRankingModal && demoResult && demoResult.ranking && (
        <div className="modal-overlay" onClick={() => setShowRankingModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>🏆 游戏排名</h3>
              <button 
                className="modal-close"
                onClick={() => setShowRankingModal(false)}
              >
                ×
              </button>
            </div>
            
            <div className="modal-body">
              <div className="ranking-summary">
                <p className="summary-text">{demoResult.ranking.summary}</p>
              </div>
              
              {/* 公共牌固定显示 */}
              <div className="community-cards-fixed">
                <div className="community-cards-row">
                  {demoResult.flop.map((card, index) => (
                    <div key={`flop-${index}`} className="community-card">
                      {renderCard(card)}
                    </div>
                  ))}
                  <div key="turn" className="community-card">
                    {renderCard(demoResult.turn)}
                  </div>
                  <div key="river" className="community-card">
                    {renderCard(demoResult.river)}
                  </div>
                </div>
              </div>
              
              <div className="player-rankings">
                {demoResult.ranking.playerRankings.map((player, index) => (
                  <div 
                    key={player.playerId} 
                    className={`ranking-item ${player.isWinner ? 'winner' : ''}`}
                  >
                    <div className="ranking-header">
                      <span className="rank-number">#{player.rank}</span>
                      <span className="player-name">{player.playerName}</span>
                      {player.isWinner && <span className="winner-badge">🏆</span>}
                    </div>
                    
                    <div className="hand-info">
                      <div className="best-five-cards">
                        <div className="best-cards-row">
                          {player.bestFiveCards.split(' ').map((cardStr, cardIndex) => {
                            // 解析卡牌字符串，如 "A♠" -> {rank: "A", suit: "♠"}
                            const match = cardStr.match(/^([2-9]|10|J|Q|K|A)([♠♥♣♦])$/);
                            if (match) {
                              const [, rank, suit] = match;
                              const card = {
                                rank: rank,
                                suit: suit,
                                display: cardStr,
                                color: (suit === '♥' || suit === '♦') ? '红色' : '黑色'
                              };
                              return (
                                <div key={cardIndex} className="best-card">
                                  {renderCard(card)}
                                </div>
                              );
                            }
                            return null;
                          })}
                        </div>
                      </div>
                    </div>
                    
                    <div className="hole-cards-display">
                      <div className="cards-row">
                        {player.holeCards.map((card, cardIndex) => (
                          <div key={cardIndex} className="small-card">
                            {renderCard(card)}
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DeckDemo;
