import axios from 'axios';

const API_BASE_URL = '/api';

//创建axios实例
const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    withCredentials: true, //必须为true才能携带Cookie
});

//请求拦截器
api.interceptors.request.use(
    (config) => {
        console.log('🛒🛒🛒 API请求:', config.method?.toUpperCase(), config.url);
        //对GET请求添加时间戳避免缓存
        if (config.method === 'get') {
            config.params = {
                ...config.params,
                _t: Date.now()
            };
        }
        return config;
    },
    (error) => {
        console.error('❌❌ 请求拦截器错误:', error);
        return Promise.reject(error);
    }
);

//响应拦截器
api.interceptors.response.use(
    (response) => {
        console.log('✅ API响应成功:', response.config.url);
        //统一处理响应格式
        const responseData = response.data;
        //如果后端返回了标准格式，直接返回
        if (responseData && typeof responseData === 'object' && 'success' in responseData) {
            return responseData;
        }
        //对于其他格式的响应，包装成标准格式
        return {
            success: true,
            data: responseData,
            message: '请求成功'
        };
    },
    (error) => {
        console.error('❌❌ API响应错误:', error.config?.url, error.response?.status);
        //专门处理Session相关的错误
        if (error.response?.status === 401 || error.response?.status === 403) {
            const errorData = error.response.data;
            //检查是否是Session错误
            if (errorData && errorData.errorType === 'SESSION_INVALID') {
                console.log('🔐🔐 检测到Session无效，清除本地存储');
                apiUtils.clearUser();
                //跳转到登录页
                if (window.location.pathname !== '/login' && !window.location.pathname.includes('/login')) {
                    const redirect = encodeURIComponent(window.location.pathname + window.location.search);
                    window.location.href = `/login?redirect=${redirect}`;
                }
                //阻止后续错误处理
                return Promise.reject({
                    success: false,
                    message: '登录已过期，请重新登录',
                    redirecting: true
                });
            }
        }
        //处理网络错误或请求超时
        if (error.code === 'NETWORK_ERROR' || error.code === 'ECONNABORTED' || !error.response) {
            return Promise.reject({
                success: false,
                message: '网络连接错误，请检查网络设置',
                data: null
            });
        }
        const status = error.response.status;
        let errorMessage = '请求失败';
        if (status === 401) {
            //未授权，清除本地存储
            apiUtils.clearUser();
            errorMessage = '登录已过期，请重新登录';
            //跳转到登录页面
            if (window.location.pathname !== '/login' && !window.location.pathname.includes('/login')) {
                const redirect = encodeURIComponent(window.location.pathname + window.location.search);
                window.location.href = `/login?redirect=${redirect}`;
            }
        } else if (status === 403) {
            errorMessage = '权限不足，无法访问该资源';
            //403错误时检查登录状态
            if (!apiUtils.isLoggedIn()) {
                apiUtils.clearUser();
                if (window.location.pathname !== '/login' && !window.location.pathname.includes('/login')) {
                    const redirect = encodeURIComponent(window.location.pathname + window.location.search);
                    window.location.href = `/login?redirect=${redirect}`;
                }
            }
        } else if (status === 404) {
            errorMessage = '请求的资源不存在';
        } else if (status >= 500) {
            errorMessage = '服务器内部错误，请稍后重试';
        }
        //尝试从响应数据中获取错误信息
        const responseData = error.response.data;
        if (responseData && typeof responseData === 'object') {
            errorMessage = responseData.message || errorMessage;
        }
        return Promise.reject({
            success: false,
            message: errorMessage,
            data: null
        });
    }
);

//工具函数
export const apiUtils = {
    //处理API响应
    handleResponse: (response) => {
        if (response && response.success) {
            return response.data !== undefined ? response.data : response;
        } else {
            throw new Error(response?.message || '请求失败');
        }
    },

    //处理API错误
    handleError: (error) => {
        console.error('API调用错误:', error);
        throw error;
    },

    //检查用户权限
    checkPermission: (requiredRole) => {
        const user = apiUtils.getCurrentUser();
        return user && user.role === requiredRole;
    },

    //获取当前用户信息
    getCurrentUser: () => {
        //首先检查localStorage中是否有用户信息
        const userStr = localStorage.getItem('currentUser');
        if (userStr && userStr !== 'undefined' && userStr !== 'null') {
            try {
                return JSON.parse(userStr);
            } catch (error) {
                console.error('解析用户信息失败:', error);
                localStorage.removeItem('currentUser');
            }
        }
        return null;
    },

    //设置用户信息
    setCurrentUser: (user) => {
        if (user) {
            localStorage.setItem('currentUser', JSON.stringify(user));
        }
    },

    //清除用户信息
    clearUser: () => {
        localStorage.removeItem('currentUser');
        localStorage.removeItem('userRole');
        // 移除token相关存储
        localStorage.removeItem('token');
        sessionStorage.removeItem('currentUser');
        sessionStorage.removeItem('token');
    },

    //基于Session状态检查是否已登录
    isLoggedIn: () => {
        const user = apiUtils.getCurrentUser();
        return !!(user && user.uid);
    },

    //获取用户角色
    getUserRole: () => {
        const user = apiUtils.getCurrentUser();
        return user ? user.role : null;
    },

    //检查登录状态并跳转
    checkLoginAndRedirect: () => {
        if (!apiUtils.isLoggedIn()) {
            apiUtils.clearUser();
            if (window.location.pathname !== '/login' && !window.location.pathname.includes('/login')) {
                const redirect = encodeURIComponent(window.location.pathname + window.location.search);
                window.location.href = `/login?redirect=${redirect}`;
            }
            return false;
        }
        return true;
    },

    //处理权限错误
    handlePermissionError: (error) => {
        if (error.message && (error.message.includes('权限') || error.message.includes('登录') || error.message.includes('认证'))) {
            apiUtils.checkLoginAndRedirect();
            return true;
        }
        return false;
    },

    //检查Session状态
    checkSession: async () => {
        try {
            const response = await api.get('/auth/session-check');
            console.log('🔍🔍 Session检查结果:', response);
            return response;
        } catch (error) {
            console.error('❌❌ Session检查失败:', error);
            throw error;
        }
    },

    //综合检查登录状态（LocalStorage+Session）
    checkLoginStatus: async () => {
        try {
            //先检查本地登录状态
            const localUser = apiUtils.getCurrentUser();
            if (!localUser) {
                return false;
            }
            //再检查后端Session状态
            const sessionResponse = await apiUtils.checkSession();
            return sessionResponse && sessionResponse.success;
        } catch (error) {
            console.error('登录状态检查失败:', error);
            // Session检查失败，清除本地状态
            apiUtils.clearUser();
            return false;
        }
    },

    //处理Session过期
    handleSessionExpired: () => {
        console.log('🔐 Session已过期，清除用户信息');
        apiUtils.clearUser();
        if (window.location.pathname !== '/login' && !window.location.pathname.includes('/login')) {
            const redirect = encodeURIComponent(window.location.pathname + window.location.search);
            window.location.href = `/login?redirect=${redirect}&reason=session_expired`;
        }
    }
};

//认证相关API
export const authAPI = {
    loginByName: (credentials) => api.post('/auth/login/name', credentials),
    loginByEmail: (credentials) => api.post('/auth/login/email', credentials),
    register: (user) => api.post('/auth/register', user),
    logout: () => api.post('/auth/logout'),
    getCurrentUser: () => api.get('/auth/current'),
    checkNickname: (nickname) => api.get('/auth/check-nickname', { params: { nickname } }),
    checkEmail: (email) => api.get('/auth/check-email', { params: { email } }),
    updateProfile: (user) => api.put('/auth/profile', user),
    debugSession: () => api.get('/auth/debug-session'),
    sessionCheck: () => api.get('/auth/session-check'),
    sessionExpired: () => api.get('/auth/session-expired'),
    becomeMerchant: () => api.post('/auth/become-merchant'),
    deleteAccount: () => api.post('/auth/delete-account')
};

//商品相关API
export const goodsAPI = {
    getAll: () => api.get('/goods'),
    getById: (id) => api.get(`/goods/${id}`),
    getMerchantByGoodsId: (id) => api.get(`/goods/${id}/merchant`),
    searchByName: (name) => api.get('/goods/search/name', { params: { name } }),
    searchByPrice: (minPrice, maxPrice) => api.get('/goods/search/price',{params:{minPrice, maxPrice}}),
    getAvailable: () => api.get('/goods/available'),
    addGoods: (goods) => api.post('/goods', goods),
    updateGoods: (id, goods) => api.put(`/goods/${id}`, goods),
    deleteGoods: (id) => api.delete(`/goods/${id}`),
    getMyGoods: () => api.get('/goods/my-goods'),
    searchMyGoods: (name) => api.get('/goods/my-goods/search', { params: { name } }),
    getMyGoodsStats: () => api.get('/goods/my-goods/stats'),
    getByMerchant: (merchantId) => api.get(`/goods/merchant/${merchantId}`),
    deactivateGoods: (gid) => api.post(`/goods/${gid}/deactivate`),
    activateGoods: (gid) => api.post(`/goods/${gid}/activate`),
    batchDeactivate: (goodsIds) => api.post('/goods/batch-deactivate', goodsIds),
    getSalesRanking: () => api.get('/goods/my-goods/sales-ranking'),
    getSalesValueRanking: () => api.get('/goods/my-goods/sales-value-ranking')
};

//购物车相关API
export const cartAPI = {
    getCart: () => api.get('/cart'),
    addToCart: (item) => api.post('/cart/add', item),
    updateCartItem: (goodsId, quantity) => api.put(`/cart/update/${goodsId}`, { quantity }),
    removeFromCart: (goodsId) => api.delete(`/cart/remove/${goodsId}`),
    clearCart: () => api.delete('/cart/clear'),
    getCartCount: () => api.get('/cart/count'),
};

//订单相关API
export const orderAPI = {
    createOrder: (orderData) => api.post('/orders/create', orderData),
    payOrder: (orderId) => api.post(`/orders/${orderId}/pay`),
    getMyOrders: () => api.get('/orders/my-orders'),
    getAllOrders: () => api.get('/orders/all'),
    getOrderDetails: (orderId) => api.get(`/orders/${orderId}/details`),
    cancelOrder: (orderId) => api.post(`/orders/${orderId}/cancel`),
    shipOrder: (orderId) => api.post(`/orders/${orderId}/ship`),
    completeOrder: (orderId) => api.post(`/orders/${orderId}/complete`),
    getOrdersByStatus: (status) => api.get('/orders', { params: { status } }),
};

//商家相关API
export const merchantAPI = {
    getAllOrders: (status) => api.get('/merchant/orders', { params: { status } }),
    shipOrder: (orderId) => api.post(`/merchant/orders/${orderId}/ship`),
    getSalesReport: (startDate, endDate) => api.get('/merchant/sales/report', {
        params: { startDate, endDate }
    }),
    getBestSellers: () => api.get('/merchant/sales/best-sellers'),
    getMerchantStats: () => api.get('/merchant/stats'),
    getMerchantGoods: () => api.get('/goods/my-goods'),
    addMerchantGoods: (goods) => api.post('/goods', goods),
    updateMerchantGoods: (id, goods) => api.put(`/goods/${id}`, goods),
    deleteMerchantGoods: (id) => api.delete(`/goods/${id}`),
    getMerchantById: (merchantId) => api.get(`/auth/merchant/${merchantId}`),
    getSalesRanking: () => api.get('/goods/my-goods/sales-ranking'),
    getSalesValueRanking: () => api.get('/goods/my-goods/sales-value-ranking')
};

//文件上传API
export const uploadAPI = {
    uploadAvatar: (file) => {
        const formData = new FormData();
        formData.append('file', file);
        return api.post('/upload/avatar', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
            timeout: 30000,
        });
    },
    uploadProduct: (file) => {
        const formData = new FormData();
        formData.append('file', file);
        return api.post('/upload/product', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
            timeout: 30000,
        });
    }
};
export default api;