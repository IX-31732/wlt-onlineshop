package com.onlineshop.onlineshop.controller;

import com.onlineshop.onlineshop.entity.User;
import com.onlineshop.onlineshop.entity.UserOrder;
import com.onlineshop.onlineshop.service.OrderService;
import com.onlineshop.onlineshop.service.SalesService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class SellController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SalesService salesService;

    //权限检查方法
    private boolean checkMerchantPermission(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        return currentUser != null && currentUser.getRole() == User.UserRole.MERCHANT;
    }

    //获取当前商家信息
    private User getCurrentMerchant(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
            throw new RuntimeException("无权限访问");
        }
        return currentUser;
    }

    //获取当前商家的订单
    @GetMapping("/orders")
    public ResponseEntity<?> getMerchantOrders(@RequestParam(required = false) String status,
                                               HttpSession session) {
        try {
            User merchant = getCurrentMerchant(session);
            List<UserOrder> orders;
            if (status != null && !status.isEmpty()) {
                // 需要实现按状态获取商家订单的方法
                orders = orderService.getMerchantOrdersByStatus(merchant, status);
            } else {
                orders = orderService.getMerchantOrders(merchant);
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orders", orders
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    //获取订单详情（商家视角）
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getMerchantOrderDetails(@PathVariable Long orderId, HttpSession session) {
        try {
            User merchant = getCurrentMerchant(session);
            //验证订单是否属于该商家
            UserOrder order = orderService.getOrderById(orderId);
            if (!orderService.isOrderBelongsToMerchant(order, merchant)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "无权访问此订单"
                ));
            }
            var orderDetails = orderService.getOrderDetails(orderId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderDetails", orderDetails
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    //发货操作
    @PostMapping("/orders/{orderId}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable Long orderId, HttpSession session) {
        try {
            User merchant = getCurrentMerchant(session);
            UserOrder order = orderService.fahuo(orderId, merchant);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "发货成功",
                    "order", order
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    //获取销售统计报表
    @GetMapping("/sales/report")
    public ResponseEntity<?> getSalesReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            HttpSession session) {
        try {
            User merchant = getCurrentMerchant(session);
            var report = salesService.getMerchantSalesReport(merchant.getUid(), startDate, endDate);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "report", report
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    //获取热销商品
    @GetMapping("/sales/best-sellers")
    public ResponseEntity<?> getBestSellingGoods(HttpSession session) {
        try {
            User merchant = getCurrentMerchant(session);
            var bestSellers = salesService.getMerchantBestSellingGoods(merchant.getUid());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "bestSellers", bestSellers
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    //获取商家相关数据
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData(HttpSession session) {
        try {
            User merchant = getCurrentMerchant(session);
            // 获取今日销售额
            Date today = new Date();
            Date startOfDay = new Date(today.getYear(), today.getMonth(), today.getDate());
            Date endOfDay = new Date(today.getYear(), today.getMonth(), today.getDate(), 23, 59, 59);
            Double todaySales = salesService.getMerchantSalesByDateRange(merchant.getUid(), startOfDay, endOfDay);
            // 获取待发货订单数量
            Long pendingShipmentCount = orderService.getMerchantOrderCountByStatus(merchant, "已付款");
            // 获取商品统计
            var goodsStats = salesService.getMerchantGoodsStats(merchant.getUid());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "dashboard", Map.of(
                            "todaySales", todaySales != null ? todaySales : 0.0,
                            "pendingShipmentCount", pendingShipmentCount,
                            "goodsStats", goodsStats
                    )
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}