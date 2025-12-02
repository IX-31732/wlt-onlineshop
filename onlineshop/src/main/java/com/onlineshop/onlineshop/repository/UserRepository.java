package com.onlineshop.onlineshop.repository;

import com.onlineshop.onlineshop.entity.User;
import com.onlineshop.onlineshop.entity.User.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, Long>  {
    //查找单个用户或商家
    Optional<User> findByNickname(String nickname);
    Optional<User> findByEmail(String email);
    Optional<User> findByNicknameAndPassword(String nickname, String password);
    Optional<User> findByEmailAndPassword(String email, String password);
    //根据角色查找多个用户或商家
    List<User> findByRole(UserRole role);

    //判断用户是否存在
    boolean existsByNickname(String nickname);
    boolean existsByEmail(String email);
}
