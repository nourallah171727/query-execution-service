package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {}
/*
    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${app.datasource.admin.username}")
    private String adminUsername;

    @Value("${app.datasource.admin.password}")
    private String adminPassword;

    @Value("${app.datasource.user.username}")
    private String userUsername;

    @Value("${app.datasource.user.password}")
    private String userPassword;

    @Bean(name = "adminDataSource")
    public DataSource adminDataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(adminUsername)
                .password(adminPassword)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean(name = "userDataSource")
    public DataSource userDataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(userUsername)
                .password(userPassword)
                .driverClassName(driverClassName)
                .build();
    }
}
*/