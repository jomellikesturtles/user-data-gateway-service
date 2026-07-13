package com.mdb.user_data_gateway_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
public class UserDataGatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserDataGatewayServiceApplication.class, args);

    }




}
