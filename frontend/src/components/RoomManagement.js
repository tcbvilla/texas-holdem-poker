import React, { useState, useEffect } from 'react';
import './RoomManagement.css';

const RoomManagement = ({ clubId, onEnterRoom }) => {
    const [rooms, setRooms] = useState([]);
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const [createForm, setCreateForm] = useState({
        name: '',
        description: '',
        smallBlind: 10,
        bigBlind: 20,
        defaultChips: 1000,
        minBuyin: 100,
        maxBuyin: 2000,
        maxSeats: 6,
        durationMinutes: 60,
        actionTimeSeconds: 30
    });

    useEffect(() => {
        if (clubId) {
            fetchRooms();
        }
    }, [clubId]);

    const fetchRooms = async () => {
        try {
            const response = await fetch(`/api/rooms/club/${clubId}`);
            const data = await response.json();
            if (data.success) {
                setRooms(data.data);
            }
        } catch (err) {
            console.error('获取房间列表失败:', err);
        }
    };

    const handleCreateRoom = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setSuccess('');

        try {
            const response = await fetch('/api/rooms', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    ...createForm,
                    clubId: clubId
                })
            });

            const data = await response.json();

            if (data.success) {
                setSuccess('房间创建成功！');
                setCreateForm({
                    name: '',
                    description: '',
                    smallBlind: 10,
                    bigBlind: 20,
                    defaultChips: 1000,
                    minBuyin: 100,
                    maxBuyin: 2000,
                    maxSeats: 6,
                    durationMinutes: 60,
                    actionTimeSeconds: 30
                });
                setShowCreateForm(false);
                fetchRooms();
            } else {
                setError(data.message || '创建失败');
            }
        } catch (err) {
            setError('网络错误，请重试');
        } finally {
            setLoading(false);
        }
    };

    const handleRoomAction = async (roomId, action) => {
        setLoading(true);
        setError('');
        setSuccess('');

        try {
            const response = await fetch(`/api/rooms/${roomId}/${action}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            const data = await response.json();

            if (data.success) {
                setSuccess(`房间${action === 'start' ? '开始' : action === 'end' ? '结束' : '取消'}成功！`);
                fetchRooms();
            } else {
                setError(data.message || '操作失败');
            }
        } catch (err) {
            setError('网络错误，请重试');
        } finally {
            setLoading(false);
        }
    };

    const handleJoinRoom = (roomId) => {
        // 调用父组件的回调函数进入房间
        if (onEnterRoom) {
            onEnterRoom(roomId);
        }
    };

    const getStatusText = (status) => {
        const statusMap = {
            'WAITING': '等待中',
            'RUNNING': '进行中',
            'FINISHED': '已结束',
            'CANCELLED': '已取消'
        };
        return statusMap[status] || status;
    };

    const getStatusClass = (status) => {
        const classMap = {
            'WAITING': 'status-waiting',
            'RUNNING': 'status-running',
            'FINISHED': 'status-finished',
            'CANCELLED': 'status-cancelled'
        };
        return classMap[status] || '';
    };

    // 如果没有clubId，显示错误信息
    if (!clubId) {
        return (
            <div className="room-management">
                <div className="error-message">
                    <h2>❌ 错误</h2>
                    <p>请先选择一个俱乐部才能管理房间</p>
                    <p>房间只能在俱乐部内创建和管理</p>
                </div>
            </div>
        );
    }

    return (
        <div className="room-management">
            <div className="room-header">
                <h1>🏠 房间管理</h1>
                <button 
                    className="create-room-btn"
                    onClick={() => setShowCreateForm(true)}
                >
                    + 创建房间
                </button>
            </div>

            {error && <div className="error-message">{error}</div>}
            {success && <div className="success-message">{success}</div>}

            {/* 创建房间表单 */}
            {showCreateForm && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <div className="modal-header">
                            <h2>创建房间</h2>
                            <button 
                                className="close-btn"
                                onClick={() => setShowCreateForm(false)}
                            >
                                ×
                            </button>
                        </div>
                        <form onSubmit={handleCreateRoom} className="create-form">
                            <div className="form-row">
                                <div className="form-group">
                                    <label htmlFor="name">房间名称</label>
                                    <input
                                        type="text"
                                        id="name"
                                        value={createForm.name}
                                        onChange={(e) => setCreateForm({...createForm, name: e.target.value})}
                                        placeholder="请输入房间名称"
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label htmlFor="maxSeats">最大座位数</label>
                                    <select
                                        id="maxSeats"
                                        value={createForm.maxSeats}
                                        onChange={(e) => setCreateForm({...createForm, maxSeats: parseInt(e.target.value)})}
                                    >
                                        {[2,3,4,5,6,7,8,9].map(num => (
                                            <option key={num} value={num}>{num}人</option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            <div className="form-group">
                                <label htmlFor="description">房间描述</label>
                                <textarea
                                    id="description"
                                    value={createForm.description}
                                    onChange={(e) => setCreateForm({...createForm, description: e.target.value})}
                                    placeholder="请输入房间描述"
                                    rows="3"
                                />
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label htmlFor="smallBlind">小盲注</label>
                                    <input
                                        type="number"
                                        id="smallBlind"
                                        value={createForm.smallBlind}
                                        onChange={(e) => setCreateForm({...createForm, smallBlind: parseInt(e.target.value)})}
                                        min="1"
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label htmlFor="bigBlind">大盲注</label>
                                    <input
                                        type="number"
                                        id="bigBlind"
                                        value={createForm.bigBlind}
                                        onChange={(e) => setCreateForm({...createForm, bigBlind: parseInt(e.target.value)})}
                                        min="1"
                                        required
                                    />
                                </div>
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label htmlFor="defaultChips">默认筹码</label>
                                    <input
                                        type="number"
                                        id="defaultChips"
                                        value={createForm.defaultChips}
                                        onChange={(e) => setCreateForm({...createForm, defaultChips: parseInt(e.target.value)})}
                                        min="1"
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label htmlFor="minBuyin">最小买入</label>
                                    <input
                                        type="number"
                                        id="minBuyin"
                                        value={createForm.minBuyin}
                                        onChange={(e) => setCreateForm({...createForm, minBuyin: parseInt(e.target.value)})}
                                        min="1"
                                        required
                                    />
                                </div>
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label htmlFor="maxBuyin">最大买入</label>
                                    <input
                                        type="number"
                                        id="maxBuyin"
                                        value={createForm.maxBuyin}
                                        onChange={(e) => setCreateForm({...createForm, maxBuyin: parseInt(e.target.value)})}
                                        min="1"
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label htmlFor="durationMinutes">房间时长(分钟)</label>
                                    <input
                                        type="number"
                                        id="durationMinutes"
                                        value={createForm.durationMinutes}
                                        onChange={(e) => setCreateForm({...createForm, durationMinutes: parseInt(e.target.value)})}
                                        min="1"
                                        required
                                    />
                                </div>
                            </div>

                            <div className="form-group">
                                <label htmlFor="actionTimeSeconds">操作时间(秒)</label>
                                <input
                                    type="number"
                                    id="actionTimeSeconds"
                                    value={createForm.actionTimeSeconds}
                                    onChange={(e) => setCreateForm({...createForm, actionTimeSeconds: parseInt(e.target.value)})}
                                    min="5"
                                    max="300"
                                    required
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
                                    {loading ? '创建中...' : '创建房间'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* 房间列表 */}
            <div className="rooms-section">
                <h2>房间列表</h2>
                <div className="rooms-grid">
                    {rooms.length === 0 ? (
                        <div className="empty-state">
                            <p>还没有创建任何房间</p>
                        </div>
                    ) : (
                        rooms.map(room => (
                            <div key={room.id} className="room-card">
                                <div className="room-header">
                                    <h3>{room.name}</h3>
                                    <span className={`status-badge ${getStatusClass(room.status)}`}>
                                        {getStatusText(room.status)}
                                    </span>
                                </div>
                                
                                <div className="room-info">
                                    <div className="room-details">
                                        <div className="detail-item">
                                            <span className="label">房间号:</span>
                                            <span className="value">{room.roomCode}</span>
                                        </div>
                                        <div className="detail-item">
                                            <span className="label">盲注:</span>
                                            <span className="value">{room.smallBlind}/{room.bigBlind}</span>
                                        </div>
                                        <div className="detail-item">
                                            <span className="label">座位:</span>
                                            <span className="value">{room.maxSeats}人</span>
                                        </div>
                                        <div className="detail-item">
                                            <span className="label">买入:</span>
                                            <span className="value">{room.minBuyin}-{room.maxBuyin}</span>
                                        </div>
                                        <div className="detail-item">
                                            <span className="label">时长:</span>
                                            <span className="value">{room.durationMinutes}分钟</span>
                                        </div>
                                        <div className="detail-item">
                                            <span className="label">操作时间:</span>
                                            <span className="value">{room.actionTimeSeconds}秒</span>
                                        </div>
                                    </div>
                                    
                                    {room.description && (
                                        <div className="room-description">
                                            <p>{room.description}</p>
                                        </div>
                                    )}
                                </div>

                                <div className="room-actions">
                                    {room.status === 'WAITING' && (
                                        <>
                                            <button 
                                                className="join-btn"
                                                onClick={() => handleJoinRoom(room.id)}
                                                disabled={loading}
                                            >
                                                进入房间
                                            </button>
                                            <button 
                                                className="start-btn"
                                                onClick={() => handleRoomAction(room.id, 'start')}
                                                disabled={loading}
                                            >
                                                开始游戏
                                            </button>
                                        </>
                                    )}
                                    {room.status === 'RUNNING' && (
                                        <>
                                            <button 
                                                className="join-btn"
                                                onClick={() => handleJoinRoom(room.id)}
                                                disabled={loading}
                                            >
                                                进入游戏
                                            </button>
                                            <button 
                                                className="end-btn"
                                                onClick={() => handleRoomAction(room.id, 'end')}
                                                disabled={loading}
                                            >
                                                结束游戏
                                            </button>
                                        </>
                                    )}
                                    {(room.status === 'WAITING' || room.status === 'RUNNING') && (
                                        <button 
                                            className="cancel-btn"
                                            onClick={() => handleRoomAction(room.id, 'cancel')}
                                            disabled={loading}
                                        >
                                            取消房间
                                        </button>
                                    )}
                                    <button 
                                        className="view-btn"
                                        onClick={() => window.location.href = `/rooms/${room.roomCode}`}
                                    >
                                        进入房间
                                    </button>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};

export default RoomManagement;
