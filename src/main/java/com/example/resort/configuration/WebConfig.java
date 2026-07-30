package com.example.resort.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String roomImageDir;

    public WebConfig(@Value("${app.upload.room-image-dir:uploads/rooms}") String roomImageDir) {
        this.roomImageDir = roomImageDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String imageLocation = Path.of(roomImageDir).toAbsolutePath().normalize().toUri().toString();
        if (!imageLocation.endsWith("/")) {
            imageLocation = imageLocation + "/";
        }

        registry.addResourceHandler("/uploads/rooms/**")
                .addResourceLocations(imageLocation);
    }
}
