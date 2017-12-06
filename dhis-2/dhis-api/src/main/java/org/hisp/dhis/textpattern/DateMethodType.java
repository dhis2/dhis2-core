package org.hisp.dhis.textpattern;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public class DateMethodType
    extends BaseMethodType
{

    DateMethodType( Pattern patternRegex )
    {
        super( patternRegex );
    }

    @Override
    public boolean validateValue( String pattern, String value )
    {
        try
        {
            new SimpleDateFormat( pattern ).parse( value );
        }
        catch ( ParseException e )
        {
            return false;
        }

        return true;
    }
}
