package com.onlineshop.onlineshop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name="user")
public class User {
    //主键定义
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long uid;
    //用户名
    @NotBlank
    @Column(unique=true)
    private String nickname;
    //用户密码
    @NotBlank
    @Size(min = 6, max = 20)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,20}$",
            message = "密码必须包含大小写字母、数字和特殊字符"
    )
    private String password;
    //用户邮箱
    @Email
    private String email;
    //用户地址
    @NotBlank
    private String address;
    //用户头像URL
    private String avatarUrl;
    //枚举定义用户角色
    @Enumerated(EnumType.STRING)
    private UserRole role;
    public enum UserRole {
        CUSTOMER,    // 顾客，可以查看并购买商品，权限最低
        MERCHANT    // 商家，可以修改商品
    }

    //外键定义
    @OneToMany(mappedBy = "user")
    @JsonManagedReference("user-orders")
    private List<UserOrder> orders = new ArrayList<>();


    //getter和setter函数
    public void setUid(Long uid) {
        this.uid = uid;
    }
    public Long getUid() {
        return uid;
    }
    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getAvatarUrl() {
        return avatarUrl;
    }
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    public UserRole getRole() {
        return role;
    }
    public void setRole(UserRole role) {
        this.role = role;
    }
}