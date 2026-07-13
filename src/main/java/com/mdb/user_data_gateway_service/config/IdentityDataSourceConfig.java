package com.mdb.user_data_gateway_service.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.mdb.user_data_gateway_service.repository.identity",
    entityManagerFactoryRef = "identityEntityManagerFactory",
    transactionManagerRef = "identityTransactionManager"
)
public class IdentityDataSourceConfig {

    @Bean(name = "identityDataSourceProperties")
    @ConfigurationProperties("spring.datasource.identity")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "identityDataSource")
    public DataSource dataSource(@Qualifier("identityDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "identityEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("identityDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.mdb.user_data_gateway_service.entity.identity")
                .persistenceUnit("identity")
                .build();
    }

    @Bean(name = "identityTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("identityEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public liquibase.integration.spring.SpringLiquibase liquibase(@Qualifier("identityDataSource") DataSource dataSource) {
        liquibase.integration.spring.SpringLiquibase liquibase = new liquibase.integration.spring.SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.xml");
        return liquibase;
    }

    @Bean(name = "identityTransactionTemplate")
    public org.springframework.transaction.support.TransactionTemplate transactionTemplate(
            @Qualifier("identityTransactionManager") PlatformTransactionManager transactionManager) {
        return new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    }
}
