package org.hisp.dhis.web.embeddedjetty;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.hisp.dhis.web.embeddedjetty.ServletUtils.getResourceFileAsString;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class GetModulesServlet
    extends HttpServlet
{
    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp )
        throws
        IOException
    {
        String content = getResourceFileAsString( "modules.json" );

        resp.setContentType( "application/json" );
        resp.setStatus( HttpServletResponse.SC_OK );
        resp.getWriter().println( content );
    }


}

