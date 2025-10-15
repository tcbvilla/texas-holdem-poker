import React, { useState, useEffect } from 'react';
import './App.css';
import DeckDemo from './components/DeckDemo';
import GameDemo from './components/GameDemo';
import RoomDemo from './components/RoomDemo';
import Auth from './components/Auth';
import ClubManagement from './components/ClubManagement';
import RoomManagement from './components/RoomManagement';

function App() {
  const [currentView, setCurrentView] = useState('home');
  const [user, setUser] = useState(null);
  const [selectedClubId, setSelectedClubId] = useState(null);
  const [selectedRoomId, setSelectedRoomId] = useState(null);

  useEffect(() => {
    // 检查用户是否已登录
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      setUser(JSON.parse(savedUser));
    }
    
    // 检查是否有选中的俱乐部ID
    if (window.selectedClubId) {
      setSelectedClubId(window.selectedClubId);
      setCurrentView('rooms');
      // 清除全局变量
      window.selectedClubId = null;
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('user');
    setUser(null);
    setCurrentView('home');
  };

  const handleEnterClub = (clubId) => {
    setSelectedClubId(clubId);
    setCurrentView('rooms');
  };

  const handleEnterRoom = (roomId) => {
    setSelectedRoomId(roomId);
    setCurrentView('game');
  };

  const renderView = () => {
    switch(currentView) {
      case 'auth':
        return <Auth />;
      case 'clubs':
        return <ClubManagement onEnterClub={handleEnterClub} />;
      case 'rooms':
        return <RoomManagement clubId={selectedClubId} onEnterRoom={handleEnterRoom} />;
      case 'game':
        return <GameDemo roomId={selectedRoomId} />;
      case 'deck-demo':
        return <DeckDemo />;
      case 'game-demo':
        return <GameDemo />;
      case 'room-demo':
        return <RoomDemo />;
      default:
        return (
          <div className="home-view">
            <h1>🃏 德州扑克系统</h1>
            
            {!user ? (
              <div className="auth-section">
                <h2>欢迎使用德州扑克系统</h2>
                <p>请先登录或注册账号开始游戏</p>
                <div className="auth-buttons">
                  <button 
                    className="auth-btn"
                    onClick={() => setCurrentView('auth')}
                  >
                    登录/注册
                  </button>
                </div>
              </div>
            ) : (
              <div className="user-section">
                <div className="user-info">
                  <h2>欢迎回来，{user.username}！</h2>
                  <p>邮箱：{user.email}</p>
                </div>
                <div className="user-actions">
                  <button 
                    className="action-btn"
                    onClick={() => setCurrentView('clubs')}
                  >
                    🏛️ 俱乐部管理
                  </button>
                  <button 
                    className="action-btn"
                    onClick={handleLogout}
                  >
                    退出登录
                  </button>
                </div>
              </div>
            )}
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
        {user && (
          <>
            <button 
              className={currentView === 'clubs' ? 'active' : ''}
              onClick={() => setCurrentView('clubs')}
            >
              俱乐部
            </button>
            <button 
              className={currentView === 'rooms' ? 'active' : ''}
              onClick={() => setCurrentView('rooms')}
            >
              房间管理
            </button>
          </>
        )}
      </nav>
      
      <main className="app-main">
        {renderView()}
      </main>
    </div>
  );
}

export default App;
