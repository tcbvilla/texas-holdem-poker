import React, { useState, useEffect, useCallback } from 'react';
import './InviteCodeManagement.css';
import { apiGet, apiPost } from '../api';
import { setInternalTitle } from '../branding';

const InviteCodeManagement = () => {
    const [codes, setCodes] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [generateCount, setGenerateCount] = useState(1);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [newCodes, setNewCodes] = useState([]);

    const fetchCodes = useCallback(async () => {
        setLoading(true);
        setError('');
        const statusParam = statusFilter === 'ALL' ? '' : `&status=${statusFilter}`;
        const data = await apiGet(`/api/admin/invite-codes?page=${page}&size=20${statusParam}`);
        setLoading(false);
        if (data.success) {
            setCodes(data.data.content || []);
            setTotalPages(data.data.totalPages || 0);
            setTotalElements(data.data.totalElements || 0);
        } else {
            setError(data.message || '加载邀请码失败');
        }
    }, [page, statusFilter]);

    useEffect(() => {
        setInternalTitle('邀请码管理');
        fetchCodes();
    }, [fetchCodes]);

    const handleGenerate = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setSuccess('');
        setNewCodes([]);
        try {
            const data = await apiPost('/api/admin/invite-codes', { count: generateCount });
            if (data.success) {
                setSuccess(`成功生成 ${data.data.codes.length} 个邀请码`);
                setNewCodes(data.data.codes || []);
                setPage(0);
                fetchCodes();
            } else {
                setError(data.message || '生成失败');
            }
        } catch (err) {
            setError('网络错误，请重试');
        } finally {
            setLoading(false);
        }
    };

    const copyCode = async (code) => {
        try {
            await navigator.clipboard.writeText(code);
            setSuccess(`已复制: ${code}`);
            setTimeout(() => setSuccess(''), 2000);
        } catch (err) {
            setError('复制失败，请手动复制');
        }
    };

    const copyAllNewCodes = async () => {
        if (newCodes.length === 0) return;
        try {
            await navigator.clipboard.writeText(newCodes.join('\n'));
            setSuccess('已复制全部新邀请码');
            setTimeout(() => setSuccess(''), 2000);
        } catch (err) {
            setError('复制失败，请手动复制');
        }
    };

    const getStatusText = (status) => (status === 'USED' ? '已使用' : '未使用');

    return (
        <div className="invite-code-management">
            <div className="invite-header">
                <h1>邀请码管理</h1>
            </div>

            {error && <div className="error-message">{error}</div>}
            {success && <div className="success-message">{success}</div>}

            <div className="invite-generate-panel">
                <form onSubmit={handleGenerate} className="invite-generate-form">
                    <label htmlFor="generateCount">生成数量</label>
                    <input
                        type="number"
                        id="generateCount"
                        min="1"
                        max="50"
                        value={generateCount}
                        onChange={(e) => setGenerateCount(parseInt(e.target.value, 10) || 1)}
                    />
                    <button type="submit" className="generate-btn" disabled={loading}>
                        {loading ? '处理中...' : '生成邀请码'}
                    </button>
                </form>
            </div>

            {newCodes.length > 0 && (
                <div className="new-codes-panel">
                    <div className="new-codes-header">
                        <h3>本次生成的邀请码</h3>
                        <button type="button" className="copy-all-btn" onClick={copyAllNewCodes}>
                            复制全部
                        </button>
                    </div>
                    <div className="new-codes-list">
                        {newCodes.map((code) => (
                            <button key={code} type="button" className="code-chip" onClick={() => copyCode(code)}>
                                {code}
                            </button>
                        ))}
                    </div>
                </div>
            )}

            <div className="invite-filter-bar">
                <label htmlFor="statusFilter">状态筛选</label>
                <select
                    id="statusFilter"
                    value={statusFilter}
                    onChange={(e) => {
                        setStatusFilter(e.target.value);
                        setPage(0);
                    }}
                >
                    <option value="ALL">全部</option>
                    <option value="UNUSED">未使用</option>
                    <option value="USED">已使用</option>
                </select>
                <span className="invite-total">共 {totalElements} 条</span>
            </div>

            <div className="invite-table-wrap">
                <table className="invite-table">
                    <thead>
                        <tr>
                            <th>邀请码</th>
                            <th>状态</th>
                            <th>创建时间</th>
                            <th>使用者</th>
                            <th>使用时间</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        {codes.length === 0 ? (
                            <tr>
                                <td colSpan="6" className="empty-cell">暂无邀请码</td>
                            </tr>
                        ) : (
                            codes.map((item) => (
                                <tr key={item.id}>
                                    <td className="code-cell">{item.code}</td>
                                    <td>
                                        <span className={`status-tag ${item.status === 'USED' ? 'used' : 'unused'}`}>
                                            {getStatusText(item.status)}
                                        </span>
                                    </td>
                                    <td>{item.createdAt ? new Date(item.createdAt).toLocaleString() : '-'}</td>
                                    <td>{item.usedByUsername || '-'}</td>
                                    <td>{item.usedAt ? new Date(item.usedAt).toLocaleString() : '-'}</td>
                                    <td>
                                        <button type="button" className="copy-btn" onClick={() => copyCode(item.code)}>
                                            复制
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            <div className="invite-pagination">
                <button
                    type="button"
                    disabled={page <= 0 || loading}
                    onClick={() => setPage((p) => p - 1)}
                >
                    上一页
                </button>
                <span>第 {page + 1} / {Math.max(totalPages, 1)} 页</span>
                <button
                    type="button"
                    disabled={page + 1 >= totalPages || loading}
                    onClick={() => setPage((p) => p + 1)}
                >
                    下一页
                </button>
            </div>
        </div>
    );
};

export default InviteCodeManagement;
