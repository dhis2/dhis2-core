package org.hisp.dhis.security.filter;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;

public class SameSiteCookieFilter
    implements Filter
{
    private static final Log log = LogFactory.getLog( SameSiteCookieFilter.class );

    private static final String ATTR_SAME_SITE = "SameSite";
    private static final String VALUE_SAME_SITE = "SameSite=Lax";

    @Override
    public void doFilter( ServletRequest req, ServletResponse resp, FilterChain chain )
        throws IOException,
        ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        chain.doFilter( request, response );

        log.info( String.format( "BEFORE Invoking SameSiteCookieFilter 2 for request URL: '%s' %s", request.getRequestURL(), response.getHeaders( HttpHeaders.SET_COOKIE ) ) );

        Collection<String> headerValues = response.getHeaders( HttpHeaders.SET_COOKIE );

        log.info( String.format( "AFTER Invoking SameSiteCookieFilter 2 for request URL: '%s' %s", request.getRequestURL(), headerValues ) );

        if ( !headerValues.isEmpty() )
        {
            log.info( String.format( "Found cookies: %s", headerValues ) );

            for ( String value : headerValues )
            {
                if ( value != null && !StringUtils.containsIgnoreCase( value, ATTR_SAME_SITE ) )
                {
                    String sameSiteValue = String.format( "%s; %s", value, VALUE_SAME_SITE );

                    response.setHeader( HttpHeaders.SET_COOKIE, sameSiteValue );
                    //response.addHeader( HttpHeaders.SET_COOKIE, sameSiteValue );

                    log.info( String.format( "Modified cookie value '%s' to '%s", value, sameSiteValue ) );
                }
            }
        }
    }

    @Override
    public void destroy()
    {
    }

    @Override
    public void init( FilterConfig filterConfig )
    {
    }
}
