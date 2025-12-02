<template>
  <div class="products-container">
    <div class="products-header">
      <h2>商品列表</h2>
      <div class="search-info" v-if="searchKeyword">
        搜索关键词: "{{ searchKeyword }}"
        <button @click="clearSearch" class="clear-search-btn">清除搜索</button>
      </div>
    </div>
    <div class="filters-section">
      <div class="filters-row">
        <div class="filter-group">
          <label>价格范围:</label>
          <div class="price-inputs">
            <input
                type="number"
                v-model.number="minPrice"
                placeholder="最低价"
                class="price-input"
                min="0"
            >
            <span class="price-separator">-</span>
            <input
                type="number"
                v-model.number="maxPrice"
                placeholder="最高价"
                class="price-input"
                min="0"
            >
            <button @click="searchByPrice" class="filter-btn price-filter-btn">筛选</button>
            <button @click="clearPriceFilter" class="filter-btn clear-btn">清除</button>
          </div>
        </div>
        <div class="filter-group">
          <label>排序方式:</label>
          <select v-model="sortBy" @change="applySorting" class="sort-select">
            <option value="default">默认排序</option>
            <option value="price_asc">价格从低到高</option>
            <option value="price_desc">价格从高到低</option>
            <option value="stock_desc">库存最多</option>
          </select>
        </div>
        <div class="filter-group">
          <button
              @click="toggleStockFilter"
              :class="['filter-btn', 'stock-filter-btn', { active: showAvailableOnly }]"
          >
            {{ showAvailableOnly ? '显示所有商品' : '仅显示有库存' }}
          </button>
        </div>
      </div>
      <div class="filter-stats">
        <span class="product-count">共找到 {{ filteredProducts.length }} 件商品</span>
        <span v-if="minPrice || maxPrice" class="price-filter-info">
          {{ priceFilterText }}
        </span>
      </div>
    </div>
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>正在加载商品...</p>
    </div>
    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <p>加载失败: {{ error }}</p>
      <button @click="loadProducts" class="retry-btn">重试</button>
    </div>
    <!-- 无商品状态 -->
    <div v-else-if="filteredProducts.length === 0" class="no-products-state">
      <div class="no-products-content">
        <div class="no-products-icon">📦📦📦📦</div>
        <h3>没有找到符合条件的商品</h3>
        <p>尝试调整筛选条件或清除搜索</p>
        <div class="no-products-actions">
          <button @click="clearAllFilters" class="action-btn primary">清除所有筛选</button>
          <button @click="loadProducts" class="action-btn secondary">重新加载</button>
        </div>
      </div>
    </div>
    <!-- 商品网格 -->
    <div v-else class="products-grid">
      <ProductCard
          v-for="product in filteredProducts"
          :key="product.gid"
          :product="product"
          @cart-updated="handleCartUpdate"
      />
    </div>
  </div>
</template>

<script>
import ProductCard from '../components/ProductCard.vue'
import { goodsAPI } from '../services/api'

export default {
  name: 'ProductsView',
  components: {
    ProductCard
  },
  data() {
    return {
      allProducts: [],
      filteredProducts: [],
      loading: false,
      error: null,
      searchKeyword: '',
      minPrice: null,
      maxPrice: null,
      showAvailableOnly: false,
      sortBy: 'default'
    }
  },
  computed: {
    isLoggedIn() {
      const userStr = localStorage.getItem('currentUser');
      if (!userStr || userStr === 'undefined' || userStr === 'null') {
        return false;
      }
      try {
        const user = JSON.parse(userStr);
        return !!(user && user.uid);
      } catch (error) {
        return false;
      }
    },
    priceFilterText() {
      if (this.minPrice && this.maxPrice) {
        return `价格: ¥${this.minPrice} - ¥${this.maxPrice}`
      } else if (this.minPrice) {
        return `价格: ≥ ¥${this.minPrice}`
      } else if (this.maxPrice) {
        return `价格: ≤ ¥${this.maxPrice}`
      }
      return ''
    }
  },
  watch: {
    '$route.query.search': {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.searchKeyword = newVal
          this.searchProducts(newVal)
        } else {
          this.searchKeyword = ''
          this.applyFilters()
        }
      }
    },
    showAvailableOnly() {
      this.applyFilters()
    },
    sortBy() {
      this.applySorting()
    }
  },
  async mounted() {
    await this.loadProducts()
  },
  methods: {
    async loadProducts() {
      this.loading = true
      this.error = null
      try {
        const response = await goodsAPI.getAvailable()
        if (response.success) {
          this.allProducts = response.data || response.goods || []
          console.log('商品列表数据:', this.allProducts);
          this.allProducts.forEach((product, index) => {
            console.log(`商品 ${index + 1}:`, {
              name: product.name,
              imageUrl: product.imageUrl,
              image: product.image,
              fullImageUrl: product.fullImageUrl
            });
          });
          this.applyFilters()
        } else {
          throw new Error(response.message || '获取商品失败')
        }
      } catch (error) {
        console.error('加载商品失败:', error)
      } finally {
        this.loading = false
      }
    },

    async searchProducts(keyword) {
      this.loading = true
      try {
        const response = await goodsAPI.searchByName(keyword)
        if (response.success) {
          this.allProducts = response.data || response.goods || []
          this.applyFilters()
        } else {
          throw new Error(response.message || '搜索失败')
        }
      } catch (error) {
        console.error('搜索商品失败:', error)
        this.allProducts = this.allProducts.filter(product =>
            product.name.toLowerCase().includes(keyword.toLowerCase())
        )
        this.applyFilters()
      } finally {
        this.loading = false
      }
    },

    async searchByPrice() {
      if (!this.minPrice && !this.maxPrice) {
        this.applyFilters()
        return
      }
      this.loading = true
      try {
        const min = this.minPrice || 0
        const max = this.maxPrice || Number.MAX_SAFE_INTEGER
        const response = await goodsAPI.searchByPrice(min, max)
        if (response.success) {
          this.allProducts = response.data || response.goods || []
          this.applyFilters()
        } else {
          throw new Error(response.message || '价格筛选失败')
        }
      } catch (error) {
        console.error('按价格搜索失败:', error)
        this.allProducts = this.allProducts.filter(product => {
          const price = product.price
          return price >= (this.minPrice || 0) && price <= (this.maxPrice || Number.MAX_SAFE_INTEGER)
        })
        this.applyFilters()
      } finally {
        this.loading = false
      }
    },

    applyFilters() {
      let filtered = [...this.allProducts]
      if (this.showAvailableOnly) {
        filtered = filtered.filter(product => product.remaining > 0)
      }
      if (this.minPrice || this.maxPrice) {
        filtered = filtered.filter(product => {
          const price = product.price
          return price >= (this.minPrice || 0) && price <= (this.maxPrice || Number.MAX_SAFE_INTEGER)
        })
      }
      this.filteredProducts = filtered
      this.applySorting()
    },

    applySorting() {
      if (this.sortBy === 'default') return
      this.filteredProducts.sort((a, b) => {
        switch (this.sortBy) {
          case 'price_asc':
            return a.price - b.price
          case 'price_desc':
            return b.price - a.price
          case 'stock_desc':
            return b.remaining - a.remaining
          default:
            return 0
        }
      })
    },

    toggleStockFilter() {
      this.showAvailableOnly = !this.showAvailableOnly
    },

    clearSearch() {
      this.searchKeyword = ''
      this.$router.push({ query: { ...this.$route.query, search: undefined } })
      this.loadProducts()
    },

    clearPriceFilter() {
      this.minPrice = null
      this.maxPrice = null
      this.applyFilters()
    },

    clearAllFilters() {
      this.searchKeyword = ''
      this.minPrice = null
      this.maxPrice = null
      this.showAvailableOnly = false
      this.sortBy = 'default'
      this.$router.push({ query: {} })
      this.loadProducts()
    },

    handleCartUpdate() {
      this.showMessage('商品已加入购物车', 'success')
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
.products-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 80vh;
}
.products-header {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e0e0e0;
}
.products-header h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 15px;
}
.search-info {
  background: #e3f2fd;
  padding: 10px 20px;
  border-radius: 20px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}
.clear-search-btn {
  padding: 4px 12px;
  background: #ff5722;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
}
.clear-search-btn:hover {
  background: #e64a19;
}
.filters-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}
.filters-row {
  display: flex;
  align-items: center;
  gap: 30px;
  flex-wrap: wrap;
  margin-bottom: 15px;
}
.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}
.filter-group label {
  font-weight: 500;
  color: #333;
  white-space: nowrap;
}
.price-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}
.price-input {
  width: 100px;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}
.price-input:focus {
  outline: none;
  border-color: #409eff;
}
.price-separator {
  color: #666;
}
.sort-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
}
.filter-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}
.filter-btn:hover {
  background: #f5f5f5;
}
.filter-btn.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
}
.price-filter-btn {
  background: #409eff;
  color: white;
  border-color: #409eff;
}
.price-filter-btn:hover {
  background: #66b1ff;
}
.clear-btn {
  background: #ff5722;
  color: white;
  border-color: #ff5722;
}
.clear-btn:hover {
  background: #ff7043;
}
.stock-filter-btn.active {
  background: #4caf50;
  border-color: #4caf50;
}
.filter-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}
.product-count {
  font-weight: 500;
  color: #333;
}
.price-filter-info {
  color: #666;
  font-size: 14px;
}
.loading-state {
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
.error-state {
  text-align: center;
  padding: 60px 20px;
  color: #f56c6c;
}
.retry-btn {
  margin-top: 15px;
  padding: 10px 20px;
  background: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.retry-btn:hover {
  background: #f78989;
}
.no-products-state {
  text-align: center;
  padding: 80px 20px;
}
.no-products-content {
  max-width: 400px;
  margin: 0 auto;
}
.no-products-icon {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}
.no-products-content h3 {
  color: #666;
  margin-bottom: 10px;
}
.no-products-content p {
  color: #999;
  margin-bottom: 25px;
}
.no-products-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
}
.action-btn {
  padding: 10px 20px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
}
.action-btn.primary {
  background: #409eff;
  color: white;
  border-color: #409eff;
}
.action-btn.primary:hover {
  background: #66b1ff;
}
.action-btn.secondary {
  background: #f5f5f5;
  color: #666;
}
.action-btn.secondary:hover {
  background: #e0e0e0;
}
.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}
@media (max-width: 768px) {
  .products-container {
    padding: 15px;
  }

  .filters-row {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }
  .filter-group {
    flex-direction: column;
    align-items: stretch;
  }
  .price-inputs {
    justify-content: flex-start;
  }
  .products-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 15px;
  }
  .filter-stats {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  .no-products-actions {
    flex-direction: column;
  }
}
@media (max-width: 480px) {
  .products-grid {
    grid-template-columns: 1fr;
  }
  .price-inputs {
    flex-wrap: wrap;
  }
  .price-input {
    width: 80px;
  }
}
</style>