package org.hisp.dhis.config;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.conf.TestDhisConfigurationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource( locations = { "classpath*:/META-INF/dhis/beans.xml", "classpath*:/META-INF/dhis/security.xml" } )
public class UnitTestConfiguration
{
    private String configFileName = "h2TestConfig.conf";

    @Bean( name = "dhisConfigurationProvider" )
    public DhisConfigurationProvider dhisConfigurationProvider()
    {
        TestDhisConfigurationProvider testDhisConfigurationProvider = new TestDhisConfigurationProvider(
            configFileName );

        return testDhisConfigurationProvider;
    }
}
