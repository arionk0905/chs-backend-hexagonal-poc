package com.chs.channel;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mariadb.MariaDBContainer;

@TestConfiguration(proxyBeanMethods = false)
class MariaDbTestConfiguration {
    @Bean
    @ServiceConnection
    MariaDBContainer mariaDb() {
        return new MariaDBContainer("mariadb:11.8")
                .withInitScript("db/mariadb/schema.sql");
    }
}
