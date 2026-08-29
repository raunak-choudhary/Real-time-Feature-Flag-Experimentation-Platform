package com.rex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RexPlatformApplication {

  public static void main(String[] args) {
    SpringApplication.run(RexPlatformApplication.class, args);
  }
}
