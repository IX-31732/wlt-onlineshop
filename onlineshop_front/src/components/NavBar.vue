<template>
  <header class="header">
    <div class="container">
      <div class="nav-bar">
        <!-- 网站Logo - 点击返回首页 -->
        <div class="logo" @click="goToHome">
          <span class="home-icon">🏠</span>
          <h1>购物商城</h1>
        </div>
        <!-- 搜索框 -->
        <div class="search-container">
          <div class="search-box">
            <input
                type="text"
                v-model="searchKeyword"
                :placeholder="searchPlaceholder"
                @focus="clearPlaceholder"
                @blur="restorePlaceholder"
                @keyup.enter="handleSearch"
                class="search-input"
            />
            <button @click="handleSearch" class="search-btn">
              <span>搜索</span>
            </button>
          </div>
        </div>
        <!-- 商家管理/成为商家按钮 -->
        <div class="merchant-button-container" v-if="isLoggedIn">
          <button
              v-if="userRole === 'MERCHANT'"
              @click="goToMerchantOperator"
              class="merchant-btn"
              title="商家管理"
          >
            <span>商家管理</span>
          </button>
          <button
              v-else
              @click="showBecomeMerchantConfirm"
              class="merchant-btn"
              title="成为商家"
          >
            <span>成为商家</span>
          </button>
        </div>
        <!-- 购物车按钮 -->
        <div class="cart-button-container">
          <button @click="goToCart" class="cart-btn" title="购物车">
            <span class="cart-icon">🛒</span>
            <span v-if="cartItemCount > 0" class="cart-badge">{{ cartItemCount }}</span>
          </button>
        </div>
        <!-- 用户操作区域 -->
        <div class="user-actions">
          <template v-if="!isLoggedIn">
            <button @click="handleLogin" class="auth-btn login-btn">登录</button>
            <button @click="handleRegister" class="auth-btn register-btn">注册</button>
          </template>
          <template v-else>
            <button @click="goToUserCenter" class="user-center-btn">
              <span>个人中心</span>
            </button>
            <button @click="handleLogout" class="auth-btn logout-btn">退出</button>
          </template>
        </div>
      </div>
    </div>
  </header>
</template>

<script>
import {cartAPI, apiUtils, authAPI} from '../services/api'

export default {
  name: 'NavBar',
  props: {
    isLoggedIn: {
      type: Boolean,
      default: false
    },
    currentUser: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      searchKeyword: '',
      searchPlaceholder: '请输入商品名称进行搜索...',
      cartItemCount: 0,
      cartPollingInterval: null
    }
  },
  computed: {
    userRole() {
      return this.currentUser?.role || localStorage.getItem('userRole') || 'CUSTOMER';
    }
  },
  async mounted() {
    if (this.isLoggedIn) {
      await this.loadCartCount()
      this.startCartPolling()
    }
  },
  beforeUnmount() {
    this.stopCartPolling()
  },
  watch: {
    isLoggedIn: {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.loadCartCount()
          this.startCartPolling()
        } else {
          this.cartItemCount = 0
          this.stopCartPolling()
        }
      }
    }
  },
  methods: {
    //返回首页
    goToHome() {
      this.$router.push('/')
    },

    //搜索相关方法
    clearPlaceholder() {
      this.searchPlaceholder = ''
    },
    restorePlaceholder() {
      if (!this.searchKeyword) {
        this.searchPlaceholder = '请输入商品名称进行搜索...'
      }
    },
    handleSearch() {
      if (this.searchKeyword.trim()) {
        this.$router.push({
          path: '/products',
          query: { search: this.searchKeyword }
        })
      }
    },

    //购物车相关方法
    async loadCartCount() {
      if (!this.isLoggedIn) {
        this.cartItemCount = 0
        return
      }
      try {
        const response = await cartAPI.getCartCount()
        const data = apiUtils.handleResponse(response)
        this.cartItemCount = data.count || 0
      } catch (error) {
        console.error('获取购物车数量失败:', error)
        this.cartItemCount = 0
      }
    },
    startCartPolling() {
      //每30秒更新一次购物车数量
      this.cartPollingInterval = setInterval(() => {
        this.loadCartCount()
      }, 30000)
    },
    stopCartPolling() {
      if (this.cartPollingInterval) {
        clearInterval(this.cartPollingInterval)
        this.cartPollingInterval = null
      }
    },
    goToCart() {
      if (!this.isLoggedIn) {
        this.$router.push('/login')
        return
      }
      this.$router.push('/cart')
    },

    //用户操作相关方法
    handleLogin() {
      this.$router.push('/login')
    },
    handleRegister() {
      this.$router.push('/register')
    },
    handleLogout() {
      this.$emit('logout')
    },
    goToUserCenter() {
      this.$router.push('/profile')
    },

    //商家管理和成为商家
    //跳转到商家管理界面
    goToMerchantOperator() {
      this.$router.push('/merchant-operator');
    },
    //显示成为商家确认框
    showBecomeMerchantConfirm() {
      if (confirm('确定要成为商家吗？成为商家后您可以管理商品和订单。')) {
        this.convertToMerchant();
      }
    },
    //转换为商家
    async convertToMerchant() {
      try {
        console.log('开始转换为商家角色...');
        //调用后端接口
        const response = await authAPI.becomeMerchant();
        const data = apiUtils.handleResponse(response);
        if (data.success) {
          //更新本地用户信息
          const updatedUser = data.user;
          apiUtils.setCurrentUser(updatedUser);
          localStorage.setItem('userRole', 'MERCHANT');
          //通知父组件用户信息已更新
          this.$emit('user-updated', updatedUser);
          //显示成功消息
          alert('恭喜！您已成为商家。');
          //跳转到商家管理页面
          this.$router.push('/merchant-operator');
        }
      } catch (error) {
        console.error('转换为商家失败:', error);
        if (error.message && error.message.includes('已经是商家')) {
          alert('您已经是商家身份！');
          //更新本地状态
          const currentUser = apiUtils.getCurrentUser();
          if (currentUser) {
            currentUser.role = 'MERCHANT';
            apiUtils.setCurrentUser(currentUser);
            localStorage.setItem('userRole', 'MERCHANT');
            this.$emit('user-updated', currentUser);
          }
        } else if (error.message && error.message.includes('请先登录')) {
          alert('请先登录后再申请成为商家');
          this.$router.push('/login');
        } else {
          alert('操作失败：' + (error.message || '请稍后重试'));
        }
      }
    }
  }
}
</script>

<style scoped>
.header {
  background-color: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 0;
  height: 80px;
}
.logo {
  cursor: pointer;
  transition: transform 0.2s;
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo:hover {
  transform: scale(1.05);
}
.home-icon {
  font-size: 24px;
}
.logo h1 {
  color: #e4393c;
  margin: 0;
  font-size: 28px;
}
.search-container {
  flex: 1;
  max-width: 600px;
  margin: 0 40px;
}
.search-box {
  display: flex;
  background: #fff;
  border: 2px solid #e4393c;
  border-radius: 25px;
  overflow: hidden;
}
.search-input {
  flex: 1;
  padding: 12px 20px;
  border: none;
  outline: none;
  font-size: 16px;
}
.search-btn {
  background: #e4393c;
  color: white;
  border: none;
  padding: 0 30px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
}
.search-btn:hover {
  background: #c03537;
}
.merchant-button-container {
  margin: 0 20px;
}
.merchant-btn {
  padding: 10px 20px;
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}
.merchant-btn:hover {
  background: #e9ecef;
  border-color: #adb5bd;
}
.cart-button-container {
  margin: 0 20px;
}
.cart-btn {
  position: relative;
  width: 50px;
  height: 50px;
  border: none;
  background: #f8f9fa;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}
.cart-btn:hover {
  background: #e9ecef;
  transform: scale(1.1);
}
.cart-icon {
  font-size: 24px;
}
.cart-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #e4393c;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}
.user-actions {
  display: flex;
  gap: 15px;
}
.auth-btn {
  padding: 10px 20px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}
.login-btn {
  color: #666;
}
.login-btn:hover {
  border-color: #e4393c;
  color: #e4393c;
}
.register-btn {
  background: #e4393c;
  color: white;
  border-color: #e4393c;
}
.register-btn:hover {
  background: #c03537;
}
.logout-btn {
  background: #f8f9fa;
  color: #666;
  border-color: #ddd;
}
.logout-btn:hover {
  background: #e9ecef;
  border-color: #adb5bd;
}
.user-center-btn {
  padding: 10px 20px;
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
}
.user-center-btn:hover {
  background: #e9ecef;
  border-color: #adb5bd;
}
@media (max-width: 768px) {
  .nav-bar {
    flex-direction: column;
    height: auto;
    gap: 15px;
  }
  .search-container {
    margin: 15px 0;
    max-width: 100%;
  }
  .merchant-button-container {
    margin: 10px 0;
  }
  .cart-button-container {
    margin: 10px 0;
  }
  .logo h1 {
    font-size: 24px;
  }
  .home-icon {
    font-size: 20px;
  }
  .cart-btn {
    width: 45px;
    height: 45px;
  }
  .cart-icon {
    font-size: 20px;
  }
}
@media (max-width: 480px) {
  .user-actions {
    flex-wrap: wrap;
    justify-content: center;
  }
  .auth-btn, .user-center-btn, .merchant-btn {
    padding: 8px 16px;
    font-size: 12px;
  }
}
</style>