<template>
  <div class="merchant-view">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>正在加载商家信息...</p>
    </div>
    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <div class="error-icon">❌</div>
      <h3>加载失败</h3>
      <p>{{ error }}</p>
      <button @click="retryLoad" class="retry-btn">重试</button>
    </div>
    <!-- 商家详情内容 -->
    <div v-else-if="merchant" class="merchant-content">
      <!-- 商家头部信息 -->
      <div class="merchant-header">
        <div class="merchant-avatar-section">
          <div class="avatar-container">
            <img
                :src="getAvatarUrl(merchant.avatarUrl)"
                :alt="merchant.nickname"
                class="merchant-avatar"
                @error="handleAvatarError"
            >
          </div>
        </div>
        <div class="merchant-info-section">
          <h1 class="merchant-name">{{ merchant.nickname || '商家' }}</h1>
          <div class="merchant-description">
            <p>{{ merchant.description || merchant.address || '这个商家还没有填写描述信息。' }}</p>
          </div>
        </div>
      </div>
      <!-- 商品列表 -->
      <div class="merchant-products">
        <h2 class="section-title">商家商品 ({{ products.length }})</h2>
        <!-- 商品加载状态 -->
        <div v-if="productsLoading" class="products-loading">
          <div class="loading-spinner"></div>
          <p>正在加载商品...</p>
        </div>
        <!-- 商品列表 -->
        <div v-else-if="products.length > 0" class="products-grid">
          <ProductCard
              v-for="product in products"
              :key="product.gid || product.id"
              :product="product"
              @cart-updated="handleCartUpdated"
              @show-message="showMessage"
          />
        </div>
        <!-- 空状态 -->
        <div v-else class="empty-products">
          <div class="empty-icon">📦</div>
          <p>该商家暂无商品</p>
        </div>
      </div>
    </div>
    <!-- 消息提示 -->
    <div v-if="message" :class="['message', messageType]">
      {{ message }}
    </div>
  </div>
</template>

<script>
import { merchantAPI, goodsAPI, orderAPI, apiUtils } from '../services/api'
import ProductCard from '../components/ProductCard.vue'

export default {
  name: 'MerchantView',
  components: {
    ProductCard
  },
  data() {
    return {
      merchant: null,
      products: [],
      stats: {
        totalProducts: 0,
        totalSales: 0
      },
      loading: false,
      productsLoading: false,
      error: '',
      message: '',
      messageType: 'info'
    }
  },
  computed: {
    isLoggedIn() {
      return apiUtils.isLoggedIn()
    },
    currentUser() {
      return apiUtils.getCurrentUser()
    },
    merchantId() {
      return this.$route.params.id
    }
  },
  watch: {
    '$route.params.id': {
      immediate: true,
      handler(newId) {
        if (newId) {
          this.loadMerchantData()
        }
      }
    }
  },
  methods: {
    async loadMerchantData() {
      this.loading = true
      this.error = ''
      try {
        //并行加载商家信息和商品列表
        await Promise.all([
          this.loadMerchantInfo(),
          this.loadMerchantProducts()
        ])
      } catch (error) {
        console.error('加载商家数据失败:', error)
        this.error = error.message || '加载商家信息失败，请稍后重试'
      } finally {
        this.loading = false
      }
    },

    async loadMerchantInfo() {
      try {
        //使用正确的API方法
        const response = await merchantAPI.getMerchantById(this.merchantId)
        const responseData = apiUtils.handleResponse(response)
        this.merchant = responseData.merchant || responseData
        if (!this.merchant) {
          throw new Error('商家信息不存在')
        }
        //加载商家统计信息
        await this.loadMerchantStats()
      } catch (error) {
        console.error('加载商家信息失败:', error)
        throw new Error('无法加载商家信息')
      }
    },

    async loadMerchantStats() {
      try {
        //获取商家的所有商品
        const goodsResponse = await goodsAPI.getByMerchant(this.merchantId);
        const goodsData = apiUtils.handleResponse(goodsResponse);
        const goodsList = Array.isArray(goodsData) ? goodsData :
            goodsData.goods || goodsData.products || [];
        //商品数量就是商品列表的长度
        const totalProducts = goodsList.length;
        //获取所有订单
        const ordersResponse = await orderAPI.getAll();
        const ordersData = apiUtils.handleResponse(ordersResponse);
        const allOrders = Array.isArray(ordersData) ? ordersData :
            ordersData.orders || [];
        //过滤出该商家的订单
        const merchantOrders = allOrders.filter(order =>
            order.merchant && order.merchant.uid === this.merchantId
        );
        //计算总销量
        let totalSales = 0;
        merchantOrders.forEach(order => {
          totalSales += order.totalAmount || 0;
        });
        //设置统计信息
        this.stats = {
          totalProducts: totalProducts,
          totalSales: totalSales
        };
        console.log('商家统计信息:', this.stats);
      } catch (error) {
        console.warn('加载商家统计信息失败:', error);
        //统计信息加载失败不影响主要功能，使用默认值
        this.stats = {
          totalProducts: 0,
          totalSales: 0
        };
      }
    },

    async loadMerchantProducts() {
      this.productsLoading = true
      try {
        const response = await goodsAPI.getByMerchant(this.merchantId)
        const responseData = apiUtils.handleResponse(response)
        this.products = Array.isArray(responseData) ? responseData :
            responseData.goods || responseData.products || []
        console.log('商家商品列表:', this.products)
      } catch (error) {
        console.error('加载商家商品失败:', error)
        this.products = []
        //商品加载失败不影响商家信息的显示
      } finally {
        this.productsLoading = false
      }
    },

    //头像URL处理
    getAvatarUrl(avatarUrl) {
      if (!avatarUrl || avatarUrl === 'null' || avatarUrl === 'undefined') {
        return '/uploads/avatars/defaultpicture.png'
      }
      if (avatarUrl.startsWith('http')) {
        return avatarUrl
      }
      if (avatarUrl.startsWith('/')) {
        return avatarUrl
      }
      return '/uploads/avatars/' + avatarUrl
    },
    handleAvatarError(event) {
      console.warn('头像加载失败，使用默认头像')
      event.target.src = '/uploads/avatars/defaultpicture.png'
    },
    handleCartUpdated() {
      this.showMessage('商品已加入购物车', 'success')
    },
    handleLogout() {
      apiUtils.clearUser()
      this.$router.push('/login')
      this.showMessage('已退出登录', 'info')
    },
    retryLoad() {
      this.loadMerchantData()
    },
    showMessage(text, type = 'info') {
      this.message = text
      this.messageType = type
      setTimeout(() => {
        this.message = ''
      }, 3000)
    }
  }
}
</script>

<style scoped>
.merchant-view {
  min-height: 100vh;
  background-color: #f5f5f5;
}
.loading-container, .products-loading {
  text-align: center;
  padding: 100px 20px;
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
  padding: 100px 20px;
  background-color: #fef0f0;
  border-radius: 8px;
  margin: 20px;
  border: 1px solid #f56c6c;
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
  transition: background-color 0.3s;
}
.retry-btn:hover {
  background-color: #66b1ff;
}
.merchant-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.merchant-header {
  display: flex;
  background: white;
  border-radius: 12px;
  padding: 30px;
  margin-bottom: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  gap: 30px;
  align-items: flex-start;
}
.merchant-avatar-section {
  flex-shrink: 0;
}
.avatar-container {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid #e8f4ff;
  background: #f8f9fa;
}
.merchant-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.merchant-info-section {
  flex: 1;
}
.merchant-name {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
}
.merchant-description {
  color: #666;
  line-height: 1.6;
}
.merchant-description p {
  margin: 0;
  font-size: 16px;
}
.merchant-products {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.section-title {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 2px solid #409eff;
}
.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 25px;
}
.empty-products {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}
.message {
  position: fixed;
  top: 100px;
  right: 20px;
  padding: 12px 20px;
  border-radius: 6px;
  color: white;
  font-size: 14px;
  z-index: 1000;
  max-width: 300px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.message.info {
  background: #909399;
}
.message.success {
  background: #67c23a;
}
.message.warning {
  background: #e6a23c;
}
.message.error {
  background: #f56c6c;
}
@media (max-width: 768px) {
  .merchant-content {
    padding: 15px;
  }
  .merchant-header {
    flex-direction: column;
    text-align: center;
    padding: 20px;
    gap: 20px;
  }
  .avatar-container {
    margin: 0 auto;
  }
  .merchant-stats {
    justify-content: center;
    gap: 30px;
  }
  .merchant-name {
    font-size: 24px;
  }
  .products-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 20px;
  }
  .merchant-products {
    padding: 20px;
  }
}
@media (max-width: 480px) {
  .merchant-stats {
    flex-direction: column;
    gap: 20px;
  }
  .products-grid {
    grid-template-columns: 1fr;
  }
}
</style>