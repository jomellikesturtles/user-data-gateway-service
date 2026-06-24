package com.mdb.user_data_gateway_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
    // "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
    // "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
    // "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
})
public class UserDataGatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserDataGatewayServiceApplication.class, args);

    }


}
