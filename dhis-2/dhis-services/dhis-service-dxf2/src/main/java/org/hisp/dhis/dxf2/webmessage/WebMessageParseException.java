package org.hisp.dhis.dxf2.webmessage;

import java.io.IOException;

/**
 * Created by vanyas on 5/5/17.
 */
public class WebMessageParseException
    extends IOException
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
