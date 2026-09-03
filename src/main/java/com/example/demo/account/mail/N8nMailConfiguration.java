package com.example.demo.account.mail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(N8nMailProperties.class)
class N8nMailConfiguration {}
