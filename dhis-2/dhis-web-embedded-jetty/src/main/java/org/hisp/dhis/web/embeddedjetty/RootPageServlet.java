package org.hisp.dhis.web.embeddedjetty;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.hisp.dhis.web.embeddedjetty.ServletUtils.getResourceFileAsString;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class RootPageServlet
    extends HttpServlet
{
    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp )
        throws
        IOException,
        ServletException
    {
//        String content = "<html><body><h1>Root page</h1></body></html>";
//
//        resp.setContentType( "text/html" );
//        resp.setStatus( HttpServletResponse.SC_OK );
//        resp.getWriter().println( content );

        HttpSession session = req.getSession( false );
        Object springSecurityContext = session().getAttribute( "SPRING_SECURITY_CONTEXT" );

//        HttpSession session1 = session();
//        Object springSecurityContext1 = session1.getAttribute( "SPRING_SECURITY_CONTEXT" );

        if ( springSecurityContext != null )
        {
            String referer = (String) req.getAttribute( "origin" );
            req.setAttribute( "origin", referer );
            resp.sendRedirect( "/dhis-web-dashboard" );
        }
        else
        {
            String content = getResourceFileAsString( "login.html" );
            resp.setContentType( "text/html" );
            resp.setStatus( HttpServletResponse.SC_OK );
            resp.getWriter().println( content );
        }

    }

    public static HttpSession session()
    {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession( false ); // true == allow create
    }

    public static void forward( HttpServletRequest request, HttpServletResponse response, String page )
        throws
        ServletException,
        IOException,
        ServletException
    {
        request.getRequestDispatcher( page )
            .forward( request, response );
    }
}
