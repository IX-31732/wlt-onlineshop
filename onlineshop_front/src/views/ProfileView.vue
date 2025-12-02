<template>
  <div class="profile-container">
    <div class="profile-header">
      <h2>用户中心</h2>
    </div>
    <div class="profile-content">
      <!-- 用户信息卡片 -->
      <div class="user-info-card">
        <h3>个人信息</h3>
        <!-- 添加头像上传区域 -->
        <div class="avatar-section">
          <div class="avatar-container">
            <img :src="getAvatarUrl()" class="user-avatar" @error="handleAvatarError">
            <div class="avatar-actions">
              <input type="file" ref="avatarInput" @change="handleAvatarUpload"
                     accept="image/*" style="display: none">
              <button @click="triggerAvatarUpload" class="avatar-upload-btn" :disabled="uploadingAvatar">
                {{ uploadingAvatar ? '上传中...' : '更换头像' }}
              </button>
            </div>
          </div>
        </div>
        <div class="user-info">
          <div class="info-item">
            <label>用户ID：</label>
            <span class="uid">{{ currentUser.uid }}</span>
          </div>
          <div class="info-item">
            <label>昵称：</label>
            <div class="editable-field">
              <span v-if="!editingNickname">{{ currentUser.nickname }}</span>
              <input
                  v-else
                  v-model="editForm.nickname"
                  type="text"
                  class="edit-input"
              >
              <button
                  @click="toggleEdit('nickname')"
                  class="edit-btn"
              >
                {{ editingNickname ? '保存' : '修改' }}
              </button>
              <button
                  v-if="editingNickname"
                  @click="cancelEdit('nickname')"
                  class="cancel-btn"
              >
                取消
              </button>
            </div>
          </div>
          <div class="info-item">
            <label>邮箱：</label>
            <div class="editable-field">
              <span v-if="!editingEmail">{{ currentUser.email }}</span>
              <input
                  v-else
                  v-model="editForm.email"
                  type="email"
                  class="edit-input"
              >
              <button
                  @click="toggleEdit('email')"
                  class="edit-btn"
              >
                {{ editingEmail ? '保存' : '修改' }}
              </button>
              <button
                  v-if="editingEmail"
                  @click="cancelEdit('email')"
                  class="cancel-btn"
              >
                取消
              </button>
            </div>
          </div>
          <div class="info-item">
            <label>地址：</label>
            <div class="editable-field">
              <span v-if="!editingAddress">{{ currentUser.address || '暂无地址' }}</span>
              <input
                  v-else
                  v-model="editForm.address"
                  type="text"
                  class="edit-input"
                  placeholder="请输入地址"
              >
              <button
                  @click="toggleEdit('address')"
                  class="edit-btn"
              >
                {{ editingAddress ? '保存' : '修改' }}
              </button>
              <button
                  v-if="editingAddress"
                  @click="cancelEdit('address')"
                  class="cancel-btn"
              >
                取消
              </button>
            </div>
          </div>
          <div class="info-item">
            <label>密码：</label>
            <div class="editable-field">
              <span>********</span>
              <button
                  @click="toggleEdit('password')"
                  class="edit-btn"
              >
                {{ editingPassword ? '保存' : '修改' }}
              </button>
              <button
                  v-if="editingPassword"
                  @click="cancelEdit('password')"
                  class="cancel-btn"
              >
                取消
              </button>
            </div>
          </div>
        </div>
        <!-- 注销账号按钮 -->
        <div class="account-actions">
          <button
              @click="deleteAccount"
              class="delete-account-btn"
              :disabled="uploadingAvatar || editingNickname || editingEmail || editingAddress || editingPassword"
          >
            注销账号
          </button>
        </div>
      </div>
      <!-- 密码修改表单 -->
      <div v-if="editingPassword" class="password-form">
        <h4>修改密码</h4>
        <div class="form-group">
          <label>新密码：</label>
          <input
              v-model="editForm.newPassword"
              type="password"
              placeholder="请输入新密码（6-20位，包含大小写字母、数字和特殊字符）"
              class="password-input"
              @blur="validateNewPassword"
              @input="validateNewPassword"
              maxlength="20"
          >
        </div>
        <div v-if="editForm.newPassword" class="password-requirements">
          <div :class="{ 'requirement-met': hasLowerCase }">包含小写字母</div>
          <div :class="{ 'requirement-met': hasUpperCase }">包含大写字母</div>
          <div :class="{ 'requirement-met': hasNumber }">包含数字</div>
          <div :class="{ 'requirement-met': hasSpecialChar }">包含特殊字符 @$!%*?&</div>
          <div :class="{ 'requirement-met': hasValidLength }">长度6-20位</div>
        </div>
        <div class="form-group">
          <label>确认密码：</label>
          <input
              v-model="editForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              class="password-input"
              @blur="validateConfirmPassword"
          >
        </div>
        <div v-if="editForm.newPassword && editForm.confirmPassword && editForm.newPassword !== editForm.confirmPassword"
             class="error-message">
          两次输入的密码不一致
        </div>
        <div v-if="passwordError" class="error-message">{{ passwordError }}</div>
      </div>
      <!-- 订单管理 -->
      <div class="orders-section">
        <h3>我的订单</h3>
        <!-- 加载状态 -->
        <div v-if="loadingOrders" class="loading-orders">
          <p>正在加载订单数据...</p>
        </div>
        <!-- 错误状态 -->
        <div v-else-if="orderError" class="error-state">
          <p>加载订单失败: {{ orderError }}</p>
          <button @click="loadOrders" class="retry-btn">重试</button>
        </div>
        <!-- 订单状态筛选 -->
        <div v-else class="order-filters">
          <button
              v-for="filter in orderFilters"
              :key="filter.status"
              @click="setOrderFilter(filter.status)"
              :class="['filter-btn', { active: currentFilter === filter.status }]"
          >
            {{ filter.label }}
          </button>
        </div>
        <!-- 订单列表 -->
        <div v-if="!loadingOrders && !orderError && filteredOrders.length > 0" class="orders-list">
          <div v-for="order in filteredOrders" :key="order.oid" class="order-card">
            <div class="order-header">
              <span class="order-id">订单号：{{ order.oid }}</span>
              <span class="order-status" :class="getStatusClass(order.status)">
                {{ getStatusText(order.status) }}
              </span>
            </div>
            <div class="order-content">
              <div class="order-items">
                <div v-for="item in order.items" :key="item.gid" class="order-item">
                  <div class="item-image">📦📦</div>
                  <div class="item-info">
                    <span class="item-name">{{ item.name }}</span>
                    <span class="item-quantity">× {{ item.quantity }}</span>
                  </div>
                  <div class="item-status">
                    <span v-if="isShipped(order.status) && item.shipped" class="shipped">
                      已发货
                    </span>
                    <span v-else-if="isShipped(order.status)" class="shipping">
                      发货中...
                    </span>
                  </div>
                </div>
              </div>
              <div class="order-footer">
                <div class="order-total">
                  总计：¥{{ order.totalAmount.toFixed(2) }}
                </div>
                <div class="order-actions">
                  <!-- 新增：查看详情按钮 -->
                  <button
                      @click="viewOrderDetails(order.oid)"
                      class="action-btn view-btn"
                  >
                    查看详情
                  </button>
                  <button
                      v-if="isPending(order.status)"
                      @click="cancelOrder(order.oid)"
                      class="action-btn cancel-btn"
                  >
                    取消订单
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else-if="!loadingOrders && !orderError" class="no-orders">
          <p>暂无{{ getCurrentFilterLabel() }}的订单</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { orderAPI, apiUtils, authAPI, uploadAPI } from '../services/api'

export default {
  name: 'ProfileView',
  data() {
    return {
      currentUser: {},
      editingNickname: false,
      editingEmail: false,
      editingAddress: false,
      editingPassword: false,
      uploadingAvatar: false,
      editForm: {
        nickname: '',
        email: '',
        address: '',
        newPassword: '',
        confirmPassword: ''
      },
      //密码要求状态
      hasLowerCase: false,
      hasUpperCase: false,
      hasNumber: false,
      hasSpecialChar: false,
      hasValidLength: false,
      passwordError: '',
      loadingOrders: false,
      orderError: '',
      orders: [],
      currentFilter: 'ALL',
      orderFilters: [
        { status: 'ALL', label: '全部订单' },
        { status: 'PENDING', label: '待付款' },
        { status: 'PAID', label: '已付款' },
        { status: 'SHIPPED', label: '已发货' },
        { status: 'COMPLETED', label: '已完成' },
        { status: 'CANCELLED', label: '已取消' }
      ]
    }
  },
  computed: {
    filteredOrders() {
      if (this.currentFilter === 'ALL') {
        return this.orders
      }
      //处理中英文状态值匹配问题
      return this.orders.filter(order => {
        const orderStatus = order.status || ''
        const filterStatus = this.currentFilter
        //英文状态 -> 中文状态
        const statusMap = {
          'PENDING': '待付款',
          'PAID': '已付款',
          'SHIPPED': '已发货',
          'COMPLETED': '已完成',
          'CANCELLED': '已取消'
        }
        //检查订单状态是否匹配筛选条件
        return orderStatus === filterStatus ||
            orderStatus === statusMap[filterStatus] ||
            (filterStatus === 'PENDING' && orderStatus === '待付款') ||
            (filterStatus === 'PAID' && orderStatus === '已付款') ||
            (filterStatus === 'SHIPPED' && orderStatus === '已发货') ||
            (filterStatus === 'COMPLETED' && orderStatus === '已完成') ||
            (filterStatus === 'CANCELLED' && orderStatus === '已取消')
      })
    },
    //检查密码是否有效
    isPasswordValid() {
      return this.hasLowerCase && this.hasUpperCase && this.hasNumber &&
          this.hasSpecialChar && this.hasValidLength
    }
  },
  async mounted() {
    await this.loadUserInfo()
    await this.loadOrders()
  },
  methods: {
    //加载用户信息
    async loadUserInfo() {
      try {
        //从后端获取最新用户信息，而不是只从本地存储
        const response = await authAPI.getCurrentUser()
        const data = apiUtils.handleResponse(response)
        if (data.user) {
          this.currentUser = data.user
          this.editForm.nickname = data.user.nickname || ''
          this.editForm.email = data.user.email || ''
          this.editForm.address = data.user.address || ''
          //保存到本地存储
          apiUtils.setCurrentUser(data.user)
        }
      } catch (error) {
        console.error('加载用户信息失败:', error)
        //失败时使用本地存储的备份数据
        const user = apiUtils.getCurrentUser()
        if (user) {
          this.currentUser = { ...user }
          this.editForm.nickname = user.nickname || ''
          this.editForm.email = user.email || ''
          this.editForm.address = user.address || ''
        }
      }
    },
    //获取头像URL
    getAvatarUrl() {
      if (this.currentUser.avatarUrl) {
        if (this.currentUser.avatarUrl.startsWith('http')) {
          return this.currentUser.avatarUrl
        }
        return `${this.currentUser.avatarUrl}`
      }
      return '/uploads/avatars/defaultpicture.png'
    },
    //头像加载失败处理
    handleAvatarError(event) {
      event.target.src = '/uploads/avatars/defaultpicture.png'
    },
    //触发头像上传
    triggerAvatarUpload() {
      this.$refs.avatarInput.click()
    },
    //处理头像上传
    async handleAvatarUpload(event) {
      const file = event.target.files[0]
      if (!file) return
      //验证文件类型和大小
      if (!file.type.startsWith('image/')) {
        alert('请选择图片文件')
        return
      }
      if (file.size > 5 * 1024 * 1024) { // 5MB限制
        alert('图片大小不能超过5MB')
        return
      }
      this.uploadingAvatar = true
      try {
        //上传头像
        const uploadResponse = await uploadAPI.uploadAvatar(file)
        const uploadData = apiUtils.handleResponse(uploadResponse)
        //更新用户信息中的头像URL
        await authAPI.updateProfile({
          nickname: this.currentUser.nickname,
          email: this.currentUser.email,
          address: this.currentUser.address || '',
          avatarUrl: uploadData.fileUrl
        })
        //更新本地用户信息
        this.currentUser.avatarUrl = uploadData.fileUrl
        apiUtils.setCurrentUser(this.currentUser)
        alert('头像上传成功')
      } catch (error) {
        console.error('头像上传失败:', error)
        alert('头像上传失败: ' + (error.message || '未知错误'))
      } finally {
        this.uploadingAvatar = false
        //清空文件输入
        event.target.value = ''
      }
    },

    //切换编辑状态
    toggleEdit(field) {
      if (field === 'nickname') {
        if (this.editingNickname) {
          this.saveNickname()
        } else {
          this.editingNickname = true
          this.editForm.nickname = this.currentUser.nickname
        }
      } else if (field === 'email') {
        if (this.editingEmail) {
          this.saveEmail()
        } else {
          this.editingEmail = true
          this.editForm.email = this.currentUser.email
        }
      } else if (field === 'address') {
        if (this.editingAddress) {
          this.saveAddress()
        } else {
          this.editingAddress = true
          this.editForm.address = this.currentUser.address || ''
        }
      } else if (field === 'password') {
        if (this.editingPassword) {
          this.savePassword()
        } else {
          this.editingPassword = true
          this.editForm.newPassword = ''
          this.editForm.confirmPassword = ''
          this.passwordError = ''
          //重置密码要求状态
          this.resetPasswordRequirements()
        }
      }
    },

    //取消编辑
    cancelEdit(field) {
      if (field === 'nickname') {
        this.editingNickname = false
        this.editForm.nickname = this.currentUser.nickname
      } else if (field === 'email') {
        this.editingEmail = false
        this.editForm.email = this.currentUser.email
      } else if (field === 'address') {
        this.editingAddress = false
        this.editForm.address = this.currentUser.address || ''
      } else if (field === 'password') {
        this.editingPassword = false
        this.editForm.newPassword = ''
        this.editForm.confirmPassword = ''
        this.passwordError = ''
        this.resetPasswordRequirements()
      }
    },

    //重置密码要求状态
    resetPasswordRequirements() {
      this.hasLowerCase = false
      this.hasUpperCase = false
      this.hasNumber = false
      this.hasSpecialChar = false
      this.hasValidLength = false
    },

    //验证新密码复杂度
    validateNewPassword() {
      this.passwordError = ''
      //更新密码要求状态
      this.hasLowerCase = /[a-z]/.test(this.editForm.newPassword)
      this.hasUpperCase = /[A-Z]/.test(this.editForm.newPassword)
      this.hasNumber = /\d/.test(this.editForm.newPassword)
      this.hasSpecialChar = /[@$!%*?&]/.test(this.editForm.newPassword)
      this.hasValidLength = this.editForm.newPassword.length >= 6 && this.editForm.newPassword.length <= 20
      if (!this.editForm.newPassword) {
        return
      }
      if (this.editForm.newPassword.length < 6 || this.editForm.newPassword.length > 20) {
        this.passwordError = '密码长度应在6-20位之间'
        return
      }
      //使用与后端相同的正则表达式
      const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{6,20}$/
      if (!passwordRegex.test(this.editForm.newPassword)) {
        this.passwordError = '密码必须包含大小写字母、数字和特殊字符(@$!%*?&)'
        return
      }
    },

    //验证确认密码
    validateConfirmPassword() {
      if (this.editForm.newPassword && this.editForm.confirmPassword &&
          this.editForm.newPassword !== this.editForm.confirmPassword) {
        this.passwordError = '两次输入的密码不一致'
      } else {
        this.passwordError = ''
      }
    },

    //保存昵称
    async saveNickname() {
      if (!this.editForm.nickname.trim()) {
        alert('昵称不能为空')
        return
      }
      try {
        await authAPI.updateProfile({
          nickname: this.editForm.nickname,
          email: this.currentUser.email,
          address: this.currentUser.address || '',
          avatarUrl: this.currentUser.avatarUrl || ''
        })
        this.currentUser.nickname = this.editForm.nickname
        apiUtils.setCurrentUser(this.currentUser)
        this.editingNickname = false
        alert('昵称修改成功')
      } catch (error) {
        console.error('修改昵称失败:', error)
        alert('修改昵称失败: ' + (error.message || '未知错误'))
      }
    },
    //保存邮箱
    async saveEmail() {
      if (!this.editForm.email.trim()) {
        alert('邮箱不能为空')
        return
      }
      //简单的邮箱格式验证
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(this.editForm.email)) {
        alert('请输入有效的邮箱地址')
        return
      }
      try {
        await authAPI.updateProfile({
          nickname: this.currentUser.nickname,
          email: this.editForm.email,
          address: this.currentUser.address || '',
          avatarUrl: this.currentUser.avatarUrl || ''
        })
        this.currentUser.email = this.editForm.email
        apiUtils.setCurrentUser(this.currentUser)
        this.editingEmail = false
        alert('邮箱修改成功')
      } catch (error) {
        console.error('修改邮箱失败:', error)
        alert('修改邮箱失败: ' + (error.message || '未知错误'))
      }
    },

    //保存地址
    async saveAddress() {
      if (!this.editForm.address.trim()) {
        alert('地址不能为空')
        return
      }
      try {
        await authAPI.updateProfile({
          nickname: this.currentUser.nickname,
          email: this.currentUser.email,
          address: this.editForm.address,
          avatarUrl: this.currentUser.avatarUrl || ''
        })
        this.currentUser.address = this.editForm.address
        apiUtils.setCurrentUser(this.currentUser)
        this.editingAddress = false
        alert('地址修改成功')
      } catch (error) {
        console.error('修改地址失败:', error)
        alert('修改地址失败: ' + (error.message || '未知错误'))
      }
    },

    //保存密码
    async savePassword() {
      //先验证密码
      this.validateNewPassword()
      this.validateConfirmPassword()
      if (this.passwordError) {
        alert('请先修正密码错误')
        return
      }
      if (!this.editForm.newPassword) {
        alert('请输入新密码')
        return
      }
      if (!this.isPasswordValid) {
        alert('密码不符合复杂度要求')
        return
      }
      if (this.editForm.newPassword !== this.editForm.confirmPassword) {
        alert('两次输入的密码不一致')
        return
      }
      try {
        await authAPI.updateProfile({
          nickname: this.currentUser.nickname,
          email: this.currentUser.email,
          address: this.currentUser.address || '',
          avatarUrl: this.currentUser.avatarUrl || '',
          password: this.editForm.newPassword
        })
        this.editingPassword = false
        this.editForm.newPassword = ''
        this.editForm.confirmPassword = ''
        this.passwordError = ''
        this.resetPasswordRequirements()
        alert('密码修改成功')
      } catch (error) {
        console.error('修改密码失败:', error)
        alert('修改密码失败: ' + (error.message || '未知错误'))
      }
    },

    //加载订单
    async loadOrders() {
      this.loadingOrders = true
      this.orderError = ''
      try {
        const response = await orderAPI.getMyOrders()
        const data = apiUtils.handleResponse(response)
        console.log('原始订单数据:', data)
        if (data.orders && Array.isArray(data.orders)) {
          this.orders = data.orders
        } else if (Array.isArray(data)) {
          this.orders = data
        } else {
          this.orders = []
        }
        //确保订单有正确的状态字段和数字类型
        this.orders = this.orders.map(order => {
          //确保金额是数字类型
          if (order.totalAmount) {
            order.totalAmount = parseFloat(order.totalAmount)
          } else {
            //如果没有总金额，计算商品总价
            const items = order.items || order.orderItems || []
            order.totalAmount = items.reduce((total, item) => {
              const price = parseFloat(item.price || item.priceAtPurchase || 0)
              const quantity = parseInt(item.quantity) || 1
              return total + (price * quantity)
            }, 0)
          }
          //确保商品数据格式正确
          if (order.items) {
            order.items = order.items.map(item => ({
              ...item,
              price: parseFloat(item.price || item.priceAtPurchase) || 0,
              quantity: parseInt(item.quantity) || 1,
              name: item.name || `商品${item.gid || item.id}`,
              description: item.description || '暂无描述'
            }))
          }
          return order
        })
        console.log('处理后的订单数据:', this.orders)
      } catch (error) {
        console.error('加载订单失败:', error)
        this.orderError = error.message || '加载订单失败'
      } finally {
        this.loadingOrders = false
      }
    },

    //设置订单筛选
    setOrderFilter(status) {
      this.currentFilter = status
    },

    //获取当前筛选标签
    getCurrentFilterLabel() {
      const filter = this.orderFilters.find(f => f.status === this.currentFilter)
      return filter ? filter.label : ''
    },

    //获取订单状态文本
    getStatusText(status) {
      const statusMap = {
        'PENDING': '待付款',
        '待付款': '待付款',
        'PAID': '已付款',
        '已付款': '已付款',
        'SHIPPED': '已发货',
        '已发货': '已发货',
        'COMPLETED': '已完成',
        '已完成': '已完成',
        'CANCELLED': '已取消',
        '已取消': '已取消'
      }
      return statusMap[status] || status || '未知状态'
    },

    //获取订单状态样式类
    getStatusClass(status) {
      const classMap = {
        'PENDING': 'status-pending',
        '待付款': 'status-pending',
        'PAID': 'status-paid',
        '已付款': 'status-paid',
        'SHIPPED': 'status-shipped',
        '已发货': 'status-shipped',
        'COMPLETED': 'status-completed',
        '已完成': 'status-completed',
        'CANCELLED': 'status-cancelled',
        '已取消': 'status-cancelled'
      }
      return classMap[status] || 'status-unknown'
    },

    //状态判断辅助方法
    isPending(status) {
      return status === 'PENDING' || status === '待付款'
    },

    isShipped(status) {
      return status === 'SHIPPED' || status === '已发货' ||
          status === 'COMPLETED' || status === '已完成'
    },

    //查看订单详情
    viewOrderDetails(orderId) {
      console.log('查看订单详情，订单ID:', orderId)
      this.$router.push(`/order/${orderId}`)
    },

    //取消订单
    async cancelOrder(orderId) {
      if (!confirm('确定要取消这个订单吗？')) {
        return
      }
      try {
        await orderAPI.cancelOrder(orderId)
        alert('订单取消成功')
        await this.loadOrders() // 重新加载订单列表
      } catch (error) {
        console.error('取消订单失败:', error)
        alert('取消订单失败: ' + (error.message || '未知错误'))
      }
    },

    //注销账号方法
    async deleteAccount() {
      if (!confirm('确定要注销账号吗？此操作不可逆，所有数据将被永久删除！')) {
        return;
      }
      //如果是商家，再次确认
      if (this.currentUser.role === 'MERCHANT') {
        const confirmMessage = '您是商家账号，注销前需要下架所有商品。确定继续吗？';
        if (!confirm(confirmMessage)) {
          return;
        }
      }
      try {
        await authAPI.deleteAccount();
        alert('账号注销成功');
        //清除本地存储
        apiUtils.clearUser();
        //跳转到首页
        this.$router.push('/');
      } catch (error) {
        console.error('注销失败:', error);
        if (error.message && error.message.includes('下架所有商品')) {
          alert('注销失败：' + error.message + '\n请先下架所有商品后再尝试注销。');
        } else {
          alert('注销失败：' + (error.message || '未知错误'));
        }
      }
    }
  }
}
</script>

<style scoped>
.profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.profile-header {
  text-align: center;
  margin-bottom: 30px;
}
.profile-header h2 {
  color: #333;
  font-size: 28px;
}
.profile-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}
.user-info-card {
  background: white;
  padding: 25px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.user-info-card h3 {
  margin-bottom: 20px;
  color: #333;
  border-bottom: 2px solid #e4393c;
  padding-bottom: 10px;
}
.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}
.avatar-container {
  text-align: center;
}
.user-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #e4393c;
  margin-bottom: 10px;
}
.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.avatar-upload-btn {
  padding: 8px 16px;
  background: #e4393c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}
.avatar-upload-btn:hover {
  background: #c03537;
}
.avatar-upload-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.user-info {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.info-item label {
  font-weight: bold;
  min-width: 80px;
  color: #666;
}
.info-item .uid {
  font-family: monospace;
  color: #999;
}
.editable-field {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}
.edit-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  flex: 1;
}
.edit-btn, .cancel-btn {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
}
.edit-btn:hover {
  background: #e4393c;
  color: white;
  border-color: #e4393c;
}
.cancel-btn:hover {
  background: #666;
  color: white;
  border-color: #666;
}
.password-form {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-top: 20px;
}
.password-form h4 {
  margin-bottom: 15px;
  color: #333;
}
.form-group {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}
.form-group label {
  min-width: 80px;
  font-weight: bold;
}
.password-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  width: 300px;
}
.error-message {
  color: #e4393c;
  font-size: 14px;
  margin-top: 10px;
}
.password-requirements {
  margin-top: 0.5rem;
  font-size: 0.75rem;
  margin-bottom: 1rem;
}
.password-requirements div {
  margin: 0.2rem 0;
  color: #666;
}
.requirement-met {
  color: #67c23a;
  font-weight: 500;
}
.orders-section {
  background: white;
  padding: 25px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.orders-section h3 {
  margin-bottom: 20px;
  color: #333;
  border-bottom: 2px solid #e4393c;
  padding-bottom: 10px;
}
.loading-orders, .error-state {
  text-align: center;
  padding: 40px;
  color: #666;
}
.retry-btn {
  padding: 8px 16px;
  background: #e4393c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 10px;
}
.retry-btn:hover {
  background: #c03537;
}
.order-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.filter-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
}
.filter-btn.active, .filter-btn:hover {
  background: #e4393c;
  color: white;
  border-color: #e4393c;
}
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.order-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
}
.order-id {
  font-weight: bold;
  color: #333;
}
.order-status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}
.status-pending { background: #fff3cd; color: #856404; }
.status-paid { background: #d1ecf1; color: #0c5460; }
.status-shipped { background: #d4edda; color: #155724; }
.status-completed { background: #d1ecf1; color: #0c5460; }
.status-cancelled { background: #f8d7da; color: #721c24; }
.order-content {
  padding: 20px;
}
.order-items {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 20px;
}
.order-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
}
.item-image {
  width: 50px;
  height: 50px;
  background: #ddd;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.item-name {
  font-weight: bold;
  color: #333;
}
.item-quantity {
  color: #666;
  font-size: 14px;
}
.item-status .shipped {
  color: #28a745;
  font-weight: bold;
}
.item-status .shipping {
  color: #ffc107;
  font-weight: bold;
}
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #e0e0e0;
}
.order-total {
  font-size: 18px;
  font-weight: bold;
  color: #e4393c;
}
.order-actions {
  display: flex;
  gap: 10px;
}
.action-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}
.pay-btn {
  background: #e4393c;
  color: white;
  border-color: #e4393c;
}
.pay-btn:hover {
  background: #c03537;
}

.view-btn {
  background: #28a745;
  color: white;
  border-color: #28a745;
}
.view-btn:hover {
  background: #218838;
}
.cancel-btn {
  background: #6c757d;
  color: white;
  border-color: #6c757d;
}
.cancel-btn:hover {
  background: #545b62;
}
.no-orders {
  text-align: center;
  padding: 40px;
  color: #666;
  font-size: 16px;
}
@media (max-width: 768px) {
  .profile-container {
    padding: 10px;
  }
  .info-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .editable-field {
    width: 100%;
  }
  .form-group {
    flex-direction: column;
    align-items: flex-start;
  }
  .password-input {
    width: 100%;
  }
  .order-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  .order-footer {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }
  .order-actions {
    width: 100%;
    justify-content: flex-start;
  }
  .account-actions {
    margin-top: 30px;
    padding-top: 20px;
    border-top: 1px solid #f0f0f0;
    text-align: center;
  }
  .delete-account-btn {
    padding: 10px 20px;
    background: #dc3545;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    transition: background 0.3s;
  }
  .delete-account-btn:hover:not(:disabled) {
    background: #c82333;
  }
  .delete-account-btn:disabled {
    background: #ccc;
    cursor: not-allowed;
  }
}
</style>