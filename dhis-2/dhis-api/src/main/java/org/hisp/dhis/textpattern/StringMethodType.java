package org.hisp.dhis.textpattern;

import java.util.regex.Pattern;

public class StringMethodType
    extends BaseMethodType
{
    StringMethodType( Pattern pattern, RequiredStatus requiredStatus )
    {
        super( pattern, requiredStatus );
    }

    @Override
    public String getValueRegex( String format )
    {
        format = format.replaceAll( "\\^", "" );
        format = format.replaceAll( "\\$", "" );
        return format;
    }
}
