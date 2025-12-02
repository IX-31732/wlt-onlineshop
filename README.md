# 在线商城项目 README

## 姓名与学号
**姓名**: 王露婷  
**学号**: 202330452982  

## 项目概述
本项目是一个电子商务网站的设计与实现，涵盖前后端开发及其在线部署。系统支持用户注册、登录、商品浏览、购物车管理、订单处理及商家数据分析等功能。

## 部署网址
http://8.148.152.84

## 文件说明

### 1. **后端代码实现**

- **Config层**:
  - `SecurityConfig.java` - 配置Spring Security的认证和授权策略，定义密码加密、会话管理等安全机制。
  - `WebConfig.java` - 配置静态资源映射和文件上传路径，确保前端能够访问上传的图片和资源。

- **Controller层**:  
  - `GoodsController.java` - 处理商品相关API，如商品浏览、搜索等。
  - `OrderController.java` - 处理订单相关操作，如创建订单、查询订单等。
  - `CartController.java` - 管理购物车操作，如添加商品、查看购物车等。
  - `LoginController.java` - 处理用户登录、注册、登出和会话管理。
  - `FileUploadController.java` - 处理文件上传功能，支持用户头像和商品图片上传。
  - `GlobalExceptionHandler.java` - 统一处理系统中的异常，保证API响应的规范性。

- **Service层**:  
  - `GoodsService.java` - 处理商品相关的业务逻辑，如商品的增加、更新、查询等。
  - `OrderService.java` - 管理订单生命周期，包括订单的创建、修改、查询等。
  - `UserService.java` - 管理用户的基本信息，支持用户注册、登录、信息更新等。

- **Repository层**:  
  - `GoodsRepository.java` - 数据访问层，管理商品数据的持久化。
  - `OrderRepository.java` - 管理订单信息的数据库操作。
  - `UserRepository.java` - 管理用户数据的数据库操作。

- **Entity层**:
  - `Goods.java` - 商品实体类，定义了商品的基本信息，如商品名、价格、库存等，及其与数据库表的映射关系。
  - `User.java` - 用户实体类，包含用户信息，如用户名、邮箱、密码等，支持顾客和商家的角色区分。
  - `UserOrder.java` - 订单实体类，关联用户和订单商品，存储订单状态和时间。
  - `OrderGoods.java` - 订单商品实体类，关联商品和订单，记录商品的数量和单价。 

### 2. **前端代码实现**

- **页面视图**:
  - `HomeView.vue` - 网站首页，展示可购买的商品列表，支持商品筛选与搜索。
  - `CartView.vue` - 购物车页面，展示已添加商品并支持数量修改、商品删除等操作。
  - `GoodsManagerView.vue` - 商家商品管理页面，商家可以进行商品增删改查操作。
  - `LoginView.vue` - 用户登录页面，支持用户名和邮箱登录。
  - `MerchantOperatorView.vue` - 商家操作页面，商家可以访问管理和统计功能。
  - `MerchantView.vue` - 商家首页，展示商家的店铺信息。
  - `OrderManagerView.vue` - 商家订单管理页面，商家查看和管理订单。
  - `OrderView.vue` - 订单详情页面，展示订单的详细信息和状态。
  - `ProductDetail.vue` - 商品详情页面，展示商品的详细描述和其他信息。
  - `ProductsView.vue` - 商品列表页面，展示所有商品，并支持筛选和排序。
  - `ProfileView.vue` - 用户个人资料页面，用户可以查看和编辑个人信息。
  - `RegisterView.vue` - 用户注册页面，提供注册功能，包含用户名、邮箱和密码输入框。
  - `SalesReportView.vue` - 销售报表页面，商家可以查看销售数据和商品销售排名。

- **组件**:
  - `NavBar.vue` - 导航栏组件，提供全局路由跳转。
  - `ProductCard.vue` - 商品展示卡片，展示商品信息并支持加入购物车功能。

- **服务层**:
  - `api.js` - 封装所有与后端API的交互，使用Axios进行HTTP请求。

- **路由配置**:
  - `index.js` - 配置应用程序的路由，定义页面导航路径。

### 3. **技术栈**
- **后端**:  
  - 框架: Spring Boot, Spring Data JPA, Spring Security  
  - 数据库: MySQL  
  - 会话管理: HttpSession  
  - 邮件服务: Spring Mail

- **前端**:  
  - 框架: Vue 3, Vue Router  
  - 状态管理: Vuex  
  - 构建工具: Vite  
  - HTTP客户端: Axios  

### 4. **部署与上线**
- 本项目部署在阿里云的云服务器上，通过配置Java、MySQL、Nginx等环境实现了后端和前端的上线。详细的服务器配置与部署流程见项目文档。

## 如何部署

### 后端
1. 安装并配置Java和MySQL环境。
2. 配置`application.yml`，设置数据库连接和邮件服务。
3. 打包后端项目，部署到云服务器上。

### 前端
1. 使用`npm run build`打包前端项目。
2. 配置前端与后端API接口的访问路径。
3. 上传并部署前端代码到服务器。

### 测试
可以使用提供的测试账号登录并测试各项功能，如商品浏览、订单创建、购物车管理等。

## 功能总结
本项目实现了一个完整的电子商务系统，包括商品管理、订单管理、用户认证、商家数据分析等功能，为用户和商家提供了便捷的操作界面和完善的后台数据支持。
