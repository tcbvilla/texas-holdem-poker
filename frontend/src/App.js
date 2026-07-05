import React, { useState, useEffect } from 'react';
import './App.css';
import Auth from './components/Auth';
import ClubManagement from './components/ClubManagement';
import RoomManagement from './components/RoomManagement';
import PokerTable from './components/PokerTable';
import { clearToken, getStoredUser } from './api';

function App() {
  const [currentView, setCurrentView] = useState('home');
  const [user, setUser] = useState(null);
  const [selectedClubId, setSelectedClubId] = useState(null);
  const [selectedRoomId, setSelectedRoomId] = useState(null);

  useEffect(() => {
    const savedUser = getStoredUser();
    if (savedUser) {
      setUser(savedUser);
    }
  }, []);

  const handleAuthSuccess = (loggedInUser) => {
    setUser(loggedInUser);
    setCurrentView('home');
  };

  const handleLogout = () => {
    clearToken();
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
    switch (currentView) {
      case 'auth':
        return <Auth onAuthSuccess={handleAuthSuccess} />;
      case 'clubs':
        return <ClubManagement onEnterClub={handleEnterClub} />;
      case 'rooms':
        return <RoomManagement clubId={selectedClubId} onEnterRoom={handleEnterRoom} />;
      case 'game':
        return (
          <PokerTable
            roomId={selectedRoomId}
            user={user}
            onBack={() => setCurrentView('rooms')}
          />
        );
      default:
        return (
          <div className="home-view">
            <h1>德州扑克系统</h1>

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
                    俱乐部管理
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
            {selectedClubId && (
              <button
                className={currentView === 'rooms' ? 'active' : ''}
                onClick={() => setCurrentView('rooms')}
              >
                房间管理
              </button>
            )}
            <span className="nav-user">{user.username}</span>
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
