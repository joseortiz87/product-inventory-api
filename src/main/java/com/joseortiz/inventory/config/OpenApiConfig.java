package com.joseortiz.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata shown in Swagger UI at {@code /swagger-ui.html}.
 */
@Configuration
public class OpenApiConfig {

  /**
   * Describes the API for the generated OpenAPI document.
   *
   * @return the OpenAPI definition
   */
  @Bean
  public OpenAPI inventoryOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Product Inventory API")
                .version("v1")
                .description(
                    "Imports product inventory spreadsheets and exposes a paginated, sortable"
                        + " catalogue with stock-age metrics.")
                .license(new License().name("MIT")));
  }
}
