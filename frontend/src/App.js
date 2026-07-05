import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link, useNavigate, useLocation, useParams } from 'react-router-dom';
import './App.css';
import Auth from './components/Auth';
import InviteCodeManagement from './components/InviteCodeManagement';
import ClubManagement from './components/ClubManagement';
import RoomManagement from './components/RoomManagement';
import PokerTable from './components/PokerTable';
import { apiGet, clearToken, getStoredUser, getToken } from './api';
import {
  PUBLIC_APP_NAME,
  PUBLIC_TAGLINE,
  INTERNAL_APP_NAME,
  setPublicTitle,
  setInternalTitle,
} from './branding';

function isAdminUser(user) {
  if (!user) return false;
  return Boolean(user.admin || String(user.username || '').toLowerCase() === 'admin');
}

function isPlaceholderEmail(email) {
  return Boolean(email && (email.endsWith('@noreply.local') || email.endsWith('@system.local')));
}

function PrivateRoute({ user, children }) {
  if (!user || !getToken()) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

function PlayerRoute({ user, children }) {
  if (!user || !getToken()) {
    return <Navigate to="/login" replace />;
  }
  if (isAdminUser(user)) {
    return <Navigate to="/app/admin/invites" replace />;
  }
  return children;
}

function AdminRoute({ user, children }) {
  if (!user || !getToken()) {
    return <Navigate to="/login" replace />;
  }
  if (!isAdminUser(user)) {
    return <Navigate to="/app" replace />;
  }
  return children;
}

function GuestRoute({ user, children }) {
  if (user && getToken()) {
    return <Navigate to={isAdminUser(user) ? '/app/admin/invites' : '/app'} replace />;
  }
  return children;
}

function PublicLanding() {
  const navigate = useNavigate();

  useEffect(() => {
    setPublicTitle();
  }, []);

  return (
    <div className="public-landing">
      <h1>{PUBLIC_APP_NAME}</h1>
      <p className="public-tagline">{PUBLIC_TAGLINE}</p>
      <p className="public-desc">登录后可使用词汇本、学习记录与笔记同步功能</p>
      <button className="public-enter-btn" onClick={() => navigate('/login')}>
        进入系统
      </button>
    </div>
  );
}

function AppHome({ user, onLogout }) {
  const navigate = useNavigate();

  useEffect(() => {
    setInternalTitle('首页');
  }, []);

  return (
    <div className="home-view">
      <h1>{INTERNAL_APP_NAME}</h1>
      <div className="user-section">
        <div className="user-info">
          <h2>欢迎回来，{user.username}！</h2>
          {!isPlaceholderEmail(user.email) && <p>邮箱：{user.email}</p>}
        </div>
        <div className="user-actions">
          <button className="action-btn" onClick={() => navigate('/app/clubs')}>
            俱乐部管理
          </button>
          <button className="action-btn" onClick={onLogout}>
            退出登录
          </button>
        </div>
      </div>
    </div>
  );
}

function AppEntry({ user, onLogout }) {
  if (isAdminUser(user)) {
    return <Navigate to="/app/admin/invites" replace />;
  }
  return <AppHome user={user} onLogout={onLogout} />;
}

function GameTableRoute({ user }) {
  const { clubId, roomId } = useParams();
  const navigate = useNavigate();

  useEffect(() => {
    setInternalTitle('牌桌');
  }, []);

  return (
    <PokerTable
      roomId={Number(roomId)}
      user={user}
      onBack={() => navigate(`/app/clubs/${clubId}/rooms`)}
    />
  );
}

function AppLayout({ user, setUser }) {
  const navigate = useNavigate();
  const location = useLocation();
  const isLoggedInArea = location.pathname.startsWith('/app');
  const isAdmin = isAdminUser(user);

  useEffect(() => {
    const saved = getStoredUser();
    if (saved && getToken() && !user) {
      setUser(saved);
    } else if (!getToken() && user) {
      setUser(null);
    }
  }, [user, setUser]);

  useEffect(() => {
    if (!getToken()) return;

    let cancelled = false;
    (async () => {
      const data = await apiGet('/api/auth/me');
      if (cancelled || !data.success || !data.data) return;
      localStorage.setItem('user', JSON.stringify(data.data));
      setUser(data.data);
    })();

    return () => {
      cancelled = true;
    };
  }, [setUser]);

  useEffect(() => {
    if (!isAdmin || !isLoggedInArea) return;
    if (!location.pathname.startsWith('/app/admin')) {
      navigate('/app/admin/invites', { replace: true });
    }
  }, [isAdmin, isLoggedInArea, location.pathname, navigate]);

  const handleAuthSuccess = (loggedInUser) => {
    const normalizedUser = {
      ...loggedInUser,
      admin: Boolean(loggedInUser.admin || String(loggedInUser.username || '').toLowerCase() === 'admin'),
    };
    localStorage.setItem('user', JSON.stringify(normalizedUser));
    setUser(normalizedUser);
    navigate(isAdminUser(normalizedUser) ? '/app/admin/invites' : '/app');
  };

  const handleLogout = () => {
    clearToken();
    localStorage.removeItem('user');
    setUser(null);
    setPublicTitle();
    navigate('/');
  };

  const clubIdMatch = location.pathname.match(/\/app\/clubs\/(\d+)/);
  const activeClubId = clubIdMatch ? clubIdMatch[1] : null;

  const navClass = isLoggedInArea ? 'app-nav internal-nav' : 'app-nav public-nav';

  return (
    <div className={`App ${isLoggedInArea ? 'internal-theme' : 'public-theme'}`}>
      <nav className={navClass}>
        {!isLoggedInArea ? (
          <>
            <Link to="/" className={location.pathname === '/' ? 'active' : ''}>
              {PUBLIC_APP_NAME}
            </Link>
            <Link to="/login" className={location.pathname === '/login' ? 'active' : ''}>
              登录
            </Link>
          </>
        ) : isAdmin ? (
          <>
            <Link
              to="/app/admin/invites"
              className={location.pathname.startsWith('/app/admin/invites') ? 'active' : ''}
            >
              邀请码管理
            </Link>
            <span className="nav-user">{user?.username}</span>
            <button type="button" className="nav-logout-btn" onClick={handleLogout}>
              退出
            </button>
          </>
        ) : (
          <>
            <Link to="/app" className={location.pathname === '/app' ? 'active' : ''}>
              首页
            </Link>
            <Link
              to="/app/clubs"
              className={location.pathname.startsWith('/app/clubs') && !location.pathname.includes('/game/') ? 'active' : ''}
            >
              俱乐部
            </Link>
            {activeClubId && (
              <Link
                to={`/app/clubs/${activeClubId}/rooms`}
                className={location.pathname.includes('/rooms') ? 'active' : ''}
              >
                房间管理
              </Link>
            )}
            <span className="nav-user">{user?.username}</span>
            <button type="button" className="nav-logout-btn" onClick={handleLogout}>
              退出
            </button>
          </>
        )}
      </nav>

      <main className="app-main">
        <Routes>
          <Route path="/" element={<PublicLanding />} />
          <Route
            path="/login"
            element={
              <GuestRoute user={user}>
                <Auth onAuthSuccess={handleAuthSuccess} />
              </GuestRoute>
            }
          />
          <Route
            path="/app"
            element={
              <PrivateRoute user={user}>
                <AppEntry user={user} onLogout={handleLogout} />
              </PrivateRoute>
            }
          />
          <Route
            path="/app/admin/invites"
            element={
              <AdminRoute user={user}>
                <InviteCodeManagement />
              </AdminRoute>
            }
          />
          <Route
            path="/app/clubs"
            element={
              <PlayerRoute user={user}>
                <ClubManagement />
              </PlayerRoute>
            }
          />
          <Route
            path="/app/clubs/:clubId/rooms"
            element={
              <PlayerRoute user={user}>
                <RoomManagement />
              </PlayerRoute>
            }
          />
          <Route
            path="/app/clubs/:clubId/game/:roomId"
            element={
              <PlayerRoute user={user}>
                <GameTableRoute user={user} />
              </PlayerRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

function App() {
  const [user, setUser] = useState(() => {
    const saved = getStoredUser();
    if (!getToken() || !saved) return null;
    return {
      ...saved,
      admin: Boolean(saved.admin || String(saved.username || '').toLowerCase() === 'admin'),
    };
  });

  return (
    <BrowserRouter>
      <AppLayout user={user} setUser={setUser} />
    </BrowserRouter>
  );
}

export default App;
