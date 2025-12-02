<template>
  <div class="product-detail-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>商品信息加载中...</p>
    </div>
    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <div class="error-icon">❌❌❌❌</div>
      <h3>加载失败</h3>
      <p>{{ error }}</p>
      <button @click="retryLoad" class="retry-btn">重试</button>
    </div>
    <!-- 商品详情内容 -->
    <div v-else-if="product" class="product-detail">
      <!-- 面包屑导航 -->
      <nav class="breadcrumb">
        <router-link to="/">首页</router-link>
        <span class="separator">/</span>
        <router-link to="/products">商品列表</router-link>
        <span class="separator">/</span>
        <span class="current">{{ product.name }}</span>
      </nav>
      <div class="product-content">
        <!-- 商品图片 -->
        <div class="product-image-section">
          <div class="main-image">
            <img :src="getProductImageUrl(product)" @error="handleImageError" class="product-image"/>
          </div>
        </div>
        <!-- 商品信息 -->
        <div class="product-info-section">
          <h1 class="product-name">{{ product.name }}</h1>
          <div class="price-section">
            <span class="current-price">¥{{ product.price }}</span>
          </div>
          <div class="stock-section">
            <span class="stock-label">库存：</span>
            <span
                :class="['stock-count', { 'low-stock': product.remaining < 10 }]"
            >
              {{ product.remaining }}件
            </span>
            <span v-if="product.remaining < 10" class="stock-warning">（库存紧张）</span>
          </div>
          <div class="description-section">
            <h3>商品描述</h3>
            <p class="product-description">{{ product.description || '暂无描述' }}</p>
          </div>
          <!-- 购买操作 -->
          <div class="action-section">
            <div class="quantity-selector">
              <label for="quantity">数量：</label>
              <div class="quantity-controls">
                <button
                    @click="decreaseQuantity"
                    :disabled="quantity <= 1"
                    class="quantity-btn"
                >
                  -
                </button>
                <input
                    id="quantity"
                    v-model.number="quantity"
                    type="number"
                    min="1"
                    :max="product.remaining"
                    @change="validateQuantity"
                />
                <button
                    @click="increaseQuantity"
                    :disabled="quantity >= product.remaining"
                    class="quantity-btn"
                >
                  +
                </button>
              </div>
            </div>
            <div class="action-buttons">
              <button
                  @click="addToCart"
                  :disabled="!isLoggedIn || product.remaining <= 0"
                  class="add-cart-btn"
              >
                {{ product.remaining <= 0 ? '已售罄罄' : '加入购物车' }}
              </button>
              <button
                  @click="buyNow"
                  :disabled="!isLoggedIn || product.remaining <= 0 || buying"
                  class="buy-now-btn"
              >
                {{ buying ? '创建订单中...' : (product.remaining <= 0 ? '已售罄罄' : '立即购买') }}
              </button>
            </div>
            <!-- 未登录提示 -->
            <div v-if="!isLoggedIn" class="login-prompt">
              <p>请先<router-link to="/login">登录</router-link>后再进行购买操作</p>
            </div>
          </div>
          <!-- 商家信息组件 -->
          <div class="merchant-section">
            <h3>商家信息</h3>
            <div
                class="merchant-info-card"
                @click="goToMerchantPage"
                :class="{ 'clickable': merchantInfo }"
            >
              <div class="merchant-avatar">
                <img :src="getAvatarUrl(merchantInfo.avatar)" @error="handleAvatarError" class="merchant-avatar-img"/>
              </div>
              <div class="merchant-details">
                <h4>{{ merchantInfo ? merchantInfo.nickname : '加载中...' }}</h4>
                <span class="merchant-badge">商家</span>
              </div>
              <div class="merchant-arrow" v-if="merchantInfo">→</div>
            </div>
            <div v-if="merchantLoading" class="merchant-loading">
              <p>加载商家信息中...</p>
            </div>
            <div v-else-if="merchantError" class="merchant-error">
              <p>商家信息加载失败</p>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 添加到购物车成功提示 -->
    <div v-if="showAddCartSuccess" class="success-message">
      <p>✅ 商品已成功添加到购物车！</p>
    </div>
    <!-- 直接购买成功提示 -->
    <div v-if="showBuyNowSuccess" class="success-message">
      <p>✅ 订单创建成功，正在跳转...</p>
    </div>
  </div>
</template>

<script>
import { goodsAPI, cartAPI, orderAPI, apiUtils } from '../services/api.js';

export default {
  name: 'ProductDetail',
  data() {
    return {
      product: null,
      merchantInfo: null,
      loading: false,
      error: '',
      quantity: 1,
      showAddCartSuccess: false,
      showBuyNowSuccess: false,
      merchantLoading: false,
      merchantError: '',
      buying: false
    };
  },
  computed: {
    isLoggedIn() {
      return apiUtils.isLoggedIn();
    },
    productId() {
      return this.$route.params.id;
    }
  },
  watch: {
    '$route.params.id': {
      immediate: true,
      handler(newId) {
        if (newId) {
          this.loadProductDetail();
        }
      }
    }
  },
  methods: {
    //统一商品图片URL获取逻辑
    getProductImageUrl(product) {
      if (!product) {
        return this.getDefaultImageUrl();
      }
      const possibleImageFields = ['imageUrl', 'image', 'imgUrl', 'picture', 'fullImageUrl'];
      for (const field of possibleImageFields) {
        if (product[field]) {
          const imageUrl = product[field];
          return this.processImageUrl(imageUrl);
        }
      }
      return this.getDefaultImageUrl();
    },

    processImageUrl(imageUrl) {
      if (!imageUrl || imageUrl === 'null' || imageUrl === 'undefined') {
        return this.getDefaultImageUrl();
      }
      if (imageUrl.startsWith('http')) {
        return imageUrl;
      }
      if (imageUrl.startsWith('/')) {
        return imageUrl;
      }
      return '/uploads/products/' + imageUrl;
    },

    getDefaultImageUrl() {
      return '/uploads/products/defaultpicture.png';
    },

    //获取头像URL
    getAvatarUrl(avatarUrl) {
      if (!avatarUrl || avatarUrl === 'null' || avatarUrl === 'undefined') {
        return '/uploads/avatars/defaultpicture.png';
      }
      if (avatarUrl.startsWith('http')) {
        return avatarUrl;
      }
      if (avatarUrl.startsWith('/')) {
        return avatarUrl;
      }
      return '/uploads/avatars/' + avatarUrl;
    },

    handleImageError(event) {
      console.warn('商品图片加载失败，使用默认图片:', event.target.src);
      event.target.src = this.getDefaultImageUrl();
    },

    handleAvatarError(event) {
      console.warn('头像加载失败，使用默认头像:', event.target.src);
      event.target.src = '/uploads/avatars/defaultpicture.png';
    },

    async loadProductDetail() {
      this.loading = true;
      this.error = '';
      try {
        const response = await goodsAPI.getById(this.productId);
        const responseData = apiUtils.handleResponse(response);
        this.product = responseData.goods || responseData;
        if (!this.product) {
          throw new Error('商品信息不存在');
        }
        //调试信息
        console.log('商品详情数据:', this.product);
        console.log('商品图片字段:', {
          imageUrl: this.product.imageUrl,
          image: this.product.image,
          fullImageUrl: this.product.fullImageUrl
        });
        await this.loadMerchantInfo();
      } catch (error) {
        console.error('加载商品详情失败:', error);
        this.error = error.message || '加载商品信息失败，请稍后重试';
      } finally {
        this.loading = false;
      }
    },

    async loadMerchantInfo() {
      if (!this.product || !this.product.gid) {
        this.merchantError = '商品信息不完整，无法获取商家信息';
        return;
      }
      this.merchantLoading = true;
      this.merchantError = '';
      try {
        const response = await goodsAPI.getMerchantByGoodsId(this.product.gid);
        const responseData = apiUtils.handleResponse(response);
        this.merchantInfo = responseData.merchant || responseData;
        if (!this.merchantInfo) {
          throw new Error('商家信息不存在');
        }
      } catch (error) {
        console.error('加载商家信息失败:', error);
        this.merchantError = error.message || '无法加载商家信息';
      } finally {
        this.merchantLoading = false;
      }
    },

    validateQuantity() {
      if (this.quantity < 1) {
        this.quantity = 1;
      } else if (this.quantity > this.product.remaining) {
        this.quantity = this.product.remaining;
      }
    },

    increaseQuantity() {
      if (this.quantity < this.product.remaining) {
        this.quantity++;
      }
    },

    decreaseQuantity() {
      if (this.quantity > 1) {
        this.quantity--;
      }
    },

    async addToCart() {
      if (!this.isLoggedIn) {
        this.$router.push('/login');
        return;
      }

      if (this.product.remaining <= 0) {
        this.error = '商品已售罄罄';
        return;
      }

      try {
        //使用正确的商品ID字段
        const response = await cartAPI.addToCart({
          gid: this.product.gid,  //使用gid而不是goodsId
          quantity: this.quantity
        });
        apiUtils.handleResponse(response);
        this.showAddCartSuccess = true;
        setTimeout(() => {
          this.showAddCartSuccess = false;
        }, 3000);
      } catch (error) {
        console.error('添加到购物车失败:', error);
        this.error = error.message || '添加到购物车失败，请稍后重试';
      }
    },

    //直接创建订单
    async buyNow() {
      if (!this.isLoggedIn) {
        this.$router.push('/login');
        return;
      }
      if (this.product.remaining <= 0) {
        this.error = '商品已售罄罄';
        return;
      }
      this.buying = true;
      try {
        console.log('开始直接购买流程');
        console.log('商品信息:', {
          gid: this.product.gid,
          name: this.product.name,
          quantity: this.quantity,
          price: this.product.price
        });
        const isReallyLoggedIn = await this.checkRealLoginStatus();
        if (!isReallyLoggedIn) {
          return;
        }
        //构造订单数据
        const orderItems = [{
          gid: this.product.gid,
          quantity: this.quantity
        }];
        console.log('发送给后端的订单数据:', orderItems);
        //直接调用创建订单API
        const response = await orderAPI.createOrder(orderItems);
        const data = apiUtils.handleResponse(response);
        console.log('直接购买订单创建成功:', data);
        //显示成功消息
        this.showBuyNowSuccess = true;
        this.showMessage('订单创建成功，正在跳转...', 'success');
        //确保使用正确的订单ID进行跳转
        let orderId = data.orderId || data.oid;
        if (!orderId && data.order) {
          orderId = data.order.oid || data.order.orderId;
        }
        if (!orderId) {
          throw new Error('订单创建成功但未返回有效的订单ID');
        }
        console.log('直接购买创建的订单ID:', orderId);
        //延迟跳转，让用户看到成功消息
        setTimeout(() => {
          this.$router.push(`/order/${orderId}`);
        }, 1000);
      } catch (error) {
        console.error('直接购买失败:', error);
        this.showMessage('直接购买失败：' + error.message, 'error');
      } finally {
        this.buying = false;
        this.showBuyNowSuccess = false;
      }
    },

    //综合检查登录状态
    async checkRealLoginStatus() {
      try {
        //检查本地登录状态
        if (!this.isLoggedIn) {
          console.log('本地未登录，跳转到登录页');
          this.$router.push('/login')
          return false;
        }
        //检查后端Session状态
        console.log('检查后端Session状态...');
        const sessionValid = await apiUtils.checkLoginStatus();
        if (!sessionValid) {
          console.log('后端Session无效，需要重新登录');
          this.showMessage('登录已过期，请重新登录', 'error');
          this.$router.push('/login');
          return false;
        }
        console.log('登录状态正常');
        return true;
      } catch (error) {
        console.error('登录状态检查失败:', error);
        this.showMessage('登录状态检查失败，请重新登录', 'error');
        this.$router.push('/login');
        return false;
      }
    },

    goToMerchantPage() {
      if (this.merchantInfo && this.merchantInfo.uid) {
        this.$router.push(`/merchant/${this.merchantInfo.uid}`);
      }
    },

    retryLoad() {
      this.loadProductDetail();
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
};
</script>

<style scoped>
.product-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.loading-container {
  text-align: center;
  padding: 60px 20px;
}
.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
.error-container {
  text-align: center;
  padding: 60px 20px;
  background-color: #fef0f0;
  border-radius: 8px;
  margin: 20px 0;
}
.error-icon {
  font-size: 48px;
  margin-bottom: 20px;
}
.retry-btn {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 15px;
}
.retry-btn:hover {
  background-color: #66b1ff;
}
.breadcrumb {
  margin-bottom: 20px;
  font-size: 14px;
}
.breadcrumb a {
  color: #409eff;
  text-decoration: none;
}
.breadcrumb a:hover {
  text-decoration: underline;
}
.separator {
  margin: 0 10px;
  color: #999;
}
.current {
  color: #666;
}
.product-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  margin-bottom: 40px;
}
.product-image-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.main-image img {
  width: 100%;
  height: 400px;
  object-fit: contain;
  border-radius: 4px;
}
.product-info-section {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.product-name {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #333;
}
.price-section {
  margin-bottom: 20px;
}
.current-price {
  font-size: 28px;
  color: #f56c6c;
  font-weight: bold;
}
.stock-section {
  margin-bottom: 25px;
  font-size: 16px;
}
.stock-label {
  color: #666;
}
.stock-count {
  font-weight: bold;
}
.low-stock {
  color: #e6a23c;
}
.stock-warning {
  color: #e6a23c;
  font-size: 14px;
}
.description-section {
  margin-bottom: 25px;
}
.description-section h3 {
  margin-bottom: 10px;
  color: #333;
}
.product-description {
  line-height: 1.6;
  color: #666;
}
.action-section {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}
.quantity-selector {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}
.quantity-selector label {
  margin-right: 15px;
  font-weight: 500;
}
.quantity-controls {
  display: flex;
  align-items: center;
}
.quantity-btn {
  width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  background: white;
  font-size: 16px;
  cursor: pointer;
}
.quantity-btn:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
  color: #ccc;
}
.quantity-controls input {
  width: 60px;
  height: 36px;
  border: 1px solid #ddd;
  border-left: none;
  border-right: none;
  text-align: center;
  margin: 0 -1px;
}
.action-buttons {
  display: flex;
  gap: 15px;
}
.add-cart-btn, .buy-now-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}
.add-cart-btn {
  background-color: #fff;
  color: #409eff;
  border: 1px solid #409eff;
}
.add-cart-btn:hover:not(:disabled) {
  background-color: #ecf5ff;
}
.buy-now-btn {
  background-color: #409eff;
  color: white;
}
.buy-now-btn:hover:not(:disabled) {
  background-color: #66b1ff;
}
.add-cart-btn:disabled, .buy-now-btn:disabled {
  background-color: #ccc;
  color: #999;
  cursor: not-allowed;
  border-color: #ccc;
}
.login-prompt {
  text-align: center;
  margin-top: 15px;
  padding: 10px;
  background-color: #fff;
  border-radius: 4px;
}
.login-prompt a {
  color: #409eff;
  text-decoration: none;
}
.login-prompt a:hover {
  text-decoration: underline;
}
.merchant-section {
  margin-bottom: 30px;
}
.merchant-section h3 {
  margin-bottom: 15px;
  color: #333;
}
.merchant-info-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  transition: all 0.3s ease;
  position: relative;
}
.merchant-info-card.clickable {
  cursor: pointer;
}
.merchant-info-card.clickable:hover {
  background-color: #e9ecef;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.merchant-avatar {
  margin-right: 15px;
}
.merchant-avatar img {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #dee2e6;
}
.merchant-details {
  flex: 1;
}
.merchant-details h4 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 18px;
}
.merchant-badge {
  display: inline-block;
  background-color: #409eff;
  color: white;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.merchant-arrow {
  font-size: 20px;
  color: #6c757d;
  margin-left: 10px;
}
.merchant-loading, .merchant-error {
  text-align: center;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
  color: #666;
}
.merchant-error {
  color: #dc3545;
  background-color: #f8d7da;
}
.success-message {
  position: fixed;
  top: 20px;
  right: 20px;
  background-color: #67c23a;
  color: white;
  padding: 15px 20px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  animation: slideIn 0.3s ease-out;
}
@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
@media (max-width: 768px) {
  .product-content {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  .action-buttons {
    flex-direction: column;
  }
  .product-detail-container {
    padding: 10px;
  }
  .merchant-info-card {
    padding: 15px;
  }
  .merchant-avatar img {
    width: 50px;
    height: 50px;
  }
  .merchant-details h4 {
    font-size: 16px;
  }
}
</style>