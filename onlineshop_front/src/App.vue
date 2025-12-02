<!-- src/App.vue -->
<template>
  <div id="app" :class="{ 'dev-environment': isDevelopment }">
    <!-- 只在特定页面显示导航栏 -->
    <NavBar
        v-if="showNavBar"
        :is-logged-in="isLoggedIn"
        :current-user="currentUser"
        @logout="handleLogout"
    />
    <!-- 主内容区域 -->
    <main class="main-content">
      <router-view/>
    </main>
  </div>
</template>

<script>
import NavBar from './components/NavBar.vue'

export default {
  name: 'App',
  components: {
    NavBar
  },
  data() {
    return {
      isLoggedIn: false,
      currentUser: {},
      isDevelopment: process.env.VUE_APP_ENV === 'development' ||
          window.location.hostname === 'localhost' ||
          window.location.hostname === '127.0.0.1'
    }
  },
  computed: {
    // 定义不需要导航栏的页面
    showNavBar() {
      const noNavBarRoutes = ['OrderView', 'ProfileView', 'MerchantView']
      return !noNavBarRoutes.includes(this.$route.name)
    }
  },
  mounted() {
    this.checkAuthStatus()
  },
  watch: {
    '$route'() {
      this.checkAuthStatus()
    }
  },
  methods: {
    checkAuthStatus() {
      try {
        const userStr = localStorage.getItem('currentUser');
        // 修复：正确处理undefined和null的情况
        if (userStr === null || userStr === 'undefined' || userStr === 'null') {
          this.isLoggedIn = false;
          this.currentUser = {};
          return;
        }
        const user = JSON.parse(userStr);
        this.isLoggedIn = !!user;
        this.currentUser = user || {};
      } catch (error) {
        console.error('解析用户信息失败:', error);
        this.isLoggedIn = false;
        this.currentUser = {};
        localStorage.removeItem('currentUser');
      }
    },

    async handleLogout() {
      try {
        if (confirm('确定要退出登录吗？')) {
          localStorage.removeItem('currentUser')
          localStorage.removeItem('userRole')
          localStorage.removeItem('token')
          this.isLoggedIn = false
          this.currentUser = {}
          //使用自定义消息提示
          this.showMessage('您已成功退出登录', 'success')
          this.$router.push('/')
        }
      } catch (error) {
        console.error('退出登录失败:', error)
        this.showMessage('退出登录失败', 'error')
      }
    },
    //自定义消息提示方法
    showMessage(message, type = 'info') {
      //创建消息元素
      const messageEl = document.createElement('div')
      messageEl.className = `custom-message custom-message-${type}`
      messageEl.textContent = message
      //添加样式
      messageEl.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 12px 20px;
        border-radius: 4px;
        color: white;
        font-size: 14px;
        z-index: 10000;
        max-width: 300px;
        animation: slideInRight 0.3s ease-out;
      `
      //根据类型设置背景色
      const bgColors = {
        success: '#67c23a',
        error: '#f56c6c',
        warning: '#e6a23c',
        info: '#909399'
      }
      messageEl.style.backgroundColor = bgColors[type] || bgColors.info
      //添加到页面
      document.body.appendChild(messageEl)
      //3秒后自动移除
      setTimeout(() => {
        messageEl.style.animation = 'slideOutRight 0.3s ease-in'
        setTimeout(() => {
          if (messageEl.parentNode) {
            document.body.removeChild(messageEl)
          }
        }, 300)
      }, 3000)
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Microsoft YaHei', Arial, sans-serif;
  background-color: #f5f5f5;
}

#app {
  min-height: 100vh;
}

.main-content {
  min-height: 100vh;
}

.dev-environment::after {
  content: "开发环境";
  position: fixed;
  bottom: 10px;
  right: 10px;
  background: #e74c3c;
  color: white;
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  z-index: 1000;
}
@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
@keyframes slideOutRight {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(100%);
    opacity: 0;
  }
}
.custom-message {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
</style>