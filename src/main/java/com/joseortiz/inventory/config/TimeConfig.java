package com.joseortiz.inventory.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the system clock as a bean so date-dependent logic (stock age, future-date checks) can
 * be tested with a fixed clock.
 */
@Configuration
public class TimeConfig {

  /**
   * @return the system clock in the JVM default zone
   */
  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
