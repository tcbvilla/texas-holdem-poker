import React, { useState } from 'react';
import './App.css';
import DeckDemo from './components/DeckDemo';

function App() {
  const [currentView, setCurrentView] = useState('home');

  const renderView = () => {
    switch(currentView) {
      case 'deck-demo':
        return <DeckDemo />;
      default:
        return (
          <div className="home-view">
            <h1>🃏 德州扑克系统</h1>
            <p>第一阶段：核心发牌功能已完成</p>
            
            <div className="feature-grid">
              <div className="feature-card">
                <h3>🎲 公平发牌</h3>
                <p>Fisher-Yates洗牌算法 + SecureRandom</p>
                <button 
                  className="demo-btn"
                  onClick={() => setCurrentView('deck-demo')}
                >
                  体验发牌演示
                </button>
              </div>
              
              <div className="feature-card">
                <h3>🔒 安全保证</h3>
                <p>加密级随机数 + 种子验证</p>
                <span className="status">已实现</span>
              </div>
              
              <div className="feature-card">
                <h3>📊 统计验证</h3>
                <p>随机性测试 + 可重现性验证</p>
                <span className="status">已实现</span>
              </div>
              
              <div className="feature-card coming-soon">
                <h3>🎮 游戏逻辑</h3>
                <p>牌型识别 + 结算系统</p>
                <span className="status">开发中</span>
              </div>
            </div>
            
            <div className="tech-info">
              <h3>技术实现</h3>
              <ul>
                <li>✅ Spring Boot 3.2 + Lombok注解</li>
                <li>✅ 52张标准扑克牌库</li>
                <li>✅ Fisher-Yates洗牌算法</li>
                <li>✅ 德州扑克发牌规则（烧牌机制）</li>
                <li>✅ 完整的单元测试覆盖</li>
                <li>⏳ 牌型识别算法（下一阶段）</li>
              </ul>
            </div>
          </div>
        );
    }
  };

  return (
    <div className="App">
      <nav className="app-nav">
        <button 
          className={currentView === 'home' ? 'active' : ''}
          onClick={() => setCurrentView('home')}
        >
          首页
        </button>
        <button 
          className={currentView === 'deck-demo' ? 'active' : ''}
          onClick={() => setCurrentView('deck-demo')}
        >
          发牌演示
        </button>
      </nav>
      
      <main className="app-main">
        {renderView()}
      </main>
    </div>
  );
}

export default App;
