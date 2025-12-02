<template>
  <div class="login-container">
    <div class="login-form">
      <h2>用户登录</h2>
      <div class="login-tabs">
        <button
            :class="['tab-button', { active: loginType === 'name' }]"
            @click="loginType = 'name'; clearForm()"
        >
          用户名登录
        </button>
        <button
            :class="['tab-button', { active: loginType === 'email' }]"
            @click="loginType = 'email'; clearForm()"
        >
          邮箱登录
        </button>
      </div>
      <form @submit.prevent="handleLogin">
        <div v-if="loginType === 'name'" class="form-group">
          <label for="nickname">用户名</label>
          <input
              id="nickname"
              v-model="nameForm.nickname"
              type="text"
              placeholder="请输入用户名"
              required
          >
        </div>
        <div v-if="loginType === 'email'" class="form-group">
          <label for="email">邮箱</label>
          <input
              id="email"
              v-model="emailForm.email"
              type="email"
              placeholder="请输入邮箱"
              required
          >
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input
              id="password"
              v-model="password"
              type="password"
              placeholder="请输入密码"
              required
          >
        </div>
        <button
            type="submit"
            :disabled="loading"
            class="submit-btn"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <div v-if="error" class="error-message">{{ error }}</div>
        <div v-if="successMessage" class="success-message">{{ successMessage }}</div>
      </form>
      <div class="register-link">
        <p>还没有账号？ <router-link to="/register">立即注册</router-link></p>
      </div>
    </div>
  </div>
</template>

<script>
import { authAPI, apiUtils } from '../services/api.js';

export default {
  name: 'LoginView',
  data() {
    return {
      loginType: 'email',
      nameForm: {
        nickname: '',
      },
      emailForm: {
        email: '',
      },
      password: '',
      loading: false,
      error: '',
      successMessage: ''
    }
  },
  methods: {
    clearForm() {
      this.password = ''
      this.error = ''
      this.successMessage = ''
    },

    async handleLogin() {
      this.loading = true
      this.error = ''
      this.successMessage = ''
      try {
        if (!this.password) {
          throw new Error('请输入密码')
        }
        let response;
        if (this.loginType === 'name') {
          if (!this.nameForm.nickname) {
            throw new Error('请输入用户名')
          }
          response = await authAPI.loginByName({
            nickname: this.nameForm.nickname,
            password: this.password
          });
        } else {
          if (!this.emailForm.email) {
            throw new Error('请输入邮箱')
          }
          response = await authAPI.loginByEmail({
            email: this.emailForm.email,
            password: this.password
          });
        }
        console.log('登录响应完整数据:', response)
        if (response && response.success) {
          const userData = response.user || response.data
          if (!userData) {
            console.error('用户数据为空，响应结构:', response)
            throw new Error('服务器返回的用户数据格式错误')
          }
          const processedUser = {
            uid: userData.uid || userData.id,
            nickname: userData.nickname || '用户',
            email: userData.email || '',
            address: userData.address || '',
            avatarUrl: userData.avatarUrl || '/uploads/avatars/default-avatar.png',
            role: userData.role || 'CUSTOMER'
          }
          console.log('处理后的用户数据:', processedUser)
          apiUtils.setCurrentUser(processedUser)
          this.successMessage = `登录成功！欢迎回来，${processedUser.nickname}`
          console.log('登录成功，准备跳转...')
          setTimeout(() => {
            const redirect = this.$route.query.redirect
            if (redirect && redirect !== '/login' && redirect !== '/') {
              console.log('跳转到来源页面:', redirect)
              this.$router.push(redirect)
            } else {
              console.log('跳转到首页')
              this.$router.push('/')
            }
          }, 1000)

        } else {
          throw new Error(response?.message || '登录失败')
        }
      } catch (error) {
        console.error('登录错误详情:', error)
        if (error.response) {
          const errorData = error.response.data
          this.error = errorData?.message || errorData || '登录失败，请检查用户名/邮箱和密码'
        } else if (error.message) {
          this.error = error.message
        } else {
          this.error = '登录失败，请重试'
        }
        apiUtils.clearUser();
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}
.login-form {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}
h2 {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #333;
}
.login-tabs {
  display: flex;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid #ddd;
}
.tab-button {
  flex: 1;
  padding: 0.75rem;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.3s;
}
.tab-button.active {
  border-bottom-color: #409eff;
  color: #409eff;
  font-weight: bold;
}
.tab-button:hover {
  background-color: #f5f5f5;
}
.form-group {
  margin-bottom: 1rem;
}
label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
}
input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.3s;
}
input:focus {
  outline: none;
  border-color: #409eff;
}
.submit-btn {
  width: 100%;
  padding: 0.75rem;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  margin-top: 1rem;
  transition: background-color 0.3s;
}
.submit-btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}
.submit-btn:hover:not(:disabled) {
  background-color: #66b1ff;
}
.error-message {
  color: #f56c6c;
  font-size: 0.875rem;
  margin-top: 1rem;
  text-align: center;
  padding: 0.5rem;
  background-color: #fef0f0;
  border-radius: 4px;
  border: 1px solid #f56c6c;
}
.success-message {
  color: #67c23a;
  font-size: 0.875rem;
  margin-top: 1rem;
  text-align: center;
  padding: 0.5rem;
  background-color: #f0f9eb;
  border-radius: 4px;
  border: 1px solid #e1f3d8;
}
.register-link {
  text-align: center;
  margin-top: 1rem;
  color: #666;
}
.register-link a {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}
.register-link a:hover {
  text-decoration: underline;
}
</style>