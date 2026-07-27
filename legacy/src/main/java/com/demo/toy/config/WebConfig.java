package com.demo.toy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 로컬 업로드 폴더를 /uploads/**로 접근 가능
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:///C:/dev/demo/uploads/");
    }
}