package org.hisp.dhis.servlet.filter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpSecureSessionCookieFilter
    implements Filter
{
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";

    @Override
    public void init( FilterConfig filterConfig )
        throws ServletException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void doFilter( ServletRequest req, ServletResponse resp, FilterChain chain )
        throws IOException,
        ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        for ( Cookie cookie : request.getCookies() )
        {

        }

        response.addCookie( null );

        chain.doFilter( request, response );
    }

    @Override
    public void destroy()
    {
        // TODO Auto-generated method stub
    }

    private Optional<Cookie> getSessionCookie( HttpServletRequest request )
    {
        return null;
    }
}
