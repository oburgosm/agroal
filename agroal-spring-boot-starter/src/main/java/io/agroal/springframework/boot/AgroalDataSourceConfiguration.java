// Copyright (C) 2020 Red Hat, Inc. and individual contributors as indicated by the @author tags.
// You may not use this file except in compliance with the Apache License, Version 2.0.

package io.agroal.springframework.boot;

import io.agroal.narayana.NarayanaTransactionIntegration;

import org.jboss.tm.XAResourceRecoveryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
@Configuration( proxyBeanMethods = false )
@ConditionalOnClass( AgroalDataSource.class )
@ConditionalOnMissingBean( DataSource.class )
@ConditionalOnProperty( name = "spring.datasource.type", havingValue = "io.agroal.springframework.boot.AgroalDataSource", matchIfMissing = true )
public class AgroalDataSourceConfiguration {

    @Autowired( required = false )
    @SuppressWarnings( "WeakerAccess" )
    public JtaTransactionManager jtaPlatform;

    @Autowired( required = false )
    @SuppressWarnings( "WeakerAccess" )
    public XAResourceRecoveryRegistry recoveryRegistry;

    @Bean
    @ConfigurationProperties( prefix = "spring.datasource.agroal" )
    public AgroalDataSource dataSource(DataSourceProperties properties, @Value("${spring.datasource.agroal.connectable:false}") boolean connectable) {
        AgroalDataSource dataSource = properties.initializeDataSourceBuilder().type( AgroalDataSource.class ).build();
        if ( !StringUtils.hasLength( properties.getDriverClassName() ) ) {
            DatabaseDriver driver = DatabaseDriver.fromJdbcUrl( properties.determineUrl() );
            if ( connectable ) {
                dataSource.setDriverClassName( driver.getDriverClassName() );
            } else {
                dataSource.setDriverClassName( driver.getXaDataSourceClassName() );
            }
        }
        String name = properties.determineDatabaseName();
        dataSource.setName( name );
        if ( jtaPlatform != null && jtaPlatform.getTransactionManager() != null && jtaPlatform.getTransactionSynchronizationRegistry() != null) {
            NarayanaTransactionIntegration transactionIntegration = new NarayanaTransactionIntegration( 
                    jtaPlatform.getTransactionManager(), 
                    jtaPlatform.getTransactionSynchronizationRegistry(),
                    name,
                    connectable,
                    recoveryRegistry );
            dataSource.setJtaTransactionIntegration( transactionIntegration );
        }
        return dataSource;
    }
}
