package com.onlineshop.onlineshop.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class FileUploadController {
    //使用相对路径，基于项目根目录
    private final String AVATARS_DIR = "uploads/avatars/";
    private final String PRODUCTS_DIR = "uploads/products/";
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        System.out.println("=== 头像上传开始 ===");
        try {
            //获取项目根目录
            String projectRoot = System.getProperty("user.dir");
            String fullAvatarsDir = projectRoot + "/" + AVATARS_DIR;
            System.out.println("项目根目录: " + projectRoot);
            System.out.println("头像目录: " + fullAvatarsDir);
            return uploadFile(file, fullAvatarsDir, "/uploads/avatars/");
        } catch (Exception e) {
            System.err.println("头像上传异常: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "头像上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/product")
    public ResponseEntity<?> uploadProductImage(@RequestParam("file") MultipartFile file) {
        try {
            String projectRoot = System.getProperty("user.dir");
            String fullProductsDir = projectRoot + "/" + PRODUCTS_DIR;
            return uploadFile(file, fullProductsDir, "/uploads/products/");
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "商品图片上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private ResponseEntity<?> uploadFile(MultipartFile file, String uploadDir, String urlPrefix) throws IOException {
        Map<String, Object> response = new HashMap<>();
        //创建目录
        File dir = new File(uploadDir);
        System.out.println("创建目录: " + dir.getAbsolutePath());
        System.out.println("目录是否存在: " + dir.exists());
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            System.out.println("目录创建结果: " + created);
            if (!created) {
                response.put("success", false);
                response.put("message", "无法创建上传目录");
                return ResponseEntity.badRequest().body(response);
            }
        }
        //验证文件
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "文件不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        if (!isImageFile(file.getOriginalFilename())) {
            response.put("success", false);
            response.put("message", "只支持图片文件格式");
            return ResponseEntity.badRequest().body(response);
        }
        try {
            //生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            //保存文件
            Path filePath = Paths.get(uploadDir + uniqueFileName);
            System.out.println("保存文件到: " + filePath.toAbsolutePath());
            Files.write(filePath, file.getBytes());
            String fileUrl = urlPrefix + uniqueFileName;
            System.out.println("文件URL: " + fileUrl);
            response.put("success", true);
            response.put("message", "文件上传成功");
            response.put("fileUrl", fileUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            System.err.println("文件保存异常: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "文件保存失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private boolean isImageFile(String filename) {
        if (filename == null) return false;
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return extension.equals("jpg") || extension.equals("jpeg") ||
                extension.equals("png") || extension.equals("gif") ||
                extension.equals("webp");
    }
}