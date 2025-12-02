package com.onlineshop.onlineshop.service;

import com.onlineshop.onlineshop.entity.Goods;
import com.onlineshop.onlineshop.entity.User;
import com.onlineshop.onlineshop.repository.GoodsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GoodsService {
    @Autowired
    private GoodsRepository goodsRepository;

    //添加商品
    public Goods addGoods(Goods goods, User merchant) {
        //验证价格和库存
        if (goods.getPrice() < 0) {
            throw new RuntimeException("商品价格不合法");
        }
        if (goods.getRemaining() == null || goods.getRemaining() < 0) {
            throw new RuntimeException("商品库存不合法");
        }
        //设置商家和默认状态
        goods.setMerchant(merchant);
        goods.setStatus(1); //默认上架
        goods.setSales(0); //默认销量为0
        return goodsRepository.save(goods);
    }

    //更新商品信息
    public Goods updateGoods(Goods goods, User merchant) {
        Goods existingGoods = getGoodsById(goods.getGid());
        //验证商家权限
        if (!existingGoods.getMerchant().getUid().equals(merchant.getUid())) {
            throw new RuntimeException("无权修改此商品");
        }
        //验证价格和库存
        if (goods.getPrice() != null && goods.getPrice() < 0) {
            throw new RuntimeException("商品价格不合法");
        }
        if (goods.getRemaining() != null && goods.getRemaining() < 0) {
            throw new RuntimeException("商品库存不合法");
        }
        //更新字段
        if (goods.getName() != null) existingGoods.setName(goods.getName());
        if (goods.getDescription() != null) existingGoods.setDescription(goods.getDescription());
        if (goods.getPrice() != null) existingGoods.setPrice(goods.getPrice());
        if (goods.getRemaining() != null) existingGoods.setRemaining(goods.getRemaining());
        if (goods.getImageUrl() != null) existingGoods.setImageUrl(goods.getImageUrl());
        return goodsRepository.save(existingGoods);
    }

    //删除商品
    public void deleteGoods(Long gid, User merchant) {
        Goods goods = getGoodsById(gid);
        //验证商家权限
        if (!goods.getMerchant().getUid().equals(merchant.getUid())) {
            throw new RuntimeException("无权删除此商品");
        }
        goodsRepository.deleteById(gid);
    }

    //下架商品
    public Goods deactivateGoods(Long gid, User merchant) {
        Goods goods = getGoodsById(gid);
        //验证商家权限
        if (!goods.getMerchant().getUid().equals(merchant.getUid())) {
            throw new RuntimeException("无权操作此商品");
        }
        goods.setStatus(0); //下架
        return goodsRepository.save(goods);
    }
    //上架商品
    public Goods activateGoods(Long gid, User merchant) {
        Goods goods = getGoodsById(gid);
        //验证商家权限
        if (!goods.getMerchant().getUid().equals(merchant.getUid())) {
            throw new RuntimeException("无权操作此商品");
        }
        goods.setStatus(1); //上架
        return goodsRepository.save(goods);
    }

    //库存销量管理
    //更新商品库存（商家权限验证）
    public void updateStock(Long gid, Integer newStock, User merchant) {
        Goods goods = getGoodsById(gid);
        //验证商家权限
        if (!goods.getMerchant().getUid().equals(merchant.getUid())) {
            throw new RuntimeException("无权修改此商品库存");
        }
        if (newStock < 0) {
            throw new RuntimeException("库存不能为负数");
        }
        goods.setRemaining(newStock);
        goodsRepository.save(goods);
    }
    //减少商品库存时同时增加销量
    public void decreaseStock(Long goodsId, Integer quantity) {
        Goods goods = getGoodsById(goodsId);
        if (goods.getRemaining() < quantity) {
            throw new RuntimeException("商品库存不足");
        }
        goods.setRemaining(goods.getRemaining() - quantity);
        goods.increaseSales(quantity); //增加销量
        goodsRepository.save(goods);
    }
    //取消订单，增加商品库存时，不减少销量，只恢复库存
    public void increaseStock(Long goodsId, Integer quantity) {
        Goods goods = getGoodsById(goodsId);
        goods.setRemaining(goods.getRemaining() + quantity);
        goodsRepository.save(goods);
    }

    //商品查询
    //获取所有商品（按名称排序）
    public List<Goods> getAllGoods() {
        return goodsRepository.findAllByOrderByName();
    }
    //根据ID获取商品
    public Goods getGoodsById(Long gid) {
        return goodsRepository.findById(gid)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
    }
    //根据名称搜索商品（只返回上架商品）
    public List<Goods> searchGoodsByName(String name) {
        return goodsRepository.findByNameContainingIgnoreCaseAndStatus(name);
    }
    //根据描述搜索商品
    public List<Goods> searchGoodsByDescription(String description) {
        return goodsRepository.findByDescriptionContainingIgnoreCase(description);
    }
    //按价格范围搜索商品（只返回上架商品）
    public List<Goods> searchGoodsByPriceRange(Double minPrice, Double maxPrice) {
        return goodsRepository.findByPriceBetweenAndStatus(minPrice, maxPrice);
    }
    //获取有库存的商品（只返回上架商品）
    public List<Goods> getAvailableGoods() {
        return goodsRepository.findByStatusAndRemainingGreaterThan(1, 0);
    }

    //获取商家的所有商品
    public List<Goods> getGoodsByMerchant(Long merchantId) {
        return goodsRepository.findByMerchantUid(merchantId);
    }
    //获取商家上架商品
    public List<Goods> getActiveGoodsByMerchant(Long merchantId) {
        return goodsRepository.findByMerchantUidAndStatus(merchantId, 1);
    }
    //获取商家的有库存商品
    public List<Goods> getAvailableGoodsByMerchant(Long merchantId) {
        return goodsRepository.findByMerchantUidAndRemainingGreaterThan(merchantId, 0);
    }
    //商家搜索自己的商品
    public List<Goods> searchGoodsByMerchantAndName(Long merchantId, String name) {
        return goodsRepository.findByMerchantUidAndNameContainingIgnoreCase(merchantId, name);
    }
    //验证商家商品权限
    public boolean hasGoodsPermission(Long merchantId, Long goodsId) {
        return goodsRepository.existsByGidAndMerchantUid(goodsId, merchantId);
    }
    //获取商家商品统计方法
    public MerchantGoodsStats getMerchantGoodsStats(Long merchantId) {
        Integer totalStock = goodsRepository.getTotalStockByMerchantId(merchantId);
        Double totalValue = goodsRepository.getTotalInventoryValueByMerchantId(merchantId);
        Long goodsCount = goodsRepository.countByMerchantUid(merchantId);
        Integer totalSales = goodsRepository.getTotalSalesByMerchantId(merchantId);
        Double totalSalesValue = goodsRepository.getTotalSalesValueByMerchantId(merchantId);
        return new MerchantGoodsStats(goodsCount, totalStock, totalValue, totalSales, totalSalesValue);
    }
    //获取商家商品销量排名
    public List<Goods> getGoodsSalesRanking(Long merchantId) {
        return goodsRepository.findByMerchantUidOrderBySalesDesc(merchantId);
    }
    //获取商家商品销售额排名
    public List<Goods> getGoodsSalesValueRanking(Long merchantId) {
        return goodsRepository.findByMerchantUidOrderBySalesValueDesc(merchantId);
    }
    //更新商品销量（订单完成时调用）
    public void updateGoodsSales(Long goodsId, Integer quantity) {
        Goods goods = getGoodsById(goodsId);
        goods.increaseSales(quantity);
        goodsRepository.save(goods);
    }

    //商家商品统计类，添加销量统计
    public static class MerchantGoodsStats {
        private Long goodsCount;
        private Integer totalStock;
        private Double totalValue;
        private Integer totalSales; //总销量
        private Double totalSalesValue; //总销售额
        public MerchantGoodsStats(Long goodsCount, Integer totalStock, Double totalValue,
                                  Integer totalSales, Double totalSalesValue) {
            this.goodsCount = goodsCount;
            this.totalStock = totalStock;
            this.totalValue = totalValue;
            this.totalSales = totalSales;
            this.totalSalesValue = totalSalesValue;
        }
        //getter函数
        public Long getGoodsCount() { return goodsCount; }
        public Integer getTotalStock() { return totalStock; }
        public Double getTotalValue() { return totalValue; }
        public Integer getTotalSales() { return totalSales; }
        public Double getTotalSalesValue() { return totalSalesValue; }
    }
}