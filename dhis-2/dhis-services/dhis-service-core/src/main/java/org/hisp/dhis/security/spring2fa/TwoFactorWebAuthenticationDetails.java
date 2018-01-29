package org.hisp.dhis.security.spring2fa;

import org.hisp.dhis.util.ObjectUtils;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Henning Håkonsen
 * @author Lars Helge Øverland
 */
public class TwoFactorWebAuthenticationDetails
    extends WebAuthenticationDetails
{
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    private String code;

    private String ip;

    TwoFactorWebAuthenticationDetails( HttpServletRequest request )
    {
        super( request );
        code = request.getParameter( "code" );
        ip = ObjectUtils.firstNonNull( request.getHeader( HEADER_FORWARDED_FOR ), request.getRemoteAddr() );
    }

    public String getCode()
    {
        return code;
    }

    public String getIp()
    {
        return ip;
    }
}

