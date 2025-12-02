package com.onlineshop.onlineshop.controller;

import com.onlineshop.onlineshop.entity.User;
import com.onlineshop.onlineshop.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class LoginController {

    @Autowired
    private UserService userService;

    //添加调试端点
    @GetMapping("/debug-session")
    public ResponseEntity<?> debugSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Map<String, Object> debugInfo = new java.util.HashMap<>();
        debugInfo.put("sessionExists", session != null);
        if (session != null) {
            debugInfo.put("sessionId", session.getId());
            debugInfo.put("creationTime", new java.util.Date(session.getCreationTime()));
            debugInfo.put("lastAccessedTime", new java.util.Date(session.getLastAccessedTime()));
            debugInfo.put("maxInactiveInterval", session.getMaxInactiveInterval());
            User currentUser = (User) session.getAttribute("currentUser");
            debugInfo.put("userInSession", currentUser != null);
            if (currentUser != null) {
                debugInfo.put("userId", currentUser.getUid());
                debugInfo.put("userName", currentUser.getNickname());
            }
            //列出所有Session属性
            java.util.Enumeration<String> attributeNames = session.getAttributeNames();
            java.util.List<String> attributes = new java.util.ArrayList<>();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement();
                attributes.add(name + " = " + session.getAttribute(name));
            }
            debugInfo.put("sessionAttributes", attributes);
        }
        //检查请求头
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        debugInfo.put("requestHeaders", headers);
        debugInfo.put("cookies", request.getCookies());
        return ResponseEntity.ok(debugInfo);
    }

    //Session过期端点
    @GetMapping("/session-expired")
    public ResponseEntity<?> sessionExpired() {
        return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Session已过期，请重新登录",
                "errorType", "SESSION_EXPIRED"
        ));
    }

    //成为商家接口
    @PostMapping("/become-merchant")
    public ResponseEntity<?> becomeMerchant(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录",
                        "errorType", "SESSION_NOT_FOUND"
                ));
            }
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录",
                        "errorType", "USER_NOT_FOUND_IN_SESSION"
                ));
            }
            //检查是否已经是商家
            if (currentUser.getRole() == User.UserRole.MERCHANT) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "您已经是商家",
                        "errorType", "ALREADY_MERCHANT"
                ));
            }
            //转换为商家角色
            currentUser.setRole(User.UserRole.MERCHANT);
            User updatedUser = userService.updateUser(currentUser);

            //更新Session中的用户信息
            session.setAttribute("currentUser", updatedUser);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "恭喜！您已成为商家",
                    "user", createUserResponse(updatedUser)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorType", "ROLE_UPDATE_ERROR"
            ));
        }
    }

    //根据商家ID获取商家信息
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<?> getMerchantById(@PathVariable Long merchantId) {
        try {
            User merchant = userService.getUserById(merchantId);

            if (merchant == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "商家不存在"
                ));
            }
            //验证是否为商家
            if (merchant.getRole() != User.UserRole.MERCHANT) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "该用户不是商家"
                ));
            }
            //返回商家信息（不包含敏感信息）
            Map<String, Object> merchantResponse = createUserResponse(merchant);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "merchant", merchantResponse  // 确保返回merchant字段，与前端期望一致
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user, HttpServletRequest request) {
        try {
            if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
                user.setAvatarUrl("/uploads/avatars/default-avatar.png");
            }
            User savedUser = userService.register(user);
            //确保Session正确创建和同步
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", savedUser);
            session.setMaxInactiveInterval(30 * 60);
            //同步Session到服务器
            session.setAttribute("sessionSynced", true);
            //验证Session是否设置成功
            User sessionUser = (User) session.getAttribute("currentUser");
            System.out.println("✅ 注册成功 - Session ID: " + session.getId() + ", 用户ID: " +
                    (sessionUser != null ? sessionUser.getUid() : "null"));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "注册成功",
                    "user", createUserResponse(savedUser),
                    "sessionCreated", true,
                    "sessionId", session.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorType", "REGISTRATION_ERROR"
            ));
        }
    }

    @PostMapping("/login/name")
    public ResponseEntity<?> loginByName(@RequestBody Map<String, String> credentials,
                                         HttpServletRequest request) {
        try {
            String nickname = credentials.get("nickname");
            String password = credentials.get("password");

            if (nickname == null || nickname.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "用户名不能为空",
                        "errorType", "VALIDATION_ERROR"
                ));
            }
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "密码不能为空",
                        "errorType", "VALIDATION_ERROR"
                ));
            }
            User user = userService.nameLogin(nickname, password);
            //确保Session正确创建和同步
            HttpSession session = request.getSession(true);
            //先清除可能存在的旧Session数据
            java.util.Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement();
                session.removeAttribute(name);
            }
            session.setAttribute("currentUser", user);
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute("sessionSynced", true);
            //立即验证Session是否设置成功
            User sessionUser = (User) session.getAttribute("currentUser");
            System.out.println("✅ 登录成功 - Session ID: " + session.getId() + ", 用户ID: " +
                    (sessionUser != null ? sessionUser.getUid() : "null"));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "登录成功",
                    "user", createUserResponse(user),
                    "sessionCreated", true,
                    "sessionId", session.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorType", "LOGIN_ERROR"
            ));
        }
    }

    @PostMapping("/login/email")
    public ResponseEntity<?> loginByEmail(@RequestBody Map<String, String> credentials,
                                          HttpServletRequest request) {
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "邮箱不能为空",
                        "errorType", "VALIDATION_ERROR"
                ));
            }
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "密码不能为空",
                        "errorType", "VALIDATION_ERROR"
                ));
            }
            User user = userService.emailLogin(email, password);
            //确保Session正确创建和同步
            HttpSession session = request.getSession(true);
            //先清除可能存在的旧Session数据
            java.util.Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement();
                session.removeAttribute(name);
            }
            session.setAttribute("currentUser", user);
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute("sessionSynced", true);
            //立即验证Session是否设置成功
            User sessionUser = (User) session.getAttribute("currentUser");
            System.out.println("✅ 登录成功 - Session ID: " + session.getId() + ", 用户ID: " +
                    (sessionUser != null ? sessionUser.getUid() : "null"));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "登录成功",
                    "user", createUserResponse(user),
                    "sessionCreated", true,
                    "sessionId", session.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorType", "LOGIN_ERROR"
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            System.out.println("注销Session: " + session.getId());
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "注销成功"
        ));
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("Session不存在");
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "用户未登录",
                    "sessionExists", false,
                    "errorType", "SESSION_NOT_FOUND"
            ));
        }
        //验证Session是否有效
        try {
            session.getAttributeNames();
        } catch (IllegalStateException e) {
            System.out.println("Session已失效: " + session.getId());
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Session已失效，请重新登录",
                    "sessionExists", false,
                    "errorType", "SESSION_EXPIRED"
            ));
        }
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            System.out.println("Session存在但用户信息为空: " + session.getId());
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "用户未登录",
                    "sessionExists", true,
                    "userInSession", false,
                    "errorType", "USER_NOT_FOUND_IN_SESSION"
            ));
        }

        System.out.println("获取当前用户成功: " + currentUser.getUid() + ", Session: " + session.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "user", createUserResponse(currentUser),
                "sessionExists", true,
                "userInSession", true,
                "sessionId", session.getId()
        ));
    }

    //增强Session验证端点
    @GetMapping("/session-check")
    public ResponseEntity<?> checkSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Session不存在",
                    "sessionExists", false,
                    "errorType", "SESSION_NOT_FOUND"
            ));
        }
        //验证Session是否有效
        try {
            session.getAttributeNames();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Session已失效",
                    "sessionExists", false,
                    "errorType", "SESSION_EXPIRED"
            ));
        }
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Session存在但用户信息为空",
                    "sessionExists", true,
                    "userInSession", false,
                    "errorType", "USER_NOT_FOUND_IN_SESSION"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session验证成功",
                "sessionExists", true,
                "userInSession", true,
                "user", createUserResponse(currentUser),
                "sessionId", session.getId()
        ));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<?> checkNickname(@RequestParam String nickname) {
        boolean exists = userService.isNicknameExists(nickname);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean exists = userService.isEmailExists(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User user, HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录",
                        "errorType", "SESSION_NOT_FOUND"
                ));
            }
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录",
                        "errorType", "USER_NOT_FOUND_IN_SESSION"
                ));
            }
            user.setUid(currentUser.getUid());
            User updatedUser = userService.updateUser(user);
            session.setAttribute("currentUser", updatedUser);
            //返回响应
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "个人信息更新成功",
                    "user", createUserResponse(updatedUser) // 使用安全的响应格式
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorType", "UPDATE_ERROR"
            ));
        }
    }

    private Map<String, Object> createUserResponse(User user) {
        return Map.of(
                "uid", user.getUid(),
                "nickname", user.getNickname(),
                "email", user.getEmail(),
                "address", user.getAddress() != null ? user.getAddress() : "",
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "/uploads/avatars/default-avatar.png",
                "role", user.getRole() != null ? user.getRole().name() : "CUSTOMER"
        );
    }

    //用户注销接口
    @PostMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录"
                ));
            }
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "请先登录"
                ));
            }
            //调用UserService处理注销逻辑
            boolean result = userService.deleteUserAccount(currentUser.getUid());
            //注销Session
            if (session != null) {
                session.invalidate();
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "账号注销成功"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}