package com.zerobase.cms.user.config;

import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Qualifier(value = "mailgun")
    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
        return new BasicAuthRequestInterceptor("api", "d9726def8643c7f7fa2be9cb96e96646-c9746cf8-30ddb7cc");
    }
}
