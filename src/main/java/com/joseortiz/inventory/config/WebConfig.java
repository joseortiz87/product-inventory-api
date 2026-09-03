package com.joseortiz.inventory.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC customisations: CORS rules for the Angular front end.
 *
 * <p>Also registers {@link InventoryProperties}, so the properties are available in {@code
 * @WebMvcTest} slices that load this configurer.
 */
@Configuration
@EnableConfigurationProperties(InventoryProperties.class)
public class WebConfig implements WebMvcConfigurer {

  private final InventoryProperties properties;

  /**
   * Creates the configuration.
   *
   * @param properties application settings holding the allowed origins
   */
  public WebConfig(InventoryProperties properties) {
    this.properties = properties;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .maxAge(3600);
  }
}
