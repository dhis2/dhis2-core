package org.hisp.dhis.textpattern;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public class DateMethodType
    extends BaseMethodType
{

    DateMethodType( Pattern pattern )
    {
        super( pattern );
    }

    @Override
    public boolean validateText( String format, String text )
    {
        try
        {
            new SimpleDateFormat( format ).parse( text );
        }
        catch ( ParseException e )
        {
            return false;
        }

        return true;
    }

    @Override
    public String getValueRegex( String format )
    {
        return ".*?";
    }

}
