package com.onlineshop.onlineshop.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name="userorder")
public class UserOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    //外键定义
    @ManyToOne
    @JoinColumn(name = "userid")
    @JsonBackReference("user-orders")
    private User user;

    @OneToMany(mappedBy = "userorder", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("order-orderitems")
    private List<OrderGoods> orderItems = new ArrayList<>();

    //其他变量
    //订单创建时间
    @Column(name = "order_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date orderDate;
    //订单支付时间
    @Column(name = "pay_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date payTime;
    //订单发货时间
    @Column(name = "ship_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date shipTime;
    //订单完成时间
    @Column(name = "completetime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date completeTime;// 完成时间
    //订单状态
    @Pattern(regexp = "待付款|已付款|已发货|已完成|已取消", message = "订单状态不合法")
    private String status;
    //订单总价
    @PositiveOrZero
    private Double totalAmount;

    //getter和setter函数
    public void setUser(User user) {
        this.user = user;
    }
    public User getUser() {
        return user;
    }
    public void setOrderItems(List<OrderGoods> orderItems) {
        this.orderItems = orderItems;
    }
    public List<OrderGoods> getOrderItems() {
        return orderItems;
    }
    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }
    public Date getOrderDate() {
        return orderDate;
    }
    public void setStatus(String status) {
        this.status=status;
    }
    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
    public String getStatus() {
        return status;
    }
    public Long getOid() {
        return oid;
    }
    public void setOid(Long oid) {
        this.oid = oid;
    }
    public Double getTotalAmount() {
        return totalAmount;
    }
    public Date getPayTime() { return payTime; }
    public void setPayTime(Date payTime) { this.payTime = payTime; }
    public Date getShipTime() { return shipTime; }
    public void setShipTime(Date shipTime) { this.shipTime = shipTime; }
    public Date getCompleteTime() { return completeTime; }
    public void setCompleteTime(Date completeTime) { this.completeTime = completeTime; }
}
