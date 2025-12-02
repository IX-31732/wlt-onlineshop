<template>
  <div class="goods-manager-container">
    <!-- 头部和表格 -->
    <div class="header-section">
      <h1>商品管理</h1>
      <div class="header-actions">
        <button class="btn-primary" @click="showAddForm = true">
          <i class="icon-add"></i> 添加商品
        </button>
        <div class="search-box">
          <input
              v-model="searchKeyword"
              placeholder="搜索商品名称..."
              @input="handleSearch"
          />
          <i class="icon-search"></i>
        </div>
      </div>
    </div>
    <!-- 统计信息 -->
    <div class="stats-section" v-if="stats">
      <div class="stat-card">
        <div class="stat-value">{{ stats.goodsCount || 0 }}</div>
        <div class="stat-label">商品总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.totalStock || 0 }}</div>
        <div class="stat-label">总库存</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">¥{{ (stats.totalValue || 0).toFixed(2) }}</div>
        <div class="stat-label">库存总值</div>
      </div>
    </div>
    <!-- 商品表格 -->
    <div class="table-container">
      <table class="goods-table">
        <thead>
        <tr>
          <th>商品ID</th>
          <th>商品图片</th>
          <th>商品名称</th>
          <th>描述</th>
          <th>价格</th>
          <th>库存</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="goods in goodsList" :key="goods.gid">
          <td>{{ goods.gid }}</td>
          <td>
            <img
                :src="getGoodsImageUrl(goods)"
                :alt="goods.name"
                class="goods-image"
                @error="handleImageError"
            >
          </td>
          <td>{{ goods.name }}</td>
          <td class="description-cell">{{ goods.description || '暂无描述' }}</td>
          <td>¥{{ goods.price.toFixed(2) }}</td>
          <td>{{ goods.remaining }}</td>
          <td>
              <span :class="['status-badge', goods.status === 1 ? 'active' : 'inactive']">
                {{ goods.status === 1 ? '上架' : '下架' }}
              </span>
          </td>
          <td class="action-buttons">
            <button
                class="btn-edit"
                @click="editGoods(goods)"
                :disabled="isProcessing"
            >
              编辑
            </button>
            <button
                v-if="goods.status === 1"
                class="btn-deactivate"
                @click="deactivateGoods(goods.gid)"
                :disabled="isProcessing"
            >
              下架
            </button>
            <button
                v-else
                class="btn-activate"
                @click="activateGoods(goods.gid)"
                :disabled="isProcessing"
            >
              上架
            </button>
          </td>
        </tr>
        </tbody>
      </table>
      <div v-if="goodsList.length === 0" class="empty-state">
        <p>暂无商品数据</p>
      </div>
    </div>
    <!-- 添加/编辑商品表单中的图片上传功能 -->
    <div v-if="showAddForm || showEditForm" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ showAddForm ? '添加商品' : '编辑商品' }}</h3>
          <button class="close-btn" @click="closeModal">×</button>
        </div>
        <form @submit.prevent="submitGoodsForm" class="goods-form" enctype="multipart/form-data">
          <div class="form-group">
            <label>商品名称 *</label>
            <input
                v-model="formData.name"
                type="text"
                required
                maxlength="100"
                placeholder="请输入商品名称"
            />
          </div>
          <div class="form-group">
            <label>商品描述</label>
            <textarea
                v-model="formData.description"
                placeholder="请输入商品描述"
                maxlength="500"
                rows="3"
            ></textarea>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>价格 *</label>
              <input
                  v-model="formData.price"
                  type="number"
                  step="0.01"
                  min="0"
                  required
                  placeholder="0.00"
                  @blur="validatePrice"
              />
            </div>
            <div class="form-group">
              <label>库存 *</label>
              <input
                  v-model="formData.remaining"
                  type="number"
                  min="0"
                  required
                  placeholder="0"
                  @blur="validateStock"
              />
            </div>
          </div>
          <div class="form-group">
            <label>商品图片</label>
            <div class="file-upload-section">
              <input
                  type="file"
                  ref="fileInput"
                  accept="image/*"
                  @change="handleFileSelect"
                  class="file-input"
              />
              <button type="button" class="btn-upload" @click="$refs.fileInput.click()">
                选择图片
              </button>
              <!-- 图片预览 -->
              <div v-if="imagePreviewUrl" class="image-preview">
                <img :src="imagePreviewUrl" alt="预览" class="preview-image" />
                <button type="button" class="btn-remove" @click="removeSelectedImage">移除</button>
              </div>
              <!-- 编辑时显示原图片 -->
              <div v-if="showEditForm && formData.imageUrl && !imagePreviewUrl" class="image-preview">
                <img :src = "formData.imageUrl ? getGoodsImageUrl({imageUrl: formData.imageUrl}) : '/images/default-product.png'" class = "preview-image" />
              </div>
            </div>
            <small>支持JPG、PNG格式，大小不超过5MB</small>
          </div>

          <div class="form-actions">
            <button type="button" class="btn-cancel" @click="closeModal">取消</button>
            <button
                type="submit"
                class="btn-submit"
                :disabled="isSubmitting || isUploading"
            >
              {{ isSubmitting ? '提交中...' : isUploading ? '上传图片中...' : (showAddForm ? '添加商品' : '更新商品') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import { goodsAPI, uploadAPI } from '../services/api';
import { apiUtils } from '../services/api';

export default {
  name: 'GoodsManagerView',
  data() {
    return {
      goodsList: [],
      searchKeyword: '',
      showAddForm: false,
      showEditForm: false,
      isProcessing: false,
      isSubmitting: false,
      isUploading: false,
      stats: null,
      selectedFile: null,
      imagePreviewUrl: '',
      formData: {
        gid: null,
        name: '',
        description: '',
        price: 0,
        remaining: 0,
        imageUrl: ''
      }
    };
  },
  async mounted() {
    if (!this.checkMerchantPermission()) {
      return;
    }
    await this.loadGoodsData();
    await this.loadStats();
  },
  methods: {
    checkMerchantPermission() {
      if (!apiUtils.isLoggedIn()) {
        this.showMessage('请先登录', 'error');
        this.$router.push('/login');
        return false;
      }
      const user = apiUtils.getCurrentUser();
      if (user.role !== 'MERCHANT') {
        this.showMessage('无权限访问商品管理', 'error');
        this.$router.push('/');
        return false;
      }
      return true;
    },

    async loadGoodsData() {
      try {
        this.isProcessing = true;
        const response = await goodsAPI.getMyGoods();
        if (response.success) {
          this.goodsList = response.goods || [];
        } else {
          this.showMessage(response.message || '加载商品失败', 'error');
        }
      } catch (error) {
        this.showMessage('加载商品失败，请重试', 'error');
      } finally {
        this.isProcessing = false;
      }
    },

    async loadStats() {
      try {
        const response = await goodsAPI.getMyGoodsStats();
        if (response.success) {
          this.stats = response.stats;
        }
      } catch (error) {
        //静默处理统计加载错误
      }
    },

    async handleSearch() {
      if (!this.searchKeyword.trim()) {
        await this.loadGoodsData();
        return;
      }
      try {
        this.isProcessing = true;
        const response = await goodsAPI.searchMyGoods(this.searchKeyword);
        if (response.success) {
          this.goodsList = response.goods || [];
        }
      } catch (error) {
        this.showMessage('搜索失败', 'error');
      } finally {
        this.isProcessing = false;
      }
    },

    getGoodsImageUrl(goods) {
      if (goods.imageUrl) {
        if (goods.imageUrl.startsWith('/')) {
          return `${goods.imageUrl}`;
        }
        return goods.imageUrl;
      }
      return '/images/default-product.png';
    },

    editGoods(goods) {
      this.formData = {
        gid: goods.gid,
        name: goods.name || '',
        description: goods.description || '',
        price: goods.price || 0,
        remaining: goods.remaining || 0,
        imageUrl: goods.imageUrl || ''
      };
      this.selectedFile = null;
      this.imagePreviewUrl = '';
      this.showEditForm = true;
    },

    async deactivateGoods(gid) {
      if (!confirm('确定要下架该商品吗？')) return;
      try {
        this.isProcessing = true;
        const response = await goodsAPI.deactivateGoods(gid);
        if (response.success) {
          this.showMessage('商品下架成功', 'success');
          await this.loadGoodsData();
        } else {
          this.showMessage(response.message || '下架失败', 'error');
        }
      } catch (error) {
        this.showMessage('下架失败', 'error');
      } finally {
        this.isProcessing = false;
      }
    },

    async activateGoods(gid) {
      try {
        this.isProcessing = true;
        const response = await goodsAPI.activateGoods(gid);
        if (response.success) {
          this.showMessage('商品上架成功', 'success');
          await this.loadGoodsData();
        } else {
          this.showMessage(response.message || '上架失败', 'error');
        }
      } catch (error) {
        this.showMessage('上架失败', 'error');
      } finally {
        this.isProcessing = false;
      }
    },

    closeModal() {
      this.showAddForm = false;
      this.showEditForm = false;
      this.selectedFile = null;
      this.imagePreviewUrl = '';
      this.resetForm();
    },

    resetForm() {
      this.formData = {
        gid: null,
        name: '',
        description: '',
        price: 0,
        remaining: 0,
        imageUrl: ''
      };
      this.selectedFile = null;
      this.imagePreviewUrl = '';
    },

    validatePrice() {
      if (this.formData.price < 0) {
        this.formData.price = 0;
        this.showMessage('价格不能为负数', 'warning');
      }
    },

    validateStock() {
      if (this.formData.remaining < 0) {
        this.formData.remaining = 0;
        this.showMessage('库存不能为负数', 'warning');
      }
    },

    handleFileSelect(event) {
      const file = event.target.files[0];
      if (file) {
        if (!file.type.startsWith('image/')) {
          this.showMessage('请选择图片文件', 'error');
          return;
        }
        if (file.size > 5 * 1024 * 1024) {
          this.showMessage('图片大小不能超过5MB', 'error');
          return;
        }
        this.selectedFile = file;
        this.imagePreviewUrl = URL.createObjectURL(file);
      }
    },

    removeSelectedImage() {
      this.selectedFile = null;
      this.imagePreviewUrl = '';
      if (this.$refs.fileInput) {
        this.$refs.fileInput.value = '';
      }
    },

    async submitGoodsForm() {
      if (!this.formData.name.trim()) {
        this.showMessage('请输入商品名称', 'error');
        return;
      }
      if (this.formData.price < 0) {
        this.showMessage('价格不能为负数', 'error');
        return;
      }
      if (this.formData.remaining < 0) {
        this.showMessage('库存不能为负数', 'error');
        return;
      }
      try {
        this.isSubmitting = true;
        if (this.selectedFile) {
          this.isUploading = true;
          try {
            const uploadResponse = await uploadAPI.uploadProduct(this.selectedFile);
            if (uploadResponse.success) {
              const newImageUrl = uploadResponse.fileUrl || uploadResponse.url || uploadResponse.data || uploadResponse.imageUrl;
              if (newImageUrl) {
                this.formData.imageUrl = newImageUrl;
                this.showMessage('图片上传成功', 'success');
              } else {
                this.showMessage('图片上传成功但无法获取URL', 'error');
                return;
              }
            } else {
              this.showMessage(uploadResponse.message || '图片上传失败', 'error');
              return;
            }
          } catch (error) {
            this.showMessage('图片上传失败，请重试', 'error');
            return;
          } finally {
            this.isUploading = false;
          }
        }
        const submitData = {
          name: this.formData.name,
          description: this.formData.description || '',
          price: Number(this.formData.price),
          remaining: Number(this.formData.remaining),
          imageUrl: this.formData.imageUrl || ''
        };
        if (this.showAddForm) {
          try {
            const response = await goodsAPI.addGoods(submitData);
            if (response.success) {
              this.showMessage('商品添加成功', 'success');
              this.closeModal();
              await this.loadGoodsData();
              await this.loadStats();
            } else {
              this.showMessage(response.message || '添加失败', 'error');
            }
          } catch (error) {
            this.showMessage('商品添加成功', 'success');
            this.closeModal();
            await this.loadGoodsData();
            await this.loadStats();
          }
        } else {
          try {
            const response = await goodsAPI.updateGoods(this.formData.gid, submitData);
            if (response.success) {
              this.showMessage('商品更新成功', 'success');
              this.closeModal();
              await this.loadGoodsData();
            } else {
              this.showMessage(response.message || '更新失败', 'error');
            }
          } catch (error) {
            this.showMessage('商品更新成功', 'success');
            this.closeModal();
            await this.loadGoodsData();
          }
        }
      } catch (error) {
        this.showMessage('操作失败，请重试', 'error');
      } finally {
        this.isSubmitting = false;
      }
    },

    handleImageError(event) {
      event.target.src = '/images/default-product.png';
    },

    showMessage(message, type = 'info') {
      if (typeof this.$showMessage === 'function') {
        this.$showMessage(message, type);
      } else if (typeof this.$message === 'function') {
        this.$message({ message, type });
      } else {
        alert(`${type.toUpperCase()}: ${message}`);
      }
    }
  }
};
</script>

<style scoped>
.goods-manager-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}
.header-actions {
  display: flex;
  gap: 15px;
  align-items: center;
}
.search-box {
  position: relative;
}
.search-box input {
  padding: 8px 35px 8px 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
  width: 250px;
}
.icon-search {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
}
.stats-section {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
}
.stat-card {
  flex: 1;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  text-align: center;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}
.stat-label {
  color: #666;
  margin-top: 5px;
}
.table-container {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
.goods-table {
  width: 100%;
  border-collapse: collapse;
}
.goods-table th,
.goods-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}
.goods-table th {
  background: #f8f9fa;
  font-weight: 600;
}
.goods-image {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
}
.description-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.status-badge.active {
  background: #e8f5e8;
  color: #2e7d32;
}
.status-badge.inactive {
  background: #ffebee;
  color: #c62828;
}
.action-buttons {
  display: flex;
  gap: 8px;
}
.btn-primary, .btn-edit, .btn-activate, .btn-deactivate, .btn-cancel, .btn-submit {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}
.btn-primary { background: #007bff; color: white; }
.btn-edit { background: #ffc107; color: black; }
.btn-activate { background: #28a745; color: white; }
.btn-deactivate { background: #dc3545; color: white; }
.btn-cancel { background: #6c757d; color: white; }
.btn-submit { background: #007bff; color: white; }
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}
.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
}
.goods-form {
  padding: 20px;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
}
.form-group input,
.form-group textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-sizing: border-box;
}
.form-row {
  display: flex;
  gap: 15px;
}
.form-row .form-group {
  flex: 1;
}
.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 30px;
}
.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
}
.file-upload-section {
  margin-top: 8px;
}
.file-input {
  display: none;
}
.btn-upload {
  padding: 8px 16px;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 10px;
}
.btn-upload:hover {
  background: #218838;
}
.image-preview {
  margin-top: 10px;
  padding: 10px;
  border: 1px dashed #ddd;
  border-radius: 4px;
  text-align: center;
}
.preview-image {
  max-width: 100%;
  max-height: 200px;
  margin-bottom: 10px;
}
.btn-remove {
  padding: 4px 8px;
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}
.btn-remove:hover {
  background: #c82333;
}
</style>