package com.onlineshop.onlineshop.controller;

import com.onlineshop.onlineshop.entity.Goods;
import com.onlineshop.onlineshop.entity.User;
import com.onlineshop.onlineshop.service.GoodsService;
import com.onlineshop.onlineshop.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/goods")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private UserService userService;

    //获取所有商品，顾客和商家都可以访问
    @GetMapping
    public ResponseEntity<?> getAllGoods() {
        try {
            List<Goods> goodsList = goodsService.getAvailableGoods();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("goods", goodsList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //根据ID获取商品详情
    @GetMapping("/{gid}")
    public ResponseEntity<?> getGoodsById(@PathVariable Long gid) {
        try {
            Goods goods = goodsService.getGoodsById(gid);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("goods", goods);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //根据商品ID获取商家信息
    @GetMapping("/{gid}/merchant")
    public ResponseEntity<?> getMerchantByGoodsId(@PathVariable Long gid) {
        try {
            Goods goods = goodsService.getGoodsById(gid);
            User merchant = goods.getMerchant();
            if (merchant == null) {
                throw new RuntimeException("该商品没有对应的商家信息");
            }
            //创建商家信息响应（不包含敏感信息）
            Map<String, Object> merchantInfo = new HashMap<>();
            merchantInfo.put("uid", merchant.getUid());
            merchantInfo.put("nickname", merchant.getNickname());
            merchantInfo.put("email", merchant.getEmail());
            merchantInfo.put("avatarUrl", merchant.getAvatarUrl() != null ? merchant.getAvatarUrl() : "/uploads/avatars/default-avatar.png");
            merchantInfo.put("role", merchant.getRole() != null ? merchant.getRole().name() : "MERCHANT");
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("merchant", merchantInfo);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //按名称搜索商品
    @GetMapping("/search/name")
    public ResponseEntity<?> searchGoodsByName(@RequestParam String name) {
        try {
            List<Goods> goodsList = goodsService.searchGoodsByName(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("goods", goodsList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //按价格范围搜索商品
    @GetMapping("/search/price")
    public ResponseEntity<?> searchGoodsByPriceRange(@RequestParam Double minPrice, @RequestParam Double maxPrice) {
        try {
            List<Goods> goodsList = goodsService.searchGoodsByPriceRange(minPrice, maxPrice);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("goods", goodsList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //获取有库存的商品
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableGoods() {
        try {
            List<Goods> goodsList = goodsService.getAvailableGoods();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("goods", goodsList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //添加商品
    @PostMapping
    public ResponseEntity<?> addGoods(@RequestBody Goods goods, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权限操作");
                return ResponseEntity.status(403).body(response);
            }
            Goods savedGoods = goodsService.addGoods(goods, currentUser);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品添加成功");
            response.put("goods", savedGoods);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //更新商品信息（仅商家，只能更新自己的商品）
    @PutMapping("/{gid}")
    public ResponseEntity<?> updateGoods(@PathVariable Long gid, @RequestBody Goods goods,
                                         HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权限操作");
                return ResponseEntity.status(403).body(response);
            }
            goods.setGid(gid);
            Goods updatedGoods = goodsService.updateGoods(goods, currentUser);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品更新成功");
            response.put("goods", updatedGoods);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //删除商品（仅商家，只能删除自己的商品）
    @DeleteMapping("/{gid}")
    public ResponseEntity<?> deleteGoods(@PathVariable Long gid, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权限操作");
                return ResponseEntity.status(403).body(response);
            }
            goodsService.deleteGoods(gid, currentUser);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品删除成功");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //获取当前商家的商品
    @GetMapping("/my-goods")
    public ResponseEntity<?> getMyGoods(HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权限访问");
                return ResponseEntity.status(403).body(response);
            }
            List<Goods> goodsList = goodsService.getGoodsByMerchant(currentUser.getUid());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("goods", goodsList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //商家搜索自己的商品
    @GetMapping("/my-goods/search")
    public ResponseEntity<?> searchMyGoods(@RequestParam String name, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权限访问");
                return ResponseEntity.status(403).body(response);
            }
            List<Goods> goodsList = goodsService.searchGoodsByMerchantAndName(currentUser.getUid(), name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("goods", goodsList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //获取商家商品统计
    @GetMapping("/my-goods/stats")
    public ResponseEntity<?> getMyGoodsStats(HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权限访问");
                return ResponseEntity.status(403).body(response);
            }
            var stats = goodsService.getMerchantGoodsStats(currentUser.getUid());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //根据商家ID获取商品列表
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<?> getGoodsByMerchant(@PathVariable Long merchantId) {
        try {
            List<Goods> goodsList = goodsService.getGoodsByMerchant(merchantId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("goods", goodsList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //下架商品
    @PostMapping("/{gid}/deactivate")
    public ResponseEntity<?> deactivateGoods(@PathVariable Long gid, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权限操作");
                return ResponseEntity.status(403).body(response);
            }
            Goods goods = goodsService.deactivateGoods(gid, currentUser);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品已下架");
            response.put("goods", goods);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //上架商品
    @PostMapping("/{gid}/activate")
    public ResponseEntity<?> activateGoods(@PathVariable Long gid, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权限操作");
                return ResponseEntity.status(403).body(response);
            }
            Goods goods = goodsService.activateGoods(gid, currentUser);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "商品已上架");
            response.put("goods", goods);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //批量下架商品
    @PostMapping("/batch-deactivate")
    public ResponseEntity<?> batchDeactivateGoods(@RequestBody List<Long> goodsIds, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权限操作");
                return ResponseEntity.status(403).body(response);
            }
            List<Goods> deactivatedGoods = new ArrayList<>();
            for (Long gid : goodsIds) {
                try {
                    Goods goods = goodsService.deactivateGoods(gid, currentUser);
                    deactivatedGoods.add(goods);
                } catch (RuntimeException e) {
                    //单个商品操作失败，继续处理其他商品
                    System.err.println("下架商品失败: " + gid + ", 错误: " + e.getMessage());
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量下架完成，成功下架 " + deactivatedGoods.size() + " 个商品");
            response.put("goods", deactivatedGoods);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量操作失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //获取商家商品销量排名
    @GetMapping("/my-goods/sales-ranking")
    public ResponseEntity<?> getMyGoodsSalesRanking(HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权限访问");
                return ResponseEntity.status(403).body(response);
            }
            List<Goods> salesRanking = goodsService.getGoodsSalesRanking(currentUser.getUid());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ranking", salesRanking);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    //获取商家商品销售额排名
    @GetMapping("/my-goods/sales-value-ranking")
    public ResponseEntity<?> getMyGoodsSalesValueRanking(HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null || currentUser.getRole() != User.UserRole.MERCHANT) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权限访问");
                return ResponseEntity.status(403).body(response);
            }
            List<Goods> salesValueRanking = goodsService.getGoodsSalesValueRanking(currentUser.getUid());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ranking", salesValueRanking);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}