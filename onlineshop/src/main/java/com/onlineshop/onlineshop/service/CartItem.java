package com.onlineshop.onlineshop.service;

public class CartItem {
    private Long gid;
    private Integer quantity;

    public CartItem() {}
    public CartItem(Long gid, Integer quantity) {
        this.gid = gid;
        this.quantity = quantity;
    }

    public Long getGid() { return gid; }
    public void setGid(Long gid) { this.gid = gid; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}