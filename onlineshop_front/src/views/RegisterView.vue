<template>
  <div class="register-container">
    <div class="register-form-wrapper">
      <div class="register-form">
        <h2>用户注册</h2>
        <form @submit.prevent="handleRegister">
          <div class="form-group">
            <label for="nickname">用户名</label>
            <input
                id="nickname"
                v-model="form.nickname"
                type="text"
                placeholder="请输入用户名（2-20个字符）"
                required
                @blur="validateNickname"
                :class="{ 'error': errors.nickname }"
            >
            <div v-if="errors.nickname" class="error-message">{{ errors.nickname }}</div>
            <div v-if="nicknameChecking" class="checking-message">检查用户名中...</div>
          </div>
          <div class="form-group">
            <label for="email">邮箱</label>
            <input
                id="email"
                v-model="form.email"
                type="email"
                placeholder="请输入邮箱地址"
                required
                @blur="validateEmail"
                :class="{ 'error': errors.email }"
            >
            <div v-if="errors.email" class="error-message">{{ errors.email }}</div>
            <div v-if="emailChecking" class="checking-message">检查邮箱中...</div>
          </div>
          <div class="form-group">
            <label for="password">密码</label>
            <input
                id="password"
                v-model="form.password"
                type="password"
                placeholder="请输入密码（6-20位，包含大小写字母、数字和特殊字符）"
                required
                minlength="6"
                maxlength="20"
                @blur="validatePassword"
                @input="validatePassword"
                :class="{ 'error': errors.password }"
            >
            <div v-if="errors.password" class="error-message">{{ errors.password }}</div>
            <div v-if="!errors.password && form.password" class="password-requirements">
              <div :class="{ 'requirement-met': hasLowerCase }">包含小写字母</div>
              <div :class="{ 'requirement-met': hasUpperCase }">包含大写字母</div>
              <div :class="{ 'requirement-met': hasNumber }">包含数字</div>
              <div :class="{ 'requirement-met': hasSpecialChar }">包含特殊字符 @$!%*?&</div>
              <div :class="{ 'requirement-met': hasValidLength }">长度6-20位</div>
            </div>
          </div>
          <div class="form-group">
            <label for="confirmPassword">确认密码</label>
            <input
                id="confirmPassword"
                v-model="form.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                required
                @blur="validateConfirmPassword"
                :class="{ 'error': errors.confirmPassword }"
            >
            <div v-if="errors.confirmPassword" class="error-message">{{ errors.confirmPassword }}</div>
          </div>
          <div class="form-group">
            <label for="address">地址</label>
            <input
                id="address"
                v-model="form.address"
                type="text"
                placeholder="请输入详细地址（如：北京市海淀区）"
                required
                @blur="validateAddress"
                :class="{ 'error': errors.address }"
            >
            <div v-if="errors.address" class="error-message">{{ errors.address }}</div>
          </div>
          <div class="form-group">
            <label for="role">用户角色</label>
            <select
                id="role"
                v-model="form.role"
                class="role-select"
                :class="{ 'error': errors.role }"
            >
              <option value="CUSTOMER">顾客</option>
              <option value="MERCHANT">商家</option>
            </select>
            <div v-if="errors.role" class="error-message">{{ errors.role }}</div>
          </div>
          <button
              type="submit"
              :disabled="loading || !isFormValid"
              class="submit-btn"
          >
            {{ loading ? '注册中...' : '注册' }}
          </button>
          <div v-if="serverError" class="error-message server-error">{{ serverError }}</div>
          <div v-if="successMessage" class="success-message">{{ successMessage }}</div>
        </form>
        <div class="login-link">
          <p>已有账号？ <router-link to="/login">立即登录</router-link></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { authAPI, apiUtils } from '../services/api.js'

export default {
  name: 'RegisterView',
  data() {
    return {
      form: {
        nickname: '',
        email: '',
        password: '',
        confirmPassword: '',
        address: '',
        role: 'CUSTOMER'
      },
      errors: {
        nickname: '',
        email: '',
        password: '',
        confirmPassword: '',
        address: '',
        role: ''
      },
      loading: false,
      nicknameChecking: false,
      emailChecking: false,
      serverError: '',
      successMessage: '',
      hasLowerCase: false,
      hasUpperCase: false,
      hasNumber: false,
      hasSpecialChar: false,
      hasValidLength: false
    }
  },
  computed: {
    isFormValid() {
      return this.form.nickname &&
          this.form.email &&
          this.form.password &&
          this.form.confirmPassword &&
          this.form.address &&
          this.form.password === this.form.confirmPassword &&
          Object.values(this.errors).every(error => error === '') &&
          this.hasLowerCase && this.hasUpperCase && this.hasNumber &&
          this.hasSpecialChar && this.hasValidLength
    }
  },
  methods: {
    async validateNickname() {
      if (!this.form.nickname) {
        this.errors.nickname = '用户名不能为空'
        return
      }
      if (this.form.nickname.length < 2 || this.form.nickname.length > 20) {
        this.errors.nickname = '用户名长度应在2-20个字符之间'
        return
      }
      this.nicknameChecking = true
      this.errors.nickname = ''
      try {
        const response = await authAPI.checkNickname(this.form.nickname)
        if (response.exists) {
          this.errors.nickname = '用户名已存在'
        } else {
          this.errors.nickname = ''
        }
      } catch (error) {
        console.error('检查用户名失败:', error)
        this.errors.nickname = ''
      } finally {
        this.nicknameChecking = false
      }
    },

    async validateEmail() {
      if (!this.form.email) {
        this.errors.email = '邮箱不能为空'
        return
      }
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(this.form.email)) {
        this.errors.email = '请输入有效的邮箱地址'
        return
      }
      this.emailChecking = true
      this.errors.email = ''
      try {
        const response = await authAPI.checkEmail(this.form.email)
        if (response.exists) {
          this.errors.email = '邮箱已被注册'
        } else {
          this.errors.email = ''
        }
      } catch (error) {
        console.error('检查邮箱失败:', error)
        this.errors.email = ''
      } finally {
        this.emailChecking = false
      }
    },

    validatePassword() {
      //更新密码要求状态
      this.hasLowerCase = /[a-z]/.test(this.form.password)
      this.hasUpperCase = /[A-Z]/.test(this.form.password)
      this.hasNumber = /\d/.test(this.form.password)
      this.hasSpecialChar = /[@$!%*?&]/.test(this.form.password)
      this.hasValidLength = this.form.password.length >= 6 && this.form.password.length <= 20
      if (!this.form.password) {
        this.errors.password = '密码不能为空'
        return
      }
      if (this.form.password.length < 6 || this.form.password.length > 20) {
        this.errors.password = '密码长度应在6-20位之间'
        return
      }
      const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{6,20}$/
      if (!passwordRegex.test(this.form.password)) {
        this.errors.password = '密码必须包含大小写字母、数字和特殊字符(@$!%*?&)'
        return
      }
      this.errors.password = ''
    },

    validateConfirmPassword() {
      if (!this.form.confirmPassword) {
        this.errors.confirmPassword = '请确认密码'
      } else if (this.form.password !== this.form.confirmPassword) {
        this.errors.confirmPassword = '两次输入的密码不一致'
      } else {
        this.errors.confirmPassword = ''
      }
    },

    validateAddress() {
      if (!this.form.address) {
        this.errors.address = '地址不能为空'
      } else if (this.form.address.length < 5) {
        this.errors.address = '地址长度至少5个字符'
      } else {
        this.errors.address = ''
      }
    },

    validateRole() {
      if (!this.form.role) {
        this.errors.role = '请选择用户角色'
      } else {
        this.errors.role = ''
      }
    },

    async handleRegister() {
      //验证所有字段
      this.validateNickname()
      this.validateEmail()
      this.validatePassword()
      this.validateConfirmPassword()
      this.validateAddress()
      this.validateRole()
      //如果有验证错误，不提交
      if (Object.values(this.errors).some(error => error !== '')) {
        this.serverError = '请完善表单信息'
        return
      }
      if (!this.isFormValid) {
        this.serverError = '请完善表单信息'
        return
      }
      this.loading = true
      this.serverError = ''
      this.successMessage = ''
      try {
        const userData = {
          nickname: this.form.nickname,
          email: this.form.email,
          password: this.form.password,
          address: this.form.address,
          role: this.form.role
        }
        const response = await authAPI.register(userData)
        if (response && response.success) {
          this.successMessage = '注册成功！正在跳转到首页...'
          if (response.user) {
            const processedUser = {
              uid: response.user.uid,
              nickname: response.user.nickname,
              email: response.user.email,
              address: response.user.address,
              avatarUrl: response.user.avatarUrl || '/uploads/avatars/default-avatar.png',
              role: response.user.role || 'CUSTOMER'
            }
            apiUtils.setCurrentUser(processedUser)
          }
          setTimeout(() => {
            this.$router.push('/')
          }, 2000)
        } else {
          throw new Error(response?.message || '注册失败')
        }
      } catch (error) {
        console.error('注册错误:', error)
        if (error.response) {
          const errorData = error.response.data
          this.serverError = errorData?.message || errorData || '注册失败，请重试'
        } else if (error.message) {
          this.serverError = error.message
        } else {
          this.serverError = '注册失败，请检查网络连接后重试'
        }
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}
.register-form-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 80px);
  padding: 2rem 0;
}
.register-form {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 450px;
}
h2 {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #333;
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
input, select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.3s;
  box-sizing: border-box;
}
input:focus, select:focus {
  outline: none;
  border-color: #409eff;
}
input.error, select.error {
  border-color: #f56c6c;
}
.error-message {
  color: #f56c6c;
  font-size: 0.875rem;
  margin-top: 0.5rem;
}
.checking-message {
  color: #666;
  font-size: 0.875rem;
  margin-top: 0.5rem;
}
.server-error {
  text-align: center;
  margin-top: 1rem;
  padding: 0.75rem;
  background-color: #fef0f0;
  border: 1px solid #f56c6c;
  border-radius: 4px;
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
.role-select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  appearance: none;
  background-image: url("data:image/svg+xml;charset=US-ASCII,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 4 5'><path fill='%23333' d='M2 0L0 2h4zm0 5L0 3h4z'/></svg>");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-size: 0.65rem;
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
.login-link {
  text-align: center;
  margin-top: 1rem;
  color: #666;
}
.login-link a {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}
.login-link a:hover {
  text-decoration: underline;
}
.password-requirements {
  margin-top: 0.5rem;
  font-size: 0.75rem;
}
.password-requirements div {
  margin: 0.2rem 0;
  color: #666;
}
.requirement-met {
  color: #67c23a;
  font-weight: 500;
}
@media (max-width: 768px) {
  .register-form-wrapper {
    padding: 1rem;
  }
  .register-form {
    padding: 1.5rem;
    margin: 0 1rem;
  }
}
@media (max-width: 480px) {
  .register-form {
    padding: 1rem;
  }
  h2 {
    font-size: 1.5rem;
  }
}
</style>