<template>
  <div class="sales-report-container">
    <div class="report-header">
      <h1>销售报表</h1>
      <p>分析您的商品销售情况和业绩统计</p>
    </div>
    <div class="sales-overview">
      <div class="stat-card">
        <div class="stat-icon">📈</div>
        <div class="stat-content">
          <h3>总销量</h3>
          <p class="stat-number">{{ salesStats.totalSales || 0 }}</p>
          <p class="stat-label">件商品</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">💰</div>
        <div class="stat-content">
          <h3>总销售额</h3>
          <p class="stat-number">¥{{ formatAmount(salesStats.totalSalesValue || 0) }}</p>
          <p class="stat-label">开店以来</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📦</div>
        <div class="stat-content">
          <h3>总商品数</h3>
          <p class="stat-number">{{ salesStats.totalGoods || 0 }}</p>
          <p class="stat-label">所有商品</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">🛒</div>
        <div class="stat-content">
          <h3>在售商品</h3>
          <p class="stat-number">{{ salesStats.activeGoods || 0 }}</p>
          <p class="stat-label">已上架</p>
        </div>
      </div>
    </div>
    <div class="report-content">
      <div class="ranking-section">
        <div class="ranking-tabs">
          <button
              :class="['tab-button', activeTab === 'quantity' ? 'active' : '']"
              @click="activeTab = 'quantity'"
          >
            销量排名
          </button>
          <button
              :class="['tab-button', activeTab === 'value' ? 'active' : '']"
              @click="activeTab = 'value'"
          >
            金额排名
          </button>
        </div>

        <div class="ranking-list">
          <div v-if="loading" class="loading">加载中...</div>
          <div v-else class="ranking-items">
            <div
                v-for="(goods, index) in currentRanking"
                :key="goods.gid"
                class="ranking-item"
                :class="{ 'inactive-goods': goods.status !== 1 }"
            >
              <div class="rank-badge" :class="getRankClass(index + 1)">
                {{ index + 1 }}
              </div>

              <div class="goods-info">
                <h4>{{ goods.name }}</h4>
                <p class="goods-description">{{ goods.description }}</p>
                <span v-if="goods.status !== 1" class="status-badge">已下架</span>
              </div>
              <div class="sales-info">
                <span v-if="activeTab === 'quantity'" class="sales-number">
                  销量: {{ goods.sales || 0 }}
                </span>
                <span v-else class="sales-number">
                  销售额: ¥{{ formatAmount((goods.sales || 0) * (goods.price || 0)) }}
                </span>
                <span class="goods-price">¥{{ formatAmount(goods.price || 0) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { goodsAPI } from '../services/api';

export default {
  name: 'SalesReportView',
  data() {
    return {
      loading: false,
      activeTab: 'quantity',
      salesRanking: [],
      salesValueRanking: [],
      salesStats: {
        totalSales: 0,
        totalSalesValue: 0,
        totalGoods: 0,
        activeGoods: 0
      }
    }
  },
  computed: {
    currentRanking() {
      return this.activeTab === 'quantity' ? this.salesRanking : this.salesValueRanking;
    }
  },
  mounted() {
    this.loadSalesData();
  },
  methods: {
    async loadSalesData() {
      this.loading = true;
      try {
        //销量排名
        const salesResponse = await goodsAPI.getSalesRanking();
        if (salesResponse.success) {
          this.salesRanking = salesResponse.ranking || [];
          //直接从销量排名数据统计在售商品
          const activeGoodsCount = this.salesRanking.filter(goods =>
              goods.status === 1 || goods.status === undefined
          ).length;
          //加载其他数据
          const valueResponse = await goodsAPI.getSalesValueRanking();
          if (valueResponse.success) {
            this.salesValueRanking = valueResponse.ranking || [];
          }
          const statsResponse = await goodsAPI.getMyGoodsStats();
          if (statsResponse.success) {
            this.salesStats = {
              totalSales: statsResponse.stats.totalSales || 0,
              totalSalesValue: statsResponse.stats.totalSalesValue || 0,
              totalGoods: statsResponse.stats.goodsCount || 0,
              activeGoods: activeGoodsCount
            };
          }
        }
      } catch (error) {
        console.error('加载销售数据失败:', error);
      } finally {
        this.loading = false;
      }
    },

    getRankClass(rank) {
      if (rank === 1) return 'rank-gold';
      if (rank === 2) return 'rank-silver';
      if (rank === 3) return 'rank-bronze';
      return 'rank-normal';
    },
    handleImageError(event) {
      event.target.src = '/images/default-product.png';
    },
    formatAmount(amount) {
      return Number(amount).toFixed(2);
    }
  }
}
</script>

<style scoped>
.sales-report-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.report-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 30px;
  background: white;
  color: black;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.report-header h1 {
  margin: 0 0 10px 0;
  font-size: 2.5rem;
}
.sales-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}
.stat-card {
  background: white;
  border-radius: 10px;
  padding: 25px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.stat-icon {
  font-size: 3rem;
  margin-right: 20px;
}
.stat-number {
  font-size: 2rem;
  font-weight: bold;
  margin: 5px 0;
  color: #333;
}
.stat-label {
  color: #666;
  margin: 0;
  font-size: 0.9rem;
}
.report-content {
  display: grid;
  grid-template-columns: 1fr;
  gap: 30px;
}
.ranking-section {
  background: white;
  border-radius: 10px;
  padding: 25px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.ranking-tabs {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
}
.tab-button {
  padding: 10px 20px;
  border: none;
  background: none;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.3s;
}
.tab-button.active {
  border-bottom-color: #ff6b6b;
  color: #ff6b6b;
  font-weight: bold;
}
.ranking-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 10px;
  transition: transform 0.2s;
}
.ranking-item:hover {
  transform: translateX(5px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.ranking-item.inactive-goods {
  opacity: 0.6;
  background-color: #f8f9fa;
}
.rank-badge {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  margin-right: 15px;
}
.rank-gold { background: linear-gradient(135deg, #ffd700, #ffa500); }
.rank-silver { background: linear-gradient(135deg, #c0c0c0, #a0a0a0); }
.rank-bronze { background: linear-gradient(135deg, #cd7f32, #a56a2b); }
.rank-normal { background: #667eea; }
.goods-image {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 5px;
  margin-right: 15px;
}
.goods-info {
  flex: 1;
  position: relative;
}
.goods-info h4 {
  margin: 0 0 5px 0;
  color: #333;
}
.goods-description {
  color: #666;
  margin: 0;
  font-size: 0.9rem;
}
.status-badge {
  position: absolute;
  top: 0;
  right: 0;
  background: #ff6b6b;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.8rem;
}
.sales-info {
  text-align: right;
}
.sales-number {
  display: block;
  font-weight: bold;
  color: #ff6b6b;
  margin-bottom: 5px;
}
.goods-price {
  color: #666;
  font-size: 0.9rem;
}
.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}
@media (max-width: 968px) {
  .sales-overview {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  }
}
@media (max-width: 768px) {
  .sales-overview {
    grid-template-columns: 1fr;
  }
  .ranking-item {
    flex-direction: column;
    text-align: center;
  }
  .goods-image {
    margin: 10px 0;
  }
  .sales-info {
    text-align: center;
    margin-top: 10px;
  }
}
</style>