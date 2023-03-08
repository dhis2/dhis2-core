package org.hisp.dhis.webapi.security.switchuser;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@RequiredArgsConstructor
public class DhisSwitchUserFilter extends SwitchUserFilter
{
    private final DhisConfigurationProvider dhisConfigurationProvider;

    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
        throws
        IOException,
        ServletException
    {
        boolean enabled = dhisConfigurationProvider.isEnabled( ConfigurationKey.SWITCH_USER_FEATURE_ENABLED );
        if ( enabled && isAllowListedIp( request.getRemoteAddr() ) )
        {
            super.doFilter( request, response, chain );
            return;
        }

        chain.doFilter( request, response );
    }

    private boolean isAllowListedIp( String remoteAddr )
    {
        String property = dhisConfigurationProvider.getProperty( ConfigurationKey.SWITCH_USER_ALLOW_LISTED_IPS );
        for ( String ip : property.split( "," ) )
        {
            if ( ip.equals( remoteAddr ) )
            {
                return true;
            }
        }

        return false;
    }
}
