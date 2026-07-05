// Unified API helper that automatically attaches the auth token.

export function getToken() {
    return localStorage.getItem('token');
}

export function setToken(token) {
    if (token) {
        localStorage.setItem('token', token);
    }
}

export function clearToken() {
    localStorage.removeItem('token');
}

export function getStoredUser() {
    const raw = localStorage.getItem('user');
    return raw ? JSON.parse(raw) : null;
}

/**
 * Perform a JSON API request. Returns the parsed body ({ success, data, message }).
 */
export async function api(path, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {}),
    };
    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    let body = options.body;
    if (body && typeof body !== 'string') {
        body = JSON.stringify(body);
    }

    try {
        const res = await fetch(path, { ...options, headers, body });
        const text = await res.text();
        return text ? JSON.parse(text) : { success: res.ok };
    } catch (err) {
        return { success: false, message: '网络错误，请重试' };
    }
}

export const apiGet = (path) => api(path, { method: 'GET' });
export const apiPost = (path, body) => api(path, { method: 'POST', body });
