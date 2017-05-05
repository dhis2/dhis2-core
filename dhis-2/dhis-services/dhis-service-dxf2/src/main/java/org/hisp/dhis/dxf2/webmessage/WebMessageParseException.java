package org.hisp.dhis.dxf2.webmessage;

/**
 * Created by vanyas on 5/5/17.
 */
public class WebMessageParseException
    extends RuntimeException
{
    public WebMessageParseException( String message )
    {
        super( message );
    }

    public WebMessageParseException( Throwable cause )
    {
        super( cause );
    }

    public WebMessageParseException( String message, Throwable cause )
    {
        super( message, cause );
    }

}
