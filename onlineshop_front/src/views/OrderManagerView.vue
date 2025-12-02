<template>
  <div class="order-manager-container">
    <h1>发货管理</h1>
    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <p>加载中...</p>
    </div>
    <!-- 无订单状态 -->
    <div v-else-if="orders.length === 0" class="no-orders">
      <div class="empty-content">
        <div class="empty-icon">📦</div>
        <h3>暂无待发货订单</h3>
        <p>所有订单已处理完成</p>
      </div>
    </div>
    <!-- 订单列表 -->
    <div v-else class="orders-list">
      <div v-for="order in orders" :key="order.oid" class="order-card">
        <div class="order-header">
          <span class="order-id">订单号: {{ order.oid }}</span>
          <span class="order-date">下单时间: {{ formatDate(order.orderDate) }}</span>
        </div>
        <div class="order-content">
          <!-- 商品信息 - 修改为显示所有商品 -->
          <div class="products-section">
            <h4 class="products-title">商品清单</h4>
            <div class="products-list">
              <div v-for="(item, index) in getOrderItems(order)" :key="index" class="product-item">
                <div class="product-image">
                  <img :src="getProductImage(item)" @error="handleImageError" class="product-img"/>
                </div>
                <div class="product-details">
                  <h4 class="product-name">{{ getProductName(item) }}</h4>
                  <p class="product-description">{{ getProductDescription(item) }}</p>
                  <div class="product-meta">
                    <span class="quantity">数量: {{ getProductQuantity(item) }}</span>
                    <span class="price">单价: ¥{{ getProductPrice(item) }}</span>
                    <span class="subtotal">小计: ¥{{ getProductSubtotal(item) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <!-- 订单总价和操作 -->
          <div class="order-summary">
            <div class="total-price">
              总价: <span class="price-amount">¥{{ getTotalPrice(order) }}</span>
            </div>
            <div class="order-actions">
              <button
                  @click="shipOrder(order)"
                  :disabled="order.status === '已发货' || order.status === 'SHIPPED' || shippingOrderId === order.oid"
                  class="ship-btn"
                  :class="{ shipped: order.status === '已发货' || order.status === 'SHIPPED' }"
              >
                {{ getShipButtonText(order) }}
              </button>
            </div>
          </div>
        </div>
        <!-- 订单状态 -->
        <div class="order-footer">
          <span class="order-status" :class="getStatusClass(order.status)">
            {{ getStatusText(order.status) }}
          </span>
        </div>
        <!-- 发货成功提示 -->
        <div v-if="showSuccess && successOrderId === order.oid" class="ship-success">
          <div class="success-icon">✅</div>
          <p class="success-message">发货成功！订单状态已更新</p>
          <div class="auto-refresh-info">
            <p>页面将自动刷新订单列表</p>
            <div class="countdown" v-if="autoRefreshCountdown > 0">
              自动刷新倒计时: {{ autoRefreshCountdown }}秒
            </div>
          </div>
          <div class="success-actions">
            <button @click="refreshOrders" class="action-btn primary">立即刷新</button>
            <button @click="stayOnPage" class="action-btn secondary">留在本页</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { merchantAPI, apiUtils, orderAPI } from '../services/api'

export default {
  name: 'OrderManagerView',
  data() {
    return {
      orders: [],
      loading: false,
      shippingOrderId: null,
      showSuccess: false,
      successOrderId: null,
      autoRefresh: true,
      autoRefreshTimer: null,
      countdownTimer: null,
      autoRefreshCountdown: 5
    }
  },
  async mounted() {
    await this.loadOrders()
  },
  beforeUnmount() {
    this.clearTimers()
  },
  methods: {
    //加载订单列表
    async loadOrders() {
      this.loading = true
      try {
        console.log('开始加载商家订单...')
        //获取已付款订单
        const response = await merchantAPI.getAllOrders('已付款')
        const data = apiUtils.handleResponse(response)
        //处理订单数据
        this.orders = data.orders || data || []
        console.log('订单加载完成，数量:', this.orders.length)
        console.log('订单数据:', this.orders)
      } catch (error) {
        console.error('加载订单失败:', error)
        this.showMessage('加载订单失败: ' + error.message, 'error')
      } finally {
        this.loading = false
      }
    },
    //获取订单的所有商品项
    getOrderItems(order) {
      //尝试不同的字段名来获取订单项
      if (order.orderItems && order.orderItems.length > 0) {
        return order.orderItems
      }
      if (order.items && order.items.length > 0) {
        return order.items
      }
      if (order.orderGoods && order.orderGoods.length > 0) {
        return order.orderGoods
      }
      if (order.orderDetails && order.orderDetails.length > 0) {
        return order.orderDetails
      }
      const firstItem = this.getFirstOrderItem(order)
      return firstItem ? [firstItem] : []
    },
    // 发货操作
    async shipOrder(order) {
      try {
        //确认发货操作
        if (!confirm('确定要发货这个订单吗？')) {
          return;
        }
        //显示加载状态
        this.shippingOrderId = order.oid;
        //调用发货API
        const response = await orderAPI.shipOrder(order.oid);
        if (response.success) {
          //发货成功
          this.showSuccess = true;
          this.successOrderId = order.oid;
          this.showMessage('发货成功', 'success');
          //触发客户管理数据更新事件
          this.triggerCustomerDataUpdate(order);
          //启动自动刷新倒计时
          this.startAutoRefresh();
        } else {
          throw new Error(response.message || '发货失败');
        }
      } catch (error) {
        console.error('发货失败:', error);
        //简化错误处理
        let errorMessage = '发货失败';
        if (error.message.includes('订单状态不允许')) {
          errorMessage = '订单当前状态不允许发货';
        } else if (error.message.includes('订单不存在')) {
          errorMessage = '订单不存在';
        } else if (error.message.includes('登录') || error.message.includes('权限')) {
          errorMessage = '请检查登录状态和权限';
          this.$router.push('/login');
        } else {
          errorMessage = error.message || '发货失败，请重试';
        }
        this.showMessage(errorMessage, 'error');
      } finally {
        this.shippingOrderId = null;
      }
    },
//触发客户数据更新事件
    triggerCustomerDataUpdate(order) {
      console.log('触发客户数据更新事件', order);
      //创建自定义事件，通知客户管理页面刷新数据
      const orderShippedEvent = new CustomEvent('orderShipped', {
        detail: {
          orderId: order.oid,
          customerId: order.customerId, //假设订单中有客户ID字段
          timestamp: new Date().toISOString(),
          orderData: order
        }
      });
      //分发事件
      window.dispatchEvent(orderShippedEvent);
    },
    //启动自动刷新
    startAutoRefresh() {
      this.clearTimers()
      this.autoRefreshCountdown = 5
      this.countdownTimer = setInterval(() => {
        if (this.autoRefreshCountdown > 0) {
          this.autoRefreshCountdown--
        } else {
          this.autoRefreshCountdown = 0
          if (this.autoRefresh) {
            this.refreshOrders()
          }
        }
      }, 1000)
    },
    //刷新订单列表
    async refreshOrders() {
      try {
        await this.loadOrders()
        this.showSuccess = false
        this.successOrderId = null
        this.clearTimers()
        this.showMessage('订单列表已刷新', 'success')
      } catch (error) {
        console.error('刷新订单列表失败:', error)
      }
    },
    //留在本页
    stayOnPage() {
      this.autoRefresh = false
      this.clearTimers()
      this.showSuccess = false
      this.successOrderId = null
      this.showMessage('已取消自动刷新', 'info')
    },
    //清理计时器
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
    //获取商品图片
    getProductImage(item) {
      if (item && item.goods) {
        return this.processImageUrl(item.goods.imageUrl)
      }
      if (item && item.imageUrl) {
        return this.processImageUrl(item.imageUrl)
      }
      return this.getDefaultImageUrl()
    },
    //处理图片URL
    processImageUrl(imageUrl) {
      if (!imageUrl || imageUrl === 'null' || imageUrl === 'undefined') {
        return this.getDefaultImageUrl()
      }
      if (imageUrl.startsWith('http')) {
        return imageUrl
      }
      if (imageUrl.startsWith('/')) {
        return  imageUrl
      }
      return '/uploads/products/' + imageUrl
    },

    //默认图片
    getDefaultImageUrl() {
      return '/uploads/products/defaultpicture.png'
    },

    //图片加载错误处理
    handleImageError(event) {
      console.warn('商品图片加载失败:', event.target.src)
      event.target.src = this.getDefaultImageUrl()
      event.target.onerror = null // 防止循环错误
    },

    //获取第一个订单项
    getFirstOrderItem(order) {
      if (order.orderItems && order.orderItems.length > 0) {
        return order.orderItems[0]
      }
      if (order.items && order.items.length > 0) {
        return order.items[0]
      }
      if (order.orderGoods && order.orderGoods.length > 0) {
        return order.orderGoods[0]
      }
      return null
    },

    //获取商品名称
    getProductName(item) {
      if (item && item.goods) {
        return item.goods.name
      }
      if (item && item.name) {
        return item.name
      }
      return '商品信息加载中...'
    },

    //获取商品描述
    getProductDescription(item) {
      if (item && item.goods) {
        return item.goods.description || '暂无描述'
      }
      if (item && item.description) {
        return item.description || '暂无描述'
      }
      return '商品描述加载中...'
    },

    //获取商品数量
    getProductQuantity(item) {
      return item ? item.quantity : 0
    },

    //获取商品单价
    getProductPrice(item) {
      if (item) {
        return (item.priceAtPurchase || item.price || 0).toFixed(2)
      }
      return '0.00'
    },

    //获取商品小计
    getProductSubtotal(item) {
      if (item) {
        const price = item.priceAtPurchase || item.price || 0
        const quantity = item.quantity || 1
        return (price * quantity).toFixed(2)
      }
      return '0.00'
    },

    //获取订单总价
    getTotalPrice(order) {
      //优先使用订单总价字段
      if (order.totalAmount) {
        return order.totalAmount.toFixed(2)
      }

      //计算所有商品的总价
      const items = this.getOrderItems(order)
      if (items && items.length > 0) {
        const total = items.reduce((sum, item) => {
          const price = item.priceAtPurchase || item.price || 0
          const quantity = item.quantity || 1
          return sum + (price * quantity)
        }, 0)
        return total.toFixed(2)
      }

      return '0.00'
    },

    //获取发货按钮文本
    getShipButtonText(order) {
      if (this.shippingOrderId === order.oid) {
        return '发货中...'
      }
      const status = order.status
      if (status === '已发货' || status === 'SHIPPED') {
        return '已发货'
      }
      if (status === '已付款' || status === 'PAID') {
        return '发货'
      }
      return '发货'
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

    //格式化日期
    formatDate(dateValue) {
      if (!dateValue || dateValue === 'null' || dateValue === 'undefined') {
        return '-'
      }
      try {
        let date;
        //处理字符串格式的日期
        if (typeof dateValue === 'string') {
          //尝试解析ISO格式
          date = new Date(dateValue);
          if (isNaN(date.getTime())) {
            //如果不是标准格式，尝试其他解析方式
            date = new Date(dateValue.replace(/-/g, '/'));
          }
        } else if (typeof dateValue === 'number') {
          //处理时间戳
          date = new Date(dateValue < 1000000000000 ? dateValue * 1000 : dateValue);
        } else {
          date = new Date(dateValue);
        }

        if (isNaN(date.getTime())) {
          return '-';  //解析失败
        }
        return date.toLocaleString('zh-CN');
      } catch (error) {
        console.error('日期解析错误:', error, dateValue);
        return '-';  //异常
      }
    },

    //显示消息提示
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
        transition: all 0.3s ease;
        max-width: 300px;
      `
      //根据类型设置背景色
      const bgColors = {
        success: '#67c23a',
        warning: '#e6a23c',
        error: '#f56c6c',
        info: '#909399'
      }
      messageEl.style.backgroundColor = bgColors[type] || bgColors.info
      //添加到页面
      document.body.appendChild(messageEl)
      //3秒后自动移除
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
.order-manager-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 70vh;
}
.loading {
  text-align: center;
  padding: 40px;
  font-size: 18px;
  color: #666;
}
.no-orders {
  text-align: center;
  padding: 40px;
}
.empty-content {
  max-width: 400px;
  margin: 0 auto;
}
.empty-icon {
  font-size: 4rem;
  margin-bottom: 1.5rem;
  opacity: 0.3;
}
.empty-content h3 {
  margin-bottom: 1rem;
  color: #666;
  font-size: 1.5rem;
}
.empty-content p {
  color: #999;
  font-size: 1.1rem;
}
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.order-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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
  font-size: 1.1rem;
}
.order-date {
  color: #666;
  font-size: 0.9rem;
}
.order-content {
  padding: 20px;
}
.products-section {
  margin-bottom: 20px;
}
.products-title {
  margin: 0 0 15px 0;
  font-size: 1.1rem;
  color: #333;
  font-weight: 600;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}
.products-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.product-item {
  display: flex;
  align-items: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
}
.product-image {
  width: 80px;
  height: 80px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  flex-shrink: 0;
}
.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.product-details {
  flex: 1;
}
.product-name {
  margin: 0 0 8px 0;
  font-size: 1rem;
  color: #333;
  font-weight: 500;
}
.product-description {
  margin: 0 0 10px 0;
  color: #666;
  font-size: 0.9rem;
  line-height: 1.4;
}
.product-meta {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}
.quantity, .price, .subtotal {
  color: #666;
  font-size: 0.9rem;
  font-weight: 500;
}
.subtotal {
  color: #e4393c;
  font-weight: bold;
}
.order-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #e0e0e0;
}
.total-price {
  font-size: 1.2rem;
  color: #333;
  font-weight: 600;
}
.price-amount {
  font-weight: bold;
  color: #e4393c;
  font-size: 1.3rem;
}
.order-actions {
  display: flex;
  gap: 10px;
}
.ship-btn {
  padding: 12px 24px;
  background: #e4393c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.3s;
  min-width: 120px;
}
.ship-btn:hover:not(:disabled) {
  background: #c03537;
  transform: translateY(-1px);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}
.ship-btn:disabled {
  background: #28a745;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
.ship-btn.shipped {
  background: #28a745;
}
.ship-btn.shipped:hover {
  background: #218838;
  transform: none;
  box-shadow: none;
}
.order-footer {
  padding: 10px 20px;
  background: #f8f9fa;
  border-top: 1px solid #e0e0e0;
  text-align: right;
}
.order-status {
  padding: 6px 16px;
  border-radius: 12px;
  font-size: 0.9rem;
  font-weight: bold;
}
.status-pending {
  background: #fff3cd;
  color: #856404;
}
.status-paid {
  background: #d1ecf1;
  color: #0c5460;
}
.status-shipped {
  background: #d4edda;
  color: #155724;
}
.status-completed {
  background: #d1ecf1;
  color: #0c5460;
}
.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}
.status-unknown {
  background: #f8f9fa;
  color: #666;
}
.ship-success {
  background: linear-gradient(135deg, #f8fff8, #f0f9f0);
  border: 2px solid #d4edda;
  border-radius: 8px;
  padding: 1.5rem;
  margin: 1rem 0;
  text-align: center;
}
.success-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}
.success-message {
  color: #155724;
  font-size: 1.1rem;
  margin-bottom: 1rem;
  font-weight: bold;
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
.action-btn.secondary:hover {
  background: #666;
  color: white;
}
@media (max-width: 768px) {
  .order-manager-container {
    padding: 15px;
  }
  .order-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  .order-content {
    padding: 15px;
  }
  .product-item {
    flex-direction: column;
    text-align: center;
    gap: 10px;
  }
  .product-image {
    margin-right: 0;
    margin-bottom: 10px;
  }
  .product-meta {
    justify-content: center;
    gap: 15px;
  }
  .order-summary {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
  .success-actions {
    flex-direction: column;
  }
  .action-btn {
    width: 100%;
    margin: 0.5rem 0;
  }
}
@media (max-width: 480px) {
  .product-meta {
    flex-direction: column;
    gap: 5px;
  }
  .ship-btn {
    width: 100%;
    padding: 15px;
  }
}
</style>