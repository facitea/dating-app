/**
 * WebMvcConfig 설정
 */
package com.dating.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    // 업로드 파일 정적 리소스 경로 등록
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String configuredDir = StringUtils.hasText(uploadProperties.dir()) ? uploadProperties.dir() : "uploads";
        Path uploadPath = Paths.get(configuredDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath.toUri().toString());
    }
}
