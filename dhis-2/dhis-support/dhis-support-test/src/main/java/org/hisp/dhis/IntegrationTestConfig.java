package org.hisp.dhis;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.context.annotation.*;

import java.util.Properties;

@Configuration
@ImportResource(locations = {"classpath*:/META-INF/dhis/beans.xml"})
public class IntegrationTestConfig
{
    private Properties properties() {
        Properties properties = new Properties();

        properties.setProperty("connection.url", System.getProperty("test.configuration.connection.url"));
        properties.setProperty("connection.dialect", "org.hisp.dhis.hibernate.dialect.DhisPostgresDialect");
        properties.setProperty("connection.driver_class", "org.postgresql.Driver");
        properties.setProperty("connection.username", System.getProperty("test.configuration.connection.username"));
        properties.setProperty("connection.password", System.getProperty("test.configuration.connection.password"));

        return properties;
    }

    @Bean(name = "dhisConfigurationProvider")
    public DhisConfigurationProvider dhisConfigurationProvider() {
        System.out.println("Test bean config");
        TestDhisConfigurationProvider dhisConfigurationProvider = new TestDhisConfigurationProvider();

        dhisConfigurationProvider.addProperties(properties());

        return dhisConfigurationProvider;
    }

}
