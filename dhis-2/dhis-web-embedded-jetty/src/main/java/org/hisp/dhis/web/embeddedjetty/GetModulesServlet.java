package org.hisp.dhis.web.embeddedjetty;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

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

    /**
     * Reads given resource file as a string.
     *
     * @param fileName path to the resource file
     * @return the file's contents
     * @throws IOException if read fails for any reason
     */
    static String getResourceFileAsString( String fileName )
        throws
        IOException
    {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream( fileName ))
        {
            if ( is == null )
                return null;
            try (InputStreamReader isr = new InputStreamReader( is );
                 BufferedReader reader = new BufferedReader( isr ))
            {
                return reader.lines().collect( Collectors.joining( System.lineSeparator() ) );
            }
        }
    }
}

