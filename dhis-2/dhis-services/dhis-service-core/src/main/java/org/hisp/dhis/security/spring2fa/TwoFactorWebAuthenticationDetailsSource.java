package org.hisp.dhis.security.spring2fa;

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Henning Håkonsen
 */
@Component
public class TwoFactorWebAuthenticationDetailsSource
    implements AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails>
{
    @Override
    public WebAuthenticationDetails buildDetails( HttpServletRequest request )
    {
        System.out.println("called");
        return new TwoFactorWebAuthenticationDetails( request );
    }
}

