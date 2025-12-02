package com.onlineshop.onlineshop.service;

import com.onlineshop.onlineshop.entity.Goods;
import com.onlineshop.onlineshop.entity.OrderGoods;
import com.onlineshop.onlineshop.entity.User;
import com.onlineshop.onlineshop.entity.User.UserRole;
import com.onlineshop.onlineshop.entity.UserOrder;
import com.onlineshop.onlineshop.repository.GoodsRepository;
import com.onlineshop.onlineshop.repository.OrderGoodsRepository;
import com.onlineshop.onlineshop.repository.UserOrderRepository;
import com.onlineshop.onlineshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private UserOrderRepository userOrderRepository;
    @Autowired
    private OrderGoodsRepository orderGoodsRepository;

    //用户注册
    public User register(User user) {
        //检查用户名是否已存在
        if (userRepository.existsByNickname(user.getNickname())) {
            throw new RuntimeException("用户名已存在。（提示：可在用户名末尾添加数字，如9999等，减少重名概率）");
        }
        //检查邮箱是否已存在
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }
        //设置默认角色为顾客，确保角色不为null
        if (user.getRole() == null) {
            user.setRole(UserRole.CUSTOMER);
        }
        return userRepository.save(user);
    }

    //用户名+密码登录
    public User nameLogin(String nickname, String password) {
        Optional<User> user = userRepository.findByNicknameAndPassword(nickname, password);
        User foundUser = user.orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        if (foundUser.getRole() == null) {
            foundUser.setRole(UserRole.CUSTOMER);
            userRepository.save(foundUser);
        }
        return foundUser;
    }

    //邮箱+密码登录
    public User emailLogin(String email, String password) {
        Optional<User> user = userRepository.findByEmailAndPassword(email,password);
        User foundUser = user.orElseThrow(()->new RuntimeException("邮箱或密码错误"));
        if (foundUser.getRole() == null) {
            foundUser.setRole(UserRole.CUSTOMER);
            userRepository.save(foundUser);
        }
        return foundUser;
    }

    //根据ID获取用户信息
    public User getUserById(Long uid) {
        User user = userRepository.findById(uid).orElseThrow(() -> new RuntimeException("用户不存在"));
        //确保角色不为null
        if (user.getRole() == null) {
            user.setRole(UserRole.CUSTOMER);
            userRepository.save(user);
        }
        return user;
    }

    //更新用户信息
    public User updateUser(User user) {
        //直接通过ID查找用户
        User existingUser = userRepository.findById(user.getUid())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        //检查昵称是否被其他用户使用
        Optional<User> existingUserWithSameNickname = userRepository.findByNickname(user.getNickname());
        if (existingUserWithSameNickname.isPresent() &&
                !existingUserWithSameNickname.get().getUid().equals(user.getUid())) {
            throw new RuntimeException("用户名已存在。（提示：可在用户名末尾添加数字，如9999等，减少重名概率）");
        }
        //检查邮箱是否被其他用户使用
        Optional<User> existingUserWithSameEmail = userRepository.findByEmail(user.getEmail());
        if (existingUserWithSameEmail.isPresent() &&
                !existingUserWithSameEmail.get().getUid().equals(user.getUid())) {
            throw new RuntimeException("邮箱已被注册");
        }
        //更新用户信息，保留原有角色（如果新角色为null）
        if (user.getRole() == null) {
            user.setRole(existingUser.getRole());
        }
        //确保密码不为空（如果前端没有传密码，使用原密码）
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            user.setPassword(existingUser.getPassword());
        }
        return userRepository.save(user);
    }

    //账号注销
    public boolean deleteUserAccount(Long userId) {
        User user = getUserById(userId);
        //检查是否为虚拟商家，虚拟商家不可注销
        if ("已注销商家".equals(user.getNickname()) && "deleted@system.com".equals(user.getEmail())) {
            throw new RuntimeException("系统预留商家不允许删除");
        }
        //如果是商家，检查商品状态
        if (user.getRole() == UserRole.MERCHANT) {
            List<Goods> merchantGoods = goodsRepository.findByMerchantUid(userId);
            //检查是否有上架商品
            boolean hasActiveGoods = merchantGoods.stream().anyMatch(goods -> goods.getStatus() == 1);
            if (hasActiveGoods) {
                throw new RuntimeException("商家注销前请先下架所有商品");
            }
            //将商品转移到虚拟商家（uid=12）
            for (Goods goods : merchantGoods) {
                //查找虚拟商家
                User virtualMerchant = userRepository.findById(12L).
                        orElseThrow(() -> new RuntimeException("系统虚拟商家不存在"));
                goods.setMerchant(virtualMerchant);
                goods.setStatus(0); // 下架状态
                goodsRepository.save(goods);
            }
        }
        //删除用户的所有订单
        List<UserOrder> userOrders = userOrderRepository.findByUserUid(userId);
        for (UserOrder order : userOrders) {
            //先删除订单项，再删除订单
            List<OrderGoods> orderItems = orderGoodsRepository.findByUserorder(order);
            orderGoodsRepository.deleteAll(orderItems);
            userOrderRepository.delete(order);
        }
        //最后删除用户
        userRepository.delete(user);
        return true;
    }

    //根据角色获取用户列表
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }
    //检查用户名是否存在
    public boolean isNicknameExists(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
    //检查邮箱是否存在
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}