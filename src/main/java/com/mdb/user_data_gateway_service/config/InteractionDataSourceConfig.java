package com.mdb.user_data_gateway_service.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.mdb.user_data_gateway_service.repository.interaction",
    entityManagerFactoryRef = "interactionEntityManagerFactory",
    transactionManagerRef = "interactionTransactionManager"
)
public class InteractionDataSourceConfig {

    @Primary
    @Bean(name = "interactionDataSourceProperties")
    @ConfigurationProperties("spring.datasource.interaction")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "interactionDataSource")
    public DataSource dataSource(@Qualifier("interactionDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "interactionEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("interactionDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.mdb.user_data_gateway_service.entity.interaction")
                .persistenceUnit("interaction")
                .build();
    }

    @Primary
    @Bean(name = "interactionTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("interactionEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Primary
    @Bean(name = "interactionTransactionTemplate")
    public org.springframework.transaction.support.TransactionTemplate transactionTemplate(
            @Qualifier("interactionTransactionManager") PlatformTransactionManager transactionManager) {
        return new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    }
}
