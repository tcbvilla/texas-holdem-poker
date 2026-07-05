import React, { useState, useEffect } from 'react';
import './RoomManagement.css';
import { apiGet, apiPost } from '../api';

const RoomManagement = ({ clubId, onEnterRoom }) => {
    const [rooms, setRooms] = useState([]);
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [settlementRoom, setSettlementRoom] = useState(null);
    const [settlementData, setSettlementData] = useState(null);
    const [settlementLoading, setSettlementLoading] = useState(false);

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
        const data = await apiGet(`/api/rooms/club/${clubId}`);
        if (data.success) {
            setRooms(data.data);
        }
    };

    const handleCreateRoom = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setSuccess('');

        try {
            const data = await apiPost('/api/rooms', { ...createForm, clubId });

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
            const data = await apiPost(`/api/rooms/${roomId}/${action}`);

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

    const handleShowSettlement = async (room) => {
        setSettlementRoom(room);
        setSettlementData(null);
        setSettlementLoading(true);
        setError('');
        const data = await apiGet(`/api/rooms/${room.id}/settlement`);
        setSettlementLoading(false);
        if (data.success) {
            setSettlementData(data.data);
        } else {
            setError(data.message || '加载盈亏数据失败');
            setSettlementRoom(null);
        }
    };

    const closeSettlementModal = () => {
        setSettlementRoom(null);
        setSettlementData(null);
    };

    const handleRestartRoom = async (roomId) => {
        if (!window.confirm('确定要重新开始此房间吗？盈亏统计将被清除。')) return;
        setLoading(true);
        setError('');
        setSuccess('');
        try {
            const data = await apiPost(`/api/rooms/${roomId}/restart`);
            if (data.success) {
                setSuccess('房间已重新开始');
                fetchRooms();
            } else {
                setError(data.message || '重新开始失败');
            }
        } catch (err) {
            setError('网络错误，请重试');
        } finally {
            setLoading(false);
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
                                    {(room.status === 'WAITING' || room.status === 'RUNNING') && (
                                        <button 
                                            className="join-btn"
                                            onClick={() => handleJoinRoom(room.id)}
                                            disabled={loading}
                                        >
                                            进入牌桌
                                        </button>
                                    )}
                                    {(room.status === 'WAITING' || room.status === 'RUNNING') && (
                                        <button 
                                            className="cancel-btn"
                                            onClick={() => handleRoomAction(room.id, 'cancel')}
                                            disabled={loading}
                                        >
                                            关闭房间
                                        </button>
                                    )}
                                    {(room.status === 'FINISHED' || room.status === 'CANCELLED') && (
                                        <>
                                            {room.hasSettlement && (
                                                <button
                                                    className="settlement-btn"
                                                    onClick={() => handleShowSettlement(room)}
                                                    disabled={loading}
                                                >
                                                    盈亏统计
                                                </button>
                                            )}
                                            <button
                                                className="restart-btn"
                                                onClick={() => handleRestartRoom(room.id)}
                                                disabled={loading}
                                            >
                                                重新开始
                                            </button>
                                        </>
                                    )}
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>

            {settlementRoom && (
                <div className="settlement-modal-overlay" onClick={closeSettlementModal}>
                    <div className="settlement-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="settlement-modal-header">
                            <h3>{settlementRoom.name} - 盈亏统计</h3>
                            <button className="modal-close-btn" onClick={closeSettlementModal}>关闭</button>
                        </div>
                        {settlementLoading ? (
                            <p className="settlement-loading">加载中...</p>
                        ) : settlementData && settlementData.players && settlementData.players.length > 0 ? (
                            <table className="settlement-table">
                                <thead>
                                    <tr>
                                        <th>玩家</th>
                                        <th>总买入</th>
                                        <th>剩余筹码</th>
                                        <th>盈亏</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {settlementData.players.map((p) => (
                                        <tr key={p.userId}>
                                            <td>{p.username}</td>
                                            <td>{p.totalBuyIn}</td>
                                            <td>{p.remainingChips}</td>
                                            <td className={p.profitLoss >= 0 ? 'profit-positive' : 'profit-negative'}>
                                                {p.profitLoss > 0 ? '+' : ''}{p.profitLoss}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        ) : (
                            <p className="settlement-empty">暂无盈亏数据</p>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default RoomManagement;
