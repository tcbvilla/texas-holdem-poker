import React, { useState, useEffect } from 'react';
import './Auth.css';
import { apiPost, setToken } from '../api';
import { PUBLIC_APP_NAME, setPublicTitle } from '../branding';

const Auth = ({ onAuthSuccess }) => {
    const [isLogin, setIsLogin] = useState(true);
    const [formData, setFormData] = useState({
        username: '',
        inviteCode: '',
        password: '',
        confirmPassword: ''
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        setPublicTitle();
    }, []);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        // 清除错误信息
        if (error) setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setSuccess('');

        // 注册时验证确认密码
        if (!isLogin && formData.password !== formData.confirmPassword) {
            setError('两次输入的密码不一致');
            setLoading(false);
            return;
        }

        if (!isLogin) {
            const code = formData.inviteCode.trim().toUpperCase();
            if (!/^[A-Z0-9]{12,32}$/.test(code)) {
                setError('邀请码为12位及以上字母数字组合');
                setLoading(false);
                return;
            }
        }

        // 验证密码长度
        if (formData.password.length < 6) {
            setError('密码长度不能少于6位');
            setLoading(false);
            return;
        }

        try {
            const endpoint = isLogin ? '/api/auth/login' : '/api/auth/register';
            const requestData = isLogin 
                ? { identifier: formData.username, password: formData.password }
                : { username: formData.username, password: formData.password, inviteCode: formData.inviteCode.trim().toUpperCase() };

            const data = await apiPost(endpoint, requestData);

            if (data.success) {
                setSuccess(isLogin ? '登录成功！' : '注册成功！');
                const userData = {
                    ...data.data,
                    admin: Boolean(data.data.admin || String(data.data.username || '').toLowerCase() === 'admin'),
                };
                setToken(userData.token);
                localStorage.setItem('user', JSON.stringify(userData));
                if (onAuthSuccess) {
                    onAuthSuccess(userData);
                }
            } else {
                setError(data.message || '操作失败');
            }
        } catch (err) {
            setError('网络错误，请重试');
        } finally {
            setLoading(false);
        }
    };

    const switchMode = () => {
        setIsLogin(!isLogin);
        setFormData({
            username: '',
            inviteCode: '',
            password: '',
            confirmPassword: ''
        });
        setError('');
        setSuccess('');
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-header">
                    <h1>{PUBLIC_APP_NAME}</h1>
                    <h2>{isLogin ? '用户登录' : '用户注册'}</h2>
                </div>

                <form onSubmit={handleSubmit} className="auth-form">
                    <div className="form-group">
                        <label htmlFor="username">用户名</label>
                        <input
                            type="text"
                            id="username"
                            name="username"
                            value={formData.username}
                            onChange={handleInputChange}
                            placeholder="请输入用户名"
                            required
                        />
                    </div>

                    {!isLogin && (
                        <div className="form-group">
                            <label htmlFor="inviteCode">邀请码</label>
                            <input
                                type="text"
                                id="inviteCode"
                                name="inviteCode"
                                value={formData.inviteCode}
                                onChange={handleInputChange}
                                placeholder="请输入邀请码"
                                required
                            />
                        </div>
                    )}

                    <div className="form-group">
                        <label htmlFor="password">密码</label>
                        <input
                            type="password"
                            id="password"
                            name="password"
                            value={formData.password}
                            onChange={handleInputChange}
                            placeholder="请输入密码"
                            required
                        />
                    </div>

                    {!isLogin && (
                        <div className="form-group">
                            <label htmlFor="confirmPassword">确认密码</label>
                            <input
                                type="password"
                                id="confirmPassword"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleInputChange}
                                placeholder="请再次输入密码"
                                required
                            />
                        </div>
                    )}

                    {error && <div className="error-message">{error}</div>}
                    {success && <div className="success-message">{success}</div>}

                    <button 
                        type="submit" 
                        className="auth-button"
                        disabled={loading}
                    >
                        {loading ? '处理中...' : (isLogin ? '登录' : '注册')}
                    </button>
                </form>

                <div className="auth-switch">
                    <span>
                        {isLogin ? '还没有账号？' : '已有账号？'}
                        <button 
                            type="button" 
                            className="switch-button"
                            onClick={switchMode}
                        >
                            {isLogin ? '立即注册' : '立即登录'}
                        </button>
                    </span>
                </div>
            </div>
        </div>
    );
};

export default Auth;
