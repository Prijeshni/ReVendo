package com.revendo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Configures Spring MVC to serve uploaded product images from the filesystem.
 *
 * Images are stored at {@code app.upload.dir} (default:
 * {@code ${user.dir}/src/main/resources/static/images}).
 * They are served publicly at {@code /images/**} so the frontend can load them
 * via {@code http://localhost:8080/images/<filename>}.
 *
 * Using an explicit ResourceHandlerRegistry instead of relying solely on
 * {@code spring.web.resources.static-locations} prevents issues when the
 * property placeholder resolves differently between Maven and IDE launchers.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:${user.dir}/src/main/resources/static/images}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve to absolute path once at startup
        String absoluteDir = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize()
                .toString();

        // Ensure the file: URI ends with a separator so Spring can list the directory
        String resourceLocation = "file:" + absoluteDir + "/";

        registry.addResourceHandler("/images/**")
                .addResourceLocations(resourceLocation);
    }
}
