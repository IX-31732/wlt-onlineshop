package com.onlineshop.onlineshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //获取项目根目录的绝对路径
        String projectRoot = System.getProperty("user.dir");
        System.out.println("项目根目录: " + projectRoot);
        //映射头像文件
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + projectRoot + "/uploads/avatars/")
                .setCachePeriod(0);
        //映射商品图片
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:" + projectRoot + "/uploads/products/")
                .setCachePeriod(0);
    }

    //处理/uploads/路径的访问
    @GetMapping("/uploads/")
    public ResponseEntity<?> handleUploadsDirectory() {
        return ResponseEntity.badRequest().body("不能直接访问上传目录，请访问具体的文件路径");
    }
}