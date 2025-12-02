package com.onlineshop.onlineshop.controller;

import com.onlineshop.onlineshop.entity.*;
import com.onlineshop.onlineshop.service.CartItem;
import com.onlineshop.onlineshop.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    //创建订单
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody List<CartItem> cartItems,
                                         HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录"
                ));
            }
            UserOrder order = orderService.createOrder(currentUser, cartItems);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "订单创建成功",
                    "order", order
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    //付款
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<?> payOrder(@PathVariable Long orderId, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录"
                ));
            }
            UserOrder order = orderService.pay(orderId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "付款成功",
                    "order", order
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    //获取用户订单历史
    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录"
                ));
            }
            List<UserOrder> orders = orderService.getUserOrderHistory(currentUser.getUid());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orders", orders
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    //获取订单详情
    @GetMapping("/{orderId}/details")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录"
                ));
            }
            //获取订单和订单项
            UserOrder order = orderService.getOrderById(orderId);
            List<OrderGoods> orderGoods = orderService.getOrderDetails(orderId);
            //构建包含完整订单信息的响应
            Map<String, Object> orderResponse = new HashMap<>();
            orderResponse.put("oid", order.getOid());
            orderResponse.put("orderDate", order.getOrderDate());
            orderResponse.put("payTime", order.getPayTime());
            orderResponse.put("shipTime", order.getShipTime());
            orderResponse.put("completeTime", order.getCompleteTime());
            orderResponse.put("status", order.getStatus());
            orderResponse.put("totalAmount", order.getTotalAmount());
            orderResponse.put("user", order.getUser()); // 如果需要用户信息
            //构建订单项详情
            List<Map<String, Object>> orderDetails = orderGoods.stream().map(og -> {
                Map<String, Object> detail = new HashMap<>();
                Goods goods = og.getGoods();
                //基础订单项信息
                detail.put("id", og.getId());
                detail.put("gid", goods != null ? goods.getGid() : og.getId());
                detail.put("price", og.getPriceAtPurchase());
                detail.put("quantity", og.getQuantity());
                detail.put("priceAtPurchase", og.getPriceAtPurchase());
                //商品详细信息
                if (goods != null) {
                    detail.put("name", goods.getName());
                    detail.put("description", goods.getDescription());
                    detail.put("imageUrl", goods.getImageUrl());
                    detail.put("fullImageUrl", goods.getFullImageUrl());
                } else {
                    detail.put("name", "商品" + og.getId());
                    detail.put("description", "商品描述信息");
                    detail.put("imageUrl", "defaultpicture.png");
                    detail.put("fullImageUrl", "/uploads/products/defaultpicture.png");
                }
                return detail;
            }).collect(Collectors.toList());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("order", orderResponse); // 使用包含完整信息的orderResponse
            response.put("orderDetails", orderDetails);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    //取消订单
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录"
                ));
            }
            UserOrder order = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "订单取消成功",
                    "order", order
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    //发货接口
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable Long orderId, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "无权限操作"
                ));
            }
            UserOrder order = orderService.fahuo(orderId, currentUser);
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

    //完成订单
    @PostMapping("/{orderId}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable Long orderId, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录"
                ));
            }
            UserOrder order = orderService.completeOrder(orderId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "订单已完成",
                    "order", order
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}