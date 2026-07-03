package com.lifecell.diia.be;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = {"com.lifecell.diia.be"})
@EnableConfigurationProperties({Properties.class})
@Slf4j
public class Application {

    public static void main(String[] args) {
        System.setProperty("java.net.useSystemProxies", "true");
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ApplicationRunner buildInfoLogger(BuildProperties buildProperties) {
        return args -> {
            log.info("========================================");
            log.info("Starting Lifecell MNP BE Application");
            log.info("Build Version: {}", buildProperties.getVersion());
            log.info("Build Time: {}", buildProperties.getTime());
            log.info("Build Group: {}", buildProperties.getGroup());
            log.info("Build Artifact: {}", buildProperties.getArtifact());
            log.info("========================================");
        };
    }
}
