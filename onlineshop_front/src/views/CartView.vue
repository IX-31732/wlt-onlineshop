<template>
  <div class="cart-container">
    <!-- 调试面板 -->
    <div class="debug-panel" v-if="showDebug">
      <button @click="debugSession" class="debug-btn">调试Session</button>
      <button @click="debugCart" class="debug-btn">调试购物车</button>
      <button @click="showDebug = false" class="debug-btn">隐藏调试</button>
    </div>
    <div class="cart-header">
      <h2>我的购物车</h2>
      <div class="cart-actions" v-if="cartItems.length > 0">
        <div class="select-all">
          <input
              type="checkbox"
              id="select-all"
              v-model="allSelected"
              @change="toggleAllSelection"
          >
          <label for="select-all">全选</label>
        </div>
        <button @click="clearCart" class="clear-cart-btn">清空购物车</button>
      </div>
    </div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="cartItems.length === 0" class="empty-cart">
      <div class="empty-content">
        <div class="empty-icon">🛒🛒🛒🛒🛒🛒🛒🛒🛒</div>
        <h3>购物车是空的</h3>
        <p>快去挑选一些商品吧</p>
        <router-link to="/products" class="shopping-btn">去购物</router-link>
      </div>
    </div>
    <div v-else class="cart-content">
      <div class="cart-items">
        <div v-for="item in cartItems" :key="item.gid" class="cart-item">
          <div class="item-selection">
            <input
                type="checkbox"
                v-model="selectedItems"
                :value="item.gid"
                class="item-checkbox"
                :disabled="getItemStock(item) === 0 || item.status === 0"
            >
          </div>
          <div class="item-info">
            <div class="item-image">
              <img
                  :src="getProductImageUrl(item)"
                  @error="handleImageError"
                  class="product-img"
              >
              <div v-if="getItemStock(item) === 0 || item.status === 0" class="out-of-stock-overlay">
                {{ item.status === 0 ? '商品已下架' : '已售罄' }}
              </div>
            </div>
            <div class="item-details">
              <h4 class="item-name">{{ item.name }}</h4>
              <p class="item-description">{{ item.description || '暂无描述' }}</p>
              <p class="item-price">¥{{ item.price }}</p>
              <div class="stock-info" :class="getStockClass(item)">
                {{ getStockText(item) }}
              </div>
            </div>
          </div>
          <div class="item-controls">
            <div class="quantity-control">
              <button
                  @click="updateQuantity(item, item.quantity - 1)"
                  :disabled="item.quantity <= 1 || getItemStock(item) === 0 || item.status === 0"
                  class="quantity-btn"
              >-</button>
              <input
                  type="number"
                  v-model.number="item.quantity"
                  min="1"
                  :max="getItemStock(item)"
                  @change="validateItemQuantity(item)"
                  class="quantity-input"
                  :disabled="getItemStock(item) === 0 || item.status === 0"
              >
              <button
                  @click="updateQuantity(item, item.quantity + 1)"
                  :disabled="item.quantity >= getItemStock(item) || getItemStock(item) === 0 || item.status === 0"
                  class="quantity-btn"
              >+</button>
            </div>
            <div class="item-total">
              ¥{{ (item.price * item.quantity).toFixed(2) }}
            </div>
            <button @click="removeItem(item.gid)" class="remove-btn">删除</button>
          </div>
        </div>
      </div>
      <div class="cart-footer">
        <div class="selected-info">
          已选 {{ selectedCount }} 件商品
        </div>
        <div class="total-section">
          <div class="price-details">
            <div class="price-item">
              <span>商品总价：</span>
              <span>¥{{ selectedTotalPrice.toFixed(2) }}</span>
            </div>
            <div class="price-item">
              <span>运费：</span>
              <span>¥{{ shippingFee.toFixed(2) }}</span>
            </div>
            <div class="price-item total">
              <span>应付总额：</span>
              <span class="total-amount">¥{{ (selectedTotalPrice + shippingFee).toFixed(2) }}</span>
            </div>
          </div>
          <button
              @click="checkoutSelected"
              class="checkout-btn"
              :disabled="!canCheckoutSelected"
          >
            {{ selectedCount > 0 ? `结算选中商品(${selectedCount}件)` : '请选择商品' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { cartAPI, apiUtils, orderAPI, authAPI, goodsAPI } from '../services/api'

export default {
  name: 'CartView',
  data() {
    return {
      cartItems: [],
      selectedItems: [], //选中的商品ID数组
      loading: false,
      shippingFee: 10, //运费
      showDebug: false
    }
  },
  computed: {
    isLoggedIn() {
      return apiUtils.isLoggedIn()
    },
    //全选状态
    allSelected: {
      get() {
        const availableItems = this.cartItems.filter(item =>
            this.getItemStock(item) > 0 && item.status !== 0
        )
        return availableItems.length > 0 && this.selectedItems.length === availableItems.length
      },
      set(value) {
        if (value) {
          //只选中有库存且上架的商品
          this.selectedItems = this.cartItems
              .filter(item => this.getItemStock(item) > 0 && item.status !== 0)
              .map(item => item.gid)
        } else {
          this.selectedItems = []
        }
      }
    },
    //选中商品数量
    selectedCount() {
      return this.selectedItems.length
    },
    //选中商品的总价
    selectedTotalPrice() {
      return this.cartItems
          .filter(item => this.selectedItems.includes(item.gid))
          .reduce((total, item) => total + (item.price * item.quantity), 0)
    },
    //选中的商品
    selectedProducts() {
      return this.cartItems.filter(item => this.selectedItems.includes(item.gid))
    },
    //是否可以结算选中商品
    canCheckoutSelected() {
      return this.selectedCount > 0 &&
          this.selectedProducts.every(item =>
              this.getItemStock(item) > 0 && item.status !== 0
          )
    }
  },
  async mounted() {
    console.log('开始加载购物车');
    //使用综合登录状态检查
    const isReallyLoggedIn = await this.checkRealLoginStatus();
    if (!isReallyLoggedIn) {
      return;
    }
    await this.loadCart()
  },
  methods: {
    //统一商品库存获取逻辑
    getItemStock(item) {
      if (!item) return 0;
      if (item.stock !== undefined && item.stock !== null) {
        return item.stock;
      }
      if (item.remaining !== undefined && item.remaining !== null) {
        return item.remaining;
      }
      return 0;
    },

    //获取库存显示文本
    getStockText(item) {
      if (!item) return '库存未知';
      if (item.status === 0) {
        return '商品已下架';
      }
      const stock = this.getItemStock(item);
      if (stock === 0) {
        return '已售罄';
      }
      return `库存: ${stock}件`;
    },

    //获取库存样式类
    getStockClass(item) {
      if (!item) return '';
      if (item.status === 0) {
        return 'out-of-stock';
      }
      const stock = this.getItemStock(item);
      if (stock === 0) {
        return 'out-of-stock';
      }
      if (stock < 10) {
        return 'low-stock';
      }
      return '';
    },

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

    handleImageError(event) {
      console.warn('商品图片加载失败，使用默认图片:', event.target.src);
      event.target.src = this.getDefaultImageUrl();
    },

    //获取完整商品信息
    async getFullProductInfo(goodsId) {
      try {
        console.log('获取商品完整信息:', goodsId);
        const response = await goodsAPI.getById(goodsId);
        const responseData = apiUtils.handleResponse(response);
        const goodsInfo = responseData.goods || responseData;
        //统一库存字段处理
        if (goodsInfo) {
          if (goodsInfo.stock === undefined && goodsInfo.remaining !== undefined) {
            goodsInfo.stock = goodsInfo.remaining;
          } else if (goodsInfo.stock === undefined) {
            goodsInfo.stock = 0; // 默认值
          }
          if (goodsInfo.status === undefined) {
            goodsInfo.status = 1; // 默认上架状态
          }
        }
        return goodsInfo;
      } catch (error) {
        console.error('获取商品信息失败:', error);
        return null;
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

    async loadCart() {
      this.loading = true
      try {
        console.log('开始加载购物车数据...');
        //在每次API调用前检查登录状态
        const isReallyLoggedIn = await this.checkRealLoginStatus();
        if (!isReallyLoggedIn) {
          return;
        }
        const response = await cartAPI.getCart()
        console.log('购物车API响应:', response);
        const data = apiUtils.handleResponse(response)
        const rawCartItems = data.cart || []
        console.log('原始购物车商品:', rawCartItems);

        //为每个商品获取完整信息（包含库存）
        const enrichedItems = []
        for (const cartItem of rawCartItems) {
          try {
            //获取商品的完整信息
            const fullProductInfo = await this.getFullProductInfo(cartItem.gid)
            if (fullProductInfo) {
              enrichedItems.push({
                ...fullProductInfo, //包含完整的库存信息
                gid: cartItem.gid,
                quantity: cartItem.quantity,
                //确保使用最新的库存信息
                stock: fullProductInfo.stock !== undefined ? fullProductInfo.stock : (fullProductInfo.remaining !== undefined ? fullProductInfo.remaining : 0)
              })
            } else {
              //如果获取失败，使用购物车中的基础信息
              console.warn('无法获取商品完整信息，使用基础信息:', cartItem.gid)
              enrichedItems.push({
                ...cartItem,
                //设置默认库存，如果cartItem中没有stock字段
                stock: cartItem.stock !== undefined ? cartItem.stock : (cartItem.remaining !== undefined ? cartItem.remaining : 0),
                //设置默认状态为上架
                status: cartItem.status !== undefined ? cartItem.status : 1
              })
            }
          } catch (error) {
            console.error('处理商品信息时出错:', error)
            enrichedItems.push({
              ...cartItem,
              stock: cartItem.stock !== undefined ? cartItem.stock : (cartItem.remaining !== undefined ? cartItem.remaining : 0),
              status: cartItem.status !== undefined ? cartItem.status : 1
            })
          }
        }
        this.cartItems = enrichedItems
        console.log('购物车加载完成，商品数量:', this.cartItems.length)
        console.log('商品库存信息:', this.cartItems.map(item => ({
          gid: item.gid,
          name: item.name,
          status: item.status,
          stock: item.stock,
          remaining: item.remaining,
          finalStock: this.getItemStock(item)
        })))
        //默认选中所有有库存且上架的商品
        this.selectedItems = this.cartItems
            .filter(item => this.getItemStock(item) > 0 && item.status !== 0)
            .map(item => item.gid)
      } catch (error) {
        console.error('加载购物车失败:', error)
        //API拦截器会处理401错误，这里只需要显示消息
        if (error.message && error.message.includes('登录')) {
          this.showMessage(error.message, 'error');
        } else {
          this.showMessage('加载购物车失败: ' + error.message, 'error')
        }
      } finally {
        this.loading = false
      }
    },

    //调试Session
    async debugSession() {
      try {
        console.log('开始调试Session...');
        const result = await authAPI.debugSession();
        console.log('Session调试结果:', result);
        this.showMessage('Session调试完成，查看控制台', 'info');
      } catch (error) {
        console.error('Session调试失败:', error);
        this.showMessage('Session调试失败: ' + error.message, 'error');
      }
    },

    //调试购物车
    async debugCart() {
      console.log('开始调试购物车...');
      console.log('当前购物车商品:', this.cartItems);
      console.log('选中商品:', this.selectedItems);
      console.log('登录状态:', this.isLoggedIn);
      console.log('商品库存详情:', this.cartItems.map(item => ({
        gid: item.gid,
        name: item.name,
        status: item.status,
        stock: item.stock,
        remaining: item.remaining,
        finalStock: this.getItemStock(item)
      })));
      //检查实际登录状态
      const realStatus = await apiUtils.checkLoginStatus();
      console.log('实际登录状态:', realStatus);
      this.showMessage('购物车调试完成，查看控制台', 'info');
    },

    //切换全选
    toggleAllSelection() {
      if (this.allSelected) {
        //只选中有库存且上架的商品
        this.selectedItems = this.cartItems
            .filter(item => this.getItemStock(item) > 0 && item.status !== 0)
            .map(item => item.gid)
      } else {
        this.selectedItems = []
      }
    },

    async updateQuantity(item, newQuantity) {
      if (newQuantity < 1 || newQuantity > this.getItemStock(item)) return
      try {
        console.log('更新商品数量:', item.gid, '从', item.quantity, '到', newQuantity);
        //先检查登录状态
        const isReallyLoggedIn = await this.checkRealLoginStatus();
        if (!isReallyLoggedIn) {
          return;
        }
        const response = await cartAPI.updateCartItem(item.gid, newQuantity)
        apiUtils.handleResponse(response)
        item.quantity = newQuantity
        this.showMessage('数量更新成功', 'success')
      } catch (error) {
        console.error('更新数量失败:', error)
        this.showMessage('更新数量失败: ' + error.message, 'error')
        await this.loadCart() // 重新加载数据
      }
    },

    validateItemQuantity(item) {
      if (item.quantity < 1) {
        item.quantity = 1
      }
      if (item.quantity > this.getItemStock(item)) {
        item.quantity = this.getItemStock(item)
      }
      this.updateQuantity(item, item.quantity)
    },

    async removeItem(goodsId) {
      try {
        console.log('移除商品:', goodsId);
        //先检查登录状态
        const isReallyLoggedIn = await this.checkRealLoginStatus();
        if (!isReallyLoggedIn) {
          return;
        }
        const response = await cartAPI.removeFromCart(goodsId)
        apiUtils.handleResponse(response)
        this.cartItems = this.cartItems.filter(item => item.gid !== goodsId)
        this.selectedItems = this.selectedItems.filter(id => id !== goodsId)
        this.showMessage('商品已移除', 'success')
      } catch (error) {
        console.error('移除商品失败:', error)
        this.showMessage('移除商品失败: ' + error.message, 'error')
      }
    },

    async clearCart() {
      if (!confirm('确定要清空购物车吗？')) return
      try {
        console.log('清空购物车');
        //先检查登录状态
        const isReallyLoggedIn = await this.checkRealLoginStatus();
        if (!isReallyLoggedIn) {
          return;
        }
        const response = await cartAPI.clearCart()
        apiUtils.handleResponse(response)
        this.cartItems = []
        this.selectedItems = []
        this.showMessage('购物车已清空', 'success')
      } catch (error) {
        console.error('清空购物车失败:', error)
        this.showMessage('清空购物车失败: ' + error.message, 'error')
      }
    },

    async checkoutSelected() {
      if (!this.canCheckoutSelected) return
      try {
        console.log('开始结算选中商品:', this.selectedItems);
        //先检查登录状态
        const isReallyLoggedIn = await this.checkRealLoginStatus();
        if (!isReallyLoggedIn) {
          return;
        }
        const selectedCartItems = this.cartItems.filter(item =>
            this.selectedItems.includes(item.gid)
        )
        console.log('结算商品列表:', selectedCartItems);
        //按照后端期望的数据结构发送
        const cartItemsForBackend = selectedCartItems.map(item => ({
          gid: item.gid,        //只发送商品ID
          quantity: item.quantity //只发送数量
        }));
        console.log('发送给后端的购物车数据:', cartItemsForBackend);
        //不发送totalAmount，让后端计算
        const response = await orderAPI.createOrder(cartItemsForBackend)
        const data = apiUtils.handleResponse(response)
        console.log('订单创建成功:', data);
        this.showMessage('订单创建成功', 'success')
        //确保使用正确的订单ID进行跳转
        let orderId = data.orderId || data.oid;
        if (!orderId && data.order) {
          orderId = data.order.oid || data.order.orderId;
        }
        if (!orderId) {
          throw new Error('订单创建成功但未返回有效的订单ID');
        }
        console.log('创建订单返回的订单ID:', orderId);
        //从购物车中移除已结算的商品
        for (const item of selectedCartItems) {
          await this.removeItemFromCart(item.gid)
        }
        //使用正确的订单ID跳转到订单页面
        this.$router.push(`/order/${orderId}`)
      } catch (error) {
        console.error('结算失败:', error)
        this.showMessage('结算失败：' + error.message, 'error')
      }
    },
    //专门用于结算后移除商品的函数
    async removeItemFromCart(goodsId) {
      try {
        //先检查登录状态
        const isReallyLoggedIn = await this.checkRealLoginStatus();
        if (!isReallyLoggedIn) {
          return;
        }
        await cartAPI.removeFromCart(goodsId)
        //更新本地数据
        this.cartItems = this.cartItems.filter(item => item.gid !== goodsId)
        this.selectedItems = this.selectedItems.filter(id => id !== goodsId)
      } catch (error) {
        console.error('移除商品失败:', error)
        this.showMessage('移除商品失败: ' + error.message, 'error')
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
.cart-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  min-height: 60vh;
}
.debug-panel {
  background: #f5f5f5;
  padding: 1rem;
  margin-bottom: 1rem;
  border-radius: 8px;
  border: 1px solid #ddd;
}
.debug-btn {
  padding: 0.5rem 1rem;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 0.5rem;
  font-size: 0.9rem;
}
.debug-btn:hover {
  background-color: #0056b3;
}
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e0e0e0;
}
.cart-header h2 {
  color: #333;
  margin: 0;
  font-size: 1.8rem;
}
.cart-actions {
  display: flex;
  align-items: center;
  gap: 2rem;
}
.select-all {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
}
.select-all input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}
.clear-cart-btn {
  padding: 0.5rem 1rem;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}
.clear-cart-btn:hover {
  background-color: #5a6268;
}
.loading {
  text-align: center;
  padding: 4rem 2rem;
  color: #666;
  font-size: 1.1rem;
}
.empty-cart {
  text-align: center;
  padding: 4rem 2rem;
}
.empty-content {
  max-width: 400px;
  margin: 0 auto;
}
.empty-icon {
  font-size: 5rem;
  margin-bottom: 1.5rem;
  opacity: 0.5;
}
.empty-content h3 {
  margin-bottom: 1rem;
  color: #666;
  font-size: 1.5rem;
}
.empty-content p {
  color: #999;
  margin-bottom: 2rem;
  font-size: 1.1rem;
}
.shopping-btn {
  display: inline-block;
  padding: 1rem 2rem;
  background-color: #409eff;
  color: white;
  text-decoration: none;
  border-radius: 6px;
  transition: background-color 0.3s;
  font-weight: 500;
}
.shopping-btn:hover {
  background-color: #66b1ff;
}
.cart-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}
.cart-items {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.cart-item {
  display: flex;
  align-items: center;
  padding: 1.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  gap: 1rem;
}
.item-selection {
  flex-shrink: 0;
}
.item-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
}
.item-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
}
.item-image {
  position: relative;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
}
.product-img {
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
  object-position: center;
}
.out-of-stock-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-weight: bold;
}
.item-details {
  flex: 1;
}
.item-name {
  margin: 0 0 0.5rem 0;
  color: #333;
  font-size: 1.1rem;
  font-weight: 500;
}
.item-description {
  margin: 0 0 0.5rem 0;
  font-size: 0.9rem;
  color: #666;
  line-height: 1.4;
}
.item-price {
  margin: 0 0 0.5rem 0;
  font-size: 1.3rem;
  font-weight: bold;
  color: #e74c3c;
}
.stock-info {
  font-size: 0.9rem;
  color: #27ae60;
}
.stock-info.low-stock {
  color: #f39c12;
}
.stock-info.out-of-stock {
  color: #e74c3c;
  font-weight: 500;
}
.item-controls {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}
.quantity-control {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.quantity-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s;
}
.quantity-btn:hover:not(:disabled) {
  background-color: #f5f5f5;
}
.quantity-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.quantity-input {
  width: 60px;
  height: 32px;
  text-align: center;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}
.quantity-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}
.item-total {
  font-size: 1.3rem;
  font-weight: bold;
  color: #333;
  min-width: 100px;
  text-align: center;
}
.remove-btn {
  padding: 0.5rem 1rem;
  background-color: #e74c3c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}
.remove-btn:hover {
  background-color: #c0392b;
}
.cart-footer {
  position: sticky;
  bottom: 0;
  background: white;
  border-top: 2px solid #e0e0e0;
  padding: 1.5rem;
  margin-top: 2rem;
}
.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  border-top: 2px solid #e0e0e0;
  padding: 1.5rem;
  margin-top: 2rem;
  border-radius: 8px;
}
.selected-info {
  font-size: 1.1rem;
  font-weight: 500;
  color: #333;
}
.total-section {
  display: flex;
  align-items: center;
  gap: 2rem;
}
.price-details {
  text-align: right;
}
.price-item {
  margin-bottom: 0.5rem;
  font-size: 1rem;
  color: #666;
}
.price-item.total {
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px solid #e0e0e0;
  font-size: 1.2rem;
  font-weight: bold;
  color: #333;
}
.total-amount {
  color: #e74c3c;
  font-size: 1.4rem;
}
.checkout-btn {
  padding: 1rem 2rem;
  background-color: #e74c3c;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 1.1rem;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.3s;
  min-width: 200px;
}
.checkout-btn:hover:not(:disabled) {
  background-color: #c0392b;
}
.checkout-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
@media (max-width: 768px) {
  .cart-container {
    padding: 1rem;
  }
  .cart-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
  .cart-actions {
    justify-content: space-between;
  }
  .cart-item {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }
  .item-info {
    flex-direction: column;
    text-align: center;
  }
  .item-controls {
    justify-content: space-between;
  }
  .cart-footer {
    flex-direction: column;
    gap: 1rem;
  }
  .total-section {
    flex-direction: column;
    width: 100%;
  }
}
</style>