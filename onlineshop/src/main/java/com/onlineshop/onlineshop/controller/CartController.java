package com.onlineshop.onlineshop.controller;

import com.onlineshop.onlineshop.entity.User;
import com.onlineshop.onlineshop.service.CartItem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class CartController {

    //统一Session验证方法
    private User validateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new RuntimeException("Session不存在，请重新登录");
        }
        //验证Session是否有效
        try {
            session.getAttributeNames();
        } catch (IllegalStateException e) {
            throw new RuntimeException("Session已失效，请重新登录");
        }
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new RuntimeException("用户未登录，请重新登录");
        }
        return currentUser;
    }

    //获取用户特定的购物车键，与Session结构匹配
    private String getCartKey(HttpServletRequest request) {
        User currentUser = validateSession(request);
        return "user_cart_" + currentUser.getUid();
    }

    //统一错误响应格式
    private Map<String, Object> createErrorResponse(String message, String errorType) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("errorType", errorType);
        return response;
    }

    //添加到购物车
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody CartItem cartItem, HttpServletRequest request) {
        try {
            User currentUser = validateSession(request);
            String cartKey = getCartKey(request);
            HttpSession session = request.getSession();
            @SuppressWarnings("unchecked")
            List<CartItem> cart = (List<CartItem>) session.getAttribute(cartKey);
            if (cart == null) {
                cart = new ArrayList<>();
            }
            boolean exists = false;
            for (CartItem item : cart) {
                if (item.getGid().equals(cartItem.getGid())) {
                    item.setQuantity(item.getQuantity() + cartItem.getQuantity());
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                cart.add(cartItem);
            }
            session.setAttribute(cartKey, cart);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品已添加到购物车");
            response.put("cart", cart);
            response.put("userId", currentUser.getUid());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(createErrorResponse(e.getMessage(), "SESSION_INVALID"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("添加商品到购物车失败: " + e.getMessage(), "CART_ERROR"));
        }
    }

    //获取购物车
    @GetMapping
    public ResponseEntity<?> getCart(HttpServletRequest request) {
        try {
            User currentUser = validateSession(request);
            String cartKey = getCartKey(request);
            HttpSession session = request.getSession();
            @SuppressWarnings("unchecked")
            List<CartItem> cart = (List<CartItem>) session.getAttribute(cartKey);
            if (cart == null) {
                cart = new ArrayList<>();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cart", cart);
            response.put("userId", currentUser.getUid());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(createErrorResponse(e.getMessage(), "SESSION_INVALID"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取购物车失败: " + e.getMessage(), "CART_ERROR"));
        }
    }

    //更新购物车商品数量
    @PutMapping("/update/{goodsId}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long goodsId, @RequestBody Map<String, Integer> requestBody, HttpServletRequest request) {
        try {
            User currentUser = validateSession(request);
            String cartKey = getCartKey(request);
            HttpSession session = request.getSession();
            Integer quantity = requestBody.get("quantity");
            if (quantity == null || quantity < 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("数量无效", "VALIDATION_ERROR"));
            }
            @SuppressWarnings("unchecked")
            List<CartItem> cart = (List<CartItem>) session.getAttribute(cartKey);
            if (cart == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("购物车为空", "CART_EMPTY"));
            }
            boolean itemFound = false;
            for (CartItem item : cart) {
                if (item.getGid().equals(goodsId)) {
                    if (quantity == 0) {
                        cart.remove(item);
                    } else {
                        item.setQuantity(quantity);
                    }
                    itemFound = true;
                    break;
                }
            }
            if (!itemFound) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到指定的商品", "ITEM_NOT_FOUND"));
            }
            if (cart.isEmpty()) {
                session.removeAttribute(cartKey);
            } else {
                session.setAttribute(cartKey, cart);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "购物车更新成功");
            response.put("cart", cart);
            response.put("userId", currentUser.getUid());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(createErrorResponse(e.getMessage(), "SESSION_INVALID"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("更新购物车失败: " + e.getMessage(), "CART_ERROR"));
        }
    }

    //从购物车移除商品
    @DeleteMapping("/remove/{goodsId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long goodsId, HttpServletRequest request) {
        try {
            User currentUser = validateSession(request);
            String cartKey = getCartKey(request);
            HttpSession session = request.getSession();
            @SuppressWarnings("unchecked")
            List<CartItem> cart = (List<CartItem>) session.getAttribute(cartKey);
            if (cart == null || cart.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("购物车为空", "CART_EMPTY"));
            }
            boolean removed = cart.removeIf(item -> item.getGid().equals(goodsId));
            if (!removed) {
                return ResponseEntity.badRequest().body(createErrorResponse("未找到指定的商品", "ITEM_NOT_FOUND"));
            }
            if (cart.isEmpty()) {
                session.removeAttribute(cartKey);
            } else {
                session.setAttribute(cartKey, cart);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品已从购物车移除");
            response.put("cart", cart);
            response.put("userId", currentUser.getUid());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(createErrorResponse(e.getMessage(), "SESSION_INVALID"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("移除商品失败: " + e.getMessage(), "CART_ERROR"));
        }
    }

    //清空购物车
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(HttpServletRequest request) {
        try {
            User currentUser = validateSession(request);
            String cartKey = getCartKey(request);
            HttpSession session = request.getSession();
            session.removeAttribute(cartKey);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "购物车已清空");
            response.put("userId", currentUser.getUid());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(createErrorResponse(e.getMessage(), "SESSION_INVALID"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("清空购物车失败: " + e.getMessage(), "CART_ERROR"));
        }
    }

    //获取购物车商品数量
    @GetMapping("/count")
    public ResponseEntity<?> getCartCount(HttpServletRequest request) {
        try {
            User currentUser = validateSession(request);
            String cartKey = getCartKey(request);
            HttpSession session = request.getSession();
            List<CartItem> cart = (List<CartItem>) session.getAttribute(cartKey);
            int totalItems = 0;
            if (cart != null) {
                totalItems = cart.stream().mapToInt(CartItem::getQuantity).sum();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", totalItems);
            response.put("userId", currentUser.getUid());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(createErrorResponse(e.getMessage(), "SESSION_INVALID"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("获取购物车数量失败: " + e.getMessage(), "CART_ERROR"));
        }
    }
}