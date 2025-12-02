<template>
  <div class="product-card" @click="goToProductDetail">
    <div class="product-image">
      <img :src=" getProductImageUrl ( product )" @error="handleImageError" class="product-img">
      <div v-if="product.remaining === 0" class="out-of-stock-label">已售罄</div>
      <div v-else-if="product.remaining < 10" class="low-stock-label">库存紧张</div>
    </div>
    <div class="product-info">
      <h3 class="product-name">{{ product.name }}</h3>
      <p class="product-description">{{ product.description || '暂无描述' }}</p>
      <div class="price-section">
        <span class="current-price">¥{{ getProductPrice(product) }}</span>
      </div>
      <div class="product-meta">
        <span class="stock" :class="{ 'low-stock': product.remaining <= 5 }">
          库存: {{ getProductStock(product) }}
        </span>
      </div>
    </div>
    <div class="product-actions">
      <button
          @click.stop="addToCart"
          :disabled="!isProductAvailable(product)"
          class="add-cart-btn"
      >
        {{ isProductAvailable(product) ? '加入购物车' : '已售罄' }}
      </button>
    </div>
  </div>
</template>
<script>
export default {
  name: 'ProductCard',
  props: {
    product: {
      type: Object,
      required: true,
      //确保product是有效对象
      validator(value) {
        return value && typeof value === 'object' && value.gid
      }
    }
  },
  computed: {
    isLoggedIn() {
      const userStr = localStorage.getItem('currentUser');
      return userStr && userStr !== 'undefined' && userStr !== 'null';
    }
  },
  methods: {
    //获取商品价格
    getProductPrice(product) {
      if (!product || typeof product.price === 'undefined' || product.price === null) {
        return '0.00';
      }
      try {
        return Number(product.price).toFixed(2);
      } catch (error) {
        console.warn('价格格式化错误:', error);
        return '0.00';
      }
    },
    //获取商品库存
    getProductStock(product) {
      if (!product || typeof product.remaining === 'undefined' || product.remaining === null) {
        return 0;
      }
      return product.remaining;
    },
    //检查商品是否可购买
    isProductAvailable(product) {
      return product && this.getProductStock(product) > 0;
    },
    getProductImageUrl(product) {
      if (!product) {
        return this.getDefaultImageUrl();
      }
      //优先检查常见的图片字段名
      const possibleImageFields = ['imageUrl', 'image', 'imgUrl', 'picture', 'fullImageUrl'];
      for (const field of possibleImageFields) {
        if (product[field]) {
          const imageUrl = product[field];
          return this.processImageUrl(imageUrl);
        }
      }
      //如果商品有图片路径但不在常见字段中，尝试直接处理
      if (product.imageUrl) {
        return this.processImageUrl(product.imageUrl);
      }
      return this.getDefaultImageUrl();
    },

    processImageUrl(imageUrl) {
      if (!imageUrl || imageUrl === 'null' || imageUrl === 'undefined') {
        return this.getDefaultImageUrl();
      }
      //如果已经是完整URL，直接返回
      if (imageUrl.startsWith('http')) {
        return imageUrl;
      }
      //处理相对路径
      if (imageUrl.startsWith('/')) {
        //如果已经是绝对路径，直接加上后端地址
        return imageUrl;
      }
      //默认图片在后端的uploads/products目录下
      return '/uploads/products/' + imageUrl;
    },
    getDefaultImageUrl() {
      return '/uploads/products/default-product.png';
    },
    handleImageError(event) {
      console.warn('图片加载失败，使用默认图片:', event.target.src);
      event.target.src = this.getDefaultImageUrl();
    },
    goToProductDetail() {
      if (!this.isProductAvailable(this.product)) return;
      //确保商品ID存在
      if (!this.product.gid) {
        console.error('商品ID不存在，无法跳转到详情页');
        return;
      }
      this.$router.push(`/product/${this.product.gid}`);
    },
    async addToCart(event) {
      event.stopPropagation();
      if (!this.isLoggedIn) {
        this.$emit('show-message', '请先登录', 'warning');
        setTimeout(() => this.$router.push('/login'), 1000);
        return;
      }
      if (!this.isProductAvailable(this.product)) {
        this.$emit('show-message', '该商品已售罄', 'warning');
        return;
      }
      try {
        const response = await fetch('/api/cart/add', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            gid: this.product.gid,
            quantity: 1
          }),
          credentials: 'include'
        });
        const data = await response.json();
        if (data.success) {
          this.$emit('show-message', '商品已加入购物车', 'success');
          this.$emit('cart-updated');
        } else {
          throw new Error(data.message || '加入购物车失败');
        }
      } catch (error) {
        console.error('加入购物车失败:', error);
        this.$emit('show-message', '加入购物车失败: ' + error.message, 'error');
      }
    }
  }
};
</script>

<style scoped>
.product-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}
.product-image {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f8f9fa;
}
.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.product-card:hover .product-img {
  transform: scale(1.05);
}
.out-of-stock-label,
.low-stock-label {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: white;
  z-index: 2;
}
.out-of-stock-label {
  background: rgba(108, 117, 125, 0.9);
}
.low-stock-label {
  background: rgba(255, 193, 7, 0.9);
}
.product-info {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.3;
}
.product-description {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
  margin-bottom: 12px;
  flex: 1;
}
.price-section {
  margin-bottom: 8px;
}
.current-price {
  font-size: 20px;
  font-weight: bold;
  color: #e4393c;
}
.product-meta {
  font-size: 12px;
  color: #666;
}
.stock.low-stock {
  color: #e4393c;
}
.product-actions {
  padding: 0 16px 16px;
}
.add-cart-btn {
  width: 100%;
  padding: 8px 12px;
  background: #e4393c;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s ease;
}
.add-cart-btn:hover:not(:disabled) {
  background: #c03537;
}
.add-cart-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
@media (max-width: 768px) {
  .product-image {
    height: 160px;
  }
  .product-name {
    font-size: 14px;
  }
  .current-price {
    font-size: 18px;
  }
}
</style>