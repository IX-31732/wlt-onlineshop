package com.onlineshop.onlineshop.repository;

import com.onlineshop.onlineshop.entity.UserOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface UserOrderRepository extends JpaRepository<UserOrder, Long> {
    //查找订单
    List<UserOrder> findByUserUid(Long uid);
    List<UserOrder> findByUserOrderByOrderDateDesc(UserOrder user);
    List<UserOrder> findByStatusOrderByOrderDateDesc(String status);
    List<UserOrder> findByUserAndStatus(UserOrder user, String status);
    List<UserOrder> findByOrderDateBetween(Date start, Date end);
    //计算某时间段的订单总数，用于销售统计
    Long countByOrderDateBetween(Date start, Date end);
    //计算某时间段的销售总额，用于销售统计
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM UserOrder o WHERE o.orderDate BETWEEN :start AND :end")
    Double getTotalSalesByDateRange(@Param("start") Date start, @Param("end") Date end);
    //根据订单状态和日期范围查找
    List<UserOrder> findByStatusAndOrderDateBetween(String status, Date start, Date end);
    // 获取包含特定商家的商品的订单
    @Query("SELECT DISTINCT o FROM UserOrder o JOIN o.orderItems og WHERE og.goods.merchant.uid = :merchantId")
    List<UserOrder> findOrdersByMerchantId(@Param("merchantId") Long merchantId);
    // 获取商家在特定状态的订单
    @Query("SELECT DISTINCT o FROM UserOrder o JOIN o.orderItems og " +
            "WHERE og.goods.merchant.uid = :merchantId AND o.status = :status")
    List<UserOrder> findOrdersByMerchantIdAndStatus(@Param("merchantId") Long merchantId,
                                                    @Param("status") String status);
}