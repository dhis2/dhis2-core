package org.hisp.dhis.api.mobile.controller;



import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.api.mobile.NotAllowedException;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class AbstractMobileController
{
    @ExceptionHandler
    public void mapException(NotAllowedException exception, HttpServletResponse response ) throws IOException
    {
        response.setStatus( HttpServletResponse.SC_CONFLICT );
        response.setContentType( "text/plain" );
        response.getWriter().write( exception.getReason() );
    }
}
