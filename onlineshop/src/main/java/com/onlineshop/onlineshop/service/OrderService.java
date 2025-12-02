package com.onlineshop.onlineshop.service;

import com.onlineshop.onlineshop.entity.*;
import com.onlineshop.onlineshop.repository.OrderGoodsRepository;
import com.onlineshop.onlineshop.repository.UserOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {
    @Autowired
    private UserOrderRepository userOrderRepository;
    @Autowired
    private OrderGoodsRepository orderGoodsRepository;
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private EmailService emailService;

    //订单生命周期（创建，付款，发货，完成，取消）
    //创建订单（直接使用商品ID，商品已关联商家）
    public UserOrder createOrder(User user, List<CartItem> cartItems) {
        //验证库存并计算总金额
        double totalAmount = 0.0;
        for (CartItem item : cartItems) {
            Goods goods = goodsService.getGoodsById(item.getGid());
            if (goods.getRemaining() < item.getQuantity()) {
                throw new RuntimeException("商品 " + goods.getName() + " 库存不足");
            }
            totalAmount += goods.getPrice() * item.getQuantity();
        }
        // 创建订单
        UserOrder order = new UserOrder();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setStatus("待付款");
        order.setTotalAmount(totalAmount);
        UserOrder savedOrder = userOrderRepository.save(order);
        //创建订单项并减少库存
        for (CartItem item : cartItems) {
            Goods goods = goodsService.getGoodsById(item.getGid());
            OrderGoods orderGoods = new OrderGoods();
            orderGoods.setUserorder(savedOrder);
            orderGoods.setGoods(goods);
            orderGoods.setQuantity(item.getQuantity());
            orderGoods.setPriceAtPurchase(goods.getPrice());
            orderGoodsRepository.save(orderGoods);
            goodsService.decreaseStock(item.getGid(), item.getQuantity());
        }
        // 发送订单确认邮件
        emailService.sendOrderConfirmation(user.getEmail(), savedOrder);
        return savedOrder;
    }
    //订单付款
    public UserOrder pay(Long orderId) {
        UserOrder order = userOrderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!"待付款".equals(order.getStatus())) {
            throw new RuntimeException("当前无需付款");
        }
        order.setStatus("已付款");
        order.setPayTime(new Date());
        return userOrderRepository.save(order);
    }
    //发货
    public UserOrder fahuo(Long orderId, User merchant) {
        UserOrder order = userOrderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!"已付款".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许发货");
        }
        order.setStatus("已发货");
        order.setShipTime(new Date());
        UserOrder savedOrder = userOrderRepository.save(order);
        // 发送邮件通知
        emailService.sendShippingConfirmation(order.getUser().getEmail(), order);
        return savedOrder;
    }
    //完成订单
    public UserOrder completeOrder(Long orderId) {
        UserOrder order = userOrderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!"已发货".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许完成");
        }
        order.setStatus("已完成");
        order.setCompleteTime(new Date());
        return userOrderRepository.save(order);
    }
    //取消订单
    public UserOrder cancelOrder(Long orderId) {
        UserOrder order = userOrderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));
        if ("已完成".equals(order.getStatus()) || "已取消".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许取消");
        }
        //恢复库存
        List<OrderGoods> orderItems = orderGoodsRepository.findByUserorder(order);
        for (OrderGoods item : orderItems) {
            Goods goods = item.getGoods();
            goodsService.increaseStock(goods.getGid(), item.getQuantity());
        }
        order.setStatus("已取消");
        return userOrderRepository.save(order);
    }

    //订单查询相关功能
    //获取用户订单历史
    public List<UserOrder> getUserOrderHistory(Long userId) {
        return userOrderRepository.findByUserUid(userId);
    }
    //获取商家的订单（订单只包含该商家的商品）
    public List<UserOrder> getMerchantOrders(User merchant) {
        List<UserOrder> allOrders = userOrderRepository.findAll();
        return allOrders.stream().filter(order -> isOrderBelongsToMerchant(order, merchant))
                .collect(Collectors.toList());
    }
    //根据状态获取商家的订单
    public List<UserOrder> getMerchantOrdersByStatus(User merchant, String status) {
        return getMerchantOrders(merchant).stream().filter(order -> status.equals(order.getStatus()))
                .collect(Collectors.toList());
    }
    //根据状态获取订单
    public List<UserOrder> getOrdersByStatus(String status) {
        return userOrderRepository.findByStatusOrderByOrderDateDesc(status);
    }
    //获取订单详情
    public List<OrderGoods> getOrderDetails(Long orderId) {
        UserOrder order = userOrderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));
        return orderGoodsRepository.findByUserorder(order);
    }
    //根据ID获取订单
    public UserOrder getOrderById(Long orderId) {
        return userOrderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("订单不存在"));
    }
    //验证订单是否属于商家
    public boolean isOrderBelongsToMerchant(UserOrder order, User merchant) {
        List<OrderGoods> orderItems = orderGoodsRepository.findByUserorder(order);
        for (OrderGoods item : orderItems) {
            if (item.getGoods().getMerchant().getUid().equals(merchant.getUid())) {
                return true;
            }
        }
        return false;
    }
    //获取商家指定状态的订单数量
    public Long getMerchantOrderCountByStatus(User merchant, String status) {
        return getMerchantOrders(merchant).stream().filter(order -> status.equals(order.getStatus())).count();
    }
}