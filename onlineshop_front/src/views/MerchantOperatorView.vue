<template>
  <div class="merchant-operator-container">
    <div class="merchant-header">
      <h1>商家管理中心</h1>
      <p>欢迎回来，{{ currentUser?.nickname }}！</p>
    </div>
    <div class="operator-actions">
      <div class="action-card" @click="goToGoodsManager">
        <div class="action-icon">📦</div>
        <h3>商品管理</h3>
        <p>管理您的商品信息</p>
      </div>
      <div class="action-card" @click="goToOrderManager">
        <div class="action-icon">🚚</div>
        <h3>发货管理</h3>
        <p>处理订单和发货</p>
      </div>
      <!-- 新增：销售报表按钮 -->
      <div class="action-card" @click="goToSalesReport">
        <div class="action-icon">📊</div>
        <h3>销售报表</h3>
        <p>分析销售数据和业绩统计</p>
      </div>
    </div>
  </div>
</template>

<script>
import { apiUtils } from '../services/api';

export default {
  name: 'MerchantOperatorView',
  data() {
    return {
      currentUser: null
    }
  },
  mounted() {
    this.currentUser = apiUtils.getCurrentUser();
  },
  methods: {
    goToGoodsManager() {
      this.$router.push('/merchant/goods');
    },
    goToOrderManager() {
      this.$router.push('/merchant/orders');
    },
    goToSalesReport() {
      this.$router.push('/merchant/sales-report');
    }
  }
}
</script>

<style scoped>
.merchant-operator-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.merchant-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 10px;
}
.merchant-header h1 {
  margin: 0 0 10px 0;
  font-size: 2.5rem;
}
.operator-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 30px;
  padding: 20px;
}
.action-card {
  background: white;
  border-radius: 15px;
  padding: 40px 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  border: 2px solid transparent;
}
.action-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  border-color: #409eff;
}
.action-icon {
  font-size: 4rem;
  margin-bottom: 20px;
}
.action-card h3 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 1.5rem;
}
.action-card p {
  margin: 0;
  color: #666;
  font-size: 1rem;
}
@media (max-width: 768px) {
  .operator-actions {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 10px;
  }
  .action-card {
    padding: 30px 20px;
  }
}
</style>