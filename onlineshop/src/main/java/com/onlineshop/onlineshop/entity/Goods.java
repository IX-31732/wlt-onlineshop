package com.onlineshop.onlineshop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;

@Entity
@Table(name="goods")
public class Goods {
    //主键定义，自动生成商品id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gid;
    //商品名
    @NotBlank
    private String name;
    //商品描述
    private String description;
    //商品价格
    @Column(scale=2, nullable = false)
    @PositiveOrZero
    private Double price;
    //商品库存
    @PositiveOrZero
    private Integer remaining;
    //商品图片URL路径
    private String imageUrl;
    //商品状态（0为下架，1为上架，默认是上架）
    @Column(nullable = false)
    private Integer status = 1;
    //销量统计，初始为0
    @Column(nullable = false)
    @PositiveOrZero
    private Integer sales = 0;

    //外键定义
    @ManyToOne
    @JoinColumn(name = "merchant_id", nullable = false)
    private User merchant;
    @OneToMany(mappedBy = "goods")
    private List<OrderGoods> orderItems = new ArrayList<>();

    //业务逻辑方法定义
    //获取完整商品图片URL
    @Transient
    public String getFullImageUrl() {
        if (this.imageUrl == null || this.imageUrl.isEmpty()) {
            return "/images/default-product.png";
        }
        // 如果已经是完整URL，直接返回
        if (this.imageUrl.startsWith("http")) {
            return this.imageUrl;
        }

        // 处理相对路径
        if (this.imageUrl.startsWith("/uploads/products/")) {
            return this.imageUrl;
        } else if (this.imageUrl.startsWith("products/")) {
            return "/uploads/" + this.imageUrl;
        } else {
            return "/uploads/products/" + this.imageUrl;
        }
    }

    //判断是否上架
    @Transient
    public boolean isAvailable() {
        return status != null && status == 1 && getRemaining() != null && getRemaining() > 0;
    }

    //getter和setter方法
    public Long getGid() {
        return gid;
    }
    public void setGid(Long gid) {
        this.gid = gid;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Double getPrice() {
        return price;
    }
    public void setPrice(Double price) {
        this.price = price;
    }
    public Integer getRemaining() {
        return remaining;
    }
    public void setRemaining(Integer remaining) {
        this.remaining = remaining;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public User getMerchant() {
        return merchant;
    }
    public void setMerchant(User merchant) {
        this.merchant = merchant;
    }
    public List<OrderGoods> getOrderItems() {
        return orderItems;
    }
    public void setOrderItems(List<OrderGoods> orderItems) {
        this.orderItems = orderItems;
    }
    // 获取和设置状态的方法
    public Integer getStatus() {
        return status;
    }
    public void setStatus(Integer status) {
        this.status = status;
    }
    public void increaseSales(Integer quantity) {
        if (quantity > 0) {
            this.sales = (this.sales == null ? 0 : this.sales) + quantity;
        }
    }
    public Integer getSales() {
        return sales != null ? sales : 0;
    }
    public void setSales(Integer sales) {
        this.sales = sales != null ? sales : 0;
    }
}