<template>
  <div class="home">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>正在加载商品...</p>
    </div>
    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <p>加载失败: {{ error }}</p>
      <button class="retry-button" @click="loadProducts">重试</button>
    </div>
    <!-- 商品列表 -->
    <div v-else class="goods-list">
      <div class="goods-grid">
        <ProductCard
            v-for="goods in products"
            :key="goods.gid"
            :product="goods"
            @cart-updated="handleCartUpdated"
        />
      </div>
      <!-- 空状态 -->
      <div v-if="products.length === 0" class="empty-state">
        <div class="empty-icon">📦📦📦📦</div>
        <p>暂无商品数据</p>
      </div>
    </div>
    <!-- 消息提示 -->
    <div v-if="message" :class="['message', messageType]">
      {{ message }}
    </div>
  </div>
</template>

<script>
import { goodsAPI } from '../services/api.js'
import ProductCard from '../components/ProductCard.vue'

export default {
  name: 'HomeView',
  components: {
    ProductCard
  },
  data() {
    return {
      products: [],
      loading: false,
      message: '',
      messageType: 'info',
      error: ''
    }
  },
  mounted() {
    this.loadProducts()
  },
  methods: {
    async loadProducts() {
      this.loading = true
      this.error = ''
      try {
        const response = await goodsAPI.getAvailable()
        if (response && response.success) {
          this.products = response.goods || response.data || []
          if (this.products.length === 0) {
            this.showMessage('暂无商品数据', 'info')
          }
        } else {
          const errorMsg = response?.message || '加载商品失败'
          this.error = errorMsg
          this.showMessage(errorMsg, 'error')
        }
      } catch (error) {
        let errorMessage = '加载商品失败'
        if (error.response) {
          if (error.response.status === 403) {
            errorMessage = '权限不足，无法访问商品列表'
          } else if (error.response.status === 404) {
            errorMessage = '商品接口不存在'
          } else if (error.response.status >= 500) {
            errorMessage = '服务器内部错误'
          }
        } else if (error.message) {
          errorMessage = error.message
        }
        this.error = errorMessage
        this.showMessage(errorMessage, 'error')
      } finally {
        this.loading = false
      }
    },
    handleCartUpdated() {
      this.showMessage('商品已加入购物车', 'success')
    },
    showMessage(text, type = 'info') {
      this.message = text
      this.messageType = type
      setTimeout(() => {this.message = ''}, 3000)
    }
  }
}
</script>

<style scoped>
.home {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.loading-container {
  text-align: center;
  padding: 60px 0;
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
.error-state {
  text-align: center;
  padding: 60px 0;
  color: #f56c6c;
}
.retry-button {
  margin-top: 10px;
  padding: 10px 20px;
  background: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.retry-button:hover {
  background: #f78989;
}
.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #666;
}
.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}
.message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 20px;
  border-radius: 4px;
  color: white;
  font-size: 14px;
  z-index: 1000;
  max-width: 300px;
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
  .home {
    padding: 10px;
  }
  .goods-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 15px;
  }
}
@media (max-width: 480px) {
  .goods-grid {
    grid-template-columns: 1fr;
  }
}
</style>