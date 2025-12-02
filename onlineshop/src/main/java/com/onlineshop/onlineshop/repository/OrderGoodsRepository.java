package com.onlineshop.onlineshop.repository;

import com.onlineshop.onlineshop.entity.UserOrder;
import com.onlineshop.onlineshop.entity.OrderGoods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface OrderGoodsRepository extends JpaRepository<OrderGoods, Long> {
    //根据订单查找订单项
    List<OrderGoods> findByUserorder(UserOrder userorder);
    //获取热销商品
    @Query("SELECT og.goods.gid, og.goods.name, SUM(og.quantity) as totalSold " +
            "FROM OrderGoods og GROUP BY og.goods.gid, og.goods.name ORDER BY totalSold DESC")
    List<Object[]> findBestSellingGoods();
    //获取商家的热销商品
    @Query("SELECT og.goods.gid, og.goods.name, SUM(og.quantity) as totalSold " +
            "FROM OrderGoods og WHERE og.goods.merchant.uid = :merchantId " +
            "GROUP BY og.goods.gid, og.goods.name ORDER BY totalSold DESC")
    List<Object[]> findMerchantBestSellingGoods(@Param("merchantId") Long merchantId);
    //统计某个商品的销售总量
    @Query("SELECT COALESCE(SUM(og.quantity), 0) FROM OrderGoods og WHERE og.goods.gid = :goodsId")
    Integer getTotalSoldQuantityByGoodsId(@Param("goodsId") Long goodsId);
    //获取某个时间段的商品销售统计
    @Query("SELECT og.goods.gid, og.goods.name, SUM(og.quantity), SUM(og.quantity * og.priceAtPurchase) " +
            "FROM OrderGoods og " +
            "WHERE og.userorder.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY og.goods.gid, og.goods.name")
    List<Object[]> getGoodsSalesByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    //获取商家在某个时间段的销售统计
    @Query("SELECT og.userorder.oid, og.goods.gid, og.goods.name, og.quantity * og.priceAtPurchase as sales " +
            "FROM OrderGoods og " +
            "WHERE og.goods.merchant.uid = :merchantId " +
            "AND og.userorder.orderDate BETWEEN :startDate AND :endDate")
    List<Object[]> getMerchantSalesByDateRange(@Param("merchantId") Long merchantId,
                                               @Param("startDate") Date startDate,
                                               @Param("endDate") Date endDate);
    //根据商家ID查找订单项
    @Query("SELECT og FROM OrderGoods og WHERE og.goods.merchant.uid = :merchantId")
    List<OrderGoods> findByMerchantId(@Param("merchantId") Long merchantId);
}