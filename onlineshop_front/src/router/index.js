import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const routes = [
    {
        path: '/',
        name: 'Home',
        component: HomeView,
        meta: { requiresAuth: false }
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/LoginView.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/RegisterView.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/products',
        name: 'Products',
        component: () => import('../views/ProductsView.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/product/:id',
        name: 'ProductDetail',
        component: () => import('../views/ProductDetail.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/cart',
        name: 'Cart',
        component: () => import('../views/CartView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/order/:id',
        name: 'OrderView',
        component: () => import('../views/OrderView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/merchant',
        name: 'Merchant',
        component: () => import('../views/MerchantView.vue'),
        meta: { requiresAuth: true, requiresMerchant: true }
    },
    {
        path: '/merchant/:id',
        name: 'MerchantDetail',
        component: () => import('../views/MerchantView.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/profile',
        name: 'Profile',
        component: () => import('../views/ProfileView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/merchant-operator',
        name: 'MerchantOperator',
        component: () => import('../views/MerchantOperatorView.vue'),
        meta: { requiresAuth: true, requiresMerchant: true }
    },
    {
        path: '/merchant/goods',
        name: 'GoodsManager',
        component: () => import('../views/GoodsManagerView.vue'),
        meta: { requiresAuth: true, requiresMerchant: true }
    },
    {
        path: '/merchant/orders',
        name: 'OrderManager',
        component: () => import('../views/OrderManagerView.vue'),
        meta: { requiresAuth: true, requiresMerchant: true }
    },
    {
        path: '/merchant/sales-report',
        name: 'SalesReport',
        component: () => import('@/views/SalesReportView.vue'),
        meta: { requiresAuth: true, requiresMerchant: true }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

//检查用户登录状态的函数
const checkAuthStatus = () => {
    try {
        const userStr = localStorage.getItem('currentUser')
        if (userStr === 'undefined' || userStr === 'null') {
            localStorage.removeItem('currentUser')
            localStorage.removeItem('userRole')
            return null
        }
        const user = userStr ? JSON.parse(userStr) : null
        if (user && user.uid) {
            localStorage.setItem('userRole', user.role || 'CUSTOMER')
            return user
        }
        localStorage.removeItem('currentUser')
        localStorage.removeItem('userRole')
        return null
    } catch (error) {
        console.error('检查认证状态失败:', error)
        localStorage.removeItem('currentUser')
        localStorage.removeItem('userRole')
        return null
    }
}

router.beforeEach((to, from, next) => {
    const currentUser = checkAuthStatus()
    const isLoggedIn = !!currentUser
    const userRole = currentUser?.role || 'CUSTOMER'
    console.log('路由导航:', to.path, '登录状态:', isLoggedIn, '用户角色:', userRole)
    //如果已登录但访问登录/注册页，重定向到首页
    if ((to.name === 'Login' || to.name === 'Register') && isLoggedIn) {
        console.log('已登录用户访问登录/注册页，跳转到首页')
        next('/')
        return
    }
    //只有当页面明确要求认证时才检查登录状态
    if (to.meta.requiresAuth) {
        if (!isLoggedIn) {
            console.log('需要登录，跳转到登录页')
            next({
                path: '/login',
                query: { redirect: to.fullPath }
            })
            return
        }
        //检查商户权限
        if (to.meta.requiresMerchant && userRole !== 'MERCHANT') {
            console.log('需要商户权限，跳转到首页')
            next('/')
            return
        }
    }
    console.log('正常放行到:', to.path)
    next()
})

export default router