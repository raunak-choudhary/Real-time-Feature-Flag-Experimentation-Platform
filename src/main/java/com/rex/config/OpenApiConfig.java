package com.rex.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI description, generated from the controllers so it cannot drift from the code. */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI rexOpenApi(@Value("${server.port:8080}") String port) {
    return new OpenAPI()
        .info(
            new Info()
                .title("REX Platform API")
                .version("v1")
                .description(
                    """
                    Feature flag and experimentation service.

                    Flags are evaluated statelessly, so a configuration change takes effect on the \
                    next call. Experiment assignments are sticky and persisted, because moving a \
                    user between variants mid experiment would invalidate the result.

                    Every error is an RFC 7807 problem detail.""")
                .license(new License().name("MIT")))
        .servers(List.of(new Server().url("http://localhost:" + port).description("Local")));
  }
}
