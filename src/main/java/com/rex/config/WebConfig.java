package com.rex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cross origin configuration.
 *
 * <p>The dashboard is deployed on a different origin to the API, so without this it cannot call
 * anything. Origins come from configuration rather than a wildcard, because a wildcard here is the
 * easy answer and the wrong one.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final String[] allowedOrigins;

  public WebConfig(@Value("${rex.cors.allowed-origins}") String allowedOrigins) {
    this.allowedOrigins = allowedOrigins.split(",");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .maxAge(3600);
  }
}
