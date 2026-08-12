package com.lzf.langchain4j.config;

import com.lzf.langchain4j.constant.AppConstant;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/code_output/**")
                .addResourceLocations(Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR).toUri().toString());
    }
}
