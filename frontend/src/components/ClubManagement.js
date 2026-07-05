import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './ClubManagement.css';
import { apiGet, apiPost } from '../api';
import { setInternalTitle } from '../branding';

const ClubManagement = () => {
    const navigate = useNavigate();
    const [clubs, setClubs] = useState([]);
    const [myClubs, setMyClubs] = useState([]);
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [searchKeyword, setSearchKeyword] = useState('');

    const [createForm, setCreateForm] = useState({
        name: '',
        description: ''
    });

    useEffect(() => {
        setInternalTitle('俱乐部');
        fetchClubs();
        fetchMyClubs();
    }, []);

    const fetchClubs = async () => {
        const data = await apiGet('/api/public/clubs');
        if (data.success) {
            setClubs(data.data);
        }
    };

    const fetchMyClubs = async () => {
        const data = await apiGet('/api/clubs/my-joined');
        if (data.success) {
            setMyClubs(data.data);
        }
    };

    const handleCreateClub = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setSuccess('');

        try {
            const data = await apiPost('/api/clubs', createForm);

            if (data.success) {
                setSuccess('俱乐部创建成功！');
                setCreateForm({ name: '', description: '' });
                setShowCreateForm(false);
                fetchClubs();
                fetchMyClubs();
            } else {
                setError(data.message || '创建失败');
            }
        } catch (err) {
            setError('网络错误，请重试');
        } finally {
            setLoading(false);
        }
    };

    const handleJoinClub = async (clubId) => {
        setLoading(true);
        setError('');
        setSuccess('');

        try {
            const data = await apiPost(`/api/club-join/${clubId}/apply`);

            if (data.success) {
                setSuccess('成功加入俱乐部！');
                fetchMyClubs();
            } else {
                setError(data.message || '加入失败');
            }
        } catch (err) {
            setError('网络错误，请重试');
        } finally {
            setLoading(false);
        }
    };

    const handleLeaveClub = async (clubId) => {
        if (!window.confirm('确定要退出这个俱乐部吗？')) {
            return;
        }

        setLoading(true);
        setError('');
        setSuccess('');

        try {
            const data = await apiPost(`/api/club-join/${clubId}/leave`);

            if (data.success) {
                setSuccess('成功退出俱乐部！');
                fetchMyClubs();
            } else {
                setError(data.message || '退出失败');
            }
        } catch (err) {
            setError('网络错误，请重试');
        } finally {
            setLoading(false);
        }
    };

    const filteredClubs = clubs.filter(club => 
        club.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
        (club.description && club.description.toLowerCase().includes(searchKeyword.toLowerCase()))
    );

    const isJoined = (clubId) => {
        return myClubs.some(club => club.id === clubId);
    };

    return (
        <div className="club-management">
            <div className="club-header">
                <h1>🏛️ 俱乐部管理</h1>
                <button 
                    className="create-club-btn"
                    onClick={() => setShowCreateForm(true)}
                >
                    + 创建俱乐部
                </button>
            </div>

            {error && <div className="error-message">{error}</div>}
            {success && <div className="success-message">{success}</div>}

            {/* 创建俱乐部表单 */}
            {showCreateForm && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <div className="modal-header">
                            <h2>创建俱乐部</h2>
                            <button 
                                className="close-btn"
                                onClick={() => setShowCreateForm(false)}
                            >
                                ×
                            </button>
                        </div>
                        <form onSubmit={handleCreateClub} className="create-form">
                            <div className="form-group">
                                <label htmlFor="name">俱乐部名称</label>
                                <input
                                    type="text"
                                    id="name"
                                    value={createForm.name}
                                    onChange={(e) => setCreateForm({...createForm, name: e.target.value})}
                                    placeholder="请输入俱乐部名称"
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="description">俱乐部描述</label>
                                <textarea
                                    id="description"
                                    value={createForm.description}
                                    onChange={(e) => setCreateForm({...createForm, description: e.target.value})}
                                    placeholder="请输入俱乐部描述"
                                    rows="4"
                                />
                            </div>
                            <div className="form-actions">
                                <button 
                                    type="button" 
                                    className="cancel-btn"
                                    onClick={() => setShowCreateForm(false)}
                                >
                                    取消
                                </button>
                                <button 
                                    type="submit" 
                                    className="submit-btn"
                                    disabled={loading}
                                >
                                    {loading ? '创建中...' : '创建俱乐部'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* 我的俱乐部 */}
            <div className="section">
                <h2>我的俱乐部</h2>
                <div className="clubs-grid">
                    {myClubs.length === 0 ? (
                        <div className="empty-state">
                            <p>您还没有加入任何俱乐部</p>
                        </div>
                    ) : (
                        myClubs.map(club => (
                            <div key={club.id} className="club-card joined">
                                <div className="club-info">
                                    <h3>{club.name}</h3>
                                    <p>{club.description || '暂无描述'}</p>
                                    <div className="club-meta">
                                        <span>创建时间: {new Date(club.createdAt).toLocaleDateString()}</span>
                                    </div>
                                </div>
                                <div className="club-actions">
                                    <button 
                                        className="view-btn"
                                        onClick={() => navigate(`/app/clubs/${club.id}/rooms`)}
                                    >
                                        进入俱乐部
                                    </button>
                                    <button 
                                        className="leave-btn"
                                        onClick={() => handleLeaveClub(club.id)}
                                        disabled={loading}
                                    >
                                        退出
                                    </button>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>

            {/* 所有俱乐部 */}
            <div className="section">
                <div className="section-header">
                    <h2>所有俱乐部</h2>
                    <div className="search-box">
                        <input
                            type="text"
                            placeholder="搜索俱乐部..."
                            value={searchKeyword}
                            onChange={(e) => setSearchKeyword(e.target.value)}
                        />
                    </div>
                </div>
                <div className="clubs-grid">
                    {filteredClubs.length === 0 ? (
                        <div className="empty-state">
                            <p>没有找到俱乐部</p>
                        </div>
                    ) : (
                        filteredClubs.map(club => (
                            <div key={club.id} className="club-card">
                                <div className="club-info">
                                    <h3>{club.name}</h3>
                                    <p>{club.description || '暂无描述'}</p>
                                    <div className="club-meta">
                                        <span>创建时间: {new Date(club.createdAt).toLocaleDateString()}</span>
                                    </div>
                                </div>
                                <div className="club-actions">
                                    {isJoined(club.id) ? (
                                        <button 
                                            className="joined-btn"
                                            disabled
                                        >
                                            已加入
                                        </button>
                                    ) : (
                                        <button 
                                            className="join-btn"
                                            onClick={() => handleJoinClub(club.id)}
                                            disabled={loading}
                                        >
                                            加入俱乐部
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};

export default ClubManagement;
