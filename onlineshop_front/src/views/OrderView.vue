<template>
  <div class="order-container">
    <div v-if="loading" class="loading">加载订单信息...</div>
    <div v-else-if="order" class="order-content">
      <!-- 订单头部 -->
      <div class="order-header">
        <h2>订单详情</h2>
        <div class="order-status" :class="getStatusClass(order.status)">
          {{ getStatusText(order.status) }}
        </div>
      </div>
      <!-- 订单信息 -->
      <div class="order-info">
        <div class="info-section">
          <h3>订单信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">订单号：</span>
              <span class="value">{{ order.oid || order.id }}</span>
            </div>
            <div class="info-item">
              <span class="label">下单时间：</span>
              <span class="value">{{ formatDate(order.orderDate) }}</span>
            </div>
            <div v-if="order.payTime" class="info-item">
              <span class="label">付款时间：</span>
              <span class="value">{{ formatDate(order.payTime) }}</span>
            </div>
            <div v-if="order.shipTime" class="info-item">
              <span class="label">发货时间：</span>
              <span class="value">{{ formatDate(order.shipTime) }}</span>
            </div>
            <div v-if="order.completeTime" class="info-item">
              <span class="label">完成时间：</span>
              <span class="value">{{ formatDate(order.completeTime) }}</span>
            </div>
            <div class="info-item">
              <span class="label">订单状态：</span>
              <span class="value" :class="getStatusClass(order.status)">
                {{ getStatusText(order.status) }}
              </span>
            </div>
            <div class="info-item">
              <span class="label">应付金额：</span>
              <span class="value total-amount">¥{{ finalTotalAmount }}</span>
            </div>
          </div>
        </div>
        <!-- 商品列表 -->
        <div class="products-section">
          <h3>商品清单</h3>
          <div class="products-list">
            <div v-for="(item, index) in orderItems" :key="index" class="product-item">
              <div class="product-image">
                <img :src="getProductImage(item)" @error="handleImageError" class="product-img">
              </div>
              <div class="product-info">
                <h4 class="product-name">{{ item.name || `商品${item.gid || item.id}` }}</h4>
                <p class="product-price">¥{{ (item.price || item.priceAtPurchase).toFixed(2) }} × {{ item.quantity }}</p>
                <p class="product-description">{{ item.description || '暂无描述' }}</p>
              </div>
              <div class="product-total">
                ¥{{ ((item.price || item.priceAtPurchase) * item.quantity).toFixed(2) }}
              </div>
            </div>
          </div>
        </div>
        <!-- 价格明细 -->
        <div class="price-section">
          <h3>价格明细</h3>
          <div class="price-details">
            <div class="price-item">
              <span>商品总价：</span>
              <span>¥{{ formattedProductAmount }}</span>
            </div>
            <div class="price-item">
              <span>运费：</span>
              <span>¥{{ shippingFee.toFixed(2) }}</span>
            </div>
            <div class="price-item total">
              <span>应付总额：</span>
              <span class="final-amount">¥{{ finalTotalAmount }}</span>
            </div>
          </div>
        </div>
        <!-- 待付款状态 -->
        <div v-if="order.status === 'PENDING' || order.status === '待付款'" class="payment-section">
          <h3>付款方式</h3>
          <div class="payment-methods">
            <div class="payment-method">
              <input
                  type="radio"
                  id="alipay"
                  value="alipay"
                  v-model="selectedPayment"
              >
              <label for="alipay" class="payment-option">
                <span class="payment-icon">💰</span>
                <span>支付宝支付</span>
              </label>
            </div>
            <div class="payment-method">
              <input
                  type="radio"
                  id="wechat"
                  value="wechat"
                  v-model="selectedPayment"
              >
              <label for="wechat" class="payment-option">
                <span class="payment-icon">💳</span>
                <span>微信支付</span>
              </label>
            </div>
            <div class="payment-method">
              <input
                  type="radio"
                  id="bank"
                  value="bank"
                  v-model="selectedPayment"
              >
              <label for="bank" class="payment-option">
                <span class="payment-icon">🏦</span>
                <span>银行卡支付</span>
              </label>
            </div>
          </div>
          <button
              @click="handlePayment"
              :disabled="!selectedPayment || paying"
              class="payment-btn"
          >
            {{ paying ? '付款中...' : `立即付款 ¥${finalTotalAmount}` }}
          </button>
        </div>
        <!-- 已付款状态 - 等待商家发货 -->
        <div v-if="order.status === 'PAID' || order.status === '已付款'" class="paid-section">
          <div class="paid-header">
            <div class="paid-icon">⏳</div>
            <h3>等待商家发货</h3>
          </div>
          <div class="paid-content">
            <p class="paid-message">您的订单已付款成功，正在等待商家确认并发货</p>
            <div class="waiting-info">
              <div class="waiting-item">
                <span class="waiting-label">当前状态：</span>
                <span class="waiting-value">商家处理中</span>
              </div>
              <div class="waiting-item">
                <span class="waiting-label">预计处理时间：</span>
                <span class="waiting-value">1-3个工作日</span>
              </div>
              <div class="waiting-item">
                <span class="waiting-label">温馨提示：</span>
                <span class="waiting-value">商家确认后会立即为您发货</span>
              </div>
            </div>
            <button @click="refreshOrder" class="refresh-btn">
              🔄 刷新状态
            </button>
          </div>
        </div>
        <!-- 已发货状态 -->
        <div v-if="order.status === 'SHIPPED' || order.status === '已发货'" class="shipped-section">
          <div class="shipped-header">
            <div class="shipped-icon">🚚🚚</div>
            <h3>商品已发货</h3>
          </div>
          <div class="shipped-content">
            <p class="shipped-message">商家已确认发货，您的商品正在运输途中</p>
            <div class="shipping-info">
              <div class="shipping-item">
                <span class="shipping-label">发货时间：</span>
                <span class="shipping-value">{{ formatDate(order.shipTime) }}</span>
              </div>
              <div class="shipping-item">
                <span class="shipping-label">预计送达时间：</span>
                <span class="shipping-value">{{ getEstimatedDeliveryTime() }}</span>
              </div>
              <div class="shipping-item">
                <span class="shipping-label">物流公司：</span>
                <span class="shipping-value">模拟快递</span>
              </div>
              <div class="shipping-item">
                <span class="shipping-label">运单号码：</span>
                <span class="shipping-value">MD{{ order.oid }}2024</span>
              </div>
            </div>
            <button @click="confirmReceipt" class="confirm-receipt-btn">
              确认收货
            </button>
          </div>
        </div>
        <!-- 已完成状态 -->
        <div v-if="order.status === 'COMPLETED' || order.status === '已完成'" class="completed-section">
          <div class="completed-header">
            <div class="completed-icon">✅</div>
            <h3>订单已完成</h3>
          </div>
          <div class="completed-content">
            <p class="completed-message">感谢您的购买，期待再次为您服务！</p>
            <div class="completed-actions">
              <button @click="goToProducts" class="action-btn primary">继续购物</button>
            </div>
          </div>
        </div>
        <!-- 订单状态时间线 -->
        <div class="order-status-details">
          <h3>订单状态详情</h3>
          <div class="status-timeline">
            <div class="timeline-item completed">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <span class="timeline-title">订单创建</span>
                <span class="timeline-time">{{ formatDate(order.orderDate) }}</span>
              </div>
            </div>
            <div class="timeline-item" :class="isPaid ? 'completed' : 'pending'">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <span class="timeline-title">付款成功</span>
                <span class="timeline-time" v-if="isPaid">{{ formatDate(order.payTime || order.orderDate) }}</span>
                <span class="timeline-time" v-else>等待付款</span>
              </div>
            </div>
            <div class="timeline-item" :class="isShipped ? 'completed' : 'pending'">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <span class="timeline-title">商家发货</span>
                <span class="timeline-time" v-if="isShipped">{{ formatDate(order.shipTime || order.orderDate) }}</span>
                <span class="timeline-time" v-else>等待商家发货</span>
              </div>
            </div>
            <div class="timeline-item" :class="isCompleted ? 'completed' : 'pending'">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <span class="timeline-title">订单完成</span>
                <span class="timeline-time" v-if="isCompleted">{{ formatDate(order.completeTime || order.orderDate) }}</span>
                <span class="timeline-time" v-else>等待确认收货</span>
              </div>
            </div>
          </div>
        </div>
        <!-- 付款成功提示 -->
        <div v-if="showSuccess" class="payment-success">
          <div class="success-icon">✅</div>
          <h3>付款成功！</h3>
          <p>订单已支付，请等待商家确认发货</p>
          <div class="auto-refresh-info">
            <p>系统将自动刷新订单状态</p>
            <div class="countdown" v-if="autoRefreshCountdown > 0">
              自动刷新倒计时: {{ autoRefreshCountdown }}秒
            </div>
          </div>
          <div class="success-actions">
            <button @click="refreshOrder" class="action-btn primary">立即刷新</button>
            <button @click="stayOnPage" class="action-btn secondary">留在本页</button>
          </div>
        </div>
        <!-- 订单操作按钮 -->
        <div class="order-actions">
          <button @click="goToCart" class="action-btn">返回购物车</button>
          <button @click="goToProducts" class="action-btn">继续购物</button>
          <button v-if="order.status === 'PENDING' || order.status === '待付款'"
                  @click="cancelOrder"
                  class="action-btn cancel">
            取消订单
          </button>
          <button v-if="order.status !== 'PENDING' && order.status !== '待付款'"
                  @click="goToOrders"
                  class="action-btn primary">
            查看我的订单
          </button>
        </div>
      </div>
    </div>
    <div v-else class="not-found">
      <h2>订单不存在或加载失败</h2>
      <p class="error-message">{{ errorMessage }}</p>
      <button @click="goToCart" class="back-btn">返回购物车</button>
      <button @click="retryLoad" class="retry-btn">重试加载</button>
    </div>
  </div>
</template>

<script>
import { orderAPI, apiUtils } from '../services/api'

export default {
  name: 'OrderView',
  data() {
    return {
      order: null,
      orderItems: [],
      loading: false,
      paying: false,
      selectedPayment: 'alipay',
      showSuccess: false,
      autoRedirect: true,
      errorMessage: '',
      autoRefreshTimer: null,
      countdownTimer: null,
      autoRefreshCountdown: 5
    }
  },
  computed: {
    isLoggedIn() {
      return apiUtils.isLoggedIn()
    },
    orderId() {
      const orderId = this.$route.params.id || this.$route.params.orderId || this.$route.query.orderId
      return orderId
    },
    shippingFee() {
      return 10.00
    },
    safeTotalAmount() {
      if (!this.order || this.order.totalAmount === undefined || this.order.totalAmount === null) {
        if (this.orderItems && this.orderItems.length > 0) {
          return this.orderItems.reduce((total, item) => {
            const price = item.price || item.priceAtPurchase || 0
            const quantity = item.quantity || 1
            return total + (price * quantity)
          }, 0)
        }
        return 0.00
      }
      return parseFloat(this.order.totalAmount)
    },
    formattedProductAmount() {
      return this.safeTotalAmount.toFixed(2)
    },
    finalTotalAmount() {
      return (this.safeTotalAmount + this.shippingFee).toFixed(2)
    },
    isPaid() {
      const status = this.order?.status
      return status === 'PAID' || status === '已付款' ||
          status === 'SHIPPED' || status === '已发货' ||
          status === 'COMPLETED' || status === '已完成'
    },
    isShipped() {
      const status = this.order?.status
      return status === 'SHIPPED' || status === '已发货' ||
          status === 'COMPLETED' || status === '已完成'
    },
    isCompleted() {
      const status = this.order?.status
      return status === 'COMPLETED' || status === '已完成'
    }
  },
  async mounted() {
    if (!this.isLoggedIn) {
      this.$router.push('/login')
      return
    }
    await this.loadOrder()
  },
  beforeUnmount() {
    this.clearTimers()
  },
  methods: {
    async loadOrder() {
      this.loading = true
      this.errorMessage = ''
      try {
        if (!this.orderId || this.orderId === 'undefined' || this.orderId === 'null') {
          throw new Error('订单ID无效或未提供')
        }
        const isReallyLoggedIn = await this.checkRealLoginStatus()
        if (!isReallyLoggedIn) {
          return
        }
        const numericOrderId = Number(this.orderId)
        if (isNaN(numericOrderId)) {
          throw new Error(`订单ID格式错误: ${this.orderId}`)
        }
        const response = await orderAPI.getOrderDetails(numericOrderId)
        const data = apiUtils.handleResponse(response)
        if (data.order && data.orderDetails) {
          this.order = data.order
          this.orderItems = data.orderDetails || []
        } else if (data.oid || data.id) {
          this.order = data
          this.orderItems = data.orderItems || data.items || []
        } else {
          this.order = data
          this.orderItems = data.orderItems || data.items || []
        }
        if (!this.order.status) {
          this.order.status = '待付款'
        }
        if (this.order.totalAmount) {
          this.order.totalAmount = parseFloat(this.order.totalAmount)
        } else {
          this.order.totalAmount = this.orderItems.reduce((total, item) => {
            const price = item.price || item.priceAtPurchase || 0
            const quantity = item.quantity || 1
            return total + (price * quantity)
          }, 0)
        }
        this.orderItems = this.orderItems.map(item => ({
          ...item,
          price: parseFloat(item.price || item.priceAtPurchase) || 0,
          quantity: parseInt(item.quantity) || 1,
          name: item.name || `商品${item.gid || item.id}`,
          description: item.description || '暂无描述'
        }))
        const from = this.$route.query.from
        if (from === 'payment') {
          this.order.status = '已付款'
          this.showSuccess = true
          this.startAutoRefresh()
        }
        if (this.order.status === '已付款' || this.order.status === 'PAID') {
          this.startAutoRefresh()
        }
      } catch (error) {
        this.errorMessage = error.message || '加载订单失败'
      } finally {
        this.loading = false
      }
    },

    async checkRealLoginStatus() {
      try {
        if (!this.isLoggedIn) {
          this.$router.push('/login')
          return false
        }
        const sessionValid = await apiUtils.checkLoginStatus()
        if (!sessionValid) {
          this.showMessage('登录已过期，请重新登录', 'error')
          this.$router.push('/login')
          return false
        }
        return true
      } catch (error) {
        this.showMessage('登录状态检查失败，请重新登录', 'error')
        this.$router.push('/login')
        return false
      }
    },

    getProductImage(item) {
      if (!item) {
        return this.getDefaultImageUrl();
      }
      if (item.fullImageUrl && item.fullImageUrl !== 'null' && item.fullImageUrl !== 'undefined') {
        return item.fullImageUrl;
      }
      if (item.imageUrl && item.imageUrl !== 'null' && item.imageUrl !== 'undefined') {
        if (item.imageUrl.startsWith('/')) {
          return item.imageUrl;
        }
        return item.imageUrl;
      }
      //如果后端没有提供图片URL，使用默认图片
      return this.getDefaultImageUrl();
    },

    getDefaultImageUrl() {
      return '/uploads/products/defaultpicture.png'
    },

    handleImageError(event) {
      event.target.src = this.getDefaultImageUrl()
      event.target.onerror = null
    },

    getStatusClass(status) {
      const statusMap = {
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
      return statusMap[status] || 'status-pending'
    },

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

    formatDate(dateValue) {
      if (!dateValue) return '-'
      try {
        if (typeof dateValue === 'number') {
          if (dateValue < 1000000000000) {
            dateValue = dateValue * 1000
          }
          return new Date(dateValue).toLocaleString('zh-CN')
        }
        const date = new Date(dateValue)
        if (isNaN(date.getTime())) {
          return dateValue
        }
        return date.toLocaleString('zh-CN')
      } catch (error) {
        return dateValue
      }
    },

    getEstimatedDeliveryTime() {
      const orderDate = this.order.orderDate ? new Date(this.order.orderDate) : new Date()
      const deliveryDate = new Date(orderDate.getTime() + 3 * 24 * 60 * 60 * 1000)
      return deliveryDate.toLocaleDateString('zh-CN')
    },

    async handlePayment() {
      if (!this.selectedPayment) {
        this.showMessage('请选择付款方式', 'warning')
        return
      }
      this.paying = true
      try {
        const isReallyLoggedIn = await this.checkRealLoginStatus()
        if (!isReallyLoggedIn) {
          return
        }
        const numericOrderId = Number(this.order.oid)
        if (isNaN(numericOrderId)) {
          throw new Error(`订单ID格式错误: ${this.order.oid}`)
        }
        await orderAPI.payOrder(numericOrderId)
        this.showMessage('付款成功', 'success')
        this.order.status = '已付款'
        this.showSuccess = true
        this.startAutoRefresh()
      } catch (error) {
        this.showMessage('付款失败：' + error.message, 'error')
      } finally {
        this.paying = false
      }
    },

    startAutoRefresh() {
      this.clearTimers()
      if (this.order.status === '已付款' || this.order.status === 'PAID') {
        this.autoRefreshCountdown = 30
        this.startCountdown()
        this.autoRefreshTimer = setInterval(() => {
          this.refreshOrder()
        }, 30000)
      }
    },

    startCountdown() {
      this.clearTimers()
      this.countdownTimer = setInterval(() => {
        if (this.autoRefreshCountdown > 0) {
          this.autoRefreshCountdown--
        } else {
          this.autoRefreshCountdown = 30
        }
      }, 1000)
    },

    async refreshOrder() {
      try {
        const numericOrderId = Number(this.order.oid)
        if (isNaN(numericOrderId)) return
        const response = await orderAPI.getOrderDetails(numericOrderId)
        const data = apiUtils.handleResponse(response)
        if (data.order && data.order.status !== this.order.status) {
          this.order = data.order
          this.showMessage('订单状态已更新', 'success')
          if (this.order.status === '已发货' || this.order.status === 'SHIPPED') {
            this.clearTimers()
            this.showMessage('商家已发货，商品正在运输中', 'success')
          }
        }
      } catch (error) {
        console.error('刷新订单失败:', error)
      }
    },

    async confirmReceipt() {
      if (!confirm('请确认您已收到商品，确认后将完成订单')) return
      try {
        const numericOrderId = Number(this.order.oid)
        if (isNaN(numericOrderId)) {
          throw new Error(`订单ID格式错误: ${this.order.oid}`)
        }
        //调用完成订单API，而不是获取详情
        await orderAPI.completeOrder(numericOrderId)
        //重新加载订单数据以确保状态同步
        await this.refreshOrder()
        this.showMessage('确认收货成功', 'success')
      } catch (error) {
        this.showMessage('确认收货失败：' + error.message, 'error')
      }
    },

    clearTimers() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
        this.countdownTimer = null
      }
      if (this.autoRefreshTimer) {
        clearInterval(this.autoRefreshTimer)
        this.autoRefreshTimer = null
      }
      this.autoRefreshCountdown = 5
    },

    stayOnPage() {
      this.autoRedirect = false
      this.clearTimers()
      this.showSuccess = false
      this.showMessage('已取消自动刷新', 'info')
    },

    goToCart() {
      this.$router.push('/cart')
    },

    goToProducts() {
      this.$router.push('/products')
    },

    goToOrders() {
      this.$router.push('/profile')
    },

    async cancelOrder() {
      if (!confirm('确定要取消这个订单吗？')) return
      try {
        const numericOrderId = Number(this.order.oid)
        if (isNaN(numericOrderId)) {
          throw new Error(`订单ID格式错误: ${this.order.oid}`)
        }
        await orderAPI.cancelOrder(numericOrderId)
        this.order.status = '已取消'
        this.showMessage('订单取消成功', 'success')
      } catch (error) {
        this.showMessage('取消订单失败：' + error.message, 'error')
      }
    },

    async retryLoad() {
      await this.loadOrder()
    },

    showMessage(message, type = 'info') {
      const messageEl = document.createElement('div')
      messageEl.className = `custom-message custom-message-${type}`
      messageEl.textContent = message
      messageEl.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 12px 20px;
        border-radius: 4px;
        color: white;
        font-size: 14px;
        z-index: 10000;
        transition: all 0.3s ease;
        max-width: 300px;
      `
      const bgColors = {
        success: '#67c23a',
        warning: '#e6a23c',
        error: '#f56c6c',
        info: '#909399'
      }
      messageEl.style.backgroundColor = bgColors[type] || bgColors.info
      document.body.appendChild(messageEl)
      setTimeout(() => {
        messageEl.style.opacity = '0'
        messageEl.style.transform = 'translateX(100%)'
        setTimeout(() => {
          if (messageEl.parentNode) {
            messageEl.parentNode.removeChild(messageEl)
          }
        }, 300)
      }, 3000)
    }
  }
}
</script>

<style scoped>
.order-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
  min-height: 80vh;
}
.loading {
  text-align: center;
  padding: 4rem 2rem;
  color: #666;
  font-size: 1.1rem;
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e0e0e0;
}
.order-header h2 {
  color: #333;
  margin: 0;
}
.order-status {
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-weight: bold;
  font-size: 0.9rem;
}
.status-pending {
  background-color: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}
.status-paid {
  background-color: #d1ecf1;
  color: #0c5460;
  border: 1px solid #bee5eb;
}
.status-shipped {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}
.status-completed {
  background-color: #d1ecf1;
  color: #0c5460;
  border: 1px solid #bee5eb;
}
.status-cancelled {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}
.order-info {
  background: white;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.info-section, .products-section, .price-section, .payment-section,
.paid-section, .shipped-section, .completed-section, .order-status-details {
  margin-bottom: 2rem;
}
.info-section h3, .products-section h3, .price-section h3,
.payment-section h3, .order-status-details h3 {
  color: #333;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #f0f0f0;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}
.info-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
}
.label {
  color: #666;
  font-weight: 500;
}
.value {
  color: #333;
}
.total-amount {
  font-weight: bold;
  color: #e74c3c;
  font-size: 1.1rem;
}
.products-list {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
}
.product-item {
  display: flex;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid #f0f0f0;
}
.product-item:last-child {
  border-bottom: none;
}
.product-image {
  width: 80px;
  height: 80px;
  background: #f5f5f5;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 1rem;
  overflow: hidden;
}
.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.product-info {
  flex: 1;
}
.product-name {
  margin: 0 0 0.5rem 0;
  color: #333;
  font-weight: 500;
}
.product-price {
  margin: 0 0 0.3rem 0;
  color: #666;
  font-weight: bold;
}
.product-description {
  margin: 0;
  color: #999;
  font-size: 0.9rem;
}
.product-total {
  font-weight: bold;
  color: #e74c3c;
  font-size: 1.1rem;
  min-width: 80px;
  text-align: right;
}
.price-details {
  max-width: 300px;
  margin-left: auto;
}
.price-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f0f0f0;
}
.price-item.total {
  border-top: 2px solid #e0e0e0;
  border-bottom: none;
  margin-top: 0.5rem;
  padding-top: 1rem;
  font-weight: bold;
  font-size: 1.1rem;
}
.final-amount {
  color: #e74c3c;
  font-size: 1.2rem;
}
.payment-methods {
  margin-bottom: 2rem;
}
.payment-method {
  margin-bottom: 1rem;
}
.payment-method input[type="radio"] {
  display: none;
}
.payment-option {
  display: flex;
  align-items: center;
  padding: 1rem;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}
.payment-method input[type="radio"]:checked + .payment-option {
  border-color: #409eff;
  background-color: #f0f9ff;
}
.payment-icon {
  font-size: 1.5rem;
  margin-right: 1rem;
}
.payment-btn {
  width: 100%;
  padding: 1.2rem;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 1.2rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s;
}
.payment-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(231, 76, 60, 0.3);
}
.payment-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
}
.paid-section {
  background: linear-gradient(135deg, #fff8f0, #fff3e6);
  border: 2px solid #ffe0b2;
  border-radius: 8px;
  padding: 1.5rem;
}
.paid-header {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}
.paid-icon {
  font-size: 2rem;
  margin-right: 1rem;
  animation: pulse 2s infinite;
}
.paid-header h3 {
  margin: 0;
  color: #e65100;
}
.paid-content {
  text-align: center;
}
.paid-message {
  color: #e65100;
  font-size: 1.1rem;
  margin-bottom: 1.5rem;
}
.waiting-info {
  background: white;
  border-radius: 6px;
  padding: 1rem;
  margin-bottom: 1.5rem;
  text-align: left;
}
.waiting-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f0f0f0;
}
.waiting-item:last-child {
  border-bottom: none;
}
.waiting-label {
  color: #666;
  font-weight: 500;
}
.waiting-value {
  color: #333;
  font-weight: bold;
}
.refresh-btn {
  padding: 1rem 2rem;
  background: linear-gradient(135deg, #ff9800, #f57c00);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 1.1rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s;
}
.refresh-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 152, 0, 0.3);
}
.shipped-section {
  background: linear-gradient(135deg, #f8fff8, #f0f9f0);
  border: 2px solid #d4edda;
  border-radius: 8px;
  padding: 1.5rem;
}
.shipped-header {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}
.shipped-icon {
  font-size: 2rem;
  margin-right: 1rem;
}
.shipped-header h3 {
  margin: 0;
  color: #155724;
}
.shipped-content {
  text-align: center;
}
.shipped-message {
  color: #155724;
  font-size: 1.1rem;
  margin-bottom: 1.5rem;
}
.shipping-info {
  background: white;
  border-radius: 6px;
  padding: 1rem;
  margin-bottom: 1.5rem;
  text-align: left;
}
.shipping-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f0f0f0;
}
.shipping-item:last-child {
  border-bottom: none;
}
.shipping-label {
  color: #666;
  font-weight: 500;
}
.shipping-value {
  color: #333;
  font-weight: bold;
}
.confirm-receipt-btn {
  padding: 1rem 2rem;
  background: linear-gradient(135deg, #28a745, #20c997);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 1.1rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s;
}
.confirm-receipt-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
}
.completed-section {
  background: linear-gradient(135deg, #f0f9ff, #e6f7ff);
  border: 2px solid #d1ecf1;
  border-radius: 8px;
  padding: 1.5rem;
}
.completed-header {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}
.completed-icon {
  font-size: 2rem;
  margin-right: 1rem;
}
.completed-header h3 {
  margin: 0;
  color: #0c5460;
}
.completed-content {
  text-align: center;
}
.completed-message {
  color: #0c5460;
  font-size: 1.1rem;
  margin-bottom: 1.5rem;
}
.completed-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}
.order-status-details {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1.5rem;
}
.status-timeline {
  position: relative;
  padding-left: 2rem;
}
.status-timeline::before {
  content: '';
  position: absolute;
  left: 1rem;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #e0e0e0;
}
.timeline-item {
  position: relative;
  margin-bottom: 1.5rem;
}
.timeline-item:last-child {
  margin-bottom: 0;
}
.timeline-dot {
  position: absolute;
  left: -2rem;
  top: 0.25rem;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  background: #e0e0e0;
  z-index: 1;
}
.timeline-item.completed .timeline-dot {
  background: #27ae60;
}
.timeline-item.pending .timeline-dot {
  background: #f39c12;
}
.timeline-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.timeline-title {
  font-weight: 500;
  color: #333;
}
.timeline-time {
  color: #666;
  font-size: 0.9rem;
}
.payment-success {
  text-align: center;
  padding: 2rem;
  background: #f8fff8;
  border: 2px solid #d4edda;
  border-radius: 8px;
  margin-bottom: 2rem;
}
.success-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}
.payment-success h3 {
  color: #27ae60;
  margin-bottom: 1rem;
}
.auto-refresh-info {
  margin-bottom: 1.5rem;
}
.auto-refresh-info p {
  color: #666;
  margin-bottom: 0.5rem;
}
.countdown {
  color: #e74c3c;
  font-weight: bold;
  font-size: 1.1rem;
  margin-bottom: 1rem;
}
.success-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}
.order-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  padding-top: 2rem;
  border-top: 1px solid #e0e0e0;
}
.action-btn {
  padding: 0.8rem 1.5rem;
  border: 1px solid #409eff;
  border-radius: 4px;
  background: white;
  color: #409eff;
  cursor: pointer;
  transition: all 0.3s;
}
.action-btn:hover {
  background: #409eff;
  color: white;
}
.action-btn.primary {
  background: #409eff;
  color: white;
}
.action-btn.secondary {
  border-color: #666;
  color: #666;
}
.action-btn.cancel {
  border-color: #e74c3c;
  color: #e74c3c;
}
.action-btn.cancel:hover {
  background: #e74c3c;
  color: white;
}
.not-found {
  text-align: center;
  padding: 4rem 2rem;
}
.error-message {
  color: #e74c3c;
  margin: 1rem 0;
}
.back-btn {
  padding: 0.8rem 1.5rem;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 1rem;
}
.retry-btn {
  padding: 0.8rem 1.5rem;
  background: #27ae60;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.7;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
@media (max-width: 768px) {
  .order-container {
    padding: 1rem;
  }
  .order-header {
    flex-direction: column;
    gap: 1rem;
    text-align: center;
  }
  .order-info {
    padding: 1rem;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
  .product-item {
    flex-direction: column;
    text-align: center;
    gap: 1rem;
  }
  .product-info {
    text-align: center;
  }
  .product-total {
    text-align: center;
  }
  .price-details {
    max-width: none;
    margin-left: 0;
  }
  .timeline-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  .waiting-info, .shipping-info {
    text-align: center;
  }
  .waiting-item, .shipping-item {
    flex-direction: column;
    text-align: center;
    gap: 0.5rem;
  }
  .completed-actions, .success-actions, .order-actions {
    flex-direction: column;
  }
  .action-btn {
    width: 100%;
    margin: 0.5rem 0;
  }
  .back-btn, .retry-btn {
    width: 100%;
    margin: 0.5rem 0;
  }
  .payment-methods {
    margin-bottom: 1rem;
  }
  .payment-option {
    padding: 0.8rem;
  }
  .payment-icon {
    font-size: 1.2rem;
    margin-right: 0.5rem;
  }
  .payment-btn {
    padding: 1rem;
    font-size: 1.1rem;
  }
}
@media (max-width: 480px) {
  .order-container {
    padding: 0.5rem;
  }
  .order-info {
    padding: 0.8rem;
  }
  .info-section, .products-section, .price-section, .payment-section,
  .paid-section, .shipped-section, .completed-section, .order-status-details {
    margin-bottom: 1.5rem;
  }
  .product-image {
    width: 60px;
    height: 60px;
    margin-right: 0.5rem;
  }
  .product-name {
    font-size: 0.9rem;
  }
  .product-price, .product-description {
    font-size: 0.8rem;
  }
  .paid-section, .shipped-section, .completed-section {
    padding: 1rem;
  }
  .paid-icon, .shipped-icon, .completed-icon {
    font-size: 1.5rem;
  }
  .paid-header h3, .shipped-header h3, .completed-header h3 {
    font-size: 1.1rem;
  }
  .paid-message, .shipped-message, .completed-message {
    font-size: 1rem;
  }
  .refresh-btn, .confirm-receipt-btn {
    padding: 0.8rem 1.5rem;
    font-size: 1rem;
  }
  .action-btn {
    padding: 0.7rem 1.2rem;
    font-size: 0.9rem;
  }
}
.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
}
.loading::after {
  content: '';
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-top: 1rem;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
.custom-message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 20px;
  border-radius: 4px;
  color: white;
  font-size: 14px;
  z-index: 10000;
  transition: all 0.3s ease;
  max-width: 300px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.custom-message-success {
  background-color: #67c23a;
}
.custom-message-warning {
  background-color: #e6a23c;
}
.custom-message-error {
  background-color: #f56c6c;
}
.custom-message-info {
  background-color: #909399;
}
</style>