package org.hisp.dhis.webapi;

import org.hisp.dhis.config.H2DhisConfigurationProvider;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JwtConfigProviderConfig
{

    @Bean( name = "dhisConfigurationProvider" )
    @Primary
    public DhisConfigurationProvider dhisConfigurationProvider()
    {
        return new H2DhisConfigurationProvider( "h2TestConfigWithJWTAuth.conf" );
    }
}
