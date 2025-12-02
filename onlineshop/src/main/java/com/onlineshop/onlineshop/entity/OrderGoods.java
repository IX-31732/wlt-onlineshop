package com.onlineshop.onlineshop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name="ordergoods")
public class OrderGoods {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //外键定义
    @ManyToOne
    @JoinColumn(name = "orderid")
    @JsonBackReference("order-orderitems")
    private UserOrder userorder;
    @ManyToOne
    @JoinColumn(name = "goodsid")
    @JsonIgnoreProperties({"orderItems", "merchant"})
    private Goods goods;

    //其他变量
    //购买商品数
    @Column(name = "quantity", nullable = false)
    @PositiveOrZero
    private Integer quantity;
    //商品单价
    @Column(name = "price_at_purchase", nullable = false)
    @PositiveOrZero
    private Double priceAtPurchase;

    //getter和setter函数
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public UserOrder getUserorder() {
        return userorder;
    }
    public void setUserorder(UserOrder userorder) {
        this.userorder = userorder;
    }
    public Goods getGoods() {
        return goods;
    }
    public void setGoods(Goods goods) {
        this.goods = goods;
    }
    public Integer getQuantity() {
        return quantity;
    }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public Double getPriceAtPurchase() {
        return priceAtPurchase;
    }
    public void setPriceAtPurchase(Double priceAtPurchase) {
        this.priceAtPurchase = priceAtPurchase;
    }
}