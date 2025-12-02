package com.onlineshop.onlineshop.repository;

import com.onlineshop.onlineshop.entity.Goods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface GoodsRepository extends JpaRepository<Goods, Long> {
    //所有商品
    List<Goods> findAllByOrderByName();

    //查找商品
    //根据商家查找商品
    List<Goods> findByMerchantUid(Long merchantId);
    //根据商家和商品名称查找
    List<Goods> findByMerchantUidAndNameContainingIgnoreCase(Long merchantId, String name);
    //查找商家有库存的商品
    List<Goods> findByMerchantUidAndRemainingGreaterThan(Long merchantId, Integer remaining);
    //根据商家和状态查找
    List<Goods> findByMerchantUidAndStatus(Long merchantId, Integer status);
    //根据状态和库存查找
    List<Goods> findByStatusAndRemainingGreaterThan(Integer status, Integer remaining);
    //按价格范围查找（只包含上架商品）
    @Query("SELECT g FROM Goods g WHERE g.status = 1 AND g.price BETWEEN :minPrice AND :maxPrice")
    List<Goods> findByPriceBetweenAndStatus(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);
    //模糊查询（只包含上架商品）
    @Query("SELECT g FROM Goods g WHERE g.status = 1 AND g.name LIKE %:name%")
    List<Goods> findByNameContainingIgnoreCaseAndStatus(@Param("name") String name);
    List<Goods> findByDescriptionContainingIgnoreCase(String description);
    //按销量排序查询
    List<Goods> findByMerchantUidOrderBySalesDesc(Long merchantId);
    //按销售额排序查询（销量*价格）
    @Query("SELECT g FROM Goods g WHERE g.merchant.uid = :merchantId ORDER BY (g.sales * g.price) DESC")
    List<Goods> findByMerchantUidOrderBySalesValueDesc(@Param("merchantId") Long merchantId);
    //获取有库存的商品（只包含上架商品）
    @Query("SELECT g FROM Goods g WHERE g.status = 1 AND g.remaining > 0")
    List<Goods> findAvailableGoods();

    //统计相关函数
    //统计商家的商品数量
    Long countByMerchantUid(Long merchantId);
    //获取商家的商品总库存
    @Query("SELECT COALESCE(SUM(g.remaining), 0) FROM Goods g WHERE g.merchant.uid = :merchantId")
    Integer getTotalStockByMerchantId(@Param("merchantId") Long merchantId);
    //获取商家的商品总价值
    @Query("SELECT COALESCE(SUM(g.remaining * g.price), 0) FROM Goods g WHERE g.merchant.uid = :merchantId")
    Double getTotalInventoryValueByMerchantId(@Param("merchantId") Long merchantId);

    //获取总销量
    @Query("SELECT COALESCE(SUM(g.sales), 0) FROM Goods g WHERE g.merchant.uid = :merchantId")
    Integer getTotalSalesByMerchantId(@Param("merchantId") Long merchantId);
    //获取总销售额
    @Query("SELECT COALESCE(SUM(g.sales * g.price), 0) FROM Goods g WHERE g.merchant.uid = :merchantId")
    Double getTotalSalesValueByMerchantId(@Param("merchantId") Long merchantId);

    //判断函数
    //检查商家是否有该商品
    boolean existsByGidAndMerchantUid(Long gid, Long merchantId);
}